package com.aryan.edenic.yahoo_finance;

import android.util.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public class YahooFinanceClient {
    private static final String BASE_URL = "https://query1.finance.yahoo.com/";
    private static YahooFinanceApi instance;

    // Cache for stock prices to minimize API calls
    private static final Map<String, CachedPrice> priceCache = new HashMap<>();
    private static final long CACHE_EXPIRY = 5 * 60 * 1000; // 5 minutes cache validity

    public static YahooFinanceApi getInstance() {
        if (instance == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            instance = retrofit.create(YahooFinanceApi.class);
        }
        return instance;
    }

    /**
     * Gets the current price for a stock symbol
     * This method can be called synchronously from PortfolioManager
     *
     * @param symbol Stock symbol to fetch the price for
     * @return Current stock price or 0 if an error occurs
     * @throws IOException If there's a network issue
     */
    public static double getCurrentPrice(String symbol) throws IOException {
        // Check cache first
        CachedPrice cachedPrice = priceCache.get(symbol);
        if (cachedPrice != null && System.currentTimeMillis() - cachedPrice.timestamp < CACHE_EXPIRY) {
            return cachedPrice.price;
        }

        // If not in cache, fetch synchronously
        final AtomicReference<Double> priceRef = new AtomicReference<>(0.0);
        final CountDownLatch latch = new CountDownLatch(1);

        getInstance().getStockData(symbol, "1d", "1d").enqueue(new Callback<YahooResponse>() {
            @Override
            public void onResponse(Call<YahooResponse> call, Response<YahooResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    YahooResponse result = response.body();
                    if (result.chart != null && !result.chart.result.isEmpty() && result.chart.result.get(0).meta != null) {
                        double price = result.chart.result.get(0).meta.regularMarketPrice;
                        priceRef.set(price);

                        // Update cache
                        priceCache.put(symbol, new CachedPrice(price, System.currentTimeMillis()));
                    }
                }
                latch.countDown();
            }

            @Override
            public void onFailure(Call<YahooResponse> call, Throwable t) {
                Log.e("YahooFinance", "Error fetching price for " + symbol, t);
                latch.countDown();
            }
        });

        try {
            // Wait for response with a timeout
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.e("YahooFinance", "Interrupted while waiting for price data", e);
        }

        return priceRef.get();
    }

    /**
     * Class to hold a cached stock price
     */
    private static class CachedPrice {
        final double price;
        final long timestamp;

        CachedPrice(double price, long timestamp) {
            this.price = price;
            this.timestamp = timestamp;
        }
    }

    public interface YahooFinanceApi {
        @GET("v8/finance/chart/{symbol}")
        Call<YahooResponse> getStockData(
                @Path("symbol") String symbol,
                @Query("interval") String interval,
                @Query("range") String range
        );
    }
}