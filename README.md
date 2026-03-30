# MemBoost

MemBoost is a client-side Fabric mod for Minecraft `26.1` focused on memory diagnostics and adaptive cleanup.

It helps you monitor heap usage, react to memory pressure, and apply lightweight client-side cleanup without needing to install anything on a server.

## Features

- Live memory HUD with heap usage, chunk count, packet activity, and cleanup stats
- Config screen with Mod Menu integration
- Presets for `Play`, `Observe`, and `Stress`
- Memory pressure cleanup for transient client-side state
- World change, disconnect, and resource reload cleanup
- Adaptive memory controls for render distance, simulation distance, and particle level
- Client-side commands for stats, presets, HUD, debug logging, and config access
- Client-side only

## Presets

- `Play` - Balanced defaults for normal gameplay
- `Observe` - HUD and debug enabled for monitoring behavior
- `Stress` - Aggressive settings for low-memory testing

## Commands

- `/memboost` - Show a quick overview
- `/memboost stats` - Show current memory and cleanup stats
- `/memboost config` - Open the config screen
- `/memboost preset <play|observe|stress>` - Apply a preset
- `/memboost hud <on|off>` - Toggle the HUD
- `/memboost debug <on|off>` - Toggle debug logging
- `/memboost profile <safe|balanced|aggressive>` - Set the cleanup profile
- `/memboost interval <ticks>` - Set the sample interval
- `/memboost threshold <percent>` - Set the warning threshold
- `/memboost resetpeak` - Reset peak memory usage

## Requirements

- Minecraft `26.1`
- Fabric Loader
- Fabric API

## Notes

This mod is client-side only and does not need to be installed on servers.

For singleplayer testing with aggressive settings, avoid extremely small heap sizes unless you are intentionally stress testing memory behavior.
