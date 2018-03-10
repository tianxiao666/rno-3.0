var $ = jQuery.noConflict();
var gisCellDisplayLib;// 负责显示小区的对象

var map;
var ge;
var cellDetails = new Array();// 小区详情

//
var minZoom = 17;// 只有大于 该 缩放级别，才真正
var randomShowCnt = 150;// 在不需要全部显示的时候，最大随机显示的个数
var preZoom = 15;// 当前的缩放级别
var minResponseInterval = 1000;// 对事件做响应的最小的间隔
var lastOperTime = 0;// 最后一次操作的时间

var mapGridLib //用于将地图分成网格加载小区的对象

var currentloadtoken = "";// 加载的token，每次分页加载都要比对。

var currentAreaCenter;// 当前所选区域的中心点

var totalCellCnt = 0;// 小区总数

$(document).ready(function() {
	// 隐藏详情面板
	hideDetailDiv();
	// 初始化地图
	initMap();
	$("#loadgiscell").click(function(){
		 	/******* peng.jm 2014-9-29 网格形式加载小区  start ********/	
		 	//清除缓存数据
		 	clearAll();
		 	//初始化网格状态
			mapGridLib.initMapGrids();
			//设置地图加载小区状态为true
			mapGridLib.setIsLoading(true);
			//设置当前屏幕经纬度范围
			var winMinLng = map.getBounds().getSouthWest().lng;
			var winMinLat = map.getBounds().getSouthWest().lat;
			var winMaxLng = map.getBounds().getNorthEast().lng;
			var winMaxLat = map.getBounds().getNorthEast().lat;
			mapGridLib.setWinLngLatRange(winMinLng,winMinLat,winMaxLng,winMaxLat);
			//设置当前加载标识
			currentloadtoken = getLoadToken();
			mapGridLib.setCurrentloadtoken(currentloadtoken);
			//加载小区
			var t = new Date().getTime();
			//t-minResponseInterval保证不延迟加载
			mapGridLib.loadGisCell(t-minResponseInterval, currentloadtoken, minResponseInterval);
		 	/******* peng.jm 2014-9-29 网格形式加载小区  end ********/	
	});
});

/**
 * 通过session存储地图ID
 */
function storageMapId(mapid){
	$(".loading_cover").css("display", "block");
	$.ajax({
		url : 'storageMapIdBySessForAjaxAction',
		data : {
			
		},
		dataType : 'json',
		type : 'post',
		success : function(data) {
			glomapid=data;
			try{
			window.location.href=window.location.href;
			}catch(err){
			
			}
		},
		error : function(xhr, textstatus, e) {

		},
		complete : function() {
			$(".loading_cover").css("display", "none");
		}
	});
}
/**
 * 初始化地图
 */
function initMap() {
	document.getElementById('map_canvas').style.position = 'absolute';
	document.getElementById('map_canvas').style.zIndex = '0';
	gisCellDisplayLib = new GisCellDisplayLib(map, minZoom, randomShowCnt,
			null, {
				'clickFunction' : handleClick,
				'mouseoverFunction' : null,
				'mouseoutFunction' : null,
				'rightclickFunction':null
			},"","", preZoom);
	map = gisCellDisplayLib.initMap("map_canvas", 0, 0, {
		"tilesloaded" : function() {
			bindEvent();
			// 加载数据
			currentloadtoken = getLoadToken();
		},
		'movestart' : handleMovestartAndZoomstart,
		'zoomstart' : handleMovestartAndZoomstart,
		'moveend' : handleMoveendAndZoomend,
		'zoomend' : handleMoveendAndZoomend 
	});

	/******* peng.jm 2014-9-29 网格形式加载小区  start ********/	
	//以网格形式在地图加载小区
	mapGridLib = new MapGridLib(gisCellDisplayLib,"conditionForm","loadingCellTip"); 
	//以城市为单位创建区域网格
	var cityName = $("#cityId").find("option:selected").text().trim();
	mapGridLib.createMapGrids(cityName);
	/******* peng.jm 2014-9-29 网格形式加载小区  end ********/
}

// 隐藏详情面板
function hideDetailDiv() {
	$("a.siwtch").hide();
	$(".switch_hidden").show();
	$(".resource_list_icon").animate({
		right : '0px'
	}, 'fast');
	$(".resource_list_box").hide("fast");
}

function bindEvent() {
	// 区域联动事件
	$("#provinceId").change(function() {
		getSubAreas("provinceId", "cityId", "市");
	});
	$("#cityId").change(function() {
		getSubAreas("cityId", "queryCellAreaId", "区/县");
	});

	// 区域选择改变
	$("#queryCellAreaId").change(function() {
		// 清除当前区域的数据
		clearAll();

		// 获取经纬度
		var optionData
		var $queryCellAreaId = $("#queryCellAreaId");
		var exist = $queryCellAreaId.val();
		// 该判断在值为null、undefined、数字零、false时，都会获得true
		if (!exist) {
			// 没有区县信息则获取小区的经纬度
			// 从jquery对象转化到DOM对象才有dataset，不建议用jquey.data()来操作DOMStringMap
			optionData = $("#cityId").find("option:selected")[0].dataset;
		} else {
			// 获取区县经纬度
			optionData = $queryCellAreaId.find("option:selected")[0].dataset;
		}
		
		// 地图中心点
		//map.panTo(new BMap.Point(lls[0], lls[1]));// 这个偏移可以不理会
		gisCellDisplayLib.panTo(optionData.lon, optionData.lat);
		
		// 重置查询表单的数据
		resetQueryCondition();
		// 重新获取新区域的数据
		currentloadtoken = getLoadToken();

		/******* peng.jm 2014-9-29 网格形式加载小区  start ********/	
		//以城市为单位创建区域网格
		var cityName = $("#cityId").find("option:selected").text().trim();
		mapGridLib.createMapGrids(cityName);
		/******* peng.jm 2014-9-29 网格形式加载小区  end ********/	
	});
	// 绑定完成，触发一次区域改变事件
	$("#queryCellAreaId").trigger("change");
}

// 清除全部的数据
function clearAll() {
	try {
		gisCellDisplayLib.clearOverlays();
	} catch (e) {

	}
	cellDetails.splice(0, cellDetails.length);
	gisCellDisplayLib.clearData();
}

function resetQueryCondition() {
	$("#hiddenCurrentPage").val(1);
	$("#hiddenTotalPageCnt").val(0);
	$("#hiddenTotalCnt").val(0);
	$("#hiddenForcedStartIndex").val(-1);
	$("#hiddenPageSize").val(100);

}

/**
 * 获取gis小区
 */
function getGisCell(loadToken) {
	$("#conditionForm").ajaxSubmit({
				url : 'getGisCellByPageForAjaxAction',
				dataType : 'json',
				success : function(data) {
					if (loadToken != currentloadtoken) {
						// 新的加载开始了，这些旧的数据要丢弃
						// console.log("丢弃此次的加载结果。");
						return;
					}
					// console.log(data);
					var obj = data;
					try {
						gisCellDisplayLib.showGisCell(obj['gisCells']);
						var page = obj['page'];
						if (page) {
							var pageSize = page['pageSize'] ? page['pageSize']
									: 0;
							$("#hiddenPageSize").val(pageSize);

							var currentPage = new Number(
									page['currentPage'] ? page['currentPage']
											: 1);
							currentPage++;// 向下一页
							$("#hiddenCurrentPage").val(currentPage);

							var totalPageCnt = new Number(
									page['totalPageCnt'] ? page['totalPageCnt']
											: 0);
							$("#hiddenTotalPageCnt").val(totalPageCnt);

							var totalCnt = page['totalCnt'] ? page['totalCnt']
									: 0;
							totalCellCnt = totalCnt;
							$("#hiddenTotalCnt").val(totalCnt);

							if (totalPageCnt >= currentPage) {
								// console.log("继续获取下一页小区");
								getGisCell(loadToken);
							}
						}
						// 如果没有获取完成，则继续获取
					} catch (err) {
						// console.log("返回的数据有问题:" + err);
					}
				},
				error : function(xmh, textstatus, e) {
//					alert("出错啦！" + textstatus);
				},
				complete : function() {
					$(".loading_cover").css("display", "none");
				}
			});
}
/**
 * 处理polygon的点击事件
 * 
 * @param {}
 *            polygon
 */
function handleClick(polygon, cell) {
	if(!cell) {
		var cmk = gisCellDisplayLib.getComposeMarkerByShape(polygon);
		cell = cmk.getCell();
	}
	showCellDetail(cell);
}

/**
 * 显示所关联的小区的详情
 * 
 * @param {}
 *            polygon
 */
function showCellDetail(label) {
	// alert("关联的小区为：" + polygon._data.getCell());

	// 看本地缓存有没有
	var cell = null;
	var find = false;
	for ( var i = 0; i < cellDetails.length; i++) {
		cell = cellDetails[i];
		if (cell['label'] === label) {
			// console.log("从缓存找到详情。")
			find = true;
			break;
		}
	}

	if (find) {
		displayCellDetail(cell);
		return;
	}

	$(".loading_cover").css("display", "block");
	$.ajax({
		url : 'getCellDetailForAjaxAction',
		data : {
			'label' : label
		},
		dataType : 'json',
		type : 'post',
		success : function(data) {
			var c = data;
			if (c) {
				cellDetails.push(c);
				displayCellDetail(c);
			}
		},
		error : function(xhr, textstatus, e) {

		},
		complete : function() {
			$(".loading_cover").css("display", "none");
		}
	});
}

/**
 * 显示小区详情
 * 
 * @param {}
 *            cell
 */
function displayCellDetail(cell) {

	$(".switch_hidden").trigger("click");

	$("#showCellLabelId").html(getValidValue(cell.label));
	$("#showCellNameId").html(getValidValue(cell.name));
	$("#showCellLacId").html(getValidValue(cell.lac));
	$("#showCellCiId").html(getValidValue(cell.ci));
	$("#showCellBcchId").html(getValidValue(cell.bcch));

	$("#showCellTchId").html(getValidValue(cell.tch));
	$("#showCellBsicId").html(getValidValue(cell.bsic));
	$("#showCellAzimuthId").html(getValidValue(cell.bearing));

	$("#showCellDownId").html(getValidValue(cell.downtilt));
	$("#showCellBtsTypeId").html(getValidValue(cell.btstype));
	$("#showCellAntHeightId").html(getValidValue(cell.antHeigh));
	$("#showCellAntTypeId").html(getValidValue(cell.antType));

	$("#showCellLngId").html(getValidValue(cell.longitude));
	$("#showCellLatId").html(getValidValue(cell.latitude));

	$("#showCellCoverareaId").html(getValidValue(cell.coverarea));
	$("#showCellFreqSectionId").html(getValidValue(cell.gsmfrequencesection));

}
/***************** 地图小区加载改造 start ********************/
function handleMoveendAndZoomend(e, lastOperTime) {
	var winMinLng = map.getBounds().getSouthWest().lng;
	var winMinLat = map.getBounds().getSouthWest().lat;
	var winMaxLng = map.getBounds().getNorthEast().lng;
	var winMaxLat = map.getBounds().getNorthEast().lat;
	//设置当前屏幕经纬度范围
	mapGridLib.setWinLngLatRange(winMinLng,winMinLat,winMaxLng,winMaxLat);
	//每一次移动，缩放结束的独立标识
	currentloadtoken = getLoadToken();
	mapGridLib.setCurrentloadtoken(currentloadtoken);
	mapGridLib.loadGisCell(lastOperTime, currentloadtoken, minResponseInterval);
}
function handleMovestartAndZoomstart(e, lastOperTime) {
	//每一次移动，缩放的独立标识
	currentloadtoken = getLoadToken();
	mapGridLib.setCurrentloadtoken(currentloadtoken);
}
/***************** 地图小区加载改造 end ********************/