package com.gm.warning.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DailyLeaveItem {

    @JsonProperty("leaveCategory")
    private String leaveCategory;

    @JsonProperty("leaveCategoryLabel")
    private String leaveCategoryLabel;

    @JsonProperty("leaveType")
    private String leaveType;

    @JsonProperty("paid")
    private String paid;

    @JsonProperty("leaveScore")
    private String leaveScore;

    public DailyLeaveItem() {}

    public String getLeaveCategory() { return leaveCategory; }
    public void setLeaveCategory(String leaveCategory) { this.leaveCategory = leaveCategory; }

    public String getLeaveCategoryLabel() { return leaveCategoryLabel; }
    public void setLeaveCategoryLabel(String leaveCategoryLabel) { this.leaveCategoryLabel = leaveCategoryLabel; }

    public String getLeaveType() { return leaveType; }
    public void setLeaveType(String leaveType) { this.leaveType = leaveType; }

    public String getPaid() { return paid; }
    public void setPaid(String paid) { this.paid = paid; }

    public String getLeaveScore() { return leaveScore; }
    public void setLeaveScore(String leaveScore) { this.leaveScore = leaveScore; }

    public double getLeaveScoreValue() {
        if (leaveScore == null || leaveScore.isBlank()) return 0.0;
        try {
            return Double.parseDouble(leaveScore.trim());
        } catch (Exception ignored) {
            return 0.0;
        }
    }
}
