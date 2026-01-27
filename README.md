# EventNotifications

A multi-loader Minecraft mod/plugin that sends real-time notifications when server events occur. Supports ntfy, Discord, Slack, and generic webhooks.

## Supported Loaders

| Loader | Minecraft Version | Status |
|--------|-------------------|--------|
| Fabric | 1.21.11+          | Stable |
| NeoForge | 1.21.11+          | Stable |
| Paper/Spigot/Bukkit | 1.21.11+          | Stable |

## Features

### Supported Events

| Event | Description |
|-------|-------------|
| Server Startup | Server has started |
| Server Shutdown | Server is stopping |
| Player Connect | Player joined the server |
| Player Disconnect | Player left the server |
| Player Death | Player died (includes death message) |
| Player Advancement | Player earned an advancement |
| Player Banned | Player was banned |
| Player Kicked | Player was kicked |
| Player Pardoned | Player was unbanned |
| Player OP | Player was given operator status |
| Player De-OP | Player's operator status was removed |
| Player Whitelisted | Player was added to whitelist |
| Player Unwhitelisted | Player was removed from whitelist |
| Whitelist On | Server whitelist was enabled |
| Whitelist Off | Server whitelist was disabled |

### Notification Targets

- **ntfy** - Push notifications via [ntfy.sh](https://ntfy.sh) or self-hosted
- **Discord** - Webhook notifications with optional embeds
- **Slack** - Webhook notifications with optional attachments
- **Generic Webhook** - Configurable HTTP POST/PUT requests

## Installation

### Fabric

1. Install [Fabric Loader](https://fabricmc.net/use/) and [Fabric API](https://modrinth.com/mod/fabric-api)
2. Download `eventnotifications-fabric-x.x.x.jar` from [Releases](../../releases)
3. Place the JAR in your `mods/` folder
4. Start the server to generate config files
5. Configure `config/eventnotifications/config.yml`

### NeoForge

1. Install [NeoForge](https://neoforged.net/)
2. Download `eventnotifications-neoforge-x.x.x.jar` from [Releases](../../releases)
3. Place the JAR in your `mods/` folder
4. Start the server to generate config files
5. Configure `config/eventnotifications/config.yml`

### Paper/Spigot/Bukkit

1. Download `eventnotifications-paper-x.x.x.jar` from [Releases](../../releases)
2. Place the JAR in your `plugins/` folder
3. Start the server to generate config files
4. Configure `plugins/EventNotifications/config.yml`

## Configuration

### Main Config (`config.yml`)

```yaml
general:
  # Server name for notifications (uses MOTD if not set)
  server_name: "My Server"

targets:
  # ntfy target
  ntfy_main:
    type: ntfy
    enabled: true
    server: "https://ntfy.sh"
    topic: "my-minecraft-server"
    # Optional authentication
    auth_type: bearer
    auth_token: "tk_your_token_here"

  # Discord webhook
  discord_main:
    type: discord
    enabled: true
    webhook_url: "https://discord.com/api/webhooks/..."
    use_embeds: true

  # Slack webhook
  slack_main:
    type: slack
    enabled: true
    webhook_url: "https://hooks.slack.com/services/..."

  # Generic webhook
  webhook_custom:
    type: webhook
    enabled: true
    url: "https://example.com/webhook"
    method: "POST"
```

### Event Templates (`templates/default.yml`)

Customize notification messages with placeholders:

```yaml
server_startup:
  enabled: true
  title: "{{server_name}} - Started"
  message: "Server is now online!"
  color: "#2ecc71"          # Discord embed color
  tags: "white_check_mark"  # ntfy emoji tags
  priority: "high"          # ntfy priority

player_connect:
  enabled: true
  title: "{{server_name}} - Player Joined"
  message: "**{{player_name}}** joined the server"
  color: "#3498db"
  tags: "video_game,arrow_right"
  priority: "default"

player_death:
  enabled: true
  title: "{{server_name}} - Player Died"
  message: "{{death_message}}"
  color: "#e74c3c"
  tags: "skull"
  priority: "default"
```

### Available Placeholders

| Event | Placeholders |
|-------|--------------|
| All events | `{{server_name}}` |
| Player events | `{{player_name}}`, `{{player_uuid}}` |
| player_death | `{{death_message}}` |
| player_advancement | `{{advancement_title}}`, `{{advancement_description}}`, `{{advancement_message}}` |
| player_kicked | `{{reason}}` |
| player_banned | `{{banned_by}}`, `{{reason}}` |

## Building from Source

Requirements:
- Java 21+
- Gradle 9+

```bash
# Build all modules
./gradlew build

# Build specific loader
./gradlew :fabric:build
./gradlew :neoforge:build
./gradlew :paper:build
```

Output JARs are in:
- `fabric/build/libs/eventnotifications-fabric-*.jar`
- `neoforge/build/libs/eventnotifications-neoforge-*.jar`
- `paper/build/libs/eventnotifications-paper-*.jar`

## Development

### Project Structure

```
EventNotifications/
├── core/           # Shared loader-agnostic code
├── fabric/         # Fabric mod
├── neoforge/       # NeoForge mod
├── paper/          # Paper/Spigot plugin
└── gradle/         # Version catalog
```

### Local Testing with Docker

A Docker Compose setup is provided for testing:

```bash
# Test with Fabric
docker compose --profile fabric up

# Test with Paper
docker compose --profile paper up

# Test with NeoForge
docker compose --profile neoforge up
```

This starts a Minecraft server with ntfy for testing notifications.

## License

MIT

## Contributing

Contributions are welcome! Please open an issue or pull request.
