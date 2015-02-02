package server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.List;

import messages.AbstractMessage;
import messages.ConnAckMessage;
import messages.PingRespMessage;
import messages.PollRespMessage;
import messages.PushMessage;

import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoServiceStatistics;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.demux.DemuxingProtocolDecoder;
import org.apache.mina.filter.codec.demux.DemuxingProtocolEncoder;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import beans.Smessage;

import commons.Constants;

import codec.ConnAckEncoder;
import codec.ConnectDecoder;
import codec.DisconnectDecoder;
import codec.PingReqDecoder;
import codec.PingRespEncoder;
import codec.PollDecoder;
import codec.PollRespEncoder;
import codec.PushAckDecoder;
import codec.PushEncoder;

public abstract class Server {
	
    private static final Logger LOG = LoggerFactory.getLogger(Server.class);
    private IoAcceptor m_acceptor;
    
    protected abstract void dealConnected(String clientID);
    protected abstract void dealDisConnected(String clientID);
    
    HandleMessage handleMsg = HandleMessage.getInstance();
    
    public void startServer() throws IOException {
        DemuxingProtocolDecoder decoder = new DemuxingProtocolDecoder();
        decoder.addMessageDecoder(new ConnectDecoder());
        decoder.addMessageDecoder(new PollDecoder());
        decoder.addMessageDecoder(new PushAckDecoder());
        decoder.addMessageDecoder(new DisconnectDecoder());
        decoder.addMessageDecoder(new PingReqDecoder());
        
        DemuxingProtocolEncoder encoder = new DemuxingProtocolEncoder();
        encoder.addMessageEncoder(ConnAckMessage.class, new ConnAckEncoder());
        encoder.addMessageEncoder(PushMessage.class, new PushEncoder());
        encoder.addMessageEncoder(PollRespMessage.class, new PollRespEncoder());
        encoder.addMessageEncoder(PingRespMessage.class, new PingRespEncoder());
        
        m_acceptor = new NioSocketAcceptor();
        
        //m_acceptor.getFilterChain().addLast( "logger", new LoggingFilter("SERVER LOG") );//TODO 自定义自己的日志过滤器
        m_acceptor.getFilterChain().addLast( "codec", new ProtocolCodecFilter(encoder, decoder));

        handleMsg.setServer(this);
        
        LSNHandler handler = new LSNHandler(handleMsg);
        
        m_acceptor.setHandler(handler);
        ((NioSocketAcceptor)m_acceptor).setReuseAddress(true);
        ((NioSocketAcceptor)m_acceptor).getSessionConfig().setReuseAddress(true);
        m_acceptor.getSessionConfig().setReadBufferSize( 2048 );
        m_acceptor.getSessionConfig().setIdleTime( IdleStatus.BOTH_IDLE, Constants.DEFAULT_CONNECT_TIMEOUT );
        m_acceptor.getStatistics().setThroughputCalculationInterval(10);
        m_acceptor.getStatistics().updateThroughput(System.currentTimeMillis());
        m_acceptor.bind( new InetSocketAddress(Constants.PORT) );
        LOG.info("Server binded");
        
        //Bind  a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                stopServer();
            }
        });
    }
    
    protected void stopServer() {
        LOG.info("Server stopping...");     

        //log statistics
        IoServiceStatistics statistics  = m_acceptor.getStatistics();
        statistics.updateThroughput(System.currentTimeMillis());
        
        //统计不准确 有时候是0  原因   获取Throughput的时候，Throughput自动变为0   
        //MINA 源码 service.getManagedSessionCount() == 0就会重置 Throughput
        //TODO 不关闭client就关闭server就能获取正确值，关闭server前需要有至少一个连接
        
        System.out.println(String.format("Total read bytes: %d, read throughtput: %f (b/s)", Long.valueOf(statistics.getReadBytes()), Double.valueOf(statistics.getReadBytesThroughput())));
        System.out.println(String.format("Total read msgs: %d, read msg throughtput: %f (msg/s)", Long.valueOf(statistics.getReadMessages()), Double.valueOf(statistics.getReadMessagesThroughput())));
        System.out.println(String.format("Total write bytes: %d, write throughtput: %f (b/s)", Long.valueOf(statistics.getWrittenBytes()), Double.valueOf(statistics.getWrittenBytesThroughput())));
        System.out.println(String.format("Total write msgs: %d, read write throughtput: %f (msg/s)", Long.valueOf(statistics.getWrittenMessages()), Double.valueOf(statistics.getWrittenMessagesThroughput())));
        
        for(IoSession session: m_acceptor.getManagedSessions().values()) {
            if(session.isConnected() && !session.isClosing()){
                session.close(false);
            }
        }

        m_acceptor.unbind();
        m_acceptor.dispose();
        
        LOG.info("Server stopped");
    }
    
    public List<String> getOnlineClientIDs(){
    	return handleMsg.getOnlineClientIDs();
    }
    
    public HashSet<String> getAllClientIDs(){
    	return handleMsg.getAllClientIDs();
    }
    
    public void push2All(Smessage msg){
    	handleMsg.push2All(msg);
    }
    
    public void allPush(){
    	handleMsg.allPush();
    }
    
	public void push(String clientID){
		handleMsg.push(clientID,false);
	}
    public void push(String clientID, boolean retain){
    	handleMsg.push(clientID,AbstractMessage.QOSType.MOST_ONE,retain);
    }
    public void push(String clientID, AbstractMessage.QOSType qos, boolean retain){
    	handleMsg.push(clientID, qos, retain);
    }
    
    public void dealConnect(String clientID){
    	dealConnected(clientID);
    }
    
    public void dealDisconnect(String clientID){
    	dealDisConnected(clientID);
    }
}
