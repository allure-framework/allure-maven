import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.is
import static ru.yandex.qatools.matchers.nio.PathMatchers.exists

def capturedCommands = Paths.get(basedir.absolutePath, 'target', 'captured commands.txt')
def resultsDirectory = Paths.get(basedir.absolutePath, 'target', 'allure-results')
def reportDirectory = Paths.get(basedir.absolutePath, 'target', 'site', 'preserved-report')

def expected = ['command=generate', 'arg=generate', 'arg=--clean',
                "arg=${resultsDirectory.toAbsolutePath()}", 'arg=-o',
                "arg=${reportDirectory.toAbsolutePath()}", '---',
                'command=serve', 'arg=serve', "arg=${resultsDirectory.toAbsolutePath()}",
                '---'].collect { it.toString() }

assertThat(Files.readAllLines(capturedCommands, StandardCharsets.UTF_8), is(expected))
assertThat(reportDirectory.resolve('index.html'), exists())
