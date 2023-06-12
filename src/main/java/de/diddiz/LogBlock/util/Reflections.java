package de.diddiz.LogBlock.util;

import de.diddiz.LogBlock.LogBlock;
import java.lang.reflect.Field;
import java.util.logging.Level;
import org.bukkit.block.Sign;

public class Reflections {
    private static Field FIELD_CraftBlockEntityState_snapshot;
    private static Field FIELD_SignBlockEntity_isWaxed;

    public static boolean isSignWaxed(Sign sign) {
        try {
            if (FIELD_CraftBlockEntityState_snapshot == null) {
                Class<?> superClass = sign.getClass().getSuperclass();
                while (superClass != null) {
                    try {
                        FIELD_CraftBlockEntityState_snapshot = superClass.getDeclaredField("snapshot");
                        FIELD_CraftBlockEntityState_snapshot.setAccessible(true);
                        break;
                    } catch (NoSuchFieldException ignored) {
                    }
                    superClass = superClass.getSuperclass();
                }
            }
            if (FIELD_CraftBlockEntityState_snapshot == null) {
                LogBlock.getInstance().getLogger().log(Level.SEVERE, "Reflections: Sign field 'snapshot' not found");
                return false;
            }
            Object snapshot = FIELD_CraftBlockEntityState_snapshot.get(sign);
            if (snapshot == null) {
                LogBlock.getInstance().getLogger().log(Level.SEVERE, "Reflections: Sign snapshot is null?");
                return false;
            }
            if (FIELD_SignBlockEntity_isWaxed == null) {
                Class<?> snapshotClass = snapshot.getClass();
                while (snapshotClass != null) {
                    for (Field f : snapshotClass.getDeclaredFields()) {
                        if (f.getType() == boolean.class) {
                            FIELD_SignBlockEntity_isWaxed = f;
                            FIELD_SignBlockEntity_isWaxed.setAccessible(true);
                            break;
                        }
                    }
                    if (FIELD_SignBlockEntity_isWaxed != null) {
                        break;
                    }
                    snapshotClass = snapshotClass.getSuperclass();
                }
            }
            if (FIELD_SignBlockEntity_isWaxed == null) {
                LogBlock.getInstance().getLogger().log(Level.SEVERE, "Reflections: Sign field 'isWaxed' not found");
                return false;
            }
            return FIELD_SignBlockEntity_isWaxed.getBoolean(snapshot);
        } catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
            LogBlock.getInstance().getLogger().log(Level.SEVERE, "Reflections: Sign.isWaxed reflection failed", e);
        }
        return false;
    }
}
