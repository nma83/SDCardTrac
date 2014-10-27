/*
 * SDCardTrac application - keeps track of the /sdcard usage
 * Copyright (C) 2012 Narendra M.A.
*/

package com.nma.sdcardtrac;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

// Helper class to access SQLite database
public class SQLiteHelper extends SQLiteOpenHelper {

	public SQLiteHelper(Context context, String name,
			CursorFactory factory, int version) {
		super(context, name, factory, version);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// TODO Auto-generated method stub
		db.execSQL(DatabaseManager.SCRIPT_CREATE_DATABASE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub

	}
}
