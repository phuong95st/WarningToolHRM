package com.gm.warning.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AttendanceRecord {

    @JsonProperty("status")
    private String status;

    @JsonProperty("work_date")
    private String workDate;

    @JsonProperty("check_in")
    private String checkIn;

    @JsonProperty("check_out")
    private String checkOut;

    public AttendanceRecord() {
    }

    public AttendanceRecord(String status, String workDate, String checkIn, String checkOut) {
        this.status = status;
        this.workDate = workDate;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getWorkDate() {
        return workDate;
    }

    public void setWorkDate(String workDate) {
        this.workDate = workDate;
    }

    public String getCheckIn() {
        return checkIn;
    }

    public void setCheckIn(String checkIn) {
        this.checkIn = checkIn;
    }

    public String getCheckOut() {
        return checkOut;
    }

    public void setCheckOut(String checkOut) {
        this.checkOut = checkOut;
    }

    public LocalDate getWorkLocalDate() {
        if (workDate == null || workDate.isBlank()) {
            return null;
        }
        return LocalDate.parse(workDate, DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public LocalTime getCheckInTime() {
        if (checkIn == null || checkIn.isBlank()) {
            return null;
        }
        String trimmed = checkIn.trim();
        if (trimmed.length() > 5) {
            trimmed = trimmed.substring(0, 5);
        }
        try {
            return LocalTime.parse(trimmed, DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            return null;
        }
    }

    public LocalTime getCheckOutTime() {
        if (checkOut == null || checkOut.isBlank()) {
            return null;
        }
        String trimmed = checkOut.trim();
        if (trimmed.length() > 5) {
            trimmed = trimmed.substring(0, 5);
        }
        try {
            return LocalTime.parse(trimmed, DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isCheckedIn() {
        return checkIn != null && !checkIn.isBlank();
    }

    public boolean isCheckedOut() {
        return checkOut != null && !checkOut.isBlank();
    }

    public boolean isFullWork() {
        return "ĐỦ CÔNG".equalsIgnoreCase(status);
    }

    public boolean isCurrentlyWorking() {
        return "ĐANG LÀM VIỆC".equalsIgnoreCase(status) || (isCheckedIn() && !isCheckedOut());
    }

    @Override
    public String toString() {
        return String.format("[%s] In: %s | Out: %s | Trạng thái: %s",
                workDate,
                checkIn != null ? checkIn : "--:--",
                checkOut != null ? checkOut : "--:--",
                status != null ? status : "N/A");
    }
}
