package server;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import beans.Smessage;

public class ClientMessages {
	
	private static ClientMessages INSTANCE;
	
	Map<String, LinkedList<Smessage>> clientsMsgs = new HashMap<String,LinkedList<Smessage>>(); 
	
	private ClientMessages(){	
	}
	
	public static ClientMessages getInstance(){
        if (INSTANCE == null) {
            INSTANCE = new ClientMessages();
        }
        return INSTANCE;
	}
	
	public void addClientMessage(String clientID,String topic,String payload) {
		if(clientsMsgs.containsKey(clientID)){
			clientsMsgs.get(clientID).add(new Smessage(topic,payload));
		} else {
			LinkedList<Smessage> list = new LinkedList<Smessage>();
			list.addFirst(new Smessage(topic,payload));
			clientsMsgs.put(clientID, list);
		}
	}
	
	public Smessage getClientMessage(String clientID){
		if(clientsMsgs.containsKey(clientID)){
			LinkedList<Smessage> msglist = clientsMsgs.get(clientID);
			return msglist.pollLast();
		}
		return null;
	}
	
	public LinkedList<Smessage> getClientAllMsg(String clientID){
		if(clientsMsgs.isEmpty())
			return null;
		else {
			return clientsMsgs.get(clientID);
		}
	}
}
