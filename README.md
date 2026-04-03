MemBoost is a client-side Fabric mod for Minecraft `26.1.1`.

It adds in-game memory stats, a config screen, presets, and a few client-side cleanup actions that can run when memory usage gets high.

MemBoost is a monitoring and cleanup utility. It does not change server-side memory handling, and results depend on your settings, world, and mod setup.

## Features

- Memory HUD with heap usage, chunk count, packet activity, and cleanup stats
- Config screen with Mod Menu integration
- Presets for `Play`, `Observe`, and `Stress`
- Cleanup during world change, disconnect, and resource reload
- Configurable cleanup when memory usage gets high
- Optional temporary changes to render distance, simulation distance, and particle level
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

- Minecraft `26.1.1`
- Fabric Loader
- Fabric API

## Example Test

Test setup:

- Minecraft `26.1.1`
- Java `25`
- CPU: `AMD Ryzen 7 9700X`
- GPU: `Nvidia RTX 3060 Ti`
- RAM: `32 GB DDR5`
- JVM args: `-Xms256M -Xmx2048M`
- Render distance: `16`
- Simulation distance: `16`
- Test case: `Loaded a singleplayer world and flew 3000 blocks south`

Results from that setup:

### Sodium only

- Peak heap: `1702 MiB`

### Sodium + MemBoost

- Peak heap: `1579 MiB`
- Cleanup count: `12`
- MemBoost preset: `Stress`

These numbers are from one local test setup and are provided as example results, not as guaranteed results for every world, server, or modpack.

## Notes

This mod is client-side only and does not need to be installed on servers.

For singleplayer testing with aggressive settings, avoid extremely small heap sizes unless you are intentionally stress testing memory behavior.