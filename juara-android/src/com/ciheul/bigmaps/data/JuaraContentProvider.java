package com.ciheul.bigmaps.data;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class JuaraContentProvider extends ContentProvider {

    private JuaraDatabaseHelper db;

    private static final int ADMINISTRATIVES = 10;
    private static final int ADMINISTRATIVE_ID = 11;

    private static final String AUTHORITY = "com.ciheul.bigmaps.data.JuaraContentProvider";
    private static final String ADMINISTRATIVE_BASE_PATH = "administratives";

    public static final Uri ADMINISTRATIVE_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + ADMINISTRATIVE_BASE_PATH);

    public static final String ADMINISTRATIVE_CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/administratives";
    public static final String ADMINISTRATIVE_CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/administrative";

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sURIMatcher.addURI(AUTHORITY, ADMINISTRATIVE_BASE_PATH, ADMINISTRATIVES);
        sURIMatcher.addURI(AUTHORITY, ADMINISTRATIVE_BASE_PATH + "/#", ADMINISTRATIVE_ID);
    }

    @Override
    public boolean onCreate() {
        db = new JuaraDatabaseHelper(getContext());
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        int uriType = sURIMatcher.match(uri);

        Cursor cursor = null;
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        switch (uriType) {
        case ADMINISTRATIVES:
            queryBuilder.setTables(JuaraDatabaseHelper.TABLE_ADMINISTRATIVE);
            break;
        case ADMINISTRATIVE_ID:
            queryBuilder.setTables(JuaraDatabaseHelper.TABLE_ADMINISTRATIVE);
            queryBuilder.appendWhere(JuaraDatabaseHelper.COL_ADMINISTRATIVE_ID + "=" + uri.getLastPathSegment());
            break;
        default:
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        cursor = queryBuilder.query(db.getWritableDatabase(), projection, selection, selectionArgs, null, null,
                sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        int uriType = sURIMatcher.match(uri);
        long id = 0;
        String PATH;

        switch (uriType) {
        case ADMINISTRATIVES:
            id = db.getWritableDatabase().insertWithOnConflict(JuaraDatabaseHelper.TABLE_ADMINISTRATIVE, null, values,
                    SQLiteDatabase.CONFLICT_IGNORE);
            PATH = ADMINISTRATIVE_BASE_PATH;

            break;
        default:
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return Uri.parse(PATH + "/" + id);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int uriType = sURIMatcher.match(uri);
        int rowsUpdated = 0;

        switch (uriType) {
        case ADMINISTRATIVES:
            rowsUpdated = db.getWritableDatabase().update(JuaraDatabaseHelper.TABLE_ADMINISTRATIVE, values, selection,
                    selectionArgs);
            break;
        case ADMINISTRATIVE_ID:
            String id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                String full_selection = JuaraDatabaseHelper.COL_ADMINISTRATIVE_ID + "=" + id;
                rowsUpdated = db.getWritableDatabase().update(JuaraDatabaseHelper.TABLE_ADMINISTRATIVE, values,
                        full_selection, null);
            } else {
                String full_selection = JuaraDatabaseHelper.COL_ADMINISTRATIVE_ID + "=" + id + " and " + selection;
                rowsUpdated = db.getWritableDatabase().update(JuaraDatabaseHelper.TABLE_ADMINISTRATIVE, values,
                        full_selection, selectionArgs);
            }
            break;
        default:
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return rowsUpdated;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int uriType = sURIMatcher.match(uri);
        int rowsDeleted = 0;

        switch (uriType) {
        case ADMINISTRATIVES:
            rowsDeleted = db.getWritableDatabase().delete(JuaraDatabaseHelper.TABLE_ADMINISTRATIVE, selection,
                    selectionArgs);
            break;
        case ADMINISTRATIVE_ID:
            String id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                String full_selection = JuaraDatabaseHelper.COL_ADMINISTRATIVE_ID + "=" + id;
                rowsDeleted = db.getWritableDatabase().delete(JuaraDatabaseHelper.TABLE_ADMINISTRATIVE, full_selection,
                        null);
            } else {
                String full_selection = JuaraDatabaseHelper.COL_ADMINISTRATIVE_ID + "=" + id + " and " + selection;
                rowsDeleted = db.getWritableDatabase().delete(JuaraDatabaseHelper.TABLE_ADMINISTRATIVE, full_selection,
                        selectionArgs);
            }
            break;
        default:
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return rowsDeleted;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

}
