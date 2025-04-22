package com.aryan.edenic.utils;

import android.content.Context;
import android.widget.ImageView;

import com.aryan.edenic.R;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

/**
 * Utility class for loading stock logos from Clearbit API
 */
public class StockLogoLoader {
    private static final String TAG = "StockLogoLoader";

    /**
     * Load a stock logo into an ImageView
     * Uses Clearbit Logo API with fallback to local resources
     *
     * @param context Context for Glide
     * @param symbol Stock symbol (e.g., "AAPL")
     * @param logoView Target ImageView
     */
    public static void loadStockLogo(Context context, String symbol, ImageView logoView) {
        // Convert stock symbol to company domain for Clearbit API
        String companyDomain = getCompanyDomain(symbol);
        String logoUrl = "https://logo.clearbit.com/" + companyDomain;

        // Use Glide to load the image with proper fallback
        Glide.with(context)
                .load(logoUrl)
                .apply(new RequestOptions()
                        .placeholder(R.drawable.default_img_holder)
                        .error(getLocalLogoResource(symbol))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .circleCrop())
                .into(logoView);
    }

    /**
     * Map stock symbol to company domain
     * @param symbol Stock symbol
     * @return Company domain
     */
    private static String getCompanyDomain(String symbol) {
        // Map stock symbols to company domains
        switch (symbol.toUpperCase()) {
            // US Tech Giants
            case "AAPL": return "apple.com";
            case "MSFT": return "microsoft.com";
            case "GOOGL":
            case "GOOG": return "google.com";
            case "AMZN": return "amazon.com";
            case "META":
            case "FB": return "meta.com";
            case "TSLA": return "tesla.com";
            case "NVDA": return "nvidia.com";

            // US Financial
            case "JPM": return "jpmorganchase.com";
            case "V": return "visa.com";
            case "BAC": return "bankofamerica.com";
            case "MA": return "mastercard.com";
            case "GS": return "goldmansachs.com";

            // US Retail
            case "WMT": return "walmart.com";
            case "HD": return "homedepot.com";
            case "TGT": return "target.com";
            case "COST": return "costco.com";

            // US Healthcare
            case "JNJ": return "jnj.com";
            case "UNH": return "unitedhealthgroup.com";
            case "PFE": return "pfizer.com";
            case "ABT": return "abbott.com";
            case "TMO": return "thermofisher.com";

            // US Consumer
            case "PG": return "pg.com";
            case "KO": return "coca-colacompany.com";
            case "PEP": return "pepsico.com";
            case "MCD": return "mcdonalds.com";
            case "NKE": return "nike.com";
            case "SBUX": return "starbucks.com";

            // US Telecom & Media
            case "VZ": return "verizon.com";
            case "T": return "att.com";
            case "CMCSA": return "comcast.com";
            case "DIS": return "disney.com";
            case "NFLX": return "netflix.com";

            // US Energy
            case "XOM": return "exxonmobil.com";
            case "CVX": return "chevron.com";
            case "COP": return "conocophillips.com";

            // US Manufacturing
            case "CAT": return "caterpillar.com";
            case "DE": return "deere.com";
            case "MMM": return "3m.com";

            // Other US Tech
            case "CSCO": return "cisco.com";
            case "INTC": return "intel.com";
            case "ADBE": return "adobe.com";
            case "PYPL": return "paypal.com";
            case "CRM": return "salesforce.com";
            case "ORCL": return "oracle.com";
            case "ACN": return "accenture.com";

            // Default fallback
            default: return symbol.toLowerCase() + ".com";
        }
    }

    /**
     * Get local drawable resource for fallback
     * @param symbol Stock symbol
     * @return Drawable resource ID
     */
    private static int getLocalLogoResource(String symbol) {
        // For simplicity, we're using the default image holder for all fallbacks
        // You could expand this with actual logo resources if desired
        return R.drawable.default_img_holder;
    }
}