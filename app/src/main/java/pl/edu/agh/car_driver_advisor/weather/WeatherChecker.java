package pl.edu.agh.car_driver_advisor.weather;


import android.os.Handler;

import pl.edu.agh.car_driver_advisor.BuildConfig;

public class WeatherChecker implements Runnable {

    private String apikey = BuildConfig.API_KEY;
    private Handler weatherUpdateHandler;
    private double latitude;
    private  double longitude;
    public WeatherChecker(Handler weatherUpdateHandler, double latitude, double longitude){
        this.weatherUpdateHandler = weatherUpdateHandler;
        this.latitude = latitude;
        this.longitude = longitude;
    }
    @Override
    public void run() {

    }
}
