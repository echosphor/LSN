package codec;

import messages.AbstractMessage;
import messages.PollMessage;
import messages.AbstractMessage.QOSType;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.filter.codec.demux.MessageEncoder;

import util.Utils;

public class PollEncoder implements MessageEncoder<PollMessage> {

	@Override
	public void encode(IoSession session, PollMessage message, ProtocolEncoderOutput out) throws Exception {
		
        byte flags = Utils.encodeFlags(message);
        
        IoBuffer buff = IoBuffer.allocate(4);
        buff.put((byte) (AbstractMessage.POLL << 4| flags));//需要标志位
        buff.put(Utils.encodeRemainingLength(2));
           
        //messageID
        if (message.getQos() == QOSType.LEAST_ONE || 
                message.getQos() == QOSType.EXACTLY_ONCE ) {
                if (message.getMessageID() == null) {
                    throw new IllegalArgumentException("POLL消息QOS=1或2，未定义消息ID！");
                }
                Utils.writeWord(buff, message.getMessageID());
            }
           
        buff.flip();
        out.write(buff);
	}

}
