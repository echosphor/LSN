package codec;

import messages.AbstractMessage;
import messages.ConnectMessage;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.demux.MessageDecoderResult;

import util.Utils;

public class ConnectDecoder extends LSNDecoder {

	@Override
	public MessageDecoderResult decodable(IoSession session, IoBuffer in) {
		return Utils.checkDecodable(AbstractMessage.CONNECT, in);
	}

	@Override
	public MessageDecoderResult decode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
		//固定头
        ConnectMessage message = new ConnectMessage();
        if (decodeCommonHeader(message, in) == NEED_DATA) {
            return NEED_DATA;
        }
        int remainingLength = message.getRemainingLength();

        //可变头
        //协议名称 4 bytes
        if (in.remaining() < 10) {
            return NEED_DATA;
        }
        byte[] encProtoName = new byte[4];
        in.skip(2); //协议名称长度,0x04 
        in.get(encProtoName);
        String protoName = new String(encProtoName, "UTF-8");
        if (!"LSNP".equals(protoName)) {
            return NOT_OK;
        }
        message.setProtocolName(protoName);

        //协议版本 1,值为0x01
        message.setProcotolVersion(in.get());

        //Connection flag
        byte connFlags = in.get();
        boolean cleanSession = ((connFlags & 0x02) >> 1) == 1 ? true : false;
        boolean willFlag = ((connFlags & 0x04) >> 2) == 1 ? true : false;
        byte willQos = (byte) ((connFlags & 0x18) >> 3);
        if (willQos > 2) {
            return NOT_OK; //QOS值不能超过2
        }
        boolean willRetain = ((connFlags & 0x20) >> 5) == 1 ? true : false;
        boolean passwordFlag = ((connFlags & 0x40) >> 6) == 1 ? true : false;
        boolean userFlag = ((connFlags & 0x80) >> 7) == 1 ? true : false;
        //有密码也要有用户名
        if (!userFlag && passwordFlag) {
            return NOT_OK;
        }
        message.setCleanSession(cleanSession);
        message.setWillFlag(willFlag);
        message.setWillQos(willQos);
        message.setWillRetain(willRetain);
        message.setPasswordFlag(passwordFlag);
        message.setUserFlag(userFlag);

        //心跳计时器  2bytes
        int keepAlive = Utils.readWord(in);
        message.setKeepAlive(keepAlive);

        if (remainingLength == 10) {
            out.write(message);
            return OK;
        }

        //Decode the ClientID
        String clientID = Utils.decodeString(in);
        if (clientID == null) {
            return NEED_DATA;
        }
        message.setClientID(clientID);

        //the user and password flags

        //Decode username
        if (userFlag) {
            String userName = Utils.decodeString(in);
            if (userName == null) {
                return NEED_DATA;
            }
            message.setUsername(userName);
        }

        //Decode password
        if (passwordFlag) {
            String password = Utils.decodeString(in);
            if (password == null) {
                return NEED_DATA;
            }
            message.setPassword(password);
        }

        out.write(message);
        return OK;
	}

}
