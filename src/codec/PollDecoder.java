package codec;

import messages.AbstractMessage;
import messages.MessageIDMessage;
import messages.PollMessage;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.demux.MessageDecoderResult;

import util.Utils;

public class PollDecoder extends MessageIDDecoder {
	
	@Override
	public MessageDecoderResult decodable(IoSession session, IoBuffer in) {
		return Utils.checkDecodable(AbstractMessage.POLL, in);
	}

	@Override
	protected MessageIDMessage createMessage() {
		// TODO Auto-generated method stub
		return new PollMessage();
	}

}
