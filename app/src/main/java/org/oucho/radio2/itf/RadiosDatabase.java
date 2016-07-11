package org.oucho.radio2.itf;


import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.oucho.radio2.MainActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class RadiosDatabase extends SQLiteOpenHelper {
	public static final String DB_NAME = "WebRadio";
	private static final int DB_VERSION = 1;
	
	public RadiosDatabase() {
		super(MainActivity.getContext(), DB_NAME, null, DB_VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE WebRadio (url TEXT PRIMARY KEY, name TEXT)");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// This constructor is intentionally empty, pourquoi ? parce que !
	}


	public boolean importExport(String source, String dest) throws IOException {

		// Close the SQLiteOpenHelper so it will commit the created empty database to internal storage.
		close();
		File newDb = new File(source);
		File oldDb = new File(dest);
		if (newDb.exists()) {
			copyFile(new FileInputStream(newDb), new FileOutputStream(oldDb));
			// Access the copied database so SQLiteHelper will cache it and mark it as created.
			getWritableDatabase().close();
			return true;
		}
		return false;
	}

	public static void copyFile(FileInputStream fromFile, FileOutputStream toFile) throws IOException {
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
