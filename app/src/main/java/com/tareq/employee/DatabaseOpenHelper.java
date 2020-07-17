package com.tareq.employee;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

public class DatabaseOpenHelper extends SQLiteOpenHelper {
    //constants, database information
    private final static String DATABASE_NAME = "employees.db";
    public static final String TABLE_NAME = "employee";
    public static final String EMPLOYEE_ID = "id";
    public static final String EMPLOYEE_NAME = "name";
    public static final String EMPLOYEE_AGE = "age";
    public static final String EMPLOYEE_GENDER = "gender";
    public static final String[] EMPLOYEE_ALL = {EMPLOYEE_ID, EMPLOYEE_NAME, EMPLOYEE_AGE, EMPLOYEE_GENDER};

    //create table SQL
    private final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    EMPLOYEE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    EMPLOYEE_NAME + " TEXT NOT NULL, " +
                    EMPLOYEE_AGE + " INTEGER NOT NULL, " +
                    EMPLOYEE_GENDER + " INTEGER NOT NULL" +
                    ")";

    public DatabaseOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    //called once while first database is created
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    //upgrading old SQL version to New
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }
}
