package org.oucho.radio2.db;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.oucho.radio2.interfaces.RadioKeys;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.oucho.radio2.db.DatabaseSave.copyFile;

class RadiosDatabase extends SQLiteOpenHelper implements RadioKeys {


	private static final int DB_VERSION = 2;


	RadiosDatabase(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE" + " " + TABLE_NAME + " " + "(url TEXT PRIMARY KEY, name TEXT, image BLOB)");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + "image BLOB");
	}


	void importExport(String source, String dest) throws IOException {

		// Close the SQLiteOpenHelper so it will commit the created empty database to internal storage.
		close();
		File newDb = new File(source);
		File oldDb = new File(dest);
		if (newDb.exists()) {

			copyFile(new FileInputStream(newDb), new FileOutputStream(oldDb));
			// Access the copied database so SQLiteHelper will cache it and mark it as created.
			getWritableDatabase().close();
		}
	}


}
