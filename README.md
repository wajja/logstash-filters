# Logstash Fetchers Plugins

These tools have been created to crawl applications/sites to fetch data and send them to elastic for searching.
No processing of the data is done so it can be processed , as is, by the logstash filters.


## Initial configuration

Clone the latest logstash source

```
git clone https://github.com/elastic/logstash.git
```

Build logstash source

```
.\gradlew.bat assemble
```

Once the project is cloned create a gradle.properties at the root. 
Add the following path to the logstash you just built

```
LOGSTASH_CORE_PATH=PATH_TO_YOUR_LOGSTASH_SRC/logstash/logstash-core
```

On windows machine your JAR might be blocked by gradle.
You can disable the gradle deamon by adding the following property in gradle.properties

```
org.gradle.daemon=false
```

## Run the project

At the root of the of the web fetcher

```
gradlew.bat clean gem
```

Install in logstash

```
/logstash/bin/logstash-plugin.bat install --no-verify --local /logstash-***-plugin/***.gem
```

Run logstash with your config

```
logstash.bat -f ..\logstash.conf
```

## Usefull commands

To refresh eclipse dependencies

```
gradlew cleanEclipse eclipse
```

then refresh inside eclipse.