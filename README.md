# BestSleep

[中文文档](docs/zh-cn.md) | [English Docs](docs/en-us.md)

BestSleep is a Paper plugin for coordinated night sleeping across Java Edition and Bedrock Edition players.

It supports:

- Paper `Dialog` UI for Java Edition players
- Floodgate / Geyser form UI for Bedrock Edition players
- Vanilla `playersSleepingPercentage` compatibility
- Teleporting only approved players back to their own beds
- Bed occupancy checks before forced sleep
- Configurable request cooldown
- Continuous title updates with current Minecraft time
- Faster night skip when more approved players are sleeping
- Built-in `zh_cn` and `en_us` message bundles

## Quick Start

1. Build the plugin:

```bash
mvn clean package
```

2. Or use the local dev runner:

```bash
./run.sh
```

3. Put the built jar from `target/` into your Paper server `plugins/` directory.

## Commands

- `/bestsleep`
- `/bestsleep request`
- `/bestsleep accept`
- `/bestsleep deny`
- `/bestsleep cancel`

Aliases:

- `/sleepvote`
- `/groupsleep`

## Configuration

Main config file: `src/main/resources/config.yml`

Important options:

- `default-locale`
- `request-cooldown-seconds`
- `messages.zh_cn`
- `messages.en_us`

## Documentation

- [Chinese documentation](docs/zh-cn.md)
- [English documentation](docs/en-us.md)

## Release Flow

This repository includes GitHub Actions for:

- CI build
- GitHub Release creation
- GitHub Release asset upload
- Modrinth publishing

See:

- [Modrinth and release workflow notes](.github/MODRINTH.md)
