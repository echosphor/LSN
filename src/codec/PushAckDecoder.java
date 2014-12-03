package codec;

import messages.AbstractMessage;
import messages.MessageIDMessage;
import messages.PushAckMessage;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.demux.MessageDecoderResult;

import util.Utils;

public class PushAckDecoder extends MessageIDDecoder {

	@Override
	public MessageDecoderResult decodable(IoSession session, IoBuffer in) {
		return Utils.checkDecodable(AbstractMessage.PUSHACK, in);
	}

	@Override
	protected MessageIDMessage createMessage() {
		return new PushAckMessage();
	}

}
