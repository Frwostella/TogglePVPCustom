# TogglePVPCustom

TogglePVPCustom is a simple Paper Minecraft plugin that lets players turn their PvP status on or off individually.

PvP only works when both players have PvP enabled. If one player has PvP disabled, they cannot attack or be attacked by other players.

The plugin also supports PlaceholderAPI, so PvP status can be displayed in scoreboards, TAB, nametags, or below-name displays.

TogglePVPCustom can also optionally hook into Frwostella's CombatLog plugin. When CombatLog is installed and the hook is enabled, players cannot turn off PvP while they are in combat.

---

## Features

- Players can toggle their own PvP status.
- Prevents attacking if the attacker has PvP disabled.
- Prevents attacking players who have PvP disabled.
- Saves player PvP status.
- Configurable messages.
- Reload command without restarting the server.
- PlaceholderAPI support.
- Optional native below-name scoreboard display.
- TAB belowname-objective compatible.
- Optional CombatLog support.
- Blocks `/pvp off` while a player is in combat if CombatLog is installed.
- Build profile support for Paper 1.21.11 and Paper 26.1+.

---

## Commands

| Command | Description |
|---|---|
| `/pvp` | Toggle your PvP status |
| `/pvp on` | Enable your PvP |
| `/pvp off` | Disable your PvP |
| `/pvp status` | Check your PvP status |
| `/pvp reload` | Reload the plugin config |

Alias:

```txt
/togglepvp
```

---

## Permissions

| Permission | Description | Default |
|---|---|---|
| `togglepvp.use` | Allows players to use `/pvp` | Everyone |
| `togglepvp.reload` | Allows players to reload the plugin | OP |

---

## Version Support

TogglePVPCustom can be built for both Paper 1.21.11 and newer Paper 26.1+ versions.

| Target | Java | Maven Profile |
|---|---|---|
| Paper 1.21.11 | Java 21 | `paper-1.21.11` |
| Paper 26.1+ | Java 25 | `paper-26.1-plus` |

Build for Paper 1.21.11:

```bash
mvn clean package
```

or:

```bash
mvn clean package -Ppaper-1.21.11
```

Build for Paper 26.1+:

```bash
mvn clean package -Ppaper-26.1-plus
```

The compiled jar will be inside the `target` folder.

---

## Optional Dependencies

These plugins are optional, but unlock extra features.

| Plugin | Required? | Link | Purpose |
|---|---|---|---|
| PlaceholderAPI | Optional | https://www.spigotmc.org/resources/placeholderapi.6245/ | Enables PvP status placeholders |
| CombatLog | Optional | https://github.com/Frwostella/CombatLogPlugin | Prevents players from disabling PvP while in combat |

In `plugin.yml`, the optional dependencies should be listed like this:

```yml
softdepend:
  - PlaceholderAPI
  - CombatLog
```

CombatLog is optional. TogglePVPCustom will still work without it.

---

## CombatLog Plugin

This plugin supports Frwostella's CombatLog plugin:

```txt
https://github.com/Frwostella/CombatLogPlugin
```

If CombatLog is installed, TogglePVPCustom can check if a player is currently combat tagged.

When enabled, players cannot run:

```txt
/pvp off
```

while they are in combat.

They can still use:

```txt
/pvp on
/pvp status
```

Example config:

```yml
combatlog-hook:
  enabled: true
  plugin-name: "CombatLog"
  block-disable-in-combat: true
```

Message:

```yml
messages:
  cannot-disable-pvp-in-combat: "%prefix%&cYou cannot disable PvP while you are in combat."
```

If you do not want this feature, set:

```yml
combatlog-hook:
  enabled: false
```

---

## PlaceholderAPI Placeholders

For the player's own status, use:

```txt
%togglepvp_status%
%togglepvp_status_colored%
%togglepvp_value%
%togglepvp_enabled%
```

Examples:

```txt
%togglepvp_status% = ON / OFF
%togglepvp_status_colored% = &aON / &cOFF
%togglepvp_value% = 1 / 0
%togglepvp_enabled% = true / false
```

For nametag or below-name plugins that support relational placeholders, use:

```txt
%rel_togglepvp_status%
%rel_togglepvp_status_colored%
%rel_togglepvp_value%
%rel_togglepvp_enabled%
```

Relational placeholders are useful when the display is showing another player's status instead of your own.

---

## TAB Belowname Setup

If you are using TAB for below-name status, disable the plugin's built-in belowname first in `plugins/TogglePVPCustom/config.yml`:

```yml
belowname:
  enabled: false
```

Then use this in TAB's config:

```yml
belowname-objective:
  enabled: true
  value: "%togglepvp_value%"
  title: ""
  fancy-value: "&cPVP: %togglepvp_status_colored%"
  fancy-value-default: "NPC"
  disable-condition: ""
```

After editing the configs, run:

```txt
/pvp reload
/papi reload
/tab reload
```

Do not put `%togglepvp_status%` in TAB's `title`, because that can show your own PvP status under other players. Use `fancy-value` instead.

---

## Native Belowname Setup

The plugin also has its own native Minecraft below-name display.

Native below-name objectives can only show numbers, so it will display something like:

```txt
1 PvP
0 PvP
```

Example config:

```yml
belowname:
  enabled: true
  objective-name: "pvpstatus"
  display-name: "&cPvP"
  enabled-score: 1
  disabled-score: 0
  remove-on-disable: true
```

Use this only if you are not using TAB's belowname-objective, because two plugins controlling belowname at the same time can conflict.

---

## Full Example Config

```yml
settings:
  default-pvp: true
  save-player-status: true
  attack-message-cooldown-seconds: 2

combatlog-hook:
  enabled: true
  plugin-name: "CombatLog"
  block-disable-in-combat: true

belowname:
  enabled: false
  objective-name: "pvpstatus"
  display-name: "&cPvP"
  enabled-score: 1
  disabled-score: 0
  remove-on-disable: true

placeholders:
  status-on: "ON"
  status-off: "OFF"

  status-colored-on: "&aON"
  status-colored-off: "&cOFF"

messages:
  prefix: "&8[&cPvP&8] "

  no-permission: "%prefix%&cYou do not have permission to use this command."
  players-only: "%prefix%&cOnly players can use this command."
  reload: "%prefix%&aConfiguration reloaded."

  pvp-enabled: "%prefix%&aYour PvP is now ENABLED."
  pvp-disabled: "%prefix%&cYour PvP is now DISABLED."
  pvp-status-enabled: "%prefix%&aYour PvP is currently ENABLED."
  pvp-status-disabled: "%prefix%&cYour PvP is currently DISABLED."

  your-pvp-disabled: "%prefix%&cYou cannot attack while your PvP is disabled."
  target-pvp-disabled: "%prefix%&cYou cannot attack &e%target% &cbecause their PvP is disabled."
  cannot-disable-pvp-in-combat: "%prefix%&cYou cannot disable PvP while you are in combat."

  usage: "%prefix%&cUsage: /pvp [on|off|status|reload]"
```

---

## Installation

1. Download or build the plugin `.jar`.
2. Put the `.jar` file into your server's `plugins` folder.
3. Install PlaceholderAPI if you want placeholder support.
4. Install CombatLog from https://github.com/Frwostella/CombatLogPlugin if you want to block `/pvp off` while players are in combat.
5. Restart your server.
6. Edit the config if needed.
7. Run:

```txt
/pvp reload
```

---

## Requirements

- Paper 1.21.11 with Java 21, or Paper 26.1+ with Java 25

Optional:

- PlaceholderAPI for placeholders
- CombatLog for combat-check support: https://github.com/Frwostella/CombatLogPlugin

---
