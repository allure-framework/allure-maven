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
```xml
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

### Selecting the runtime

By default, the plugin uses Allure 3.

`reportVersion` selects the runtime family:

- blank or unset: Allure 3 `3.4.1`
- `3.x`: Allure 3
- `2.x`: Allure 2 commandline

You can configure the version in the plugin configuration:

```xml
<plugin>
	<groupId>io.qameta.allure</groupId>
	<artifactId>allure-maven</artifactId>
	<version>2.12.0</version>
	<configuration>
		<reportVersion>3.4.1</reportVersion>
	</configuration>
</plugin>
```

Or from the command line:

```bash
mvn -Dreport.version=2.39.0 allure:report
```

### Allure 3 runtime provisioning

For Allure 3, the plugin provisions a private Node.js runtime and installs the Allure 3 package
into the plugin cache. It does not require a system-wide Node.js installation.

Default cache layout under `${project.basedir}/.allure`:

- `node-v24.14.1-<os>-<arch>`: private Node.js runtime
- `allure-3.4.1`: installed Allure 3 package
- `bin/allure` or `bin/allure.bat`: generated launcher used by the plugin

Relevant Allure 3 parameters:

- `allure.install.directory`
- `allure.node.version`
- `allure.node.download.url`
- `allure.npm.registry`
- `allure.package.path`
- `allure.config.path`

`allure.package.path` is an optional local `.tgz` or `.tar.gz` archive that is installed instead of
`allure@<reportVersion>`. This is mainly useful for tests and custom local package builds.

### Allure 2 compatibility

Allure 2 remains available when `reportVersion` is set to a `2.x` release.

The Allure 2-specific `allure.download.url` customization still applies only to the Allure 2 ZIP
flow. It is rejected for Allure 3.

### Allure 3 config support

For Allure 3, the plugin supports:

- auto-detected project-root config files:
  - `allurerc.js`
  - `allurerc.mjs`
  - `allurerc.cjs`
  - `allurerc.json`
  - `allurerc.yaml`
  - `allurerc.yml`
- explicit config via `allure.config.path`

JSON/YAML configs are merged into a generated config file, while JS/MJS/CJS configs are wrapped so
the plugin can still overlay the report output, report name, and `singleFile` setting.

### Allure 3 serve behavior

For Allure 3, `allure:serve` performs:

1. `allure generate <resultsDirs...> --config <generated config>`
2. `allure open <reportDir> --config <generated config>`

`allure.serve.port` is supported for Allure 3.

`allure.serve.host` is not supported for Allure 3. Use `reportVersion` `2.x` if host binding is
required.

Additional information can be found [on official website](https://allurereport.org/).
