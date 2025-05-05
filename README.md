# TravellerPlus Minecraft Plugin

## Overview

TravellerPlus is a Spigot/Paper Minecraft plugin designed for enhanced world management and travel. It allows server administrators to create custom worlds (including void and arena types) and provides players with sophisticated ways to travel between allowed worlds, including configurable commands and special travel signs.

This project was developed iteratively based on specific feature requests.

## Features

*   **World Creation:**
    *   Create new worlds using the `/createworld` command.
    *   Supports different world types (e.g., normal, void, arena - requires appropriate generators).
*   **World Travel:**
    *   Travel between configured worlds using the `/travel <world_name>` command.
    *   **Travel Signs:**
        *   Admins can create special, unbreakable signs using `/gettravelsign <world_name>`.
        *   Signs display bold red text indicating the destination world.
        *   Players can right-click these signs to initiate travel.
        *   Signs retain their data when placed and are protected from being broken by players or explosions.
        *   Correctly handles long world names that might be truncated on the sign display.
*   **Teleport Countdown:**
    *   A 3-second countdown is initiated before teleporting via command or sign.
    *   Moving during the countdown cancels the teleport.
    *   Players with the `travellerplus.countdown.bypass` permission teleport instantly.
*   **Last Location Memory:**
    *   The plugin remembers the player's location when they leave a world via `/travel` or a travel sign.
    *   When returning to that world, the player is teleported back to their last known location.
    *   Location data is stored in `coords.yml` within the plugin's data folder.
*   **Configuration:**
    *   Define allowed worlds for travel in `config.yml`.
    *   Set custom spawn points per world.
    *   Designate specific worlds as OP-only.
*   **Reload Command:**
    *   Reload the plugin's configuration using `/tpreload`.

## Commands

*   `/createworld <world_name> [generator_type]` - Creates a new world. Requires OP.
*   `/travel <world_name>` - Travels to the specified allowed world.
*   `/gettravelsign <world_name>` - Gives the player a special travel sign item for the specified world. Requires `travellerplus.gettravelsign` permission.
*   `/tpreload` - Reloads the plugin configuration. Requires `travellerplus.reload` permission.

## Permissions

*   `travellerplus.travel` - Allows use of the `/travel` command (defaults to all players, but access to specific worlds depends on `config.yml`).
*   `travellerplus.createworld` - Allows use of the `/createworld` command (defaults to OP).
*   `travellerplus.gettravelsign` - Allows use of the `/gettravelsign` command (defaults to OP).
*   `travellerplus.reload` - Allows use of the `/tpreload` command (defaults to OP).
*   `travellerplus.sign.break` - Allows players to break protected travel signs (defaults to OP).
*   `travellerplus.countdown.bypass` - Allows players to bypass the teleport countdown (defaults to OP).

## Configuration (`config.yml`)

```yaml
# List of worlds players are allowed to travel between using /travel or signs
allowed-worlds:
  - world
  - world_nether
  - world_the_end
  - MyArena

# Custom spawn points for specific worlds (optional)
# If not defined, uses the world's default spawn point
custom-spawn-points:
  MyArena: # World name (case-insensitive)
    world: MyArena # World name (case-sensitive, must match actual world)
    x: 100.5
    y: 65.0
    z: -50.5
    yaw: 90.0
    pitch: 0.0

# Worlds that require OP status to travel to (optional)
op-only-worlds:
  - admin_world

# Settings for world creation (if using custom generators)
world-generators:
  void: VoidGenerator # Example, ensure your server has this generator
  arena: ArenaGenerator # Example
```

## Installation

1.  Ensure your server is running Java 21 and a compatible version of Spigot, Paper, or a derivative.
2.  Place the `TravellerPlus-X.X.X.jar` file into your server's `plugins` folder.
3.  Restart or reload your server.
4.  Configure `config.yml` in the `plugins/TravellerPlus` folder as needed.
5.  Use `/tpreload` to apply configuration changes.

