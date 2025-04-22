package com.aryan.edenic.yahoo_finance;

import com.google.gson.annotations.SerializedName;

public class Meta {

    @SerializedName("symbol")
    public String symbol;
    @SerializedName("regularMarketPrice")
    public double regularMarketPrice;
    @SerializedName("previousClose")
    public double previousClose;
}
