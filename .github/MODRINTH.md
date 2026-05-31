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
  Comma-separated exact Minecraft versions, for example `1.21.11`
- `MODRINTH_LOADERS`
  Comma-separated loaders, for example `paper,purpur,spigot`

If `MODRINTH_PROJECT_ID` or `MODRINTH_TOKEN` is missing, the release workflow will skip Modrinth publishing and still complete the GitHub Release steps.

## How publishing works

- `CI` runs on pushes, pull requests, tags, and manual dispatch.
- `Release` runs when a tag matching `v*` is pushed.
- `Release` can also run manually through `workflow_dispatch`.
- The release workflow builds the plugin, reads the version from `pom.xml`, creates or updates a GitHub Release with generated release notes, uploads the jar to that release, and then publishes the same jar to Modrinth.

## Recommended release flow

1. Push code to GitHub.
2. Create and push a tag like `v1.0.0` or `v0.1b`.
3. GitHub Actions creates or updates the GitHub Release, uploads the jar, and publishes to Modrinth.

## Notes

- The workflow derives the published version from the Git tag, for example `v1.0.0` becomes `1.0.0`.
- `pom.xml` version is used for Maven build metadata only and does not need to match the release tag.
- `-alpha` in the version becomes Modrinth `alpha`.
- `-beta` and `-rc` in the version become Modrinth `beta`.
- If you do not provide a manual changelog override, GitHub release notes are generated automatically through the GitHub Releases API.
