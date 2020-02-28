package com.tekartik.sqflite;

import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.util.Log;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.io.File;

import static com.tekartik.sqflite.Constant.TAG;

public class Database {
    final boolean singleInstance;
    final String path;
    final int id;
    final int logLevel;

    SQLiteDatabase sqliteDatabase;
    private static final ConcurrentMap<String, SQLiteDatabase>
            sqliteDatabaseSingleInstance = new ConcurrentHashMap<>();
    private static volatile Handler dbHandler;
    private static volatile boolean readOnly;
    boolean inTransaction;


    public Database(String path, int id, boolean singleInstance, int logLevel) {
        this.path = path;
        this.singleInstance = singleInstance;
        this.id = id;
        this.logLevel = logLevel;
    }

    public static Handler getDbHandler() {
        return dbHandler;
    }

    public void open(Handler handler) {
        if (!singleInstance) {
            sqliteDatabase = SQLiteDatabase.openDatabase(path, null,
                    SQLiteDatabase.CREATE_IF_NECESSARY);
        } else {
            openSingleInstance(path, false, SQLiteDatabase.CREATE_IF_NECESSARY, handler);
        }
    }

    private static void openSingleInstance(String path, boolean readOnlyFlag, int flags, Handler handler) {
        if (!sqliteDatabaseSingleInstance.containsKey(path)) {
            synchronized (Database.class) {
                if (!sqliteDatabaseSingleInstance.containsKey(path)) {
                    SQLiteDatabase dbInstance = SQLiteDatabase.openDatabase(path, null,
                            flags, new DatabaseErrorHandler() {
                                @Override
                                public void onCorruption(SQLiteDatabase dbObj) {
                                    // ignored
                                    // default implementation delete the file
                                    //
                                    // This happens asynchronously so cannot be tracked. However a simple
                                    // access should fail
                                }
                            });
                    SQLiteDatabase previousDatabase = sqliteDatabaseSingleInstance.putIfAbsent(path, dbInstance);
                    // Check if the newly created instance was successfully added to the concurrent map
                    // If someone beat us to it, delete our second connection which will not be used
                    if (previousDatabase != null) {
                        dbInstance.close();
                    }
                    dbHandler = handler;
                }
                readOnly = false;
            }
        }

        if (readOnly != readOnlyFlag) {
            throw new RuntimeException("Tried to open a single database with a different READ_ONLY mode");
        }
    }

    public void openReadOnly(Handler handler) {
        if (!singleInstance) {
            sqliteDatabase = SQLiteDatabase.openDatabase(path, null,
                    SQLiteDatabase.OPEN_READONLY, new DatabaseErrorHandler() {
                        @Override
                        public void onCorruption(SQLiteDatabase dbObj) {
                            // ignored
                            // default implementation delete the file
                            //
                            // This happens asynchronously so cannot be tracked. However a simple
                            // access should fail
                        }
                    });
        } else {
            openSingleInstance(path, true, SQLiteDatabase.OPEN_READONLY, handler);
        }
    }

    public void close() {
        if (sqliteDatabase != null) {
            sqliteDatabase.close();
        }
    }

    public SQLiteDatabase getWritableDatabase() {
        if (!singleInstance) {
            return sqliteDatabase;
        }
        return sqliteDatabaseSingleInstance.get(path);
    }

    public SQLiteDatabase getReadableDatabase() {
        if (!singleInstance) {
            return sqliteDatabase;
        }
        return sqliteDatabaseSingleInstance.get(path);
    }

    public boolean enableWriteAheadLogging() {
        try {
            return sqliteDatabase.enableWriteAheadLogging();
        } catch (Exception e) {
            Log.e(TAG, getThreadLogPrefix() + "enable WAL error: " + e);
            return false;
        }
    }

    String getThreadLogTag() {
        Thread thread = Thread.currentThread();

        return "" + id + "," + thread.getName() + "(" + thread.getId() + ")";
    }

    String getThreadLogPrefix() {
        return "[" + getThreadLogTag() + "] ";
    }


    static void deleteDatabase(String path) {
        SQLiteDatabase.deleteDatabase(new File(path));
    }
}
