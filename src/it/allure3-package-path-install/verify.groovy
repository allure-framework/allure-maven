import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.hasItem
import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.not
import static ru.yandex.qatools.matchers.nio.PathMatchers.exists

def installDirectory = Paths.get(basedir.absolutePath, '.allure install')
def captureFile = Paths.get(basedir.absolutePath, 'target', 'allure3 package install args.txt')
def allureCli = installDirectory.resolve(Paths.get('allure-3.4.1', 'node_modules', 'allure',
        'cli.js'))
def packageArchive = Paths.get(basedir.absolutePath, 'packages', 'custom-allure.tgz')

assertThat(allureCli, exists())
def args = Files.readAllLines(captureFile, StandardCharsets.UTF_8)
assertThat(args, hasItem("arg=${packageArchive.toAbsolutePath()}".toString()))
assertThat(args, not(hasItem('arg=--registry')))
assertThat(args, not(hasItem('arg=allure@3.4.1')))
assertThat(args[2], is("arg=${installDirectory.resolve('allure-3.4.1').toAbsolutePath()}".toString()))
