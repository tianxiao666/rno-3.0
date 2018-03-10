var map = null;
var currentInfoWindow = null;
var queryConditions ={};	    //查询条件
var FrameMap = {"ListView":"ListView","MapView":"MapView"};
var currentFrame = "ListView";    //当前员工信息展现形式(ListView:列表展现形式 ; MapView:地图展现形式)
queryConditions._carNumber="";      	//车辆牌照
queryConditions._carBizId="";  		//所属区域id
queryConditions._isArranged="";		//是否排班
queryConditions._dutyDate="";    	//上班日期
queryConditions._carType="";		//车类型
queryConditions._freId = "";	//值班类型
queryConditions._terminalState = "";	//车辆定位状态
queryConditions.currentPage =1;
queryConditions.totalPage =1;
var totalPage;	  //总页数
var queryCarList = new Array();		//查询结果(车辆列表)
var autoRefresh = null;
$(function(){
	//车牌输入框提示
	promptCueDiv($("#carNumber"),"请输入车牌号");
	$("#carNumber_div").css({
			"font-size":"12px"  
	});
	//$("#carNumber_div").removeAttr("style");
	
	$(".search_tab ul li").each(function(index){
		$(this).click(function(){
			$(".search_tab ul li").removeClass("ontab");
			$(this).addClass("ontab");
			$(".search_tab_content").hide().eq(index).show();
		})
	})
		
	/*展示tab*/
	$(".present_tab li a").each(function(index){
		$(this).click(function(){
			$(".present_tab li a").removeClass("select").eq(index).addClass("select")
			$(".present_tab_content").hide().eq(index).show();
		})
	})
	
	
	var now = new Date().toString("yyyy-MM-dd");
	$("#dutyDate").val(now);
	
	searchProviderOrgTree();
	
	$("#bizunitNameText").click(function(){
		$("#treeDiv").css("display","block");
	});
	
	
	
	
	$("#treeDiv").hover(function(){},function(){
		$(this).slideUp("fast");
	});
	
	
	
	$("#gisBizName").click(function(){
		$("#treeDiv3").toggle();
	});
	
	
	$("#orgSelectButton3").click(function(){
		if($("#treeDiv3").css("display") == "none"){
			$("#treeDiv3").show();
		}else{
			$("#treeDiv3").hide();
		}
	});
	
	
	$("#orgSelectButton").click(function(){
		if($("#treeDiv").css("display") == "none"){
			$("#treeDiv").show();
		}else{
			$("#treeDiv").hide();
		}
	});
	$("#orgSelectButton2").click(function(){
		if($("#treeDiv2").css("display") == "none"){
			$("#treeDiv2").show();
		}else{
			$("#treeDiv2").hide();
		}
	});
	
	$("#gis_content_right_show_img").click(function(){
		
		if($(".gis_right_information_top").is(":visible"))
		{
			$(".gis_right_information_top").hide();
			$(".gis_right_information").css("width","0px");
			
		}
		else
		{
			$(".gis_right_information_top").show();
			$(".gis_right_information").css("width","330px");
		}
	});
	//右边信息框Table切换
	$(".top_tab ul h4").each(function(index){
		$(this).click(function(){
			$(".top_tab ul h4").removeClass("active");
			$(this).addClass("active");
			$(".gis_content_right_main").hide();
			$(".gis_content_right_main").eq(index).show();
			
		})
	});
	
	$(".top_tab ul li").each(function(index){
		$(this).click(function(){
			$(".top_tab ul li").removeClass("active");
			$(this).addClass("active");
			$(".top_tab_content").hide();
			$(".top_tab_content").eq(index).show();
		})
	});
	
	//普通查询按钮
	$("#simpleQueryButton").click(function(){
		$("#hiddenCarNumber").val("");
		if(currentInfoWindow){
			currentInfoWindow.close();
		}
		var carNumber=$("#carNumber").val();
		
		var carType = $("#carType").val();
		var carState = $("#carState").val();
		
		var _carBizId = $("#bizunitIdText").val();
		queryConditions._carNumber=carNumber;
		queryConditions._carBizId = _carBizId;
		queryConditions._freId = "";
		queryConditions._carType = carType;
		queryConditions._isArranged = "";
		queryConditions._dutyDate = "";
		queryConditions._terminalState = carState;
		if ($("#MapView").css("display") == "none") {
			showListViewByConditions(queryConditions);
		} else {
			//在地图上显示数据
			showMapViewByConditions(queryConditions,null);
		}
		$("#carNumber").val("");
	});
	$("#multiSearchButton").click(function(){
		$("#hiddenCarNumber").val("");
		if(currentInfoWindow){
			currentInfoWindow.close();
		}
		$(".gis_right_information_display").css("right","0px");
		$(".left_hide_grey").show();
		//班次
		var shiftType = "";
		$(".shiftType").each(function(){
			if($(this).attr("checked") == "checked"){
				shiftType+=$(this).val()+",";
			}
		});
		if(shiftType!=""){
			shiftType = shiftType.substring(0,shiftType.length-1);
		}
		var dutyDate = $("#dutyDate").val();
		queryConditions._carNumber="";
		queryConditions._dutyDate = dutyDate;
		queryConditions._isArranged = "是";
		queryConditions._freId = shiftType;
		queryConditions._carBizId = $("#carBizId").val();	//TODO 获取组织Id
		queryConditions._terminalState = "";
		if ($("#MapView").css("display") == "none") {
			showListViewByConditions(queryConditions);
		} else {
			//按条件在地图上展现员工信息
			showMapViewByConditions(queryConditions,null);
		};
		$(".advanced-search").slideToggle("fast");
	});

	//车辆搜索框
	//信息弹框Table切换
	$(".dialog-tabmenu li").live("click",function(){
   		$(".dialog-tabmenu li").removeClass("on");
   		$(this).addClass("on");
   		$(".dialog-tabcontent dd").hide();
   		var curIndex = $(".dialog-tabmenu li").index($(this));
   		$(this).parent().parent().find($(".dialog-tabcontent")).find("dd").eq(curIndex).show();
   	})
	//取消按钮
	$("#cancelButton").click(function(){
		$(".advanced-search").toggle("fast");
	});
	//地图、列表切换
	$("#listViewButton").click(function(){
		currentFrame = FrameMap.ListView;
		if($("#listView").css("display") == "none"){
			$("#listView").css("display","block");
			$("#MapView").css("display","none");
			$(this).addClass("tab2-li-show");
			$("#mapViewButton").removeClass("tab2-li-show");
		}
		var hiddenCarNumber = $("#hiddenCarNumber").val();
		var _carBizId = $("#bizunitIdText").val();
		if(hiddenCarNumber && hiddenCarNumber!=""){
			queryConditions._carNumber = hiddenCarNumber;
			queryConditions._carBizId = _carBizId;
		}
		showListViewByConditions(queryConditions);
	});
	//地图切换
	$("#mapViewButton").click(function(){
		//显示地图
		if(map==null){
		    initMap();
		}
		if($("#hiddenCarNumber").val()!=null && $("#hiddenCarNumber").val()!=""){
			$("#listView").css("display","none");
			$("#MapView").css("display","block");
		}
		if(polylineMarkers.length>0){
			return false;
		}
		
		$(".loading_cover").show();
		
		
		currentFrame = FrameMap.MapView;
		if(currentFrame==FrameMap.MapView){
			$("#listView").css("display","none");
			$("#MapView").css("display","block");
			if($("#rightInformation").css("display") == "block"){
				$("#replayDiv").css("display","block");
			}else{
				$("#replayDiv").css("display","none");
			}
			$(this).addClass("tab2-li-show");
			$("#listViewButton").removeClass("tab2-li-show");
			setTimeout(function(){
				showMapViewByConditions(queryConditions,null);
			},300);
		}
		
	});
	//开启自动刷新
	autoRefresh = setInterval(function(){
		if($("#listView").css("display") == "block"){
			$("#listViewButton").click();
		}else{
			//清除车辆轨迹
			cleanTaskPolylineArray();
			cleanMarkersAndPolyline();
			$("#mapViewButton").click();
		}
	},60000) 
	//自动刷新按钮事件
	$("#autoRefreshButton").click(function(){
		if($(this).attr("checked") == "checked"){
			var timer = $("#autoRefreshTime").val();
			autoRefresh = setInterval(function(){
				if($("#listView").css("display") == "block"){
					$("#listViewButton").click();
				}else{
					$("#mapViewButton").click();
				}
			},timer) 
		}else{
			if(autoRefresh!=null){
				//清除定时器
				clearInterval(autoRefresh);
				autoRefresh = null;
			}
		}
	});
	//自动刷新时间间隔
	$("#autoRefreshTime").change(function(){
		var timer = $(this).val();
		//判断是否需要自动刷新
		if($("#autoRefreshButton").attr("checked") == "checked"){
			if(autoRefresh!=null){
				//清除定时器
				clearInterval(autoRefresh);
				autoRefresh = null;
			}
			autoRefresh = setInterval(function(){
				if($("#listView").css("display") == "block"){
					$("#listViewButton").click();
				}else{
					//清除车辆轨迹
					cleanTaskPolylineArray();
					cleanMarkersAndPolyline();
					$("#mapViewButton").click();
				}
			},timer) 
		}
	});
	
	//预填当前日期
	var currentTime=new Date();
	currentTime.setDate(currentTime.getDate() - 1);
	var year=currentTime.getFullYear();
	var month=currentTime.getMonth()+1;
	var date=currentTime.getDate();
	if(month<10){month = "0"+month;}
	if(date<10){date = "0"+date;}
	currentTime = year+"-"+month+"-"+date;
	$("#locusDate").val(currentTime);
	$("#dutyDate").val(currentTime);
	
	//判断页面传参，是否轨迹回放
	var hiddenCarNumber = $("#hiddenCarNumber").val();
	var hiddenBeginTime = $("#hiddenBeginTime").val();
	if(hiddenCarNumber!=null && hiddenCarNumber!=""){
		//清除车辆轨迹
		cleanTaskPolylineArray();
		cleanMarkersAndPolyline();
        queryConditions._carBizId = $("#bizunitIdText").val();
		queryConditions._carNumber = hiddenCarNumber;
		queryConditions._carId = "";
		if(map==null){
			initMap();
		}
		$("#MapView").css("display","block");
		$("#mapViewButton").attr("class","tab2-li-show");
		$("#listViewButton").removeAttr("class");
		$("#listView").css("display","none");
		if(autoRefresh!=null){
			//清除定时器
			clearInterval(autoRefresh);
			autoRefresh = null;
		}
		$("#autoRefreshButton").removeAttr("checked");
		if(hiddenBeginTime!=null && hiddenBeginTime!=""){
			var hiddenDate = hiddenBeginTime.split(" ")[0];
			$("#locusDate").val(hiddenDate);
		}
		var curPosition = $("#curPosition").val();
		if(curPosition && curPosition!=""){
			showMapViewByConditions(queryConditions,hiddenCarNumber);
			return false;
		}
		
		
		setTimeout(function(){
			showCarLocus(hiddenCarNumber);
		},1500);
	}else{
		 //默认获取司机车辆信息
    	initDriverCarInfo();
	}
	
	
	$("#queryCarNumber").click(function(){
		autoComplete();
	})
	
	
	
		    
});
function moreMenu(carId,carNumber,be){
	$("#moreMenuDiv").html("");
	var div = $("<a href='javascript:void(0)' onclick=\"viewCarInfo('"+carId+"')\">查看车辆详情</a><br/><a href='javascript:void(0)' onclick=\"linkToCar('"+carNumber+"')\">地图定位车辆</a>")
	div.appendTo($("#moreMenuDiv"));
	var offset = $(be).offset();
	$("#moreMenuDiv").css("top",offset.top + 20 + "px");
	$("#moreMenuDiv").css("left",offset.left + 20 + "px");
	$("#moreMenuDiv").show();
	$("#moreMenuDiv").hover(function(){},function(){
		$("#moreMenuDiv").hide();
	});
}
function linkToCar(carNumber){
	if(map==null){
		initMap();
	}
	cleanTaskPolylineArray();
	cleanMarkersAndPolyline();
	
	$("#MapView").css("display","block");
	$("#mapViewButton").attr("class","tab2-li-show");
	$("#listViewButton").removeAttr("class");
	$("#listView").css("display","none");
	queryConditions._carBizId = $("#bizunitIdText").val();
	$(".loading_cover").show();
	showMapViewByConditions(queryConditions,carNumber);
	$(".loading_cover").hide();
	$("#moreMenuDiv").hide();
}
//默认获取车辆司机信息
function initDriverCarInfo(){
    queryConditions._carNumber = "";
    queryConditions._carBizId = $("#bizunitIdText").val();
    var curFrame = $("#hiddenFrame").val();
    if(curFrame!=null && curFrame!=""){
		$("#mapViewButton").click();
		return false;
	}
/*	var queryUrl="cardispatchWorkorder_ajax!findCarListWithDuty.action";
	$.ajax( {
		url : queryUrl,
		cache : false,
		data : queryConditions,
		async : false,
		type : "POST",
		dataType : 'json',
		success : function(result) {
			showResultTable(result);
		}
	});*/
	//yuan.yw
	var queryUrl="cardispatchWorkorder!findCarListForMonitor.action";
	queryConditions.currentPage = 1;
	var param = queryConditions;
	pagingColumnByBackgroundJsp("pageContent","listView1",queryUrl,param);
};
/**
 * 获取车辆任务信息
 */
function getCarTaskInfoForGantt(carId,be){
	createGanttContent("ctGantt","ctGanttContent","ctDatepicker",carId,"car",null);
	var offset = $(be).offset();
	$("#ctGantt").css("top",offset.top + 20 + "px");
	$("#ctGantt").css("left",offset.left - 50 + "px");
	$("#ctGantt").show();
	$("#ctGantt").hover(function(){},function(){
		$(this).hide();
	});
}
/**
 * 显示数据
 * @param {Object} json
 */
function showResultTable(dataList){
	$("#resultListTable").html("");
	var table = $("#resultListTable");
    //表头
	var headTr = $("" 
                +"<tr class='thead'>"
                +"	<th>车辆牌照</th>"
                +"  <th>任务总数</th>"
                +"  <th>车辆归属地</th>"
                +"  <th>车辆类型</th>"
                +"  <th>司机姓名</th>"
                +"  <th>司机电话</th>"
                +"  <th>定位状态</th>"
                +"  <th>是否排班</th>"
                +"  <th>当前位置</th>"//yuan.yw
                +"  <th>当前里程</th>"
                +"  <th>操作</th>"
                +"</tr>");
	headTr.appendTo(table);
	//数据
	if(dataList){
		for(var i=0;i<dataList.length;i++){
			var data = dataList[i];
			var dataTr = $("<tr class='pageTr'></tr>");
			
			var carNumber = data.carNumber ;
			var carId = data.carId;
			var carDriverPairId = data.carDriverPairId ;
			var accountId = data.accountId;
			var driverName = data.driverName;
			var driverPhone = data.driverPhone;
			var isArranged = data.isArranged;
			var carBizName = data.carBizName;
			var carType = data.carType;
			var terminalState = data.terminalState;
			
			if(!carNumber){carNumber=""};
			if(!carDriverPairId){carDriverPairId=""};
			if(!carBizName){carBizName="";}
			if(!carType){carType="";}
			if(!driverName){driverName="";}
			if(!driverPhone){driverPhone="";}
			if ( terminalState == "0" ) {
				terminalState = "离线";
			} else if ( terminalState == "1" ) {
				terminalState = "行驶中";
			} else if ( terminalState == "2" ) {
				terminalState = "静止";
			} else if ( terminalState == "3" ){
				terminalState = "待初始化";
			} else {
				terminalState = "待初始化";
			}
			
			var carNumberTd = $("<td><input type='hidden' value='"+carId+"'/><a href='cargeneral_index.jsp?carId="+carId+"&type=view' target='_blank' >"+carNumber+"</a></td>");
			var carBizNameTd = $("<td>"+carBizName+"</td>");
			var typeTd = $("<td>"+carType+"</td>");
			var totalTask = data['totalTask'];
            var driverName = $("<td>"+driverName+"</td>");
            var phoneTd = $("<td>"+driverPhone+"</td>");
            var terminalStateTd = $("<td>"+terminalState+"</td>");
            
            var totalTask = $("<td><a href=\"#\" onclick=\"getCarTaskInfoForGantt('"+carId+"',this)\">"+totalTask+"</a></td>");
              //判断是否排班控制样式
            if(isArranged=='是'){
            	isArranged = $("<td style='background-color: green;'>"+isArranged+"</td>");
            }else{
            	isArranged = $("<td>"+isArranged+"</td>");
            }
            var op = $("<td><a class='map_ico position' href='javascript:void(0)' title='地图定位' onclick=\"linkToCar('"+carNumber+"')\"></a><a class='map_ico operate' target='_blank' href=\"loadCarStateMonitoringPageAction?carNumber="+encodeURI(encodeURI(carNumber))+"&beginTime="+$("#locusDate").val()+" 00:00:00&endTime="+$("#locusDate").val()+" 23:59:59&display=none\" title='轨迹回放' ></a></td>");
            var address = $("<td id='address"+carId+"' style='width:250px'><img src='images/newLoading.gif'></td>");
            carNumberTd.appendTo(dataTr);
            totalTask.appendTo(dataTr);
            carBizNameTd.appendTo(dataTr);
            typeTd.appendTo(dataTr);
            driverName.appendTo(dataTr);
            phoneTd.appendTo(dataTr);
            terminalStateTd.appendTo(dataTr);
		    isArranged.appendTo(dataTr);
		    address.appendTo(dataTr);
		    op.appendTo(dataTr);
		    dataTr.appendTo(table);	
		    //yuan.yw
		    var longitude = data.jingdu;
			var latitude = data.weidu;		
			var params = {carId:carId,longitude:longitude,latitude:latitude};
			
		    $.post("cardispatchWorkorder_ajax!getAddressByLngLatAction.action",params,function(data){
		    	if(data.address==""){
		    		$("#address"+data.carId).html("&nbsp;");
		    	}else{
		    		$("#address"+data.carId).html(data.address);
		    	}
		    	
		    },"json");
		}
	}
	//分页
	pagingColumnByForeground("pageContent",$(".pageTr"),10);
}

//初始化地图
function initMap(){
    var center = new ILatLng(23.14651977,113.34106554);
    map = new IMap("map_canvas",center,14,{});
    currentInfoWindow = new IInfoWindow("",{"position":center});
    map.addContextMenu();
}

//工作区自适应
function workspaceSelfAdaption(){
//    var mainDiv = $("#mainDiv");
    var allWidth = $(window).width();
    var allHeight = $(window).height();

    //垂直
    var v1 = $("#vertical_div1");
    var v2 = $("#vertical_div2");
    var v3 = $("#vertical_div3");
    var vh1 = v1.height();
    var vh2 = v2.height();
    v3.height(allHeight-vh1-vh2);  
    
    //水平
    var h1 = $("#horizontal_div1");
    var h2 = $("#horizontal_div2");
    var h3 = $("#horizontal_div3");
    var hw1 = h1.width();
    var hw2 = h2.width();
    h3.width(allWidth-hw1-hw2);
    
    $(document).resize();
}

/**
 * 按搜索条件展现地图
 * @param {Object} conditions
 */
function showMapViewByConditions(queryConditions,carNumber){
	if(playCar!=null){
		return false;
	}
	var carIds = "";
	$("#resultListTable .pageTr").each(function(index){
		if($(this).css("display")=="table-row"){
			var carId = $(this).find("input[type='hidden']").val();
			carIds += carId+",";
		}
	});
	if(carIds!=""){
		carIds = carIds.substring(0,carIds.length-1);
	}
	queryConditions._carId = carIds;
	var queryUrl="cardispatchWorkorder_ajax!findCarListWithDutyByGis.action";
	$.ajax( {
		url : queryUrl,
		cache : false,
		data : queryConditions,
		async : false,
		type : "POST",
		dataType : 'json',
		success : function(result) {
			var queryResult = result;
			//清除地图上的所有标记
			cleanAllMarkers();
            if(queryResult){
               showCarListOnMap(queryResult);
            }
            if(carNumber!=null){
            	for(var i=0;i<queryCarList.length;i++){
					var car = queryCarList[i];
					if(car.carNumber_ == carNumber){
						setTimeout(function(){
							map.panTo(car.getPosition());
						},1000);
						setTimeout(function(){
							showMarkerDetail(car);
						},1500);
						return false;
					}
				}
            }
		}
	})
	
	$(".loading_cover").hide();
}
/**
 * 清除地图上的所有标记
 */
function cleanAllMarkers(){
	for(var i=0;i<queryCarList.length;i++){
		var marker = queryCarList[i];
		marker.textOverlay_.setMap(null);
		marker.textOverlay_ = undefined;
		marker.setMap(null);
	}
	queryCarList = new Array();
}

/**
 * 显示车辆列表
 * @param {Object} carList
 * @return {TypeName} 
 */
function showCarListOnMap(carList){
	if(!carList)return ; //参数过滤
	var panFlag = false;  //是否已经移动过
	for(var i=0;i<carList.length;i++){
		var car = carList[i];
		if(!car)continue;
		var marker = createCarMarker(car);
		if(!panFlag&&marker!=null&&car.jingdu!=114.22222){
			map.panTo(marker.getPosition());
			panFlag=true;
		}
		if(marker){
			marker.bizValues_ = car;	//将业务信息添加到marker对象中
			bindMarkerEvent(marker);
			marker.setMap(map);
			marker.textOverlay_.setMap(map);
			queryCarList.push(marker);
		}
	}
}

/**
 * 生成车辆在地图上显示的图标
 * @param {Object} car 当前车辆信息
 */
function createCarMarker(car){
	if(!car)return null;
	
	if (typeof(car.weidu) == "undefined" || car.weidu == 0 || car.weidu == "undefined") 
	{
		car.jingdu = 114.22222;
		car.weidu = 23.55555;
	}
	var lat = car.weidu;  //当前纬度
	var lng = car.jingdu;  //当前经度
	lat = stringToFloat(""+lat);
	lng = stringToFloat(""+lng);
	if(!lat){lat=0.0;}
	if(!lng){lng=0.0;}
	//员工所在的的GPS坐标
    var position = new ILatLng(lat,lng);
    //坐标校准
    var position = IMapComm.gpsToMapLatLng(position);
    
    //在地图上添加标记
    var marker = new IMarker(position,car.carNumber,{});
    var markerOptions = {};
    
    var carState = car.terminalState;
   // LineOff(离线) : 0 ,Travel (行驶中 ) : 1 ,Static (静止 ) : 2 ,Init (待初始化 ) : 3 
    var carIcon = "icon-for-map/car-LineOff.png";	//离线或待初始化
    if(carState && carState=='1'){
    	carIcon = "icon-for-map/car-Travel.png";
    }else if(carState && carState=='2'){
    	carIcon = "icon-for-map/car-Static.png";
    }
    markerOptions.icon = carIcon;
    marker.setOptions(markerOptions);
    
    var name = car.carNumber;
    var statusLabel = "<lable style='color: #0000FF;font-size: 12px;font-weight: bold;font-style: italic;'>"+name+"</lable>";
    marker.textOverlay_ = new ITextOverlay(position,statusLabel,{});
    
    marker.carNumber_ = name;
    
    return marker;
}

/**
 * 将字符串转换成浮点数
 */
function stringToFloat(str){
	var num = null;
	try{
		var re = parseFloat(str);
		num = re;
	}catch(err){
		
	}
	return num;
}

/**
 * 给标记绑定点击事件
 * @param {Object} marker
 */
function bindMarkerEvent(marker){
    var clickType = undefined;
	IMapEvent.addListener(marker, "click", function(){
		clickType = "click";
		setTimeout(function(){
			if(clickType=="click")
			showMarkerDetail(marker);
			clickType=undefined;
		},300);
	}); 
};



/**
 * 显示当前车辆的详细信息
 * @param {Object} marker
 * @return {TypeName} 
 */
function showMarkerDetail(marker){
	if(!currentInfoWindow)return;
	var position = marker.getPosition();
	var content = "";
	
	if("bizValues_" in marker){
		var carInfo = marker.bizValues_;
		
		var carNumber = carInfo.carNumber;
		var carId = carInfo.carId;
		var carType = carInfo.carType;
		var phone = carInfo.telphoneNo;
		var driverName = carInfo.driverName;
		
		if(!carType){carType="";}
		if(!phone){phone="";}
		if(!driverName){driverName="";}
		
		content += "<div class=\"dialog-title-bar clearfix\"><div class=\"dialog-bar-m\"><h3><a href=\"cargeneral_index.jsp?carId="+carId+"&type=view\" target='_blank' >"+carNumber+"</a>，"+carType+"<span class=\"dialog-ji\"></span></h3>";
		content+="<h3>"+driverName+"，"+phone+"</h3>";
		content+="</div></div>";
		
		var taskOrderList = null;
		$.ajax({
			url:"cardispatchWorkorder!findCardispatchWordorderByState.action",
			type:"post",
			data:{"workorder#carNumber":carNumber,"workorder#state":"unFinish"},
			dataType:"json",
			async:false,
			success:function(result){
			   	taskOrderList = result;
			}
		})
		
		var taskCount = 0;
		if(taskOrderList!=null){
			taskCount = taskOrderList.length;
		}
		content+="<div class=\"dialog-content clearfix\"><div class=\"dialog-content-box\"><div class=\"dialog-content-tab\"><ul class=\"dialog-tabmenu clearfix\"><li class=\"on\">出车任务(<span style=\"color:red\">"+taskCount+"</span>个)</li><li style=\"border-right:1px solid #ccc;\">甘特图</li></ul><dl class=\"dialog-tabcontent\"><dd><div class=\"dialog-task\"><ul>";
		if(taskOrderList!=null){
			for(var i=0;i<taskOrderList.length;i++){
			   	var task = taskOrderList[i];
			   	var woId = task.woId;
			   	var planUseCarAddress = task.planUseCarAddress;
			   	var planUseCarTime = task.planUseCarTime;
			   	if(!planUseCarAddress){planUseCarAddress=" ";}
			   	if(!planUseCarTime){planUseCarTime=" ";}
				content+="<li><a href='cardispatchWorkorder!enterCardispatchWorkorderAction.action?WOID="+woId+"' target='_blank'>"+woId+"</a>,"+planUseCarAddress+","+planUseCarTime+"</li>";
			}
		}
		
		content+="</ul></div></dd><dd style=\"display:none\" id='cGantt'>";
		
		
		content+="</dd></dl></div></div>";
		content+="</div><div class=\"dd_bottom\">&nbsp;&nbsp;<a class=\"dialog-locus\" target='_blank' href=\"loadCarStateMonitoringPageAction?carNumber="+encodeURI(encodeURI(carNumber))+"&beginTime="+$("#locusDate").val()+" 00:00:00&endTime="+$("#locusDate").val()+" 23:59:59&display=none\" >轨迹回放</a></div>";
	}
	currentInfoWindow.setContent(content);
	currentInfoWindow.setPosition(position);
	marker.openInfoWindow(currentInfoWindow);
	//右边信息框Table切换
	$(".dialog-tabmenu li").each(function(index){
		$(this).click(function(){
			$(".dialog-tabmenu li").removeClass("on");
	   		$(this).addClass("on");
	   		$(".dialog-tabcontent dd").hide();
	   		var curIndex = $(".dialog-tabmenu li").index($(this));
	   		$(this).parent().parent().find($(".dialog-tabcontent")).find("dd").eq(curIndex).show();
		})
	});
	createGanttContent("cGantt","carGanttContent","carDatepicker",carId,"car",null);
};
function showResourceMonthGantt(){
	if($(".carDatepicker").prev().val()){
		var carId = $(".carDatepicker").prev().val();
		showResourceMonthGanttByConditions(".carDatepicker",carId,"car");
	}
	if($(".ctDatepicker").prev().val()){
		var carId = $(".ctDatepicker").prev().val();
		showResourceMonthGanttByConditions(".ctDatepicker",carId,"car");
	}
}
//按条件获取月份任务
function showResourceMonthGanttByConditions(content,resourceId,resourceType){
	var year = $(content+" .ui-datepicker-year").html().replace("年","-");
	var month = $(content+" .ui-datepicker-month").html().replace("月","-");
	var taskDate = year+month;
	var endDate = "30";
	$(content+" .ui-datepicker-calendar tbody td a").each(function(){
		endDate = $(this).html();
	});
	$.ajax({
		url : "getResourceMonthTaskAction",
		data : {"taskDate":taskDate,"endDate":endDate,"resourceId":resourceId,"resourceType":resourceType},
		dataType : 'json',
		async:false,
		type : 'POST',
		success : function(result){
			if(result){
				$(content+" .ui-datepicker-calendar tbody td a").each(function(){
					var i = $(this).html();
					var ti = result[i];
					var hasTask = ti.hasTask;
					// 有无任务颜色显示不同，有任务为红色，无任务为绿色
					var cs = "bg_green";
					if(hasTask&&hasTask=='true'){
						cs = "bg_red";
						$(this).attr("class",cs);
					}
				});
			}
		}
	});
}
/*点击显示查询车辆轨迹*/
function showCarLocus(carNumber)
{
	$("#loading_div").css("display","block");
	if($("#replayDiv").css("display") != "block"){
		$(".gis_right_information").css("display","block");
		//$("#taskDiv").hide();
		$(".left_hide_grey").hide();
		$(".gis_right_information_display").css("right","300px");
	}
	carNumber = decodeURI(decodeURI(carNumber));
	$("#queryCarNumber").val(carNumber);
	getCarLocus();
	$("#loading_div").css("display","none");
	searchProviderOrgTree();
	
}

function autoComplete()
{
	$('#queryCarNumber').AutoComplete({
		'data':"cardispatchManage!getCarnumber",
		'ajaxParams':{'orgId':sysOrgId},
		'maxHeight': 120,
		'ajaxType':'post',
		'width': 'auto',
		'ajaxDataType': 'json',
		'emphasisHandler': function(keyword, data){
            var regex = RegExp("("+keyword.replace(/([.?*+^$[\]\\(){}|-])/g, "\\$1")+")", 'ig');
            data.label = data.label.replace(regex, "<span style='font-weight:bold;'>$1</span>");
        }
    }).AutoComplete('show');
}


/*清除查询结果*/
function cleanQueryResult(){
	if(currentInfoWindow){
		currentInfoWindow.close();
	}
	if(queryCarList){
		for(var i=0;i<queryCarList.length;i++){
			var marker = queryCarList[i];
			if(marker){
				marker.textOverlay_.setMap(null);
				marker.setMap(null);
			}
		}
	}
}
/*还原查询结果*/
function restoreQueryResult(){
	if(queryCarList){
		for(var i=0;i<queryCarList.length;i++){
			var marker = queryCarList[i];
			if(marker){
				marker.textOverlay_.setMap(map);
				marker.setMap(map);
			}
		}
	}
}
//多条件查询，列表显示
function showListViewByConditions(queryConditions){
	if(playCar!=null){
		return false;
	}
	$("#resultListTable").find($(".thead")).nextAll().remove();
	var thead = $("<tr><td colspan='9'><img src='images/page_loading.gif'/></td></tr>");
	thead.appendTo($("#resultListTable"));
	//var queryUrl = "cardispatchWorkorder_ajax!findCarListWithDuty.action";
	queryConditions._carId = "";
	/*$.ajax( {
		url : queryUrl,
		cache : false,
		data : queryConditions,
		async : true,
		type : "POST",
		dataType : 'json',
		success : function(result) {
			var queryResult = result;
			if(queryResult){
				showResultTable(queryResult);
			}
		}
	});*/
	//yuan.yw
	var queryUrl="cardispatchWorkorder!findCarListForMonitor.action";
	queryConditions.currentPage = 1;
	var param = queryConditions;
	pagingColumnByBackgroundJsp("pageContent","listView1",queryUrl,param);
}

/**张声js**/
$(document).ready(function(){
	$(".container-tab2 ul li:eq(0)").css("border-left","1px solid #ccc");
	$(".container-tab2 ul li").each(function(){
		$(this).click(function(){
			$(".container-tab2 ul li").removeClass("tab2-li-show");
			$(this).addClass("tab2-li-show");
			})
		})
	$(".container-tab2 ul li").each(function(index){
		$(this).click(function(){
				$(".container-main-table1-tab").hide();
				$(".container-main-table1-tab").eq(index).show();
			})
		})
})
$(document).ready(function(){
	$(".main-table2 tr th>input").each(function(){
		$(".main-table2 tr th>input").click(function(){
			if($(".main-table2 tr th>input").attr("checked")=="checked"){
				$(".main-table2 tr td>input").attr("checked","checked");
				}else{
					$(".main-table2 tr td>input").removeAttr("checked");
					}
			})
		})
	$("#tree>li>input").each(function(){
		$("#tree>li>input").click(function(){
			if($("#tree>li>input").attr("checked")=="checked"){
				$("#tree>li>ul>li>input").attr("checked","checked");
				}else{
					$("#tree>li>ul>li>input").removeAttr("checked");
					}
			})
		})
	})
$(function(){
	$("#treeDivButton").click(function(){
		$("#treeDiv").toggle("fast");
	})
	
	/*弹出高级搜索框*/
	$("#gaojisousuo").click(function(){
		$(".advanced-search").toggle("fast");
	})
	/*隐藏GIS右边信息框*/
	$(".close_gis_right").click(function(){
		$(".gis_right_information").toggle("fast");
		$("#replayDiv").toggle("fast");
		//清除车辆轨迹
		cleanMarkersAndPolyline();
		//还原查询结果
		//restoreQueryResult();
		$("#mapViewButton").click();
		//重新开始定时器
		autoRefresh = setInterval(function(){
			if($("#listView").css("display") == "block"){
				$("#listViewButton").click();
			}else{
				//清除车辆轨迹
				cleanTaskPolylineArray();
				cleanMarkersAndPolyline();
				$("#mapViewButton").click();
			}
		},60000);
		$("#hiddenCarNumber").val("");
		queryConditions._carNumber="";
	})
})
		
var sysOrgId = 0;
//生成服务商的组织架构树
function searchProviderOrgTree(){
	var orgId = "16";
	$.ajax({
		"url" : "cardispatchForeign_ajax!getLoginUserBiz.action" , 
		"type" : "post" , 
		"async" : true , 
		"success" : function ( data ) {
			data = eval( "(" + data + ")" );
			orgId = data.orgId;
			$("#bizunitNameText").val(data.name);
			$("#bizunitIdText").val(data.orgId);
			$("#carBizName").val(data.name);
			$("#carBizId").val(data.id);
			$("#gisBizName").val(data.name);
			$("#gisBizId").val(data.orgId);
			sysOrgId = data.orgId;
			if(orgId==null||orgId==""){
				orgId="16";
			}
			var values = {"orgId":orgId}
			var myUrl = "../organization/getProviderOrgTreeByOrgIdAction";
			$.post(myUrl,values,function(data){
				createOrgTreeOpenFirstNode(data,"treeDiv","tree1","a","searchOrgTreeClick");
				createOrgTreeOpenFirstNode(data,"treeDiv2","tree2","a","searchOrgTreeClick2");
				createOrgTreeOpenFirstNode(data,"treeDiv3","tree3","a","searchOrgTreeClick3");
			},"json");
			
		}    
	});
//	autoComplete();
}
//显示服务商的组织信息
function searchOrgTreeClick(dataStr,tableId){
	var data = eval( "(" + dataStr + ")" ) ;
	var orgId = data.orgId;
	$("#bizunitNameText").val(data.name);
	$("#bizunitIdText").val(data.orgId);
	$("#treeDiv").slideUp("fast");
}

//显示服务商的组织信息
function searchOrgTreeClick2(dataStr,tableId){
	var data = eval( "(" + dataStr + ")" ) ;
	var orgId = data.orgId;
	$("#carBizName").val(data.orgName);
	$("#carBizId").val(data.orgId);
	$("#treeDiv2").slideUp("fast");
}


//显示服务商的组织信息
function searchOrgTreeClick3(dataStr,tableId){
	var data = eval( "(" + dataStr + ")" ) ;
	var orgId = data.orgId;
	sysOrgId = orgId;
	$("#gisBizName").val(data.name);
	$("#gisBizId").val(data.orgId);
	$("#treeDiv3").slideUp("fast");
//	autoComplete();
}



