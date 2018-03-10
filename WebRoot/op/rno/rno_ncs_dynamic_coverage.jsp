<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>
<%@ taglib uri="/struts-tags" prefix="s"%>
<%
	String path = request.getContextPath();
	String basePath = request.getScheme() + "://"
			+ request.getServerName() + ":" + request.getServerPort()
			+ path + "/";
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"><html>
<head>

<title>小区动态覆盖展示</title>

<meta http-equiv="pragma" content="no-cache">
<meta http-equiv="cache-control" content="no-cache">
<meta http-equiv="expires" content="0">
<meta http-equiv="keywords" content="keyword1,keyword2,keyword3">

<%@include file="commonheader.jsp"%>
<link rel="stylesheet" href="jslib/farbtastic/farbtastic.css"
	type="text/css" />
	
<script type="text/javascript" src="js/rno_ncs_dynamic_coverage.js"></script>
<script type="text/javascript" src="js/trafficstaticsView.js"></script>
<script type="text/javascript" src="jslib/farbtastic/farbtastic.js"></script>
<script type="text/javascript" src="js/cityMapGrid.js"></script>
<script type="text/javascript" src="jslib/lib_mapgrid.js"></script>
<style type="text/css">
html,body {
	width: 100%;
	height: 100%;
	margin: 0px;
	background: #EEEEEE;
}

#map_canvas {
	width: 100%;
	height: 100%;
	position: absolute;
	z-index: 0;
}

.interferDialog {
	background: #99BBE8;
	border: 1px solid #99BBE8;
	border-radius: 5px 5px 5px 5px;
	font-size: 12px;
	padding: 5px;
	position: absolute;
	z-index: 20;
}

.interferDialog_header {
	color: #15428B;
	font-size: 12px;
	font-weight: bold;
	line-height: 15px;
	padding: 2px 0 4px;
	position: relative;
	cursor: move;
}

.interferDialog_title div {
	cursor: pointer;
	display: block;
	float: right;
	height: 30px;
	margin-left: 2px;
	opacity: 0.8;
	width: 16px;
}

.interferDialog_content {
	border: 1px solid #99BBE8;
	background-color: #fff;
	padding: 10px 0;
}

#DialogBody {
	
	border-top: none;
	padding: 0px;
}

.greystyle-standard .greystyle-standard-red-inter td {
	border: 1px solid #CCC;
	padding: 4px;
	padding-left: 6px;
	color: #000;
	line-height: 21px;
	vertical-align: middle;
	background: #7CEFF7;
	height: 20px
} 


.queryButton { 
	background-color: #4A8BF4;
	display: inline-block;
	height: 25px;
	vertical-align: middle;
	width: auto;
	overflow: visible;
	font-size: 12px;
}
.showCellNameButton { 
	background-color: #4A8BF4;
	display: inline-block;
	height: 25px;
	vertical-align: middle;
	width: auto;
	overflow: visible;
	font-size: 12px;
}
</style>
<script type="text/javascript">

var $ = jQuery.noConflict();
$(function() {
	//tab选项卡
	tab("div_tab", "li", "onclick");//项目服务范围类别切换
})

</script>
</head>


<body>

	<div class="loading_cover" id="loadingDataDiv" style="display: none">
		<div class="cover"></div>
		<h4 class="loading">
			 <em class="loading_fb" id="loadContentId"></em>,请稍侯...
		</h4>
	</div>

	<div class="div_left_main" style="width: auto">
		<div class="div_left_content">
		
			<div style="padding-bottom: 0px; padding-top: 0px">
				<div class="map_hd" style="padding-bottom: 0px">
					<div class="head_box clearfix" style="padding-bottom: 0px">
						<div class="dialog2 draggable ui-draggable">
							<div style="padding: 5px">

								<form id="conditionForm" method="post">
									省：<select class="required" id="provinceId">
										<%-- option value="-1">请选择</option --%>
										<s:iterator value="provinceAreas" id="onearea">
											<option value="<s:property value='#onearea.area_id' />">
												<s:property value="#onearea.name" />
											</option>
										</s:iterator>
									</select> 市：<select name="cityId" class="required" id="cityId">
										<s:iterator value="cityAreas" id="onearea">
											<option value="<s:property value='#onearea.area_id' />">
												<s:property value="#onearea.name" />
											</option>
										</s:iterator>
									</select> 区：<select name="areaId" class="required" id="areaId">
										<s:iterator value="countryAreas" id="onearea">
											<option value="<s:property value='#onearea.area_id' />">
												<s:property value="#onearea.name" />
											</option>
										</s:iterator>
										<option value="-1">全部</option>
									</select>
									<input type="button" class="queryButton"  value="打开搜索面板"　style="font-size: 12px"></input>
									 <input  type="button" id="showCellName" class="showCellNameButton" value="显示小区名字"/>
									<span id="loadingCellTip"></span>
									<input  type="button" id="trigger" name="trigger" value=""/>
									<div id="hiddenAreaLngLatDiv" style="display:none">
										<s:iterator value="countryAreas" id="onearea">
											<input type="hidden"
												id="areaid_<s:property value='#onearea.area_id' />"
												value="<s:property value='#onearea.longitude' />,<s:property value='#onearea.latitude' />">
										</s:iterator>
									</div>

									<input type="hidden" id="hiddenZoom" name="zoom" value="16" />
									<input type="hidden" id="hiddenLng"
										value="<s:property value='centerPoint.lng' />" /> <input
										type="hidden" id="hiddenLat"
										value="<s:property value='centerPoint.lat' />" /> <input
										type="hidden" id="hiddenPageSize" name="page.pageSize"
										value="100" /> <input type="hidden" id="hiddenCurrentPage"
										name="page.currentPage" value="1" /> <input type="hidden"
										id="hiddenForcedStartIndex" name="page.forcedStartIndex"
										value="-1" /> <input type="hidden" id="hiddenTotalPageCnt" />
									<input type="hidden" id="hiddenTotalCnt" />
								</form>

							</div>
						</div>
					</div>
				</div>

			</div>

			<%-- 地图--%>
			<div style="padding-top: 0px">
				<div class="map_bd">
					<div id="map_canvas"></div>
					<div class="resource_list_icon" style="right:380px">
						<a href="#" class="switch"></a> <a href="#" class="switch_hidden"></a>
						<div class="shad_v"></div>
					</div>
					<div class="resource_list300_box" style="height:100%;">
						<div class="resource_list300">
							<div id="div_tab" class="div_tab divtab_menu">
								<ul>
									<li class="selected" id="interPlanLi">选择条件</li>
								</ul>
							</div>
						</div>
						<div class="divtab_content" style="overflow-y:auto; overflow-x:auto; width:360px; height:600px">
							<div id="div_tab_0">
								<%-- 标题--%>
								<div class="div_title_24px_blue">
									<span class="sp_title">小区加载</span>
								</div>
								<div style="margin: 3px"></div>
								<input id="loadCellToMap" type="button" value="加载小区信息" name="loadCellToMap" style="margin-right: 0px"/>
								<%-- 标题--%>
								<div class="div_title_24px_blue">
									<span class="sp_title">日期选择</span>
								</div>
								<div style="margin: 3px"></div>
								日期	<input id="sDate" name="sDate"
										value="<s:property value="preFiveDayTime" />" type="text"
										onFocus="WdatePicker({dateFmt:'yyyy-MM-dd HH:mm:ss',onpicked:function(){eDate.focus();},maxDate:'#F{$dp.$D(\'eDate\')}'})"
										readonly class="Wdate input-text" style="width: 132px;" />
								- <s:set name="date2" value="new java.util.Date()" />
									<input id="eDate" name="eDate"
										value="<s:date name="date2" format="yyyy-MM-dd HH:mm:ss" />" type="text"
										onFocus="WdatePicker({dateFmt:'yyyy-MM-dd HH:mm:ss',minDate:'#F{$dp.$D(\'sDate\')}'})"
										readonly class="Wdate input-text" style="width: 132px;" />
																<%-- 标题--%>
								<div class="div_title_24px_blue">
									<span class="sp_title">图形系数</span>
								</div>
								<div style="margin: 3px"></div>
								折线图形状系数&nbsp;<input type="text" id="imgCoeff" value="0.2"/> 
								<br/>(默认值0.2，输入数字且k大于0小于0.5)

								<hr>
								<input id="clearCoverPolygon" type="button" value="清除覆盖图" name="clearCoverPolygon" style="margin-right: 0px"/>
							
							</div>

						</div>
					</div>

					<%-- 地图高宽--%>
					<div class="htl_map_move">
						<div>
							<iframe frameborder="none" src=""
								style="border: medium none; width: 1600px; height: 650px;">
							</iframe>
						</div>
					</div>
				</div>

			</div>
			
			<%-- 小区查找窗口 --%>
			<div id="searchDiv" class="dialog2 draggable"
				style="display:none; left: 55px; top: 74px;">
				<div class="dialog_header">
					<div class="dialog_title">小区查找</div>
					<div class="dialog_tool">
						<div class="dialog_tool_close dialog_closeBtn"
							onclick="$('#searchDiv').hide();"></div>
					</div>
				</div>
				<div class="dialog_content"
					style="background:#f9f9f9;padding:10px">
					搜索条件： <select id="conditionType">
						<option value="cell">CELL</option>
						<option value="chineseName">小区中文名</option>
						<option value="site">SITE</option>
						<option value="lac">LAC</option>
						<option value="ci">CI</option>
					</select> 
					<input type="text" value="" id="conditionValue" /> 
					<input type="button" id="searchCellBtn" value="搜小区" /><br /> 
					输入邻区：<input type="text" id="cellForNcell" />
					<input type="button" id="searchNcellBtn" value="搜邻区" /> <br /> 
					输入频点： <input type="text" id="freqValue" />
					<input type="button" id="searchFreqBtn" value="搜频点" /> <br /><br /> 
					<input type="button" value="清除搜索结果" id="clearSearchResultBtn" />
					<span style="color:red;width:100px;font-family:华文中宋;text-align:center" id="errorDiv"></span>
				</div>
			</div>
			
			<div id="operInfo"
				style="display:none; top:40px;left:600px;z-index:999;width:400px; height:40px; background-color:#7dff3f; filter:alpha(Opacity=80);-moz-opacity:0.5;opacity: 0.5;z-index:9999;position: fixed;">
				<table height="100%" width="100%" style="text-align:center">
					<tr>
						<td><span id="operTip"></span></td>
					</tr>
				</table>

			</div>
</body>
</html>
