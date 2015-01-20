package server;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import db.ConnectionFactory;

public class Authenticator implements IAuthenticator {
	
	private ConnectionFactory factory = ConnectionFactory.getInstatnce();
	private Connection connection = null;
	private Statement statement = null;
	private ResultSet rs = null;
	
	
	@Override
	@SuppressWarnings("finally")
	public boolean checkValid(String username, String password) {	
		boolean flag = false;
		try {

			connection = factory.getConnection();
			statement = connection.createStatement();
			rs = statement.executeQuery("select * from user where username='"+ username + "' and password='"+ password + "' limit 1;");
			if (rs.next()) {
				// 用户存在
				String dbpassword = rs.getString(2);
				if (dbpassword.equals(password))
					flag = true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			this.close();
			return flag;
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

}
