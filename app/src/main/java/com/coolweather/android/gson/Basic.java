package com.coolweather.android.gson;

import com.google.gson.annotations.SerializedName;

/**
 *  创建类
 */

public class Basic {
    @SerializedName("city")
    public String cityName;

    @SerializedName("id")
    public String weatherId;

    public Update update;

    private class Update {
        @SerializedName("loc")
        public String updateTime;
    }
}
