package codec;

import messages.AbstractMessage;
import messages.ConnAckMessage;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.filter.codec.demux.MessageEncoder;

import util.Utils;

public class ConnAckEncoder implements MessageEncoder<ConnAckMessage>{
	
    public void encode(IoSession session, ConnAckMessage message, ProtocolEncoderOutput out) throws Exception {
        IoBuffer buff = IoBuffer.allocate(4);
        buff.put((byte) (AbstractMessage.CONNACK << 4));//标志位为空
        buff.put(Utils.encodeRemainingLength(2));//长度
        buff.put((byte) 0);//保留字节
        buff.put(message.getReturnCode()).flip();//连接返回值
        out.write(buff);
    }
}
