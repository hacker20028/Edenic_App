package com.aryan.edenic.utils;

import android.os.Handler;
import android.os.Looper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class StockChangeClient {
    private static final String SHEET_URL = "https://docs.google.com/spreadsheets/d/e/2PACX-1vQnLyH8bT2N0PiwgnYVBal01zmQwS1u5b5Ryp_vJRO_qi6LnePNHM5DlDk0RLj8icMOPgVTZhZn4Mjn/pub?gid=0&single=true&output=csv";

    public interface StockChangeCallback {
        void onSuccess(Map<String, Double> changePercentMap);
        void onFailure(Exception e);
    }

    public static void fetchChangePercentages(StockChangeCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(SHEET_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()));

                // Parse CSV data
                Map<String, Double> changePercentMap = new HashMap<>();
                String line;
                // Skip header
                reader.readLine();

                while ((line = reader.readLine()) != null) {
                    String[] data = line.split(",");
                    if (data.length >= 6) {
                        // Format: CompanyName,Symbol,CurrentPrice,PreviousClose,PriceChange,PriceChangePercent
                        String symbol = data[1].trim();
                        double changePercent = Double.parseDouble(data[5].trim());

                        changePercentMap.put(symbol, changePercent);
                    }
                }

                reader.close();
                connection.disconnect();

                // Return data on main thread
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onSuccess(changePercentMap));

            } catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onFailure(e));
            }
        }).start();
    }
}