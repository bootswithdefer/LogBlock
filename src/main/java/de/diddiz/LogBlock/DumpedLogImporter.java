package de.diddiz.LogBlock;

import de.diddiz.util.Utils.ExtensionFilenameFilter;

import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

import static de.diddiz.util.Utils.newline;

public class DumpedLogImporter implements Runnable {
    private final LogBlock logblock;

    DumpedLogImporter(LogBlock logblock) {
        this.logblock = logblock;
    }

    @Override
    public void run() {
        final File[] imports = new File("plugins/LogBlock/import/").listFiles(new ExtensionFilenameFilter("sql"));
        if (imports != null && imports.length > 0) {
            logblock.getLogger().info("Found " + imports.length + " imports.");
            Connection conn = null;
            try {
                conn = logblock.getConnection();
                if (conn == null) {
                    return;
                }
                conn.setAutoCommit(false);
                final Statement st = conn.createStatement();
                final BufferedWriter writer = new BufferedWriter(new FileWriter(new File(logblock.getDataFolder(), "import/failed.txt")));
                int successes = 0, errors = 0;
                try {
                    for (final File sqlFile : imports) {
                        String line = null;
                        try {
                            logblock.getLogger().info("Trying to import " + sqlFile.getName() + " ...");
                            final BufferedReader reader = new BufferedReader(new FileReader(sqlFile));
                            while ((line = reader.readLine()) != null) {
                                if (line.endsWith(";")) {
                                    line = line.substring(0, line.length() - 1);
                                }
                                if(!line.isEmpty()) {
                                    st.addBatch(line);
                                    successes++;
                                }
                            }
                            st.executeBatch();
                            conn.commit();
                            reader.close();
                            sqlFile.delete();
                            logblock.getLogger().info("Successfully imported " + sqlFile.getName() + ".");
                        } catch (final Exception ex) {
                            logblock.getLogger().warning("Error while importing: '" + line + "': " + ex.getMessage());
                            writer.write(line + newline);
                            errors++;
                            ex.printStackTrace();
                            return;
                        }
                    }
                } finally {
                    writer.close();
                }
                st.close();
                logblock.getLogger().info("Successfully imported stored queue. (" + successes + " rows imported, " + errors + " errors)");
            } catch (final Exception ex) {
                logblock.getLogger().log(Level.WARNING, "Error while importing: ", ex);
            } finally {
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (final SQLException ex) {
                    }
                }
            }
        }
    }
}
