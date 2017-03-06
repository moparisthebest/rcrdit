/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */



$( document ).ready(function() {
    getSchedule2(null);
});

function padToTwoDigits(ss){
    ss = ""+ss;
    if(ss.length < 2){
        ss= "0"+ss;
    }
    return ss;
}


function getSchedule2(requestObject){
    if(isNull(requestObject)){
        requestObject = {"channelsPerPage" : "4000", "pageNum" : "1"};
    }
    $.ajax({
        url: 'rest/getSchedule',
        type: 'post',
        dataType: 'json',
        contentType: "application/json",
        data: JSON.stringify(requestObject),
        success: function (data) {
            var channelList = data.schedule;
            var requestObject = data.requestObject;
            var requestStartTimeEpochSeconds = requestObject.startTime.epochSecond;
            var requestEndTimeEpochSeconds = requestObject.endTime.epochSecond;
            
            
            $("#guideGoesHere").html("");
            $("#guideGoesHere").append(getProgramsHeader(requestObject));
            var channelUl = $("<ul></ul>").addClass("roundedBottom");
            var channelGroupsUl = $("<ul></ul>").attr("id","channelGroups").append($("<li></li>").append(channelUl));
            for(var idx in channelList){
                var individualChannelLi = $("<li></li>");
                var channel = channelList[idx];
                var currentPercentOver = 0;
                var channelProgramUl = $("<ul></ul>").attr("channelNum",channel.displayName);
                individualChannelLi.append(channelProgramUl);
                var channelNameLi = $("<li class='channel'></li>");
                channelNameLi.append("<span></span>").addClass("channelNumLabel").append(channel.displayName);
                channelProgramUl.append(channelNameLi);
                
                var programsLi = $("<li></li>").addClass("programs");
                for(var idx2 in channel.programs){
                    var program = channel.programs[idx2];
                    programsLi.append(getProgramDiv(program,requestObject,currentPercentOver));
                    currentPercentOver += program.widthPercent;
                }
                channelProgramUl.append(programsLi);
                channelUl.append(individualChannelLi);
            }
            $("#guideGoesHere").append(channelGroupsUl);
        },
        error:  function ( jqXHR, textStatus, errorThrown ){
            alert(errorThrown);
        }
    });
}

function formatTime(dateObj){
    var hours = dateObj.getHours();
    var returnString = "";
    switch(hours) {
        case 0:
            returnString = "12:00 AM";
            break;
        case 12:
            returnString = "12:00 PM";
            break;
        case 13:
            returnString = "1:00 PM";
            break;
        case 14:
            returnString = "2:00 PM";
            break;
        case 15:
            returnString = "3:00 PM";
            break;
        case 16:
            returnString = "4:00 PM";
            break;
        case 17:
            returnString = "5:00 PM";
            break;
        case 18:
            returnString = "6:00 PM";
            break;
        case 19:
            returnString = "7:00 PM";
            break;
        case 20:
            returnString = "8:00 PM";
            break;
        case 21:
            returnString = "9:00 PM";
            break;
        case 22:
            returnString = "10:00 PM";
            break;
        case 23:
            returnString = "11:00 PM";
            break;
        default:
            returnString = hours+":00 AM";
    }
    return returnString;
}

function getProgramsHeader(requestObject){
    var d = new Date();
    
    var heading = $("<h1></h1>").append("&nbsp;&nbsp;&nbsp;&nbsp;TV Schedule: ");
    var dateDiv = $().append("THE DATE GOES HERE");
    
    var emptrySpacerLi = $("<li></li>").addClass("navlspacer");
    var numHoursDisplayed = Math.ceil((requestObject.endTime.epochSecond-requestObject.startTime.epochSecond)/60/60);
    var hourBlockWidth = (100.0/numHoursDisplayed)+"%";
    
    var listOfHoursDisplayed = $("<li></li>").addClass("hours")
    
    var firstHourDate = new Date(requestObject.startTime.epochSecond*1000);
    firstHourDate.setMinutes(0);
    for(var i=0;i<numHoursDisplayed;i++){
        var displayTime = formatTime(firstHourDate);
        var hour = $("<div></div>").attr("style","width: "+hourBlockWidth).append($("<p>"+displayTime+"</p>"));
        listOfHoursDisplayed.append(hour);
        firstHourDate.setHours(firstHourDate.getHours()+1);
    }
    var timeUl = $("<ul></ul>").addClass("hours").append(emptrySpacerLi).append(listOfHoursDisplayed);
    
    var innerLi = $("<li></li>").addClass("nav").addClass("first").append(timeUl);
    
    
    var timeDisplayDiv = $("<div></div>").attr("id","timeDisplayDiv").append(innerLi);
    
    var guideHeader = $("<div></div>").addClass("guide-header").append(heading).append(dateDiv).append(timeDisplayDiv);
    var guideHeaderContainer = $("<div class='guide-header-container'></div>").attr("style","height: 77px;").append(guideHeader);
    return guideHeaderContainer;
}

function getProgramDiv(program, requestObject, startAtPercent){
    var programDiv = $("<div></div>").addClass("channelProgram");
    var titleInfoDiv = $("<div></div>").addClass("titleInfo");
    var otherStuffDiv = $("<div></div>").addClass("otherProgramStuff");
    titleInfoDiv.append(program.title);
    if(!isNull(program.subTitle)){
        titleInfoDiv.append($("<p>"+program.subTitle+"</p>").addClass("programSubtitle"));
    }
    programDiv.append(titleInfoDiv);
    programDiv.append(otherStuffDiv);
    
    var requestStartTimeEpochSeconds = requestObject.startTime.epochSecond;
    var requestEndTimeEpochSeconds = requestObject.endTime.epochSecond;
    var showingMinutes = (requestEndTimeEpochSeconds-requestStartTimeEpochSeconds)/60;
    var durationSeconds = program.stop.epochSecond-program.start.epochSecond;
    var displayDurationSeconds = durationSeconds;
    if(program.start.epochSecond < requestStartTimeEpochSeconds){
        displayDurationSeconds = displayDurationSeconds-(requestStartTimeEpochSeconds-program.start.epochSecond);
    }
    if(program.stop.epochSecond > requestEndTimeEpochSeconds){
        displayDurationSeconds = displayDurationSeconds-(program.stop.epochSecond-requestEndTimeEpochSeconds);
    }
    var displayDurationMinutes = displayDurationSeconds/60;
    
    var widthPercent = (displayDurationMinutes/showingMinutes)*100;
    programDiv.attr("style","left: "+startAtPercent+"%; width: "+widthPercent+"%");
    program.widthPercent = widthPercent;
    return programDiv;
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