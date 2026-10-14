package com.gm.warning.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DailyWorkModeItem {

    @JsonProperty("workMode")
    private String workMode;

    @JsonProperty("period")
    private String period;

    @JsonProperty("approvedScore")
    private Double approvedScore;

    @JsonProperty("label")
    private String label;

    @JsonProperty("periodLabel")
    private String periodLabel;

    public DailyWorkModeItem() {}

    public String getWorkMode() { return workMode; }
    public void setWorkMode(String workMode) { this.workMode = workMode; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public Double getApprovedScore() { return approvedScore; }
    public void setApprovedScore(Double approvedScore) { this.approvedScore = approvedScore; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getPeriodLabel() { return periodLabel; }
    public void setPeriodLabel(String periodLabel) { this.periodLabel = periodLabel; }
}
