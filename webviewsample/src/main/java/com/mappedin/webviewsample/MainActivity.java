package com.mappedin.webviewsample;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Hides the top bar, to allow more space for the map
        try
        {
            this.getSupportActionBar().hide();
        }
        catch (NullPointerException e){}


        // Setup a WebView to be used for Mappedin Web
        WebView webView = findViewById(R.id.webview);


        // Enable Javascript
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        /*
        Loads a local html file (located in the assets folder) containing the Mappedin Web Snippet.
        Mappedin api keys, search keys, and the venue's slug will need to be added to this file.

        Another option is to host the site separately, and load an external URL here instead.
        */
        webView.loadUrl("file:///android_asset/index.html");

        /*
        Note about External Links:
        By default this application will open all links tapped by a user in an external application that handles URLs.
        If this is not your desired behaviour, you can override this by providing a WebViewClient for the WebView
        Further customization options and information can be found here: https://developer.android.com/guide/webapps/webview#HandlingNavigation
        */
    }
}
