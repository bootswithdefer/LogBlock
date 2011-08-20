package de.diddiz.LogBlock;

import static de.diddiz.util.BukkitUtils.friendlyWorldname;
import static de.diddiz.util.Utils.readURL;
import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
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
			log.info("[LogBlock] Updating config to 1.10 ...");
			String params = config.getString("lookup.toolQuery");
			if (!params.contains("silent"))
				config.setProperty("lookup.toolQuery", params + " silent");
			params = config.getString("lookup.toolBlockQuery");
			if (!params.contains("silent"))
				config.setProperty("lookup.toolBlockQuery", params + " silent");
			config.setProperty("version", "1.10");
		}
		if (config.getString("version").compareTo("1.20") < 0) {
			log.info("[LogBlock] Updating tables to 1.20 ...");
			final Connection conn = logblock.getConnection();
			try {
				conn.setAutoCommit(true);
				final Statement st = conn.createStatement();
				for (final String table : config.getStringList("tables", null))
					st.execute("ALTER TABLE `" + table + "-sign` MODIFY signtext VARCHAR(255) NOT NULL");
				st.close();
				conn.close();
			} catch (final SQLException ex) {}
			config.setProperty("version", "1.20");
		}
		if (config.getString("version").compareTo("1.23") < 0) {
			log.info("[LogBlock] Updating tables to 1.23 ...");
			final Connection conn = logblock.getConnection();
			try {
				conn.setAutoCommit(true);
				final Statement st = conn.createStatement();
				for (final String table : config.getStringList("tables", null))
					if (st.executeQuery("SELECT * FROM `" + table + "-chest` LIMIT 1").getMetaData().getColumnCount() != 4)
						st.execute("DROP TABLE `" + table + "-chest`");
				st.close();
				conn.close();
			} catch (final SQLException ex) {}
			log.info("[LogBlock] Updating config to 1.23 ...");
			final List<String> worldNames = config.getStringList("loggedWorlds", null), worldTables = config.getStringList("tables", null);
			final String[] nodes = new String[]{"BlockCreations", "BlockDestroyings", "SignTexts", "Explosions", "Fire", "LeavesDecay", "LavaFlow", "ChestAccess", "ButtonsAndLevers", "Kills", "Chat"};
			for (int i = 0; i < worldNames.size(); i++) {
				final Configuration wcfg = new Configuration(new File("plugins/LogBlock/" + friendlyWorldname(worldNames.get(i)) + ".yml"));
				wcfg.load();
				wcfg.setProperty("table", worldTables.get(i));
				for (final String node : nodes)
					wcfg.setProperty("log" + node, config.getBoolean("logging.log" + node, true));
				wcfg.save();
			}
			for (final String node : nodes)
				config.removeProperty("logging.log" + node);
			config.removeProperty("tables");
			config.setProperty("version", "1.23");
		}
		if (config.getString("version").compareTo("1.27") < 0) {
			log.info("[LogBlock] Updating tables to 1.27 ...");
			final Connection conn = logblock.getConnection();
			try {
				conn.setAutoCommit(true);
				final Statement st = conn.createStatement();
				st.execute("ALTER TABLE `lb-chat` ENGINE = MyISAM, ADD FULLTEXT message (message)");
				st.close();
				conn.close();
			} catch (final SQLException ex) {}
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
		if (logblock.getConfig().logChat)
			createTable(dbm, state, "lb-chat", "(id INT UNSIGNED NOT NULL AUTO_INCREMENT, date DATETIME NOT NULL, playerid SMALLINT UNSIGNED NOT NULL, message VARCHAR(255) NOT NULL, PRIMARY KEY (id), KEY playerid (playerid), FULLTEXT message (message)) ENGINE=MyISAM");
		for (final WorldConfig wcfg : logblock.getConfig().worlds.values()) {
			createTable(dbm, state, wcfg.table, "(id INT UNSIGNED NOT NULL AUTO_INCREMENT, date DATETIME NOT NULL, playerid SMALLINT UNSIGNED NOT NULL, replaced TINYINT UNSIGNED NOT NULL, type TINYINT UNSIGNED NOT NULL, data TINYINT UNSIGNED NOT NULL, x SMALLINT NOT NULL, y TINYINT UNSIGNED NOT NULL, z SMALLINT NOT NULL, PRIMARY KEY (id), KEY coords (x, z, y), KEY date (date), KEY playerid (playerid))");
			createTable(dbm, state, wcfg.table + "-sign", "(id INT UNSIGNED NOT NULL, signtext VARCHAR(255) NOT NULL, PRIMARY KEY (id))");
			createTable(dbm, state, wcfg.table + "-chest", "(id INT UNSIGNED NOT NULL, itemtype SMALLINT UNSIGNED NOT NULL, itemamount SMALLINT NOT NULL, itemdata TINYINT UNSIGNED NOT NULL, PRIMARY KEY (id))");
			if (wcfg.logKills)
				createTable(dbm, state, wcfg.table + "-kills", "(id INT UNSIGNED NOT NULL AUTO_INCREMENT, date DATETIME NOT NULL, killer SMALLINT UNSIGNED, victim SMALLINT UNSIGNED NOT NULL, weapon SMALLINT UNSIGNED NOT NULL, PRIMARY KEY (id))");
		}
		state.close();
		conn.close();
	}

	private void createTable(DatabaseMetaData dbm, Statement state, String table, String query) throws SQLException {
		if (!dbm.getTables(null, null, table, null).next()) {
			log.log(Level.INFO, "[LogBlock] Creating table " + table + ".");
			state.execute("CREATE TABLE `" + table + "` " + query);
			if (!dbm.getTables(null, null, table, null).next())
				throw new SQLException("Table " + table + " not found and failed to create");
		}
	}

	String checkVersion() {
		try {
			return readURL(new URL("http://diddiz.insane-architects.net/lbuptodate.php?v=" + logblock.getDescription().getVersion()));
		} catch (final Exception ex) {
			return "Can't check version";
		}
	}
}
