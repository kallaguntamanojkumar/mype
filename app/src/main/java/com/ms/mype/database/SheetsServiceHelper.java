package com.ms.mype.database;

import android.content.Context;
import android.util.Log;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import java.io.InputStream;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import java.security.GeneralSecurityException;
import java.util.Collections;

import com.ms.mype.R;

public class SheetsServiceHelper {

    private static Sheets sheetsService;

    public static void initialize(Context context) {
        try {
            InputStream inputStream = context.getResources().openRawResource(R.raw.credentials);
            GoogleCredentials credentials = GoogleCredentials.fromStream(inputStream)
                    .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));

            // Initialize HttpTransport
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

            sheetsService = new Sheets.Builder(
                    httpTransport,
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName("mype")
                    .build();
        } catch (IOException | GeneralSecurityException e) {
            Log.e("SheetsServiceHelper", "Error initializing SheetsServiceHelper", e);
        }
    }

    public static Sheets getSheetsService() {
        return sheetsService;
    }
}
