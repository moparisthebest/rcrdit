/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

$( document ).ready(function() {
    getSchedule(null);
});

function padToTwoDigits(ss){
    ss = ""+ss;
    if(ss.length < 2){
        ss= "0"+ss;
    }
    return ss;
}


function getSchedule(requestObject){
    if(isNull(requestObject)){
        requestObject = {"channelsPerPage" : "100", "pageNum" : "1"};
    }
    $.ajax({
        url: 'rest/getSchedule',
        type: 'post',
        dataType: 'json',
        contentType: "application/json",
        data: JSON.stringify(requestObject),
        success: function (data) {
            $("#guideGoesHere").html("");
            var guideTable = $("<table border='1' cellpadding='1' cellspacing='0' style='border-collapse: collapse;'></table>");
            
            var channelList = data.schedule;
            var requestObject = data.requestObject;
            var requestStartTimeEpochSeconds = requestObject.startTime.epochSecond;
            var requestEndTimeEpochSeconds = requestObject.endTime.epochSecond;
            var requestStartDate = new Date(requestStartTimeEpochSeconds*1000);
            var requestEndDate = new Date(requestEndTimeEpochSeconds*1000);
            var nextDate = new Date(requestStartDate.getTime());
            nextDate.setHours(nextDate.getHours()+1);
            var timeDisplayRow = $("<tr><td></td><td colspan=60>"+padToTwoDigits(requestStartDate.getHours())+":"+padToTwoDigits(requestStartDate.getMinutes())+"</td><td colspan=60>"+padToTwoDigits(nextDate.getHours())+":"+padToTwoDigits(nextDate.getMinutes())+"</td></tr>");
            guideTable.append(timeDisplayRow);
            for(var idx in channelList){
                var channel = channelList[idx];
                var channelTableRow = $("<tr><td>"+channel.displayName+"</td></tr>");
                for(var idx2 in channel.programs){
                    var program = channel.programs[idx2];
                    var durationSeconds = program.stop.epochSecond-program.start.epochSecond;
                    var durationMinutes = durationSeconds/60;
                    var displayDurationSeconds = durationSeconds;
                    if(program.start.epochSecond < requestStartTimeEpochSeconds){
                        displayDurationSeconds = displayDurationSeconds-(requestStartTimeEpochSeconds-program.start.epochSecond);
                    }
                    if(program.stop.epochSecond > requestEndTimeEpochSeconds){
                        displayDurationSeconds = displayDurationSeconds-(program.stop.epochSecond-requestEndTimeEpochSeconds);
                    }
                    var displayDurationMinutes = displayDurationSeconds/60;
                    
                    
                    
                    var programText = program.title;
                    if(!isNull(program.subTitle)){
                        programText+="<br/><font style='font-size:8pt'>"+program.subTitle+"</font>";
                    }
                    channelTableRow.append("<td colspan='"+Math.ceil(displayDurationMinutes)+"'>"+programText+"</td>");
                }
                guideTable.append(channelTableRow);
            }
            $("#guideGoesHere").append(guideTable);
        },
        error:  function ( jqXHR, textStatus, errorThrown ){
            alert(errorThrown);
        }
    });
}


function isNull(item){
    if(item === null || typeof item === "undefined"){
        return true;
    }
    return false;
    
}