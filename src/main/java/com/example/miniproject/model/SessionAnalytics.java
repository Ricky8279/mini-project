package com.example.miniproject.model;

import lombok.Data;

@Data
public class SessionAnalytics {
    private double averageStartupTimeMs;
    private long totalBufferingMs;
    private double averageBitrateKbps;
    private long totalErrors;
}