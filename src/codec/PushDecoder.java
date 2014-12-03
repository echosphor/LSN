package codec;

import messages.AbstractMessage;
import messages.AbstractMessage.QOSType;
import messages.PushMessage;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.demux.MessageDecoderResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.Utils;

public class PushDecoder extends LSNDecoder{

	private static Logger LOG = LoggerFactory.getLogger(PushDecoder.class);
	
	@Override
	public MessageDecoderResult decodable(IoSession session, IoBuffer in) {
		return Utils.checkDecodable(AbstractMessage.PUSH, in);
	}

	@Override
	public MessageDecoderResult decode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
        LOG.debug("decode invoked with buffer " + in);
        int startPos = in.position();

        //固定头
        PushMessage message = new PushMessage();
        if (decodeCommonHeader(message, in) == NEED_DATA) {
            LOG.info("PUSH-需要更多数据才能解码 " + in);
            return NEED_DATA;
        }
        int remainingLength = message.getRemainingLength();
        
        //消息话题内容
        String topic = Utils.decodeString(in);
        if (topic == null) {
            return NEED_DATA;
        }
        message.setTopicName(topic);
        
        if (message.getQos() == QOSType.LEAST_ONE || 
                message.getQos() == QOSType.EXACTLY_ONCE) {
            message.setMessageID(Utils.readWord(in));
        }
        int stopPos = in.position();
        
        //读取消息具体内容
        //消息内容长度=剩余长度-可变头长度+表示内容长度的额外字节
        int payloadSize = remainingLength - (stopPos - startPos - 2) + (Utils.numBytesToEncode(remainingLength) - 1);
        if (in.remaining() < payloadSize) {
            return NEED_DATA;
        }
        byte[] b = new byte[payloadSize];
        in.get(b);
        message.setPayload(Utils.UnCompress(b));
        
        out.write(message);
        return OK;
	}

}
