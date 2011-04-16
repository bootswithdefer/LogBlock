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
	private final LogBlock logblock;

	public ClearLog(LogBlock logblock) {
		this.logblock = logblock;
	}

	@Override
	public void run() {
		File dumpFolder = new File(logblock.getDataFolder(), "dumb");
		dumpFolder.mkdirs();
		Connection conn = logblock.pool.getConnection();
		if (conn == null)
			return;
		Statement state = null;
		try {
			state = conn.createStatement();
			String time = new SimpleDateFormat("yy-MM-dd-HH-mm-ss").format(System.currentTimeMillis() - logblock.config.keepLogDays*86400000L);
			ResultSet rs;
			for (String table : logblock.config.tables.values()) {
				rs = state.executeQuery("SELECT count(*) FROM `" + table + "` WHERE date < '" + time + "'");
				rs.next();
				int deleted = rs.getInt(1); 
				if (deleted > 0) {
					if (logblock.config.dumpDeletedLog)
						state.execute("SELECT * FROM `" + table + "` WHERE date < '" + time + "' INTO OUTFILE '" + new File(dumpFolder, table + "-" + time + ".csv").getAbsolutePath().replace("\\", "\\\\") + "' FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '\"'  LINES TERMINATED BY '\n'");
					state.execute("DELETE FROM `" + table + "` WHERE date < '" + time + "'");
					LogBlock.log.info("[LogBlock] Cleared out table " + table + ". Deleted " + deleted + " entries.");
				}
				rs = state.executeQuery("SELECT COUNT(*) FROM `" + table + "-sign` LEFT JOIN `" + table + "` USING (id) WHERE `" + table + "`.id IS NULL");
				rs.next();
				deleted = rs.getInt(1); 
				if (deleted > 0) {
					if (logblock.config.dumpDeletedLog)
						state.execute("SELECT id, signtext FROM `" + table + "-sign` LEFT JOIN `" + table + "` USING (id) WHERE `" + table + "`.id IS NULL INTO OUTFILE '" + new File(dumpFolder, table + "-sign-" + time + ".csv").getAbsolutePath().replace("\\", "\\\\") + "' FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '\"'  LINES TERMINATED BY '\n'");
					state.execute("DELETE `" + table + "-sign` FROM `" + table + "-sign` LEFT JOIN `" + table + "` USING (id) WHERE `" + table + "`.id IS NULL;");
					LogBlock.log.info("[LogBlock] Cleared out table " + table + "-sign. Deleted " + deleted + " entries.");
				}
				rs = state.executeQuery("SELECT COUNT(*) FROM `" + table + "-chest` LEFT JOIN `" + table + "` USING (id) WHERE `" + table + "`.id IS NULL");
				rs.next();
				deleted = rs.getInt(1); 
				if (deleted > 0) {
					if (logblock.config.dumpDeletedLog)
						state.execute("SELECT id, intype, inamount, outtype, outamount FROM `" + table + "-chest` LEFT JOIN `" + table + "` USING (id) WHERE `" + table + "`.id IS NULL INTO OUTFILE '" + new File(dumpFolder, table + "-chest-" + time + ".csv").getAbsolutePath().replace("\\", "\\\\") + "' FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '\"'  LINES TERMINATED BY '\n'");
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
