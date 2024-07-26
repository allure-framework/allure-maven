[release]: https://github.com/allure-framework/allure-maven/releases/latest "Release"
[release-badge]: https://img.shields.io/github/release/allure-framework/allure-maven.svg

# Allure Maven Plugin

[![Build](https://github.com/allure-framework/allure-maven/actions/workflows/build.yml/badge.svg)](https://github.com/allure-framework/allure-maven/actions/workflows/build.yml) [![release-badge][]][release]

> This plugin generates Allure Report from [allure-results](https://allurereport.org/docs/how-it-works/).

[<img src="https://allurereport.org/public/img/allure-report.svg" height="85px" alt="Allure Report logo" align="right" />](https://allurereport.org "Allure Report")

- Learn more about Allure Report at https://allurereport.org
- 📚 [Documentation](https://allurereport.org/docs/) – discover official documentation for Allure Report
- ❓ [Questions and Support](https://github.com/orgs/allure-framework/discussions/categories/questions-support) – get help from the team and community
- 📢 [Official annoucements](https://github.com/orgs/allure-framework/discussions/categories/announcements) – be in touch with the latest updates
- 💬 [General Discussion ](https://github.com/orgs/allure-framework/discussions/categories/general-discussion) – engage in casual conversations, share insights and ideas with the community

---

## Getting Started

* Add following lines into your `pom.xml` build section:
```
<plugin>
	<groupId>io.qameta.allure</groupId>
	<artifactId>allure-maven</artifactId>
	<version>2.12.0</version>
</plugin>
```

* `mvn clean test` - run your tests

You can generate a report using one of the following command:

* `mvn allure:serve`

Report will be generated into temp folder. Web server with results will start.

* `mvn allure:report`

Report will be generated tо directory: `target/site/allure-maven/index.html`

## Configuration

You can configure Allure Report version by using `reportVersion` property:
```
<plugin>
	<groupId>io.qameta.allure</groupId>
	<artifactId>allure-maven</artifactId>
	<version>2.12.0</version>
	<configuration>
		<reportVersion>2.30.0</reportVersion>
	</configuration>
</plugin>
```

Additional information can be found [on official website](https://allurereport.org/).
