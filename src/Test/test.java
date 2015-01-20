package Test;

import java.io.IOException;

import messages.AbstractMessage;

import server.ClientMessages;
import server.Server;
import client.Client;


public class test {

	public static void main(String[] args) throws IOException {
		ClientMessages clientMsgGetter = ClientMessages.getInstance();
		clientMsgGetter.addClientMessage("LSN7F000001", "1", "1111");
		clientMsgGetter.addClientMessage("LSN7F000001", "2", "2222");
		clientMsgGetter.addClientMessage("LSN7F000001", "3", "3333");

		Server server = new Server(){

			@Override
			protected void dealConnected(String clientID) {
				//连接入服务器的客户ID
				
			}

			@Override
			protected void dealDisConnected(String clientID) {
				//断开服务器的客户ID
			}
			
		};
		
		server.startServer();
		
		Client client = new Client("localhost",9898,"sss","sss",true){

			@Override
			protected void dealPushed(String topicName, byte[] payload) {
				// PUSH的消息内容
				
			}

			@Override
			protected void dealPollResp(String topicName, byte[] payload) {
				// POLLRESP的消息内容
				
			}
			
		};
		client.connect(false);
		
		server.push("LSN7F000001",AbstractMessage.QOSType.LEAST_ONE,false);
	}

}
