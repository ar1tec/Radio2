package org.oucho.radio2.update;

import android.content.Context;
import android.os.AsyncTask;

@SuppressWarnings("unused")
class CheckAsync {

    static class LatestAppVersion extends AsyncTask<Void, Void, Update> {
        private final Context context;
        private final String xmlUrl;
        private final AppUpdate.LibraryListener listener;

        public LatestAppVersion(Context context, String xmlUrl, AppUpdate.LibraryListener listener) {
            this.context = context;
            this.xmlUrl = xmlUrl;
            this.listener = listener;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            if (UtilsLibrary.isNetworkAvailable(context)) {

                if (xmlUrl == null || !UtilsLibrary.isStringAnUrl(xmlUrl)) {
                    listener.onFailed();
                    cancel(true);
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
