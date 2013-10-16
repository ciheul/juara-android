package com.ciheul.bigmaps.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class JuaraDatabaseHelper extends SQLiteOpenHelper {

    // database information
    private static final String DATABASE_NAME = "administrative.db";
    private static final int DATABASE_VERSION = 1;

    // table: business
    public static final String TABLE_ADMINISTRATIVE = "administrative";
    public static final String COL_ADMINISTRATIVE_ID = "_id";
    public static final String COL_ADMINISTRATIVE_NAME = "name";
    public static final String COL_ADMINISTRATIVE_TYPE = "type";
    public static final String COL_LEADER = "leader";
    public static final String COL_ADDRESS = "address";
    public static final String COL_EMAIL = "email";
    public static final String COL_TELEPHONE = "telephone";
    public static final String COL_POPULATION_FEMALE = "population_female";
    public static final String COL_POPULATION_MALE = "population_male";
    public static final String COL_CREATED_AT = "created_at";
    public static final String COL_UPDATED_AT = "updated_at";

    // create business table
    private static final String CREATE_TABLE_ADMINISTRATIVE = "CREATE TABLE " + TABLE_ADMINISTRATIVE + "("
            + COL_ADMINISTRATIVE_ID + " integer primary key autoincrement, "
            + COL_ADMINISTRATIVE_NAME + " text unique not null, " 
            + COL_ADMINISTRATIVE_TYPE + " text not null, "
            + COL_LEADER + " text not null, "
            + COL_ADDRESS + " text not null, "
            + COL_EMAIL + " text not null, "
            + COL_TELEPHONE + " text not null, " 
            + COL_POPULATION_FEMALE + " integer not null, " 
            + COL_POPULATION_MALE + " integer not null, "
            + COL_CREATED_AT + " text not null, "
            + COL_UPDATED_AT + " text not null, UNIQUE(" + COL_ADMINISTRATIVE_NAME
            + ") ON CONFLICT IGNORE);";

    // drop business table
    private static final String DROP_TABLE_ADMINISTRATIVE = "DROP TABLE IF EXISTS " + TABLE_ADMINISTRATIVE;

    public JuaraDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_ADMINISTRATIVE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String upgrade_message = "Upgrade database v" + oldVersion + " to v" + newVersion + ".";
        Log.w(JuaraDatabaseHelper.class.getName(), upgrade_message);
        db.execSQL(DROP_TABLE_ADMINISTRATIVE);
        onCreate(db);
    }
}
