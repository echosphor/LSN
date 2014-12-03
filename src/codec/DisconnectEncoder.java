package codec;

import messages.AbstractMessage;
import messages.DisconnectMessage;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.filter.codec.demux.MessageEncoder;

public class DisconnectEncoder implements MessageEncoder<DisconnectMessage> {

	@Override
    public void encode(IoSession session, DisconnectMessage message, ProtocolEncoderOutput out) throws Exception {
        IoBuffer buff = IoBuffer.allocate(2);
        buff.put((byte) (AbstractMessage.DISCONNECT << 4)).put((byte)0).flip();//remain length为0
        out.write(buff);
    }
    
}
