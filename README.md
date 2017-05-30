# Allure Maven Plugin 

[![Release](http://github-release-version.herokuapp.com/github/allure-framework/allure-maven/release.svg?style=flat)](https://github.com/allure-framework/allure-maven/releases/latest) 
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.qameta.allure/allure-maven/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/io.qameta.allure/allure-maven) 
[![Build Status](https://ci.qameta.io/buildStatus/icon?job=allure-maven/master)](https://ci.qameta.io/job/allure-maven/job/master/)

This plugin generates Allure report [from existing XML files](https://github.com/allure-framework/allure-core/wiki#gathering-information-about-tests) during Maven build.

## Getting Started

### Reporing section

* add following lines at your `pom.xml`
```
<reporting>
    <excludeDefaults>true</excludeDefaults>
    <plugins>
        <plugin>
            <groupId>io.qameta.allure</groupId>
            <artifactId>allure-maven</artifactId>
            <version>2.8</version>
        </plugin>
    </plugins>
</reporting>
```
* `mvn clean test` - run your tests
* `mvn sute` - generate report

Report will be generated tо directory: `target/site/allure-maven/index.html`

## Links

* [Issues](https://github.com/allure-framework/allure-maven/issues)
* [Jenkins](https://ci.qameta.io/job/allure-maven/)
* [Releases](https://github.com/allure-framework/allure-maven/releases)

## Contacts
* Mailing list: [allure@qameta.io](mailto:allure@qameta.io)
* Gitter chat room: [https://gitter.im/allure-framework/allure-core](https://gitter.im/allure-framework/allure-core)
* StackOverflow tag: [Allure](http://stackoverflow.com/questions/tagged/allure)
