package Test;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import messages.AbstractMessage;

import server.ClientMessages;
import server.Server;
import client.Client;


public class test {
	
	private static final Logger LOG = LoggerFactory.getLogger(test.class);

	public static void main(String[] args) throws IOException {
		ClientMessages clientMsgGetter = ClientMessages.getInstance();
		clientMsgGetter.addClientMessage("LSN7F000001", "1", "1111");
		clientMsgGetter.addClientMessage("LSN7F000001", "2", "2222");
		clientMsgGetter.addClientMessage("LSN7F000001", "3", "3333");

		Server server = new Server(){

			@Override
			protected void dealConnected(String clientID) {
				//连接入服务器的客户ID
				LOG.info("client "+clientID+" connected.\n");
			}

			@Override
			protected void dealDisConnected(String clientID) {
				//断开服务器的客户ID
				LOG.info("client "+clientID+" disconnected.\n");
			}
			
		};
		
		server.startServer();		
		//server.push("LSN7F000001",AbstractMessage.QOSType.LEAST_ONE,false);
	}

}
