<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    
    <title>My JSP 'columnBasic.jsp' starting page</title>
    
	<meta http-equiv="pragma" content="no-cache">
	<meta http-equiv="cache-control" content="no-cache">
	<meta http-equiv="expires" content="0">    
	<meta http-equiv="keywords" content="keyword1,keyword2,keyword3">
	<meta http-equiv="description" content="This is my page">
	<%--
	<link rel="stylesheet" type="text/css" href="styles.css">
	--%>
	<link rel="stylesheet" type="text/css" href="../../css/base.css"/>
<link rel="stylesheet" type="text/css" href="../../css/public.css"/>
<link rel="stylesheet" type="text/css" href="../css/leftMenu.css"></script>
<link rel="stylesheet" type="text/css" href="css/statements.css" />
<link rel="stylesheet" type="text/css" href="../../jslib/jquery/css/jquery.treeview.css" />
<link rel="stylesheet" type="text/css" href="../../jslib/dialog/dialog.css"/>
		<script type="text/javascript" src="../../jslib/jquery/jquery-1.6.2.min.js"></script>
		<script type="text/javascript" src="../../jslib/highcharts/highcharts.src.js"></script>
		<script type="text/javascript" src="../../jslib/highcharts/exporting.src.js"></script>
		<script type="text/javascript">
				//获得上个月在昨天这一天的日期   
$(function() {
	loadWork();
});
  function getLastMonthYestdy(date){   

     var daysInMonth = new Array([0],[31],[28],[31],[30],[31],[30],[31],[31],[30],[31],[30],[31]);   

     var strYear = date.getFullYear();     

     var strDay = date.getDate();     

     var strMonth = date.getMonth()+1;   

     if(strYear%4 == 0 && strYear%100 != 0){   

        daysInMonth[2] = 29;   

     }   

     if(strMonth - 1 == 0)   

     {   

        strYear -= 1;   

        strMonth = 12;   

     }   

     else  

     {   

        strMonth -= 1;   

     }   

     strDay = daysInMonth[strMonth] >= strDay ? strDay : daysInMonth[strMonth];   

     if(strMonth<10)     

     {     

        strMonth="0"+strMonth;     

     }   

     if(strDay<10)     

     {     

        strDay="0"+strDay;     

     }   

     datastr = strYear+"-"+strMonth+"-"+"01 00:00:00";   

     return datastr;   

  }

  

  	//获得6个月在昨天这一天的日期   

  function getLastSixMonthYestdy(date,count){   

     var daysInMonth = new Array([0],[31],[28],[31],[30],[31],[30],[31],[31],[30],[31],[30],[31]);   

     var strYear = date.getFullYear();     

     var strDay = date.getDate();     

     var strMonth = date.getMonth()+1;   

     if(strYear%4 == 0 && strYear%100 != 0){   

        daysInMonth[2] = 29;   

     }   

     if(strMonth - count < 0)   

     {   

        strYear -= 1;   

        strMonth = 12 + (strMonth - count);   

     }   
	
	 else if(strMonth - count == 0)   

     {   

        strYear -= 1;   

        strMonth = 12;   

     }   
     else  

     {   

        strMonth -= count;   

     }   

     strDay = daysInMonth[strMonth] >= strDay ? strDay : daysInMonth[strMonth];   

     if(strMonth<10)     

     {     

        strMonth="0"+strMonth;     

     }   

     if(strDay<10)     

     {     

        strDay="0"+strDay;     

     }   

     datastr = strYear+"-"+strMonth+"-"+"01 00:00:00";   

     return datastr;   

  }	
  
  	function getFirstAndLastMonthDay(i){  
  		var aDate = new Date();
		var year=aDate.getFullYear();
		var month=aDate.getMonth()+1;
		month = month - i;
		if(month <= 0){
			month = 12 + month;
			year = year -1;
		}
		var monthString = "-" + month;
		if(month < 10){
			monthString = "-0"+month;
		}
	   var   firstdate = year + monthString + '-01 00:00:00';
	   var  day = new Date(year,month,0); 
	   var lastdate = year + monthString + '-' + day.getDate() + ' 23:59:59';//获取当月最后一天日期  
	   var dates= new Array(); //定义一数组;
	   dates[0] = firstdate;
	   dates[1] = lastdate;
	   return dates;
	}

      function loadWork(){
      var date = new Date();
      var date2 = getFirstAndLastMonthDay(5);
      var date3 = getFirstAndLastMonthDay(4);
      var date4 = getFirstAndLastMonthDay(3);
      var date5 = getFirstAndLastMonthDay(2);
      var date6 = getFirstAndLastMonthDay(1);
      var date7 = getFirstAndLastMonthDay(0);
      
      var startTime = date2[0]+","+date3[0]+","+date4[0]+","+date5[0]+","+date6[0]+","+date7[0];
      var endTime = date2[1]+","+date3[1]+","+date4[1]+","+date5[1]+","+date6[1]+","+date7[1];
		//alert(startTime);
		//alert(endTime);
      var cdate2 = date2[0].substring(0,7);
      var cdate3 = date3[0].substring(0,7);
      var cdate4 = date4[0].substring(0,7);
      var cdate5 = date5[0].substring(0,7);
      var cdate6 = date6[0].substring(0,7);
      var cdate7 = date7[0].substring(0,7);
      var series2 = "{ type: 'column', yAxis: 1,name: '抢修工单数量', data: [";
      var series1 = "{ type: 'line' ,name: '处理平均历时(小时)', data: [";
      var orgName = "";
      var params = "";
      var projectRoOrg = $("#projectName").val();
      if(projectRoOrg == "" || projectRoOrg == null){
      	$("#container").html("<em><p style='font-size: 18px; line-height: 200px; color: rgb(153, 153, 153);'>没有对应数据</p><em>");
      	return;
      }
		var arr =  projectRoOrg.split(",");
		var id = arr[0];
		var type = arr[1];
      if(type == "Org"){
		params = {orgId:id,type:type,startTime:startTime,endTime:endTime};
		}else{
			params = {projectId:id,type:type,startTime:startTime,endTime:endTime};
		}
      $.ajax({
		url : "getPortalReportUrgentRepairReportReportAction",
		data : params,
		dataType: "json",//,
		type : 'POST',
		async:false,
		success : function(data) {
			if(data == "" || data == null){
				$("#container").html("<em><p style='font-size: 18px; line-height: 200px; color: rgb(153, 153, 153);'>没有对应数据</p><em>");
				return;
			}
			//alert(data);
			//data =  eval(data);
			$.each(data.value,function(index,value){
			//alert(value.workOrderCount + value.time);
      		series2 = series2 + value.workOrderCount+",";
      		series1 = series1 + value.count+",";
      		});
      		orgName = data.bizName;
		}	
    });
    if(series2 != null && series2 != ""){
    	series2 = series2.substring(0,series2.length - 1);
    }
    if(series1 != null && series1 != ""){
    	series1 = series1.substring(0,series1.length - 1);
    }
    var series  = "[" + series2 + "]},"+ series1 +"]}]";
    //alert(series);
    series = eval(series); 
    var title = $("#projectName option:selected").text() + "("+cdate2+"~"+cdate7+")";
    var categories = [cdate2,cdate3,cdate4,cdate5,cdate6,cdate7];
    var yAxisTitle = "";
	var yAxisTitle2 = "";
      columnAndSplineBasic(title,"","","","container",categories,series);
      }
      
      
      
      	  
var columnAndSplineBasicChart;	
		//柱状图+折线(对比)
		function columnAndSplineBasic(title1,subtitle1,yAxisTitle,yAxisTitle2,divId,categories,series){
       	 columnBasicChart = new Highcharts.Chart({
            chart: {
                renderTo: divId,
                zoomType: 'xy'
            },
            title: {
                text: title1
            },
            subtitle: {
                text: subtitle1
            },
            xAxis: {
                categories: categories
                ,
                labels: {
                    rotation: -45,
                    align: 'right',
                    style: {
                        fontSize: '12px',
                        fontFamily: 'Verdana, sans-serif'
                    }
                }
            },
            yAxis: [{ // Primary yAxis
                labels: {
                    formatter: function() {
                        return this.value;
                    },
                    style: {
                        color: '#AA4643'
                    }
                },
                title: {
                    text: yAxisTitle,
                    style: {
                        color: '#AA4643'
                    }
                },
                opposite: true
    
            }, { // Secondary yAxis
                gridLineWidth: 0,
                title: {
                    text: yAxisTitle2,
                    style: {
                        color: '#4572A7'
                    }
                },
                labels: {
                    formatter: function() {
                        return this.value;
                    },
                    style: {
                        color: '#4572A7'
                    }
                }
    
            }],
            
            tooltip: {
                	formatter: function() {
    
                    return ''+
                        this.x +':'+this.series.name+':'+ this.y;
                }
                 
            },
            plotOptions: {
                column: {
                	dataLabels: {
                        enabled: true
                    },
                    pointPadding: 0.2,
                    borderWidth: 0,
                    point: {
                        events: {
                            click: function(e) {
                            	clickShowReport(categories[this.x]);
                            }
                        }
                    }
                },
                spline: {
                    dataLabels: {
                        enabled: true
                    }
                }
            },
                series: series
        });
		}      
		
		function clickShowReport(time){
			window.open("reportIndex.jsp?time="+time);
		}
		</script>
	</head>

	
  <body>
    <div class="clearfix">
    	<s:if test="#type == 'Project'">
			<div class="statements_top">
		</s:if>
		<s:else>
			<div class="statements_top" style="display: none;">
		</s:else>
			<span style="margin-left:20px;" >统计范围:</span>
			<s:if test="entityList.size() > 1">
				<select id="projectName" onchange="loadWork();">
			</s:if>
			<s:else>
				<s:property value="orgRoProjectList[0].name"/>
				<select id="projectName" onchange="loadWork();" style="display: none;">
			</s:else>
				<s:iterator value="orgRoProjectList" id="vs" status="st2">
					<option value="<s:property value="#vs.id"/>,<s:property value="#vs.type"/>"><s:property value="#vs.name"/></option>
				</s:iterator>
			</select>
			
		</div>
		<div class="statements_img_half">
			<div id="container"  style="height: 300px; margin: 0 auto; text-align: center;">
				
			</div>
		</div>
	</div>
  </body>
</html>
