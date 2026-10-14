package com.gm.warning.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OvertimeItem {

    @JsonProperty("id")
    private int id;

    @JsonProperty("requestType")
    private String requestType;

    @JsonProperty("workDate")
    private String workDate;

    @JsonProperty("startTime")
    private String startTime;

    @JsonProperty("endTime")
    private String endTime;

    @JsonProperty("requestedHours")
    private Double requestedHours;

    @JsonProperty("requestedHoursInput")
    private Double requestedHoursInput;

    @JsonProperty("approvedHours")
    private Double approvedHours;

    @JsonProperty("reason")
    private String reason;

    @JsonProperty("status")
    private String status;

    @JsonProperty("reviewNote")
    private String reviewNote;

    @JsonProperty("createdAt")
    private String createdAt;

    @JsonProperty("project")
    private OvertimeProject project;

    @JsonProperty("approver")
    private OvertimeApprover approver;

    public OvertimeItem() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getRequestType() { return requestType; }
    public void setRequestType(String requestType) { this.requestType = requestType; }

    public String getWorkDate() { return workDate; }
    public void setWorkDate(String workDate) { this.workDate = workDate; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public Double getRequestedHours() { return requestedHours; }
    public void setRequestedHours(Double requestedHours) { this.requestedHours = requestedHours; }

    public Double getRequestedHoursInput() { return requestedHoursInput; }
    public void setRequestedHoursInput(Double requestedHoursInput) { this.requestedHoursInput = requestedHoursInput; }

    public Double getApprovedHours() { return approvedHours; }
    public void setApprovedHours(Double approvedHours) { this.approvedHours = approvedHours; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public OvertimeProject getProject() { return project; }
    public void setProject(OvertimeProject project) { this.project = project; }

    public OvertimeApprover getApprover() { return approver; }
    public void setApprover(OvertimeApprover approver) { this.approver = approver; }
}
