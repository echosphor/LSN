package codec;

import messages.AbstractMessage;
import messages.AbstractMessage.QOSType;
import messages.PollRespMessage;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.filter.codec.demux.MessageEncoder;

import util.Utils;

public class PollRespEncoder implements MessageEncoder<PollRespMessage> {

	
	@Override
	public void encode(IoSession session, PollRespMessage message, ProtocolEncoderOutput out) throws Exception {
        if (message.getQos() == QOSType.RESERVED) {
            throw new IllegalArgumentException("POLL消息使用的保留QOS位！");
        }
        if (message.getTopicName() == null || message.getTopicName().isEmpty()) {
            throw new IllegalArgumentException("POLL消息未定义话题名称！");
        }
        
        IoBuffer variableHeaderBuff = IoBuffer.allocate(2).setAutoExpand(true);
        variableHeaderBuff.put(Utils.encodeString(message.getTopicName()));
        
		/*
		* POLLRESP 不需要flags
		* */   
//        if (message.getQos() == QOSType.LEAST_ONE || message.getQos() == QOSType.EXACTLY_ONCE ) {
//            if (message.getMessageID() == null) {
//                throw new IllegalArgumentException("POLL消息QOS=1或2，未定义消息ID！");
//            }
            Utils.writeWord(variableHeaderBuff, message.getMessageID());
//        }
//        
        variableHeaderBuff.put(Utils.Compress(message.getPayload()));
        variableHeaderBuff.flip();
        
        int variableHeaderSize = variableHeaderBuff.remaining();
        
        //byte flags = Utils.encodeFlags(message);
        
        IoBuffer buff = IoBuffer.allocate(2 + variableHeaderSize).setAutoExpand(true);;
        buff.put((byte) (AbstractMessage.POLLRESP << 4 /*| flags*/));
        buff.put(Utils.encodeRemainingLength(variableHeaderSize));
        buff.put(variableHeaderBuff).flip();

        out.write(buff);
	}

}
