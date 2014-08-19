# Allure Maven Plugin 
This plugin generates Allure report from existing XML files during Maven build.

## Installation
Simply add **allure-maven-plugin** to project reporting section: 

```xml
<project>

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
You can find latest version number [here](https://github.com/allure-framework/allure-maven-plugin/releases/latest).

## Usage 

* Run tests as usually: 
```bash
$ mvn clean test
```
* Build report:
```bash
$ mvn site
```
* Open report page: `target/site/allure-maven-plugin/index.html`

## Configuration
This plugin allows to select Allure version (1.3.0+) to be generated, Ant pattern to search for XML files and report output path.
### Default Values
 * **Report Version**: `1.3.9`
 * **Report Path**: `target/site/allure-maven-plugin`
 * **Results Pattern**: `**/allure-results`
 
### Overriding Default Values
```xml
<project>

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
```
