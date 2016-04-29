package org.oucho.radio2.net;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;


public class WifiLocker {

   private static WifiLock lock = null;
   private static WifiManager manager = null;

   public static void lock(Context context, String app_name) {

      if ( manager == null )
         manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

      if ( manager != null && lock == null )
         lock = manager.createWifiLock(WifiManager.WIFI_MODE_FULL, app_name);

      if ( lock == null )
         return;

      if ( ! Connectivity.onWifi() ) {
          unlock();
          return;
      }

      if ( lock.isHeld() )
         return;

      lock.acquire();
   }

   public static void unlock() {

      if ( lock != null && lock.isHeld() )
         lock.release();

   }
}
