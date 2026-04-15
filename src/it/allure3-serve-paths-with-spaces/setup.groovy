import java.nio.file.Paths

import static io.qameta.allure.maven.Allure3SetupHelper.prepareFakeReportRuntime

def basedirPath = basedir.toPath().toAbsolutePath()
prepareFakeReportRuntime(
        basedirPath.resolve('.allure install'),
        basedirPath.resolve(Paths.get('target', 'allure3 serve args.txt')),
        basedirPath.resolve(Paths.get('target', 'site', 'allure serve report')),
        false
)
