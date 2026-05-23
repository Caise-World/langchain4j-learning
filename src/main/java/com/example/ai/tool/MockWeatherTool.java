package com.example.ai.tool;

import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

@Component
public class MockWeatherTool {

    @Tool("Query weather information for a city")
    public String queryWeather(String city) {
        // Mock weather data
        return switch (city.toLowerCase()) {
            case "beijing" -> "Beijing: 22°C, Sunny, AQI: 78";
            case "shanghai" -> "Shanghai: 25°C, Cloudy, AQI: 55";
            case "guangzhou" -> "Guangzhou: 28°C, Rainy, AQI: 40";
            case "shenzhen" -> "Shenzhen: 27°C, Partly Cloudy, AQI: 35";
            case "hangzhou" -> "Hangzhou: 24°C, Sunny, AQI: 62";
            default -> "Weather data not available for " + city;
        };
    }
}