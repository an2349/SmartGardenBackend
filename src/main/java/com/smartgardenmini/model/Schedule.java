package com.smartgardenmini.model;

import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
@Table(name = "schedules")
public class Schedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String macId;

    @Column(nullable = false)
    private LocalTime time; // Giờ tưới (VD: 06:00, 17:30)

    @Column(nullable = false)
    private int durationSec; // Thời gian tưới (giây)

    @Column(nullable = false)
    private boolean enabled = true;

    // Ngày trong tuần: bitmask (1=CN, 2=T2, 4=T3, 8=T4, 16=T5, 32=T6, 64=T7)
    @Column(nullable = false)
    private int dayMask = 127; // 127 = tất cả các ngày

    public Schedule() {}

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

    public boolean matchesToday() {
        int dayOfWeek = java.time.LocalDate.now().getDayOfWeek().getValue(); // 1=T2...7=CN
        int bit = 1 << (dayOfWeek % 7); // CN=1, T2=2, T3=4, T4=8, T5=16, T6=32, T7=64
        return (dayMask & bit) != 0;
    }
}