package jre.frwostella.togglePVPCustom;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;

public final class TogglePVPCustom extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private final Map<UUID, Boolean> pvpStatus = new HashMap<>();
    private final Map<UUID, Long> attackMessageCooldown = new HashMap<>();
    private final Map<UUID, Long> pvpCommandCooldown = new HashMap<>();

    /*
     * Local combat tracking.
     * This prevents /pvp off escaping combat even if the external CombatLog hook fails.
     */
    private final Map<UUID, Long> localCombatUntil = new HashMap<>();

    private File dataFile;
    private FileConfiguration dataConfig;

    private String activeBelowNameObjectiveName;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        setupDataFile();
        loadPlayerData();

        getServer().getPluginManager().registerEvents(this, this);

        Objects.requireNonNull(getCommand("pvp")).setExecutor(this);
        Objects.requireNonNull(getCommand("pvp")).setTabCompleter(this);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new TogglePVPExpansion(this).register();
            getLogger().info("PlaceholderAPI hook enabled.");
        } else {
            getLogger().warning("PlaceholderAPI was not found. Placeholders will not work.");
        }

        if (isCombatLogHookAvailable()) {
            getLogger().info("CombatLog hook enabled.");
        }

        setupBelowNameObjective();
        updateBelowNameForEveryone();

        getLogger().info("TogglePVPCustom has been enabled.");
    }

    @Override
    public void onDisable() {
        savePlayerData();

        if (getConfig().getBoolean("belowname.remove-on-disable", true)) {
            removeBelowNameObjective();
        }

        getLogger().info("TogglePVPCustom has been disabled.");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(this, this::updateBelowNameForEveryone, 20L);
    }

    /*
     * Runs early so blocked PvP damage gets cancelled before CombatLog handles it.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerDamagePlayer(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        Player attacker = getAttackingPlayer(event.getDamager());

        if (attacker == null) {
            return;
        }

        if (attacker.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }

        if (!isPvpEnabled(attacker.getUniqueId())) {
            event.setCancelled(true);
            sendCooldownMessage(attacker, "messages.your-pvp-disabled", null);
            return;
        }

        if (!isPvpEnabled(victim.getUniqueId())) {
            event.setCancelled(true);

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("%target%", victim.getName());

            sendCooldownMessage(attacker, "messages.target-pvp-disabled", placeholders);
        }
    }

    /*
     * Tracks real successful PvP hits locally.
     * This is the important fix that blocks /pvp off while in combat.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void trackSuccessfulPvpCombat(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        Player attacker = getAttackingPlayer(event.getDamager());

        if (attacker == null) {
            return;
        }

        if (attacker.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }

        if (!isPvpEnabled(attacker.getUniqueId()) || !isPvpEnabled(victim.getUniqueId())) {
            return;
        }

        int seconds = getConfig().getInt("settings.pvp-toggle-block-seconds", 15);

        if (seconds <= 0) {
            return;
        }

        long expireAt = System.currentTimeMillis() + (seconds * 1000L);

        localCombatUntil.put(attacker.getUniqueId(), expireAt);
        localCombatUntil.put(victim.getUniqueId(), expireAt);
    }

    private Player getAttackingPlayer(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }

        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();

            if (shooter instanceof Player player) {
                return player;
            }
        }

        return null;
    }

    public boolean isPvpEnabled(UUID uuid) {
        return pvpStatus.getOrDefault(uuid, getDefaultPvpStatus());
    }

    public boolean isPvpEnabled(OfflinePlayer player) {
        return isPvpEnabled(player.getUniqueId());
    }

    public boolean getDefaultPvpStatus() {
        return getConfig().getBoolean("settings.default-pvp", true);
    }

    private void setPvpStatus(Player player, boolean enabled) {
        pvpStatus.put(player.getUniqueId(), enabled);

        /*
         * Do NOT clear CombatLog here.
         * Clearing combat here lets players use /pvp off to escape combat.
         */

        if (getConfig().getBoolean("settings.save-player-status", true)) {
            savePlayerData();
        }

        updateBelowNameForEveryone();
    }

    private boolean isCombatLogHookAvailable() {
        if (!getConfig().getBoolean("combatlog-hook.enabled", true)) {
            return false;
        }

        String pluginName = getConfig().getString("combatlog-hook.plugin-name", "CombatLog");
        Plugin combatLogPlugin = Bukkit.getPluginManager().getPlugin(pluginName);

        return combatLogPlugin != null && combatLogPlugin.isEnabled();
    }

    private boolean isPvpChangeCommand(String[] args) {
        if (args.length == 0) {
            return true;
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "on", "enable", "enabled", "off", "disable", "disabled" -> true;
            default -> false;
        };
    }

    private boolean isPvpToggleBlockedInCombat(Player player) {
        if (!getConfig().getBoolean("combatlog-hook.block-toggle-while-in-combat", true)) {
            return false;
        }

        return isLocallyCombatTagged(player) || isPlayerInCombat(player);
    }

    private boolean isLocallyCombatTagged(Player player) {
        long expireAt = localCombatUntil.getOrDefault(player.getUniqueId(), 0L);

        if (expireAt <= 0L) {
            return false;
        }

        if (System.currentTimeMillis() >= expireAt) {
            localCombatUntil.remove(player.getUniqueId());
            return false;
        }

        return true;
    }

    private boolean isPlayerInCombat(Player player) {
        try {
            if (!getConfig().getBoolean("combatlog-hook.enabled", true)) {
                return false;
            }

            String pluginName = getConfig().getString("combatlog-hook.plugin-name", "CombatLog");
            Plugin combatLogPlugin = Bukkit.getPluginManager().getPlugin(pluginName);

            if (combatLogPlugin == null || !combatLogPlugin.isEnabled()) {
                return false;
            }

            Method getCombatManagerMethod = combatLogPlugin.getClass().getMethod("getCombatManager");
            Object combatManager = getCombatManagerMethod.invoke(combatLogPlugin);

            if (combatManager == null) {
                return false;
            }

            Method isInCombatMethod = combatManager.getClass().getMethod("isInCombat", Player.class);
            Object result = isInCombatMethod.invoke(combatManager, player);

            return result instanceof Boolean value && value;
        } catch (Exception exception) {
            getLogger().warning("Could not check CombatLog status for " + player.getName() + ": " + exception.getMessage());
            return false;
        }
    }

    private boolean checkPvpCommandCooldown(Player player) {
        int cooldownSeconds = getConfig().getInt("settings.pvp-command-cooldown-seconds", 0);

        if (cooldownSeconds <= 0) {
            return true;
        }

        if (player.hasPermission("togglepvp.cooldown.bypass")) {
            return true;
        }

        long now = System.currentTimeMillis();
        long cooldownMillis = cooldownSeconds * 1000L;
        long lastUsed = pvpCommandCooldown.getOrDefault(player.getUniqueId(), 0L);
        long remainingMillis = cooldownMillis - (now - lastUsed);

        if (remainingMillis > 0) {
            long remainingSeconds = (remainingMillis + 999L) / 1000L;

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("%time%", String.valueOf(remainingSeconds));

            sendMessage(player, "messages.command-cooldown", placeholders);
            return false;
        }

        pvpCommandCooldown.put(player.getUniqueId(), now);
        return true;
    }

    public String getPlaceholderStatus(UUID uuid) {
        boolean enabled = isPvpEnabled(uuid);

        return getConfig().getString(
                enabled ? "placeholders.status-on" : "placeholders.status-off",
                enabled ? "ON" : "OFF"
        );
    }

    public String getPlaceholderStatusColored(UUID uuid) {
        boolean enabled = isPvpEnabled(uuid);

        String value = getConfig().getString(
                enabled ? "placeholders.status-colored-on" : "placeholders.status-colored-off",
                enabled ? "&aON" : "&cOFF"
        );

        return color(value);
    }

    public int getPlaceholderValue(UUID uuid) {
        return isPvpEnabled(uuid)
                ? getConfig().getInt("belowname.enabled-score", 1)
                : getConfig().getInt("belowname.disabled-score", 0);
    }

    private void setupDataFile() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        dataFile = new File(getDataFolder(), "data.yml");

        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException exception) {
                getLogger().severe("Could not create data.yml!");
                exception.printStackTrace();
            }
        }

        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    private void loadPlayerData() {
        pvpStatus.clear();

        if (!getConfig().getBoolean("settings.save-player-status", true)) {
            return;
        }

        ConfigurationSection playersSection = dataConfig.getConfigurationSection("players");

        if (playersSection == null) {
            return;
        }

        for (String uuidString : playersSection.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidString);
                boolean enabled = playersSection.getBoolean(uuidString + ".pvp", getDefaultPvpStatus());
                pvpStatus.put(uuid, enabled);
            } catch (IllegalArgumentException exception) {
                getLogger().warning("Invalid UUID found in data.yml: " + uuidString);
            }
        }
    }

    private void savePlayerData() {
        if (dataConfig == null || dataFile == null) {
            return;
        }

        if (!getConfig().getBoolean("settings.save-player-status", true)) {
            return;
        }

        dataConfig.set("players", null);

        for (Map.Entry<UUID, Boolean> entry : pvpStatus.entrySet()) {
            dataConfig.set("players." + entry.getKey() + ".pvp", entry.getValue());
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException exception) {
            getLogger().severe("Could not save data.yml!");
            exception.printStackTrace();
        }
    }

    private void reloadPlugin() {
        reloadConfig();

        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        if (getConfig().getBoolean("settings.save-player-status", true)) {
            loadPlayerData();
        }

        attackMessageCooldown.clear();
        pvpCommandCooldown.clear();

        setupBelowNameObjective();
        updateBelowNameForEveryone();
    }

    private void setupBelowNameObjective() {
        if (!getConfig().getBoolean("belowname.enabled", true)) {
            removeBelowNameObjective();
            return;
        }

        Scoreboard scoreboard = getMainScoreboard();

        if (scoreboard == null) {
            getLogger().warning("Could not access the main scoreboard.");
            return;
        }

        String objectiveName = getBelowNameObjectiveName();

        if (activeBelowNameObjectiveName != null && !activeBelowNameObjectiveName.equalsIgnoreCase(objectiveName)) {
            Objective oldObjective = scoreboard.getObjective(activeBelowNameObjectiveName);

            if (oldObjective != null) {
                oldObjective.unregister();
            }
        }

        Objective objective = scoreboard.getObjective(objectiveName);

        Component displayName = LegacyComponentSerializer.legacyAmpersand()
                .deserialize(getConfig().getString("belowname.display-name", "&cPvP"));

        if (objective == null) {
            objective = scoreboard.registerNewObjective(objectiveName, Criteria.DUMMY, displayName);
        } else {
            objective.displayName(displayName);
        }

        objective.setDisplaySlot(DisplaySlot.BELOW_NAME);
        activeBelowNameObjectiveName = objectiveName;
    }

    private void updateBelowNameForEveryone() {
        if (!getConfig().getBoolean("belowname.enabled", true)) {
            return;
        }

        Scoreboard scoreboard = getMainScoreboard();

        if (scoreboard == null) {
            return;
        }

        Objective objective = scoreboard.getObjective(getBelowNameObjectiveName());

        if (objective == null) {
            setupBelowNameObjective();
            objective = scoreboard.getObjective(getBelowNameObjectiveName());
        }

        if (objective == null) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            int score = getPlaceholderValue(player.getUniqueId());
            objective.getScore(player.getName()).setScore(score);
        }
    }

    private void removeBelowNameObjective() {
        Scoreboard scoreboard = getMainScoreboard();

        if (scoreboard == null) {
            return;
        }

        String objectiveName = activeBelowNameObjectiveName != null
                ? activeBelowNameObjectiveName
                : getBelowNameObjectiveName();

        Objective objective = scoreboard.getObjective(objectiveName);

        if (objective != null) {
            objective.unregister();
        }

        activeBelowNameObjectiveName = null;
    }

    private Scoreboard getMainScoreboard() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();

        if (manager == null) {
            return null;
        }

        return manager.getMainScoreboard();
    }

    private String getBelowNameObjectiveName() {
        String name = getConfig().getString("belowname.objective-name", "pvpstatus");

        if (name == null || name.isBlank()) {
            return "pvpstatus";
        }

        return name.trim();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("togglepvp.reload")) {
                sendMessage(sender, "messages.no-permission", null);
                return true;
            }

            reloadPlugin();
            sendMessage(sender, "messages.reload", null);
            return true;
        }

        if (!(sender instanceof Player player)) {
            sendMessage(sender, "messages.players-only", null);
            return true;
        }

        if (!player.hasPermission("togglepvp.use")) {
            sendMessage(player, "messages.no-permission", null);
            return true;
        }

        boolean pvpChangeCommand = isPvpChangeCommand(args);

        if (pvpChangeCommand) {
            if (isPvpToggleBlockedInCombat(player)) {
                sendMessage(player, "messages.cannot-toggle-in-combat", null);
                return true;
            }

            if (!checkPvpCommandCooldown(player)) {
                return true;
            }
        }

        if (args.length == 0) {
            boolean newStatus = !isPvpEnabled(player.getUniqueId());
            setPvpStatus(player, newStatus);

            sendMessage(player, newStatus ? "messages.pvp-enabled" : "messages.pvp-disabled", null);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "on", "enable", "enabled" -> {
                setPvpStatus(player, true);
                sendMessage(player, "messages.pvp-enabled", null);
                return true;
            }

            case "off", "disable", "disabled" -> {
                setPvpStatus(player, false);
                sendMessage(player, "messages.pvp-disabled", null);
                return true;
            }

            case "status", "check" -> {
                sendMessage(
                        player,
                        isPvpEnabled(player.getUniqueId())
                                ? "messages.pvp-status-enabled"
                                : "messages.pvp-status-disabled",
                        null
                );
                return true;
            }

            default -> {
                sendMessage(player, "messages.usage", null);
                return true;
            }
        }
    }

    private void sendCooldownMessage(Player player, String path, Map<String, String> placeholders) {
        int cooldownSeconds = getConfig().getInt("settings.attack-message-cooldown-seconds", 2);

        if (cooldownSeconds <= 0) {
            sendMessage(player, path, placeholders);
            return;
        }

        long now = System.currentTimeMillis();
        long cooldownMillis = cooldownSeconds * 1000L;
        long lastMessage = attackMessageCooldown.getOrDefault(player.getUniqueId(), 0L);

        if (now - lastMessage >= cooldownMillis) {
            attackMessageCooldown.put(player.getUniqueId(), now);
            sendMessage(player, path, placeholders);
        }
    }

    private void sendMessage(CommandSender sender, String path, Map<String, String> placeholders) {
        String message = getConfig().getString(path);

        if (message == null || message.isBlank()) {
            return;
        }

        String prefix = getConfig().getString("messages.prefix", "");

        message = message.replace("%prefix%", prefix);

        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace(entry.getKey(), entry.getValue());
            }
        }

        sender.sendMessage(color(message));
    }

    public String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }

        List<String> completions = new ArrayList<>();

        if (sender.hasPermission("togglepvp.use")) {
            completions.add("on");
            completions.add("off");
            completions.add("status");
        }

        if (sender.hasPermission("togglepvp.reload")) {
            completions.add("reload");
        }

        String input = args[0].toLowerCase(Locale.ROOT);
        completions.removeIf(option -> !option.toLowerCase(Locale.ROOT).startsWith(input));

        return completions;
    }
}
