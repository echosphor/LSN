package db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;
import com.devdaily.opensource.database.DDConnectionBroker;

public class ConnectionBroker implements BuildConnection {

	private String driver = null;

	private String url = null;

	private String username = null;

	private String password = null;

	private int minConnections = 0;

	private int maxConnections = 0;

	private long timeout = 0;

	private long leaseTime = 0;

	private String logFile = null;

	private DDConnectionBroker broker = null;

	void setUp() {
		driver = "com.mysql.jdbc.Driver";
		url = "jdbc:mysql://localhost:3306/lsndata?useUnicode=true&amp;characterEncoding=utf8";
		username = "root";
		password = "";
		minConnections = 2;
		maxConnections = 5;
		timeout = 100;
		leaseTime = 60000;
		logFile = "DDConnectionBroker.log";
		broker = null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see connectionbroker.database.BuildConnection#getConnection()
	 */
	public Connection getConnection() throws SQLException {
		try {
			// construct the broker
			broker = new DDConnectionBroker(driver, url, username, password,
					minConnections, maxConnections, timeout, leaseTime, logFile);

		} catch (SQLException se) {
			// could not get a broker; not much reason to go on
			System.out.println(se.getMessage());
			System.out.println("Could not construct a broker, quitting.");

		}
		return broker.getConnection();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see connectionbroker.database.BuildConnection#freeConnection()
	 */
	public void freeConnection(Connection conn) throws SQLException {
		try {
			broker.freeConnection(conn);
		} catch (Exception e) {
			System.out
					.println("Threw an exception trying to free my Connection: "
							+ e.getMessage());
		}
	}

	public int getNumberConnections() throws SQLException {
		if (broker != null)
			return broker.getNumberConnections();
		else
			return -1;
	}

	public ConnectionBroker() {
		super();
		setUp();
	}

	// ****************测试数据库连接池是否正常工作*******************************
	public static void main(String[] args) {

		ConnectionBroker connectionBroker = new ConnectionBroker();
		Connection conn = null;
		try {
			conn = connectionBroker.getConnection();
			System.out.println("the number of connection is:"
					+ connectionBroker.getNumberConnections());
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery("SELECT  username, password FROM user");
			while (rs.next()) {

				String theUsername = rs.getString("username");
				String thePassword = rs.getString("password");
				System.out.println("Database record: " + theUsername + ", "
						+ thePassword );
			}

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				connectionBroker.freeConnection(conn);
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			try {
				System.out.println("the number of connection is:"
						+ connectionBroker.getNumberConnections());
			} catch (SQLException e2) {
				e2.printStackTrace();
			}
		}
	}

}
