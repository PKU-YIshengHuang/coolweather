package com.coolweather.android.gson;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 总的Weather类，包含Basic、Api、Suggestion和Forecast类的实例对象
 */

public class Weather {
    public Basic basic;

    public Forecast forecast;

    public Now now;

    public Suggestion suggestion;

    public AQI aqi;

    // 返回的JSON数据里面有一个字段是 status
    public String status;

    @SerializedName("daily_forecast")
    public List<Forecast> forecastList;

}
