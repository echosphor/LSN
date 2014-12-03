package codec;

import messages.MessageIDMessage;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.demux.MessageDecoderResult;

import util.Utils;

/*与QOS有关，POLL/PUSH确认送达时需要获取对应消息ID*/

public abstract class MessageIDDecoder extends LSNDecoder {
	
    protected abstract MessageIDMessage createMessage();
    
    @Override
    public MessageDecoderResult decode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
        //固定头
        MessageIDMessage message = createMessage();
        if (decodeCommonHeader(message, in) == NEED_DATA) {
            return NEED_DATA;
        }
        
        //消息ID
        message.setMessageID(Utils.readWord(in));
        out.write(message);
        return OK;
    }

}
