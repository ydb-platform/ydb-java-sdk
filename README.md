[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/ydb-platform/ydb-java-sdk/blob/master/LICENSE)
[![Maven metadata URL](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Ftech%2Fydb%2Fydb-sdk-parent%2Fmaven-metadata.xml)](https://mvnrepository.com/artifact/tech.ydb/ydb-sdk-parent)
[![Build](https://img.shields.io/github/actions/workflow/status/ydb-platform/ydb-java-sdk/build.yaml)](https://github.com/ydb-platform/ydb-java-sdk/actions/workflows/build.yaml)
[![CI](https://img.shields.io/github/actions/workflow/status/ydb-platform/ydb-java-sdk/ci.yaml?label=CI)](https://github.com/ydb-platform/ydb-java-sdk/actions/workflows/ci.yaml)
[![Codecov](https://img.shields.io/codecov/c/github/ydb-platform/ydb-java-sdk)](https://app.codecov.io/gh/ydb-platform/ydb-java-sdk)

The Java SDK for YDB enables Java developers to work with YDB.

* [YDB Documentation][ydb-docs]
* [YDB SDK Documentation][sdk-docs]

## Getting Started

#### Connection setup ####

Before you begin, you need to create a database and setup authorization. Please see the [Prerequisites][prerequisites] section of the connection guide in documentation for information on how to do that.

#### Minimum requirements ####

To use YDB Java SDK you will need **Java 1.8+**.

#### Install the SDK ####

The recommended way to use the YDB Java SDK in your project is to consume it from Maven.
Firstly you can import YDB Java BOM to specify correct versions of SDK modules.

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>tech.ydb</groupId>
            <artifactId>ydb-sdk-bom</artifactId>
            <version>2.3.23</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

After that you can specify the SDK modules that your project needs in the dependencies:
```xml
<dependencies>
    <!-- Scheme service client -->
    <dependency>
        <groupId>tech.ydb</groupId>
        <artifactId>ydb-sdk-scheme</artifactId>
    </dependency>

    <!-- Table service client -->
    <dependency>
        <groupId>tech.ydb</groupId>
        <artifactId>ydb-sdk-table</artifactId>
    </dependency>

    <!-- Query service client -->
    <dependency>
        <groupId>tech.ydb</groupId>
        <artifactId>ydb-sdk-query</artifactId>
    </dependency>

    <!-- Topic service client -->
    <dependency>
        <groupId>tech.ydb</groupId>
        <artifactId>ydb-sdk-topic</artifactId>
    </dependency>

    <!-- Coordination service client -->
    <dependency>
        <groupId>tech.ydb</groupId>
        <artifactId>ydb-sdk-coordination</artifactId>
    </dependency>
</dependencies>
```

## Examples ##

#### Using Maven ####

In [basic example][basic-example] folder there is simple example application that uses YDB Java SDK from Maven.
See the [Connect to a database][connect-to-a-database] section of the documentation for an instruction on how to setup and launch it.

#### Generic examples ####

In [examples][generic-examples] folder you can find more example applications with YDB Java SDK usage.

## How to build
To build the YDB Java SDK artifacts locally, see [Building YDB Java SDK](BUILD.md).

[ydb-docs]: https://ydb.tech/en/docs
[sdk-docs]: https://ydb.tech/en/docs/reference/ydb-sdk/
[prerequisites]: https://ydb.tech/en/docs/concepts/connect
[connect-to-a-database]: https://ydb.tech/en/docs/reference/ydb-sdk/example/java#init
[basic-example]: https://github.com/ydb-platform/ydb-java-examples/tree/master/basic_example
[generic-examples]: https://github.com/ydb-platform/ydb-java-examples
