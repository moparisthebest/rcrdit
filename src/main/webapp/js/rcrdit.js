/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

var recordingProfiles = null;

$( document ).ready(function() {
    initializeToastr();
    initializeProgramInfoPopup();
    getRecordingProfiles();
    getSchedule2(null);
    moment.locale('en');
    $("#gotoTvGuide").click(function(){
        getSchedule2(null);
        $("#autoRecsGoHere").hide();
        $("#upcomingRecordingsGoHere").hide();
        $("#guideGoesHere").show();
    });
    
    $("#gotoAutoRecs").click(function(){
        $("#guideGoesHere").hide();
        $("#upcomingRecordingsGoHere").hide();
        getAutoRecs();
        $("#autoRecsGoHere").show();
        $("#programInfo").dialog("close");
    });
    
    $("#gotoUpcomingRecordings").click(function(){
        $("#guideGoesHere").hide();
        getUpcomingRecordings();
        $("#autoRecsGoHere").hide();
        $("#upcomingRecordingsGoHere").show();
        $("#programInfo").dialog("close");
    });
    getCurrentlyRecording();
    setInterval(getCurrentlyRecording,30000);
});


function getCurrentlyRecording(){
    $.ajax({
        url: 'rest/getCurrentlyRecording',
        type: 'post',
        dataType: 'json',
        success: function (data) {
            var nowRecordingTable = $("<table></table");
            for(var idx in data){
                nowRecordingTable.append($("<tr><td><span class='circle'></span></td><td>"+data[idx].program.title+"</td></tr>"));
            }
            $("#nowRecordingDiv").html(nowRecordingTable);
        },
        error:  function ( jqXHR, textStatus, errorThrown ){
            alert(errorThrown);
        }
    });
}

function getUpcomingRecordings(){
     $("#upcomingRecordingsGoHere").html("");
    
    $.ajax({
        url: 'rest/getUpcomingRecordings',
        type: 'post',
        dataType: 'json',
        success: function (data) {
           var upcomingRecordingsDiv = $("#upcomingRecordingsGoHere");
           var upcomingRecordingTable = $("<table></table");
           var lastDate = "";
           // we want this sorted by keys
           var keys = [];
           for(var key in data) {
               keys[keys.length] = key;
           }
           keys.sort();
           for (var i=0; i<keys.length; i++) {
               var idx = keys[i];
               var scheduledRec = data[idx];
               var startRecordingDate = new Date(0); 
               startRecordingDate.setUTCSeconds((idx/1000));
               var formattedDate = moment(startRecordingDate).format("YYYY-MM-DD");
               var formattedTime = moment(startRecordingDate).format("h:mm A");
               if(formattedDate !== lastDate){
                   upcomingRecordingTable.append($("<tr></tr>").append($("<td></td>").addClass("dateDisplayRow").append(formattedDate)));
                   lastDate = formattedDate;
               }
               var subtitle = scheduledRec.subTitle;
               if(subtitle === null)subtitle = "";
               
               upcomingRecordingTable.append($("<tr></tr>").append($("<td></td>").append("&nbsp;")).append($("<td></td>").append(formattedTime)).append($("<td></td>").append(scheduledRec.channelName)).append($("<td></td>").append(scheduledRec.title+" - "+subtitle)));
               
                
                
           } 
           upcomingRecordingsDiv.append(upcomingRecordingTable);
        },
        error:  function ( jqXHR, textStatus, errorThrown ){
            alert(errorThrown);
        }
    });
}

function sortStringsIgnoreCase(a, b) {
  var nameA = a.toUpperCase();
  var nameB = b.toUpperCase();
  if (nameA < nameB) {
    return -1;
  }
  if (nameA > nameB) {
    return 1;
  }

  // names must be equal
  return 0;
}

function sortAutoRecs(a, b) {
  return sortStringsIgnoreCase(a.title, b.title);
}

function getAutoRecs(){
     $("#autoRecsGoHere").html("");
    
    $.ajax({
        url: 'rest/getAutoRecs',
        type: 'post',
        dataType: 'json',
        success: function (data) {
           var autoRecDiv = $("#autoRecsGoHere");
           // we want this sorted by name
           var autoRecs = [];
           for(var idx in data) {
               autoRecs[autoRecs.length] = data[idx];
           }
           autoRecs.sort(sortAutoRecs);
           for (var i=0; i<autoRecs.length; i++) {
               var autoRec = autoRecs[i];
               // todo: display more autorec details, expire old autorecs
               autoRecDiv.append($("<tr></tr>").append($("<td></td>").append(autoRec.title)));
           } 
        },
        error:  function ( jqXHR, textStatus, errorThrown ){
            alert(errorThrown);
        }
    });
}

function initializeToastr(){

    toastr.options = {
      "closeButton": false,
      "debug": false,
      "newestOnTop": false,
      "progressBar": false,
      "positionClass": "toast-top-center",
      "preventDuplicates": false,
      "onclick": null,
      "showDuration": "300",
      "hideDuration": "1000",
      "timeOut": "5000",
      "extendedTimeOut": "1000",
      "showEasing": "swing",
      "hideEasing": "linear",
      "showMethod": "fadeIn",
      "hideMethod": "fadeOut"
    }
}

function padToTwoDigits(ss){
    ss = ""+ss;
    if(ss.length < 2){
        ss= "0"+ss;
    }
    return ss;
}

function forceScheduleUpdate(){
    $.ajax({
        url: 'rest/refreshAutoRecs',
        type: 'post',
        success: function (data) {
           alert("Refresh Scheduled");
        },
        error:  function ( jqXHR, textStatus, errorThrown ){
            alert(errorThrown);
        }
    });
    
}

function sortChannels(a, b) {
  //return sortStringsIgnoreCase(a.displayName, b.displayName);
  // these are basically decimals, so compare them like so
  return a.displayName - b.displayName;
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
            var requestObject = data.requestObject;
            var requestStartTimeEpochSeconds = requestObject.startTime.epochSecond;
            $("#guideGoesHere").html("");
            $("#guideGoesHere").append(getProgramsHeader(requestObject));
            var channelUl = $("<ul></ul>").addClass("roundedBottom");
            var channelGroupsUl = $("<ul></ul>").attr("id","channelGroups").append($("<li></li>").append(channelUl));

            var channels = [];
            for(var idx in data.schedule) {
                channels[channels.length] = data.schedule[idx];
            }
            channels.sort(sortChannels);

            for (var i=0; i<channels.length; i++) {
                var channel = channels[i];
                var individualChannelLi = $("<li></li>");
                var currentPercentOver = 0;
                var channelProgramUl = $("<ul></ul>").attr("channelNum",channel.displayName);
                individualChannelLi.append(channelProgramUl);
                var channelNameLi = $("<li class='channel'></li>");
                channelNameLi.data("channelObj",channel);
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
            $("#guideGoesHere").data("lastSearchObject",requestObject);
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
    
    var heading = $("<div style='height: 60px'><div text-align:middle; style='position:fixed; width:300px; left: 35px;'><h1>TV Schedule:</h1></div></div>");
    var datePickerTextBox = $("<input type='text' style='position: relative; z-index: 100000' class='datePickerTextBox'></input>").datepicker({
        onSelect: function(){
            var lastRequestObject = $("#guideGoesHere").data("lastSearchObject");
            var startDate = new Date(lastRequestObject.startTime.epochSecond*1000);
            var endDate = new Date(lastRequestObject.endTime.epochSecond*1000);
            var selectedDate = $(this).datepicker( "getDate" )
            startDate.setDate(selectedDate.getDate());
            startDate.setMonth(selectedDate.getMonth());
            startDate.setFullYear(selectedDate.getFullYear());
            endDate.setDate(selectedDate.getDate());
            endDate.setMonth(selectedDate.getMonth());
            endDate.setFullYear(selectedDate.getFullYear());
            if(endDate.getTime() < startDate.getTime()){
                endDate.setDate(endDate.getDate()+1);
            }
            delete lastRequestObject.startTime;
            delete lastRequestObject.endTime;
            lastRequestObject.startTimeString = startDate.toISOString();
            lastRequestObject.endTimeString = endDate.toISOString();
            getSchedule2(lastRequestObject); 
        }
    }).datepicker("setDate",new Date(requestObject.startTime.epochSecond*1000));
    
    var dateDiv = $($("<div style='position: fixed; top: 25px; left: 250px;'></div>")).append(datePickerTextBox);
    
    heading.append(dateDiv);
    
    //heading.add(dateDiv);
    
    
    
    var leftArrowLink = $("<a href='javascript:void(0)' id='backArrow'><span class='helper'></span><img style='height:20px; width:20px' style=' vertical-align: middle; margin-top: auto; margin-bottom:auto' src='images/left.png'></img></a>");
    var goLeftDiv = $("<div class='frame' style='position:absolute; left:0px; line-height: 36px;'></div>").append(leftArrowLink);
    leftArrowLink.click(function(){
       var lastRequestObject = $("#guideGoesHere").data("lastSearchObject");
        var startDate = new Date(lastRequestObject.startTime.epochSecond*1000);
        var endDate = new Date(lastRequestObject.endTime.epochSecond*1000);
        startDate.setHours(startDate.getHours()-2);
        endDate.setHours(endDate.getHours()-2);
        delete lastRequestObject.startTime;
        delete lastRequestObject.endTime;
        lastRequestObject.startTimeString = startDate.toISOString();
        lastRequestObject.endTimeString = endDate.toISOString();
        getSchedule2(lastRequestObject); 
    });
    var emptrySpacerLi = $("<li></li>").addClass("navlspacer").append(goLeftDiv);
    
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
    
    var rightArrowLink = $("<a href='javascript:void(0)' id='forwardArrow'><span class='helper'></span><img style='height:20px; width:20px' style=' vertical-align: middle; margin-top: auto; margin-bottom:auto' src='images/right.png'></img></a>");
    var goRightDiv = $("<div class='frame' style='position:absolute; right:0px'></div>").append(rightArrowLink);
    rightArrowLink.click(function(){
        var lastRequestObject = $("#guideGoesHere").data("lastSearchObject");
        var startDate = new Date(lastRequestObject.startTime.epochSecond*1000);
        var endDate = new Date(lastRequestObject.endTime.epochSecond*1000);
        startDate.setHours(startDate.getHours()+2);
        endDate.setHours(endDate.getHours()+2);
        delete lastRequestObject.startTime;
        delete lastRequestObject.endTime;
        lastRequestObject.startTimeString = startDate.toISOString();
        lastRequestObject.endTimeString = endDate.toISOString();
        getSchedule2(lastRequestObject);
        
    });
    
    listOfHoursDisplayed.append(goRightDiv);
    
    var timeUl = $("<ul></ul>").addClass("hours").append(emptrySpacerLi).append(listOfHoursDisplayed);
    
    var innerLi = $("<li></li>").addClass("nav").addClass("first").append(timeUl);
    
    
    var timeDisplayDiv = $("<div></div>").attr("id","timeDisplayDiv").append(innerLi);
    
    var guideHeader = $("<div></div>").addClass("guide-header").append(heading).append(timeDisplayDiv);
    var guideHeaderContainer = $("<div class='guide-header-container'></div>").attr("style","height: 96px;").append(guideHeader);
    return guideHeaderContainer;
}

function getProgramDiv(program, requestObject, startAtPercent){
    var programDiv = $("<div></div>").addClass("channelProgram");
    var titleInfoDiv = $("<div></div>").addClass("titleInfo");
    var otherStuffDiv = $("<div></div>").addClass("otherProgramStuff");
    programDiv.data("programObj",program);
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
    programDiv.click(function(){
        $("#programInfoTable tr").remove();
        $(".recordSingleDiv, .recordMultiDiv").remove();
        $("#recordOptionsDiv").remove();
        var obj = $(this).data("programObj");
        $("#programInfoTable").data("programObj",obj);
        $("#programInfoTable").append($("<tr></tr>").addClass("programTitle").append($("<td></td>").html(obj.title)));
        if(!isNull(obj.subTitle)){
            $("#programInfoTable").append($("<tr></tr>").addClass("programSubtitleLine").append($("<td></td>").html(obj.subTitle)));
        }
        if(!isNull(obj.desc)){
            $("#programInfoTable").append($("<tr></tr>").append($("<td></td>").addClass("blankSpace")));
            $("#programInfoTable").append($("<tr></tr>").addClass("programDescription").append($("<td></td>").html(obj.desc)));
        }
        
        $("#programInfo").dialog("open");
        var recordSingleDiv = $("<div></div>").attr("class","recordSingleDiv");
        $("#programInfo").append(recordSingleDiv);
        recordSingleDiv.click(function(){
           var programToRecord = $("#programInfoTable").data("programObj");
           var recordingScheduleObject = {"profileNo":"1","title": programToRecord.title,"channelName": programToRecord.channelName,"priority": "9999999","startDateEpochSeconds" : programToRecord.start.epochSecond , "endDateEpochSeconds" : programToRecord.stop.epochSecond};
           $(".recordMultiDiv").css("background-color","transparent");
            $(this).css("background-color","#eaedf2");
           $(".multiScheduleOption").hide();
           $(".singleScheduleOption").show();
           $("#recordOptionsDiv").slideDown();
        });
        var recordMultiDiv = $("<div></div>").addClass("recordMultiDiv");
        $("#programInfo").append(recordMultiDiv);
        recordMultiDiv.click(function(){
           var programToRecord = $("#programInfoTable").data("programObj");
           $(".recordSingleDiv").css("background-color","transparent");
           $(this).css("background-color","#eaedf2");
           $(".singleScheduleOption").hide();
           $(".multiScheduleOption").show();
           $("#recordOptionsDiv").slideDown();
        });
        
        $("#programInfo").append(getRecordOptionsDiv(program));
    });
    return programDiv;
}

function getRecordOptionsDiv(program){
    var recordOptionsDiv = $("<div></div>").attr("id","recordOptionsDiv").css("display","none").css("width","100%");
    var recordOptionsTable = $("<table width='100%'></table>");
    var recordProfileSelectBox = $("<select id='profileSelectBox'></select>");
    for(var idx in recordingProfiles){
        var recordingProfile = recordingProfiles[idx];
        recordProfileSelectBox.append($("<option value='"+recordingProfile.profileId+"'>"+recordingProfile.name+"</option>"));
    }
    recordOptionsTable.append($("<tr></tr>").append($("<td>Profile: </td>")).append($("<td></td>").append(recordProfileSelectBox)).addClass("multiScheduleOption").addClass("singleScheduleOption"));
    recordOptionsTable.append($("<tr><td>Priority:</td><td><input type='text' id='recordingPriorityText' value='99999'/></td></tr>").addClass("multiScheduleOption").addClass("singleScheduleOption"));
    recordOptionsTable.append($("<tr><td>Channel:</td><td><input type='text' id='recordingChannelText' value='"+program.channelName+"'/></td></tr>").addClass("multiScheduleOption"));
    var startTimeTextBox = $("<input size=8 type='text' id='startTimeText'/>").addClass("multiScheduleOption");
    var endTimeTextBox = $("<input size=8 type='text' id='endTimeText'/>").addClass("multiScheduleOption");
    recordOptionsTable.append($("<tr><td>Between Hours:</td></tr>").append($("<td></td>").append(startTimeTextBox).append(" and ").append(endTimeTextBox)).addClass("multiScheduleOption"));
    var effectiveBeginTextBox = $("<input size=20 type='text' id='effectiveBeginText'/>")
    recordOptionsTable.append($("<tr><td>Effective Begin:</td></tr>").append($("<td></td>").append(effectiveBeginTextBox)).addClass("multiScheduleOption"));
    var effectiveEndTextBox = $("<input size=20 type='text' id='effectiveEndText'/>")
    recordOptionsTable.append($("<tr><td>Effective End:</td></tr>").append($("<td></td>").append(effectiveEndTextBox)).addClass("multiScheduleOption"));
    
    var scheduleButton = $("<button id='scheduleRecordingButton' class='ui-button ui-widget ui-corner-all'>Schedule</button>");
    recordOptionsTable.append($("<tr></tr>").append($("<td></td>").attr("colspan","2").attr("align","right").append(scheduleButton)));
    scheduleButton.click(function(){
        var advancedRecord = $("#effectiveBeginText").is(":visible");
        var programToRecord = $("#programInfoTable").data("programObj");
       
        //looking for a return of false.. meaning no errors
        if(!validateProgramRecordDialog(advancedRecord)){
            if(advancedRecord){
                scheduleAdvancedRecord(programToRecord);
            }else{
                scheduleSimpleRecord(programToRecord);
            }
            
        }
    });
    startTimeTextBox.timepicker();
    endTimeTextBox.timepicker();
    effectiveBeginTextBox.datetimepicker();
    effectiveEndTextBox.datetimepicker();
    recordOptionsDiv.append(recordOptionsTable);
    return recordOptionsDiv;
}

function scheduleSimpleRecord(programToRecord){
    var priority = $("#recordingPriorityText").val();
    if(isEmpty(priority))priority = "5";
    
    var profile = $("#profileSelectBox").val();
    if(isEmpty(profile))profile = "1";
    
    var recordingScheduleObject = {"profileNo":profile,"title": programToRecord.title,"channelName": programToRecord.channelName,"priority": priority,"startDateEpochSeconds" : programToRecord.start.epochSecond , "endDateEpochSeconds" : programToRecord.stop.epochSecond};
    scheduleRecording(recordingScheduleObject);
}

function scheduleAdvancedRecord(programToRecord){
    var priority = $("#recordingPriorityText").val();
    if(isEmpty(priority))priority = "5";

    var profile = $("#profileSelectBox").val();
    if(isEmpty(profile))profile = "1";
    
    var channel = $("#recordingChannelText").val();
    if(isEmpty(channel))channel = null;
    
    var effectiveBeginText = $("#effectiveBeginText").val();
    var effectiveBeginTimestamp =  moment(effectiveBeginText,["MM/DD/YYYY hh:mm"]).unix();
    if(isNaN(effectiveBeginTimestamp) || isEmpty(effectiveBeginText)){
        effectiveBeginTimestamp = null;
    }
    
    var effectiveEndText = $("#effectiveEndText").val();
    var effectiveEndTimestamp =  moment(effectiveEndText,["MM/DD/YYYY hh:mm"]).unix();
    if(isNaN(effectiveEndTimestamp) || isEmpty(effectiveEndText)){
        effectiveEndTimestamp = null;
    }
    
    var startTime = $("#startTimeText").val();
    if(isEmpty(startTime)){
        startTime = null;
    }
    var stopTime = $("#endTimeText").val();
    if(isEmpty(stopTime)){
        stopTime = null;
    }

    var recordingScheduleObject = {"profileNo":profile,"title": programToRecord.title,"channelName": channel,"priority": priority,"startDateEpochSeconds" : effectiveBeginTimestamp , "endDateEpochSeconds" : effectiveEndTimestamp, "startTime":startTime,"stopTime":stopTime};
    scheduleRecording(recordingScheduleObject); 
}

function validateProgramRecordDialog(advancedRecordMode){
    $("#recordOptionsDiv input").removeClass("entryError");
    $("#recordOptionsDiv select").removeClass("entryError");
    var error = false;
    if(advancedRecordMode){
        var effectiveBeginText = $("#effectiveBeginText").val();
        var effectiveBeginTimestamp =  moment(effectiveBeginText,["MM/DD/YYYY hh:mm"]).unix();
        var effectiveEndText = $("#effectiveEndText").val();
        var effectiveEndTimestamp =  moment(effectiveEndText,["MM/DD/YYYY hh:mm"]).unix();
        
        if(isNaN(effectiveBeginTimestamp) && !isEmpty(effectiveBeginText)){
            toastr["error"]("Effective Begin entry is not valid!");
            $("#effectiveBeginText").addClass("entryError");
            error = true;
        }
        if(isNaN(effectiveEndTimestamp) && !isEmpty(effectiveEndText)){
            toastr["error"]("Effective End entry is not valid!");
            $("#effectiveEndText").addClass("entryError");
            error = true;
        }
    }
    
    return error;
}

function scheduleRecording(recordingDetails){
    $.ajax({
        url: 'rest/recordSingleInstanceOfProgram',
        type: 'post',
        contentType: "application/json",
        data: JSON.stringify(recordingDetails),
        success: function (data) {
            $("#recordOptionsDiv").hide();
            setTimeout(getCurrentlyRecording,5000);
            toastr["success"]("Recording Scheduled!");
        },
        error:  function ( jqXHR, textStatus, errorThrown ){
            toastr["error"]("An error occurred while scheduling the recording!");
        }
    });
    
}

function getRecordingProfiles(){
    $.ajax({
        url: 'rest/getRecordingProfiles',
        type: 'post',
        dataType: 'json',
        success: function (data) {
            recordingProfiles = data;
        },
        error:  function ( jqXHR, textStatus, errorThrown ){
            alert(errorThrown);
        }
    });
    
}

function isEmpty(item){
    if(isNull(item))return true;
    if("" === $.trim(item))return true;
    return false;
}

function isNull(item){
    if(item === null || typeof item === "undefined"){
        return true;
    }
    return false;
    
}

function divExists(divId){
    var found = $("#"+divId);
    if(found.length === 0){
        return false;
    }
    return true;
}

function initializeProgramInfoPopup(){
    if(!divExists("programInfo")){
        var programInfoDiv = $("<div></div>").attr("id","programInfo");
        programInfoDiv.append($("<table></table>").attr("id","programInfoTable"));
        $("body").append(programInfoDiv);
    }
    $("#programInfoTable tr").remove();
            
    $("#programInfo").dialog({
        autoOpen: false,
        dialogClass: 'noTitleStuff2 fixed-dialog',
        width: "600",
        position: {"my": "center center", "at": "center top+15%", "of": "body"}
    });
}