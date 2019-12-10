# ChangeLog

## [1.0.0](https://gitlab.fokus.fraunhofer.de/viaduct/piveau-exporting-hub/tags/1.0.0) (2019-11-08)

**Added:**
* `PIVEAU_LOG_LEVEL` for general log level configuration of the `io.piveau` package
* `PIVEAU_HUB_ADDRESS`, `PIVEAU_HUB_APIKEY` in case of missing endpoint in pipe segment config
* `PIVEAU_HUB_ADD_HASH` to add canonical hash if no hash is present
* Ability to export only the Datasets Metrics to the Hub

**Changed:**
* Requires now latest LTS Java 11
* Docker base image to openjdk:11-jre

**Fixed:**
* Update all dependencies

## [0.1.2](https://gitlab.fokus.fraunhofer.de/viaduct/piveau-exporting-hub/tags/0.1.2) (2019-06-21)

**Added:**
* buildInfo.json for build info via `/health` path
* config.schema.json

**Changed:**
* `PIVEAU_` prefix to logstash configuration environment variables

**Fixed:**
* encode identifier when delete

## [0.1.1](https://gitlab.fokus.fraunhofer.de/viaduct/piveau-exporting-hub/tags/0.1.1) (2019-05-17)

**Fixed:**
* Hub address structure reading in delete

## [0.1.0](https://gitlab.fokus.fraunhofer.de/viaduct/piveau-exporting-hub/tags/0.1.0) (2019-05-17)

**Changed:**
* Configuration structure of pipe segment configuration
* Readme

## [0.0.1](https://gitlab.fokus.fraunhofer.de/viaduct/piveau-exporting-hub/tags/0.0.1) (2019-05-03)
Initial release
