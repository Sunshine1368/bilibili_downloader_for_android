package uno.toolkit.bilibilidownloader;

public class DownloadTask {
    public static final int TYPE_VIDEO = 1;
    public static final int TYPE_AUDIO = 2;
    public static final int TYPE_DANMAKU = 3;

    public String id;
    public String title;
    public int type;
    public String format;
    public int progress;
    public String status;

    public DownloadTask(String id, String title, int type, String format) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.format = format;
        this.progress = 0;
        this.status = "等待中";
    }
}