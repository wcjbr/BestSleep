# Modrinth Publishing

This repository can build releases with GitHub Actions, upload the jar to GitHub Releases, and then publish the same file to Modrinth.

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
- `Release` runs when a tag matching `v*` is pushed.
- `Release` can also run manually through `workflow_dispatch`.
- The release workflow builds the plugin, reads the version from `pom.xml`, creates or updates a GitHub Release with generated release notes, uploads the jar to that release, and then publishes the same jar to Modrinth.

## Recommended release flow

1. Push code to GitHub.
2. Update `pom.xml` to the exact release version, for example `1.0.0`.
3. Create and push a tag like `v1.0.0`.
4. GitHub Actions creates or updates the GitHub Release, uploads the jar, and publishes to Modrinth.

## Notes

- The workflow requires the pushed tag to match the `pom.xml` version exactly, for example `pom.xml: 1.0.0` and tag `v1.0.0`.
- `-alpha` in the version becomes Modrinth `alpha`.
- `-beta` and `-rc` in the version become Modrinth `beta`.
- `-SNAPSHOT` versions are rejected for release publishing.
- If you do not provide a manual changelog override, GitHub release notes are generated automatically through the GitHub Releases API.
