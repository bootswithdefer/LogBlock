/**
 * <b>Purpose:</b>Wrapper for JDBCConnectionDriver.<br>
 * <b>Description:</b>http://java.sun.com/developer/onlineTraining/Programming/JDCBook/
 * conpool.html<br>
 * <b>Copyright:</b>Licensed under the Apache License, Version 2.0.
 * http://www.apache.org/licenses/LICENSE-2.0<br>
 * <b>Company:</b> SIMPL<br>
 * 
 * @author schneimi
 * @version $Id: JDCConnectionDriver.java 1224 2010-04-28 14:17:34Z
 *		michael.schneidt@arcor.de $<br>
 * @link http://code.google.com/p/simpl09/
 */

package de.diddiz.util;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

public class ConnectionPool implements Driver {
	public static final String URL_PREFIX = "jdbc:jdc:";
	private static final int MAJOR_VERSION = 1;
	private static final int MINOR_VERSION = 0;
	private ConnectionService pool;

	public ConnectionPool(String driver, String url, String user, String password) throws ClassNotFoundException, InstantiationException, IllegalAccessException, SQLException {
		DriverManager.registerDriver(this);
		Class.forName(driver).newInstance();
		pool = new ConnectionService(url, user, password);
	}

	public Connection connect(String url, Properties props) throws SQLException {
		if (!url.startsWith(URL_PREFIX))
			return null;
		return pool.getConnection();
	}

	public boolean acceptsURL(String url) {
		return url.startsWith(URL_PREFIX);
	}

	public int getMajorVersion() {
		return MAJOR_VERSION;
	}

	public int getMinorVersion() {
		return MINOR_VERSION;
	}

	public DriverPropertyInfo[] getPropertyInfo(String str, Properties props) {
		return new DriverPropertyInfo[0];
	}

	public boolean jdbcCompliant() {
		return false;
	}

	private class JDCConnection implements Connection
	{
		private ConnectionService pool;
		private Connection conn;
		private boolean inuse;
		private long timestamp;

		public JDCConnection(Connection conn, ConnectionService pool) {
			this.conn = conn;
			this.pool = pool;
			this.inuse = false;
			this.timestamp = 0;
		}

		public synchronized boolean lease() {
			if (inuse) {
				return false;
			} else {
				inuse = true;
				timestamp = System.currentTimeMillis();
				return true;
			}
		}

		public boolean validate() {
			try {
				conn.getMetaData();
			} catch (Exception e) {
				return false;
			}
			return true;
		}

		public boolean inUse() {
			return inuse;
		}

		public long getLastUse() {
			return timestamp;
		}

		public void close() throws SQLException {
			pool.returnConnection(this);
		}

		protected void expireLease() {
			inuse = false;
		}

		protected Connection getConnection() {
			return conn;
		}

		public PreparedStatement prepareStatement(String sql) throws SQLException {
			return conn.prepareStatement(sql);
		}

		public CallableStatement prepareCall(String sql) throws SQLException {
			return conn.prepareCall(sql);
		}

		public Statement createStatement() throws SQLException {
			return conn.createStatement();
		}

		public String nativeSQL(String sql) throws SQLException {
			return conn.nativeSQL(sql);
		}

		public void setAutoCommit(boolean autoCommit) throws SQLException {
			conn.setAutoCommit(autoCommit);
		}

		public boolean getAutoCommit() throws SQLException {
			return conn.getAutoCommit();
		}

		public void commit() throws SQLException {
			conn.commit();
		}

		public void rollback() throws SQLException {
			conn.rollback();
		}

		public boolean isClosed() throws SQLException {
			return conn.isClosed();
		}

		public DatabaseMetaData getMetaData() throws SQLException {
			return conn.getMetaData();
		}

		public void setReadOnly(boolean readOnly) throws SQLException {
			conn.setReadOnly(readOnly);
		}

		public boolean isReadOnly() throws SQLException {
			return conn.isReadOnly();
		}

		public void setCatalog(String catalog) throws SQLException {
			conn.setCatalog(catalog);
		}

		public String getCatalog() throws SQLException {
			return conn.getCatalog();
		}

		public void setTransactionIsolation(int level) throws SQLException {
			conn.setTransactionIsolation(level);
		}

		public int getTransactionIsolation() throws SQLException {
			return conn.getTransactionIsolation();
		}

		public SQLWarning getWarnings() throws SQLException {
			return conn.getWarnings();
		}

		public void clearWarnings() throws SQLException {
			conn.clearWarnings();
		}

		@Override
		public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
			return conn.createArrayOf(typeName, elements);
		}

		@Override
		public Blob createBlob() throws SQLException {
			return createBlob();
		}

		@Override
		public Clob createClob() throws SQLException {
			return conn.createClob();
		}

		@Override
		public NClob createNClob() throws SQLException {
			return conn.createNClob();
		}

		@Override
		public SQLXML createSQLXML() throws SQLException {
			return conn.createSQLXML();
		}

		@Override
		public Statement createStatement(int resultSetType, int resultSetConcurrency)
			throws SQLException {
			return conn.createStatement(resultSetType, resultSetConcurrency);
		}

		@Override
		public Statement createStatement(int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) throws SQLException {
			return conn
				.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
		}

		@Override
		public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
			return conn.createStruct(typeName, attributes);
		}

		@Override
		public Properties getClientInfo() throws SQLException {
			return conn.getClientInfo();
		}

		@Override
		public String getClientInfo(String name) throws SQLException {
			return conn.getClientInfo(name);
		}

		@Override
		public int getHoldability() throws SQLException {
			return conn.getHoldability();
		}

		@Override
		public Map<String, Class<?>> getTypeMap() throws SQLException {
			return null;
		}

		@Override
		public boolean isValid(int timeout) throws SQLException {
			return false;
		}

		@Override
		public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
			return null;
		}

		@Override
		public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
			return null;
		}

		@Override
		public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
			return null;
		}

		@Override
		public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
			return null;
		}

		@Override
		public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
			return null;
		}

		@Override
		public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
			return null;
		}

		@Override
		public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
			return null;
		}

		@Override
		public void releaseSavepoint(Savepoint savepoint) throws SQLException {
		}

		@Override
		public void rollback(Savepoint savepoint) throws SQLException {
		}

		@Override
		public void setClientInfo(Properties properties) throws SQLClientInfoException {
		}

		@Override
		public void setClientInfo(String name, String value) throws SQLClientInfoException {
		}

		@Override
		public void setHoldability(int holdability) throws SQLException {
		}

		@Override
		public Savepoint setSavepoint() throws SQLException {
			return null;
		}

		@Override
		public Savepoint setSavepoint(String name) throws SQLException {
			return null;
		}

		@Override
		public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
		}

		@Override
		public boolean isWrapperFor(Class<?> iface) throws SQLException {
			return false;
		}

		@Override
		public <T> T unwrap(Class<T> iface) throws SQLException {
			return null;
		}
	}

	public class ConnectionService
	{
		private Vector<JDCConnection> connections;
		private String url, user, password;
		final private long timeout = 60000;
		private ConnectionReaper reaper;
		final private int poolsize = 10;

		public ConnectionService(String url, String user, String password) {
			this.url = url;
			this.user = user;
			this.password = password;
			connections = new Vector<JDCConnection>(poolsize);
			reaper = new ConnectionReaper(this);
			reaper.start();
		}

		public synchronized void reapConnections() {
			long stale = System.currentTimeMillis() - timeout;
			Enumeration<JDCConnection> connlist = connections.elements();
			while ((connlist != null) && (connlist.hasMoreElements())) {
				JDCConnection conn = connlist.nextElement();
				if ((conn.inUse()) && (stale > conn.getLastUse()) && (!conn.validate()))
					removeConnection(conn);
			}
		}

		public synchronized void closeConnections() {
			Enumeration<JDCConnection> connlist = connections.elements();
			while ((connlist != null) && (connlist.hasMoreElements())) {
				JDCConnection conn = connlist.nextElement();
				removeConnection(conn);
			}
		}

		private synchronized void removeConnection(JDCConnection conn) {
			connections.removeElement(conn);
		}

		public synchronized Connection getConnection() throws SQLException {
			JDCConnection c;
			for (int i = 0; i < connections.size(); i++) {
				c = connections.elementAt(i);
				if (c.lease()) {
					if (c.validate())
						return c.getConnection();
					else
						removeConnection(c);
				}
			}
			c = new JDCConnection(DriverManager.getConnection(url, user, password), this);
			c.lease();
			connections.addElement(c);
			return c.getConnection();
		}

		public synchronized void returnConnection(JDCConnection conn) {
			conn.expireLease();
		}
	}

	class ConnectionReaper extends Thread
	{
		private ConnectionService pool;
		private final long delay = 300000;

		ConnectionReaper(ConnectionService pool) {
			this.pool = pool;
		}

		@Override
		public void run() {
			while (true) {
				try {
					Thread.sleep(delay);
				} catch (InterruptedException e) {
			}
			pool.reapConnections();
			}
		}
	}
}
