package de.diddiz.LogBlock;

import static de.diddiz.util.Utils.readURL;
import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

class Updater
{
	private final Logger log;
	private final LogBlock logblock;

	Updater(LogBlock logblock) {
		this.logblock = logblock;
		log = logblock.getServer().getLogger();
	}

	void checkTables() throws SQLException {
		final Connection conn = logblock.getConnection();
		if (conn == null)
			throw new SQLException("No connection");
		final Statement state = conn.createStatement();
		final DatabaseMetaData dbm = conn.getMetaData();
		conn.setAutoCommit(true);
		if (!dbm.getTables(null, null, "lb-players", null).next()) {
			log.log(Level.INFO, "[LogBlock] Crating table lb-players.");
			state.execute("CREATE TABLE `lb-players` (playerid SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT, playername varchar(32) NOT NULL DEFAULT '-', PRIMARY KEY (playerid), UNIQUE (playername))");
			if (!dbm.getTables(null, null, "lb-players", null).next())
				throw new SQLException("Table lb-players not found");
		}
		for (final String table : logblock.getConfig().tables.values()) {
			if (!dbm.getTables(null, null, table, null).next()) {
				log.log(Level.INFO, "[LogBlock] Crating table " + table + ".");
				state.execute("CREATE TABLE `" + table + "` (id INT NOT NULL AUTO_INCREMENT, date DATETIME NOT NULL, playerid SMALLINT UNSIGNED NOT NULL, replaced TINYINT UNSIGNED NOT NULL, type TINYINT UNSIGNED NOT NULL, data TINYINT UNSIGNED NOT NULL, x SMALLINT NOT NULL, y TINYINT UNSIGNED NOT NULL, z SMALLINT NOT NULL, PRIMARY KEY (id), KEY coords (x, z, y), KEY date (date))");
				if (!dbm.getTables(null, null, table, null).next())
					throw new SQLException("Table " + table + " not found");
			}
			if (!dbm.getTables(null, null, table + "-sign", null).next()) {
				log.log(Level.INFO, "[LogBlock] Crating table " + table + "-sign.");
				state.execute("CREATE TABLE `" + table + "-sign` (id INT NOT NULL, signtext TEXT, PRIMARY KEY (id));");
				if (!dbm.getTables(null, null, table + "-sign", null).next())
					throw new SQLException("Table " + table + "-sign not found");
			}
			if (dbm.getTables(null, null, table + "-chest", null).next() && state.executeQuery("SELECT * FROM `" + table + "-chest` LIMIT 1").getMetaData().getColumnCount() != 4) // Chest table update
				state.execute("DROP TABLE `" + table + "-chest`");
			if (!dbm.getTables(null, null, table + "-chest", null).next()) {
				log.log(Level.INFO, "[LogBlock] Crating table " + table + "-chest.");
				state.execute("CREATE TABLE `" + table + "-chest` (id INT NOT NULL, itemtype SMALLINT UNSIGNED NOT NULL, itemamount SMALLINT NOT NULL, itemdata TINYINT UNSIGNED NOT NULL, PRIMARY KEY (id))");
				if (!dbm.getTables(null, null, table + "-chest", null).next())
					throw new SQLException("Table " + table + "-chest not found");
			}
			if (logblock.getConfig().logKills && !dbm.getTables(null, null, table + "-kills", null).next()) {
				log.log(Level.INFO, "[LogBlock] Crating table " + table + "-kills.");
				state.execute("CREATE TABLE `" + table + "-kills` (id INT UNSIGNED NOT NULL AUTO_INCREMENT, date DATETIME NOT NULL, killer SMALLINT UNSIGNED, victim SMALLINT UNSIGNED NOT NULL, weapon SMALLINT UNSIGNED NOT NULL, PRIMARY KEY (id));");
				if (!dbm.getTables(null, null, table + "-kills", null).next())
					throw new SQLException("Table " + table + "-kills not found");
			}
		}
		state.close();
		conn.close();
	}

	String checkVersion() {
		try {
			return readURL(new URL("http://diddiz.insane-architects.net/lbuptodate.php?v=" + logblock.getDescription().getVersion()));
		} catch (final Exception ex) {
			return "Can't connect to server";
		}
	}
}
