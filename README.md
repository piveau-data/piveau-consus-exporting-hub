# exporting hub
Microservice for exporting to the hub.

The service is based on the [pipe-connector](https://github.com/piveau-data/piveau-pipe-connector) library. Any configuration applicable for the pipe-connector can also be used for this service.

## Table of Contents
1. [Build](#build)
1. [Run](#run)
1. [Docker](#docker)
1. [Configuration](#configuration)
    1. [Pipe](#pipe)
    1. [Data Info Object](#data-info-object)
    1. [Logging](logging)
1. [License](#license)

## Build
Requirements:
 * Git
 * Maven
 * Java

```bash
$ git clone https://github.com/piveau-data/piveau-consus-exporting-hub.git
$ cd piveau-consus-exporting-hub
$ mvn package
```

## Run

```bash
$ java -jar target/piveau-exporting-hub-far.jar
```

## Docker

Build docker image:

```bash
$ docker build -t piveau/piveau-exporting-hub .
```

Run docker image:

```bash
$ docker run -it -p 8080:8080 piveau/piveau-exporting-hub
```

## Configuration

### Pipe

_optional_

* `hub`

    Configuration object for the hub
    
* `hub/serviceName`

    The service name of the hub

Example
```json
{
  "hub": {
    "serviceName": "piveau-hub"
  }
}
```

### Data Info Object
The service understands the following properties as part of the data info object.
* `content` 

    The type of content of the data, if different from dataset. Possible values are `indentifierList` and `metrics` (optional, if content of the data is a dataset)
    
    
* `total`
 
    Total number of datasets (optional)

* `counter` 

    The number of this dataset (optional)

* `identifier` 

    The unique identifier of this dataset from the source side (required, when `content`does _not_ equal `identifierList`)

* `catalogue`

    The id of the target catalogue (required)
    
* `hash` 

    The hash value calculated at the source (optional)

Example
```json
{
  "total": 20300,
  "counter": 5,
  "identifier": "dataset_1",
  "catalogue": "govdata",
  "hash": "hexvalue"
}
```

### Environment
See also [pipe-connector](https://github.com/piveau-data/piveau-pipe-connector)

| Variable| Description | Default Value |
| :--- | :--- | :--- |
| `PIVEAU_HUB_ADDRESS` | The hub address used when no endpoint is set in the pipe segment configuration | none |
| `PIVEAU_HUB_APIKEY` | The hub api key to use when no endpoint is set in the pipe segment configuration | none |
| `PIVEAU_HUB_ADD_HASH` | Generate a canonical hash value if no hash is present | false |

### Logging
See [logback](https://logback.qos.ch/documentation.html) documentation for more details

| Variable| Description | Default Value |
| :--- | :--- | :--- |
| `PIVEAU_PIPE_LOG_APPENDER` | Configures the log appender for the pipe context | `STDOUT` |
| `PIVEAU_LOGSTASH_HOST`            | The host of the logstash service | `logstash` |
| `PIVEAU_LOGSTASH_PORT`            | The port the logstash service is running | `5044` |
| `PIVEAU_PIPE_LOG_PATH`     | Path to the file for the file appender | `logs/piveau-pipe.%d{yyyy-MM-dd}.log` |
| `PIVEAU_PIPE_LOG_LEVEL`    | The log level for the pipe context | `INFO` |
| `PIVEAU_LOG_LEVEL`    | The general log level for the `io.piveau` package | `INFO` |

## License

[Apache License, Version 2.0](LICENSE.md)
