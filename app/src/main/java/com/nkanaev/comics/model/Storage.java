package com.nkanaev.comics.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;


public class Storage {
    public static abstract class Book implements BaseColumns {
        public static final String TABLE_NAME = "book";

        public static final String COLUMN_NAME_ID = "id";
        public static final String COLUMN_NAME_FILEPATH = "path";
        public static final String COLUMN_NAME_FILENAME = "name";
        public static final String COLUMN_NAME_NUM_PAGES = "num_pages";
        public static final String COLUMN_NAME_CURRENT_PAGE = "cur_page";
        public static final String COLUMN_NAME_TYPE = "type";

        public static final String[] columns = {
                Book.COLUMN_NAME_ID,
                Book.COLUMN_NAME_FILEPATH,
                Book.COLUMN_NAME_FILENAME,
                Book.COLUMN_NAME_NUM_PAGES,
                Book.COLUMN_NAME_CURRENT_PAGE,
                Book.COLUMN_NAME_TYPE
        };
    }

    public class ComicDbHelper extends SQLiteOpenHelper {
        public static final int DATABASE_VERSION = 1;
        public static final String DATABASE_NAME = "comics.db";

        public ComicDbHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            final String sql = "CREATE TABLE " + Book.TABLE_NAME + " ("
                    + Book.COLUMN_NAME_ID + " INTEGER PRIMARY KEY,"
                    + Book.COLUMN_NAME_FILEPATH + " TEXT,"
                    + Book.COLUMN_NAME_FILENAME + " TEXT,"
                    + Book.COLUMN_NAME_NUM_PAGES + " INTEGER,"
                    + Book.COLUMN_NAME_CURRENT_PAGE + " INTEGER DEFAULT 1,"
                    + Book.COLUMN_NAME_TYPE + " TEXT"
                    + ")";
            db.execSQL(sql);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }

    private ComicDbHelper mDbHelper;
    private static Storage mSharedInstance;

    private Storage(Context context) {
        mDbHelper = new ComicDbHelper(context);
    }

    public static Storage getStorage(Context context) {
        if (mSharedInstance == null) {
            synchronized (Storage.class) {
                if (mSharedInstance == null) {
                    mSharedInstance = new Storage(context);
                }
            }
        }
        return mSharedInstance;
    }

    public void clearStorage() {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.delete(Book.TABLE_NAME, null, null);
    }

    public void addBook(File filepath, String type, int numPages) {
        ContentValues cv = new ContentValues();
        cv.put(Book.COLUMN_NAME_FILEPATH, filepath.getParentFile().getAbsolutePath());
        cv.put(Book.COLUMN_NAME_FILENAME, filepath.getName());
        cv.put(Book.COLUMN_NAME_NUM_PAGES, numPages);
        cv.put(Book.COLUMN_NAME_TYPE, type);

        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.insert(Book.TABLE_NAME, "null", cv);
    }

    public ArrayList<Comic> listDirectoryComics() {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        Cursor c = db.query(
                Book.TABLE_NAME, Book.columns, null, null,
                Book.COLUMN_NAME_FILEPATH, null,
                Book.COLUMN_NAME_FILENAME + " DESC");

        ArrayList<Comic> comics = new ArrayList<>();
        if (c.getCount() == 0) return comics;

        c.moveToFirst();
        do {
            comics.add(comicFromCursor(c));
        } while (c.moveToNext());

        c.close();

        return comics;
    }

    private Comic comicFromCursor(Cursor c) {
        int id = c.getInt(c.getColumnIndex(Book.COLUMN_NAME_ID));
        String path = c.getString(c.getColumnIndex(Book.COLUMN_NAME_FILEPATH));
        String name = c.getString(c.getColumnIndex(Book.COLUMN_NAME_FILENAME));
        int numPages = c.getInt(c.getColumnIndex(Book.COLUMN_NAME_NUM_PAGES));
        int currentPage = c.getInt(c.getColumnIndex(Book.COLUMN_NAME_CURRENT_PAGE));
        String type = c.getString(c.getColumnIndex(Book.COLUMN_NAME_TYPE));

        return new Comic(this, id, path, name, type, numPages, currentPage);
    }

    public ArrayList<Comic> listComics(String path) {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        String order = Book.COLUMN_NAME_FILEPATH + " DESC";
        String selection = "";
        if (path != null) {
            selection = Book.COLUMN_NAME_FILEPATH + "=\"" + path +  "\"";
        }

        Cursor c = db.query(Book.TABLE_NAME, Book.columns, selection, null, null, null, order);
        ArrayList<Comic> comics = new ArrayList<>();

        c.moveToFirst();
        if (c.getCount() > 0) {
            do {
                comics.add(comicFromCursor(c));
            } while (c.moveToNext());
        }

//        c.close();

        return comics;
    }

    public Comic getComic(int comicId) {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        String order = Book.COLUMN_NAME_FILEPATH + " DESC";
        String selection = Book.COLUMN_NAME_ID + "=" + Integer.toString(comicId);
        Cursor c = db.query(Book.TABLE_NAME, Book.columns, selection, null, null, null, order);

        if (c.getCount() != 1) {
            return null;
        }

        c.moveToFirst();

        Comic comic = comicFromCursor(c);
        c.close();
        return comic;
    }

    public void bookmarkPage(int comicId, int page) {
        ContentValues values = new ContentValues();
        values.put(Book.COLUMN_NAME_CURRENT_PAGE, page);
        String filter = Book.COLUMN_NAME_ID + "=" + Integer.toString(comicId);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.update(Book.TABLE_NAME, values, filter, null);
    }

    public void removeComic(int comicId) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        String whereClause = Book.COLUMN_NAME_ID + '=' + Integer.toString(comicId);
        db.delete(Book.TABLE_NAME, whereClause, null);
    }
}
