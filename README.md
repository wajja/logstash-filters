# Logstash Europa Processor

This plugin will preprocess the Europa Data before sending to the logstash Output

## Europa Plugin

This plugin will process data for europa

### Configuration

```
filter {
  europafilter {
  	threads => 10
  }
}
```
## Initial configuration

Download the latest logstash and put it at the root of the project, it needs to be outside of this clone
git clone --branch 7.2 --single-branch https://github.com/elastic/logstash.git logstash-7.2

Build logstash
.\gradlew.bat assemble

Once the project is cloned create a gradle.properties at the root
Add the following path to the logstash you just built

LOGSTASH_CORE_PATH=../../logstash-7.2/logstash-core

On windows machine your JAR might be blocked by gradle. 

You can disable the gradle deamon by adding the following property in gradle.properties
org.gradle.daemon=false

## Run the project

At the root of the of the web fetcher

gradlew.bat clean
gradlew.bat gem

Install in logstash

/logstash-7.2.0/bin/logstash-plugin.bat install --no-verify --local /logstash-filter-europa-plugin/logstash-input-europafilter-1.0.0.gem

In logstash conf you can configure the tool

## TODO ##

Run logstash with your config

logstash.bat -f ..\logstash.conf

## Usefull commands

To refresh eclipse dependencies
gradlew cleanEclipse eclipse

then refresh inside eclipse.