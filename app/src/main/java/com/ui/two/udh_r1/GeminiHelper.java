package com.ui.two.udh_r1;

import android.annotation.SuppressLint;
import android.util.Log;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;
import com.google.api.services.youtube.model.VideoStatistics;

// New Imports for calling Gemini API
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.JsonParser;
import java.util.HashMap;
import java.util.Map;
// ---

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class GeminiHelper {

    // --- IMPORTANT ---
    // This API_KEY is for your Google Cloud project. It works for BOTH
    // the YouTube Data API and the Gemini API.
    private static final String API_KEY = "AIzaSyB8WTCL4xO9RtIAiD_UM_2YQTPX7ys7-nw";

    private static final String APPLICATION_NAME = "Gemini-Sorter";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final long NUMBER_OF_VIDEOS_TO_FETCH = 25; // YouTube API search limit

    /**
     * Stores the video details and our calculated ratio.
     */
    public static class VideoResult {
        String id;
        String title;
        long viewCount;
        long likeCount;
        double likeToViewRatio;

        public VideoResult(String id, String title, long viewCount, long likeCount) {
            this.id = id;
            this.title = title;
            this.viewCount = viewCount;
            this.likeCount = likeCount;

            // Calculate the ratio
            if (viewCount > 0) {
                // We multiply by 1000 to get a more readable number
                this.likeToViewRatio = (double) likeCount / viewCount * 1000.0;
            } else {
                this.likeToViewRatio = 0.0;
            }
        }

    }

    public List<VideoResult> search_gemini(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java com.ui.two.YouTubeSorter \"<search query>\"");
            System.exit(1);
        }
        String query = args[0];

        try {
            // 1. Set up YouTube Service
            final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            YouTube youtubeService = new YouTube.Builder(httpTransport, JSON_FACTORY, null)
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            // 2. --- NEW AI-Powered Search ---
            // Call Gemini to get a semantically relevant list of video IDs
            Log.i("Info","Asking AI to find the best videos for: \"" + query + "\"...");
            List<String> videoIds = getAiCuratedVideoIds(query, API_KEY, httpTransport, JSON_FACTORY);

            if (videoIds.isEmpty()) {
                Log.i("Info","The AI couldn't find any videos for that query.");
                return null;
            }

            // 3. --- Get Video Statistics (Same as before) ---
            // Fetch the stats for the IDs the AI gave us
            Log.i("msg","Fetching statistics for " + videoIds.size() + " AI-curated videos...");
            List<VideoResult> videoResults = getVideoStatistics(youtubeService, videoIds);

            // 4. Sort the results by our like/view ratio (descending)
            videoResults.sort((v1, v2) -> Double.compare(v2.likeToViewRatio, v1.likeToViewRatio));

            // 5. Print the sorted list
            return videoResults;

        } catch (GeneralSecurityException | IOException e) {
            Log.e("err","An error occurred: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * --- NEW FUNCTION ---
     * Step 1: Calls the Gemini API to get a list of relevant video IDs.
     * This replaces the YouTube `search.list()` call.
     */
    private static List<String> getAiCuratedVideoIds(String userQuery, String apiKey, HttpTransport httpTransport, JsonFactory jsonFactory) throws IOException {

        // This is the model that supports Google Search grounding
        String model = "gemini-2.5-flash";
        String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

        // 1. Create a powerful System Prompt
        String systemPrompt = "You are a world-class YouTube search expert. " +
                "A user wants to learn about a topic. Your job is to find the most relevant, high-quality, and helpful YouTube videos on that topic. " +
                "You MUST use your Google Search tool to find actual, real video IDs. " +
                "Return a JSON array of 5 video IDs. Do NOT return anything else, just the JSON array string. " +
                "For example: [\"L0U7-28-s1I\", \"9G-XsI01o-k\"]";

        // 2. Build the complex JSON payload as a Map
        Map<String, Object> payload = new HashMap<>();

        // System Instruction
        Map<String, Object> systemInstruction = new HashMap<>();
        systemInstruction.put("parts", List.of(Map.of("text", systemPrompt)));
        payload.put("systemInstruction", systemInstruction);

        // User Content
        Map<String, Object> userContent = new HashMap<>();
        userContent.put("parts", List.of(Map.of("text", userQuery)));
        payload.put("contents", List.of(userContent));

        // Grounding Tool (This is CRITICAL!)
        Map<String, Object> groundingTool = new HashMap<>();
        groundingTool.put("google_search", new HashMap<>()); // Empty object enables search
        payload.put("tools", List.of(groundingTool));

        // 3. Make the HTTP POST Request
        HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
        GenericUrl url = new GenericUrl(apiUrl);

        // Serialize the Map payload into a JSON request body
        JsonHttpContent httpContent = new JsonHttpContent(jsonFactory, payload);
        HttpRequest request = requestFactory.buildPostRequest(url, httpContent);

        String rawResponse = "";
        try {
            rawResponse = request.execute().parseAsString();
        } catch (IOException e) {
            System.err.println("Error calling Gemini API: " + e.getMessage());
            // Log the raw payload for debugging if something goes wrong
            // System.err.println("Failed Payload: " + jsonFactory.toString(payload));
            throw e;
        }

        // 4. Parse the AI's Response
        try {
            // Parse the outer JSON response from Gemini
            JsonParser parser = jsonFactory.createJsonParser(rawResponse);
            Map<String, Object> responseMap = parser.parse(HashMap.class);

            // Navigate the nested JSON to get the text content
            // response -> candidates[0] -> content -> parts[0] -> text
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseMap.get("candidates");
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            String aiResponseText = (String) parts.get(0).get("text");

            // 5. Parse the AI's *inner* JSON (the array of IDs)
            // The AI's text response *is* the JSON array we asked for
            JsonParser listParser = jsonFactory.createJsonParser(aiResponseText);

            return (List<String>) listParser.parseArray(ArrayList.class,String.class);

        } catch (Exception e) {
            System.err.println("Error parsing Gemini response: " + e.getMessage());
            System.err.println("Raw response was: " + rawResponse);
            return Collections.emptyList();
        }
    }


    /**
     * Step 2: Fetches statistics for a list of video IDs.
     * (This function is unchanged)
     */
    private static List<VideoResult> getVideoStatistics(YouTube youtube, List<String> videoIds) throws IOException {
        // The API supports fetching stats for up to 50 videos at once.
        // We join the list of IDs into a single comma-separated string.
        String allVideoIds = String.join(",", videoIds);

        // 1. Create the API request
        YouTube.Videos.List videoRequest = youtube.videos()
                .list("snippet,statistics") // We need 'snippet' for title, 'statistics' for counts
                .setKey(API_KEY)
                .setId(allVideoIds);

        // 2. Execute the request
        VideoListResponse videoResponse = videoRequest.execute();
        List<Video> videos = videoResponse.getItems();
        List<VideoResult> videoResults = new ArrayList<>();

        // 3. Process the results
        if (videos.isEmpty()) {
            System.out.println("Could not find statistics for the provided video IDs.");
            return videoResults;
        }

        for (Video video : videos) {
            VideoStatistics stats = video.getStatistics();
            if (stats == null) continue;

            // Get the counts, default to 0 if null
            BigInteger viewCount = stats.getViewCount();
            BigInteger likeCount = stats.getLikeCount();

            videoResults.add(new VideoResult(
                    video.getId(),
                    video.getSnippet().getTitle(),
                    (viewCount != null) ? viewCount.longValue() : 0L,
                    (likeCount != null) ? likeCount.longValue() : 0L
            ));
        }
        return videoResults;
    }
}

