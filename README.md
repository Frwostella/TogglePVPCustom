# TogglePVPCustom

TogglePVPCustom is a simple Paper Minecraft plugin that lets players turn their PvP status on or off individually.

PvP only works when both players have PvP enabled. If one player has PvP disabled, they cannot attack or be attacked by other players.

The plugin also supports PlaceholderAPI, so PvP status can be displayed in scoreboards, TAB, nametags, or below-name displays.

TogglePVPCustom can optionally hook into Frwostella's CombatLog plugin. It also has its own local combat timer, so players cannot use `/pvp off` to escape combat after a real successful PvP hit.

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
- Local combat tracking after successful PvP hits.
- Blocks `/pvp`, `/pvp on`, and `/pvp off` while combat tagged.
- Prevents `/pvp off` from clearing CombatLog tags.
- Configurable `/pvp` command cooldown.
- Optional cooldown bypass permission.

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
| `togglepvp.reload` | Allows players to reload the plugin config | OP |
| `togglepvp.cooldown.bypass` | Allows players to bypass the `/pvp` command cooldown | False |

---

## Optional Dependencies

These plugins are optional, but unlock extra features.

| Plugin | Required? | Purpose |
|---|---|---|
| PlaceholderAPI | Optional | Enables PvP status placeholders |
| CombatLog | Optional | Lets TogglePVPCustom also check CombatLog combat status |

In `plugin.yml`, the optional dependencies should be listed like this:

```yml
softdepend:
  - PlaceholderAPI
  - CombatLog
```

CombatLog is optional. TogglePVPCustom will still work without it because it also tracks successful PvP hits locally.

---

## Combat Protection

TogglePVPCustom has two layers of combat protection:

1. Local combat tracking after successful PvP hits.
2. Optional CombatLog hook if Frwostella's CombatLog plugin is installed.

When combat protection is enabled, players cannot run PvP-changing commands while combat tagged:

```txt
/pvp
/pvp on
/pvp off
```

They can still use:

```txt
/pvp status
```

This prevents players from using `/pvp off` to escape combat.

Example config:

```yml
settings:
  pvp-toggle-block-seconds: 15

combatlog-hook:
  enabled: true
  plugin-name: "CombatLog"
  block-toggle-while-in-combat: true
```

Message:

```yml
messages:
  cannot-toggle-in-combat: "%prefix%&cYou cannot change your PvP status while in combat."
```

To disable combat-toggle blocking:

```yml
combatlog-hook:
  block-toggle-while-in-combat: false
```

To disable only the CombatLog hook:

```yml
combatlog-hook:
  enabled: false
```

---

## PvP Command Cooldown

You can add a cooldown to:

```txt
/pvp
/pvp on
/pvp off
```

Example:

```yml
settings:
  pvp-command-cooldown-seconds: 5
```

Set it to `0` to disable the cooldown:

```yml
settings:
  pvp-command-cooldown-seconds: 0
```

Cooldown message:

```yml
messages:
  command-cooldown: "%prefix%&cPlease wait &e%time%s &cbefore using this command again."
```

Players with this permission bypass the cooldown:

```txt
togglepvp.cooldown.bypass
```

By default, this permission is set to `false` in `plugin.yml`, so OPs do not bypass the cooldown unless you give them the permission manually.

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

For another player's status in nametags, TAB layouts, or relational placeholders, use:

```txt
%rel_togglepvp_status%
%rel_togglepvp_status_colored%
%rel_togglepvp_value%
%rel_togglepvp_enabled%
```

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
  # If true, new players have PvP enabled by default.
  default-pvp: true

  # Saves player PvP status into data.yml.
  save-player-status: true

  # Prevents spam when players keep trying to attack PvP-disabled players.
  attack-message-cooldown-seconds: 2

  # Cooldown before a player can use /pvp, /pvp on, or /pvp off again.
  # Set to 0 to disable.
  pvp-command-cooldown-seconds: 5

  # How long players cannot toggle PvP after a real successful PvP hit.
  # Match this with your CombatLog combat-time.
  pvp-toggle-block-seconds: 15

combatlog-hook:
  # Optional hook for Frwostella's CombatLog plugin.
  enabled: true

  # This must match the CombatLog plugin name in its plugin.yml.
  plugin-name: "CombatLog"

  # If true, players cannot change PvP status while they are combat tagged.
  block-toggle-while-in-combat: true

belowname:
  # This is the native Minecraft below-name objective.
  # Native below-name objectives can only show numbers, not text placeholders.
  enabled: true

  objective-name: "pvpstatus"

  # Shows like:
  # 1 PvP
  # 0 PvP
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

  cannot-toggle-in-combat: "%prefix%&cYou cannot change your PvP status while in combat."
  command-cooldown: "%prefix%&cPlease wait &e%time%s &cbefore using this command again."

  usage: "%prefix%&cUsage: /pvp [on|off|status|reload]"
```

---

## Example `plugin.yml`

```yml
name: TogglePVPCustom
version: 1.1.0
main: jre.frwostella.togglePVPCustom.TogglePVPCustom
api-version: '1.21'
author: Frwostella
description: Allows players to individually toggle their PvP status.
softdepend:
  - PlaceholderAPI
  - CombatLog

commands:
  pvp:
    description: Toggle your PvP status.
    usage: /pvp [on|off|status|reload]
    aliases:
      - togglepvp

permissions:
  togglepvp.use:
    description: Allows players to use /pvp.
    default: true

  togglepvp.reload:
    description: Allows reloading the plugin config.
    default: op

  togglepvp.cooldown.bypass:
    description: Allows players to bypass the /pvp command cooldown.
    default: false
```

---

## Installation

1. Download or build the plugin `.jar`.
2. Put the `.jar` file into your server's `plugins` folder.
3. Install PlaceholderAPI if you want placeholder support.
4. Install CombatLog if you want the plugin to also check CombatLog combat tags.
5. Restart your server after replacing the `.jar`.
6. Delete or manually update the old `plugins/TogglePVPCustom/config.yml` if new settings are missing.
7. Run:

```txt
/pvp reload
```

---

## Important Notes

After rebuilding the jar, you should restart the server.

Do not only use `/pvp reload` after changing the jar file.

If new config options are missing, either manually add them or delete the old config so the plugin can generate a new one.

The most important new config options are:

```yml
settings:
  pvp-command-cooldown-seconds: 5
  pvp-toggle-block-seconds: 15

combatlog-hook:
  block-toggle-while-in-combat: true
```

---

## Requirements

- Paper 1.21+
- Java 21+

Optional:

- PlaceholderAPI
- CombatLog

---
