# Allure Maven Plugin 

[![release](http://github-release-version.herokuapp.com/github/allure-framework/allure-maven-plugin/release.svg?style=flat)](https://github.com/allure-framework/allure-maven-plugin/releases/latest) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/ru.yandex.qatools.allure/allure-maven-plugin/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/ru.yandex.qatools.allure/allure-maven-plugin) [![build](https://img.shields.io/teamcity/http/teamcity.qatools.ru/s/allure_maven_plugin_master_deploy.svg?style=flat)](http://teamcity.qatools.ru/viewType.html?buildTypeId=allure_maven_plugin_master_deploy&guest=1)

This plugin generates Allure report [from existing XML files](https://github.com/allure-framework/allure-core/wiki#gathering-information-about-tests) during Maven build.

## Installation
Simply add **allure-maven-plugin** to project reporting section: 

```xml
<project>
    ...
    <reporting>
        <excludeDefaults>true</excludeDefaults>
        <plugins>
            <plugin>
                <groupId>ru.yandex.qatools.allure</groupId>
                <artifactId>allure-maven-plugin</artifactId>
                <version>{latest.version}</version>
            </plugin>
        </plugins>
    </reporting>
    ...
</project>
```
You can find the latest version number [here](https://github.com/allure-framework/allure-maven-plugin/releases/latest).

**NOTE: required maven version 3.1.1 or above**

## Configuration
This plugin allows to select Allure version (1.3.0+) to be generated, [Ant pattern](https://ant.apache.org/manual/dirtasks.html) to search for XML files and report output path.
### Default Values
 * **Report Version**: `1.3.9`
 * **Report Path**: `target/site/allure-maven-plugin`
 * **Results Pattern**: `**/allure-results`
 
### Overriding Default Values
```xml
<project>
    ...
    <reporting>
        <plugins>
            <plugin>
                <groupId>ru.yandex.qatools.allure</groupId>
                <artifactId>allure-maven-plugin</artifactId>
                <version>${latest.version}</version>
                <configuration>
                    <resultsPattern>target/allure</resultsPattern>
                    <reportPath>target/report</reportPath>
                    <reportVersion>1.4.0</reportVersion>
                </configuration>
            </plugin>
        </plugins>
    </reporting>
    ...
</project>
```

### Maven Projects With Multiple Modules
When your Maven project contains several modules you need to include reporting section **only to root pom.xml**. In that case a separate report will be generated for every module and one report containing results for all modules will be generated in root build directory.

## Usage 

* Set up [Allure adapter](https://github.com/allure-framework/allure-core/wiki#gathering-information-about-tests) for test framework of your choice
* Run tests as usually: 
```bash
$ mvn clean test
```
* Build report:
```bash
$ mvn site
```
* Open report page: `target/site/allure-maven-plugin/index.html`

## Contact us
Mailing list: [allure@yandex-team.ru](mailto:allure@yandex-team.ru)
