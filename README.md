# Allure Maven Plugin 

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.qameta.allure/allure-maven/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/io.qameta.allure/allure-maven) 
[![Build Status](https://ci.qameta.io/buildStatus/icon?job=allure-maven/master)](https://ci.qameta.io/job/allure-maven/job/master/)

This plugin generates Allure report by [existing XML files](https://github.com/allure-framework/allure-core/wiki#gathering-information-about-tests) during Maven build process.

## Getting Started

* Add following lines into your `pom.xml` build section:
```
<plugin>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-maven</artifactId>
    <version>2.9</version>
</plugin>
```

* `mvn clean test` - run your tests

You can generate a report using one of the following command:

* `mvn allure:serve`

Report will be generated into temp folder. Web server with results will start.

* `mvn allure:report`

Report will be generated tо directory: `target/site/allure-maven/index.html`


Additional information can be found [here](https://docs.qameta.io/allure).

## Links

* [Issues](https://github.com/allure-framework/allure-maven/issues)
* [Jenkins](https://ci.qameta.io/job/allure-maven/)
* [Releases](https://github.com/allure-framework/allure-maven/releases)

## Contacts
* Mailing list: [allure@qameta.io](mailto:allure@qameta.io)
* Gitter chat room: [https://gitter.im/allure-framework/allure-core](https://gitter.im/allure-framework/allure-core)
* StackOverflow tag: [Allure](http://stackoverflow.com/questions/tagged/allure)
