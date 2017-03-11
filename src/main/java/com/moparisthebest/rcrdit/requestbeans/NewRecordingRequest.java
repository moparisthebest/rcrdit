/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.moparisthebest.rcrdit.requestbeans;

/**
 *
 * @author Jeff
 */
public class NewRecordingRequest {
    protected Long profileNo;
    protected String title;
    protected String channelName;
    protected String startTime;
    protected String stopTime;
    protected Long startDateEpochSeconds;
    protected Long endDateEpochSeconds;
    protected Long priority;

    public Long getProfileNo() {
        return profileNo;
    }

    public void setProfileNo(Long profileNo) {
        this.profileNo = profileNo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public Long getStartDateEpochSeconds() {
        return startDateEpochSeconds;
    }

    public void setStartDateEpochSeconds(Long startDateEpochSeconds) {
        this.startDateEpochSeconds = startDateEpochSeconds;
    }

    public Long getEndDateEpochSeconds() {
        return endDateEpochSeconds;
    }

    public void setEndDateEpochSeconds(Long endDateEpochSeconds) {
        this.endDateEpochSeconds = endDateEpochSeconds;
    }

    public Long getPriority() {
        return priority;
    }

    public void setPriority(Long priority) {
        this.priority = priority;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getStopTime() {
        return stopTime;
    }

    public void setStopTime(String stopTime) {
        this.stopTime = stopTime;
    }
    
    
}
