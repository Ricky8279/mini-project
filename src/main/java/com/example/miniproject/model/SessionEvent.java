package com.example.miniproject.model;

import lombok.Data;
import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;

@Data
@Entity
@Table(name = "session_events")
public class SessionEvent implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sessionId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private Instant timestamp;

    @Embedded
    private QualityMetrics metrics;

    private String deviceType;
    private String region;
    private String contentId;

}
