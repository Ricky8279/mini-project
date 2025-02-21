package com.example.miniproject.model;

import lombok.Data;
import javax.persistence.Embeddable;


@Data
@Embeddable
public class QualityMetrics {
    private Long startupTimeMs;
    private Long bufferingDurationMs;
    private Long endTimeMs;
    private Integer bitrate;
    private Double bufferingRatio;
    private Integer errorCount;

}
