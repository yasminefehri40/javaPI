package com.otemps.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WeatherService {
    private static final int BAD_TEMP_MIN = 10;
    private static final int BAD_TEMP_MAX = 40;
    private static final int BAD_WIND_SPEED = 60;
    private static final List<Integer> SEVERE_CODES = Arrays.asList(65, 67, 71, 73, 75, 77, 82, 85, 86, 95, 96, 99);
    
    private final HttpClient httpClient;

    public WeatherService() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public Map<String, Object> getWeather(String lieu, LocalDateTime date) {
        try {
            String city = extractCity(lieu);
            Map<String, Double> coords = geocode(city);
            if (coords == null) return null;

            return fetchWeather(coords, date);
        } catch (Exception e) {
            System.err.println("Weather error: " + e.getMessage());
            return null;
        }
    }

    public boolean isBadWeather(Map<String, Object> weather) {
        int temp = (int) weather.get("temperature");
        int code = (int) weather.get("weather_code");
        int wind = (int) weather.get("wind_speed");

        return temp < BAD_TEMP_MIN || temp > BAD_TEMP_MAX || SEVERE_CODES.contains(code) || wind > BAD_WIND_SPEED;
    }

    public String getBadWeatherReason(Map<String, Object> weather) {
        int temp = (int) weather.get("temperature");
        int code = (int) weather.get("weather_code");
        int wind = (int) weather.get("wind_speed");

        if (temp < BAD_TEMP_MIN) return "Température trop basse (" + temp + "°C)";
        if (temp > BAD_TEMP_MAX) return "Température trop élevée (" + temp + "°C)";
        if (SEVERE_CODES.contains(code)) return "Conditions météo sévères : " + weather.get("description");
        if (wind > BAD_WIND_SPEED) return "Vent trop fort (" + wind + " km/h)";
        return "Inconnu";
    }

    private String extractCity(String lieu) {
        String[] parts = lieu.split(",");
        return parts[parts.length - 1].trim();
    }

    private Map<String, Double> geocode(String city) throws Exception {
        String url = "https://geocoding-api.open-meteo.com/v1/search?name=" + city.replace(" ", "%20") + "&count=1&language=fr&format=json";
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        if (!json.has("results")) return null;
        
        JsonObject first = json.getAsJsonArray("results").get(0).getAsJsonObject();
        Map<String, Double> coords = new HashMap<>();
        coords.put("lat", first.get("latitude").getAsDouble());
        coords.put("lon", first.get("longitude").getAsDouble());
        return coords;
    }

    private Map<String, Object> fetchWeather(Map<String, Double> coords, LocalDateTime date) throws Exception {
        // Use Locale.US to ensure dot as decimal separator
        String url = String.format(java.util.Locale.US, "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f&current=temperature_2m,wind_speed_10m,weather_code&timezone=auto", 
                coords.get("lat"), coords.get("lon"));
        
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

        if (!json.has("current")) {
            System.err.println("Weather API Error: 'current' block missing in response.");
            return null;
        }

        JsonObject current = json.getAsJsonObject("current");
        Map<String, Object> weather = new HashMap<>();
        weather.put("temperature", current.get("temperature_2m").getAsInt());
        weather.put("wind_speed", (int) Math.round(current.get("wind_speed_10m").getAsDouble()));
        weather.put("weather_code", current.get("weather_code").getAsInt());
        weather.put("description", describe(current.get("weather_code").getAsInt()));
        
        return weather;
    }

    private String describe(int code) {
        switch (code) {
            case 0: return "Ciel dégagé";
            case 1: return "Principalement dégagé";
            case 2: return "Partiellement nuageux";
            case 3: return "Couvert";
            case 45: case 48: return "Brouillard";
            case 51: case 53: case 55: return "Bruine";
            case 61: case 63: case 65: return "Pluie";
            case 71: case 73: case 75: return "Neige";
            case 95: case 96: case 99: return "Orage";
            default: return "Variable";
        }
    }
}
