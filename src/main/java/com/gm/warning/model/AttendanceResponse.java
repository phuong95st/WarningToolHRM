package com.gm.warning.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AttendanceResponse {

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("data")
    private List<AttendanceRecord> data = new ArrayList<>();

    @JsonProperty("errorCode")
    private int errorCode;

    @JsonProperty("httpStatusCode")
    private String httpStatusCode;

    public AttendanceResponse() {
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public List<AttendanceRecord> getData() {
        return data;
    }

    public void setData(List<AttendanceRecord> data) {
        this.data = data;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getHttpStatusCode() {
        return httpStatusCode;
    }

    public void setHttpStatusCode(String httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }
}
