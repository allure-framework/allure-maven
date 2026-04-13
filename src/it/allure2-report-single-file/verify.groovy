import java.nio.file.Paths

import static io.qameta.allure.maven.TestHelper.checkSingleFile

def base = Paths.get(basedir.absolutePath, 'target', 'site')

checkSingleFile(base.resolve('allure-maven-plugin'))
