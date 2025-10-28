package com.ui.two.udh_r1;

import android.graphics.drawable.Drawable;
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
        final ArrayList<GeminiHelper.VideoResult>[] videoResults = new ArrayList[1];

        new Thread(new Runnable() {
            @Override
            public void run() {
                videoResults[0] = (ArrayList<GeminiHelper.VideoResult>) geminiHelper.search_gemini(new String[]{query});
            }
        }).start();

    }

    public void add_entry(Drawable thumbnail, String title, String duration, String rating){
        LinearLayout container = findViewById(R.id.result);

// Inflate the separate layout
        LayoutInflater inflater = LayoutInflater.from(this);
        View itemView = inflater.inflate(R.layout.entry, container, false);

// Update views inside the inflated layout
        TextView title_holder = itemView.findViewById(R.id.title);
        TextView duration_holder = itemView.findViewById(R.id.duration);
        TextView rating_holder = itemView.findViewById(R.id.rating);
        ImageView thumbnail_holder = itemView.findViewById(R.id.thumbnail);

        title_holder.setText(title);
        duration_holder.setText(duration);
        rating_holder.setText(rating);
        thumbnail_holder.setImageDrawable(thumbnail);// or use Glide/Picasso

// Add the inflated layout to the container
        container.addView(itemView);
    }

    public void parse_entry(){}


}