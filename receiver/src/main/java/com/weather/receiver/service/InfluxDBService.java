package com.weather.receiver.service;

import com.weather.receiver.model.WeatherData;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Service
public class InfluxDBService {

    private final InfluxDB influxDB;

    public InfluxDBService(
        @Value("${influxdb.url}") String influxDBUrl,
        @Value("${influxdb.username}") String username,
        @Value("${influxdb.password}") String password,
        @Value("${influxdb.database}") String databaseName) {

        this.influxDB = InfluxDBFactory.connect(influxDBUrl, username, password);

        influxDB.query(new org.influxdb.dto.Query("CREATE DATABASE " + databaseName + " WITH DURATION 90d"));

        influxDB.setDatabase(databaseName);
    }

    private long parseTimestamp(String timestamp) {
        DateTimeFormatter iso8601Formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        LocalDateTime localDateTime = LocalDateTime.parse(timestamp, iso8601Formatter);
        Instant instant = localDateTime.atZone(ZoneId.of("America/Sao_Paulo")).toInstant();
        return instant.toEpochMilli();
    }

    public void saveData(WeatherData data) throws ParseException {
        System.out.println("Localização: " + data.getLocationName());
        System.out.println("Temperatura: " + data.getTemperature());
        System.out.println("Umidade: " + data.getHumidity());
        System.out.println("Timestamp: " + data.getTimestamp());
        System.out.println(" ");

        Point point = Point.measurement("weather_readings")
                .time(parseTimestamp(data.getTimestamp()), TimeUnit.MILLISECONDS)
                .tag("location", data.getLocationName())
                .addField("temperature", data.getTemperature())
                .addField("humidity", data.getHumidity())
                .build();

        influxDB.write(point);
    }

}
