package uno.toolkit.bilibilidownloader;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.ReturnCode;
import com.arthenica.ffmpegkit.Session;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadService extends Service {

    public static final String ACTION_START = "ACTION_START";
    public static final String ACTION_CANCEL = "ACTION_CANCEL";
    private static final String CHANNEL_ID = "DownloadChannel";

    private ExecutorService executorService;
    private NotificationManager notificationManager;
    private Map<String, Boolean> cancelFlags = new HashMap<>();
    private OkHttpClient okHttpClient;

    @Override
    public void onCreate() {
        super.onCreate();
        executorService = Executors.newFixedThreadPool(3);
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        okHttpClient = RetrofitClient.getClient(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "下载服务", NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("B站下载器")
                .setContentText("正在运行")
                .setSmallIcon(R.drawable.ic_menu)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(1, notification);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            String id = intent.getStringExtra("id");
            if (ACTION_START.equals(action)) {
                cancelFlags.put(id, false);
                executorService.submit(() -> handleDownload(intent));
            } else if (ACTION_CANCEL.equals(action)) {
                cancelFlags.put(id, true);
            }
        }
        return START_NOT_STICKY;
    }

    private void handleDownload(Intent intent) {
        String id = intent.getStringExtra("id");
        int type = intent.getIntExtra("type", 0);
        String title = intent.getStringExtra("title");

        broadcastProgress(id, 0, "准备中");

        try {
            if (type == DownloadTask.TYPE_VIDEO) {
                String url = intent.getStringExtra("videoUrl");
                File tempFile = new File(getCacheDir(), id + "_video.m4s");
                if (downloadUrl(id, url, tempFile)) {
                    broadcastProgress(id, 100, "保存中...");
                    Utils.saveToMediaStore(this, tempFile, "video/mp4", title + ".mp4", DownloadTask.TYPE_VIDEO);
                    tempFile.delete();
                    broadcastProgress(id, 100, "完成");
                }
            } else if (type == DownloadTask.TYPE_AUDIO) {
                String url = intent.getStringExtra("audioUrl");
                String format = intent.getStringExtra("format"); // MP3, AAC, WAV
                File tempAudio = new File(getCacheDir(), id + "_audio.m4s");

                if (downloadUrl(id, url, tempAudio)) {
                    broadcastProgress(id, 100, "转码中...");
                    File finalAudio = new File(getCacheDir(), id + "_final." + format.toLowerCase());

                    // FFmpeg 转换命令
                    String codec = format.equals("MP3") ? "libmp3lame -q:a 2" :
                            (format.equals("AAC") ? "aac -b:a 192k" : "pcm_s16le");

                    String cmd = String.format("-y -i %s -c:a %s %s",
                            tempAudio.getAbsolutePath(), codec, finalAudio.getAbsolutePath());

                    Session session = FFmpegKit.execute(cmd);
                    if (ReturnCode.isSuccess(session.getReturnCode())) {
                        Utils.saveToMediaStore(this, finalAudio, "audio/" + format.toLowerCase(), title + "." + format.toLowerCase(), DownloadTask.TYPE_AUDIO);
                        broadcastProgress(id, 100, "完成");
                    } else {
                        broadcastProgress(id, 0, "转码失败");
                    }
                    tempAudio.delete();
                    finalAudio.delete();
                }
            } else if (type == DownloadTask.TYPE_DANMAKU) {
                String cid = intent.getStringExtra("cid");
                String url = "https://api.bilibili.com/x/v1/dm/list.so?oid=" + cid;
                File tempXml = new File(getCacheDir(), id + "_dm.xml");
                if (downloadUrl(id, url, tempXml)) {
                    Utils.saveToMediaStore(this, tempXml, "text/xml", title + "_danmaku.xml", DownloadTask.TYPE_DANMAKU);
                    tempXml.delete();
                    broadcastProgress(id, 100, "完成");
                }
            }
        } catch (Exception e) {
            broadcastProgress(id, 0, "出错: " + e.getMessage());
        }
    }

    private boolean downloadUrl(String id, String url, File dest) throws Exception {
        Request request = new Request.Builder()
                .url(url)
                .header("Referer", "https://www.bilibili.com")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) return false;

            long total = response.body().contentLength();
            InputStream is = response.body().byteStream();
            FileOutputStream fos = new FileOutputStream(dest);

            byte[] buffer = new byte[8192];
            long downloaded = 0;
            int read;
            int lastProgress = 0;

            while ((read = is.read(buffer)) != -1) {
                if (cancelFlags.containsKey(id) && cancelFlags.get(id)) {
                    fos.close();
                    dest.delete();
                    broadcastProgress(id, 0, "已取消");
                    return false;
                }
                fos.write(buffer, 0, read);
                downloaded += read;

                if (total > 0) {
                    int progress = (int) (downloaded * 100 / total);
                    if (progress - lastProgress >= 2) { // 降低刷新频率
                        broadcastProgress(id, progress, "下载中");
                        lastProgress = progress;
                    }
                }
            }
            fos.close();
            return true;
        }
    }

    private void broadcastProgress(String id, int progress, String status) {
        Intent intent = new Intent("DOWNLOAD_PROGRESS");
        // 【关键修复】适配 Android 14+：内部广播必须明确指定当前应用的包名，否则接收器收不到
        intent.setPackage(getPackageName());
        intent.putExtra("id", id);
        intent.putExtra("progress", progress);
        intent.putExtra("status", status);
        sendBroadcast(intent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}