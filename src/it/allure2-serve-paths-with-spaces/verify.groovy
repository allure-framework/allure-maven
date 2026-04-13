import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.is

def capturedArgs = Paths.get(basedir.absolutePath, 'target', 'captured args.txt')
def expectedResultsDirectory = Paths.get(basedir.absolutePath, 'target', 'my results')
        .toAbsolutePath()
        .toString()

assertThat(Files.readAllLines(capturedArgs, StandardCharsets.UTF_8),
        is(['serve', expectedResultsDirectory]))
