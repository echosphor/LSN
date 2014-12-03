package codec;

import messages.AbstractMessage;
import messages.ConnAckMessage;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.demux.MessageDecoderResult;

import util.Utils;

public class ConnAckDecoder extends LSNDecoder {

	@Override
    public MessageDecoderResult decodable(IoSession session, IoBuffer in) {
        return Utils.checkDecodable(AbstractMessage.CONNACK, in);
    }

	@Override
    public MessageDecoderResult decode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
        //固定头
        ConnAckMessage message = new ConnAckMessage();
        if (decodeCommonHeader(message, in) == NEED_DATA) {
            return NEED_DATA;
        }
        //保留字节
        in.skip(1);
        
        //连接返回值
        message.setReturnCode(in.get());
        out.write(message);
        return OK;
    }
}
