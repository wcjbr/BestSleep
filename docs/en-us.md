# BestSleep Documentation

## Overview

BestSleep is a Paper plugin that lets players start a shared sleep request at night. Once the vanilla sleep percentage is satisfied, only the players who accepted the request are teleported back to their own beds and put to sleep. Time then advances toward day, and more sleeping players make the process faster.

The plugin is designed for mixed Java Edition and Bedrock Edition servers:

- Java players get a Paper dialog
- Bedrock players get a Floodgate / Geyser form

## Features

- Night-only sleep requests
- Uses vanilla `playersSleepingPercentage`
- Does not require unanimous approval
- Only approved players are teleported and slept
- Checks bed availability before teleporting and before forcing sleep
- Tracks temporary bed occupancy during managed sleep
- Configurable per-player cooldown for starting requests
- Realtime title updates showing Minecraft time
- Faster day advancement when more players are sleeping
- Built-in `en_us` and `zh_cn` i18n messages

## Commands

- `/bestsleep`
  Starts a sleep request
- `/bestsleep request`
  Starts a sleep request
- `/bestsleep accept`
  Accepts the current request
- `/bestsleep deny`
  Denies the current request
- `/bestsleep cancel`
  Cancels the request if you started it

Aliases:

- `/sleepvote`
- `/groupsleep`

## Requirements

- Paper `1.21.x`
- Java `21+`
- Floodgate if you want Bedrock player identity support
- Geyser if you want Bedrock players to join through the proxy bridge

## Installation

1. Build the plugin:

```bash
mvn clean package
```

2. Copy the built jar from `target/` into your Paper server `plugins/` directory.

3. Start the server once so Paper generates plugin data.

4. If you want Bedrock support, also install:

- Geyser-Spigot
- floodgate-spigot

## Local Development

This repository includes a local run script:

```bash
./run.sh
```

It does the following:

- builds the plugin with Maven
- copies the jar into `run/plugins/`
- clears the Paper remap cache for the plugin
- updates Geyser auth mode to offline if its config already exists
- starts the Paper server in `run/`

## Configuration

Main config:

```yml
default-locale: zh_cn
request-cooldown-seconds: 60
```

### `default-locale`

Fallback language used when a player locale does not match a defined message bundle.

Supported bundles currently included:

- `zh_cn`
- `en_us`

### `request-cooldown-seconds`

Cooldown for starting a new sleep request, per player.

### Message bundles

The `messages` section contains translated UI text and player-facing messages.

You can add more locales by following the same structure as:

- `messages.zh_cn`
- `messages.en_us`

## Sleep Flow

1. A player starts a sleep request at night.
2. Players in the same world are included as participants.
3. Every participant must have a usable respawn bed.
4. Java players receive a Paper dialog.
5. Bedrock players receive a Floodgate modal form.
6. When approvals reach the vanilla sleeping percentage threshold, the request enters advancing mode.
7. Only approved players are teleported to their own beds.
8. Bed occupancy is checked again before forced sleeping.
9. The world time advances toward day.
10. The request ends only when daytime is reached.

## Release and Publishing

The repository contains GitHub Actions workflows for:

- CI builds
- automatic GitHub Releases from tags
- release notes generation
- GitHub Release asset upload
- Modrinth publishing

Release publishing expects:

- `pom.xml` version to be a real release version, not `-SNAPSHOT`
- a tag matching the version, such as `v1.0.0`

Relevant files:

- `.github/workflows/ci.yml`
- `.github/workflows/release.yml`
- `.github/MODRINTH.md`
