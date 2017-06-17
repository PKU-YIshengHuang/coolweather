package com.coolweather.android.gson;

import com.google.gson.annotations.SerializedName;

/**
 * Created by pc on 2017/6/17.
 */

public class Forecast {
    public String data;
    public More more;
    public Temperature temperature;

    class More {
        @SerializedName("txt_d")
        public String info;
    }

    class Temperature {
        public String max;

        public String min;

    }


}
