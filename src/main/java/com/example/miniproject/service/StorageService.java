package com.example.miniproject.service;

import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import com.example.miniproject.model.QualityMetrics;
import com.example.miniproject.model.SessionEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;


@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final JdbcTemplate jdbcTemplate;
    private final TransferManager transferManager;
    private final ObjectMapper objectMapper;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    private static final int RETENTION_DAYS = 7;

    @Transactional
    public void storeEvent(SessionEvent event){
        String sql = "INSERT INTO session_events (session_id, event_type, timestamp, " +
                "startup_time_ms, buffering_duration_ms, bitrate, buffering_ratio, " +
                "error_count, device_type, region, content_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql,
                event.getSessionId(),
                event.getEventType(),
                event.getTimestamp(),
                event.getMetrics().getStartupTimeMs(),
                event.getMetrics().getBufferingDurationMs(),
                event.getMetrics().getBitrate(),
                event.getMetrics().getBufferingRatio(),
                event.getMetrics().getErrorCount(),
                event.getDeviceType(),
                event.getRegion(),
                event.getContentId()
        );

        if (isEventOld(event)){
            archiveToS3(event);
        }
    }

    private boolean isEventOld(SessionEvent event){
        return event.getTimestamp().isBefore(Instant.now().minus(RETENTION_DAYS, ChronoUnit.DAYS));
    }

    private void archiveToS3(SessionEvent event){
        try{
            String key = generateS3Key(event);
            String eventJson = objectMapper.writeValueAsString(event);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(eventJson.getBytes());

            Upload upload = transferManager.upload(bucketName, key, inputStream, null);

            CompletableFuture.runAsync(() ->{
                try{
                    upload.waitForCompletion();
                    log.info("Successfully archived event to S3: {}", key);
                }catch (Exception e){
                    log.error("Error archived to S3: {}", e.getMessage());
                }
            });
        }catch (Exception e){
            log.error("Error preparing event for S3 archive: {}", e.getMessage());

        }
    }

    private String generateS3Key(SessionEvent event){
        Instant timestamp = event.getTimestamp();
        return String.format("%tY/%tm/%td/%s/%s-%s.json",
                timestamp, timestamp, timestamp,
                event.getSessionId(),
                event.getEventType(),
                timestamp.toEpochMilli());
    }

    public List<SessionEvent> getRecentEvents(String sessionId){
        String sql = "SELECT * FROM session_events WHERE session_id = ?" +
                " AND timestamp > ? ORDER BY timestamp DESC";

        return jdbcTemplate.query(sql,
                (rs, rowNum) -> {
                    SessionEvent event = new SessionEvent();
                    event.setId(rs.getLong("Id"));
                    event.setSessionId(rs.getString("session_id"));
                    event.setEventType(rs.getString("event_type"));
                    event.setTimestamp(rs.getTimestamp("event_timestamp").toInstant());
                    event.setDeviceType(rs.getString("device_type"));
                    event.setRegion(rs.getString("region"));
                    event.setContentId(rs.getString("content_id"));

                    QualityMetrics metrics = new QualityMetrics();
                    metrics.setStartupTimeMs(rs.getLong("startup_time_ms"));
                    metrics.setBufferingDurationMs(rs.getLong("buffering_duration_ms"));
                    metrics.setBitrate(rs.getInt("bitrate"));
                    metrics.setBufferingRatio(rs.getDouble("buffering_ratio"));
                    metrics.setErrorCount(rs.getInt("error_count"));

                    event.setMetrics(metrics);
                    return event;
                }, sessionId, Instant.now().minus(RETENTION_DAYS, ChronoUnit.DAYS));

    }


}
