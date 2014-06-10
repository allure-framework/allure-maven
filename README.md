## Allure Maven Plugin 

Use this plugin to generate Allure report from test results during Maven build.

Usage:

```xml
 <reporting>
     <plugins>
         <plugin>
             <groupId>ru.yandex.qatools.allure</groupId>
             <artifactId>allure-maven-plugin</artifactId>
             <version>2.0-SNAPSHOT</version>
         </plugin>
     </plugins>
 </reporting>
```

Use ```allure.version``` property to specify Allure version.
