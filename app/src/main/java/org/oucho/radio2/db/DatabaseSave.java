package org.oucho.radio2.db;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import org.oucho.radio2.R;
import org.oucho.radio2.interfaces.RadioKeys;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import static org.oucho.radio2.db.RadiosDatabase.DB_NAME;

public class DatabaseSave implements RadioKeys {


    public void importDB(Context context, String source) {


        try {

            String destination = String.valueOf(context.getDatabasePath(DB_NAME));

            File file = new File(source);

            if (file.exists()) {

                RadiosDatabase radiosDatabase = new RadiosDatabase(context);
                radiosDatabase.importExport(source, destination);


                Toast.makeText(context, context.getString(R.string.importer), Toast.LENGTH_SHORT).show();

            } else {
                Toast.makeText(context, context.getString(R.string.importer_erreur), Toast.LENGTH_SHORT).show();
            }

        } catch (IOException ignored) {}

    }



    public void exportDB(Context context) {


        try {

            String repDestination = Environment.getExternalStorageDirectory().toString() + "/Radio";

            File newRep = new File(repDestination);
            if (!newRep.exists()) {
                //noinspection ResultOfMethodCallIgnored
                newRep.mkdir();
            }

            String source = String.valueOf(context.getDatabasePath(DB_NAME));

            String destination = repDestination + "/WebRadioList.db";

            RadiosDatabase database = new RadiosDatabase(context);

            database.importExport(source, destination);

            Toast.makeText(context, context.getString(R.string.exporter), Toast.LENGTH_SHORT).show();

        } catch (IOException ignored) {}
    }




    static void copyFile(FileInputStream fromFile, FileOutputStream toFile) throws IOException {
        FileChannel fromChannel = null;
        FileChannel toChannel = null;
        try {
            fromChannel = fromFile.getChannel();
            toChannel = toFile.getChannel();
            fromChannel.transferTo(0, fromChannel.size(), toChannel);
        } finally {
            try {
                if (fromChannel != null) {
                    fromChannel.close();
                }
            } finally {
                if (toChannel != null) {
                    toChannel.close();
                }
            }
        }
    }


}

