package beans;

public class Smessage {
	private String topic;
	private String payload;

	public Smessage(String topic, String payload) {
		this.setTopic(topic);
		this.setPayload(payload);
	}

	public Smessage() {
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public String getPayload() {
		return payload;
	}

	public void setPayload(String payload) {
		this.payload = payload;
	}
	
	public String toString(){
		return "topic:"+topic+" payload:"+payload;
	}
}