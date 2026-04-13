import java.nio.file.Paths

import static io.qameta.allure.maven.Allure3SetupHelper.prepareFakeInstallRuntime

def basedirPath = basedir.toPath().toAbsolutePath()
prepareFakeInstallRuntime(
        basedirPath.resolve('.allure install'),
        basedirPath.resolve(Paths.get('target', 'allure3 install args.txt'))
)
