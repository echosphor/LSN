package codec;

import messages.AbstractMessage;
import messages.PushAckMessage;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.filter.codec.demux.MessageEncoder;

import util.Utils;

public class PushAckEncoder implements MessageEncoder<PushAckMessage> {
	@Override
    public void encode(IoSession session, PushAckMessage message, ProtocolEncoderOutput out) throws Exception {
        IoBuffer buff = IoBuffer.allocate(4);
        buff.put((byte) (AbstractMessage.PUSHACK << 4));//无标志位
        buff.put(Utils.encodeRemainingLength(2));
        Utils.writeWord(buff, message.getMessageID());
        buff.flip();
        out.write(buff);
    }
}
