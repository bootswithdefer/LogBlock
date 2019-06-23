package de.diddiz.LogBlock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.data.BlockData;

public class MaterialConverter {
    private static String[] idToMaterial = new String[10];
    private static HashMap<String, Integer> materialToID = new HashMap<>();
    private static int nextMaterialId;

    private static String[] idToBlockState = new String[10];
    private static HashMap<String, Integer> blockStateToID = new HashMap<>();
    private static int nextBlockStateId;

    private static HashMap<String, Material> materialKeyToMaterial = new HashMap<>();

    static {
        for (Material m : Material.values()) {
            materialKeyToMaterial.put(m.getKey().toString(), m);
        }
    }

    public static int getOrAddMaterialId(NamespacedKey nameSpaceKey) {
        return getOrAddMaterialId(nameSpaceKey.toString());
    }

    public static int getOrAddMaterialId(String blockDataString) {
        String materialString = blockDataString;
        int dataPart = blockDataString.indexOf("[");
        if (dataPart >= 0) {
            materialString = blockDataString.substring(0, dataPart);
        }
        Integer key = materialToID.get(materialString);
        int tries = 0;
        while (key == null && tries < 10) {
            tries++;
            key = nextMaterialId;
            Connection conn = LogBlock.getInstance().getConnection();
            try {
                conn.setAutoCommit(false);
                PreparedStatement smt = conn.prepareStatement("INSERT IGNORE INTO `lb-materials` (id, name) VALUES (?, ?)");
                smt.setInt(1, key);
                smt.setString(2, materialString);
                boolean couldAdd = smt.executeUpdate() > 0;
                conn.commit();
                smt.close();
                if (couldAdd) {
                    internalAddMaterial(key, materialString);
                } else {
                    initializeMaterials(conn);
                }
            } catch (Exception e) {
                LogBlock.getInstance().getLogger().log(Level.SEVERE, "Could not update lb-materials", e);
                reinitializeMaterialsCatchException();
                if (tries == 10) {
                    throw new RuntimeException(e);
                }
            } finally {
                try {
                    conn.close();
                } catch (SQLException e) {
                    // ignored
                }
            }
            key = materialToID.get(materialString);
        }
        return key.intValue();
    }

    public static int getOrAddBlockStateId(String blockDataString) {
        int dataPart = blockDataString.indexOf("[");
        if (dataPart < 0) {
            return -1;
        }
        String materialString = blockDataString.substring(dataPart);
        Integer key = blockStateToID.get(materialString);
        int tries = 0;
        while (key == null && tries < 10) {
            tries++;
            key = nextBlockStateId;
            Connection conn = LogBlock.getInstance().getConnection();
            try {
                conn.setAutoCommit(false);
                PreparedStatement smt = conn.prepareStatement("INSERT IGNORE INTO `lb-blockstates` (id, name) VALUES (?, ?)");
                smt.setInt(1, key);
                smt.setString(2, materialString);
                boolean couldAdd = smt.executeUpdate() > 0;
                conn.commit();
                smt.close();
                if (couldAdd) {
                    internalAddBlockState(key, materialString);
                } else {
                    initializeMaterials(conn);
                }
            } catch (Exception e) {
                LogBlock.getInstance().getLogger().log(Level.SEVERE, "Could not update lb-blockstates", e);
                reinitializeMaterialsCatchException();
                if (tries == 10) {
                    throw new RuntimeException(e);
                }
            } finally {
                try {
                    conn.close();
                } catch (SQLException e) {
                    // ignored
                }
            }
            key = blockStateToID.get(materialString);
        }
        return key.intValue();
    }

    public static BlockData getBlockData(int materialId, int blockStateId) {
        String material = materialId >= 0 && materialId < idToMaterial.length ? idToMaterial[materialId] : null;
        if (material == null) {
            return null;
        }
        if (blockStateId >= 0 && blockStateId < idToBlockState.length && idToBlockState[blockStateId] != null) {
            material = material + idToBlockState[blockStateId];
        }
        try {
            return Bukkit.createBlockData(material);
        } catch (IllegalArgumentException ignored) {
            // fall back to create the default block data for the material
            try {
                return Bukkit.createBlockData(idToMaterial[materialId]);
            } catch (IllegalArgumentException ignored2) {
                return null;
            }
        }
    }

    public static Material getMaterial(int materialId) {
        return materialId >= 0 && materialId < idToMaterial.length ? materialKeyToMaterial.get(idToMaterial[materialId]) : null;
    }

    private static void reinitializeMaterialsCatchException() {
        Connection conn = LogBlock.getInstance().getConnection();
        try {
            initializeMaterials(conn);
        } catch (Exception e) {
            LogBlock.getInstance().getLogger().log(Level.SEVERE, "Could not reinitialize lb-materials", e);
        } finally {
            try {
                conn.close();
            } catch (Exception e) {
                // ignored
            }
        }
    }

    public static void initializeMaterials(Connection connection) throws SQLException {
        Statement smt = connection.createStatement();
        ResultSet rs = smt.executeQuery("SELECT id, name FROM `lb-materials`");
        while (rs.next()) {
            int key = rs.getInt(1);
            String materialString = rs.getString(2);
            internalAddMaterial(key, materialString);
        }
        rs.close();
        rs = smt.executeQuery("SELECT id, name FROM `lb-blockstates`");
        while (rs.next()) {
            int key = rs.getInt(1);
            String materialString = rs.getString(2);
            internalAddBlockState(key, materialString);
        }
        rs.close();
        smt.close();
        connection.close();
    }

    private synchronized static void internalAddMaterial(int key, String materialString) {
        materialToID.put(materialString, key);
        int length = idToMaterial.length;
        while (length <= key) {
            length = (length * 3 / 2) + 5;
        }
        if (length > idToMaterial.length) {
            idToMaterial = Arrays.copyOf(idToMaterial, length);
        }
        idToMaterial[key] = materialString;
        if (nextMaterialId <= key) {
            nextMaterialId = key + 1;
        }
    }

    private synchronized static void internalAddBlockState(int key, String materialString) {
        blockStateToID.put(materialString, key);
        int length = idToBlockState.length;
        while (length <= key) {
            length = (length * 3 / 2) + 5;
        }
        if (length > idToBlockState.length) {
            idToBlockState = Arrays.copyOf(idToBlockState, length);
        }
        idToBlockState[key] = materialString;
        if (nextBlockStateId <= key) {
            nextBlockStateId = key + 1;
        }
    }
}
