package com.weather.collector;

import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class ApiService {

    private final String baseUrl;
    private final String timezone;
    private final double latitude;
    private final double longitude;
    private final String dataList;
    private final String location;

    private final Environment env;    
    private final RestTemplate restTemplate;
    private ObjectMapper objectMapper;

    private final String receiverUrl = "http://receiver-app:8080/api/weather/data";


    public ApiService(RestTemplate restTemplate, Environment env, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.env = env;
        this.objectMapper = objectMapper;

        this.baseUrl = getPropertyOrTerminate("WEATHER_API_BASEURL");
        this.timezone = getPropertyOrTerminate("WEATHER_API_TIMEZONE");
        this.latitude = Double.parseDouble(getPropertyOrTerminate("WEATHER_API_LATITUDE"));
        this.longitude = Double.parseDouble(getPropertyOrTerminate("WEATHER_API_LONGITUDE"));
        this.dataList = getPropertyOrTerminate("WEATHER_API_DATALIST");
        this.location = getPropertyOrTerminate("WEATHER_API_LOCATION");

    }

    private String getPropertyOrTerminate(String propertyName) {
        String value = env.getProperty(propertyName);
        if (value == null) {
            System.out.printf("Missing required environment variable: %s%n", propertyName);
            System.out.println("Application will terminate in 10 seconds...");
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.exit(1);
        }
        return value;
    }

    @Scheduled(fixedRate = 900000)
    public void fetchAndSendData() {

        String url = String.format("%s?latitude=%.4f&longitude=%.4f&current=%s&timezone=%s", baseUrl, latitude, longitude, dataList, timezone);

        String jsonData = restTemplate.getForObject(url, String.class);

        if (jsonData != null) {
            try {
                JsonNode rootNode = objectMapper.readTree(jsonData);
                JsonNode currentData = rootNode.path("current");

                String rawTime = currentData.path("time").asText();

                DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
                LocalDateTime dateTime = LocalDateTime.parse(rawTime, inputFormatter);

                DateTimeFormatter isoFormatter = DateTimeFormatter.ISO_DATE_TIME;
                String isoTimestamp = dateTime.format(isoFormatter);

                double temperature = currentData.path("temperature_2m").asDouble();
                int humidity = currentData.path("relative_humidity_2m").asInt();

                ObjectNode newJson = objectMapper.createObjectNode();

                newJson.put("timestamp", isoTimestamp);
                newJson.put("temperature", temperature);
                newJson.put("humidity", humidity);
                newJson.put("location", location);

                String newJsonString = objectMapper.writeValueAsString(newJson);
                System.out.println("data fetched: " + newJsonString);

                String response = restTemplate.postForObject(receiverUrl, newJson, String.class);

            } catch (IOException e) {
                System.err.println("Error parsing JSON data: " + e.getMessage());
            }
        } else {
            System.out.println("No data received from the API.");
        }
    }

    @PostConstruct
    public void fetchDataOnStart() {
        String url = String.format("%s?latitude=%.4f&longitude=%.4f&hourly=%s&timezone=%s&past_days=90",baseUrl, latitude, longitude, dataList, timezone);

        // String data = restTemplate.getForObject(url, String.class);
        System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
        // System.out.println("First time fetch Data: " + data);
        System.out.println("URL: " + url);
        System.out.println("LOCATION: " + location);
        System.out.println("startup finnished!");
        System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
