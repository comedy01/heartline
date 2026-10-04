package dev.heartline.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public final class MobTypes {
    private static final String VANILLA = "minecraft";

    private MobTypes() {
    }

    public record Mob(String id, Component name) {
    }

    public record Mod(String id, String name, List<Mob> mobs) {
    }

    public static List<Mod> byMod() {
        Map<String, List<Mob>> grouped = new TreeMap<>();
        for (EntityType<?> type : EntityTypes.all()) {
            if (!DefaultAttributes.hasSupplier(type)) {
                continue;
            }
            String id = String.valueOf(EntityType.getKey(type));
            if (id.equals("minecraft:player") || id.equals("minecraft:armor_stand")) {
                continue;
            }
            int colon = id.indexOf(':');
            String mod = colon < 0 ? VANILLA : id.substring(0, colon);
            grouped.computeIfAbsent(mod, key -> new ArrayList<>()).add(new Mob(id, type.getDescription()));
        }
        List<Mod> mods = new ArrayList<>();
        for (Map.Entry<String, List<Mob>> entry : grouped.entrySet()) {
            List<Mob> mobs = entry.getValue();
            mobs.sort(Comparator.comparing(mob -> mob.name().getString().toLowerCase(Locale.ROOT)));
            mods.add(new Mod(entry.getKey(), ModNames.of(entry.getKey()), mobs));
        }
        mods.sort(Comparator.comparing((Mod mod) -> !mod.id().equals(VANILLA))
                .thenComparing(mod -> mod.name().toLowerCase(Locale.ROOT)));
        return mods;
    }
}
