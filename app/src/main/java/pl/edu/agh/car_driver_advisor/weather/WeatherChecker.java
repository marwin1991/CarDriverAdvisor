package pl.edu.agh.car_driver_advisor.weather;


import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

import pl.edu.agh.car_driver_advisor.BuildConfig;

public class WeatherChecker implements Runnable {

    private static String API_CALL_PATTERN = "https://api.openweathermap.org/data/2.5/onecall?lat=%s&lon=%s&exclude=current,minutely,daily&appid=%s";

    private String apikey = BuildConfig.API_KEY;
    private Handler weatherUpdateHandler;
    private double latitude;
    private double longitude;

    public WeatherChecker(Handler weatherUpdateHandler, double latitude, double longitude) {
        this.weatherUpdateHandler = weatherUpdateHandler;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Override
    public void run() {
        String weatherForecast;
        try {
            System.out.println("GETTING WEATHER DATA");

            weatherForecast = getJSONWeatherForecast();

            JSONObject o = new JSONObject(weatherForecast);
            JSONArray forecastArray = (JSONArray) (o.get("hourly"));
            JSONObject nextHourWeather = forecastArray.getJSONObject(0);
            Integer temperature = nextHourWeather.getInt("temp") - 273;
            String pressure = nextHourWeather.getString("pressure");
            String wind = nextHourWeather.getString("wind_speed");
            String generalWeather = nextHourWeather.getJSONArray("weather").getJSONObject(0).getString("main");

            Message msg = weatherUpdateHandler.obtainMessage();
            Bundle b = new Bundle();
            b.putString("temperature", String.valueOf(temperature));
            b.putString("pressure", pressure);
            b.putString("wind", wind);
            b.putString("generalWeather", generalWeather);

            msg.setData(b);
            weatherUpdateHandler.sendMessage(msg);

        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }


    }

    private URL createApiQuery() throws MalformedURLException {
        return new URL(String.format(API_CALL_PATTERN, latitude, longitude, apikey));
    }

    private String getJSONWeatherForecast() throws IOException {
        URL url = createApiQuery();
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod("GET");
        urlConnection.setDoInput(true);
        urlConnection.setDoOutput(true);
        urlConnection.setRequestProperty("Content-Type", "application/json");
        urlConnection.connect();

        InputStream inputStream = (InputStream) urlConnection.getContent();
        Scanner sc = new Scanner(inputStream);
        StringBuffer sb = new StringBuffer();
        while (sc.hasNext()) {
            sb.append(sc.nextLine());
        }

        return sb.toString();
    }
}
