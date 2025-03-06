package dev.nikee.nikerantykomar;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class NikerAntyKomar extends JavaPlugin implements Listener {
    private final HashMap<UUID, Long> fireworkUsageTimes = new HashMap();
    private final HashMap<UUID, Long> attackTimes = new HashMap();
    private final HashMap<UUID, Integer> playerReports = new HashMap();
    private final HashMap<UUID, Long> lastReportTime = new HashMap();
    private int elytraCheckThreshold;
    private int maxReports;
    private String adminMessage;
    private String banCommand;
    private List<String> allowedUsers;

    public NikerAntyKomar() {
    }

    public void onEnable() {
        getLogger().info("|-----------<NikerAntyKomar>------------|");
        getLogger().info("|   Ver: 1.0       |      Author: Niker |");
        getLogger().info("|________________<WORKS>________________|");
        this.saveDefaultConfig();
        this.reloadConfigValues();
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getScheduler().runTaskTimer(this, this::resetOldReports, 6000L, 6000L);
    }

    public void onDisable() {
        getLogger().info("|-----------<NikerAntyKomar>------------|");
        getLogger().info("|   Ver: 1.0       |      Author: Niker |");
        getLogger().info("|_____________<DON'T WORKS>_____________|");
    }

    private void reloadConfigValues() {
        FileConfiguration config = this.getConfig();
        this.maxReports = config.getInt("reports", 8);
        this.adminMessage = config.getString("messages", "§7Gracz §f%name% §7może używać komara §f§lx%reportsId%");
        this.banCommand = config.getString("BanCommand", "ban %name% cheaty - komar (Podejrzenie) 30min");
        this.allowedUsers = config.getStringList("allowedUsers");
        this.elytraCheckThreshold = config.getInt("elytraCheckThreshold", 250);
    }

    @EventHandler
    public void onElytraHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player attacker = (Player)event.getDamager();
            if (attacker.isGliding() && attacker.getGameMode() != GameMode.CREATIVE) {
                this.attackTimes.put(attacker.getUniqueId(), System.currentTimeMillis());
                this.checkForElytraCheat(attacker);
            }
        }

    }

    @EventHandler
    public void onFireworkUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item != null && item.getType() == Material.FIREWORK_ROCKET && player.isGliding()) {
            this.fireworkUsageTimes.put(player.getUniqueId(), System.currentTimeMillis());
        }

    }

    private void checkForElytraCheat(Player player) {
        if (this.fireworkUsageTimes.containsKey(player.getUniqueId()) && this.attackTimes.containsKey(player.getUniqueId())) {
            long fireworkTime = this.fireworkUsageTimes.get(player.getUniqueId());
            long attackTime = this.attackTimes.get(player.getUniqueId());
            if (Math.abs(fireworkTime - attackTime) < this.elytraCheckThreshold) {
                this.incrementReports(player);
            }
        }
    }

    private void incrementReports(Player player) {
        UUID playerId = player.getUniqueId();
        int currentReports = (Integer)this.playerReports.getOrDefault(playerId, 0) + 1;
        this.playerReports.put(playerId, currentReports);
        this.lastReportTime.put(playerId, System.currentTimeMillis());
        String message = this.adminMessage.replace("%name%", player.getName()).replace("%reportsId%", String.valueOf(currentReports));
        if (currentReports >= this.maxReports) {
            this.banPlayer(player);
        } else {
            this.sendAdminMessage(message);
        }

    }

    private void resetOldReports() {
        long currentTime = System.currentTimeMillis();
        this.playerReports.entrySet().removeIf((entry) -> {
            UUID playerId = (UUID)entry.getKey();
            Long lastTime = (Long)this.lastReportTime.get(playerId);
            return lastTime != null && currentTime - lastTime > 300000L;
        });
    }

    private void sendAdminMessage(String message) {
        Bukkit.getLogger().warning("[KomarDetector] " + message);
        Iterator var2 = Bukkit.getOnlinePlayers().iterator();

        while(var2.hasNext()) {
            Player p = (Player)var2.next();
            if (p.hasPermission("NikerAntyKomar.ac.show")) {
                p.sendMessage(message);
            }
        }

    }

    private void banPlayer(Player player) {
        Bukkit.getScheduler().runTask(this, () -> {
            String command = this.banCommand.replace("%name%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            this.playerReports.remove(player.getUniqueId());
        });
    }
}
