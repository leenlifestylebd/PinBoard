package com.pinboard.keyboard;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class PinDatabase extends SQLiteOpenHelper {

    private static final String DB_NAME    = "pinboard.db";
    private static final int    DB_VERSION = 2;   // bumped for migration
    private static final String TABLE      = "pins";

    // columns
    private static final String COL_ID         = "id";
    private static final String COL_TYPE       = "type";
    private static final String COL_TEXT       = "text_content";
    private static final String COL_IMAGE      = "image_path";
    private static final String COL_LABEL      = "label";
    private static final String COL_TIMESTAMP  = "timestamp";
    private static final String COL_ORDER      = "sort_order";

    private static PinDatabase instance;

    public static PinDatabase getInstance(Context ctx) {
        if (instance == null) instance = new PinDatabase(ctx.getApplicationContext());
        return instance;
    }

    private PinDatabase(Context ctx) { super(ctx, DB_NAME, null, DB_VERSION); }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " (" +
                COL_ID        + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TYPE      + " INTEGER NOT NULL, " +
                COL_TEXT      + " TEXT, " +
                COL_IMAGE     + " TEXT, " +
                COL_LABEL     + " TEXT, " +
                COL_TIMESTAMP + " INTEGER, " +
                COL_ORDER     + " INTEGER DEFAULT 0" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // migrate: add new columns if upgrading from v1
        if (oldVersion < 2) {
            try { db.execSQL("ALTER TABLE " + TABLE + " ADD COLUMN " + COL_TEXT + " TEXT"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + TABLE + " ADD COLUMN " + COL_IMAGE + " TEXT"); } catch (Exception ignored) {}
            // copy old "content" column into proper columns based on type
            try {
                db.execSQL("UPDATE " + TABLE + " SET " + COL_TEXT + " = content WHERE type = 0");
                db.execSQL("UPDATE " + TABLE + " SET " + COL_IMAGE + " = content WHERE type = 1");
            } catch (Exception ignored) {}
        }
    }

    // ---- INSERT ----

    public long insertPin(PinItem pin) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_TYPE,      pin.getType());
        cv.put(COL_TEXT,      pin.getTextContent());
        cv.put(COL_IMAGE,     pin.getImagePath());
        cv.put(COL_LABEL,     pin.getLabel());
        cv.put(COL_TIMESTAMP, pin.getTimestamp());
        cv.put(COL_ORDER,     getMaxOrder() + 1);
        long id = db.insert(TABLE, null, cv);
        pin.setId(id);
        return id;
    }

    // ---- QUERY ----

    public List<PinItem> getAllPins() {
        return query(null, null);
    }

    public List<PinItem> getPinsByType(int type) {
        return query(COL_TYPE + "=?", new String[]{String.valueOf(type)});
    }

    /** Returns text-only + combo pins (anything with text) */
    public List<PinItem> getTextPins() {
        return query(COL_TYPE + " IN (0,2)", null);
    }

    /** Returns image-only + combo pins (anything with image) */
    public List<PinItem> getImagePins() {
        return query(COL_TYPE + " IN (1,2)", null);
    }

    private List<PinItem> query(String selection, String[] args) {
        List<PinItem> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE, null, selection, args, null, null,
                COL_ORDER + " ASC, " + COL_TIMESTAMP + " DESC");
        while (c.moveToNext()) {
            PinItem pin = new PinItem();
            pin.setId(c.getLong(c.getColumnIndexOrThrow(COL_ID)));
            pin.setType(c.getInt(c.getColumnIndexOrThrow(COL_TYPE)));
            pin.setTextContent(c.getString(c.getColumnIndexOrThrow(COL_TEXT)));
            pin.setImagePath(c.getString(c.getColumnIndexOrThrow(COL_IMAGE)));
            pin.setLabel(c.getString(c.getColumnIndexOrThrow(COL_LABEL)));
            pin.setTimestamp(c.getLong(c.getColumnIndexOrThrow(COL_TIMESTAMP)));
            pin.setOrder(c.getInt(c.getColumnIndexOrThrow(COL_ORDER)));
            list.add(pin);
        }
        c.close();
        return list;
    }

    // ---- DELETE / UPDATE ----

    public void deletePin(long id) {
        getWritableDatabase().delete(TABLE, COL_ID + "=?", new String[]{String.valueOf(id)});
    }

    public void updatePin(PinItem pin) {
        ContentValues cv = new ContentValues();
        cv.put(COL_TEXT,  pin.getTextContent());
        cv.put(COL_IMAGE, pin.getImagePath());
        cv.put(COL_LABEL, pin.getLabel());
        cv.put(COL_TYPE,  pin.getType());
        getWritableDatabase().update(TABLE, cv, COL_ID + "=?",
                new String[]{String.valueOf(pin.getId())});
    }

    private int getMaxOrder() {
        Cursor c = getReadableDatabase().rawQuery("SELECT MAX(" + COL_ORDER + ") FROM " + TABLE, null);
        int max = 0;
        if (c.moveToFirst() && !c.isNull(0)) max = c.getInt(0);
        c.close();
        return max;
    }
}
