package com.moobywotsit.models;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

public record LibreCgmData(double Value, boolean IsHigh, boolean IsLow, Trend TrendArrow, Date Timestamp) {
    private enum Trend {
        @SerializedName("1")
        SingleDown,
        @SerializedName("2")
        FortyFiveDown,
        @SerializedName("3")
        Flat,
        @SerializedName("4")
        FortyFiveUp,
        @SerializedName("5")
        SingleUp,
        @SerializedName("6")
        NotComputable
    }
}
