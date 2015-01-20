package server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import messages.AbstractMessage;
import messages.ConnAckMessage;
import messages.ConnectMessage;
import messages.PollRespMessage;

import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import beans.Smessage;

import util.MessageIDGenerator;

import commons.Constants;

public class HandleMessage{
	
	private static HandleMessage INSTANCE;
	
	private static final Logger LOG = LoggerFactory.getLogger(HandleMessage.class);
	Map<String, ConnectionDescriptor> m_clientIDs = new HashMap<String, ConnectionDescriptor>();
	
	private Server server;
	
	private IAuthenticator m_authenticator = new Authenticator();
	
    private MessageIDGenerator m_messageIDGenerator = new MessageIDGenerator();
    
	
	//管理多个client的push过程
//    private Map<String,CountDownLatch> m_pushBarriers;
//    private Map<String,Integer> m_receivedPushAckMessageIDs;
	Map<Integer,PushRunner> pushers;
	
	ClientMessages clientMsgGetter = ClientMessages.getInstance();
	
    private HandleMessage() {
    	pushers = new HashMap<Integer,PushRunner>();
    }

    public static HandleMessage getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new HandleMessage();
        }
        return INSTANCE;
    }
    
    public void processConnect(IoSession session, ConnectMessage message) {
        if (message.getProcotolVersion() != 0x01) {
            ConnAckMessage badProto = new ConnAckMessage();
            badProto.setReturnCode(ConnAckMessage.UNNACEPTABLE_PROTOCOL_VERSION);
            session.write(badProto);
            session.close(false);
            return;
        }

        if (message.getClientID() == null || message.getClientID().length() > 23) {
            ConnAckMessage okResp = new ConnAckMessage();
            okResp.setReturnCode(ConnAckMessage.IDENTIFIER_REJECTED);
            session.write(okResp);
            return;
        }
        
        if(m_clientIDs.containsKey(message.getClientID())){//如果相同的ID，则关闭以前的连接
        	m_clientIDs.get(message.getClientID()).getSession().close(true).awaitUninterruptibly();
        	m_clientIDs.remove(message.getClientID());
        }
        
        ConnectionDescriptor connDescr = new ConnectionDescriptor(message.getClientID(), session, message.isCleanSession());
        m_clientIDs.put(message.getClientID(), connDescr);

        int keepAlive = message.getKeepAlive();
        LOG.debug(String.format("Connect with keepAlive %d s",  keepAlive));
        session.setAttribute("keepAlive", keepAlive);
        session.setAttribute(Constants.CLEAN_SESSION, message.isCleanSession());
        //session属性用来标记客户ID 
        session.setAttribute(Constants.ATTR_CLIENTID, message.getClientID());

        session.getConfig().setIdleTime(IdleStatus.READER_IDLE, Math.round(keepAlive * 1.5f));

        //用户验证
        if (message.isUserFlag()) {
            String pwd = null;
            if (message.isPasswordFlag()) {
                pwd = message.getPassword();
            }
            if (!m_authenticator.checkValid(message.getUsername(), pwd)) {
                ConnAckMessage okResp = new ConnAckMessage();
                okResp.setReturnCode(ConnAckMessage.BAD_USERNAME_OR_PASSWORD);
                session.write(okResp);
                return;
            }
        }

        ConnAckMessage okResp = new ConnAckMessage();
        okResp.setReturnCode(ConnAckMessage.CONNECTION_ACCEPTED);
        session.write(okResp);
        if(server != null)
        	server.dealConnect(message.getClientID());
    }
    
    public void processPoll(IoSession session, Integer messageID){//TODO 需要实现从服务器获取用户消息
    	String clientID = (String) session.getAttribute(Constants.ATTR_CLIENTID);
    	
    	Smessage msg = clientMsgGetter.getClientMessage(clientID);
    	if(msg== null)
    	{
    		msg = new Smessage("哎呀","已经没有消息啦！");
    	}
    	
        PollRespMessage pollResp = new PollRespMessage();
        pollResp.setTopicName(msg.getTopic());
        pollResp.setPayload(msg.getPayload().getBytes());
        pollResp.setMessageID(messageID);
        session.write(pollResp);
    }
    
    public void processPushAck(IoSession session, Integer messageID){//TODO 需要分别处理不同的ack
    	PushRunner pusher = pushers.get(messageID);
    	pusher.setReceivedPushAckMessageID(messageID);
    	pusher.getPushBarrier().countDown();
    }
    
    public void processDisconnect(IoSession session) throws InterruptedException {
        String clientID = (String) session.getAttribute(Constants.ATTR_CLIENTID);
        
    	m_clientIDs.remove(clientID);
    	server.dealDisconnect(clientID);
    	
    	session.close(true);
    }
	
	public void push(String clientID){
		push(clientID,false);
	}
    public void push(String clientID, boolean retain){
    	push(clientID,AbstractMessage.QOSType.MOST_ONE,retain);
    }
    public void push(String clientID, AbstractMessage.QOSType qos, boolean retain){//向多个client push 如何在ack中控制 DONE 
    	//管理多个client的push过程
    	ConnectionDescriptor conDescriptor = m_clientIDs.get(clientID);
    	if(conDescriptor == null)
    		return;
    	if(conDescriptor.getSession() == null){//客户端意外断开连接
    		m_clientIDs.remove(clientID);
    	}
    	
    	Smessage msg = clientMsgGetter.getClientMessage(clientID);
    	if(msg== null)
    	{
    		msg = new Smessage("哎呀","已经没有消息啦！");//没有信息了 
    	}	
    	PushRunner gopush;	
    	int msgID = m_messageIDGenerator.next();
    	gopush = new PushRunner(conDescriptor.getSession(), qos,retain,msgID);
    	gopush.setTopic(msg.getTopic());
    	gopush.setPayload(msg.getPayload());
    	pushers.put(msgID, gopush);
    	gopush.start();   	
    }
    
    public List<String> getClientIDs(){
    	ArrayList<String> ret = new ArrayList<String>();
    	Iterator it = m_clientIDs.entrySet().iterator();
    	if(it.hasNext()){
    		Map.Entry me = (Map.Entry) it.next();
    		ret.add((String) me.getKey());
    	}
		return ret;
    }
    
    public void setServer(Server server){
    	this.server = server;
    }
}
