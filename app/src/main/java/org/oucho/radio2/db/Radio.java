package org.oucho.radio2.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.Toast;

import org.oucho.radio2.R;
import org.oucho.radio2.interfaces.PlayableItem;
import org.oucho.radio2.interfaces.RadioKeys;

import java.util.ArrayList;
import java.util.List;

public class Radio implements PlayableItem, RadioKeys {
	private final String url;
	private final String name;
    private final byte[] img;

	public Radio(String url, String name, byte[] img) {
		this.url = url;
		this.name = name;
        this.img = img;
	}

	public String getUrl() {
		return url;
	}
	
	public String getName() {
		return name;
	}

	public byte[] getImg() {
        return img;
    }
	
	public static ArrayList<Radio> getRadios(Context context) {
		RadiosDatabase radiosDatabase = new RadiosDatabase(context);
		SQLiteDatabase db = radiosDatabase.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT url, name, image FROM " + TABLE_NAME + " ORDER BY NAME", null);
        ArrayList<Radio> radios = new ArrayList<>();
        while (cursor.moveToNext()) {
        	Radio radio = new Radio(cursor.getString(0), cursor.getString(1), cursor.getBlob(2));
        	radios.add(radio);
        }
        db.close();
        cursor.close();
        return radios;
	}


	public static List<String> getListe(Context context) {
		RadiosDatabase radiosDatabase = new RadiosDatabase(context);
		SQLiteDatabase db = radiosDatabase.getReadableDatabase();

		Cursor cursor = db.rawQuery("SELECT url FROM " + TABLE_NAME + " ORDER BY NAME", null);

		List<String> lst = new ArrayList<>();

		while (cursor.moveToNext()) {
			String radio = cursor.getString(0);
			lst.add(radio);
		}

		db.close();
		cursor.close();
		return lst;
	}

	public static void addRadio(Context context, Radio radio) {

        Log.d("Radio", "loading: " + radio);

		RadiosDatabase radiosDatabase = new RadiosDatabase(context);
		ContentValues values = new ContentValues();
		values.put("url", radio.url);
		values.put("name", radio.name);
        values.put("image", radio.img);

		try (SQLiteDatabase db = radiosDatabase.getWritableDatabase()) {
			db.insertOrThrow(TABLE_NAME, null, values);

            String text = context.getResources().getString(R.string.addRadio_fromApp, radio.getName());
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
		} catch (Exception e) {

            String text = context.getResources().getString(R.string.addRadio_error);
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show();

            Log.d("Radio", "Error: " + e);
		}
	}
	
	public static void deleteRadio(Context context, Radio radio) {
		RadiosDatabase radiosDatabase = new RadiosDatabase(context);
		SQLiteDatabase db = radiosDatabase.getWritableDatabase();
		db.delete(TABLE_NAME, "url = '" + radio.getUrl() + "'", null);
		db.close();
	}

	@Override
	public String getTitle() {
		return name;
	}

	@Override
	public String getPlayableUri() {
		return url;
	}

	@Override
    public byte[] getLogo() {
        return img;
    }

}
