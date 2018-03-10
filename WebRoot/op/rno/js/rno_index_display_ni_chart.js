function LimitQueue(maxSize) {
	this.maxSize = (maxSize != null && maxSize != undefined
			&& typeof maxSize == 'number' && maxSize > 0) ? maxSize : 10;
	this.values = new Array();
	this.keys = new Array();
}
LimitQueue.prototype.put = function(key, value) {
	if (key == null || key == undefined) {
		return;
	}
	if (this.keys.length > this.maxSize) {
		this.removeFirst();
	}
	this.keys.push(key);
	this.values.push(value);
}

LimitQueue.prototype.removeFirst = function() {
	this.keys = this.keys.shift();
	this.values = this.values.shift();
}

LimitQueue.prototype.remove = function(key) {
	for ( var i = 0; i < this.keys.length; i++) {
		if (key == this.keys[i]) {
			// 删除该key以及value
			this.keys.splice(i, 1);
			this.values.splice(i, 1);
		}
	}
}
LimitQueue.prototype.get = function(key) {
	for ( var i = 0; i < this.keys.length; i++) {
		if (key == this.keys[i]) {
			return this.values[i];
		}
	}
	return null;
}

var ncsDateToTime = new Object();// key为date
// value为一个array，array里的元素有2个key：ncsId,time
var ncsIdToDetail = new Object();// key为ncsId，value为ncs对象
var ncsDates = new Array();// 日期
var selectCell = "";
var selectBsc = "";

var charts = null;
var currentCellNcsData = null;// 当前小区对应的某个ncs的信息，
var cellFreqInterferQueue = new LimitQueue(10);// key为ncsId+"_"+cell，value为该ncs下的该小区的频点的干扰情况

var Rxlev = [ 'CELL_NAME', 'CHANNEL_GROUP_NUM', 'RXLEVUL0', 'RXLEVUL1',
		'RXLEVUL2', 'RXLEVUL3', 'RXLEVUL4', 'RXLEVUL5', 'RXLEVUL6', 'RXLEVUL7',
		'RXLEVUL8', 'RXLEVUL9', 'RXLEVUL10', 'RXLEVUL11', 'RXLEVUL12',
		'RXLEVUL13', 'RXLEVUL14', 'RXLEVUL15', 'RXLEVUL16', 'RXLEVUL17',
		'RXLEVUL18', 'RXLEVUL19', 'RXLEVUL20', 'RXLEVUL21', 'RXLEVUL22',
		'RXLEVUL23', 'RXLEVUL24', 'RXLEVUL25', 'RXLEVUL26', 'RXLEVUL27',
		'RXLEVUL28', 'RXLEVUL29', 'RXLEVUL30', 'RXLEVUL31', 'RXLEVUL32',
		'RXLEVUL33', 'RXLEVUL34', 'RXLEVUL35', 'RXLEVUL36', 'RXLEVUL37',
		'RXLEVUL38', 'RXLEVUL39', 'RXLEVUL40', 'RXLEVUL41', 'RXLEVUL42',
		'RXLEVUL43', 'RXLEVUL44', 'RXLEVUL45', 'RXLEVUL46', 'RXLEVUL47',
		'RXLEVUL48', 'RXLEVUL49', 'RXLEVUL50', 'RXLEVUL51', 'RXLEVUL52',
		'RXLEVUL53', 'RXLEVUL54', 'RXLEVUL55', 'RXLEVUL56', 'RXLEVUL57',
		'RXLEVUL58', 'RXLEVUL59', 'RXLEVUL60', 'RXLEVUL61', 'RXLEVUL62',
		'RXLEVUL63', 'RXLEVDL0', 'RXLEVDL1', 'RXLEVDL2', 'RXLEVDL3',
		'RXLEVDL4', 'RXLEVDL5', 'RXLEVDL6', 'RXLEVDL7', 'RXLEVDL8', 'RXLEVDL9',
		'RXLEVDL10', 'RXLEVDL11', 'RXLEVDL12', 'RXLEVDL13', 'RXLEVDL14',
		'RXLEVDL15', 'RXLEVDL16', 'RXLEVDL17', 'RXLEVDL18', 'RXLEVDL19',
		'RXLEVDL20', 'RXLEVDL21', 'RXLEVDL22', 'RXLEVDL23', 'RXLEVDL24',
		'RXLEVDL25', 'RXLEVDL26', 'RXLEVDL27', 'RXLEVDL28', 'RXLEVDL29',
		'RXLEVDL30', 'RXLEVDL31', 'RXLEVDL32', 'RXLEVDL33', 'RXLEVDL34',
		'RXLEVDL35', 'RXLEVDL36', 'RXLEVDL37', 'RXLEVDL38', 'RXLEVDL39',
		'RXLEVDL40', 'RXLEVDL41', 'RXLEVDL42', 'RXLEVDL43', 'RXLEVDL44',
		'RXLEVDL45', 'RXLEVDL46', 'RXLEVDL47', 'RXLEVDL48', 'RXLEVDL49',
		'RXLEVDL50', 'RXLEVDL51', 'RXLEVDL52', 'RXLEVDL53', 'RXLEVDL54',
		'RXLEVDL55', 'RXLEVDL56', 'RXLEVDL57', 'RXLEVDL58', 'RXLEVDL59',
		'RXLEVDL60', 'RXLEVDL61', 'RXLEVDL62', 'RXLEVDL63' ];

//新的开始
//var Rxlev=new object();
var Rxlev_xAxis=['-110','-105','-100','-95','-90','-85','-80','-75','-70','-65','-60','-55'];
//var RxQual=new object();
var RxQual_xAxis=['0','1','2','3','4','5','6','7'];
//var POWER=new object();
var POWER_xAxis=['0','1','2','3','4','5','6','7','8','9','10','11','12','13','14','15','16','17','18','19','20','21','22','23','24','25','26','27','28','29','30','31'];
//var PATHLOSS=new object();
var PATHLOSS_xAxis=['35','40','45','50','55','60','65','70','75','80','85','90','95','100','105','110','115','120','125','130','135','140','145','150','155','160','165','170'];
//var PLDIFF=new object();
var PLDIFF_xAxis=['-25','-20','-15','-10','-5','0','5','10','15','20','25','30','35','40','45'];
//var TA=new object();
var TA_xAxis=['0','1','2','3','4','5','6','7','8','9','10','11','12','13','14','15','16','17','18','19','20','21','22','23','24','25','26','27','28','29','30'];
//存储缓存小区数据
var cellIndexObj=new Object();
$(document).ready(
		function() {
			//tab选项卡
			tab("div_tab", "li", "onclick");//项目服务范围类别切换
//			console.log($(document).height());
			var docheight=$(document).height();
			 $("#chartDiv").css({
		           height: docheight-175
		        });
			initAreaCascade();

//			getAllBscCell($("#cityId1").val());
			getAllLteCell($("#cityId1").val());
			
			$(".cellCls").live("click", function() {
				//getCellMrrList($(this).attr("data"),'chartDiv');
				var mrrChartType=$("#mrrChartType").find("option:selected").val();
				if(mrrChartType=="accumulatedVal"){
					var selDate = $(this).attr("data");
					var cell_msg = $("#cell_msg").val();
					if(cell_msg==""){
						$("#cell_msg").val(selDate);
					}else{
						$("#cell_msg").val(cell_msg+"	"+selDate);
					}
				}else{
					//单小区指标才响应
					var data = $("#date_msg").val();
					if (data=="" || data == null || data == undefined) {
						animateInAndOut("operInfo", 500, 500, 2000, "operTip", "未选择时间段信息！");
						return;
					}
					getSingleCellNiList($(this).attr("data"),'chartDiv');
					selectBsc = $(this).attr('bsc');
				}
			});
			$(".cellCls").live("mouseover", function() {
//				var str=$(this).text();
				$(this).text($(this).attr("all"));
			});
			$(".cellCls").live("mouseout", function() {
//				var str=$(this).text();
				$(this).text($(this).attr("part"));
			});
		

			// 模糊查询
			$("#inputCell").keyup(function() {
//				matchCell();
				searchCell();
			});

			// set highchart
//			sethighchart();
			//新的开始
//			generateCellIndexChart(cellIndexObj,"chartDiv","line","Rxlev",chartType);
			defaultCellIndexChart(cellIndexObj,"chartDiv",true);
			$("#mrrInfoTab tr:first td").text($("#mrrDateType").find("option:selected").text()+"测量信息"); 
			//全屏展现
			$("#fullScreen").click(function(){
				//click全屏
				clickFullScreen();
				});
			$("#fullScreenChart").dblclick(function(){
				//双击全屏关闭
				$("#fullScreenChart").css('display','none');
			});
			$("#chartDiv").dblclick(function(){
				//双击图表全屏
				clickFullScreen();
			});
			//日期选择器
			//timeFormat: "HH:mm:ss",datetimepicker
		   $.datepicker.regional['zh-CN'] = {
			       
			        monthNames: ['一月','二月','三月','四月','五月','六月', '七月','八月','九月','十月','十一月','十二月'],
			        monthNamesShort: ['一月','二月','三月','四月','五月','六月', '七月','八月','九月','十月','十一月','十二月'],
			        dayNamesShort: ['周日','周一','周二','周三','周四','周五','周六'],
			        dayNamesMin: ['日','一','二','三','四','五','六'],
			        closeText: '关闭',
			        constrainInput: true,
			        gotoCurrent:true
			       };
			$.datepicker.setDefaults($.datepicker.regional['zh-CN']);
			$("#mrrMeaBegDate").datetimepicker(
					{
						dateFormat : "yy-mm-dd",
						timeFormat: "HH:mm:ss",
						defaultDate : "-2",
						changeMonth : true,
						numberOfMonths : 1,
						showButtonPanel:true,//是否显示按钮面板  
						closeText: '关闭',
						currentText:'加载数据',
						gotoCurrent:true,
						maxDate:new Date() ,
						timeText:'',
						//buttonClose:false,
						onClose : function(selectedDate,e) {
							/*$("#mrrMeaEndDate").datetimepicker("option", "minDate",
									selectedDate);*/
							if($("#mrrMeaEndDate").datetimepicker("getDate")>addDays(new Date($.trim(selectedDate)),+30)){
								$("#mrrMeaEndDate").datetimepicker("option", "minDate",
										addDays(new Date($.trim(selectedDate)),+30));
							}
							if($("#mrrMeaEndDate").datetimepicker("getDate")<new Date($.trim(selectedDate))){
								$("#mrrMeaEndDate").datetimepicker("option", "minDate",
										addDays(new Date($.trim(selectedDate)),+30));
							}
						},
						onSelect : function(selectedDate) {
							if($("#mrrMeaEndDate").datetimepicker("getDate")>addDays(new Date($.trim(selectedDate)),+30)){
								$("#mrrMeaEndDate").datetimepicker("setDate",addDays(new Date($.trim(selectedDate)),+30));
							}
							if($("#mrrMeaEndDate").datetimepicker("getDate")<new Date($.trim(selectedDate))){
								$("#mrrMeaEndDate").datetimepicker("setDate",addDays(new Date($.trim(selectedDate)),+30));
							}
						}
					});
			 
//			$("#mrrMeaBegDate").datetimepicker("setDate",-30);// 减去2天
//			console.log($('#mrrMeaBegDate').datepicker('option', 'buttonText'));
//			$('#mrrMeaBegDate').datepicker('option', 'currentText','加载');
			$("#mrrMeaBegDate").datetimepicker("setDate",addDays(new Date(),-30).Format("yyyy-MM-dd"));// 减去30天
			$("#mrrMeaEndDate").datetimepicker(
					{
						dateFormat : "yy-mm-dd",
						timeFormat: "HH:mm:ss",
						defaultDate :"+1w",
						changeMonth : true,
						numberOfMonths : 1,
						showButtonPanel:true,//是否显示按钮面板  
						maxDate:new Date(),
						closeText: '关闭',
						currentText:'加载数据',
						gotoCurrent:true,
						timeText:'',
						onClose : function(selectedDate) {
							/*$("#mrrMeaBegDate").datetimepicker("option", "maxDate",
									selectedDate);*/
							if($("#mrrMeaBegDate").datetimepicker("getDate")<addDays(new Date($.trim(selectedDate)),-30)){
								$("#mrrMeaBegDate").datetimepicker("option", "maxDate",
										addDays(new Date($.trim(selectedDate)),-30));
								}
							if($("#mrrMeaBegDate").datetimepicker("getDate")>new Date($.trim(selectedDate))){
								$("#mrrMeaBegDate").datetimepicker("option", "maxDate",
										addDays(new Date($.trim(selectedDate)),-30));
								}
						},
						onSelect : function(selectedDate) {
							if($("#mrrMeaBegDate").datetimepicker("getDate")<addDays(new Date($.trim(selectedDate)),-30)){
							$("#mrrMeaBegDate").datetimepicker("setDate",addDays(new Date($.trim(selectedDate)),-30));
							}
							if($("#mrrMeaBegDate").datetimepicker("getDate")>new Date($.trim(selectedDate))){
								$("#mrrMeaBegDate").datetimepicker("setDate",addDays(new Date($.trim(selectedDate)),-30));
								}
						}
					});
			$("#mrrMeaEndDate").datetimepicker("setDate",(new Date()));
			//切换展示方式
			$("#mrrDisMode").change(function(){
//				$("#chartDiv").empty();//清空
//				$("#chartDiv").attr("style","padding:10px;width:800px;height: 572px");
				var disMode=$(this).find("option:selected").text().trim();
				var dateType=$("#mrrDateType").find("option:selected").text().trim();
				var chartType=$("#mrrChartType").find("option:selected").val();
				if("折线图"===disMode){
					if("Rxlev"===dateType){
						generateCellIndexChart(cellIndexObj,"chartDiv","line","Rxlev",chartType);
					}else if("RxQual"===dateType){
						generateCellIndexChart(cellIndexObj,"chartDiv","line","RxQual",chartType);
					}else if("POWER"===dateType){
						generateCellIndexChart(cellIndexObj,"chartDiv","line","POWER",chartType);
					}else if("PATHLOSS"===dateType){
						generateCellIndexChart(cellIndexObj,"chartDiv","line","PATHLOSS",chartType);
					}else if("PLDIFF"===dateType){
						generateCellIndexChart(cellIndexObj,"chartDiv","line","PLDIFF",chartType);
					}else if("TA"===dateType){
						generateCellIndexChart(cellIndexObj,"chartDiv","line","TA",chartType);
					}
				}else if("柱状图"===disMode){
					if("Rxlev"===dateType){
						generateCellIndexChart(cellIndexObj,"chartDiv","bar","Rxlev",chartType);
					}else if("RxQual"===dateType){
						generateCellIndexChart(cellIndexObj,"chartDiv","bar","RxQual",chartType);
					}else if("POWER"===dateType){
						generateCellIndexChart(cellIndexObj,"chartDiv","bar","POWER",chartType);
					}else if("PATHLOSS"===dateType){
						generateCellIndexChart(cellIndexObj,"chartDiv","bar","PATHLOSS",chartType);
					}else if("PLDIFF"===dateType){
						generateCellIndexChart(cellIndexObj,"chartDiv","bar","PLDIFF",chartType);
					}else if("TA"===dateType){
						generateCellIndexChart(cellIndexObj,"chartDiv","bar","TA",chartType);
					}
				}
				
			});
			//切换数据类型
			$("#mrrDateType").change(function(){
				getCellMrrList(selectCell,'chartDiv');				
			});
			//选取不同信道组号
			$("#mrrChgr").change(function(){
				getCellMrrList(selectCell,'chartDiv');
			});
			//结束日期改变触发
			/*$("#mrrMeaEndDate").change(function(){
				getCellMrrList(selectCell);
			});*/
			//选择日期重新加载
			$(document).delegate('.ui-datepicker-current', 'click',
					function() {
						getCellMrrList(selectCell,'chartDiv');
					});
			
			$("#end_date_seq").hide();
			$("#end_date_div").hide();
			$("#date_text_change").text("时段选择");
			$("#addto_date").show();
			$("#cell_msg").hide();
			$("#cellselect_span").hide();
			$("#cellsconfirm").hide();
			//图表改变触发
			$("#mrrChartType").change(function(){
				//getCellMrrList(selectCell,'chartDiv');
				var mrrChartType=$("#mrrChartType").find("option:selected").val();
				if(mrrChartType=="singleVal"){
					$("#end_date_seq").hide();
					$("#end_date_div").hide();
					$("#date_text_change").text("时段选择");
					$("#addto_date").show();
					$("#date_msg").show();
					$("#date_msg").val("");
					$("#cell_msg").hide();
					$("#cell_msg").val("");
					$("#cellselect_span").hide();
					$("#cellsconfirm").hide();
					defaultCellIndexChart(null,"chartDiv",false);
					selectCell = "";
				}
				if(mrrChartType=="accumulatedVal"){
					$("#end_date_seq").show();
					$("#end_date_div").show();
					$("#date_text_change").text("开始时间");
					$("#addto_date").hide();
					$("#date_msg").hide();
					$("#date_msg").val("");
					$("#cell_msg").show();
					$("#cell_msg").val("");
					$("#cellselect_span").show();
					$("#cellsconfirm").show();
					defaultCellIndexChart(null,"chartDiv",false);
					selectCell = "";
				}
			});
			$("#addto_date").click(function(){
				var selDate = $("#mrrMeaBegDate").val();
				var date_msg = $("#date_msg").val();
				if(date_msg==""){
					$("#date_msg").val(selDate);
				}else{
					$("#date_msg").val(date_msg+"	"+selDate);
				}
				
			});
			 
			 
			 $("#cellsconfirm").click(function(){
			 
				 getMultiCellNiList("",'chartDiv');
			 });
			
		});

/**
 * 输入小区名，模糊查询小区，把符合条件的小区列出来， 其他的隐藏
 */
function matchCell() {
	var cell = $.trim($("#inputCell").val());
	var count=0;
	if (cell == '') {
		$("span.cellCls").each(function(i, ele) {
			if ($(ele).closest("li").css("display") == 'none') {
				$(ele).closest("li").css("display", '');
			}
		});
		$("span.bscCls").each(function(i, ele) {
			if ($(ele).closest("li").css("display") == 'none') {
				$(ele).closest("li").css("display", '');
			}
		});
	} else {
		cell = cell.toUpperCase();
//		console.log("筛选【" + cell + "】");
		$("span.cellCls").each(function(i, ele) {
			if ($(ele).attr("data").indexOf(cell) >= 0) {
				if ($(ele).closest("li").css("display") == 'none') {
					$(ele).closest("li").css("display", '');
				}
			} else {
				$(ele).closest("li").css("display", 'none');
			}
		});
		
	}
			if(cell != ''){
				$("span.bscCls").each(function(i, ele) {
					var aa=$(ele).text();//HZSMNB8（小区数量:85）
					var bscstr=aa.substring(0,aa.indexOf("("));//HZSMNB8
//					console.log(aa.substring(0,aa.indexOf("（")));
//					console.log(aa);
//					console.log(aa.indexOf("（"));
//					console.log(aa.substring(aa.indexOf("（")));
//					console.log($("span[bsc='"+bscstr+"']").attr("bsc"));
					$("span[bsc='"+bscstr+"']").each(function(i, ele) {
//							console.log("i:"+i);
							if ($(ele).closest("li").css("display") != 'none') {
									count++;
//									console.log("count:"+count);
								}
							
					})
					$(ele).text(bscstr+"(小区数量:"+count+")");
					if(count==0){
						$(ele).closest("li").css("display", 'none');
					}else{
						$(ele).closest("li").css("display", '');
					}
					count=0;
				});
			}
}


function initAreaCascade() {
	// 区域联动事件
	$("#provinceId1").change(function() {
		getSubAreas("provinceId1", "cityId1", "市");
	});
	$("#cityId1").change(function() {
		// 清除bsc列表
		$("#allBscCell").html("");
		// 重新加载bsc列表
//		getAllBscCell($("#cityId1").val());
		getAllLteCell($("#cityId1").val());
	});

}

/**
 * 获取包含有指定小区的ncs的信息
 * 
 * @param cell
 *//*
function getCellNcsList(cell) {
	// cell = 'S3ASDS1';
	selectCell = cell;

	// alert(cell);

	// 情况日期下拉框
	$("#ncsDate").html("");
	$("#ncsTime").html("");

	var cityId = $("#cityId1").val();

	$.ajax({
		url : 'searchNcsContainsCellForAjaxAction',
		data : {
			'cell' : cell,
			'cityId' : cityId
		},
		dataType : 'text',
		type : 'post',
		success : function(raw) {
			ncsDateToTime = new Object();
			ncsIdToDetail = new Object();
			ncsDates.splice(0, ncsDates.length);

			var data = eval("(" + raw + ")");
			var one;
			var ncsId;
			var index, startdate, starthour;
			var times;
			var t;
			for ( var i = 0; i < data.length; i++) {
				one = data[i];
				if (!one) {
					continue;
				}
				ncsId = one['RNO_NCS_DESC_ID'];
				startTime = one['START_TIME'];
				index = startTime.indexOf(' ');
				startdate = startTime.substring(0, index);
				starthour = startTime.substring(index + 1);

				// 记录
				ncsIdToDetail[ncsId] = one;
				//
				times = ncsDateToTime[startdate];
				if (!times) {
					times = new Array();
					ncsDateToTime[startdate] = times;

					ncsDates.push(startdate);
				}
				t = new Object();
				t['time'] = starthour;
				t['ncsId'] = ncsId;
				times.push(t);
			}

			// 填充日期和时间下来框
			if (ncsDates.length > 0) {
				var ds = "";
				for ( var j = 0; j < ncsDates.length; j++) {
					ds += "<option value='" + ncsDates[j] + "'>" + ncsDates[j]
							+ "</option>";
				}
				// alert(ds);
				$("#ncsDate").html(ds);

				// 取第一个日期的时间填充时间下来框
				var times = ncsDateToTime[startdate];
				if (times) {
					var ts = "";
					for ( var j = 0; j < times.length; j++) {
						ts += "<option value='" + times[j]['ncsId'] + "'>"
								+ times[j]['time'] + "</option>"
					}
					// alert(ts);
					$("#ncsTime").html(ts);
				}

				// 触发一次获取小区在ncs里的测量信息的事件
				$("#ncsTime").trigger("change");
			} else {
				animateInAndOut("operInfo", 500, 500, 1000, "operTip",
						"该小区无测量数据！");
				return;
			}
		}
	})
}*/

/**
 * 获取区域下的所有的bsc列表
 * 
 * @param cityId
 */
/*function getAllBscCell(cityId) {
	//正在加载 <em class="loading_fb" id="tipcontentId"></em>,请稍侯...
	var cityname=$("#cityId1").find("option:selected").text();
	var str="正在加载"+cityname+"小区数据 <em class=\"loading_fb\" id=\"tipcontentId\"></em>,请稍侯...";
	$(".loading").html(str);
	$(".loading_cover").css("display","block");
	//getAllBscCellsInCityForAjaxAction
	$.ajax({
		url : 'getAllBscCellsInCityForAjaxAction',
		data : {
			'cityId' : cityId
		},
		type : 'post',
		dataType : 'text',
		success : function(raw) {
			$("#allBscCell").html(raw);
			$("#allBscCell").treeview({
				collapsed : true
			});
			$("span.cellCls").each(function(i, ele) {
			
			var str=$(ele).text();
			$(ele).attr("all",str) ;
			$(ele).attr("part",str.length>14?str.substring(0,14)+"...":str) ;
			$(ele).text($(ele).attr("part"));
			});
		},
		complete:function(){
		$(".loading_cover").css("display","none");
		}
	})
}*/
/**
 * 获取区域下的所有的bsc列表
 */
function getAllBscCell(cityId) {
	//正在加载 <em class="loading_fb" id="tipcontentId"></em>,请稍侯...
	var cityname=$("#cityId1").find("option:selected").text();
	/*var str="正在加载"+cityname+"小区数据 <em class=\"loading_fb\" id=\"tipcontentId\"></em>,请稍侯...";
	$(".loading").html(str);
	$(".loading_cover").css("display","block");*/
	showOperTips("loadingDataDiv", "tipcontentId", cityname+"小区数据");
	$.ajax({
		url : 'getAllBscCellsByCityIdInMapForAjaxAction',
		data : {
			'cityId' : cityId
		},
		type : 'post',
		dataType : 'text',
		success : function(raw) {
			var data = eval("("+raw+")");
			bscToCells = data;
			var bscList = new Array();
			for(var key in data) {
				bscList.push(key);
			}
			bscList.sort();
			//console.log(bscList);
			var bscHtml = "";

			for(var i=0; i<bscList.length; i++) {
				bscHtml += "<li><span class='bscCls' id='"+bscList[i]+"'>" + bscList[i] + "(小区数量：" 
					+ bscToCells[bscList[i]].length + ")</span><ul id='"+bscList[i]+"'></ul></li>";
			} 
			
			$("#allBscCell").html(bscHtml);
			$("#allBscCell").treeview({
				collapsed : true
			});

			$("span.bscCls").on("click", function() {
				var bsc = $(this).attr("id");
				var cells = bscToCells[bsc];
				//console.log(cells);
				var cellsHtml = "";
				var all;
				var part;
				for(var i=0; i<cells.length; i++) {
					all = cells[i]['LABEL'] + "(" + cells[i]['NAME'] + ")";
					part = all.length>12?all.substring(0,12)+"...":all;
					cellsHtml += "<li><span class='cellCls' data='" + cells[i]['LABEL'] + "'" 
							+ " bsc='" + bsc + "'" 
							+ " manufacturers='"+cells[i]['MANUFACTURERS']+"' " 
							+ " all='"+all+"' " 
							+ " part='"+part+"'>" 
							+ part +"</span></li>";
				}
				$("ul#"+bsc).html(cellsHtml);
			});
			//图标事件
			$("#allBscCell").find("div").bind("click", function(event) {
				var bsc = $(this).parent().find("span").attr("id");
				var cells = bscToCells[bsc];
				//console.log(cells);
				var cellsHtml = "";
				var all;
				var part;
				for(var i=0; i<cells.length; i++) {
					all = cells[i]['LABEL'] + "(" + cells[i]['NAME'] + ")";
					part = all.length>12?all.substring(0,12)+"...":all;
					cellsHtml += "<li><span class='cellCls' data='" + cells[i]['LABEL'] + "'" 
							+ " bsc='" + bsc + "'" 
							+ " manufacturers='"+cells[i]['MANUFACTURERS']+"' " 
							+ " all='"+all+"' " 
							+ " part='"+part+"'>" 
							+ part +"</span></li>";
				}
				$("ul#"+bsc).html(cellsHtml);
            });
		},
		complete:function(){
			$(".loading_cover").css("display","none");
		}
	})
}

/**
 * 在某个基准日期的基础上，对时间进行加减
 * 正数为加，负数为减去
 * @param baseDate
 * @param day
 * @returns {Date}
 */
function addDays(baseDate,day){
    var tinoneday=24*60*60*1000;
    var nt=baseDate.getTime();
    var cht=day*tinoneday;
    var newD=new Date();
    newD.setTime(nt+cht);
    return newD;
}
/**
 * 
 * @title 生成动态小区指标图表
 * @param data
 * @param chartDiv 装载图表容器
 * @param disMode 展现方式
 * @param dataType 数据类型
 * @param chartType 图表类型
 * @author chao.xj
 * @date 2014-11-19下午3:37:55
 * @company 怡创科技
 * @version 1.2
 */
function generateCellIndexChart(data,chartDiv,disMode,dataType,chartType) {
	
	if (data == null || data == undefined) {
		animateInAndOut("operInfo", 500, 500, 2000, "operTip", "无相关MRR数据！");
		return;
	}
//	console.log(dataType+"---data:"+data['CELL_NAME']);
	var dom=document.getElementById(chartDiv);
//	dom=$("#"+chartDiv )[0];从jquery至dom对象
	var UL=new Array();//上行有序数组
	var DL=new Array();//下行有序数组
//	if(dataType.substring(0,dataType.lastIndexOf("_"))=='Rxlev'){
	var boundGap=false;
	var cell=data['CELL_NAME'];
	var channel_group_num=data['CHANNEL_GROUP_NUM'];
	var title="";//图表子标题
	var fieldUL="";//上行字段前缀
	var fieldDL="";//下行字段前缀
	var serieNameUL="";//上行系统名
	var serieNameDL="";//下行系统名
	var xAxis_name="";//X轴名称
	var accumuValUL=0;//上行累积值
	var accumuValDL=0;//下行累积值
	var axis =new Array();
			if(data!=null){
				axis.length=0;
				if(dataType=='Rxlev'){
					title=cell+(channel_group_num==null?"全部信道组":"信道组号"+channel_group_num)+"的接收电平分布图";
					fieldUL="RXLEVUL";
					fieldDL="RXLEVDL";
					serieNameUL="上行电平";
					serieNameDL="下行电平";
					xAxis_name="电平值(dBm)";
					for ( var key in data) {
//						console.log(key+"="+obj[key]);
						if(key.indexOf("RXLEVUL")!=-1){
							UL[getNum(key)]=data[key];
							axis.push(getNum(key)-110);
							
						}
						if(key.indexOf("RXLEVDL")!=-1){
							DL[getNum(key)]=data[key];
						}
					}
				}else if(dataType=='RxQual'){
					title=cell+(channel_group_num==null?"全部信道组":"信道组号"+channel_group_num)+"的通话质量分布图";
					fieldUL="RXQUALUL";
					fieldDL="RXQUALDL";
					serieNameUL="上行质量";
					serieNameDL="下行质量";
					xAxis_name="质量";
					boundGap=true;
					for ( var key in data) {
//						console.log(key+"="+obj[key]);
						if(key.indexOf("RXQUALUL")!=-1){
							UL[getNum(key)]=data[key];
							axis.push(getNum(key));
						}
						if(key.indexOf("RXQUALDL")!=-1){
							DL[getNum(key)]=data[key];
						}
					}
				}else if(dataType=='POWER'){
					title=cell+(channel_group_num==null?"全部信道组":"信道组号"+channel_group_num)+"的发射功率分布图";
					fieldUL="MSPOWER";
					fieldDL="BSPOWER";
					serieNameUL="手机功率";
					serieNameDL="基站功率";
					xAxis_name="功率等级(dBm)";
					for ( var key in data) {
//						console.log(key+"="+obj[key]);
						if(key.indexOf("MSPOWER")!=-1){
							UL[getNum(key)]=data[key];
							axis.push(getNum(key));
						}
						if(key.indexOf("BSPOWER")!=-1){
							DL[getNum(key)]=data[key];
						}
					}
				}else if(dataType=='PATHLOSS'){
					boundGap=true;
					title=cell+(channel_group_num==null?"全部信道组":"信道组号"+channel_group_num)+"的路径损耗分布图";
					fieldUL="PLOSSUL";
					fieldDL="PLOSSDL";
					serieNameUL="上行路径损耗";
					serieNameDL="下行路径损耗";
					xAxis_name="路径损耗(dBm)";
					for ( var key in data) {
//						console.log(key+"="+obj[key]);
						if(key.indexOf("PLOSSUL")!=-1){
							UL[getNum(key)]=data[key];
							axis.push(getNum(key)*2+30);
						}
						if(key.indexOf("PLOSSDL")!=-1){
							DL[getNum(key)]=data[key];
						}
					}
				}else if(dataType=='PLDIFF'){
					boundGap=true;
					title=cell+(channel_group_num==null?"全部信道组":"信道组号"+channel_group_num)+"的上下行路径损耗分布图";
					fieldUL="PLDIFF";
					serieNameUL="下行-上行路径损耗差";
					xAxis_name="下行-上行路径损耗差(dBm)";
					for ( var key in data) {
//						console.log(key+"="+obj[key]);
						if(key.indexOf("PLDIFF")!=-1){
							UL[getNum(key)]=data[key];
							axis.push(getNum(key)-25);
						}
					}
					DL.length=0;
				}else if(dataType=='TA'){
					title=cell+(channel_group_num==null?"全部信道组":"信道组号"+channel_group_num)+"的时间提前量分布图";
					fieldUL="TAVAL";
					serieNameUL="时间提前量";
					xAxis_name="时间提前量";
					for ( var key in data) {
//						console.log(key+"="+obj[key]);
						if(key.indexOf("TAVAL")!=-1){
							if(chartDiv=="fullScreenChart"){
								UL[getNum(key)]=data[key];
								axis.push(getNum(key));
				            }else{
				            	if(getNum(key)<=35){
				            		UL[getNum(key)]=data[key];
				            		axis.push(getNum(key));
				            	}
				            }
						}
					}
					DL.length=0;
				}
//				axis.sort();
//				axis.sort(function(a,b){return a>b?1:-1});
				axis.sort(function compare(a,b){return a-b;});
				//填充测量信息表
//				$("#mrrInfoTab tr").eq(0).nextAll().remove();
				$("#mrrInfoTab tr:first td").text(dataType+"测量信息"); 
				$("#mrrInfoTab tr:not(:first)").remove();  
				$("#mrrInfoTab").append("<TR><td class='menuTd'>CELL_NAME</td><td>"+cell+"</td></TR>");
				$("#mrrInfoTab").append("<TR><td class='menuTd'>CHANNEL_GROUP_NUM</td><td>"+(typeof(channel_group_num)=="undefined"?"全部":channel_group_num)+"</td></TR>");
				for ( var i = 0; i < UL.length; i++) {
					$("#mrrInfoTab").append("<TR><td class='menuTd'>"+fieldUL+i+"</td><td>"+UL[i]+"</td></TR>");
					if(chartType=="accumulatedVal"){
						accumuValUL+=UL[i];
						UL[i]=accumuValUL;
					}
				}
				for ( var j = 0; j < DL.length; j++) {
					$("#mrrInfoTab").append("<TR><td class='menuTd'>"+fieldDL+j+"</td><td>"+DL[j]+"</td></TR>");
					if(chartType=="accumulatedVal"){
						accumuValDL+=DL[j];
						DL[j]=accumuValDL;
					}
				}
			}
//	}
	require(
	        [
	            'echarts',
	            'echarts/chart/line',   // 按需加载所需图表，如需动态类型切换功能，别忘了同时加载相应图表
	            'echarts/chart/bar'
	        ],
	        function (ec) {
	            var myChart = ec.init(dom);
	            
	            myChart.showLoading({
	                text : "图表数据正在努力加载..."
	            });
	            //信道组号0的接收电平分布图
	            var option = {
	            		title : {
	            	        text: dataType+'指标',
	            	        subtext: title,
	            			x: "center", //标题水平方向位置
	            			subtextStyle:{color: '#4A4AFF'} 
	            	    },
	            	    tooltip : {
	            	        trigger: 'axis'
	            	    },
	            	    legend: {
	            	    	orient:'horizontal',
	            	        data:[serieNameUL,serieNameDL] ,
	            	        y	: 'top',
	            	        x 	: 'left',
	            	       /* itemWidth:10,
	            	        itemHeight:20,*/
	            	       /* formatter:function(val){
	            	            return (serieNameDL==""?val.split("").join("\n"):val);
	            	        },*/
	            	        padding: (serieNameDL==""?[5, 5, 5, 5]:0)
	            	    },
	            	    toolbox: {
	            	        show : true,
	            	        feature : {
	            	            mark : {show: (chartDiv=="fullScreenChart"?false:true)},
	            	            dataView : {show: (chartDiv=="fullScreenChart"?false:true), readOnly: false},
	            	            magicType : {show: (chartDiv=="fullScreenChart"?false:true), type: ['line', 'bar']},
	            	            restore : {show: (chartDiv=="fullScreenChart"?false:true)},
	            	            saveAsImage : {show: (chartDiv=="fullScreenChart"?false:true)},
	            	            myCloseTool : {
	            	                show : (chartDiv=="fullScreenChart"?true:false),
	            	                title : '关闭',
	            	                icon : 'image://../rno/jslib/echarts/close1.png',
	            	                onclick : function (){
	            	                	$("#fullScreenChart").css('display','none');
	            	                }
	            	            },
	            	            myFullTool : {
	            	                show : (chartDiv=="fullScreenChart"?false:true),
	            	                title : '全屏',
	            	                icon : 'image://../rno/jslib/echarts/full1.png',
	            	                onclick : function (){
	            	                	clickFullScreen();
	            	                }
	            	            }
	            	        }
	            	    },
	            	    calculable : true,
	            	    xAxis : [
	            	        {
	            	        	name:xAxis_name,
	            	            type : 'category',
	            	            boundaryGap : boundGap,
	            	            data : axis,
	            	            splitLine:{lineStyle:{type: 'dashed'}},
	            	            axisLine:{lineStyle:{color: '#223434'}},
	            	            axisTick:{show:true,length:5}
	            	        }
	            	    ],
	            	    yAxis : [
	            	        {
	            	        	name:'采样点个数(个)',
	            	            type : 'value',
	            	            axisLabel : {
	            	                formatter: '{value}'
	            	            },
	            	            splitLine:{lineStyle:{type: 'dashed'}},
	            	            axisLine:{lineStyle:{color: '#223434'}}
	            	        }
	            	    ],
	            	    //112,56, 233, 343, 454, 89, 343, 123, 66, 123
	            	    series : [
	            	        {
	            	            name:serieNameUL,
	            	            type:disMode,
	            	            data:UL,
	            	            markPoint : {
	            	                data : [
	            	                    {type : 'max', name: '最大值'},
	            	                    {type : 'min', name: '最小值'}
	            	                ]
	            	            },
	            	            markLine : {
	            	                data : [
	            	                    {type : 'average', name: '平均值'}
	            	                ]
	            	            },
	            	            itemStyle :{normal :{color:"#FF0000"}}
	            	        },
	            	        //45, 23, 49, 33, 43, 44, 38, 23, 46, 13
	            	        {
	            	            name:serieNameDL,
	            	            type:disMode,
	            	            data:DL,
	            	            markPoint : {
	            	                data : [
	            	                    {name : '周最低', value : -2, xAxis: 1, yAxis: -1.5},
	            	                    {type : 'max', name: '最大值'},
	            	                    {type : 'min', name: '最小值'}
	            	                ]
	            	            },
	            	            markLine : {
	            	                data : [
	            	                    {type : 'average', name : '平均值'}
	            	                ]
	            	            },
	            	            itemStyle :{normal :{color:"#007300"}}
	            	        }
	            	    ]
	            }
	            myChart.hideLoading();
	            myChart.setOption(option);
	            if(chartDiv=="fullScreenChart"){
	            	$("#fullScreenChart").css("background","white");
	            }
	        }
	    );

}
/**
 * 
 * @title 获取包含有指定小区的mrr的信息
 * @param cell
 * @author chao.xj
 * @date 2014-11-17下午4:16:40
 * @company 怡创科技
 * @version 1.2
 */
function getCellMrrList(cell,chartDiv) {
	// cell = 'S3ASDS1';
	if(cell==null || cell==""){
		animateInAndOut("operInfo", 500, 500, 2000, "operTip", "请先选择小区数据！");
		return;
	}
	showOperTips("loadingDataDiv", "tipcontentId", "小区指标数据");
	selectCell = cell;
	// alert(cell);

	var cityId = $("#cityId1").find("option:selected").val();
	var mrrMeaBegDate=$("#mrrMeaBegDate").val();
	var mrrMeaEndDate=$("#mrrMeaEndDate").val();
	var mrrDateType=$("#mrrDateType").find("option:selected").text();
	var mrrChgr=$("#mrrChgr").find("option:selected").text();
	var mrrChartType=$("#mrrChartType").find("option:selected").val();
	var mrrDisMode=$("#mrrDisMode").find("option:selected").text();
	$.ajax({
		url : 'search4GNiCellIndexForAjaxAction',
		data : {
			'attachParams.cell' : cell,
			'attachParams.cityId' : cityId,
			'attachParams.meaBegTime' : mrrMeaBegDate,
			'attachParams.meaEndTime' : mrrMeaEndDate,
			'attachParams.dataType' : mrrDateType,
			'attachParams.chartType' : mrrChartType,
			'attachParams.disMode' : mrrDisMode,
			'attachParams.chgr' : mrrChgr
		},
		dataType : 'text',
		type : 'post',
		success : function(raw) {
//			console.log("raw:"+raw);
			var data = eval("(" + raw + ")");
			data =data['data'];
			var obj;
			obj=data[0];
			cellIndexObj=obj;
			if (obj == null || obj == undefined) {
				defaultCellIndexChart(obj,"chartDiv",false);
			}else{
				
				if("折线图"===mrrDisMode){
					generateCellIndexChart(obj,chartDiv,"line",mrrDateType,mrrChartType);
				}else if("柱状图"===mrrDisMode){
					generateCellIndexChart(obj,chartDiv,"bar",mrrDateType,mrrChartType);
				}
				showOperTips("loadingDataDiv", "tipcontentId", "数据成功展现");
			}
		},
		complete:function(){
			hideOperTips("loadingDataDiv");
			}
	})
}
/**
 * 
 * @title 从字符串中提取数字
 * @param text
 * @returns
 * @author chao.xj
 * @date 2014-11-18下午12:06:27
 * @company 怡创科技
 * @version 1.2
 */
function getNum(text){
	var value = text.replace(/[^0-9]/ig,""); 
	return value;
	}
/*function test(arrDemo){
	
	arrDemo.sort(function(a,b){return a>b?1:-1});//从小到大排序
}*/
/**
 * 生成缺省小区指标图表
 */
function defaultCellIndexChart(data,chartDiv,isInit) {
	if(isInit){
		
		if (data == null || data == undefined) {
			animateInAndOut("operInfo", 500, 500, 2000, "operTip", "请选择小区数据！");
			return;
		}
	}/*else{
		if (data == null || data == undefined) {
			animateInAndOut("operInfo", 500, 500, 2000, "operTip", "无相关MRR数据！");
		}
	}*/
	$("#mrrInfoTab tr:not(:first)").remove();  
	for ( var i = 0; i < Rxlev.length; i++) {
		$("#mrrInfoTab").append("<TR><td class='menuTd'>"+Rxlev[i]+"</td><td></td></TR>");
	}
	var dom=document.getElementById(chartDiv);
	require(
	        [
	            'echarts',
	            'echarts/chart/line',   // 按需加载所需图表，如需动态类型切换功能，别忘了同时加载相应图表
	            'echarts/chart/bar'
	        ],
	        function (ec) {
	            var myChart = ec.init(dom);
	            
	            myChart.showLoading({
	                text : "图表数据正在努力加载..."
	            });
	            //信道组号0的接收电平分布图
	            var option = {
	            		title : {
	            	        text: '标题',
	            	        subtext: '请在左侧选择小区数据',
	            			x: "center", //标题水平方向位置
	            			subtextStyle:{color: '#FF0000'} 
	            	    },
	            	    tooltip : {
	            	        trigger: 'axis'
	            	    },
	            	    toolbox: {
	            	        show : true,
	            	        feature : {
	            	            mark : {show: true},
	            	            dataView : {show: true, readOnly: false},
	            	            magicType : {show: true, type: ['line', 'bar']},
	            	            restore : {show: true},
	            	            saveAsImage : {show: true}
	            	        }
	            	    },
	            	    calculable : true,
	            	    xAxis : [
	            	        {
	            	        	name:'X轴',
	            	            type : 'category',
	            	            boundaryGap : false,
	            	            data : ['00:00','05:00','12:00','18:00','00:00'],
	            	            splitLine:{lineStyle:{type: 'dashed'}},
	            	            axisLine:{lineStyle:{color: '#223434'}},
	            	            axisTick:{show:true,length:5}
	            	        }
	            	    ],
	            	    yAxis : [
	            	        {
	            	        	name:'Y轴',
	            	            type : 'category',
	            	            data : ['0.0','0.2','0.4','0.6','0.8','1.0','1.2'],
	            	            axisLabel : {
	            	                formatter: '{value}'
	            	            },
	            	            splitLine:{lineStyle:{type: 'dashed'}},
	            	            axisLine:{lineStyle:{color: '#223434'}}
	            	        }
	            	    ],
	            	    //112,56, 233, 343, 454, 89, 343, 123, 66, 123
	            	    series : [
	            	        {
	            	            name:'',
	            	            type:'line',
	            	            data:[],
	            	            markPoint : {
	            	                data : [
	            	                    {type : 'max', name: '最大值'},
	            	                    {type : 'min', name: '最小值'}
	            	                ]
	            	            },
	            	            markLine : {
	            	                data : [
	            	                    {type : 'average', name: '平均值'}
	            	                ]
	            	            },
	            	            itemStyle :{normal :{color:"#FF0000"}}
	            	        },
	            	        //45, 23, 49, 33, 43, 44, 38, 23, 46, 13
	            	        {
	            	            name:'',
	            	            type:'line',
	            	            data:[],
	            	            markPoint : {
	            	                data : [
	            	                    {name : '周最低', value : -2, xAxis: 1, yAxis: -1.5},
	            	                    {type : 'max', name: '最大值'},
	            	                    {type : 'min', name: '最小值'}
	            	                ]
	            	            },
	            	            markLine : {
	            	                data : [
	            	                    {type : 'average', name : '平均值'}
	            	                ]
	            	            },
	            	            itemStyle :{normal :{color:"#007300"}}
	            	        }
	            	    ]
	            }
	            myChart.hideLoading();
	            myChart.setOption(option);
	        }
	    );

}
/**
 * 对Date的扩展，将 Date 转化为指定格式的String   
 *月(M)、日(d)、小时(h)、分(m)、秒(s)、季度(q) 可以用 1-2 个占位符，   
 *年(y)可以用 1-4 个占位符，毫秒(S)只能用 1 个占位符(是 1-3 位的数字)   
 *例子：   
 *(new Date()).Format("yyyy-MM-dd hh:mm:ss.S") ==> 2006-07-02 08:09:04.423   
 *(new Date()).Format("yyyy-M-d h:m:s.S")      ==> 2006-7-2 8:9:4.18   
 * @title 
 * @param fmt
 * @returns
 * @author chao.xj
 * @date 2014-11-16下午3:58:43
 * @company 怡创科技
 * @version 1.2
 */
Date.prototype.Format = function(fmt)   
{  
  var o = {   
    "M+" : this.getMonth()+1,                 //月份   
    "d+" : this.getDate(),                    //日   
    "h+" : this.getHours(),                   //小时   
    "m+" : this.getMinutes(),                 //分   
    "s+" : this.getSeconds(),                 //秒   
    "q+" : Math.floor((this.getMonth()+3)/3), //季度   
    "S"  : this.getMilliseconds()             //毫秒   
  };   
  if(/(y+)/.test(fmt))   
    fmt=fmt.replace(RegExp.$1, (this.getFullYear()+"").substr(4 - RegExp.$1.length));   
  for(var k in o)   
    if(new RegExp("("+ k +")").test(fmt))   
  fmt = fmt.replace(RegExp.$1, (RegExp.$1.length==1) ? (o[k]) : (("00"+ o[k]).substr((""+ o[k]).length)));   
  return fmt;   
}
/**
 * 单击全屏幕展现
 */
function clickFullScreen(){
	/*if(selectCell==""){
		animateInAndOut("operInfo", 500, 500, 2000, "operTip", "请选择小区数据！");
		return;
	}*/
	$("#fullScreenChart").css('display','block');
	/*$("#fullScreenChart").attr('style','background-color: #cfeace;height:100%;border: 1px solid #00457b;padding: 8px;font-size: .85em; ');
	$("html,body").css('height','97%').css('margin','0px');*/
       /*属性*/
	    $("#fullScreenChart").css({
		"position" : "absolute",
		"text-align" : "center",
		"top" : "0px",
		"left" : "0px",
		"right" : "0px",
		"bottom" : "0px",
		"background" : "white",
		"visibility" : "visible",
		"z-index":"999"	
			/*,
		"filter" : "Alpha(opacity=90)",
		"-moz-opacity":"0.4"*/
	});
       /*高为屏幕的高*/
       $("#fullScreenChart").css({
           height: function () {
           return $(document).height();
       },
           width:"100%"
        });
     
	//getCellMrrList(selectCell,'fullScreenChart');
	var mrrChartType=$("#mrrChartType").find("option:selected").val();
	if(mrrChartType=="accumulatedVal"){
		getMultiCellNiList("",'fullScreenChart');
	}else{
		getSingleCellNiList(selectCell,'fullScreenChart');
	}
//	$("#fullScreenChart").css("background","white");
}
/**
 * 输入小区名，模糊查询小区，把符合条件的小区列出来， 其他的隐藏
 */
function searchCell() {
	var cell = $.trim($("#inputCell").val());
	//清空
	$("#allBscCell").html("");

	//符合搜索条件的bsc到cell信息的映射对象
	var obj = new Object();
	var list;
	for(var key in bscToCells) {
		list = new Array();
		for(var i=0; i<bscToCells[key].length; i++) {
			if(bscToCells[key][i]["LABEL"].indexOf(cell) >= 0) {
				list.push(bscToCells[key][i]);
			}
		}
		if(list.length > 0) {
			obj[key] = list;
		}
	}
	//console.log(obj);
	
	//排序BSC
	var bscList = new Array();
	for(var key in obj) {
		bscList.push(key);
	}
	bscList.sort();
	//console.log(bscList);
	var bscHtml = "";
	
	for(var i=0; i<bscList.length; i++) {
		bscHtml += "<li><span class='bscCls' id='"+bscList[i]+"'>" + bscList[i] + "(小区数量：" 
			+ obj[bscList[i]].length + ")</span><ul id='"+bscList[i]+"'></ul></li>";
	} 

	$("#allBscCell").html(bscHtml);
	$("#allBscCell").treeview({
		collapsed : true
	});

	$("span.bscCls").on("click", function() {
		var bsc = $(this).attr("id");
		var cells1 = obj[bsc];
		var cellsHtml = "";
		var all;
		var part;
		for(var i=0; i<cells1.length; i++) {
			all = cells1[i]['LABEL'] + "(" + cells1[i]['NAME'] + ")";
			part = all.length>12?all.substring(0,12)+"...":all;
			cellsHtml += "<li><span class='cellCls' data='" + cells1[i]['LABEL'] + "'" 
					+ " bsc='" + bsc + "'" 
					+ " manufacturers='"+cells1[i]['MANUFACTURERS']+"' " 
					+ " all='"+all+"' " 
					+ " part='"+part+"'>" 
					+ part +"</span></li>";
		}
		$("ul#"+bsc).html(cellsHtml);
	});
	//图标事件
	$("#allBscCell").find("div").bind("click", function(event) {
		var bsc = $(this).parent().find("span").attr("id");
		var cells2 = obj[bsc];
		//console.log(cells);
		var cellsHtml = "";
		var all;
		var part;
		for(var i=0; i<cells2.length; i++) {
			all = cells2[i]['LABEL'] + "(" + cells2[i]['NAME'] + ")";
			part = all.length>12?all.substring(0,12)+"...":all;
			cellsHtml += "<li><span class='cellCls' data='" + cells2[i]['LABEL'] + "'" 
					+ " bsc='" + bsc + "'" 
					+ " manufacturers='"+cells2[i]['MANUFACTURERS']+"' " 
					+ " all='"+all+"' " 
					+ " part='"+part+"'>" 
					+ part +"</span></li>";
		}
		$("ul#"+bsc).html(cellsHtml);
    });

}
/**
 * 获取区域下的所有的LTe小区列表
 */
function getAllLteCell(cityId) {
	//正在加载 <em class="loading_fb" id="tipcontentId"></em>,请稍侯...
	var cityname=$("#cityId1").find("option:selected").text();
	/*var str="正在加载"+cityname+"小区数据 <em class=\"loading_fb\" id=\"tipcontentId\"></em>,请稍侯...";
	$(".loading").html(str);
	$(".loading_cover").css("display","block");*/
	showOperTips("loadingDataDiv", "tipcontentId", cityname+"小区数据");
	$.ajax({
		url : 'getAllLteCellsByCityIdInMapForAjaxAction',
		data : {
			'cityId' : cityId
		},
		type : 'post',
		dataType : 'text',
		success : function(raw) {
			var data = eval("("+raw+")");
			bscToCells = data;
			var bscList = new Array();
			for(var key in data) {
				bscList.push(key);
			}
			bscList.sort();
			//console.log(bscList);
			var bscHtml = "";

			for(var i=0; i<bscList.length; i++) {
				bscHtml += "<li><span class='bscCls' id='"+bscList[i]+"'>" + bscList[i] + "(小区数量：" 
					+ bscToCells[bscList[i]].length + ")</span><ul id='"+bscList[i]+"'></ul></li>";
			} 
			
			$("#allBscCell").html(bscHtml);
			$("#allBscCell").treeview({
				collapsed : true
			});

			$("span.bscCls").on("click", function() {
				var bsc = $(this).attr("id");
				var cells = bscToCells[bsc];
				var cellsHtml = "";
				var all;
				var part;
				for(var i=0; i<cells.length; i++) {
					all = cells[i]['LABEL'] + "(" + cells[i]['NAME'] + ")";
					part = all.length>12?all.substring(0,12)+"...":all;
					cellsHtml += "<li><span class='cellCls' data='" + cells[i]['LABEL'] + "'" 
							+ " bsc='" + bsc + "'" 
							+ " manufacturers='"+cells[i]['MANUFACTURERS']+"' " 
							+ " all='"+all+"' " 
							+ " part='"+part+"'>" 
							+ part +"</span></li>";
				}
				$("ul#"+bsc).html(cellsHtml);
			});
			//图标事件
			$("#allBscCell").find("div").bind("click", function(event) {
				var bsc = $(this).parent().find("span").attr("id");
				var cells = bscToCells[bsc];
				var cellsHtml = "";
				var all;
				var part;
				for(var i=0; i<cells.length; i++) {
					all = cells[i]['LABEL'] + "(" + cells[i]['NAME'] + ")";
					part = all.length>12?all.substring(0,12)+"...":all;
					cellsHtml += "<li><span class='cellCls' data='" + cells[i]['LABEL'] + "'" 
							+ " bsc='" + bsc + "'" 
							+ " manufacturers='"+cells[i]['MANUFACTURERS']+"' " 
							+ " all='"+all+"' " 
							+ " part='"+part+"'>" 
							+ part +"</span></li>";
				}
				$("ul#"+bsc).html(cellsHtml);
            });
		},
		complete:function(){
			$(".loading_cover").css("display","none");
		}
	})
}

/**
 * 
 * @title 获取包含有单小区的ni的信息
 * @param cell
 * @param chartDiv
 * @author chao.xj
 * @date 2016年3月29日下午4:58:28
 * @company 怡创科技
 * @version 1.2
 */
function getSingleCellNiList(cell,chartDiv) {
	// cell = 'S3ASDS1';
	if(cell==null || cell==""){
		animateInAndOut("operInfo", 500, 500, 2000, "operTip", "请先选择小区数据！");
		return;
	}
	showOperTips("loadingDataDiv", "tipcontentId", "小区指标数据");
	selectCell = cell;
	// alert(cell);

	var cityId = $("#cityId1").find("option:selected").val();
	var mrrMeaBegDate=$("#mrrMeaBegDate").val();
	var mrrMeaEndDate=$("#mrrMeaEndDate").val();
	var mrrDateType=$("#mrrDateType").find("option:selected").text();
	var mrrChgr=$("#mrrChgr").find("option:selected").text();
	var mrrChartType=$("#mrrChartType").find("option:selected").val();
	var mrrDisMode=$("#mrrDisMode").find("option:selected").text();
	$.ajax({
		url : 'search4GNiCellIndexForAjaxAction',
		data : {
			'attachParams.cell' : cell,
			'attachParams.cityId' : cityId,
			'attachParams.meaBegTime' : mrrMeaBegDate,
			'attachParams.meaEndTime' : mrrMeaEndDate,
			'attachParams.dataType' : mrrDateType,
			'attachParams.chartType' : mrrChartType,
			'attachParams.disMode' : mrrDisMode,
			'attachParams.chgr' : mrrChgr
		},
		dataType : 'text',
		type : 'post',
		success : function(raw) {
//			console.log("raw:"+raw);
			var data = eval("(" + raw + ")");
			data =data['data'];
			var obj;
			obj=data[0];
			cellIndexObj=obj;
				
			//generateCellIndexChart(obj,chartDiv,"line",mrrDateType,mrrChartType);
			obj = $("#date_msg").val();
			generateSingleCellIndexChart(selectCell,obj,chartDiv,"line")
			showOperTips("loadingDataDiv", "tipcontentId", "数据成功展现");
		},
		complete:function(){
			hideOperTips("loadingDataDiv");
			}
	})
	/*sleep(5000);
	obj = $("#date_msg").val();
	generateSingleCellIndexChart(cell,obj,chartDiv,"line")
	showOperTips("loadingDataDiv", "tipcontentId", "数据成功展现");
	hideOperTips("loadingDataDiv");*/
}
/**
 * 
 * @title 获取包含有多小区的ni的信息
 * @param cell
 * @param chartDiv
 * @author chao.xj
 * @date 2016年3月29日下午4:57:19
 * @company 怡创科技
 * @version 1.2
 */
function getMultiCellNiList(cell,chartDiv) {
	// cell = 'S3ASDS1';
	var cells = $("#cell_msg").val();
	if(cells==null || cells==""){
		animateInAndOut("operInfo", 500, 500, 2000, "operTip", "请先选择小区数据！");
		return;
	}
	showOperTips("loadingDataDiv", "tipcontentId", "小区指标数据");
	//selectCell = cell;
	// alert(cell);

	var cityId = $("#cityId1").find("option:selected").val();
	var mrrMeaBegDate=$("#mrrMeaBegDate").val();
	var mrrMeaEndDate=$("#mrrMeaEndDate").val();
	var mrrDateType=$("#mrrDateType").find("option:selected").text();
	var mrrChgr=$("#mrrChgr").find("option:selected").text();
	var mrrChartType=$("#mrrChartType").find("option:selected").val();
	var mrrDisMode=$("#mrrDisMode").find("option:selected").text();
	$.ajax({
		url : 'search4GNiCellIndexForAjaxAction',
		data : {
			'attachParams.cell' : cell,
			'attachParams.cityId' : cityId,
			'attachParams.meaBegTime' : mrrMeaBegDate,
			'attachParams.meaEndTime' : mrrMeaEndDate,
			'attachParams.dataType' : mrrDateType,
			'attachParams.chartType' : mrrChartType,
			'attachParams.disMode' : mrrDisMode,
			'attachParams.chgr' : mrrChgr
		},
		dataType : 'text',
		type : 'post',
		success : function(raw) {
//			console.log("raw:"+raw);
			var data = eval("(" + raw + ")");
			data =data['data'];
			var obj;
			obj=data[0];
			cellIndexObj=obj;
			//generateCellIndexChart(obj,chartDiv,"line",mrrDateType,mrrChartType);
			
			/*if (obj=="" || obj == null || obj == undefined) {
				defaultCellIndexChart(obj,"chartDiv",false);
			}else{*/
				generateMultiCellIndexChart(cells,chartDiv,"line");
				showOperTips("loadingDataDiv", "tipcontentId", "数据成功展现");
//			}
		},
		complete:function(){
			hideOperTips("loadingDataDiv");
			}
	})
}

/**
 * 
 * @title 生成动态单小区多时段指标图表
 * @param data
 * @param chartDiv
 * @param disMode
 * @author chao.xj
 * @date 2016年3月30日上午10:44:40
 * @company 怡创科技
 * @version 1.2
 */
function generateSingleCellIndexChart(cell,data,chartDiv,disMode) {
	
	if (data=="" || data == null || data == undefined) {
		animateInAndOut("operInfo", 500, 500, 2000, "operTip", "未选择时间段信息！");
		return;
	}
//	console.log(data);
	var obj =  data.split("	");
	var datenum = obj.length;
//	console.log(obj[0]);
//	console.log(dataType+"---data:"+data['CELL_NAME']);
	var dom=document.getElementById(chartDiv);
//	dom=$("#"+chartDiv )[0];从jquery至dom对象
	var UL=new Array();//上行有序数组
	var DL=new Array();//下行有序数组
//	if(dataType.substring(0,dataType.lastIndexOf("_"))=='Rxlev'){
	var boundGap=false;
	
	var title=cell+"多时间段图表";//图表子标题
	var fieldUL="";//上行字段前缀
	var fieldDL="";//下行字段前缀
	var serieNameUL=obj[0];//上行系统名
	var serieNameDL=obj[1];//下行系统名
	var xAxis_name="小区RB上行平均干扰电平";//X轴名称
	var accumuValUL=0;//上行累积值
	var accumuValDL=0;//下行累积值
	var axis =new Array();
	axis = [0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87,88,89,90,91,92,93,94,95,96,97,98,99];		
	var cnt;
	
	var legend_str="";
	var series_str="";
	var legend_arr = new Array();
	var series_arr = new Array();
	/*var max = -88;
	var min = -100;*/
	var max = Math.max.apply(null,alldata);
	var min = Math.min.apply(null,alldata);
	for(var j = 0 ; j < datenum; j++){
//		legend_str += obj[j]+",";
		legend_arr.push(obj[j]);
		
		var dataArr;
		if(j<4 || j==4){
			
			dataArr = zhanyunshaojiang[j];
			
		}else{
			
			dataArr = getRandomNumArr(-92,-99,100);
		}
		var seriesObj ={
		          name:obj[j],								
		          type:disMode,                                 
		          data:dataArr.map(function (item) {
		        	  	if(hasDot(item)){
		        	  		item = item.toFixed(2);
		        	  	}
		                return item;
		            }),          
		          markPoint : {                                 
		              data : [                                  
		                  {type : 'max', name: '最大值'},       
		                  {type : 'min', name: '最小值'}        
		              ]
		          },                                            
		          markLine : {                                  
		              data : [                                  
		                  {type : 'average', name: '平均值'}    
		              ]                                         
		          },                                            
		          itemStyle :{normal :{color:getRandomColor()}}        
		      };
		series_arr.push(seriesObj);
		
	}
	
	require(
	        [
	            'echarts',
	            'echarts/chart/line',   // 按需加载所需图表，如需动态类型切换功能，别忘了同时加载相应图表
	            'echarts/chart/bar'
	        ],
	        function (ec) {
	            var myChart = ec.init(dom);
	            
	            myChart.showLoading({
	                text : "图表数据正在努力加载..."
	            });
	            //信道组号0的接收电平分布图
	            var option = {
	            		title : {
	            	        text: 'RB的干扰波形指标',
	            	        subtext: title,
	            			x: "center", //标题水平方向位置
	            			subtextStyle:{color: '#4A4AFF'} 
	            	    },
	            	    tooltip : {
	            	        trigger: 'axis'
	            	    },
	            	    legend: {
	            	    	orient:'horizontal',
	            	        data:legend_arr ,
	            	        y	: 'bottom',
	            	        x 	: 'center',
	            	        itemWidth:10,
	            	        itemHeight:20,
	            	        formatter:function(val){
	            	            return (serieNameDL==""?val.split("").join("\n"):val);
	            	        },
	            	        //padding: (serieNameDL==""?[5, 5, 5, 5]:0)
	            	        padding:5
	            	    },
	            	    toolbox: {
	            	        show : true,
	            	        feature : {
	            	            mark : {show: (chartDiv=="fullScreenChart"?false:true)},
	            	            dataView : {show: (chartDiv=="fullScreenChart"?false:true), readOnly: false},
	            	            magicType : {show: (chartDiv=="fullScreenChart"?false:true), type: ['line', 'bar']},
	            	            restore : {show: (chartDiv=="fullScreenChart"?false:true)},
	            	            saveAsImage : {show: (chartDiv=="fullScreenChart"?false:true)},
	            	            myCloseTool : {
	            	                show : (chartDiv=="fullScreenChart"?true:false),
	            	                title : '关闭',
	            	                icon : 'image://../rno/jslib/echarts/close1.png',
	            	                onclick : function (){
	            	                	$("#fullScreenChart").css('display','none');
	            	                }
	            	            },
	            	            myFullTool : {
	            	                show : (chartDiv=="fullScreenChart"?false:true),
	            	                title : '全屏',
	            	                icon : 'image://../rno/jslib/echarts/full1.png',
	            	                onclick : function (){
	            	                	clickFullScreen();
	            	                }
	            	            }
	            	        }
	            	    },
	            	    calculable : true,
	            	    xAxis : [
	            	        {
	            	        	name:xAxis_name,
	            	            type : 'category',
	            	            boundaryGap : boundGap,
	            	            data : axis,
	            	            splitLine:{lineStyle:{type: 'dashed'}},
	            	            axisLine:{lineStyle:{color: '#223434'}},
	            	            axisTick:{show:true,length:5}
	            	        }
	            	    ],
	            	    yAxis : [
	            	        {
	            	        	name:'PRB的数值',
	            	            type : 'value',
	            	            axisLabel : {
	            	                formatter: '{value}'
	            	            },
	            	            splitNumber: 6,
	            	            splitLine:{lineStyle:{type: 'dashed'}},
	            	            axisLine:{lineStyle:{color: '#223434'}},
	            	            max:max,
	            	            min:min
	            	        }
	            	    ],
	            	    //112,56, 233, 343, 454, 89, 343, 123, 66, 123
	            	    series : series_arr
	            }
	            myChart.hideLoading();
	            myChart.setOption(option);
	            if(chartDiv=="fullScreenChart"){
	            	$("#fullScreenChart").css("background","white");
	            }
	        }
	    );
}
/**
 * 
 * @title 生成动态多小区时段指标图表
 * @param data
 * @param chartDiv
 * @param disMode
 * @author chao.xj
 * @date 2016年3月30日上午10:44:40
 * @company 怡创科技
 * @version 1.2
 */
function generateMultiCellIndexChart(data,chartDiv,disMode) {
	
	if (data == null || data == undefined) {
		animateInAndOut("operInfo", 500, 500, 2000, "operTip", "未选择小区数据信息！");
		return;
	}
	var obj =  data.split("	");
	var cellnum = obj.length;
	var dom=document.getElementById(chartDiv);
//	dom=$("#"+chartDiv )[0];从jquery至dom对象
	var UL=new Array();//上行有序数组
	var DL=new Array();//下行有序数组
//	if(dataType.substring(0,dataType.lastIndexOf("_"))=='Rxlev'){
	var boundGap=false;
	
	var title="多小区对比图表";//图表子标题
	var fieldUL="";//上行字段前缀
	var fieldDL="";//下行字段前缀
	var serieNameUL=obj[0];//上行系统名
	var serieNameDL=obj[1];//下行系统名
	var xAxis_name="小区RB上行平均干扰电平";//X轴名称
	var accumuValUL=0;//上行累积值
	var accumuValDL=0;//下行累积值
	var axis =new Array();
	axis = [0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87,88,89,90,91,92,93,94,95,96,97,98,99];		
	var cnt;
	
	var legend_str="";
	var series_str="";
	var legend_arr = new Array();
	var series_arr = new Array();
/*	var max = -88;
	var min = -100;*/
	var max = Math.max.apply(null,alldata);
	var min = Math.min.apply(null,alldata);
	for(var j = 0 ; j < cellnum; j++){
//		legend_str += obj[j]+",";
		legend_arr.push(obj[j]);
		
		var dataArr;
		if(j<4 || j==4){
			
			dataArr = zhanyunshaojiang[j];
			
		}else{
			
			dataArr = getRandomNumArr(-92,-99,100);
		}
		var seriesObj ={
		          name:obj[j],								
		          type:disMode,                                 
		          data:dataArr.map(function (item) {
		        	  	if(hasDot(item)){
		        	  		item = item.toFixed(2);
		        	  	}
		                return item;
		            }),          
		          markPoint : {                                 
		              data : [                                  
		                  {type : 'max', name: '最大值'},       
		                  {type : 'min', name: '最小值'}        
		              ]                                         
		          },                                            
		          markLine : {                                  
		              data : [                                  
		                  {type : 'average', name: '平均值'}    
		              ]                                         
		          },                                            
		          itemStyle :{normal :{color:getRandomColor()}}        
		      };
		series_arr.push(seriesObj);
		
	}
	
	require(
	        [
	            'echarts',
	            'echarts/chart/line',   // 按需加载所需图表，如需动态类型切换功能，别忘了同时加载相应图表
	            'echarts/chart/bar'
	        ],
	        function (ec) {
	            var myChart = ec.init(dom);
	            
	            myChart.showLoading({
	                text : "图表数据正在努力加载..."
	            });
	            //信道组号0的接收电平分布图
	            var option = {
	            		title : {
	            	        text: 'RB的干扰波形指标',
	            	        subtext: title,
	            			x: "center", //标题水平方向位置
	            			subtextStyle:{color: '#4A4AFF'} 
	            	    },
	            	    tooltip : {
	            	        trigger: 'axis'
	            	    },
	            	    legend: {
	            	    	orient:'horizontal',
	            	        data:legend_arr ,
	            	        y	: 'bottom',
	            	        x 	: 'center',
	            	        itemWidth:10,
	            	        itemHeight:20,
	            	        formatter:function(val){
	            	            return (serieNameDL==""?val.split("").join("\n"):val);
	            	        },
	            	        //padding: (serieNameDL==""?[5, 5, 5, 5]:0)
	            	        padding:5
	            	    },
	            	    toolbox: {
	            	        show : true,
	            	        feature : {
	            	            mark : {show: (chartDiv=="fullScreenChart"?false:true)},
	            	            dataView : {show: (chartDiv=="fullScreenChart"?false:true), readOnly: false},
	            	            magicType : {show: (chartDiv=="fullScreenChart"?false:true), type: ['line', 'bar']},
	            	            restore : {show: (chartDiv=="fullScreenChart"?false:true)},
	            	            saveAsImage : {show: (chartDiv=="fullScreenChart"?false:true)},
	            	            myCloseTool : {
	            	                show : (chartDiv=="fullScreenChart"?true:false),
	            	                title : '关闭',
	            	                icon : 'image://../rno/jslib/echarts/close1.png',
	            	                onclick : function (){
	            	                	$("#fullScreenChart").css('display','none');
	            	                }
	            	            },
	            	            myFullTool : {
	            	                show : (chartDiv=="fullScreenChart"?false:true),
	            	                title : '全屏',
	            	                icon : 'image://../rno/jslib/echarts/full1.png',
	            	                onclick : function (){
	            	                	clickFullScreen();
	            	                }
	            	            }
	            	        }
	            	    },
	            	    calculable : true,
	            	    xAxis : [
	            	        {
	            	        	name:xAxis_name,
	            	            type : 'category',
	            	            boundaryGap : boundGap,
	            	            data : axis,
	            	            splitLine:{lineStyle:{type: 'dashed'}},
	            	            axisLine:{lineStyle:{color: '#223434'}},
	            	            axisTick:{show:true,length:5}
	            	        }
	            	    ],
	            	    yAxis : [
	            	        {
	            	        	name:'PRB的数值',
	            	            type : 'value',
	            	            axisLabel : {
	            	                formatter: '{value}'
	            	            },
	            	            splitNumber: 6,
	            	            splitLine:{lineStyle:{type: 'dashed'}},
	            	            axisLine:{lineStyle:{color: '#223434'}},
	            	            max:max,
	            	            min:min
	            	        }
	            	    ],
	            	    //112,56, 233, 343, 454, 89, 343, 123, 66, 123
	            	    series : series_arr
	            }
	            myChart.hideLoading();
	            myChart.setOption(option);
	            if(chartDiv=="fullScreenChart"){
	            	$("#fullScreenChart").css("background","white");
	            }
	        }
	    );

}
//获取某个区间的随机数据
function getRandomNum(maxNum,minNum){
	
	var cnt = Math.round((minNum+Math.random()*(maxNum-(minNum)+1)));
	return cnt;
}
/**
 * 获取某区间的随机数组
 */
function getRandomNumArr(maxNum,minNum,size){
	var UL = new Array();
	var cnt;
	for (var i = 0; i < size; i++) {
		cnt = getRandomNum(maxNum,minNum);
		UL.push(cnt);
	}
	return UL;
}
/**
 * 获取十六进制的随机颜色
 */
var getRandomColor = function(){   
	  return  '#' +   
	    (function(color){   
	    return (color +=  '0123456789abcdef'[Math.floor(Math.random()*16)])   
	      && (color.length == 6) ?  color : arguments.callee(color);   
	  })('');   
	} 
/**
 * 
 * @title 休眠，阻塞程序继续运行若干秒后苏醒
 * @param numberMillis
 * @author chao.xj
 * @date 2016年3月30日下午6:01:36
 * @company 怡创科技
 * @version 1.2
 */
function sleep(numberMillis) { 
	var now = new Date(); 
	var exitTime = now.getTime() + numberMillis; 
	while (true) { 
	now = new Date(); 
	if (now.getTime() > exitTime) 
	return; 
	} 
	}
/**
 * 
 * @title 是否包含逗号
 * @param num
 * @returns
 * @author chao.xj
 * @date 2016年4月7日下午4:02:11
 * @company 怡创科技
 * @version 1.2
 */
function hasDot(num) {
    if (!isNaN(num)) {
        return ((num + '').indexOf('.') != -1) ? true : false;
    }
}
var alldata = [-86.5,-90.25,-91.75,-92.25,-93.5,-94.5,-96.25,-97,-98,-99,-99.75,-100.25,-101.75,-103,-104,-104.5,-106,-106.25,-107,-108,-108,-108.75,-108.75,-109.5,-109.75,-110.5,-110.5,-110.5,-110.5,-110.75,-111.5,-111.5,-112.25,-112.25,-112.5,-112.5,-112.5,-113.25,-113.5,-113.5,-113.75,-113.75,-114,-114,-114.5,-114.5,-114.75,-114.75,-114.75,-114.5,-114.75,-114.75,-114.75,-114.75,-114.75,-114.75,-114.75,-114.75,-114.75,-114.75,-114.75,-114.75,-114.75,-114.75,-114.75,-114.75,-114.75,-114.75,-115,-114.75,-114.75,-114.75,-114.75,-114.75,-114.25,-114.75,-114.75,-114.75,-115,-115,-115,-114.75,-115,-115,-114.75,-114.75,-114.75,-114.25,-114.5,-115,-115,-115,-114.75,-114.5,-114.25,-114.5,-115,-115,-114.25,-110.25,-110,-109.25,-109.5,-107,-107.5,-108.5,-109.25,-106.25,-95.75,-95.25,-98.5,-98.25,-104,-109.25,-108,-106.5,-106,-101.25,-101,-101.25,-101.25,-104,-103,-101.25,-97,-97.5,-99.75,-103.75,-106.75,-108,-102,-101.25,-100.25,-95.75,-98,-105.5,-107.5,-109.25,-105.25,-106,-106.25,-88.25,-86.75,-98,-107.5,-109.5,-109,-106.25,-99.5,-100.5,-108.25,-108,-107.5,-109.5,-108.25,-93.25,-87,-90,-106,-104.75,-104.25,-100.5,-97.75,-104.5,-105.75,-107.75,-107.5,-108,-107,-97.5,-94.75,-90,-87.5,-98.25,-97,-106,-104.25,-103.5,-92.25,-93,-91.75,-90.5,-101.5,-105.25,-101.75,-102.75,-93.25,-98.5,-104.5,-105.25,-107.5,-108.25,-102.25,-98.25,-103,-96.5,-102,-109,-108,-109,-111.675806,-111.675806,-111.675806,-111.675806,-111.675806,-111.675806,-111.675806,-111.675806,-106.093212,-105.963296,-105.926039,-105.877002,-105.911459,-106.023362,-106.104998,-106.082538,-105.91816,-105.792004,-105.882766,-106.080086,-106.168088,-106.084347,-105.936003,-105.847055,-105.78329,-105.798508,-105.889438,-106.002416,-106.022177,-105.949256,-105.89888,-105.892008,-105.96213,-106.064831,-106.152086,-106.154804,-106.114891,-106.103654,-106.150563,-106.151727,-106.149,-106.138526,-106.09308,-105.995533,-105.949688,-106.024378,-106.046009,-105.938059,-105.820193,-105.650208,-105.698983,-105.88515,-105.927011,-105.920782,-105.983997,-105.962882,-105.845807,-105.745014,-105.829567,-106.036938,-106.132386,-106.036164,-105.857734,-105.82083,-105.920161,-106.046853,-106.148214,-106.159189,-106.069731,-105.920681,-105.802467,-105.814086,-105.88936,-106.030812,-106.088244,-106.060596,-105.958343,-106.020762,-106.243912,-106.407377,-106.313678,-106.118616,-106.019651,-106.011799,-106.049243,-106.145146,-106.154397,-106.172506,-106.175818,-106.053196,-106.014608,-106.019923,-111.675806,-111.675806,-111.675806,-111.675806,-111.675806,-111.675806,-111.675806,-111.675806,-79,-82.666667,-83.666667,-86,-83,-81.333333,-82.333333,-84,-83.333333,-82.333333,-79,-79.666667,-82.333333,-82.666667,-81.666667,-82.666667,-84.666667,-84.666667,-83.666667,-84,-85.666667,-85.666667,-85.333333,-85.333333,-85.666667,-86,-86.333333,-86.333333,-86.666667,-86.666667,-86.666667,-87,-87,-85.666667,-85.666667,-86.333333,-86.333333,-86.666667,-87,-87.333333,-87.333333,-87.333333,-87.333333,-87.333333,-87.333333,-87.333333,-87.333333,-87.333333,-87.333333,-87.333333,-87.333333,-87.333333,-87.333333,-87.333333,-87.333333,-87.333333,-87.333333,-87.333333,-87.333333,-87.333333,-87.333333,-87.333333,-87.333333,-87.333333,-87.333333,-86.666667,-86.666667,-87.333333,-87.333333,-87,-87.333333,-87,-87,-87,-87,-86.666667,-87,-87,-86.666667,-86.666667,-86,-85.333333,-86,-86,-85,-84,-85,-85,-83.333333,-82.666667,-85,-85,-85.333333,-84.333333,-82.666667,-84,-86,-84.333333,-82.333333,-79,-89,-90,-83.666667,-79,-76,-81.333333,-82.333333,-84,-83.333333,-82.333333,-90,-95,-98,-100,-108,-98,-100,-108,-111,-120,-118,-116,-114,-120,-118,-116,-118,-116,-117,-121,-118,-116,-117,-121,-116,-114,-120,-118,-121,-118,-116,-117,-121,-114,-120,-118,-113,-118,-113,-114,-113,-116,-113,-121,-113,-116,-117,-116,-114,-120,-114,-121,-121,-121,-115,-121,-112,-114,-114,-115,-112,-121,-121,-121,-114,-121,-114,-114,-121,-115,-112,-121,-115,-112,-112,-121,-115,-121,-115,-112,-115,-121,-115,-121,-121,-121,-115,-121,-115,-121];
var zhanjiang = [-86.5,-90.25,-91.75,-92.25,-93.5,-94.5,-96.25,-97,-98,-99,-99.75,-100.25,-101.75,-103,-104,-104.5,-106,-106.25,-107,-108,-108,-108.75,-108.75,-109.5,-109.75,-110.5,-110.5,-110.5,-110.5,-110.75,-111.5,-111.5,-112.25,-112.25,-112.5,-112.5,-112.5,-113.25,-113.5,-113.5,-113.75,-113.75,-114,-114,-114.5,-114.5,-114.75,-114.75,-114.75,-114.5,-114.75,-114.75,-114.75,-114.75,-114.75,-114.75,-114.75,-114.75,-114.75,-114.75,-114.75,-114.75,-114.75,-114.75,-114.75,-114.75,-114.75,-114.75,-115,-114.75,-114.75,-114.75,-114.75,-114.75,-114.25,-114.75,-114.75,-114.75,-115,-115,-115,-114.75,-115,-115,-114.75,-114.75,-114.75,-114.25,-114.5,-115,-115,-115,-114.75,-114.5,-114.25,-114.5,-115,-115,-114.25,-110.25];
var yunfu = [-110,-109.25,-109.5,-107,-107.5,-108.5,-109.25,-106.25,-95.75,-95.25,-98.5,-98.25,-104,-109.25,-108,-106.5,-106,-101.25,-101,-101.25,-101.25,-104,-103,-101.25,-97,-97.5,-99.75,-103.75,-106.75,-108,-102,-101.25,-100.25,-95.75,-98,-105.5,-107.5,-109.25,-105.25,-106,-106.25,-88.25,-86.75,-98,-107.5,-109.5,-109,-106.25,-99.5,-100.5,-108.25,-108,-107.5,-109.5,-108.25,-93.25,-87,-90,-106,-104.75,-104.25,-100.5,-97.75,-104.5,-105.75,-107.75,-107.5,-108,-107,-97.5,-94.75,-90,-87.5,-98.25,-97,-106,-104.25,-103.5,-92.25,-93,-91.75,-90.5,-101.5,-105.25,-101.75,-102.75,-93.25,-98.5,-104.5,-105.25,-107.5,-108.25,-102.25,-98.25,-103,-96.5,-102,-109,-108,-109];
var shaoguang = [-111.675806,-111.675806,-111.675806,-111.675806,-111.675806,-111.675806,-111.675806,-111.675806,-106.093212,-105.963296,-105.926039,-105.877002,-105.911459,-106.023362,-106.104998,-106.082538,-105.91816,-105.792004,-105.882766,-106.080086,-106.168088,-106.084347,-105.936003,-105.847055,-105.78329,-105.798508,-105.889438,-106.002416,-106.022177,-105.949256,-105.89888,-105.892008,-105.96213,-106.064831,-106.152086,-106.154804,-106.114891,-106.103654,-106.150563,-106.151727,-106.149,-106.138526,-106.09308,-105.995533,-105.949688,-106.024378,-106.046009,-105.938059,-105.820193,-105.650208,-105.698983,-105.88515,-105.927011,-105.920782,-105.983997,-105.962882,-105.845807,-105.745014,-105.829567,-106.036938,-106.132386,-106.036164,-105.857734,-105.82083,-105.920161,-106.046853,-106.148214,-106.159189,-106.069731,-105.920681,-105.802467,-105.814086,-105.88936,-106.030812,-106.088244,-106.060596,-105.958343,-106.020762,-106.243912,-106.407377,-106.313678,-106.118616,-106.019651,-106.011799,-106.049243,-106.145146,-106.154397,-106.172506,-106.175818,-106.053196,-106.014608,-106.019923,-111.675806,-111.675806,-111.675806,-111.675806,-111.675806,-111.675806,-111.675806,-111.675806];
var jiangmen = [-79,-82.666667,-83.666667,-86,-83,-81.333333,-82.333333,-84,-83.333333,-82.333333,-79,-79.666667,-82.333333,-82.666667,-81.666667,-82.666667,-84.666667,-84.666667,-83.666667,-84,-85.666667,-85.666667,-85.333333,-85.333333,-85.666667,-86,-86.333333,-86.333333,-86.666667,-86.666667,-86.666667,-87,-87,-85.666667,-85.666667,-86.333333,-86.333333,-86.666667,-87,-87.333333,-87.333333,-87.333333,-87.333333,-87.333333,-87.333333,-87.333333,-87.333333,-87.333333,-87.333333,-87.333333,-87.333333,-87.333333,-87.333333,-87.333333,-87.333333,-87.333333,-87.333333,-87.333333,-87.333333,-87.333333,-87.333333,-87.333333,-87.333333,-87.333333,-87.333333,-86.666667,-86.666667,-87.333333,-87.333333,-87,-87.333333,-87,-87,-87,-87,-86.666667,-87,-87,-86.666667,-86.666667,-86,-85.333333,-86,-86,-85,-84,-85,-85,-83.333333,-82.666667,-85,-85,-85.333333,-84.333333,-82.666667,-84,-86,-84.333333,-82.333333,-79];
var jiangmen2 = [-89,-90,-83.666667,-79,-76,-81.333333,-82.333333,-84,-83.333333,-82.333333,-90,-95,-98,-100,-108,-98,-100,-108,-111,-120,-118,-116,-114,-120,-118,-116,-118,-116,-117,-121,-118,-116,-117,-121,-116,-114,-120,-118,-121,-118,-116,-117,-121,-114,-120,-118,-113,-118,-113,-114,-113,-116,-113,-121,-113,-116,-117,-116,-114,-120,-114,-121,-121,-121,-115,-121,-112,-114,-114,-115,-112,-121,-121,-121,-114,-121,-114,-114,-121,-115,-112,-121,-115,-112,-112,-121,-115,-121,-115,-112,-115,-121,-115,-121,-121,-121,-115,-121,-115,-121];
var zhanyunshaojiang = [zhanjiang,yunfu,shaoguang,jiangmen,jiangmen2];

