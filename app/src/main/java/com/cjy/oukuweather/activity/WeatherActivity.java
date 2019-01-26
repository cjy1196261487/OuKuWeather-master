package com.cjy.oukuweather.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.icu.text.AlphabeticIndex;
import android.os.Build;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.cjy.oukuweather.R;
import com.cjy.oukuweather.json.Daily_forecast;
import com.cjy.oukuweather.json.Heweather;
import com.cjy.oukuweather.json.Weather;
import com.cjy.oukuweather.service.AutoupdateService;
import com.cjy.oukuweather.util.HttpUtil;
import com.cjy.oukuweather.util.SharePreferenceUtil;
import com.cjy.oukuweather.util.Utility;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.KeyStore;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


public class WeatherActivity extends BaseActivity {

    private ScrollView weatherscrollview;
    private TextView titleCity,titleUpdateTime,degreeText,weatherInfo,aqiText,pm25Text,comfortText,carWashText,sportText,qutytext;
    private LinearLayout forcastLayout;

    public SwipeRefreshLayout swipeRefreshLayout;
    private ImageView backimg, citybutton;
    public DrawerLayout drawerLayout;
    private Weather weather;
    private Heweather heweather;
    private SharePreferenceUtil sputil;
    //腾讯api接口
    public static String GET_CITY_NAME_TENCENT="https://apis.map.qq.com/ws/location/v1/ip?key=TZEBZ-JGDLU-HAGV6-2JNEY-MYBL6-EXBL7";





    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sputil=new SharePreferenceUtil(WeatherActivity.this,"saveweather");


        setContentView(R.layout.activity_weather);
        initView();
       loadPic();
        if (sputil.getweatherid()==null) {
            loadLocal(GET_CITY_NAME_TENCENT);
        }else {
            requestWeather(sputil.getweatherid());
        }

        initlisten();




    }

    private void loadLocal(String getCityNameTencent) {
        HttpUtil.send0khttpRequest(getCityNameTencent, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
               final String json=response.body().string();
                try {
                    JSONObject object = new JSONObject(json).getJSONObject("result").getJSONObject("ad_info");
                    String city=object.getString("district");
                    requestWeather(city);
                } catch (JSONException e) {
                    e.printStackTrace();
                }


            }
        });
    }

    private void loadPic() {
        final String picUrl="http://guolin.tech/api/bing_pic";
        HttpUtil.send0khttpRequest(picUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                final String picurl=response.body().string();
                Log.e("bing 每日一图 url",picurl);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(picurl).into(backimg);

                    }
                });



            }
        });



    }

    private void initlisten() {
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
               requestWeather(sputil.getweatherid());


            }
        });

        citybutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

    }

    public void requestWeather(String weatherId) {
        sputil.setweatherid(weatherId);
      //  String url="http://guolin.tech/api/weather?cityid="+weatherId+"&key=2c07f3cfca7440a48d2b7c0b52975dd7";
        String url="https://free-api.heweather.com/s6/weather?location="+weatherId+"&key=2c07f3cfca7440a48d2b7c0b52975dd7";
        HttpUtil.send0khttpRequest(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        swipeRefreshLayout.setRefreshing(false);

                    }
                });


            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String respond_text = response.body().string();

                heweather = Utility.handleWeatherResponse(respond_text);
                Log.i("实时天气是",heweather.toString());

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (heweather.getStatus().equals("ok")) {

                            showWeather(heweather);
                            swipeRefreshLayout.setRefreshing(false);
                        } else if (heweather.getStatus().equals("unknown city")) {

                            Toast.makeText(WeatherActivity.this, "未知地区", Toast.LENGTH_SHORT).show();
                            swipeRefreshLayout.setRefreshing(false);

                        }
                    }
                });
            }
        });

    }


    private void initView() {
//        初始化控件
        weatherscrollview=findViewById(R.id.weather_layout);
        titleCity=findViewById(R.id.title_city);
        titleUpdateTime=findViewById(R.id.title_updatetime);
        degreeText=findViewById(R.id.degree_text);
        weatherInfo=findViewById(R.id.weatherinfo_text);
        aqiText=findViewById(R.id.aqi_text);
        pm25Text=findViewById(R.id.pm25_text);
        qutytext=findViewById(R.id.quty_text);
        comfortText=findViewById(R.id.comfort_text);
        carWashText=findViewById(R.id.car_text);
        sportText=findViewById(R.id.sport_text);
        forcastLayout=findViewById(R.id.forecast_layout);
        swipeRefreshLayout=(SwipeRefreshLayout)findViewById(R.id.refresh);
        swipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.colorPrimary));
        backimg=findViewById(R.id.backgroun_pic);
        citybutton=findViewById(R.id.city_select);
        drawerLayout=findViewById(R.id.drawer_layout);










    }



    private void showWeather(Heweather heweather) {
        if (heweather!=null && "ok".equals(heweather.getStatus())){
            Intent intent=new Intent(this, AutoupdateService.class);
            intent.putExtra("cityId",heweather.getBasic().getCid());
            startService(intent);
        }else {
            Toast.makeText(WeatherActivity.this,"获取天气失败",Toast.LENGTH_SHORT).show();
        }
        String updatetime = heweather.getUpdate().getLoc().split("\\s")[1];

           // Log.e("",""+simpleDateFormat.parse(updatetime));


        titleCity.setText(heweather.getBasic().getLocation());
        titleUpdateTime.setText(updatetime);
        degreeText.setText(heweather.getNow().getTmp() + "℃");
        weatherInfo.setText(heweather.getNow().getCond_txt());
        initChart(heweather);


//        aqiText.setText(weather.aqi.getCity().getAqi());
//        pm25Text.setText(weather.aqi.getCity().getPm25());
//        qutytext.setText(weather.aqi.getCity().getQlty());
        comfortText.setText("舒适度：" + heweather.getLifestyle().get(0).getTxt());
        carWashText.setText("洗车 ：" +heweather.getLifestyle().get(heweather.getLifestyle().size()-2).getTxt());
        sportText.setText("运动 ：" + heweather.getLifestyle().get(3).getTxt());
        forcastLayout.removeAllViews();

        for (Heweather.DailyForecastBean dailyForecas:heweather.getDaily_forecast()) {
            View view = LayoutInflater.from(this).inflate(R.layout.forcast_item, forcastLayout, false);
            TextView dateText = view.findViewById(R.id.date_info);
            TextView infoText = view.findViewById(R.id.info_text);
            TextView max = view.findViewById(R.id.max_text);
            TextView min = view.findViewById(R.id.min_text);
            dateText.setText(dailyForecas.getDate());
            infoText.setText(dailyForecas.getCond_txt_d());
            max.setText(dailyForecas.getTmp_max()+"℃");
            min.setText(dailyForecas.getTmp_min()+"℃");
            forcastLayout.addView(view);
        }

        swipeRefreshLayout.setRefreshing(false);

    }

    private void initChart(Heweather heweather) {
         final  ArrayList<String>timedate=new ArrayList<>();
         LineChart mlinechart;
         XAxis xa;
         YAxis ya;
        mlinechart=findViewById(R.id.timeweather);
        mlinechart.setDrawBorders(true);




        List<Heweather.HourlyBean>hourlyBeans=heweather.getHourly();

        Log.i("hourlyBeans size",""+hourlyBeans.size());
        //折线图数据填充
        //设置数据
        List<Entry>entries=new ArrayList<>();
        for (int i = 0; i <hourlyBeans.size() ; i++) {
            entries.add(new Entry(i,(float)Integer.parseInt(hourlyBeans.get(i).getTmp())));
        }
        //刷新数据
        mlinechart.invalidate();
        //设置linedateaert
        LineDataSet lineDataSet=new LineDataSet(entries,"温度");
        lineDataSet.setValueFormatter(new IValueFormatter() {
            @Override
            public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
                return (int)value+"℃";
            }
        });

        lineDataSet.setValueTextColor(Color.RED);
        LineData lineData=new LineData(lineDataSet);
        mlinechart.setData(lineData);
        for (int i = 0; i <hourlyBeans.size() ; i++) {
            timedate.add(hourlyBeans.get(i).getTime().split("\\s")[1]);
        }
//        mlinechart.notifyDataSetChanged();

        xa=mlinechart.getXAxis();
        xa.setTextColor(Color.WHITE);
        xa.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return timedate.get((int)value);
            }
        });
        ya=mlinechart.getAxisLeft();
        ya.setTextColor(Color.WHITE);
        ya.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return value+"℃";
            }
        });
    }


}



