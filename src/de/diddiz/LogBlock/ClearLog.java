package de.diddiz.LogBlock;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.logging.Level;

public class ClearLog implements Runnable
{
	private Connection conn;
	private File dumbFolder;

	public ClearLog(Connection conn, File dataFolder) {
		this.conn = conn;
		dumbFolder = new File(dataFolder, "dumb");
		dumbFolder.mkdirs();
	}

	@Override
	public void run() {
		if (conn == null)
			return;
		Statement state = null;
		try {
			state = conn.createStatement();
			String time = new SimpleDateFormat("yy-MM-dd-HH-mm-ss").format(System.currentTimeMillis() - LogBlock.config.keepLogDays*86400000);
			ResultSet rs;
			for (String table : LogBlock.config.tables.values()) {
				rs = state.executeQuery("SELECT count(*) FROM `" + table + "` WHERE date < now()");
				rs.next();
				int deleted = rs.getInt(1); 
				if (deleted > 0) {
					if (LogBlock.config.dumpDroppedLog)
						state.execute("SELECT * FROM `" + table + "` WHERE date < '" + time + "' INTO OUTFILE '" + dumbFolder.getAbsolutePath().replace("\\", "\\\\") + "\\\\" + table + "-" + time + ".csv' FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '\"'  LINES TERMINATED BY '\n'");
					state.execute("DELETE FROM `" + table + "` WHERE date < '" + time + "'");
					LogBlock.log.info("[LogBlock] Cleared out table " + table + ". Deleted " + deleted + " entries.");
				}
				rs = state.executeQuery("SELECT COUNT(*) FROM `" + table + "-sign` LEFT JOIN `" + table + "` USING (id) WHERE `" + table + "`.id IS NULL");
				rs.next();
				deleted = rs.getInt(1); 
				if (deleted > 0) {
					if (LogBlock.config.dumpDroppedLog)
						state.execute("SELECT * FROM `" + table + "-sign` LEFT JOIN `" + table + "` USING (id) WHERE `" + table + "`.id IS NULL INTO OUTFILE '" + dumbFolder.getAbsolutePath().replace("\\", "\\\\") + "\\\\" + table + "-sign-" + time + ".csv' FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '\"'  LINES TERMINATED BY '\n'");
					state.execute("DELETE `" + table + "-sign` FROM `" + table + "-sign` LEFT JOIN `" + table + "` USING (id) WHERE `" + table + "`.id IS NULL;");
					LogBlock.log.info("[LogBlock] Cleared out table " + table + "-sign. Deleted " + deleted + " entries.");
				}
				rs = state.executeQuery("SELECT COUNT(*) FROM `" + table + "-chest` LEFT JOIN `" + table + "` USING (id) WHERE `" + table + "`.id IS NULL");
				rs.next();
				deleted = rs.getInt(1); 
				if (deleted > 0) {
					if (LogBlock.config.dumpDroppedLog)
						state.execute("SELECT * FROM `" + table + "-chest` LEFT JOIN `" + table + "` USING (id) WHERE `" + table + "`.id IS NULL INTO OUTFILE '" + dumbFolder.getAbsolutePath().replace("\\", "\\\\") + "\\\\" + table + "-chest-" + time + ".csv' FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '\"'  LINES TERMINATED BY '\n'");
					state.execute("DELETE `" + table + "-chest` FROM `" + table + "-chest` LEFT JOIN `" + table + "` USING (id) WHERE `" + table + "`.id IS NULL;");
					LogBlock.log.info("[LogBlock] Cleared out table " + table + "-chest. Deleted " + deleted + " entries.");
				}
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
