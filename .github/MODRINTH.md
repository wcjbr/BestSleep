# Modrinth Publishing

This repository can publish releases to Modrinth with GitHub Actions.

## Required GitHub settings

Add this repository secret:

- `MODRINTH_TOKEN`
  A Modrinth personal access token with permission to create project versions.

Add these repository variables:

- `MODRINTH_PROJECT_ID`
  Your Modrinth project id or slug.
- `MODRINTH_GAME_VERSIONS`
  Comma-separated Minecraft versions, for example `1.21.11`
- `MODRINTH_LOADERS`
  Comma-separated loaders, for example `paper,purpur,spigot`

## How publishing works

- `CI` runs on pushes, pull requests, tags, and manual dispatch.
- `Publish To Modrinth` runs when a GitHub Release is published.
- Manual publish is also available through `workflow_dispatch`.

## Recommended release flow

1. Push code to GitHub.
2. Create and push a tag like `v1.0.0`.
3. Create a GitHub Release for that tag.
4. When the release is published, GitHub Actions builds the jar and uploads it to Modrinth.

## Notes

- Release tags are converted from `v1.0.0` to Modrinth version number `1.0.0`.
- GitHub prereleases are published to Modrinth as `beta`.
- Normal GitHub releases are published to Modrinth as `release`.
