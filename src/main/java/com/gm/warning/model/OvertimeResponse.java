package com.gm.warning.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OvertimeResponse {

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("data")
    private List<OvertimeItem> data = new ArrayList<>();

    @JsonProperty("message")
    private String message;

    @JsonProperty("errorCode")
    private int errorCode;

    public OvertimeResponse() {}

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public List<OvertimeItem> getData() { return data; }
    public void setData(List<OvertimeItem> data) { this.data = data; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public int getErrorCode() { return errorCode; }
    public void setErrorCode(int errorCode) { this.errorCode = errorCode; }
}
