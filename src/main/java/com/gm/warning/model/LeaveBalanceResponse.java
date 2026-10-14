package com.gm.warning.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LeaveBalanceResponse {

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("data")
    private LeaveBalanceData data;

    @JsonProperty("errorCode")
    private int errorCode;

    @JsonProperty("httpStatusCode")
    private String httpStatusCode;

    public LeaveBalanceResponse() {
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public LeaveBalanceData getData() { return data; }
    public void setData(LeaveBalanceData data) { this.data = data; }

    public int getErrorCode() { return errorCode; }
    public void setErrorCode(int errorCode) { this.errorCode = errorCode; }

    public String getHttpStatusCode() { return httpStatusCode; }
    public void setHttpStatusCode(String httpStatusCode) { this.httpStatusCode = httpStatusCode; }
}
