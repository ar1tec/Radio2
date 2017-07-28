package org.oucho.radio2.net;

import org.oucho.radio2.utils.Playlist;

import java.net.URL;
import java.net.HttpURLConnection;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.List;
import java.util.ArrayList;

public class HttpGetter {

   public static List<String> httpGet(String url) {

      HttpURLConnection connection = null;
      List<String> lines = new ArrayList<>();

      try {

         URL urlRadio = new URL(url);
         connection = (HttpURLConnection) urlRadio.openConnection();

         if ( Playlist.isPlaylistMimeType(connection.getContentType()) ) {
            InputStream stream = new BufferedInputStream(connection.getInputStream());
            readStream(stream, lines);
         }
         connection.disconnect();
      } catch ( Exception e ) {

         if ( connection != null )
            connection.disconnect();
      }

      return lines;
   }

   private static void readStream(InputStream stream, List<String> lines) throws Exception {

      String line;
      BufferedReader buff = new BufferedReader(new InputStreamReader(stream));

      while ((line = buff.readLine()) != null)
         lines.add(line);

      stream.close();
   }


}
