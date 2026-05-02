package uno.toolkit.bilibilidownloader;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private TextView tvLoginStatus, tvVideoTitle;
    private TextInputEditText etBvId;
    private Spinner spinnerAudioFormat;
    private View layoutActions;
    private RecyclerView rvTasks;

    private String currentCid = "";
    private String currentTitle = "";
    private String videoUrl = "";
    private String audioUrl = "";

    private TaskAdapter taskAdapter;
    private List<DownloadTask> taskList = new ArrayList<>();

    // 接收 Service 下载进度广播
    private BroadcastReceiver progressReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String id = intent.getStringExtra("id");
            int progress = intent.getIntExtra("progress", 0);
            String status = intent.getStringExtra("status");

            for (int i = 0; i < taskList.size(); i++) {
                if (taskList.get(i).id.equals(id)) {
                    taskList.get(i).progress = progress;
                    taskList.get(i).status = status;
                    taskAdapter.notifyItemChanged(i);
                    break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 【修复1】移除了错误的 Bundle.spacedBy 和多余的 super.onCreate 调用
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }

        initViews();
        checkLoginStatus();

        // 【修复2】使用 ContextCompat 注册广播，解决 Android 14+ 要求导出标志的警告
        ContextCompat.registerReceiver(
                this,
                progressReceiver,
                new IntentFilter("DOWNLOAD_PROGRESS"),
                ContextCompat.RECEIVER_NOT_EXPORTED
        );
    }

    private void initViews() {
        tvLoginStatus = findViewById(R.id.tvLoginStatus);
        etBvId = findViewById(R.id.etBvId);
        tvVideoTitle = findViewById(R.id.tvVideoTitle);
        layoutActions = findViewById(R.id.layoutActions);
        spinnerAudioFormat = findViewById(R.id.spinnerAudioFormat);
        rvTasks = findViewById(R.id.rvTasks);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, AudioFormatSelector.getFormats());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAudioFormat.setAdapter(adapter);

        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new TaskAdapter();
        rvTasks.setAdapter(taskAdapter);

        findViewById(R.id.btnLogin).setOnClickListener(v -> {
            startActivity(new Intent(this, LoginWebViewActivity.class));
        });

        findViewById(R.id.btnParse).setOnClickListener(v -> parseVideo());

        findViewById(R.id.btnDownloadVideo).setOnClickListener(v -> startDownload(DownloadTask.TYPE_VIDEO));
        findViewById(R.id.btnDownloadAudio).setOnClickListener(v -> startDownload(DownloadTask.TYPE_AUDIO));
        findViewById(R.id.btnDownloadDanmaku).setOnClickListener(v -> startDownload(DownloadTask.TYPE_DANMAKU));
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkLoginStatus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(progressReceiver);
    }

    private void checkLoginStatus() {
        String cookie = Utils.getCookie(this);
        if (!cookie.isEmpty()) {
            tvLoginStatus.setText("状态：已登录");
            tvLoginStatus.setTextColor(ContextCompat.getColor(this, R.color.seed));
        } else {
            tvLoginStatus.setText("状态：未登录");
        }
    }

    private void parseVideo() {
        String bvId = etBvId.getText().toString().trim();
        if (bvId.isEmpty()) {
            // 【修复3】更正了错误的 Toast 参数和方法调用（原为 Toast.showShort 和 .setCurrent()）
            Toast.makeText(this, "请输入BV号", Toast.LENGTH_SHORT).show();
            return;
        }

        layoutActions.setVisibility(View.GONE);
        tvVideoTitle.setText("解析中...");

        // 获取视频信息(cid, title)
        RetrofitClient.getApi(this).getVideoInfo(bvId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject body = response.body();
                    if (body.get("code").getAsInt() == 0) {
                        JsonObject data = body.getAsJsonObject("data");
                        currentCid = data.get("cid").getAsString();
                        currentTitle = data.get("title").getAsString();
                        tvVideoTitle.setText(currentTitle);
                        getPlayUrl(bvId, currentCid);
                    } else {
                        tvVideoTitle.setText("解析失败: " + body.get("message").getAsString());
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                tvVideoTitle.setText("网络请求失败");
            }
        });
    }

    private void getPlayUrl(String bvid, String cid) {
        // qn=80 为 1080P，fnval=4048 请求 DASH 格式（音视频分离）
        RetrofitClient.getApi(this).getPlayUrl(bvid, cid, 80, 4048).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject data = response.body().getAsJsonObject("data");
                        JsonObject dash = data.getAsJsonObject("dash");

                        JsonArray videos = dash.getAsJsonArray("video");
                        if (videos.size() > 0) {
                            videoUrl = videos.get(0).getAsJsonObject().get("base_url").getAsString();
                        }

                        JsonArray audios = dash.getAsJsonArray("audio");
                        if (audios.size() > 0) {
                            audioUrl = audios.get(0).getAsJsonObject().get("base_url").getAsString();
                        }
                        layoutActions.setVisibility(View.VISIBLE);
                    } catch (Exception e) {
                        tvVideoTitle.setText("未获取到播放流(请确保已登录或视频存在)");
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                tvVideoTitle.setText("播放流获取失败");
            }
        });
    }

    private void startDownload(int type) {
        // 【关键修复】拦截空 URL，防止传入 Service 后 OkHttp 崩溃
        if (type == DownloadTask.TYPE_VIDEO && (videoUrl == null || videoUrl.isEmpty())) {
            Toast.makeText(this, "未能获取到视频直链，请尝试登录或更换BV号", Toast.LENGTH_SHORT).show();
            return;
        }
        if (type == DownloadTask.TYPE_AUDIO && (audioUrl == null || audioUrl.isEmpty())) {
            Toast.makeText(this, "未能获取到音频直链，请尝试登录或更换BV号", Toast.LENGTH_SHORT).show();
            return;
        }

        String id = String.valueOf(System.currentTimeMillis());
        String format = spinnerAudioFormat.getSelectedItem().toString();

        DownloadTask task = new DownloadTask(id, currentTitle, type, format);
        taskList.add(task);
        taskAdapter.notifyItemInserted(taskList.size() - 1);

        Intent intent = new Intent(this, DownloadService.class);
        intent.setAction(DownloadService.ACTION_START);
        intent.putExtra("id", id);
        intent.putExtra("type", type);
        intent.putExtra("title", currentTitle);
        intent.putExtra("cid", currentCid);
        intent.putExtra("videoUrl", videoUrl);
        intent.putExtra("audioUrl", audioUrl);
        intent.putExtra("format", format);

        ContextCompat.startForegroundService(this, intent);
    }

    private void cancelTask(String id) {
        Intent intent = new Intent(this, DownloadService.class);
        intent.setAction(DownloadService.ACTION_CANCEL);
        intent.putExtra("id", id);
        startService(intent);
    }

    // --- RecyclerView Adapter ---
    class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_download_task, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DownloadTask task = taskList.get(position);
            String typeStr = task.type == DownloadTask.TYPE_VIDEO ? "[视频] " :
                    (task.type == DownloadTask.TYPE_AUDIO ? "[音频] " : "[弹幕] ");
            holder.tvName.setText(typeStr + task.title);
            holder.tvStatus.setText(task.status);
            holder.progressBar.setProgress(task.progress);

            holder.btnCancel.setOnClickListener(v -> cancelTask(task.id));
            if (task.status.equals("完成") || task.status.equals("已取消")) {
                holder.btnCancel.setVisibility(View.GONE);
            } else {
                holder.btnCancel.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public int getItemCount() { return taskList.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvStatus;
            ProgressBar progressBar;
            MaterialButton btnCancel;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvTaskName);
                tvStatus = itemView.findViewById(R.id.tvTaskStatus);
                progressBar = itemView.findViewById(R.id.pbTask);
                btnCancel = itemView.findViewById(R.id.btnCancelTask);
            }
        }
    }
}