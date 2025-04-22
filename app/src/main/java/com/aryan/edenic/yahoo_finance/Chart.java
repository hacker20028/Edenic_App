package com.aryan.edenic.yahoo_finance;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Chart {
    @SerializedName("result")
    public List<Result> result;
    @SerializedName("error")
    public Object error;
}
