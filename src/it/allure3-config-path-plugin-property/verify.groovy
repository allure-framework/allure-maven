import groovy.json.JsonSlurper
import java.nio.file.Paths

import static io.qameta.allure.maven.TestHelper.checkRegularReportMojoOnly
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.nullValue
import static org.hamcrest.Matchers.is

def basedirPath = basedir.absolutePath
checkRegularReportMojoOnly(Paths.get(basedirPath))

def config = new JsonSlurper().parse(Paths.get(basedirPath, 'target',
        'allure-maven', 'allure3', 'allurerc.json').toFile())

assertThat(config.plugins.'plugin-config'.options.enabled, is(true))
assertThat(config.plugins.'root-config', nullValue())
