package uno.toolkit.bilibilidownloader;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface BiliApi {
    @GET("x/web-interface/view")
    Call<JsonObject> getVideoInfo(@Query("bvid") String bvid);

    @GET("x/player/playurl")
    Call<JsonObject> getPlayUrl(
            @Query("bvid") String bvid,
            @Query("cid") String cid,
            @Query("qn") int qn,
            @Query("fnval") int fnval
    );
}