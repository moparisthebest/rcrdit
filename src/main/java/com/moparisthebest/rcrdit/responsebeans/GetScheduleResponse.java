/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.moparisthebest.rcrdit.responsebeans;

import com.moparisthebest.rcrdit.requestbeans.GetScheduleRequest;
import com.moparisthebest.rcrdit.xmltv.Channel;
import java.util.List;

/**
 *
 * @author Jeff
 */
public class GetScheduleResponse {
    protected GetScheduleRequest requestObject;
    protected List<Channel> schedule;
    
    public GetScheduleResponse(GetScheduleRequest requestObject, List<Channel> schedule) {
        this.requestObject = requestObject;
        this.schedule = schedule;
    }

    
    
    
    public GetScheduleRequest getRequestObject() {
        return requestObject;
    }

    public void setRequestObject(GetScheduleRequest requestObject) {
        this.requestObject = requestObject;
    }

    public List<Channel> getSchedule() {
        return schedule;
    }

    public void setSchedule(List<Channel> schedule) {
        this.schedule = schedule;
    }
    
    
    
}
