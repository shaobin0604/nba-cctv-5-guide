package cn.yo2.aquarium.nbatvguide;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper {
	private static final String CLASSTAG = DatabaseHelper.class.getSimpleName();

	private static final String EXEC_SQL_PREFIX = "SQL EXEC -- ";

	private static final String DB_NAME = "nbatvguide.db";

	private static final int DB_VER = 2;

	static final String T_ITEMS = "items";
	
	static final String C_ID = "_id";
	static final String C_NUMBER = "number";
	static final String C_DATE = "date";
	static final String C_TIME = "time";
	static final String C_WEEK = "week";
	static final String C_VS = "vs";
	
	static final String CS[] = {
		C_ID, C_NUMBER, C_DATE, C_TIME, C_WEEK, C_VS,
	};

	private static final String CREATE_TABLE_SQL = "CREATE TABLE " + T_ITEMS 
			+ "(" 
			+ C_ID     + " INTEGER PRIMARY KEY AUTOINCREMENT,"
			+ C_NUMBER + " INTEGER,"
			+ C_DATE   + " INTEGER,"
			+ C_TIME   + " INTEGER,"
			+ C_WEEK   + " TEXT,"
			+ C_VS     + " TEXT" 
			+ ");";

	private static final String DROP_TABLE_SQL = "DROP TABLE IF EXISTS " + T_ITEMS + ";";

	private DatabaseOpenHelper mDbHelper;
	private SQLiteDatabase mDb;
	private Context mCtx;

	public DatabaseHelper(Context context) {
		mCtx = context;
		mDbHelper = new DatabaseOpenHelper(mCtx);
	}

	public void open() {
		if (mDb == null)
			mDb = mDbHelper.getWritableDatabase();
	}

	public void close() {
		if (mDb != null) {
			mDb.close();
			mDb = null;
		}
	}

	public long createItem(String number, String date, String time, String week, String vs) {
		ContentValues v = new ContentValues();
		v.put(C_NUMBER, number);
		v.put(C_DATE, date);
		v.put(C_TIME, time);
		v.put(C_WEEK, week);
		v.put(C_VS, vs);
		return mDb.insert(T_ITEMS, null, v);
	}
	
	public Cursor queryItemsAfterDate(String start) {
		return mDb.query(T_ITEMS, CS, C_DATE + " >= " + start,
				null, null, null, C_DATE + " ASC");
	}
	
	public Cursor queryItemsBetweenDate(String start, String end) {
		return mDb.query(T_ITEMS, CS, C_DATE + " BETWEEN " + start + " AND " + end,
				null, null, null, C_DATE + " ASC");
	}
	
	public long deleteItemsAll() {
		return mDb.delete(T_ITEMS, "1", null);
	}

	private static class DatabaseOpenHelper extends SQLiteOpenHelper {

		DatabaseOpenHelper(Context context) {
			super(context, DB_NAME, null, DB_VER);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			try {
				Log.d(CLASSTAG, EXEC_SQL_PREFIX + CREATE_TABLE_SQL);
				db.execSQL(CREATE_TABLE_SQL);
			} catch (SQLException e) {
				Log.e(CLASSTAG, "Error when create tables", e);
			}

		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log
					.w(CLASSTAG, "Upgrading database from version "
							+ oldVersion + " to " + newVersion
							+ ", which will destroy all old data");
			try {
				Log.d(CLASSTAG, EXEC_SQL_PREFIX + DROP_TABLE_SQL);
				db.execSQL(DROP_TABLE_SQL);
			} catch (SQLException e) {
				Log.e(CLASSTAG, "Error when drop tables", e);
			}

			onCreate(db);
		}
	}

}
