package com.gm.warning.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HrmNotificationItem {

    @JsonProperty("id")
    private int id;

    @JsonProperty("title")
    private String title;

    @JsonProperty("content")
    private String content;

    @JsonProperty("targetRole")
    private String targetRole;

    @JsonProperty("targetEmployeeId")
    private String targetEmployeeId;

    @JsonProperty("createdBy")
    private String createdBy;

    @JsonProperty("createdAt")
    private String createdAt;

    @JsonProperty("read")
    private boolean read;

    public HrmNotificationItem() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getTargetRole() { return targetRole; }
    public void setTargetRole(String targetRole) { this.targetRole = targetRole; }

    public String getTargetEmployeeId() { return targetEmployeeId; }
    public void setTargetEmployeeId(String targetEmployeeId) { this.targetEmployeeId = targetEmployeeId; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    @Override
    public String toString() {
        return String.format("[#%d] %s: %s", id, title, content);
    }
}
