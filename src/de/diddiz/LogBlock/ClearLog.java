package de.diddiz.LogBlock;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

public class ClearLog implements Runnable
{
	private Connection conn;

	public ClearLog(Connection conn) {
		this.conn = conn;
	}

	@Override
	public void run() {
		if (conn == null)
			return;
		Statement state = null;
		try {
			state = conn.createStatement();
			for (String table : LogBlock.config.tables.values()) {
				int deleted = state.executeUpdate("DELETE FROM `" + table + "` WHERE date < date_sub(now(), INTERVAL " + LogBlock.config.keepLogDays + " DAY)");
				if (deleted > 0)
					LogBlock.log.info("[LogBlock] Cleared out table " + table + ". Deleted " + deleted + " entries.");
				deleted = state.executeUpdate("DELETE `" + table + "-sign` FROM `" + table + "-sign` LEFT JOIN `" + table + "` ON (`" + table + "-sign`.`id` = `" + table + "`.`id`) WHERE `" + table + "`.`id` IS NULL;");
				if (deleted > 0)
					LogBlock.log.info("[LogBlock] Cleared out table " + table + "-sign. Deleted " + deleted + " entries.");
				deleted = state.executeUpdate("DELETE `" + table + "-chest` FROM `" + table + "-chest` LEFT JOIN `" + table + "` ON (`" + table + "-chest`.`id` = `" + table + "`.`id`) WHERE `" + table + "`.`id` IS NULL;");
				if (deleted > 0)
					LogBlock.log.info("[LogBlock] Cleared out table " + table + "-chest. Deleted " + deleted + " entries.");
			}
		} catch (SQLException ex) {
			LogBlock.log.log(Level.SEVERE, "[LogBlock] SQL exception", ex);
		} finally {
			try {
				if (state != null)
					state.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				LogBlock.log.log(Level.SEVERE, "[LogBlock] SQL exception on close", ex);
			}
		}
	}
}
