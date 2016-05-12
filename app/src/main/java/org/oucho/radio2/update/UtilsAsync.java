package org.oucho.radio2.update;

import android.content.Context;
import android.os.AsyncTask;

import org.oucho.radio2.update.objects.Update;

@SuppressWarnings("unused")
class UtilsAsync {

    static class LatestAppVersion extends AsyncTask<Void, Void, Update> {
        private final Context context;
        private final LibraryPreferences libraryPreferences;
        private final Boolean fromUtils;
        private final String xmlUrl;
        private final AppUpdater.LibraryListener listener;

        public LatestAppVersion(Context context, Boolean fromUtils, String xmlUrl, AppUpdater.LibraryListener listener) {
            this.context = context;
            this.libraryPreferences = new LibraryPreferences(context);
            this.fromUtils = fromUtils;
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
                    if (xmlUrl == null || !UtilsLibrary.isStringAnUrl(xmlUrl)) {
                        listener.onFailed();
                        cancel(true);
                    }
                }
            } else {
                listener.onFailed();
                cancel(true);
            }
        }

        @Override
        protected Update doInBackground(Void... voids) {

            try {

                    return UtilsLibrary.getLatestAppVersionXml(xmlUrl);

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
            }
        }
    }

}
