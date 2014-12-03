package db;

import java.sql.*;

public interface BuildConnection {
	// 从数据库连接池中得到数据库连接池	
	public Connection getConnection() throws SQLException;
	
    //释放数据库连接到数据库连接池	
	public void freeConnection(Connection conn) throws SQLException;

	//得到数据库连接池中的连接数量
	public int getNumberConnections() throws SQLException;

}
