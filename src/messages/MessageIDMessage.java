package messages;

/*与QOS有关：需要确认ID对应的POLL/PUSH消息是否发送成功*/
public class MessageIDMessage extends AbstractMessage {
    private Integer m_messageID; //Qos为0时，可以为null

    public Integer getMessageID() {
        return m_messageID;
    }

    public void setMessageID(Integer messageID) {
        this.m_messageID = messageID;
    }

}
