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
                FIELD_CraftBlockEntityState_snapshot = sign.getClass().getSuperclass().getDeclaredField("snapshot");
                FIELD_CraftBlockEntityState_snapshot.setAccessible(true);
            }
            Object snapshot = FIELD_CraftBlockEntityState_snapshot.get(sign);
            if (snapshot == null) {
                return false;
            }
            if (FIELD_SignBlockEntity_isWaxed == null) {
                for (Field f : snapshot.getClass().getDeclaredFields()) {
                    if (f.getType() == boolean.class) {
                        FIELD_SignBlockEntity_isWaxed = f;
                        FIELD_SignBlockEntity_isWaxed.setAccessible(true);
                    }
                }
            }
            return FIELD_SignBlockEntity_isWaxed != null && FIELD_SignBlockEntity_isWaxed.getBoolean(snapshot);
        } catch (SecurityException | NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
            LogBlock.getInstance().getLogger().log(Level.SEVERE, "Sign.isWaxed reflection failed", e);
        }
        return false;
    }
}
