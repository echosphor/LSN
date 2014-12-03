package db;

import java.sql.*;

public class ConnectionFactory {

	private BuildConnection build = null;
	private static ConnectionFactory instance;

	public BuildConnection getBuild() {
		return build;
	}

	public void setBuild(BuildConnection build) {
		this.build = build;
	}

	private ConnectionFactory() {
		this.build = new ConnectionBroker();
	}

	// 单态模式，确保只使用一个ConnectionFactory的实例
	synchronized public static ConnectionFactory getInstatnce() {
		if (instance == null) {
			instance = new ConnectionFactory();
		}
		return instance;
	}

	public Connection getConnection() throws SQLException {
		return build.getConnection();
	}

	public void freeConnection(Connection conn) {
		try {
			build.freeConnection(conn);
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println(e.toString());
		}
	}

	public int getNumbersConnection() {
		try {
			return build.getNumberConnections();
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println(e.toString());
		}
		return -1;
	}

	// 演示如何使用ConnectionFactory
	/*public static void main(String[] args) {
		ConnectionFactory factory = ConnectionFactory.getInstatnce();
		Connection conn = null;
		try {
			conn = factory.getConnection();
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		Statement statement = null;
		if (conn == null)
			System.out.println("null");
		try {
			statement = conn.createStatement();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(factory.getNumbersConnection());
	}*/
}
