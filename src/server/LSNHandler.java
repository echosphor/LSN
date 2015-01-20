package server;

import messages.AbstractMessage;
import messages.ConnectMessage;
import messages.PingRespMessage;
import messages.PollMessage;
import messages.PushAckMessage;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.Utils;

public class LSNHandler extends IoHandlerAdapter {

	private static final Logger LOG = LoggerFactory.getLogger(LSNHandler.class);
    
	HandleMessage handleMsg;
	
    public LSNHandler(HandleMessage handleMsg) {
    	this.handleMsg = handleMsg;
	}
    
    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        AbstractMessage msg = (AbstractMessage) message;
        LOG.info(String.format("Received a message of type %s", Utils.msgType2String(msg.getMessageType())));
        try {
            switch (msg.getMessageType()) {
                case AbstractMessage.CONNECT:
                	handleMsg.processConnect(session, (ConnectMessage)message);
                    break;
                case AbstractMessage.POLL:
                	handleMsg.processPoll(session, ((PollMessage)message).getMessageID());//session 中可以获取clientID，在连接阶段定义了属性
                	break;
                case AbstractMessage.PUSHACK:
                	handleMsg.processPushAck(session, ((PushAckMessage)message).getMessageID());
                	break;
                case AbstractMessage.DISCONNECT:
                    //.handleProtocolMessage(session, msg);
                	handleMsg.processDisconnect(session);      	
                    break;
                case AbstractMessage.PINGREQ:
                    PingRespMessage pingResp = new PingRespMessage();
                    session.write(pingResp);
                    break;
            }
        } catch (Exception ex) {
            LOG.error("Bad error in processing the message", ex);
        }
    }

    @Override
    public void sessionIdle(IoSession session, IdleStatus status) {
        if (status == IdleStatus.READER_IDLE) {
            session.close(false);
            //send a notification to messaging part to remove the bining clientID-ConnConfig
        }
    }
}
