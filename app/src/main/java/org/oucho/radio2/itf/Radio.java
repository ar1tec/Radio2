package org.oucho.radio2.itf;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.oucho.radio2.db.RadiosDatabase;

import java.util.ArrayList;

public class Radio implements PlayableItem {
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
        Cursor cursor = db.rawQuery("SELECT url, name, image FROM WebRadio ORDER BY NAME", null);
        ArrayList<Radio> radios = new ArrayList<>();
        while (cursor.moveToNext()) {
        	Radio radio = new Radio(cursor.getString(0), cursor.getString(1), cursor.getBlob(2));
        	radios.add(radio);
        }
        db.close();
        cursor.close();
        return radios;
	}
	
	public static void addRadio(Context context, Radio radio) {
		RadiosDatabase radiosDatabase = new RadiosDatabase(context);
		ContentValues values = new ContentValues();
		values.put("url", radio.url);
		values.put("name", radio.name);
        values.put("image", radio.img);
		try (SQLiteDatabase db = radiosDatabase.getWritableDatabase()) {
			db.insertOrThrow("WebRadio", null, values);
		} catch (Exception ignored) {
		}
	}
	
	public static void deleteRadio(Context context, Radio radio) {
		RadiosDatabase radiosDatabase = new RadiosDatabase(context);
		SQLiteDatabase db = radiosDatabase.getWritableDatabase();
		db.delete("WebRadio", "url = '" + radio.getUrl() + "'", null);
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
