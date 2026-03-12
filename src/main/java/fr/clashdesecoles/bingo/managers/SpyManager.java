package fr.clashdesecoles.bingo.managers;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SpyManager {
    private final ConcurrentMap<UUID, Boolean> spying = new ConcurrentHashMap<>();

    public boolean isSpying(UUID uuid) {
        return spying.getOrDefault(uuid, true); // enabled by default
    }

    public boolean isSpying(org.bukkit.entity.Player player) {
        return isSpying(player.getUniqueId());
    }

    public void setSpying(UUID uuid, boolean enabled) {
        spying.put(uuid, enabled);
    }

    public void toggle(UUID uuid) {
        spying.put(uuid, !isSpying(uuid));
    }

    public void setDefaultIfAbsent(UUID uuid, boolean enabled) {
        spying.putIfAbsent(uuid, enabled);
    }
}
