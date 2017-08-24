package org.oucho.radio2.tunein.loaders;

import android.content.Context;
import android.content.Intent;
import android.text.Html;
import android.util.Log;
import android.widget.Toast;


import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class TuneInLoader extends BaseLoader<List<String>> {

    private final String TAG = "TuneInLoader";

    private final Context mContext;

    private final String urlRadioTime;

    public TuneInLoader(Context context, String url) {
        super(context);
        Log.d(TAG, "TuneInLoader: " + url);

        this.mContext = context;
        this.urlRadioTime = url;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> loadInBackground() {

        String data;
        List<String> liste = null;

        String langue = Locale.getDefault().getLanguage();
        String pays = Locale.getDefault().getCountry();

        try {

            Log.d(TAG, "loadInBackground(): " + urlRadioTime);

            URL url = new URL(urlRadioTime);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            //conn.setRequestProperty("User-Agent", "Radio/2.0 (Android 6.0; Intel Atom) Version/1.1.2 Radio/30");
            conn.setRequestProperty("Accept-Language", langue + "-" + pays);
            conn.setRequestMethod("GET");
            conn.setInstanceFollowRedirects(true);

            conn.setDoInput(true);
            conn.connect();

            InputStream stream = conn.getInputStream();

            data = convertStreamToString(stream);

          //  Log.d(TAG, "loadInBackground() data: " + data);


            liste = parse(data);

            conn.disconnect();
            stream.close();

        } catch (SocketTimeoutException e) {

            Toast.makeText(mContext, "Erreur de connexion: " + e, Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
        }


        return liste;
    }


    private String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    private List<String> parse(String string) {

        //
        // Parse header
        //
        if (!string.contains("<opml version=\"1\">"))
            return null;

        String head = string;

        head = head.substring(head.indexOf("<head>") + 6, head.indexOf("</head>"));


        String titre;
        String status = head.substring(head.indexOf("<status>") + 8, head.indexOf("</status>"));

        if (status.equals("200") && head.contains("<title>")) {
            titre = head.substring(head.indexOf("<title>") + 7, head.indexOf("</title>"));

          //  Log.d(TAG, "titre " + Html.fromHtml(titre));

            Intent intent = new Intent();
            intent.setAction("org.oucho.radio2.INTENT_TITRE");
            intent.putExtra("titre", titre);
            mContext.sendBroadcast(intent);
        }

        if (status.equals("400")) {
            Log.e(TAG, "Error: " + head.substring(head.indexOf("<fault>") + 7, head.indexOf("</fault>")));
            Log.e(TAG, "Error code: " + head.substring(head.indexOf("<fault_code>") + 12, head.indexOf("</fault_code>")));
            Intent intent = new Intent();
            intent.setAction("org.oucho.radio2.INTENT_TITRE");
            intent.putExtra("Titre", "Error");
            mContext.sendBroadcast(intent);
            return null;
        }


        //
        // Parse body
        //
        String body = string;

        body = body.substring(body.indexOf("<body>") + 6, body.indexOf("</body>"));

        body = body.substring(2, body.length() - 1);

        final String[] lines = body.split("\r\n|\r|\n");

        List<String> liste = new ArrayList<>();

        for (String line : lines) {

            String type = line.replace("<outline ", "").replace("/>", "").replace("\n", "");

            liste.add(Html.fromHtml(type).toString());

            // Log.e(TAG, "Parse body: " + Html.fromHtml(type).toString());

        }

        return liste;

    }

}
