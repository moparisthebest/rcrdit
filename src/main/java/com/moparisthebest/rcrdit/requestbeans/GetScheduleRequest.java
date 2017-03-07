/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.moparisthebest.rcrdit.requestbeans;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 *
 * @author Jeff
 */
public class GetScheduleRequest {
   protected String startTimeString;
   protected String endTimeString;
   protected int channelsPerPage = 100;
   protected int pageNum = 1;
   
   protected Instant startTime;
   protected Instant endTime;
   private final String dateTimeFormatPattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    public GetScheduleRequest() {
        setStartAndEndTimeBasedOnCurrentType();
    }
    
    
    private void setStartAndEndTimeBasedOnCurrentType(){
        LocalDateTime now =  LocalDateTime.now();
        LocalDateTime topOfHour = now.withMinute(0).withSecond(0).withNano(0);
        setStartTime(topOfHour.toInstant(ZoneOffset.systemDefault().getRules().getOffset(topOfHour)));
        LocalDateTime endOfNextHour = now.plusHours(2).withMinute(59).withSecond(59).withNano(0);
        setEndTime(endOfNextHour.toInstant(ZoneOffset.systemDefault().getRules().getOffset(endOfNextHour)));
    }
    
     public void setStartAndEndTimeBasedOnLastDataAvailable(Instant lastDataAvailable){
        LocalDateTime lastDataAvailableLdt = LocalDateTime.ofInstant(lastDataAvailable,ZoneId.systemDefault());
        LocalDateTime newLastDate = lastDataAvailableLdt.minusHours(5).withMinute(59).withSecond(59).withNano(0);
        LocalDateTime newStartTimeLdt = newLastDate.minusHours(2).withMinute(0).withSecond(0).withNano(0);
        setStartTime(newStartTimeLdt.toInstant(ZoneOffset.systemDefault().getRules().getOffset(newStartTimeLdt)));
        setEndTime(newLastDate.toInstant(ZoneOffset.systemDefault().getRules().getOffset(newLastDate)));
    }
     
     public static Instant getLastPossibleDateToDisplayOnSchedule(Instant lastDataAvailable){
        LocalDateTime lastDataAvailableLdt = LocalDateTime.ofInstant(lastDataAvailable,ZoneId.systemDefault());
        LocalDateTime newLastDate = lastDataAvailableLdt.minusHours(5).withMinute(59).withSecond(59).withNano(0);
        return newLastDate.toInstant(ZoneOffset.systemDefault().getRules().getOffset(newLastDate));
     }
   
   
   
    public String getStartTimeString() {
        return startTimeString;
    }
    
    public void setStartTimeString(String startTimeString, boolean setStartTimeObject){
        if(setStartTimeObject){
            setStartTimeString(startTimeString);
        }else{
            this.startTimeString = startTimeString;
        }
    }

    public void setStartTimeString(String startTimeString) {
        this.startTimeString = startTimeString;
        if(startTimeString != null){
            startTime = Instant.parse(startTimeString);
            LocalDateTime topOfHour = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
            Instant topOfHourInstant = topOfHour.toInstant(ZoneOffset.systemDefault().getRules().getOffset(topOfHour));
            if(startTime.equals(topOfHourInstant) || startTime.isBefore(topOfHourInstant)){
                setStartAndEndTimeBasedOnCurrentType();
            }
            
        }
        
    }

    public String getEndTimeString() {
        return endTimeString;
    }

    public void setEndTimeString(String endTime) {
        this.endTimeString = endTime;
        if(endTimeString != null){
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateTimeFormatPattern).withZone( ZoneOffset.UTC );
            this.endTime = Instant.parse(endTimeString);
            LocalDateTime endOfNextHour = LocalDateTime.now().plusHours(2).withMinute(59).withSecond(59).withNano(0);
            Instant endOfHourInstant = endOfNextHour.toInstant(ZoneOffset.systemDefault().getRules().getOffset(endOfNextHour));
            if(this.endTime.equals(endOfHourInstant) || this.endTime.isBefore(endOfHourInstant)){
                setStartAndEndTimeBasedOnCurrentType();
            }
        }
    }

    public int getChannelsPerPage() {
        return channelsPerPage;
    }

    public void setChannelsPerPage(int channelsPerPage) {
        this.channelsPerPage = channelsPerPage;
    }

    public int getPageNum() {
        return pageNum;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public final void setStartTime(Instant startTime) {
        this.startTime = startTime;
        if(this.startTime != null){
            this.startTimeString = DateTimeFormatter.ofPattern(dateTimeFormatPattern).withZone(ZoneOffset.UTC).format(this.startTime);
        }
    }

    public Instant getEndTime() {
        return endTime;
    }

    public final void setEndTime(Instant endTime) {
        this.endTime = endTime;
        if(this.endTime != null){
            this.endTimeString = DateTimeFormatter.ofPattern(dateTimeFormatPattern).withZone(ZoneOffset.UTC).format(this.endTime);
        }
    }
   
   
}
