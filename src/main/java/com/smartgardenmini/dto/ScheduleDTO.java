package com.smartgardenmini.dto;

import java.time.LocalTime;

public class ScheduleDTO {
    private Long id;
    private String macId;
    private LocalTime time;
    private int durationSec;
    private boolean enabled;
    private int dayMask;

    public ScheduleDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMacId() { return macId; }
    public void setMacId(String macId) { this.macId = macId; }
    public LocalTime getTime() { return time; }
    public void setTime(LocalTime time) { this.time = time; }
    public int getDurationSec() { return durationSec; }
    public void setDurationSec(int durationSec) { this.durationSec = durationSec; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getDayMask() { return dayMask; }
    public void setDayMask(int dayMask) { this.dayMask = dayMask; }
}