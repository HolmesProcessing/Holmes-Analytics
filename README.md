# Holmes-Analytics

*This project is currently under heavy development and is part of [Google Summer of Code 2017](https://developers.google.com/open-source/gsoc/).*


## Overview
The goal of this project is to implement a semi-generic interface that enables Holmes Processing to manage the execution of advanced statistical and machine learning analysis operations.


## Installation

### Manual (recommended)
1) Clone the Git Repository and Change Directory
```shell
$ git clone https://github.com/HolmesProcessing/Holmes-Analytics.git
$ cd Holmes-Analytics
```

2) Compile Holmes-Analytics using SBT to download all dependencies and compile the source into a working JAR file.
```shell
$ sbt assembly
```
The assembled jar file will be located in `./target/scala-2.11/Holmes-Analytics-assembly-0.1.jar`

3) Copy `config/analytics.conf.example` to `config/analytics.conf` and perform any adjustments to the configuration to match your environment and needs.

4) Execute via 
```shell
$ java -jar ./target/scala-2.11/Holmes-Analytics-assembly-0.1.jar ./config/analytics.conf
```

## Configuration
All necessary configuration can be done via the `config/analytics.conf` file.
