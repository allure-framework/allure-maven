[release]: https://github.com/allure-framework/allure-maven/releases/latest "Release"
[release-badge]: https://img.shields.io/github/release/allure-framework/allure-maven.svg

# Allure Maven Plugin 

[![Build](https://github.com/allure-framework/allure-maven/actions/workflows/build.yml/badge.svg)](https://github.com/allure-framework/allure-maven/actions/workflows/build.yml) [![release-badge][]][release]

This plugin generates Allure report by [existing XML files](https://github.com/allure-framework/allure-core/wiki#gathering-information-about-tests) during Maven build process.

## Getting Started

* Add following lines into your `pom.xml` build section:
```
<plugin>
	<groupId>io.qameta.allure</groupId>
	<artifactId>allure-maven</artifactId>
	<version>2.10.0</version>
</plugin>
```

* `mvn clean test` - run your tests

You can generate a report using one of the following command:

* `mvn allure:serve`

Report will be generated into temp folder. Web server with results will start.

* `mvn allure:report`

Report will be generated tо directory: `target/site/allure-maven/index.html`

## Configuration

You can configurate allure version like here:
```
<plugin>
	<groupId>io.qameta.allure</groupId>
	<artifactId>allure-maven</artifactId>
	<version>2.10.0</version>
	<configuration>
		<reportVersion>2.14.0</reportVersion>
	</configuration>
</plugin>
```

Additional information can be found [here](https://docs.qameta.io/allure).

## Links

* [Issues](https://github.com/allure-framework/allure-maven/issues)
* [Releases](https://github.com/allure-framework/allure-maven/releases)

## Contacts
* Mailing list: [allure@qameta.io](mailto:allure@qameta.io)
* Gitter chat room: [https://gitter.im/allure-framework/allure-core](https://gitter.im/allure-framework/allure-core)
* StackOverflow tag: [Allure](http://stackoverflow.com/questions/tagged/allure)
