# Allure Maven Plugin 
Use this plugin to generate Allure report from test results during Maven build.

## Installation
Simply add **allure-maven-plugin** to project reporting section: 

```xml
<project ... >

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
</project>
```
You can find latest version number [here](https://github.com/allure-framework/allure-maven-plugin/releases/latest)

## Configuration
### Defaults
 * **Report Version**: `1.3.9`
 * **Report Path**: `target/site/allure-maven-plugin`
 * **Results Pattern**: `**/allure-results`
 
### Allure Report Property

```xml
<project ... >

    <properties>
        <allure.version>1.4.0</allure.version>
    </properties>

</project>

```

### Other configuration

<project ... >

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
</project>

## Usage 

* Run test via maven: `mvn clean test`
* Build report: `mvn site`
* Open report `target/site/allure-maven-plugin/index.html`
