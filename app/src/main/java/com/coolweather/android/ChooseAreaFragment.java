package com.coolweather.android;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.coolweather.android.db.City;
import com.coolweather.android.db.County;
import com.coolweather.android.db.Province;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;


public class ChooseAreaFragment extends Fragment {
    private static final int LEVEL_PROVINCE = 0;
    private static final int LEVEL_CITY = 1;
    private static final int LEVEL_COUNTY = 2;
    private TextView titleText;
    private Button backButton;
    private ListView listView;
    private int currentLevel;

    private ArrayAdapter<String> adapter;
    private List<String> dataList = new ArrayList<String>();

    private List<Province> provinceList;
    private List<City> cityList;
    private List<County> countyList;

    private Province selectedProvince;
    private City selectedCity;
    private ProgressDialog progressDialog;

    private static final String TARGET = "ChooseAreaFragment";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area,container,false);
        titleText = (TextView) view.findViewById(R.id.title_text);
        backButton = (Button) view.findViewById(R.id.back_button);
        listView = (ListView) view.findViewById(R.id.list_view);
        adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        // activity和fragment都必须先调用父类的onActivityCreated方法
        super.onActivityCreated(savedInstanceState);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(currentLevel == LEVEL_PROVINCE){
                    selectedProvince = provinceList.get(position);
                    queryCities();
                    Log.i("ChooseAreaFragment","--------------------------省----------------");
                }else if(currentLevel == LEVEL_CITY){
                    selectedCity = cityList.get(position);
                    queryCounties();
                    Log.i("ChooseAreaFragment","--------------------------城市----------------");
                }else if(currentLevel == LEVEL_COUNTY){
                    // 如果是mainActivity页面，则跳转到天气页面
                    String weatherId = countyList.get(position).getWeatherId();
                    if(getActivity() instanceof MainActivity){
                        // 当前如果是县级被选中，需要跳转到天气显示活动
                        Log.i("ChooseAreaFragment","--------------------------跳转----------------");
                        Intent intent = new Intent(getActivity(),WeatherActivity.class);
                        intent.putExtra("weather_id",weatherId);
                        startActivity(intent);
                        getActivity().finish();
                    }else if(getActivity() instanceof WeatherActivity){
                        // 是WeatherActivity页面，则直接显示出出来
                        WeatherActivity activity = (WeatherActivity) getActivity();
                        activity.drawerLayout.closeDrawers();
                        activity.swipeRefresh.setRefreshing(true);
                        activity.requestWeather(weatherId);
                    }

                }
            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentLevel == LEVEL_COUNTY){
                    queryCities();
                }else if(currentLevel == LEVEL_CITY){
                    queryProvinces();

                 }
            }
        });
        queryProvinces();
    }

    /**
     * 查询县区,优先查询数据库；然后查询服务器
     */
    private void queryCounties() {
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList = DataSupport.where("cityid = ?",String.valueOf(selectedCity.getId()))
                .find(County.class);
        if(countyList.size()>0){
            dataList.clear();
            for (County county : countyList) {
                dataList.add(county.getCountyName()); // 在listView上显示城市的名字
            }
            adapter.notifyDataSetChanged();
            Log.i("ChooseAreaFragment","--------------------------展示conuty----------------");
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
            Log.i("ChooseAreaFragment","-----------------currentLevel = LEVEL_COUNTY----------------");
        }else{
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = "http://guolin.tech/api/china/"+provinceCode+"/"+cityCode;
            queryFromServer(address,"county");
        }


    }

    /**
     * 查询城市,优先查询数据库；然后查询服务器
     */
    private void queryCities() {
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList = DataSupport.where("provinceid = ?",String.valueOf(selectedProvince.getId()))
                .find(City.class);
        if(cityList.size()>0){
            dataList.clear();
            for (City city : cityList) {
                dataList.add(city.getCityName()); // 在listView上显示城市的名字
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_CITY;
        }else{
            int provinceCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/"+provinceCode;
            queryFromServer(address,"city");
        }

    }

    /**
     *查询全国所有的省份，优先查询数据库；然后查询服务器
     */
    private void queryProvinces() {
        titleText.setText("中国");
        backButton.setVisibility(View.GONE);
        provinceList = DataSupport.findAll(Province.class);
        if(provinceList.size()>0){
            dataList.clear();
            for (Province province : provinceList) {
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_PROVINCE;
        }else{
            // 数据库中没有天气数据，访问数据库
            String address = "http://guolin.tech/api/china";
            queryFromServer(address,"province");
        }
    }

    /**
     * 查询数据库
     * @param address
     * @param type
     */
    private void queryFromServer(String address, final String type) {
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(),"加载失败",Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                boolean result = false;
                if("province".equals(type)){
                    result = Utility.handleProvinceResponse(responseText);
                }else if("city".equals(type)){
                    result = Utility.handleCityResponse(responseText,selectedProvince.getId());
                }else if("county".equals(type)){
                    Log.i("ChooseAreaFragment","--------------------------解析conuty----------------");
                    result = Utility.handleCountyResponse(responseText,selectedCity.getId());
                }

                if(result){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if("province".equals(type)){
                                queryProvinces();
                            }else if("city".equals(type)){
                                queryCities();
                            }else if("county".equals(type)){
                                Log.i("ChooseAreaFragment","--------------------------查询conuty----------------");
                                queryCounties();
                                Log.i("ChooseAreaFragment","--------------------------查询conuty完毕----------------");
                            }
                        }
                    });


                }
            }
        });

    }



    /**
     * 显示进度条
     */
    private void showProgressDialog() {
        if(progressDialog == null){
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载....尝试。。。..");
            progressDialog.setCancelable(false);
        }
        progressDialog.show();
    }

    /**
     * 关闭进度条
     */
    private void closeProgressDialog() {
        if(progressDialog!=null){
            progressDialog.dismiss();
        }
    }


}
