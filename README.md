
The Java SDK for YDB enables Java developers to work with YDB.

* [YDB Documentation][ydb-docs]
* [YDB SDK Documentation][sdk-docs]

## Getting Started

#### Connection setup ####

Before you begin, you need to create a database and setup authorization. Please see the [Prerequisites][prerequisites] section of the connection guide in documentation for information on how to do that.

#### Minimum requirements ####

To use YDB Java SDK you will need **Java 1.8+**. 

#### Install the SDK ####

The recommended way to use the YDB Java SDK in your project is to consume it from Maven. Specify the SDK Maven modules that your project needs in the
dependencies:

```xml
<dependencies>
    <dependency>
        <groupId>tech.ydb</groupId>
        <artifactId>ydb-sdk-core</artifactId>
        <version>1.13.5</version>
    </dependency>
    <dependency>
        <groupId>tech.ydb</groupId>
        <artifactId>ydb-sdk-table</artifactId>
        <version>1.13.5</version>
    </dependency>
    <dependency>
        <groupId>tech.ydb</groupId>
        <artifactId>ydb-sdk-auth-iam</artifactId>
        <version>1.13.5</version>
    </dependency>
</dependencies>
```

## Examples ##

#### Using Maven ####

In [examples/basic_example][basic-example] folder there is simple example application that uses YDB Java SDK from Maven.
See the [Connect to a database][connect-to-a-database] section of the documentation for an instruction on how to setup and launch it.

#### Generic examples ####

In [examples][generic-examples] folder you can find more example applications with YDB Java SDK usage.


[ydb-docs]: https://ydb.tech/en/docs
[sdk-docs]: https://ydb.tech/en/docsreference/ydb-sdk/
[prerequisites]: https://ydb.tech/en/docsconcepts/connect
[connect-to-a-database]: https://ydb.tech/en/docsreference/ydb-sdk/example/java#init
[basic_example]: https://github.com/ydb-platform/ydb-java-examples/tree/master/ydb-cookbook/basic_example
[generic-examples]: https://github.com/ydb-platform/ydb-java-examples/tree/master/ydb-cookbook
