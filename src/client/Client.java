package client;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import messages.AbstractMessage;
import messages.ConnAckMessage;
import messages.ConnectMessage;
import messages.DisconnectMessage;
import messages.MessageIDMessage;
import messages.PingReqMessage;
import messages.PollMessage;
import messages.PushAckMessage;

import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.demux.DemuxingProtocolDecoder;
import org.apache.mina.filter.codec.demux.DemuxingProtocolEncoder;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import commons.Constants;

import codec.ConnAckDecoder;
import codec.ConnectEncoder;
import codec.DisconnectEncoder;
import codec.PingReqEncoder;
import codec.PingRespDecoder;
import codec.PollEncoder;
import codec.PollRespDecoder;
import codec.PushAckEncoder;
import codec.PushDecoder;
import exception.ConnectionException;
import exception.LSNException;
import exception.PollException;
import exception.PushException;

import util.MessageIDGenerator;

public abstract class Client {
	
    final static int DEFAULT_RETRIES = 3;
    final static int RETRIES_QOS_GT0 = 3;
    
	private static final Logger LOG = LoggerFactory.getLogger(Client.class);
	 
	private static final long CONNECT_TIMEOUT = 3 * 1000L; // 3秒
	private static final long POLLRESP_TIMEOUT = 3 * 1000L;
	private static final int KEEPALIVE_SECS = 3;
	private static final int POLL_SECS = 5;//轮询间隔
	private static final int NUM_SCHEDULER_TIMER_THREAD = 1;
	private int m_connectRetries = DEFAULT_RETRIES;
	
    private String m_hostname;
    private int m_port;
    private IoConnector m_connector;
    private IoSession m_session;
    private CountDownLatch m_connectBarrier;
    private CountDownLatch m_pollBarrier;
    private int m_receivedPollRespMessageID;
    
    private byte m_returnCode;
    
    private ScheduledExecutorService m_scheduler;
	private ScheduledFuture m_pingerHandler;
    private String m_macAddress;
    private String m_ipAddress;
    private MessageIDGenerator m_messageIDGenerator = new MessageIDGenerator();
    
    private String m_clientID;
    private boolean isPush;
    private String userName;
	private String passWord;
	private String status;
    protected abstract void dealPushed(String msg, byte[] payload);
    protected abstract void dealPollResp(String msg, byte[] payload);
    final Runnable pingerDeamon = new Runnable() {
        public void run() {
            LOG.debug("Pingreq sent");
            //send a ping req
            m_session.write(new PingReqMessage());
        }
    };
    
    //需要定时向服务器poll，不需要心跳  POLL情况下
    final Runnable pollerDeamon = new Runnable(){
        public void run() {
            LOG.debug("Poll sent");
            //send a poll
            poll();
        }
    };
    
    public Client(String host, int port, String username, String password, boolean isPush) {
        m_hostname = host;
        m_port = port;
        this.isPush = isPush;
        this.userName = username;
        this.passWord = password;
        init();
    }
    public Client(String host, int port, String username, String password, boolean isPush, String clientID) {
        this(host, port,username,password,isPush);
        m_clientID = clientID;
    }
    public Client(String host, int port, boolean isPush) {//用户登陆@2015.01.20
        this(host, port,"DEFFAULT","DEFFAULT",isPush);
    }
    
    protected void init() {
        DemuxingProtocolDecoder decoder = new DemuxingProtocolDecoder();
        decoder.addMessageDecoder(new ConnAckDecoder());
        decoder.addMessageDecoder(new PushDecoder());
        decoder.addMessageDecoder(new PollRespDecoder());
        decoder.addMessageDecoder(new PingRespDecoder());

        DemuxingProtocolEncoder encoder = new DemuxingProtocolEncoder();
        encoder.addMessageEncoder(ConnectMessage.class, new ConnectEncoder());
        encoder.addMessageEncoder(PollMessage.class, new PollEncoder());
        encoder.addMessageEncoder(PushAckMessage.class, new PushAckEncoder());
        encoder.addMessageEncoder(DisconnectMessage.class, new DisconnectEncoder());
        encoder.addMessageEncoder(PingReqMessage.class, new PingReqEncoder());

        m_connector = new NioSocketConnector();

        //m_connector.getFilterChain().addLast("logger", new LoggingFilter());
        m_connector.getFilterChain().addLast("codec", new ProtocolCodecFilter(encoder, decoder));

        m_connector.setHandler(new ClientLSNHandler(this));
        m_connector.getSessionConfig().setReadBufferSize(2048);
        m_connector.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, Constants.DEFAULT_CONNECT_TIMEOUT);

        m_scheduler = Executors.newScheduledThreadPool(NUM_SCHEDULER_TIMER_THREAD);
        
        m_macAddress = readMACAddress(); 
        m_ipAddress = readIPAddress();
        
        //Bind  a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                close();
                shutdown();
            }
        });
        
        if(!isPush)
        	updatePoller();
    }
    
    public boolean connect() throws LSNException {
        return connect(false);
    }
    public boolean connect(boolean cleanSession) throws LSNException {
        int retries = 0;

        try {
            ConnectFuture future = m_connector.connect(new InetSocketAddress(m_hostname, m_port));
            LOG.info("Client waiting to connect to server");
            future.awaitUninterruptibly();
            m_session = future.getSession();//TODO 连接不到服务器的情况下，没有做异常处理
        } catch (RuntimeIoException e) {
            LOG.info("Failed to connect, retry " + retries + " of (" + m_connectRetries + ")", e);
        }

        if (retries == m_connectRetries) {
            throw new LSNException("Can't connect to the server after " + retries + "retries");//没有进行多次重新连接
        }

        m_connectBarrier = new CountDownLatch(1);

        //send a message over the session
        ConnectMessage connMsg = new ConnectMessage();
        connMsg.setKeepAlive(KEEPALIVE_SECS);
        if (m_clientID == null) {
            //m_clientID = generateClientID();
        	m_clientID = userName;
        }
        connMsg.setClientID(m_clientID);
        connMsg.setCleanSession(cleanSession);
        if(userName != null && passWord != null){
        	connMsg.setUserFlag(true);
        	connMsg.setPasswordFlag(true);
        	connMsg.setUsername(userName);
        	connMsg.setPassword(passWord);
        }
        
        m_session.write(connMsg);

        //suspend until the server respond with CONN_ACK
        boolean unlocked = false;
        try {
            unlocked = m_connectBarrier.await(CONNECT_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            throw new ConnectionException(ex);
        }

        //if not arrive into certain limit, raise an error
        if (!unlocked) {
            throw new ConnectionException("Connection timeout elapsed unless server responded with a CONN_ACK");
        }

        //also raise an error when CONN_ACK is received with some error code inside
        if (m_returnCode != ConnAckMessage.CONNECTION_ACCEPTED) {
            String errMsg;
            switch (m_returnCode) {
                case ConnAckMessage.UNNACEPTABLE_PROTOCOL_VERSION:
                    errMsg = "Unacceptable protocol version";
                    break;
                case ConnAckMessage.IDENTIFIER_REJECTED:
                    errMsg = "Identifier rejected";
                    break;
                case ConnAckMessage.SERVER_UNAVAILABLE:
                    errMsg = "Server unavailable";
                    break;
                case ConnAckMessage.BAD_USERNAME_OR_PASSWORD:
                    errMsg = "Bad username or password";
                    break;
                case ConnAckMessage.NOT_AUTHORIZED:
                    errMsg = "Not authorized";
                    break;
                default:
                    errMsg = "Not idetified erro code " + m_returnCode;
                return false;
            }
            throw new ConnectionException(errMsg);
        }else{      	
            if(isPush)
            	updatePinger();
            else
            	updatePoller();           
        	return true;
        }

    }
    /**
     * In the current pinger is not ye executed, then cancel it and schedule
     * another by KEEPALIVE_SECS
     */
    private void updatePinger() {
        if (m_pingerHandler != null) {
            m_pingerHandler.cancel(false);
        }
        m_pingerHandler = m_scheduler.scheduleWithFixedDelay(pingerDeamon, KEEPALIVE_SECS, KEEPALIVE_SECS, TimeUnit.SECONDS);
    }

    private void updatePoller(){
        if (m_pingerHandler != null) {
            m_pingerHandler.cancel(false);
        }
        m_pingerHandler = m_scheduler.scheduleWithFixedDelay(pollerDeamon, POLL_SECS, POLL_SECS, TimeUnit.SECONDS);
    }
    
    public void disconnect(){
    	m_session.write(new DisconnectMessage());
    }
    
    public void switchMode(){
    	if(isPush)
    	{
    		disconnect();
    		updatePoller();
    		status = "POLL";
    	}else{
    		if(!m_session.isConnected())
    			connect();
    		updatePinger();
    		status = "PUSH";
    	}
    	isPush = !isPush;
    }
    
    private String readMACAddress() {//mac地址读取不成功
        try {
            NetworkInterface network = NetworkInterface.getNetworkInterfaces().nextElement();
            
            byte[] mac = network.getHardwareAddress();

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mac.length; i++) {
                sb.append(String.format("%02X%s", mac[i], ""));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new LSNException("Can't retrieve host MAC address", ex);
        }
    }
    
    private String readIPAddress() {
        try {
            NetworkInterface network = NetworkInterface.getNetworkInterfaces().nextElement();
            
            byte[] ip = network.getInetAddresses().nextElement().getAddress();

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < ip.length; i++) {
                sb.append(String.format("%02X%s", ip[i], ""));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new LSNException("Can't retrieve host ip address", ex);
        }
    }
    
    private String generateClientID() {//因为需要在服务器储存用户消息  故不需要每次都才去随机ID  随机ID为在单独一台电脑内测试多个客户端
        double rnd = Math.random();
        String id =  "LSN" + m_ipAddress + Math.round(rnd*1000);
        LOG.debug("Generated ClientID " + id);
        return id;
    }

    public void close() {
        //stop the pinger
    	if(m_pingerHandler != null)
    		m_pingerHandler.cancel(false);

        //send the CLOSE message
        // wait until the summation is done
    	if(m_session != null){
    		m_session.write(new DisconnectMessage());
    		m_session.getCloseFuture().awaitUninterruptibly();	
    	}
    }
    
    
    public void shutdown() {
        m_connector.dispose();
    }
    
    private void poll() throws PollException {
        poll(false);
    }
    /**
     * Poll by default with QoS 0
     * */
    private void poll(boolean retain) throws PollException {
        poll(AbstractMessage.QOSType.LEAST_ONE, retain);
    }
    private void poll(AbstractMessage.QOSType qos, boolean retain) throws PollException {
    	
    	connect(false);//每次poll都需要建立短连接
    	
        PollMessage msg = new PollMessage();
        msg.setRetainFlag(retain);

        if (qos != AbstractMessage.QOSType.MOST_ONE) {
            msg.setQos(AbstractMessage.QOSType.LEAST_ONE);
            int messageID = m_messageIDGenerator.next();
            msg.setMessageID(messageID);

            try {
                manageSendQoS1(msg);
            } catch (Throwable ex) {
                throw new LSNException(ex);
            }
            
        } else {
            //QoS 0 case
            msg.setQos(AbstractMessage.QOSType.MOST_ONE);
            WriteFuture wf = m_session.write(msg);
            try {
                wf.await();
            } catch (InterruptedException ex) {
                LOG.debug(null, ex);
                throw new PollException(ex);
            }

            Throwable ex = wf.getException();
            if (ex != null) {
                throw new PollException(ex);
            }
        }

        disconnect();
    }
    
    private void manageSendQoS1(MessageIDMessage msg) throws Throwable{
        int messageID = msg.getMessageID();
        boolean unlocked = false;
        for (int retries = 0; retries < RETRIES_QOS_GT0 && !unlocked; retries++) {//为什么是 “或” 而不是 “与”
            LOG.debug("manageSendQoS1 retry " + retries);
            if (retries > 0) {
                msg.setDupFlag(true);
            }

            WriteFuture wf = m_session.write(msg);
            //wf.addListener(IoFutureListener.CLOSE);短连接
            wf.await();
            LOG.info("message sent");

            Throwable ex = wf.getException();
            if (ex != null) {
                throw ex;
            }

            //等待pollresp
            m_pollBarrier = new CountDownLatch(1);

            //挂起线程等待resp
            LOG.info("POLL waiting for POLLRESP");
            unlocked = m_pollBarrier.await(POLLRESP_TIMEOUT, TimeUnit.MILLISECONDS); 
        }

        //超时
        if (!unlocked) {
            throw new LSNException(String.format("Server doesn't replyed with a POLLRESP after %d replies", RETRIES_QOS_GT0));
        } else {
            //检查ID
            if (m_receivedPollRespMessageID != messageID) {
                throw new LSNException(String.format("Server replyed with "
                + "a broken MessageID in POLLRESP, expected %d but received %d", 
                messageID, m_receivedPollRespMessageID));
            }
        }
    }

    protected void connectionAckCallback(byte returnCode) {
        LOG.info("connectionAckCallback invoked");
        m_returnCode = returnCode;
        m_connectBarrier.countDown();
    }
    
	public void pushCallback(String topicName, byte[] payload, Integer messageID) {
		/*
		 * push控制台输出显示 for test
		 * */
		String msg = new String();;
		try {
			msg = "topic:"+topicName+"\n"+"payload:"+new String(payload, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		/*
		 * 自定义方式显示
		 * */
		dealPushed(topicName, payload);
		
		if (messageID == null) //qos == 0  不需要发送ack确认
			return;
		/*
		 * 发送pushACK
		 * */
		PushAckMessage pushAckMessage = new PushAckMessage();		
		pushAckMessage.setMessageID(messageID);
		
        WriteFuture wf = m_session.write(pushAckMessage);
        
        //await 互相等待   会造成IO死锁
//        try {
//            wf.await();
//        } catch (InterruptedException ex) {
//            LOG.info(null, ex);
//            throw new PushException(ex);
//        }

        Throwable ex = wf.getException();
        if (ex != null) {
            throw new PushException(ex);
        }
        
//        updatePinger();//需要吗？
	}
	
	public void pollRespCallback(String topicName, byte[] payload, Integer messageID) {
		LOG.info("pollRespCallback invoked");
		/*
		 * poll输出显示for test
		 * */
		String msg = new String();;
		try {
			msg = "topic:"+topicName+"\n"+"payload:"+new String(payload, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		dealPollResp(topicName, payload);
		m_receivedPollRespMessageID = messageID;
		m_pollBarrier.countDown();
	}
	
    public String getUserName() {
		return userName;
	}
	public void setUserName(String usrName) {
		this.userName = usrName;
	}
	public String getPassWord() {
		return passWord;
	}
	public void setPassWord(String passWord) {
		this.passWord = passWord;
	}
	public String getStatus() {
		return status;
	}
    public String getHostname() {
		return m_hostname;
	}
	public void setHostname(String m_hostname) {
		this.m_hostname = m_hostname;
	}
}
