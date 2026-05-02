package uno.toolkit.bilibilidownloader;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;

public class Utils {

    public static void saveCookie(Context context, String cookie) {
        SharedPreferences prefs = context.getSharedPreferences("bili_prefs", Context.MODE_PRIVATE);
        prefs.edit().putString("cookie", cookie).apply();
    }

    public static String getCookie(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("bili_prefs", Context.MODE_PRIVATE);
        return prefs.getString("cookie", "");
    }

    public static void saveToMediaStore(Context context, File tempFile, String mimeType, String displayName, int type) throws Exception {
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);

        Uri collection;
        String relativePath;

        if (type == DownloadTask.TYPE_VIDEO) {
            collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            relativePath = Environment.DIRECTORY_MOVIES + "/BiliDownloader";
        } else if (type == DownloadTask.TYPE_AUDIO) {
            collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            relativePath = Environment.DIRECTORY_MUSIC + "/BiliDownloader";
        } else {
            collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
            relativePath = Environment.DIRECTORY_DOWNLOADS + "/BiliDownloader";
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath);
            values.put(MediaStore.MediaColumns.IS_PENDING, 1);
        }

        Uri itemUri = context.getContentResolver().insert(collection, values);
        if (itemUri != null) {
            try (OutputStream out = context.getContentResolver().openOutputStream(itemUri);
                 FileInputStream in = new FileInputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear();
                values.put(MediaStore.MediaColumns.IS_PENDING, 0);
                context.getContentResolver().update(itemUri, values, null, null);
            }
        }
    }
}