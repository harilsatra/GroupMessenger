package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static edu.buffalo.cse.cse486586.groupmessenger2.GroupMessengerActivity.TAG;

/**
 * GroupMessengerProvider is a key-value table. Once again, please note that we do not implement
 * full support for SQL as a usual ContentProvider does. We re-purpose ContentProvider's interface
 * to use it as a key-value table.
 * 
 * Please read:
 * 
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 * 
 * before you start to get yourself familiarized with ContentProvider.
 * 
 * There are two methods you need to implement---insert() and query(). Others are optional and
 * will not be tested.
 * 
 * @author stevko
 *
 */
public class GroupMessengerProvider extends ContentProvider {

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // You do not need to implement this.
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        /*
         * TODO: You need to implement this method. Note that values will have two columns (a key
         * column and a value column) and one row that contains the actual (key, value) pair to be
         * inserted.
         * 
         * For actual storage, you can use any option. If you know how to use SQL, then you can use
         * SQLite. But this is not a requirement. You can use other storage options, such as the
         * internal storage option that we used in PA1. If you want to use that option, please
         * take a look at the code for PA1.
         */
        //Log.v(TAG,"I am in Insert");
        String filename = values.get("key").toString(); // The actual value in 'key' will be our file name.
        String string = values.get("value").toString(); // The actual value in 'value' will be the content of the file which has name equal to it's corresponding key.

        //The following snippet of code is taken from PA1 and is used to create internal storage file with key as the name and value as the content.
        FileOutputStream outputStream;

        try {
            outputStream = getContext().openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(string.getBytes());
            outputStream.close();
        } catch (Exception e) {
            Log.e(TAG, "File write failed");
        }
        Log.v("insert", values.toString());
        return uri;
    }

    @Override
    public boolean onCreate() {
        // If you need to perform any one-time initialization task, please do it here.
        return false;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        /*
         * TODO: You need to implement this method. Note that you need to return a Cursor object
         * with the right format. If the formatting is not correct, then it is not going to work.
         *
         * If you use SQLite, whatever is returned from SQLite is a Cursor object. However, you
         * still need to be careful because the formatting might still be incorrect.
         *
         * If you use a file storage option, then it is your job to build a Cursor * object. I
         * recommend building a MatrixCursor described at:
         * http://developer.android.com/reference/android/database/MatrixCursor.html
         */
        //Log.v(TAG,"I am in Query");
        FileInputStream fis = null;
        try {
            fis = getContext().openFileInput(selection); //the parameter 'selection' specifies the criteria for selecting rows which in our case is the key and hence the filename.
        } catch (FileNotFoundException e) {
            Log.e(TAG, "FILE NOT FOUND");
            //e.printStackTrace();
        }
        StringBuffer fileContent = new StringBuffer("");

        byte[] buffer = new byte[1024];
        int n;
        try {
            while ((n = fis.read(buffer)) != -1)
            {
                fileContent.append(new String(buffer, 0, n));
            }
        } catch (IOException e) {
            Log.e(TAG, "COULD NOT READ THE FILE");
            //e.printStackTrace();
        }

        //The matrix cursor constructor takes the names of the columns as a parameter, which in our case will be 'key' and 'value'
        String[] columns = {"key","value"};
        MatrixCursor m = new MatrixCursor(columns);
        //The addRow function adds a new row to the end with the given column values in the same order as mentioned while calling the constructor.
        //We add the key (parameter 'selection') and the content read from the file above as the key value pair in the matrix cursor.
        String[] rows = {selection, String.valueOf(fileContent)};
        m.addRow(rows);
        //Log.v("query", selection);
        return m;
    }
}
