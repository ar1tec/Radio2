package org.oucho.radio2.update;

import java.net.URL;

class Update {
    private String version;
    private String releaseNotes;
    private URL apk;

    Update() {}

    String getLatestVersion() {
        return version;
    }

    void setLatestVersion(String latestVersion) {
        this.version = latestVersion;
    }

    String getReleaseNotes() {
        return releaseNotes;
    }

    void setReleaseNotes(String releaseNotes) {
        this.releaseNotes = releaseNotes;
    }

    URL getUrlToDownload() {
        return apk;
    }

    void setUrlToDownload(URL apk) {
        this.apk = apk;
    }
}
