package com.gm.warning.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HolidayItem {

    @JsonProperty("id")
    private int id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("holidayDate")
    private String holidayDate;

    @JsonProperty("description")
    private String description;

    @JsonProperty("paid")
    private boolean paid;

    @JsonProperty("applicableContractTypes")
    private List<String> applicableContractTypes;

    @JsonProperty("deleted")
    private Boolean deleted;

    public HolidayItem() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getHolidayDate() { return holidayDate; }
    public void setHolidayDate(String holidayDate) { this.holidayDate = holidayDate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isPaid() { return paid; }
    public void setPaid(boolean paid) { this.paid = paid; }

    public List<String> getApplicableContractTypes() { return applicableContractTypes; }
    public void setApplicableContractTypes(List<String> applicableContractTypes) {
        this.applicableContractTypes = applicableContractTypes;
    }

    public Boolean getDeleted() { return deleted; }
    public void setDeleted(Boolean deleted) { this.deleted = deleted; }

    public boolean isDeleted() {
        return Boolean.TRUE.equals(deleted);
    }

    public LocalDate getLocalDate() {
        if (holidayDate == null || holidayDate.isBlank()) {
            return null;
        }
        return LocalDate.parse(holidayDate, DateTimeFormatter.ISO_LOCAL_DATE);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (Paid: %s)", holidayDate, name, paid);
    }
}
