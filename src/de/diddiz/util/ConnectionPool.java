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
import java.sql.DriverManager;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

public class ConnectionPool {
	private final Vector<JDCConnection> connections;
	private final String url, user, password;
	private final long timeout = 60000;
	private final ConnectionReaper reaper;
	private final int poolsize = 10;

	public ConnectionPool(String url, String user, String password) throws ClassNotFoundException {
		Class.forName("com.mysql.jdbc.Driver");
		this.url = url;
		this.user = user;
		this.password = password;
		connections = new Vector<JDCConnection>(poolsize);
		reaper = new ConnectionReaper();
		reaper.start();
	}

	public synchronized Connection getConnection() throws SQLException {
		JDCConnection c;
		for (int i = 0; i < connections.size(); i++) {
			c = connections.elementAt(i);
			if (c.lease()) {
				if (c.validate())
					return c;
				else {
					c.terminate();
					removeConnection(c);
				}
			}
		}
		c = new JDCConnection(DriverManager.getConnection(url, user, password));
		c.lease();
		if (!c.validate()) {
			c.terminate();
			throw new SQLException("Failed to validate a brand new connection");
		}
		connections.addElement(c);
		return c;
	}

	private synchronized void reapConnections() {
		final long stale = System.currentTimeMillis() - timeout;
		final Enumeration<JDCConnection> connlist = connections.elements();
		while (connlist != null && connlist.hasMoreElements()) {
			final JDCConnection conn = connlist.nextElement();
			if (conn.inUse() && stale > conn.getLastUse() && !conn.validate())
				removeConnection(conn);
		}
	}

	public synchronized void closeConnections() {
		final Enumeration<JDCConnection> connlist = connections.elements();
		while (connlist != null && connlist.hasMoreElements()) {
			final JDCConnection conn = connlist.nextElement();
			conn.terminate();
			removeConnection(conn);
		}
	}

	private synchronized void removeConnection(JDCConnection conn) {
		connections.removeElement(conn);
	}

	private class ConnectionReaper extends Thread
	{
		@Override
		public void run() {
			while (true) {
				try {
					Thread.sleep(300000);
				} catch (final InterruptedException e) {}
				reapConnections();
			}
		}
	}

	private class JDCConnection implements Connection
	{
		private final Connection conn;
		private boolean inuse;
		private long timestamp;

		public JDCConnection(Connection conn) {
			this.conn = conn;
			inuse = false;
			timestamp = 0;
		}

		public void terminate() {
			try {
				conn.close();
			} catch (SQLException ex) {}
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
			Statement state = null;
			ResultSet rs = null;
			try {
				conn.getMetaData();
				state = conn.createStatement();
				rs = state.executeQuery("/* ping */");
				if (rs.next() && rs.getInt(1) == 1)
					return true;
			} catch (final SQLException ex) {
			} finally {
				try {
					if (rs != null)
						rs.close();
					if (state != null)
						state.close();
				} catch (final SQLException ex) {}
			}
			return false;
		}

		public boolean inUse() {
			return inuse;
		}

		public long getLastUse() {
			return timestamp;
		}

		@Override
		public void close() {
			inuse = false;
		}

		@Override
		public PreparedStatement prepareStatement(String sql) throws SQLException {
			return conn.prepareStatement(sql);
		}

		@Override
		public CallableStatement prepareCall(String sql) throws SQLException {
			return conn.prepareCall(sql);
		}

		@Override
		public Statement createStatement() throws SQLException {
			return conn.createStatement();
		}

		@Override
		public String nativeSQL(String sql) throws SQLException {
			return conn.nativeSQL(sql);
		}

		@Override
		public void setAutoCommit(boolean autoCommit) throws SQLException {
			conn.setAutoCommit(autoCommit);
		}

		@Override
		public boolean getAutoCommit() throws SQLException {
			return conn.getAutoCommit();
		}

		@Override
		public void commit() throws SQLException {
			conn.commit();
		}

		@Override
		public void rollback() throws SQLException {
			conn.rollback();
		}

		@Override
		public boolean isClosed() throws SQLException {
			return conn.isClosed();
		}

		@Override
		public DatabaseMetaData getMetaData() throws SQLException {
			return conn.getMetaData();
		}

		@Override
		public void setReadOnly(boolean readOnly) throws SQLException {
			conn.setReadOnly(readOnly);
		}

		@Override
		public boolean isReadOnly() throws SQLException {
			return conn.isReadOnly();
		}

		@Override
		public void setCatalog(String catalog) throws SQLException {
			conn.setCatalog(catalog);
		}

		@Override
		public String getCatalog() throws SQLException {
			return conn.getCatalog();
		}

		@Override
		public void setTransactionIsolation(int level) throws SQLException {
			conn.setTransactionIsolation(level);
		}

		@Override
		public int getTransactionIsolation() throws SQLException {
			return conn.getTransactionIsolation();
		}

		@Override
		public SQLWarning getWarnings() throws SQLException {
			return conn.getWarnings();
		}

		@Override
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
		public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
			return conn.createStatement(resultSetType, resultSetConcurrency);
		}

		@Override
		public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
			return conn.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
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
}
