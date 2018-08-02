package com.yaroslav.factorynfcreader;

import java.util.HashMap;
import java.util.Map;

public class Ticket {
    private String tagId;
    private String tagName;
    private String latitude;
    private String longitude;
    private String readTime;

    public Ticket() {}

    public Ticket(String id, String name, String lat, String lon, String time) {
        this.tagId = id;
        this.tagName = name;
        this.latitude = lat;
        this.longitude = lon;
        this.readTime = time;
    }

    public String getTagId() {
        return tagId;
    }

    public String getTagName() {
        return tagName;
    }

    public String getLatitude() {
        return latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public String getReadTime() {
        return readTime;
    }

    public void setTagId(String tagId) {
        this.tagId = tagId;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public void setReadTime(String readTime) {
        this.readTime = readTime;
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();

        result.put("id", getTagId());
        result.put("name", getTagName());
        result.put("latitude", getLatitude());
        result.put("longitude", getLongitude());
        result.put("date", getReadTime());

        return result;
    }
}


































