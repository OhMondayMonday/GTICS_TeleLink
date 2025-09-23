package com.example.telelink.model;

public class DniResponse {
    private boolean success;
    private DniData data;
    private double time;
    private String source;

    // Getters y setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public DniData getData() { return data; }
    public void setData(DniData data) { this.data = data; }
    public double getTime() { return time; }
    public void setTime(double time) { this.time = time; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}