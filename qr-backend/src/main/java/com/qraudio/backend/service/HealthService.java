package com.qraudio.backend.service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.ReadinessState;
@Service
public class HealthService {
    private final JdbcTemplate jdbc;
    private final ApplicationAvailability availability;
    public HealthService(JdbcTemplate jdbc, ApplicationAvailability availability) {
        this.jdbc = jdbc; this.availability = availability;
    }
    public boolean ready() {
        if (availability.getReadinessState() != ReadinessState.ACCEPTING_TRAFFIC) return false;
        try { return Integer.valueOf(1).equals(jdbc.queryForObject("SELECT 1", Integer.class)); }
        catch (RuntimeException exception) { return false; }
    }
}
