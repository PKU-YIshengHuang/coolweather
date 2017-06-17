package com.coolweather.android.gson;

import com.google.gson.annotations.SerializedName;

/**
 * Created by pc on 2017/6/17.
 */

public class Now {
    @SerializedName("temp")
    public String temperature;

    @SerializedName("cond")
    public More more;

    public class More{
        @SerializedName("txt")
        public String info;
    }
}
