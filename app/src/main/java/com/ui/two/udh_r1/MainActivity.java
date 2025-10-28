package com.ui.two.udh_r1;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //This is a trial

        EditText searchEditText = findViewById(R.id.edit_query);

        searchEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String query = searchEditText.getText().toString().trim();
                    if (!query.isEmpty()) {
                        search(query);
                    }
                    return true; // consume the action
                }
                return false;
            }
        });


    }

    public void search(String query){
        //Implement all algo after yt api
        GeminiHelper geminiHelper = new GeminiHelper();
        geminiHelper.getTopVideos(this, query, new GeminiHelper.VideoCallback() {
            @Override
            public void onSuccess(List<GeminiHelper.YouTubeVideo> videos) {
                iterative_add(videos);
            }

            @Override
            public void onError(Exception e) {
                Log.e("ERR",e.toString());
            }
        });

    }

    public void iterative_add(List<GeminiHelper.YouTubeVideo> videoResults){
        for (GeminiHelper.YouTubeVideo v:videoResults){
            add_entry(null,v.title,String.valueOf(videoResults.indexOf(v)),Float.toString(((float) v.likes /v.views)*60),v.id);
        }
    }
    public void add_entry(Drawable thumbnail, String title, String duration, String rating,String id){
        LinearLayout container = findViewById(R.id.result);

        // Inflate the separate layout
        LayoutInflater inflater = LayoutInflater.from(this);
        View itemView = inflater.inflate(R.layout.entry, container, false);

        // Update views inside the inflated layout
        TextView title_holder = itemView.findViewById(R.id.title);
        TextView rating_holder = itemView.findViewById(R.id.rating);
        TextView textView = itemView.findViewById(R.id.s_no);

        title_holder.setText(title);
        rating_holder.setText(rating);
        textView.setText(duration);
        container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                play_vid(id);
            }
        });
        // Add the inflated layout to the container
        container.addView(itemView);
    }

    public void play_vid(String id){
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.youtube.com/watch?v="+id));
        intent.putExtra("force_fullscreen",true);
        startActivity(intent);
    }


}