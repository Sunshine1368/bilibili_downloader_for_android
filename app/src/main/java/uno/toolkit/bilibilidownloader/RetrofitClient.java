package uno.toolkit.bilibilidownloader;

import android.content.Context;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit retrofit;
    private static OkHttpClient okHttpClient;

    public static OkHttpClient getClient(Context context) {
        if (okHttpClient == null) {
            okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {
                        String cookie = Utils.getCookie(context);
                        Request original = chain.request();
                        Request request = original.newBuilder()
                                .header("Referer", "https://www.bilibili.com")
                                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36")
                                .header("Cookie", cookie)
                                .build();
                        return chain.proceed(request);
                    })
                    .build();
        }
        return okHttpClient;
    }

    public static BiliApi getApi(Context context) {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl("https://api.bilibili.com/")
                    .client(getClient(context))
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(BiliApi.class);
    }
}