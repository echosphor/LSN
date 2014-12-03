package codec;

import messages.AbstractMessage;
import messages.ConnectMessage;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.filter.codec.demux.MessageEncoder;

import util.Utils;

public class ConnectEncoder implements MessageEncoder<ConnectMessage> {

	@Override
	public void encode(IoSession session, ConnectMessage message, ProtocolEncoderOutput out) throws Exception {
		
        IoBuffer staticHeaderBuff = IoBuffer.allocate(10);
        staticHeaderBuff.put(Utils.encodeString("LSNP"));
        
        //协议版本
        staticHeaderBuff.put((byte)0x01);
        
        //连接标志
        byte connectionFlags = 0;
        if (message.isCleanSession()) {
            connectionFlags |= 0x02;
        }
        if (message.isWillFlag()) {
            connectionFlags |= 0x04;
        }
        connectionFlags |= ((message.getWillQos() & 0x03) << 3);
        if (message.isWillRetain()) {
            connectionFlags |= 0x020;
        }
        if (message.isPasswordFlag()) {
            connectionFlags |= 0x040;
        }
        if (message.isUserFlag()) {
            connectionFlags |= 0x080;
        }
        staticHeaderBuff.put(connectionFlags);
        
        //心跳计时器
        Utils.writeWord(staticHeaderBuff, message.getKeepAlive());
        staticHeaderBuff.flip();
        
        //连接消息头的可变部分
        IoBuffer variableHeaderBuff = IoBuffer.allocate(10).setAutoExpand(true);
        if (message.getClientID() != null) {
            variableHeaderBuff.put(Utils.encodeString(message.getClientID()));
            if (message.isUserFlag() && message.getUsername() != null) {
                variableHeaderBuff.put(Utils.encodeString(message.getUsername()));
                if (message.isPasswordFlag() && message.getPassword() != null) {
                    variableHeaderBuff.put(Utils.encodeString(message.getPassword()));
                }
            }
        }
        variableHeaderBuff.flip();
        
        int variableHeaderSize = variableHeaderBuff.remaining();
        IoBuffer buff = IoBuffer.allocate(10 + 2 + variableHeaderSize);//2字节的固定消息头，
        buff.put((byte) (AbstractMessage.CONNECT << 4));//消息类型+标记位（空）
        buff.put(Utils.encodeRemainingLength(10 + variableHeaderSize));//之后的长度 remain length
        buff.put(staticHeaderBuff).put(variableHeaderBuff).flip();

        out.write(buff);
        
	}

}
