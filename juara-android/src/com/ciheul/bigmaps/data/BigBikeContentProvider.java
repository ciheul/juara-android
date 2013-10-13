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

public class BigBikeContentProvider extends ContentProvider {

    private BigBikeDatabaseHelper db;

    private static final int SHELTERS = 10;
    private static final int SHELTER_ID = 11;
    private static final int IMAGES = 20;
    private static final int IMAGE_ID = 21;

    private static final String AUTHORITY = "com.ciheul.bigmaps.data.BigBikeContentProvider";
    private static final String SHELTER_BASE_PATH = "shelters";
    private static final String IMAGE_BASE_PATH = "images";

    public static final Uri SHELTER_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + SHELTER_BASE_PATH);
    public static final Uri IMAGE_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + IMAGE_BASE_PATH);

    public static final String SHELTER_CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/shelters";
    public static final String SHELTER_CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/shelter";
    public static final String IMAGE_CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/images";
    public static final String IMAGE_CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/image";

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sURIMatcher.addURI(AUTHORITY, SHELTER_BASE_PATH, SHELTERS);
        sURIMatcher.addURI(AUTHORITY, SHELTER_BASE_PATH + "/#", SHELTER_ID);
        sURIMatcher.addURI(AUTHORITY, IMAGE_BASE_PATH, IMAGES);
        sURIMatcher.addURI(AUTHORITY, IMAGE_BASE_PATH + "/#", IMAGE_ID);
    }

    @Override
    public boolean onCreate() {
        db = new BigBikeDatabaseHelper(getContext());
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        int uriType = sURIMatcher.match(uri);

        Cursor cursor = null;
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        switch (uriType) {
        case SHELTERS:
            queryBuilder.setTables(BigBikeDatabaseHelper.TABLE_SHELTER);
            break;
        case SHELTER_ID:
            queryBuilder.setTables(BigBikeDatabaseHelper.TABLE_SHELTER);
            queryBuilder.appendWhere(BigBikeDatabaseHelper.COL_SHELTER_ID + "=" + uri.getLastPathSegment());
            break;
        case IMAGE_ID:
            queryBuilder.setTables(BigBikeDatabaseHelper.TABLE_IMAGE);
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
        case SHELTERS:
            id = db.getWritableDatabase().insertWithOnConflict(BigBikeDatabaseHelper.TABLE_SHELTER, null, values,
                    SQLiteDatabase.CONFLICT_IGNORE);
            PATH = SHELTER_BASE_PATH;
            break;
        case IMAGES:
            id = db.getWritableDatabase().insert(BigBikeDatabaseHelper.TABLE_IMAGE, null, values);
            PATH = IMAGE_BASE_PATH;
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
        case SHELTERS:
            rowsUpdated = db.getWritableDatabase().update(BigBikeDatabaseHelper.TABLE_SHELTER, values, selection,
                    selectionArgs);
            break;
        case SHELTER_ID:
            String id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                String full_selection = BigBikeDatabaseHelper.COL_SHELTER_ID + "=" + id;
                rowsUpdated = db.getWritableDatabase().update(BigBikeDatabaseHelper.TABLE_SHELTER, values,
                        full_selection, null);
            } else {
                String full_selection = BigBikeDatabaseHelper.COL_SHELTER_ID + "=" + id + " and " + selection;
                rowsUpdated = db.getWritableDatabase().update(BigBikeDatabaseHelper.TABLE_SHELTER, values,
                        full_selection, selectionArgs);
            }
            break;
        case IMAGES:
            rowsUpdated = db.getWritableDatabase().update(BigBikeDatabaseHelper.TABLE_IMAGE, values, selection,
                    selectionArgs);
            break;
        case IMAGE_ID:
            String businessId = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                String full_selection = BigBikeDatabaseHelper.COL_SHELTER_FK + "=" + businessId;
                rowsUpdated = db.getWritableDatabase().update(BigBikeDatabaseHelper.TABLE_IMAGE, values,
                        full_selection, null);
            } else {
                String full_selection = BigBikeDatabaseHelper.COL_SHELTER_FK + "=" + businessId + " and " + selection;
                rowsUpdated = db.getWritableDatabase().update(BigBikeDatabaseHelper.TABLE_IMAGE, values,
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
        case SHELTERS:
            rowsDeleted = db.getWritableDatabase()
                    .delete(BigBikeDatabaseHelper.TABLE_SHELTER, selection, selectionArgs);
            break;
        case SHELTER_ID:
            String id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                String full_selection = BigBikeDatabaseHelper.COL_SHELTER_ID + "=" + id;
                rowsDeleted = db.getWritableDatabase()
                        .delete(BigBikeDatabaseHelper.TABLE_SHELTER, full_selection, null);
            } else {
                String full_selection = BigBikeDatabaseHelper.COL_SHELTER_ID + "=" + id + " and " + selection;
                rowsDeleted = db.getWritableDatabase().delete(BigBikeDatabaseHelper.TABLE_SHELTER, full_selection,
                        selectionArgs);
            }
            break;
        case IMAGES:
            rowsDeleted = db.getWritableDatabase().delete(BigBikeDatabaseHelper.TABLE_IMAGE, selection, null);
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
