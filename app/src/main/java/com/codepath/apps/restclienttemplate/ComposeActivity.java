package com.codepath.apps.restclienttemplate;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.codepath.apps.restclienttemplate.models.Tweet;
import com.codepath.asynchttpclient.callback.JsonHttpResponseHandler;

import org.json.JSONException;
import org.parceler.Parcels;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import okhttp3.Headers;

public class ComposeActivity extends AppCompatActivity {

    public static final int MAX_TWEET_LENGTH = 280;
    public static final String TAG = "ComposeActivity";

    EditText etCompose;
    Button btnTweet;
    TwitterClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose);
        etCompose = findViewById(R.id.etCompose);
        btnTweet = findViewById(R.id.btnTweet);
        client = TwitterApp.getRestClient(this);

        try {
            BufferedReader input = new BufferedReader(new InputStreamReader(openFileInput("tweet_draft")));
            StringBuffer buffer = new StringBuffer();
            String line;
            while ((line = input.readLine()) != null) {
                buffer.append(line + "\n");
            }
            String text = buffer.toString();
            etCompose.setText(text);
        } catch (Exception e) {}

        btnTweet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String tweetContent = etCompose.getText().toString();
                if (tweetContent.isEmpty()) {
                    Toast.makeText(ComposeActivity.this, "Sorry, your tweet cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (tweetContent.length() > MAX_TWEET_LENGTH) {
                    Toast.makeText(ComposeActivity.this, "Sorry, your tweet is too long", Toast.LENGTH_SHORT).show();
                    return;
                }
                client.publishTweet(tweetContent, new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Headers headers, JSON json) {
                        Log.i(TAG, "onSuccess to publish tweet");
                        try {
                            Tweet tweet = Tweet.fromJson(json.jsonObject);
                            Log.i(TAG, "Published tweet says: " + tweet.body);
                            Intent intent = new Intent();
                            intent.putExtra("tweet", Parcels.wrap(tweet));
                            setResult(RESULT_OK, intent);
                            finish();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Headers headers, String response, Throwable throwable) {
                        Log.e(TAG, "onFailure to publish tweet", throwable);
                    }
                });
            }
        });
    }

    @Override
    public void onBackPressed() {
        String tweetContent = etCompose.getText().toString();
        if (!tweetContent.isEmpty()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle("Would you like to save this tweet as a draft?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            FileOutputStream fos = null;
                            try {
                                fos = openFileOutput("tweet_draft", Context.MODE_PRIVATE);
                                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos));
                                writer.write(tweetContent);
                                writer.close();
                            } catch (Exception e) {}
                            ComposeActivity.super.onBackPressed();
                        }
                    })
                    .setCancelable(false)
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            FileOutputStream fos = null;
                            try {
                                fos = openFileOutput("tweet_draft", Context.MODE_PRIVATE);
                                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos));
                                writer.write("");
                                writer.close();
                            } catch (Exception e) {}
                            ComposeActivity.super.onBackPressed();
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.setCancelable(false);
            dialog.show();
        } else {
            super.onBackPressed();
        }
    }
}