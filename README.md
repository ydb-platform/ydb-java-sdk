[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/ydb-platform/ydb-java-genproto/blob/main/LICENSE)
![Maven metadata URL](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Ftech%2Fydb%2Fydb-sdk-table%2Fmaven-metadata.xml)

The Java SDK for YDB enables Java developers to work with YDB.

* [YDB Documentation][ydb-docs]
* [YDB SDK Documentation][sdk-docs]

## Warning

This project is in development

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
        <version>2.0.0-RC1</version>
    </dependency>
    <dependency>
        <groupId>tech.ydb</groupId>
        <artifactId>ydb-sdk-table</artifactId>
        <version>2.0.0-RC1</version>
    </dependency>
</dependencies>
```

## Examples ##

#### Using Maven ####

In [basic example][basic-example] folder there is simple example application that uses YDB Java SDK from Maven.
See the [Connect to a database][connect-to-a-database] section of the documentation for an instruction on how to setup and launch it.

#### Generic examples ####

In [examples][generic-examples] folder you can find more example applications with YDB Java SDK usage.


[ydb-docs]: https://ydb.tech/en/docs
[sdk-docs]: https://ydb.tech/en/docs/reference/ydb-sdk/
[prerequisites]: https://ydb.tech/en/docs/concepts/connect
[connect-to-a-database]: https://ydb.tech/en/docs/reference/ydb-sdk/example/java#init
[basic-example]: https://github.com/ydb-platform/ydb-java-examples/tree/master/basic_example
[generic-examples]: https://github.com/ydb-platform/ydb-java-examples
