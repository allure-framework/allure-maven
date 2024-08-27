import java.nio.file.Paths

import static io.qameta.allure.maven.TestHelper.checkIndexHtml

def base = Paths.get(basedir.absolutePath, 'target', 'site')

checkIndexHtml(base.resolve('allure-maven-plugin'))
