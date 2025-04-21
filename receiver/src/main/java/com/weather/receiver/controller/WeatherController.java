package com.weather.receiver.controller;

import com.weather.receiver.model.WeatherData;
import com.weather.receiver.service.InfluxDBService;

import java.text.ParseException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    @Autowired
    private InfluxDBService influxDBService;

    @PostMapping("/data")
    public ResponseEntity<String> receiveWeatherData(@RequestBody WeatherData weatherData) throws ParseException {
        influxDBService.saveData(weatherData);
        return new ResponseEntity<>("Weather data saved successfully", HttpStatus.CREATED);
    }
}