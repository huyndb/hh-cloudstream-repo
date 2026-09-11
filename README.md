# HH CloudStream Repo

Personal CloudStream repository containing two catalog providers:

- HHKungfu (`https://hhkungfu.ee`)
- HH3D (`https://www.hh3d.com`)

## Build compatibility

This repo follows the current `recloudstream/extensions` build setup:

- Kotlin Gradle plugin `2.3.0`
- CloudStream library `com.lagradost:cloudstream3:pre-release`
- Android Gradle Plugin `8.7.3`
- Java 17
- Gradle 8.12 in GitHub Actions

The current CloudStream app is built with Kotlin 2.4.0 metadata; Kotlin 2.3.x can consume that metadata while Kotlin 2.1.x cannot.

## Scope

The providers implement catalog/home/search/detail/episode indexing. Playback extraction is intentionally not implemented; `loadLinks()` returns `false` and does not bypass protected streaming, DRM, anti-bot, or token mechanisms.

## Publish

Push to `main`. GitHub Actions builds both `.cs3` plugins, generates `plugins.json`, and force-publishes the generated repository files to branch `builds`.

Repository URL for CloudStream:

```text
https://raw.githubusercontent.com/huyndb/hh-cloudstream-repo/builds/repo.json
```
