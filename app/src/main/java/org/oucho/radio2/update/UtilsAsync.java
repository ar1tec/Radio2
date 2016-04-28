package org.oucho.radio2.update;

import android.content.Context;
import android.os.AsyncTask;

import org.oucho.radio2.update.enums.AppUpdaterError;
import org.oucho.radio2.update.enums.UpdateFrom;
import org.oucho.radio2.update.objects.GitHub;
import org.oucho.radio2.update.objects.Update;

class UtilsAsync {

    static class LatestAppVersion extends AsyncTask<Void, Void, Update> {
        private final Context context;
        private final LibraryPreferences libraryPreferences;
        private final Boolean fromUtils;
        private final UpdateFrom updateFrom;
        private final GitHub gitHub;
        private final String xmlUrl;
        private final AppUpdater.LibraryListener listener;

        public LatestAppVersion(Context context, Boolean fromUtils, UpdateFrom updateFrom, GitHub gitHub, String xmlUrl, AppUpdater.LibraryListener listener) {
            this.context = context;
            this.libraryPreferences = new LibraryPreferences(context);
            this.fromUtils = fromUtils;
            this.updateFrom = updateFrom;
            this.gitHub = gitHub;
            this.xmlUrl = xmlUrl;
            this.listener = listener;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            if (UtilsLibrary.isNetworkAvailable(context)) {
                if (!fromUtils && !libraryPreferences.getAppUpdaterShow()) {
                    cancel(true);
                } else {
                    if (updateFrom == UpdateFrom.GITHUB && !GitHub.isGitHubValid(gitHub)) {
                        listener.onFailed(AppUpdaterError.GITHUB_USER_REPO_INVALID);
                        cancel(true);
                    } else if (updateFrom == UpdateFrom.XML && (xmlUrl == null || !UtilsLibrary.isStringAnUrl(xmlUrl))) {
                        listener.onFailed(AppUpdaterError.XML_URL_MALFORMED);
                        cancel(true);
                    }
                }
            } else {
                listener.onFailed(AppUpdaterError.NETWORK_NOT_AVAILABLE);
                cancel(true);
            }
        }

        @Override
        protected Update doInBackground(Void... voids) {

            try {

                if (updateFrom == UpdateFrom.XML) {
                    return UtilsLibrary.getLatestAppVersionXml(xmlUrl);
                } else {
                    return UtilsLibrary.getLatestAppVersionHttp(context, updateFrom, gitHub);
                }

            } catch (Exception e) {
                cancel(true);
            }

            return null;

        }

        @Override
        protected void onPostExecute(Update update) {
            super.onPostExecute(update);
            if (UtilsLibrary.isStringAVersion(update.getLatestVersion())) {
                listener.onSuccess(update);
            } else {
                listener.onFailed(AppUpdaterError.UPDATE_VARIES_BY_DEVICE);
            }
        }
    }

}
