package com.ui.two.udh_r1;

import android.content.Context;
import android.os.Build;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class GeminiHelper {

    private static final String API_KEY = "AIzaSyDB8__yKvqlxFITMrB-ntBVCCIWWC16jak";
    private static final String SEARCH_URL = "https://www.googleapis.com/youtube/v3/search?part=snippet&maxResults=20&q=%s&type=video&key=%s";
    private static final String VIDEOS_URL = "https://www.googleapis.com/youtube/v3/videos?part=statistics,contentDetails,snippet&id=%s&key=%s";

    /**
     * Fetches the top 5 YouTube videos for the given query based on like/view ratio.
     * Uses asynchronous callbacks to return the result.
     *
     * @param context The Android context (e.g., Activity or Application context).
     * @param query The search query.
     * @param callback The callback to handle success or error.
     */
    public void getTopVideos(Context context, String query, final VideoCallback callback) {
        RequestQueue queue = Volley.newRequestQueue(context);


        String searchUrl = String.format(SEARCH_URL, query, API_KEY);
        JsonObjectRequest searchRequest = new JsonObjectRequest(Request.Method.GET, searchUrl, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray items = response.getJSONArray("items");
                            StringBuilder idBuilder = new StringBuilder();
                            for (int i = 0; i < items.length(); i++) {
                                JSONObject item = items.getJSONObject(i);
                                String videoId = item.getJSONObject("id").getString("videoId");
                                if (idBuilder.length() > 0) {
                                    idBuilder.append(",");
                                }
                                idBuilder.append(videoId);
                            }

                            if (idBuilder.length() == 0) {
                                callback.onError(new Exception("No videos found"));
                                return;
                            }


                            String videoIds = idBuilder.toString();
                            String videosUrl = String.format(VIDEOS_URL, videoIds, API_KEY);
                            JsonObjectRequest videosRequest = new JsonObjectRequest(Request.Method.GET, videosUrl, null,
                                    new Response.Listener<JSONObject>() {
                                        @Override
                                        public void onResponse(JSONObject response) {
                                            try {
                                                JSONArray items = response.getJSONArray("items");
                                                List<YouTubeVideo> videos = new ArrayList<>();
                                                for (int i = 0; i < items.length(); i++) {
                                                    JSONObject item = items.getJSONObject(i);
                                                    JSONObject snippet = item.getJSONObject("snippet");
                                                    String title = snippet.getString("title");
                                                    String thumbnail = snippet.getJSONObject("thumbnails")
                                                            .getJSONObject("medium").getString("url"); // Use medium thumbnail
                                                    JSONObject statistics = item.getJSONObject("statistics");
                                                    long viewCount = Long.parseLong(statistics.getString("viewCount"));
                                                    long likeCount = statistics.has("likeCount")
                                                            ? Long.parseLong(statistics.getString("likeCount")) : 0;
                                                    //String durationIso = item.getJSONObject("contentDetails").getString("duration");
                                                    Duration duration = null;

                                                    String videoId = item.getString("id");
                                                    double ratio = viewCount > 0 ? (double) likeCount / viewCount : 0.0;
                                                    videos.add(new YouTubeVideo(title, videoId, likeCount, viewCount,
                                                            "", thumbnail, ratio));
                                                }

                                                // Sort by like/view ratio descending
                                                Collections.sort(videos, new Comparator<YouTubeVideo>() {
                                                    @Override
                                                    public int compare(YouTubeVideo v1, YouTubeVideo v2) {
                                                        return Double.compare(v2.ratio, v1.ratio);
                                                    }
                                                });

                                                // Get top 15 (or fewer if less available)
                                                List<YouTubeVideo> topVideos = videos.subList(0, Math.min(15, videos.size()));
                                                callback.onSuccess(topVideos);
                                            } catch (Exception e) {
                                                callback.onError(e);
                                            }
                                        }
                                    },
                                    new Response.ErrorListener() {
                                        @Override
                                        public void onErrorResponse(VolleyError error) {
                                            callback.onError(error);
                                        }
                                    });
                            queue.add(videosRequest);
                        } catch (Exception e) {
                            callback.onError(e);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        callback.onError(error);
                    }
                });

        queue.add(searchRequest);
    }

    public interface VideoCallback {
        void onSuccess(List<YouTubeVideo> videos);
        void onError(Exception e);
    }

    public static class YouTubeVideo {
        public final String title;
        public final String id;
        public final long likes;
        public final long views;
        public final String thumbnail;
        public final double ratio; // like/view ratio

        public YouTubeVideo(String title, String id, long likes, long views, String duration,
                            String thumbnail, double ratio) {
            this.title = title;
            this.id = id;
            this.likes = likes;
            this.views = views;
            this.thumbnail = thumbnail;
            this.ratio = ratio;
        }
    }
}