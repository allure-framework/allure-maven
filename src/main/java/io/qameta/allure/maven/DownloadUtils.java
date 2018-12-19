package io.qameta.allure.maven;

/**
 * @author eroshenkoam (Artem Eroshenko).
 */
public final class DownloadUtils {

    private DownloadUtils() {
    }

    private static final String BINTRAY_TEMPLATE = "https://dl.bintray.com/qameta/generic/io/qameta/allure/allure/%s/allure-%s.zip";

    private static final String CENTRAL_TEMPLATE = "https://repo.maven.apache.org/maven2/io/qameta/allure/allure-commandline/%s/allure-commandline-%s.zip";

    public static String getAllureDownloadUrl(final String version, final String downloadUrl) {
        if (downloadUrl != null) {
            return downloadUrl;
        }
        if (versionCompare(version, "2.8.0") < 0) {
            return BINTRAY_TEMPLATE;
        } else {
            return CENTRAL_TEMPLATE;
        }
    }

    private static Integer versionCompare(String first, String second) {
        String[] firstVersions = first.split("\\.");
        String[] secondVersions = second.split("\\.");
        int i = 0;
        while (i < firstVersions.length && i < secondVersions.length && firstVersions[i].equals(secondVersions[i])) {
            i++;
        }
        if (i < firstVersions.length && i < secondVersions.length) {
            int diff = Integer.valueOf(firstVersions[i]).compareTo(Integer.valueOf(secondVersions[i]));
            return Integer.signum(diff);
        } else {
            return Integer.signum(firstVersions.length - secondVersions.length);
        }
    }




}
