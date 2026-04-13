import java.nio.file.Paths

import static io.qameta.allure.maven.Allure3SetupHelper.prepareFakeInstallRuntime
import static io.qameta.allure.maven.Allure3SetupHelper.prepareFakePackageArchive

def basedirPath = basedir.toPath().toAbsolutePath()
prepareFakeInstallRuntime(
        basedirPath.resolve('.allure install'),
        basedirPath.resolve(Paths.get('target', 'allure3 package install args.txt'))
)
prepareFakePackageArchive(
        basedirPath.resolve(Paths.get('packages', 'custom-allure.tgz'))
)
