package com.ciheul.bigmaps.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class BigBikeDatabaseHelper extends SQLiteOpenHelper {

    // database information
    private static final String DATABASE_NAME = "bigbike.db";
    private static final int DATABASE_VERSION = 1;

    // table: business
    public static final String TABLE_SHELTER = "shelter";
    public static final String COL_SHELTER_ID = "_id";
    public static final String COL_SHELTER_NAME = "name";
    public static final String COL_CAPACITY = "capacity";
    public static final String COL_LON = "longitude";
    public static final String COL_LAT = "latitude";
    public static final String COL_UPDATED_AT = "updated_at";

    // table: images
    public static final String TABLE_IMAGE = "image";
    public static final String COL_IMAGE_ID = "_id";
    public static final String COL_IMAGE_URL = "url";
    public static final String COL_SHELTER_FK = "shelter_id";

    // coordinates : [lon, lat]
    public static final String COORDINATES = "coordinates";
    public static final String IMAGES = "images";

    // create business table
    private static final String CREATE_TABLE_SHELTER = "CREATE TABLE " + TABLE_SHELTER + "(" + COL_SHELTER_ID
            + " integer primary key autoincrement, " + COL_SHELTER_NAME + " text unique not null, " + COL_CAPACITY
            + " integer not null, " + COL_LON + " real not null, " + COL_LAT + " text not null, " +
            COL_UPDATED_AT + " text not null, UNIQUE(" + COL_SHELTER_NAME + ") ON CONFLICT IGNORE);";

    // create images table
    private static final String CREATE_TABLE_IMAGES = "CREATE TABLE " + TABLE_IMAGE + "(" + COL_IMAGE_ID
            + " integer primary key autoincrement, " + COL_IMAGE_URL + " text not null, " + COL_SHELTER_FK
            + " integer not null);";

    // drop business table
    private static final String DROP_TABLE_BUSINESS = "DROP TABLE IF EXISTS " + TABLE_SHELTER;

    // drop images table
    private static final String DROP_TABLE_IMAGES = "DROP TABLE IF EXISTS " + TABLE_IMAGE;

    public BigBikeDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_SHELTER);
        db.execSQL(CREATE_TABLE_IMAGES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String upgrade_message = "Upgrade database v" + oldVersion + " to v" + newVersion + ".";
        Log.w(BigBikeDatabaseHelper.class.getName(), upgrade_message);
        db.execSQL(DROP_TABLE_BUSINESS);
        db.execSQL(DROP_TABLE_IMAGES);
        onCreate(db);
    }
}
