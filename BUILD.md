## Building the YDB Java SDK

### Requirements

* Java 8 or newer
* Maven 3.0.0 or newer

### Installing in local repo

You can install the SDK artifacts in your local maven cache by running the following command.
```
mvn clean install
```

If you don't need the test executions, just disable them
```
mvn clean install -DskipTests=true
```

