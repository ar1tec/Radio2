package org.oucho.radio2.tunein.loaders;

import android.content.Context;
import android.content.Intent;
import android.text.Html;
import android.util.Log;
import android.widget.Toast;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static org.oucho.radio2.interfaces.RadioKeys.INTENT_TITRE;


public class TuneInLoader extends BaseLoader<List<String>> {

    private final String TAG = "TuneInLoader";

    private final Context mContext;

    private final String urlRadioTime;

    public TuneInLoader(Context context, String url) {
        super(context);
        this.mContext = context;
        this.urlRadioTime = url;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> loadInBackground() {

        List<String> liste = null;

        String langue = Locale.getDefault().getLanguage();
        String pays = Locale.getDefault().getCountry();

        try {

            Log.d(TAG, "loadInBackground(): " + urlRadioTime);

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(urlRadioTime)
                    .header("Accept-Language", langue + "-" + pays)
                    .build();
            Response response = client.newCall(request).execute();

            liste = parse(response.body().string());

        } catch (SocketTimeoutException e) {

            Toast.makeText(mContext, "Erreur de connexion: " + e, Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return liste;
    }


    private List<String> parse(String string) {

        // Parse header
        if (!string.contains("<opml version=\"1\">"))
            return null;

        String header = string;
        header = header.substring(header.indexOf("<head>") + 6, header.indexOf("</head>"));

        String titre;
        String status = header.substring(header.indexOf("<status>") + 8, header.indexOf("</status>"));

        if (status.equals("200") && header.contains("<title>")) {
            titre = header.substring(header.indexOf("<title>") + 7, header.indexOf("</title>"));
            Intent intent = new Intent();
            intent.setAction(INTENT_TITRE);
            intent.putExtra("titre", titre);
            mContext.sendBroadcast(intent);
        }

        if (status.equals("400")) {
            Log.e(TAG, "Error: " + header.substring(header.indexOf("<fault>") + 7, header.indexOf("</fault>")));
            Log.e(TAG, "Error code: " + header.substring(header.indexOf("<fault_code>") + 12, header.indexOf("</fault_code>")));
            Intent intent = new Intent();
            intent.setAction("org.oucho.radio2.INTENT_TITRE");
            intent.putExtra("Titre", "Error");
            mContext.sendBroadcast(intent);
            return null;
        }

        // Parse body
        List<String> liste = new ArrayList<>();

        String body = string;
        body = body.substring(body.indexOf("<body>") + 6, body.indexOf("</body>"));
        body = body.substring(2, body.length() - 1);

        final String[] lines = body.split("\r\n|\r|\n");

        for (String line : lines) {
            String type = line.replace("<outline ", "").replace("/>", "").replace("\n", "");
            liste.add(Html.fromHtml(type).toString());
        }

        return liste;
    }

}
