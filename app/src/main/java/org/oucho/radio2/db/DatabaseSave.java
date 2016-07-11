package org.oucho.radio2.db;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class DatabaseSave {

    private final SQLiteDatabase mDb;
    private Exporter mExporter;


    public DatabaseSave(SQLiteDatabase db, String destXml) {
        mDb = db;

        try {
            File myFile = new File(destXml);
            //noinspection ResultOfMethodCallIgnored
            myFile.createNewFile();

            FileOutputStream fOut = new FileOutputStream(myFile);
            BufferedOutputStream bos = new BufferedOutputStream(fOut);

            mExporter = new Exporter(bos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void exportData() {


        try {
            mExporter.enTete();

            exportTable();

            mExporter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void exportTable() throws IOException {

        String sql = "select * from " + RadiosDatabase.TABLE_NAME;
        Cursor cur = mDb.rawQuery(sql, new String[0]);
        int numcols = cur.getColumnCount();

        cur.moveToFirst();


        while (cur.getPosition() < cur.getCount()) {
            mExporter.startRadio();

            String name;
            String val;
            for (int idx = 0; idx < numcols; idx++) {
                name = cur.getColumnName(idx);
                val = cur.getString(idx);
                mExporter.addRadio(name, val);
            }

            mExporter.endRadio();
            cur.moveToNext();
        }

        mExporter.fin();

        cur.close();
    }



    class Exporter {

        private static final String ENTETE = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" + "\n" + "<map>" + "\n" ;

        private static final String START_RADIO = "<radio>" + "\n";

        private static final String OPEN = "<";
        private static final String CLOSE = ">";

        private static final String END_RADIO = "</radio>" + "\n";

        private static final String FIN = "</map>";


        private final BufferedOutputStream mbufferos;


        public Exporter(BufferedOutputStream bos) {
            mbufferos = bos;
        }


        public void enTete() throws IOException {

                String stg = ENTETE;
                mbufferos.write(stg.getBytes());
        }


        public void startRadio() throws IOException {

                String stg = START_RADIO;
                mbufferos.write(stg.getBytes());
        }

        public void endRadio() throws IOException {

                String stg = END_RADIO;
                mbufferos.write(stg.getBytes());
        }

        public void addRadio(String name, String val) throws IOException {

                String stg = OPEN + name + CLOSE + val + OPEN + "/" + name + CLOSE + "\n";
                mbufferos.write(stg.getBytes());
        }

        public void fin() throws IOException {

            String stg = FIN;
            mbufferos.write(stg.getBytes());
        }

        public void close() throws IOException {
            if (mbufferos != null) {
                mbufferos.close();
            }
        }

    }

}
