package com.tareq.employee;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class DatabaseContentProvider extends ContentProvider {
    //constants
    private static final String AUTHORITY = "com.tareq.employee";
    private static final String BASE_PATH = DatabaseOpenHelper.EMPLOYEE_ID;
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);
    private static final int CONTACTS = 1;
    private static final int CONTACT_ID = 2;

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    //uri types
    static {
        uriMatcher.addURI(AUTHORITY, BASE_PATH, CONTACTS);
        uriMatcher.addURI(AUTHORITY, BASE_PATH + "/#", CONTACT_ID);
    }

    //the main SQLite database
    private SQLiteDatabase database;


    @Override
    public boolean onCreate() {
        DatabaseOpenHelper helper = new DatabaseOpenHelper(getContext());
        database = helper.getWritableDatabase();
        return true;

    }

    //called while querying data
    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        Cursor cursor;
        switch (uriMatcher.match(uri)) {
            case CONTACTS:
                //query ordered by ID ascending
                cursor = database.query(DatabaseOpenHelper.TABLE_NAME, DatabaseOpenHelper.EMPLOYEE_ALL,
                        selection, null, null, null, DatabaseOpenHelper.EMPLOYEE_ID + " ASC");
                break;
            default:
                throw new IllegalArgumentException("This is an Unknown URI " + uri);
        }
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    //returns type of URI supports
    @Nullable
    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
            case CONTACTS:
                return "vnd.android.cursor.dir/" + BASE_PATH;
            default:
                throw new IllegalArgumentException("This is an Unknown URI " + uri);
        }
    }

    //called while inserting a data
    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        long id = database.insert(DatabaseOpenHelper.TABLE_NAME, null, contentValues);
        if (id >= 0) {
            //setting the id of inserted row in the uri
            Uri _uri = ContentUris.withAppendedId(CONTENT_URI, id);
            getContext().getContentResolver().notifyChange(_uri, null);
            return _uri;
        }

        throw new SQLException("Insertion Failed for URI :" + uri);
    }

    //called when deleting a data
    @Override
    public int delete(Uri uri, String s, String[] strings) {
        //counts the total deleted rows
        int delCount = 0;
        switch (uriMatcher.match(uri)) {
            case CONTACTS:
                delCount = database.delete(DatabaseOpenHelper.TABLE_NAME, s, strings);
                break;
            default:
                throw new IllegalArgumentException("This is an Unknown URI " + uri);
        }
        //notifies that data is modified
        getContext().getContentResolver().notifyChange(uri, null);
        return delCount;
    }


    //called while updating data
    @Override
    public int update(Uri uri, ContentValues contentValues, String s, String[] strings) {
        //counts the updated rows
        int updCount = 0;
        switch (uriMatcher.match(uri)) {
            case CONTACTS:
                updCount = database.update(DatabaseOpenHelper.TABLE_NAME, contentValues, s, strings);
                break;
            default:
                throw new IllegalArgumentException("This is an Unknown URI " + uri);
        }
        //notifies that data is modified
        getContext().getContentResolver().notifyChange(uri, null);
        return updCount;
    }
}
