package com.tashafeen.labeldetector;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;


public class DetectorActivity extends AppCompatActivity {


    private EditText m_apiKeyView;
    private EditText m_clientIDView;
    private EditText m_databaseURLView;
    private EditText m_storageBucketView;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detector);

        m_apiKeyView = findViewById(R.id.apiKey);
        m_clientIDView = findViewById(R.id.clientID);
        m_databaseURLView = findViewById(R.id.databaseURL);
        m_storageBucketView = findViewById(R.id.storageBucket);

        Button signInView = findViewById(R.id.firebase_sign_in);
        signInView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

    }

    private void attemptLogin() {

        String apiKey = m_apiKeyView.getText().toString(); //AIzaSyBixjfp6t1KQq_GqsqjCdKgiHDOyqzPZ34


        String clientID = m_clientIDView.getText().toString(); //923413741408-7kpk90njeacq0pdhkivsrimgptit7qdi.apps.googleusercontent.com


        String databaseURL = m_databaseURLView.getText().toString(); //https://androidapp-b1ec6.firebaseio.com


        String storageBucket = m_storageBucketView.getText().toString();//starlit-water-181616.appspot.com



        FirebaseOptions options = new FirebaseOptions.Builder()
                .setApplicationId(clientID) // Required for Analytics.
                .setApiKey(apiKey) // Required for Auth.
                .setDatabaseUrl(databaseURL) // Required for RTDB.
                .build();

        if (!FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this /* Context */, options, "secondary");
        }
        Intent FWrapperIntent = new Intent(this, FirebaseWrapperActivity.class);
        startActivity(FWrapperIntent);


    }


}



