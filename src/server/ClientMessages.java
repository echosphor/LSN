package server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.LinkedList;

import db.ConnectionFactory;

import beans.Smessage;

public class ClientMessages {
	
	private ConnectionFactory factory = ConnectionFactory.getInstatnce();
	private Connection connection = null;
	private PreparedStatement pstatement = null;
	private Statement statement = null;
	private ResultSet rs = null;
	
	private static ClientMessages INSTANCE;
	
	private ClientMessages(){//TODO rename to XXXservice	
	}
	
	public static ClientMessages getInstance(){
        if (INSTANCE == null) {
            INSTANCE = new ClientMessages();
        }
        return INSTANCE;
	}
	
	public boolean addClientMessage(String username,String topic,String payload) {		
		boolean flag = false;
		try {
			// 得到数据库连接池中的链接
			connection = factory.getConnection();
			String sql = "insert into messages(username,topic,payload) values ('"
					+ username+ "','"
					+ topic+ "','"
					+ payload + "');";
			pstatement = connection.prepareStatement(sql);
			int status = pstatement.executeUpdate();
			// 添加成功
			if (status == 1) {
				flag = true;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			this.close();
		}
		return flag;
	}
	
	/*
	 *  poll a message form db.(get&delete)
	 */
	@SuppressWarnings("finally")
	public Smessage getClientMessage(String username){		
		Smessage msg = null;
		try {
			connection = factory.getConnection();
			statement = connection.createStatement();
			rs = statement.executeQuery("select * from messages where username='"+ username + "' limit 1;");
			String msgid = null;
			if (rs.next()) {				
				// user message exist 
				msgid = rs.getString(1);
				String topic = rs.getString(3);
				String payload = rs.getString(4);
				msg = new Smessage(topic,payload);
			}
			if(msgid != null){
				deleteMsg(msgid);//need to optimize
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			this.close();
			return msg;
		}
	}
	
	public boolean deleteMsg(String id) {
		boolean flag = false;
		try {
			connection = factory.getConnection();
			pstatement = connection.prepareStatement("delete from messages where id='" + id+ "'");
			int status = pstatement.executeUpdate();
			if (status == 1)
				flag = true;

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			this.close();
		}
		return flag;
	}
	
	@SuppressWarnings("finally")
	public LinkedList<Smessage> getClientAllMsg(String username){
		LinkedList<Smessage> list = null;
		try {
			connection = factory.getConnection();
			statement = connection.createStatement();
			rs = statement.executeQuery("select * from messages where username='"+ username + "' ;");
			list = new LinkedList<Smessage>();
			while (rs.next()) {				
				// user message exist 
				String topic = rs.getString(3);
				String payload = rs.getString(4);
				list.addFirst(new Smessage(topic,payload));
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			this.close();
			return list;//TODO 可能返回空表	
		}
	}
	
	public void close() {
		try {
			if (rs != null)
				rs.close();
			if (statement != null)
				statement.close();
			if (factory != null)
				factory.freeConnection(connection);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("finally")
	public HashSet<String> getAllClientName(){//HashSet maybe faster than List when pick a name from it
		HashSet<String> list = null;
		try {
			connection = factory.getConnection();
			statement = connection.createStatement();
			rs = statement.executeQuery("select username from user ;");
			list = new HashSet<String>();
			while (rs.next()) {				
				// user message exist 
				String name = rs.getString(1);
				list.add(name);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			this.close();
			return list;
		}
	}
}
