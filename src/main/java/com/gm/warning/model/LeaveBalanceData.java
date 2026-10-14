package com.gm.warning.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LeaveBalanceData {

    @JsonProperty("employeeId")
    private String employeeId;

    @JsonProperty("year")
    private int year;

    @JsonProperty("totalDays")
    private double totalDays;

    @JsonProperty("usedDays")
    private double usedDays;

    @JsonProperty("remainingDays")
    private double remainingDays;

    public LeaveBalanceData() {
    }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public double getTotalDays() { return totalDays; }
    public void setTotalDays(double totalDays) { this.totalDays = totalDays; }

    public double getUsedDays() { return usedDays; }
    public void setUsedDays(double usedDays) { this.usedDays = usedDays; }

    public double getRemainingDays() { return remainingDays; }
    public void setRemainingDays(double remainingDays) { this.remainingDays = remainingDays; }
}
