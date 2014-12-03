package client;

import messages.AbstractMessage;
import messages.ConnAckMessage;
import messages.PollRespMessage;
import messages.PushMessage;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.Utils;

public class ClientLSNHandler extends IoHandlerAdapter {
	private static final Logger LOG = LoggerFactory.getLogger(ClientLSNHandler.class);
	
    Client m_callback;

    ClientLSNHandler(Client callback)  {
        m_callback = callback;
    } 
    
    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        AbstractMessage msg = (AbstractMessage) message;
        LOG.info("Received a message of type " + Utils.msgType2String(msg.getMessageType()));
        switch (msg.getMessageType()) {
            case AbstractMessage.CONNACK:
                handleConnectAck(session, (ConnAckMessage) msg);
                break;
            case AbstractMessage.PUSH:
                handlePush(session, (PushMessage) msg);
                break;
            case AbstractMessage.POLLRESP:
                handlePollResp(session, (PollRespMessage) msg);
                break;
            case AbstractMessage.PINGRESP:    
                break;
        }
    }

	private void handleConnectAck(IoSession session, ConnAckMessage connAckMessage) {
		m_callback.connectionAckCallback(connAckMessage.getReturnCode());	
	}
	
	private void handlePush(IoSession session, PushMessage pushMessage) {
		m_callback.pushCallback(pushMessage.getTopicName(), pushMessage.getPayload(),pushMessage.getMessageID());
	}
	
	private void handlePollResp(IoSession session, PollRespMessage pollRespMessage) {
		m_callback.pollRespCallback(pollRespMessage.getTopicName(), pollRespMessage.getPayload(),pollRespMessage.getMessageID());
	}
}
