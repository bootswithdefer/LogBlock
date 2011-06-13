package de.diddiz.LogBlock;

import static de.diddiz.util.Utils.readURL;
import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.util.config.Configuration;

class Updater
{
	private final Logger log;
	private final LogBlock logblock;

	Updater(LogBlock logblock) {
		this.logblock = logblock;
		log = logblock.getServer().getLogger();
	}

	boolean update() {
		final Configuration config = logblock.getConfiguration();
		config.load();
		if (config.getString("version").compareTo(logblock.getDescription().getVersion()) >= 0)
			return false;
		if (config.getString("version").compareTo("1.10") < 0) {
			log.info("Updating config to 1.10 ...");
			String params = config.getString("lookup.toolQuery");
			if (!params.contains("silent"))
				config.setProperty("lookup.toolQuery", params + " silent");
			params = config.getString("lookup.toolBlockQuery");
			if (!params.contains("silent"))
				config.setProperty("lookup.toolBlockQuery", params + " silent");
			config.setProperty("version", "1.10");
		}
		config.save();
		return true;
	}

	void checkTables() throws SQLException {
		final Connection conn = logblock.getConnection();
		if (conn == null)
			throw new SQLException("No connection");
		final Statement state = conn.createStatement();
		final DatabaseMetaData dbm = conn.getMetaData();
		conn.setAutoCommit(true);
		createTable(dbm, state, "lb-players", "(playerid SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT, playername varchar(32) NOT NULL DEFAULT '-', PRIMARY KEY (playerid), UNIQUE (playername))");
		for (final String table : logblock.getConfig().tables.values()) {
			if (dbm.getTables(null, null, table + "-chest", null).next() && state.executeQuery("SELECT * FROM `" + table + "-chest` LIMIT 1").getMetaData().getColumnCount() != 4) // Chest table update
				state.execute("DROP TABLE `" + table + "-chest`");
			createTable(dbm, state, table, "(id INT NOT NULL AUTO_INCREMENT, date DATETIME NOT NULL, playerid SMALLINT UNSIGNED NOT NULL, replaced TINYINT UNSIGNED NOT NULL, type TINYINT UNSIGNED NOT NULL, data TINYINT UNSIGNED NOT NULL, x SMALLINT NOT NULL, y TINYINT UNSIGNED NOT NULL, z SMALLINT NOT NULL, PRIMARY KEY (id), KEY coords (x, z, y), KEY date (date), KEY playerid (playerid))");
			createTable(dbm, state, table + "-sign", "(id INT NOT NULL, signtext TEXT, PRIMARY KEY (id))");
			createTable(dbm, state, table + "-chest", "(id INT NOT NULL, itemtype SMALLINT UNSIGNED NOT NULL, itemamount SMALLINT NOT NULL, itemdata TINYINT UNSIGNED NOT NULL, PRIMARY KEY (id))");
			if (logblock.getConfig().logKills)
				createTable(dbm, state, table + "-kills", "(id INT UNSIGNED NOT NULL AUTO_INCREMENT, date DATETIME NOT NULL, killer SMALLINT UNSIGNED, victim SMALLINT UNSIGNED NOT NULL, weapon SMALLINT UNSIGNED NOT NULL, PRIMARY KEY (id))");
		}
		state.close();
		conn.close();
	}

	private void createTable(DatabaseMetaData dbm, Statement state, String table, String query) throws SQLException {
		if (!dbm.getTables(null, null, table, null).next()) {
			log.log(Level.INFO, "[LogBlock] Crating table " + table + ".");
			state.execute("CREATE TABLE `" + table + "` " + query);
			if (!dbm.getTables(null, null, table, null).next())
				throw new SQLException("Table " + table + " not found and failed to create");
		}
	}

	String checkVersion() {
		try {
			return readURL(new URL("http://diddiz.insane-architects.net/lbuptodate.php?v=" + logblock.getDescription().getVersion()));
		} catch (final Exception ex) {
			return "Can't connect to server";
		}
	}
}
