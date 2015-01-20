package server;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import messages.AbstractMessage;
import messages.MessageIDMessage;
import messages.PushMessage;

import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import exception.LSNException;
import exception.PushException;

class PushRunner extends Thread {

    final static int RETRIES_QOS_GT0 = 3;
	private static final long PUSHACK_TIMEOUT = 4 * 1000L;
	
	private static final Logger LOG = LoggerFactory.getLogger(PushRunner.class);
	
    private CountDownLatch m_pushBarrier;
    private int m_receivedPushAckMessageID;
    private IoSession m_session;
    private AbstractMessage.QOSType qos;
    private boolean retain;
    private String topic;
	private String payload;
    
    public PushRunner(IoSession session, AbstractMessage.QOSType qos, boolean retain,int MessageID){
    	m_session = session;
    	this.qos = qos;
    	this.retain = retain;
    	this.m_receivedPushAckMessageID = MessageID;
    }
	@Override
	public void run() {
		doPush();
	}
	
	public void doPush(){
		
        PushMessage msg = new PushMessage();
        msg.setRetainFlag(retain);
        msg.setTopicName(topic);
        msg.setPayload(payload.getBytes());

        if (qos != AbstractMessage.QOSType.MOST_ONE) {
            msg.setQos(AbstractMessage.QOSType.LEAST_ONE);
            msg.setMessageID(m_receivedPushAckMessageID);

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
                throw new PushException(ex);
            }

            Throwable ex = wf.getException();
            if (ex != null) {
                throw new PushException(ex);
            }
        }
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
            wf.await();//同一session  阻塞  互相等待  防止IO死锁
            LOG.info("message sent");

            Throwable ex = wf.getException();
            if (ex != null) {
                throw ex;
            }

            //wait for the pollresp
            m_pushBarrier = new CountDownLatch(1);

            //suspend until the server respond with POLLRESP
            LOG.info("PUSH waiting for PUSHACK");
            unlocked = m_pushBarrier.await(PUSHACK_TIMEOUT, TimeUnit.MILLISECONDS); //线程等待响应
        }
        //if not arrive into certain limit, raise an error
        if (!unlocked) {
            throw new LSNException(String.format("Server doesn't replyed with a PUSHACK after %d replies", RETRIES_QOS_GT0));
        } else {
            //check if message ID match
            if (m_receivedPushAckMessageID != messageID) {
                throw new LSNException(String.format("Server replyed with "
                + "a broken MessageID in PUSHACK, expected %d but received %d", 
                messageID, m_receivedPushAckMessageID));
            }
        }
    }
    
	public CountDownLatch getPushBarrier() {
		return m_pushBarrier;
	}
	public void setPushBarrier(CountDownLatch m_pushBarrier) {
		this.m_pushBarrier = m_pushBarrier;
	}
	public int getReceivedPushAckMessageID() {
		return m_receivedPushAckMessageID;
	}
	public void setReceivedPushAckMessageID(int m_receivedPushAckMessageID) {
		this.m_receivedPushAckMessageID = m_receivedPushAckMessageID;
	}	
    public String getTopic() {
		return topic;
	}
	public void setTopic(String topic) {
		this.topic = topic;
	}
	public String getPayload() {
		return payload;
	}
	public void setPayload(String payload) {
		this.payload = payload;
	}
	
}