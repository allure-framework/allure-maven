import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.is

def capturedArgs = Paths.get(basedir.absolutePath, 'target', 'allure3 serve args.txt')
def config = Paths.get(basedir.absolutePath, 'target', 'allure-maven', 'allure3',
        'allurerc.json')
def results = Paths.get(basedir.absolutePath, 'target', 'my results')
def reportDirectory = Paths.get(basedir.absolutePath, 'target', 'site', 'allure serve report')
def allureCli = Paths.get(basedir.absolutePath, '.allure install', 'allure-3.4.1',
        'node_modules', 'allure', 'cli.js').toAbsolutePath().toString()

def expected = [ "cli=${allureCli}", 'command=generate', 'arg=generate',
        "arg=${results.toAbsolutePath()}", 'arg=--config',
        "arg=${config.toAbsolutePath()}", '---',
        "cli=${allureCli}", 'command=open', 'arg=open',
        "arg=${reportDirectory.toAbsolutePath()}", 'arg=--config',
        "arg=${config.toAbsolutePath()}", '---' ].collect { it.toString() }
assertThat(Files.readAllLines(capturedArgs, StandardCharsets.UTF_8), is(expected))
