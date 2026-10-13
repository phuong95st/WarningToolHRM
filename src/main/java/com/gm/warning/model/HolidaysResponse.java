package com.gm.warning.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HolidaysResponse {

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("data")
    private List<HolidayItem> data = new ArrayList<>();

    @JsonProperty("message")
    private String message;

    @JsonProperty("errorCode")
    private int errorCode;

    public HolidaysResponse() {
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public List<HolidayItem> getData() { return data; }
    public void setData(List<HolidayItem> data) { this.data = data; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public int getErrorCode() { return errorCode; }
    public void setErrorCode(int errorCode) { this.errorCode = errorCode; }
}
