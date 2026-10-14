package com.gm.warning.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AttendanceSummaryData {

    @JsonProperty("employeeId")
    private String employeeId;

    @JsonProperty("employeeName")
    private String employeeName;

    @JsonProperty("department")
    private String department;

    @JsonProperty("totalScore")
    private double totalScore;

    @JsonProperty("attendanceScore")
    private double attendanceScore;

    @JsonProperty("holidayDays")
    private double holidayDays;

    @JsonProperty("workModeDays")
    private double workModeDays;

    @JsonProperty("annualLeaveDays")
    private double annualLeaveDays;

    @JsonProperty("actualPaidDays")
    private double actualPaidDays;

    @JsonProperty("dailyScores")
    private Map<String, Double> dailyScores = Collections.emptyMap();

    @JsonProperty("dailyWorkModes")
    private Map<String, DailyWorkModeItem> dailyWorkModes = Collections.emptyMap();

    @JsonProperty("dailyLeaves")
    private Map<String, DailyLeaveItem> dailyLeaves = Collections.emptyMap();

    public AttendanceSummaryData() {
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public double getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(double totalScore) {
        this.totalScore = totalScore;
    }

    public double getAttendanceScore() {
        return attendanceScore;
    }

    public void setAttendanceScore(double attendanceScore) {
        this.attendanceScore = attendanceScore;
    }

    public double getHolidayDays() {
        return holidayDays;
    }

    public void setHolidayDays(double holidayDays) {
        this.holidayDays = holidayDays;
    }

    public double getWorkModeDays() {
        return workModeDays;
    }

    public void setWorkModeDays(double workModeDays) {
        this.workModeDays = workModeDays;
    }

    public double getAnnualLeaveDays() {
        return annualLeaveDays;
    }

    public void setAnnualLeaveDays(double annualLeaveDays) {
        this.annualLeaveDays = annualLeaveDays;
    }

    public double getActualPaidDays() {
        return actualPaidDays;
    }

    public void setActualPaidDays(double actualPaidDays) {
        this.actualPaidDays = actualPaidDays;
    }

    public Map<String, Double> getDailyScores() {
        return dailyScores != null ? dailyScores : Collections.emptyMap();
    }

    public void setDailyScores(Map<String, Double> dailyScores) {
        this.dailyScores = dailyScores;
    }

    public Map<String, DailyWorkModeItem> getDailyWorkModes() {
        return dailyWorkModes != null ? dailyWorkModes : Collections.emptyMap();
    }

    public void setDailyWorkModes(Map<String, DailyWorkModeItem> dailyWorkModes) {
        this.dailyWorkModes = dailyWorkModes;
    }

    public Map<String, DailyLeaveItem> getDailyLeaves() {
        return dailyLeaves != null ? dailyLeaves : Collections.emptyMap();
    }

    public void setDailyLeaves(Map<String, DailyLeaveItem> dailyLeaves) {
        this.dailyLeaves = dailyLeaves;
    }

    /**
     * Tra cứu điểm công thực tế của một ngày (1..31)
     */
    public Double getScoreForDay(int day) {
        if (dailyScores == null || dailyScores.isEmpty()) return null;
        Double score = dailyScores.get(String.valueOf(day));
        if (score == null) {
            score = dailyScores.get(String.format("%02d", day));
        }
        return score;
    }

    /**
     * Tra cứu chế độ làm việc (Remote, ...) của một ngày (1..31)
     */
    public DailyWorkModeItem getWorkModeForDay(int day) {
        if (dailyWorkModes == null || dailyWorkModes.isEmpty()) return null;
        DailyWorkModeItem item = dailyWorkModes.get(String.valueOf(day));
        if (item == null) {
            item = dailyWorkModes.get(String.format("%02d", day));
        }
        return item;
    }

    /**
     * Tra cứu ngày nghỉ phép theo ngày (hỗ trợ cả khóa "yyyy-MM-dd" và khóa "d"/"dd")
     */
    public DailyLeaveItem getLeaveForDay(int year, int month, int day) {
        if (dailyLeaves == null || dailyLeaves.isEmpty()) return null;
        String dateKey = String.format("%04d-%02d-%02d", year, month, day);
        DailyLeaveItem item = dailyLeaves.get(dateKey);
        if (item == null) {
            item = dailyLeaves.get(String.valueOf(day));
        }
        if (item == null) {
            item = dailyLeaves.get(String.format("%02d", day));
        }
        return item;
    }
}
