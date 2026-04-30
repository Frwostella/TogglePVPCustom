package jre.frwostella.togglePVPCustom;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Relational;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.UUID;

public final class TogglePVPExpansion extends PlaceholderExpansion implements Relational {

    private final TogglePVPCustom plugin;

    public TogglePVPExpansion(TogglePVPCustom plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "togglepvp";
    }

    @Override
    public String getAuthor() {
        if (plugin.getDescription().getAuthors().isEmpty()) {
            return "Frwostella";
        }

        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    /*
     * Normal placeholders:
     *
     * %togglepvp_status%
     * %togglepvp_status_colored%
     * %togglepvp_value%
     * %togglepvp_enabled%
     *
     * Use these for the player's own scoreboard/sidebar.
     */
    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null || player.getUniqueId() == null) {
            return "";
        }

        return parseStatus(player.getUniqueId(), params);
    }

    /*
     * Relational placeholders:
     *
     * %rel_togglepvp_status%
     * %rel_togglepvp_status_colored%
     * %rel_togglepvp_value%
     * %rel_togglepvp_enabled%
     *
     * Use these for nametags, belowname plugins, TAB layouts,
     * or anything that shows another player's status.
     *
     * IMPORTANT:
     * We return the TARGET player's PvP status, not the viewer's.
     */
    @Override
    public String onPlaceholderRequest(Player viewer, Player target, String params) {
        if (target == null) {
            return "";
        }

        return parseStatus(target.getUniqueId(), params);
    }

    private String parseStatus(UUID uuid, String params) {
        if (params == null) {
            return null;
        }

        String id = params.toLowerCase(Locale.ROOT);

        return switch (id) {
            case "status" -> plugin.getPlaceholderStatus(uuid);

            case "status_colored", "colored_status" -> plugin.getPlaceholderStatusColored(uuid);

            case "value", "belowname", "score" -> String.valueOf(plugin.getPlaceholderValue(uuid));

            case "enabled" -> String.valueOf(plugin.isPvpEnabled(uuid));

            default -> null;
        };
    }
}