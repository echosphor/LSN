package messages;

public class PollRespMessage extends MessageIDMessage {
	private String m_topicName;
    private byte[] m_payload;//具体消息内容
	
    public String getTopicName() {
        return m_topicName;
    }

    public void setTopicName(String topicName) {
        this.m_topicName = topicName;
    }
    public byte[] getPayload() {
        return m_payload;
    }

    public void setPayload(byte[] payload) {
        this.m_payload = payload;
    }

}
