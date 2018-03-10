$(document).ready(
		function() {
		
	//切换区域
	initAreaCascade();
	initThresholdCascade();
	//检查是否存在这周计算的干扰矩阵
	isCalculateInterMartixThisWeek();
	
	//加载MR数据
	$("#showMrData").click(function() {
		$("#isDateRightTip").text("");
		initFormPage('interferMartixAddMrForm');
		loadMrData();
	});
	//加载HO数据
	$("#showHoData").click(function() {
		$("#isHoDateRightTip").text("");
		initFormPage('interferMartixAddMrForm');
		loadHoData();
	});
	// 扫频文件框选择
	$("#isUseSf").change(function() {
		if ($("#isUseSf").prop("checked")) {
			initFormPage('interferMartixAddMrForm');
			loadSfFiles();
			$(".forfilecheck").removeAttr("disabled");
			$("#isFreqAdj").removeAttr("disabled");
		} else {
			$(".forfilecheck").attr("disabled", "disabled");
			$("#isFreqAdj").attr("disabled", "disabled");
		}
	});
	//新增计算mr干扰矩阵
	$("#calculateInterMartix").click(function() {
		if(!providerTaskName()){
			$('#calculateInterMartix').removeAttr("disabled"); 
		}else{
			addMrInterMartix();
		}		
	});
});

/**
* 新增计算MR干扰矩阵
*/
function addMrInterMartix() {
	var isUseSf = $("#isUseSf").prop("checked");
	var filenames = getFileName();
	if (isUseSf && (typeof (filenames) == "undefined" || filenames.length == 0)) {
		alert("您还没有选择扫频文件");
		return;
	}
	$("#sffiles").val(filenames);
	showOperTips("loadingDataDiv", "loadContentId", "正在提交干扰矩阵任计算任务");
	if(!checkTaskInfoSubmit()){
		hideOperTips("loadingDataDiv");
		$('#calculateInterMartix').removeAttr("disabled"); 
		return;
	}
	$("#interferMartixAddMrForm").ajaxSubmit({
		url : 'addNewLteInterMartixForAjaxAction',
		dataType : 'text',
		success : function(raw) {
			var data = eval("("+raw+")");
			var isDateRight = data['isDateRight'];
			var isMrExist = data['isMrExist'];
			var isHoExist = data['isHoExist'];
			var dataType = data['dataType'];
			//var isCalculating = data['isCalculating'];
			if(dataType=="MR"){
				if(isDateRight && isMrExist /*&& !isCalculating*/) {
					//提示计算任务已经提交到系统
					alert("计算任务已经提交到系统");
					//页面跳转到干扰矩阵显示页
					var cityId=$("#cityId2").val();
					location.href = 'initNewLteInterferMartixManageAction?cityId='+cityId;
				} else {
					/*	if(isCalculating) {
					$("span#isCalculateTip").html("存在正在计算或者等待中的干扰矩阵任务..请稍后");
					}*/
					if(!isDateRight) {
						$("span#isDateRightTip").html("日期范围不符合要求（最迟不会超过本周一的0点，最早不会早于上周的周一0点）");
						$('#calculateInterMartix').removeAttr("disabled");
					} else {
						if(!isMrExist) {
							$("span#isDateRightTip").html("该日期范围不存在MR数据记录");
							$('#calculateInterMartix').removeAttr("disabled"); 
						}
					}
				}
			}else if(dataType=="HO"){
				if(isHoExist) {
					//提示计算任务已经提交到系统
					alert("计算任务已经提交到系统");
					//页面跳转到干扰矩阵显示页
					var cityId=$("#cityId2").val();
					location.href = 'initNewLteInterferMartixManageAction?cityId='+cityId;
				} else {
					$("span#isDateRightTip").html("该日期范围不存在HO数据记录");
					$('#calculateInterMartix').removeAttr("disabled"); 
				}
			}else {
				if(isMrExist||isHoExist){
					//提示计算任务已经提交到系统
					alert("计算任务已经提交到系统");
					//页面跳转到干扰矩阵显示页
					var cityId=$("#cityId2").val();
					location.href = 'initNewLteInterferMartixManageAction?cityId='+cityId;
				}else{
					$("span#isDateRightTip").html("该日期范围内MR和HO数据记录都不存在");
					$('#calculateInterMartix').removeAttr("disabled"); 
				}
			}
		},
		complete : function() {
			hideOperTips("loadingDataDiv");
		}
	});
	$("#sffiles").val("");
}
/**
 * 检查任务信息
 * @returns {Boolean}
 */
function checkTaskInfoSubmit() {
	var flagDate = true;
	var flagTh = true;
	if($("#SAMEFREQCELLCOEFWEIGHT").val()==''||$("#SWITCHRATIOWEIGHT").val()==''){
		alert("阈值必须为数字");
		return false;
	}
	$("#SAMEFREQCELLCOEFWEIGHT").val(Number($("#SAMEFREQCELLCOEFWEIGHT").val()).toFixed(2));
	$("#SWITCHRATIOWEIGHT").val(Number($("#SWITCHRATIOWEIGHT").val()).toFixed(2));
	var th1 = $("#SAMEFREQCELLCOEFWEIGHT").val();
	var th2 = $("#SWITCHRATIOWEIGHT").val();
	var startTime = $.trim($("#beginTime").val());
	var endTime = $.trim($("#latestAllowedTime").val());

	$("span#isDateRightTip").html("");
	
	//检查时间
	if(startTime=="" || endTime==""){
		hideOperTips("loadingDataDiv");
		$('#calculateInterMartix').removeAttr("disabled"); 
		alert("时间不能为空！");
		flagDate = false;
	}
	console.log(th1+"_"+th2+"_"+(th1==''||th2==''));
	//检查阈值
	if(!(isNumeric(th1)&&isNumeric(th2))||(th1<0||th1>1||th2<0||th2>1)){
		alert("请输入0到1之间的数");
		flagTh = false;
	}else if ((Number(th1)+Number(th2))!=1){
		alert("两个阈值相加必须为1");
		flagTh = false;
	}
/*
	if (endTime == "" || startTime == "") {
		$("span#dateErrorText").html("请填写需要使用的测量数据的时间！");
		$("span#dateError").html("※");
		flagDate = false;
	} else if (exDateRange(endTime, startTime) > maxDateInterval) {
		// 验证测试日期是否大于十天
		$("span#dateErrorText").html("（时间跨度请不要超过"+maxDateInterval+"天！）");
		$("span#dateError").html("※");
		flagDate = false;
	}*/

	if (flagDate && flagTh) {
		result = true;
	} else {
		result = false;
	}
	return result;
}

/**
* 检查是否存在这周计算的干扰矩阵
*/
function isCalculateInterMartixThisWeek() {
	$("span#isCalculateTip").html("");
	showOperTips("loadingDataDiv", "loadContentId", "正在检查这周是否计算干扰矩阵");
	
	$("#interferMartixAddMrForm").ajaxSubmit({
		url : 'isExistedNewLteInterMartixThisWeekAction',
		dataType : 'text',
		success : function(raw) {
			var data = eval("("+raw+")");
			var flag = data['flag'];
			var desc = data['desc'];
			if(flag) {
				$("span#isCalculateTip").html(desc);
			} else {
			}
		},
		complete : function() {
			hideOperTips("loadingDataDiv");
		}
	});
}

/**
* 加载对应的MR数据
*/
function loadMrData() {

	showOperTips("loadingDataDiv", "loadContentId", "正在加载MR数据");
	if($("#beginTime").val()=="" || $("#latestAllowedTime").val()==""){
		hideOperTips("loadingDataDiv");
		alert("日期不能为空!");
		return;
	};
	$("#interferMartixAddMrForm").ajaxSubmit({
		url : 'queryMrDataByPageForAjaxAction',
		dataType : 'text',
		success : function(raw) {
			//showMrDataList(raw);
			showDataList(raw,"MR");
		},
		complete : function() {
			hideOperTips("loadingDataDiv");
		}
	});
}

/**
* 分页显示mr数据
*/
function showMrDataList(raw) {
	
 	var data = eval("("+raw+")");
	var table = $("#mrDataTable");
	var city=$("#cityId2").find("option:selected").text();
	//清空干扰矩阵详情列表
	$("#mrDataTable tr:gt(0)").remove();
	
	if(data == null || data == undefined){
		return;
	}
	
	var list=data['data'];
	var html="";
	var tr="";
	var one="";
	
	for(var i=0;i<list.length;i++){
		one = list[i];
		tr = "<tr>";
		tr += "<td>"+getValidValue(city,'')+"</td>";
		tr += "<td>"+getValidValue(one['MEA_TIME'],'')+"</td>";
		tr += "<td>"+getValidValue(one['FACTORY'],'')+"</td>";
		tr += "<td>"+getValidValue(one['RECORD_COUNT'],'')+"</td>";
		tr += "<td>"+getValidValue(one['CREATE_TIME'],'')+"</td>";
		tr += "</tr>";
		html += tr;
	}
	table.append(html);
	
	//设置隐藏的page信息
	setFormPageInfo("interferMartixAddMrForm",data['page']);
	
	//设置分页面板
	setPageView(data['page'],"mrDataPageDiv");
}
/**
* 分页显示mr或ho数据
*/
function showDataList(raw, dataType) {

	var data = eval("(" + raw + ")");
	var table = $("#mrDataTable");
	var city = $("#cityId2").find("option:selected").text();
	// 清空干扰矩阵详情列表
	$("#mrDataTable tr").remove();

	if (data == null || data == undefined) {
		return;
	}

	var list = data['data'];
	var html = "";
	var tr = "";
	var one = "";
	if (dataType == "SF") {
		html = "<thead><tr><th style='width: 8%'>操作</th>"
			+ "<th style='width: 8%'>类型</th>"
			+ "<th style='width: 8%'>文件名</th>"
			+ "<th style='width: 8%'>测量时间</th>"
			+ "<th style='width: 8%'>测量数据量</th></tr></thead>";
		for (var i = 0; i < list.length; i++) {
			one = list[i];
			tr = "<tr>";
			tr += "<td><input class='forfilecheck' type='checkbox' /></td>";
			tr += "<td class='filename'>" + getValidValue(one['FILE_NAME'], '') + "</td>";
			tr += "<td>" + getValidValue(dataType, '') + "</td>";
			tr += "<td>" + getValidValue(one['MEA_TIME'], '') + "</td>";
			tr += "<td>" + getValidValue(one['RECORD_COUNT'], '') + "</td>";
			tr += "</tr>";
			html += tr;
		}
	} else {
		html = "<thead><tr><th style='width: 8%'>地市</th>"
				+ "<th style='width: 8%'>测量时间</th>"
				+ "<th style='width: 8%'>类型</th>"
				+ "<th style='width: 8%'>厂家</th>"
				+ "<th style='width: 8%'>测量数据量</th>"
				+ "<th style='width: 8%'>进入系统时间</th></tr></thead>";
		for (var i = 0; i < list.length; i++) {
			one = list[i];
			tr = "<tr>";
			tr += "<td>" + getValidValue(city, '') + "</td>";
			tr += "<td>" + getValidValue(one['MEA_TIME'], '') + "</td>";
			tr += "<td>" + getValidValue(dataType, '') + "</td>";
			tr += "<td>" + getValidValue(one['FACTORY'], '') + "</td>";
			tr += "<td>" + getValidValue(one['RECORD_COUNT'], '') + "</td>";
			tr += "<td>" + getValidValue(one['CREATE_TIME'], '') + "</td>";
			tr += "</tr>";
			html += tr;
		}
	}

	table.append(html);

	// 设置隐藏的page信息
	setFormPageInfo("interferMartixAddMrForm", data['page']);

	// 设置分页面板
	setPageView(data['page'], "mrDataPageDiv");
}
/**
* 加载对应的Ho数据
*/
function loadHoData() {

	showOperTips("loadingDataDiv", "loadContentId", "正在加载HO数据");
	if($("#beginTime").val()=="" || $("#latestAllowedTime").val()==""){
		hideOperTips("loadingDataDiv");
		alert("日期不能为空!");
		return;
	};
	$("#interferMartixAddMrForm").ajaxSubmit({
		url : 'queryHoDataByPageForAjaxAction',
		dataType : 'text',
		success : function(raw) {
			//showMrDataList(raw);
			showDataList(raw,"HO");
		},
		complete : function() {
			hideOperTips("loadingDataDiv");
		}
	});
}
/**
* 加载对应的Ho数据
*/
function loadSfFiles() {
	showOperTips("loadingDataDiv", "loadContentId", "正在加载SF数据");
	if($("#beginTime").val()=="" || $("#latestAllowedTime").val()==""){
		hideOperTips("loadingDataDiv");
		alert("日期不能为空!");
		return;
	};
	$("#interferMartixAddMrForm").ajaxSubmit({
		url : 'querySfFilesByPageForAjaxAction',
		dataType : 'text',
		success : function(raw) {
			showDataList(raw,"SF");
		},
		complete : function() {
			hideOperTips("loadingDataDiv");
		}
	});
}
//设置隐藏的page信息
function setFormPageInfo(formId, page) {
	if(formId==null || formId==undefined || page==null || page==undefined){
		return;
	}
	
	var form=$("#"+formId);
	if(!form){
		return;
	}
	
	form.find("#hiddenPageSize").val(page.pageSize);
	form.find("#hiddenCurrentPage").val(new Number(page.currentPage));
	form.find("#hiddenTotalPageCnt").val(page.totalPageCnt);
	form.find("#hiddenTotalCnt").val(page.totalCnt);
}

/**
* 设置分页面板
* @param page
* 分页信息
* @param divId
* 分页面板id
*/
function setPageView(page,divId){
	if(page==null || page==undefined  ){
		return;
	}
	
	var div=$("#"+divId);
	if(!div){
		return;
	}

	var pageSize = page['pageSize'] ? page['pageSize'] : 0;
	var currentPage = page['currentPage'] ? page['currentPage'] : 1;
	var totalPageCnt = page['totalPageCnt'] ? page['totalPageCnt'] : 0;
	var totalCnt = page['totalCnt'] ? page['totalCnt'] : 0;
	console.log(totalCnt+"_"+totalPageCnt);
	//设置到面板上
	$(div).find("#emTotalCnt").html(totalCnt);
	$(div).find("#showCurrentPage").val(currentPage);
	$(div).find("#emTotalPageCnt").html(totalPageCnt);
}

//初始化form下的page信息
function initFormPage(formId){
	var form=$("#"+formId);
	if(!form){
		return;
	}
	form.find("#hiddenPageSize").val(25);
	form.find("#hiddenCurrentPage").val(1);
	form.find("#hiddenTotalPageCnt").val(-1);
	form.find("#hiddenTotalCnt").val(-1);
}

/**
* 分页跳转的响应
* @param dir
* @param action（方法名）
* @param formId
* @param divId
*/
function showListViewByPage(dir,action,formId,divId) {
	
	var form=$("#"+formId);
	var div=$("#"+divId);
	//alert(form.find("#hiddenPageSize").val());
	var pageSize =new Number(form.find("#hiddenPageSize").val());
	var currentPage = new Number(form.find("#hiddenCurrentPage").val());
	var totalPageCnt =new Number(form.find("#hiddenTotalPageCnt").val());
	var totalCnt = new Number(form.find("#hiddenTotalCnt").val());

	//console.log("pagesize="+pageSize+",currentPage="+currentPage+",totalPageCnt="+totalPageCnt+",totalCnt="+totalCnt);
	if (dir === "first") {
		if (currentPage <= 1) {
			return;
		} else {
			$(form).find("#hiddenCurrentPage").val("1");
		}
	} else if (dir === "last") {
		if (currentPage >= totalPageCnt) {
			return;
		} else {
			$(form).find("#hiddenCurrentPage").val(totalPageCnt);
		}
	} else if (dir === "back") {
		if (currentPage <= 1) {
			return;
		} else {
			$(form).find("#hiddenCurrentPage").val(currentPage - 1);
		}
	} else if (dir === "next") {
		if (currentPage >= totalPageCnt) {
			return;
		} else {
			$(form).find("#hiddenCurrentPage").val(currentPage + 1);
		}
	} else if (dir === "num") {
		var userinput = $(div).find("#showCurrentPage").val();
		if (isNaN(userinput)) {
			alert("请输入数字！")
			return;
		}
		if (userinput > totalPageCnt || userinput < 1) {
			alert("输入页面范围不在范围内！");
			return;
		}
		$(form).find("#hiddenCurrentPage").val(userinput);
	}else{
		return;
	}
	//获取资源
	if(typeof action =="function"){
		action();
	}
}

//区域切换触发
function initAreaCascade() {
	
	$("#provinceId2").change(function() {
		getSubAreas("provinceId2", "cityId2", "市");
	});

	$("#cityId2").change(function() {	
		//检查是否存在这周计算的干扰矩阵
		isCalculateInterMartixThisWeek();
		/*getSubAreas("cityId2", "areaId2", "区/县",function(){
			$("#areaId2").append("<option selected='true' value=''>全部</option>");
		});*/
	});
}
//阈值联动
function initThresholdCascade() {
	$("#SAMEFREQCELLCOEFWEIGHT").blur(function() {
		$("#SAMEFREQCELLCOEFWEIGHT").val(Number($("#SAMEFREQCELLCOEFWEIGHT").val()).toFixed(2));
		$("#SWITCHRATIOWEIGHT").val((1 - Number($("#SAMEFREQCELLCOEFWEIGHT").val())).toFixed(2));
		if($("#SAMEFREQCELLCOEFWEIGHT").val()=="NaN"||$("#SWITCHRATIOWEIGHT").val()=="NaN"){
			$("#SWITCHRATIOWEIGHT").val("");
			$("#SAMEFREQCELLCOEFWEIGHT").val("");
		}
	});
	$("#SWITCHRATIOWEIGHT").blur(function() {
		$("#SWITCHRATIOWEIGHT").val(Number($("#SWITCHRATIOWEIGHT").val()).toFixed(2));
		$("#SAMEFREQCELLCOEFWEIGHT").val((1 - Number($("#SWITCHRATIOWEIGHT").val())).toFixed(2));
		if($("#SAMEFREQCELLCOEFWEIGHT").val()=="NaN"||$("#SWITCHRATIOWEIGHT").val()=="NaN"){
			$("#SWITCHRATIOWEIGHT").val("");
			$("#SAMEFREQCELLCOEFWEIGHT").val("");
		}
	});
}
//检测任务名
function providerTaskName(){
	var errorStr = "";
	var flagStr = "";
	$("#taskName_error").html(errorStr);
	$("#taskName_flag").html(flagStr);
	if(!checkTaskName()){
		return false;
	}
	var taskName = $.trim($("#taskName").val());
	var cityId = $("#cityId2").val();
	var flag = false;
	$.ajax({
			url : "checkTaskNameByCityIdNewAjaxAction",
			data : {
				"attachParams.taskName":taskName,
				"attachParams.cityId":cityId
				},
			async:false,
			type : 'POST',
			success : function(data) {
				if(data=="success"){
					flagStr = "该任务名可用";
					animateInAndOut("operInfo", 500, 500, 1000, "operTip",flagStr);
					flag =  true;
				}else{
					errorStr = "该任务名不可用";
					animateInAndOut("operInfo", 500, 500, 1000, "operTip",errorStr);
					flag =  false;
				}
			}
		});
		
	$("#taskName_error").html(errorStr);
	$("#taskName_flag").html(flagStr);
		return flag;
}
/**
 * 检查任务名
 * @returns {Boolean}
 */
function checkTaskName() {
	var errorStr = "";
	var flagStr = "";
	$("#taskName_error").html(errorStr);
	$("#taskName_flag").html(flagStr);
	var result = true;
	var n=0;
	var taskName = $.trim($("#taskName").val());
	for ( var i = 0; i < taskName.length; i++) { // 应用for循环语句,获取表单提交用户名字符串的长度
		var leg = taskName.charCodeAt(i); // 获取字符的ASCII码值
		if (leg > 255) { // 判断如果长度大于255
			n += 2; // 则表示是汉字为2个字节
		} else {
			n += 1; // 否则表示是英文字符,为1个字节
		}
	}
	if(n == 0){
		errorStr = "请输入任务名....";
		animateInAndOut("operInfo", 500, 500, 1000, "operTip",errorStr);
		$("#taskName_error").html(errorStr);
		$("#taskName_flag").html(flagStr);
		result = false;
	}else if(ifHasSpecChar2(taskName)){
		errorStr = "包含有以下特殊字符:~'!@#$%^&*+=";
		animateInAndOut("operInfo", 500, 500, 1000, "operTip",errorStr);
		$("#taskName_error").html(errorStr);
		$("#taskName_flag").html(flagStr);
		result = false;
	}else if(n>25){
		errorStr = "不超过25个字符....";
		animateInAndOut("operInfo", 500, 500, 1000, "operTip",errorStr);
		$("#taskName_error").html(errorStr);
		$("#taskName_flag").html(flagStr);
		result = false;
	}
	return result;
}
/**
 * 过滤检查返回布尔值
 */
function checkProviderTaskName(){
	providerTaskName();
}
/**
 * 获取选中的扫频文件
 * @returns {String}
 */
function getFileName() {
	var filenames = "";
	if ($("#isUseSf").prop("checked")) {
		var files = $(".forfilecheck");
		for (var i = 0; i < files.length; i++) {
			if (files.eq(i).prop("checked")) {
				filenames += files.eq(i).parent().next().text() + ",";
			}
		}
		filenames = filenames.substr(0, filenames.length - 1) + "";
	}
	console.log(filenames);
	return filenames;
}