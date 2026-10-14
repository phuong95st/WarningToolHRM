package com.gm.warning.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LeaveRequestItem {

    @JsonProperty("id")
    private int id;

    @JsonProperty("employeeId")
    private String employeeId;

    @JsonProperty("employeeName")
    private String employeeName;

    @JsonProperty("department")
    private String department;

    @JsonProperty("startDate")
    private String startDate;

    @JsonProperty("endDate")
    private String endDate;

    @JsonProperty("days")
    private double days;

    @JsonProperty("leaveType")
    private String leaveType;

    @JsonProperty("leaveCategory")
    private String leaveCategory;

    @JsonProperty("leaveCategoryLabel")
    private String leaveCategoryLabel;

    @JsonProperty("paid")
    private boolean paid;

    @JsonProperty("reason")
    private String reason;

    @JsonProperty("status")
    private String status;

    @JsonProperty("managerId")
    private String managerId;

    @JsonProperty("managerNote")
    private String managerNote;

    @JsonProperty("createdAt")
    private String createdAt;

    @JsonProperty("updatedAt")
    private String updatedAt;

    public LeaveRequestItem() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public double getDays() { return days; }
    public void setDays(double days) { this.days = days; }

    public String getLeaveType() { return leaveType; }
    public void setLeaveType(String leaveType) { this.leaveType = leaveType; }

    public String getLeaveCategory() { return leaveCategory; }
    public void setLeaveCategory(String leaveCategory) { this.leaveCategory = leaveCategory; }

    public String getLeaveCategoryLabel() { return leaveCategoryLabel; }
    public void setLeaveCategoryLabel(String leaveCategoryLabel) { this.leaveCategoryLabel = leaveCategoryLabel; }

    public boolean isPaid() { return paid; }
    public void setPaid(boolean paid) { this.paid = paid; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getManagerId() { return managerId; }
    public void setManagerId(String managerId) { this.managerId = managerId; }

    public String getManagerNote() { return managerNote; }
    public void setManagerNote(String managerNote) { this.managerNote = managerNote; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
