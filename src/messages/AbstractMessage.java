package messages;

public abstract class AbstractMessage {
	public static final byte CONNECT = 1;
	public static final byte CONNACK = 2;
	public static final byte POLL = 3;
	public static final byte POLLRESP = 4;
	public static final byte PUSH = 5;
	public static final byte PUSHACK = 6;
	public static final byte PINGREQ = 7;
	public static final byte PINGRESP = 8;
	public static final byte DISCONNECT = 9;

	public static enum QOSType {

		MOST_ONE, LEAST_ONE, EXACTLY_ONCE, RESERVED;
	}

	protected boolean m_dupFlag;
	protected QOSType m_qos;
	protected boolean m_retainFlag;
	protected int m_remainingLength;
	protected byte m_messageType;

	public byte getMessageType() {
		return m_messageType;
	}

	public void setMessageType(byte messageType) {
		this.m_messageType = messageType;
	}

	public boolean isDupFlag() {
		return m_dupFlag;
	}

	public void setDupFlag(boolean dupFlag) {
		this.m_dupFlag = dupFlag;
	}

	public QOSType getQos() {
		return m_qos;
	}

	public void setQos(QOSType qos) {// message.setQos(AbstractMessage.QOSType.values()[qosLevel]);
		this.m_qos = qos;
	}

	public boolean isRetainFlag() {
		return m_retainFlag;
	}

	public void setRetainFlag(boolean retainFlag) {
		this.m_retainFlag = retainFlag;
	}

	public int getRemainingLength() {
		return m_remainingLength;
	}

	public void setRemainingLength(int remainingLength) {
		this.m_remainingLength = remainingLength;
	}
}
