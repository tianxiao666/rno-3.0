<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>
<%@ taglib uri="/struts-tags" prefix="s"%>
<%
	String path = request.getContextPath();
	String basePath = request.getScheme() + "://"
			+ request.getServerName() + ":" + request.getServerPort()
			+ path + "/";
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>

<title>小区信息管理</title>

<meta http-equiv="pragma" content="no-cache">
<meta http-equiv="cache-control" content="no-cache">
<meta http-equiv="expires" content="0">
<meta http-equiv="keywords" content="keyword1,keyword2,keyword3">
<meta http-equiv="description" content="This is my page">

<%@include file="commonheader.jsp"%>
<link href="jslib/jquery-ui-1.11.1.custom/jquery-ui.css"
	rel="stylesheet">
<link
	href="jslib/jquery-ui-1.11.1.custom/jquery-ui-timepicker-addon.css"
	rel="stylesheet">
<script src="jslib/jquery-ui-1.11.1.custom/jquery-ui.js"></script>
<script
	src="jslib/jquery-ui-1.11.1.custom/jquery-ui-timepicker-addon.js"></script>
<script type="text/javascript" src="js/cellimport.js"></script>
<script type="text/javascript" src="js/cellmanager.js"></script>
<script type="text/javascript">
	var $ = jQuery.noConflict();
	$(function() {
		//tab选项卡
		tab("div_tab", "li", "onclick");//项目服务范围类别切换

	})
</script>
<style type="text/css">
/* body {
	margin: 50px;
} */
.greystyle-standard-noborder tr td {
	padding: 0
}

.dict_left {
	width: 130px;
	float: left;
	left: 0;
	z-index: 1;
	border: 1px;
	margin: 5px
}

.dict_right {
	width: 100%;
	left: 200px;
	float: left;
	border: 1px;
	margin: 5px
}

/* 覆盖分页面板的对齐样式 */
.paging_div {
	text-align: left;
}

select {
	width: 100px;
}

.sectionDivCls {
	margin-top: 10px;
}

.importTitle {
	background: url("images/ui-bg_glass_65_ffffff_1x400.png") repeat-x
		scroll 50% 50% #ffffff;
	border: 1px solid #fbd850;
	color: #eb8f00;
	font-weight: bold;
	padding-left: 2.2em;
	height: 30px;
	cursor: pointer;
	border-radius: 5px;
}

.importContent {
	background: url("images/ui-bg_highlight-soft_100_eeeeee_1x100.png")
		repeat-x scroll 50% top #eeeeee;
	border: 1px solid #dddddd;
	padding: 1em 2.2em;
	border-radius: 5px;
}

select.areaCls {
	width: 90px;
}
</style>
</head>

<body>
	<div class="loading_cover" style="display: none">
		<div class="cover"></div>
		<h4 class="loading">
			正在加载 <em class="loading_fb">小区</em>资源,请稍侯...
		</h4>
	</div>
	<div class="div_left_main" style="width: 100%">
		<div class="div_left_content">
			<%--  <div class="div_left_top">小区信息管理</div> --%>
			<div style="padding: 10px">
				<div id="frame" style="border: 1px solid #ddd">
					<div id="div_tab" class="div_tab divtab_menu">
						<ul>
							<li class="selected">GSM小区信息导入</li>
							<li>GSM小区信息查询</li>

						</ul>
					</div>

					<div class="divtab_content">
						<div id="div_tab_0">
							<div id="listinfoDiv">
								<form id="searchImportForm">
									<select id="provincemenu">
										<s:iterator value="provinceAreas" id="onearea">
											<option value="<s:property value='#onearea.area_id' />">
												<s:property value="#onearea.name" />
											</option>
										</s:iterator>
									</select> <select id="citymenu" name="attachParams['cityId']">
										<s:iterator value="cityAreas" id="onearea">
											<option value="<s:property value='#onearea.area_id' />">
												<s:property value="#onearea.name" />
											</option>
										</s:iterator>
									</select>
									<div style="margin: 9px"></div>
									<div id="searchImportDiv"
										style="height:153px;background:url('images/ui-bg_glass_65_ffffff_1x400.png') repeat-x scroll 50% 50% #ffffff;border:1px solid #fbd850">
										<div style="margin: 9px"></div>
										<input type="hidden" name="fileStyle" id='fileStyle'
											value="GSM_CELL_FILE" /> <input type="hidden"
											id="hiddenPageSize" name="page.pageSize" value="25" /> <input
											type="hidden" id="hiddenCurrentPage" name="page.currentPage"
											value="1" /> <input type="hidden" id="hiddenTotalPageCnt" />
										<input type="hidden" id="hiddenTotalCnt" value="-1"
											name="page.totalCnt" />
										<table>
											<tr>
												<td style="padding-left:30px;display:none">所选区域:<input
													type="text" id="area1" name="" value="" readonly="readonly" /></td>
												<td style="padding-left:30px"><label>上传时间 从:</label> <input
													type="text" value="" id="begUploadDate"
													name="attachParams['begUploadDate']" /> 至：<input
													type="text"
													value="<s:date name="todaytime" format="yyyy-MM-dd" />"
													id="endUploadDate" name="attachParams['endUploadDate']" /></td>
												<td style="padding-left:30px"><label>状态:</label></td>
												<td><select id="fileStatus"
													name="attachParams['fileStatus']">
														<option value='全部'>全部</option>
														<option value='全部成功'>全部成功</option>
														<option value='成功'>成功</option>
														<option value='全部失败'>全部失败</option>
														<option value='正在解析'>正在解析</option>
														<option value='等待解析'>等待解析</option>
												</select></td>
												<td style="padding-left:30px"><input type="button"
													id="searchImportBtn" value="查询导入记录"></input></td>
											</tr>
										</table>
									</div>
								</form>

								<div style="margin: 9px"></div>

								<div id="importTitleDiv" class="importTitle">
									<div style="margin: 9px"></div>
									导入
								</div>
								<div id="importDiv" class="importContent" style="height:250px">
									<form id="formImportNcs" enctype="multipart/form-data"
										method="post">
										<input type="hidden" name="fileCode" id='fileCode'
											value="GSM_CELL_FILE" /> <input type="hidden" name="token"
											id="token" value="" /> <input type="hidden"
											id="uploadCityId" name="cityId" value="" />
										<div>
											<table class="main-table1 half-width"
												style="margin-left:0;width:50%">
												<tbody>
													<tr style="display:none">
														<td class="menuTd">所选区域<span class="txt-impt">*</span></td>
														<td><input type="text" id="area2" name="" value=""
															readonly="readonly" style="width: 132px;" /></td>
													</tr>
													<tr style="display:none">
														<td class="menuTd">文件日期<span class="txt-impt">*</span>
															<td><s:set name="todaytime"
																	value="new java.util.Date()" /> <input name="meaTime"
																class="required" id='meatime'
																value="<s:date name="todaytime" format="yyyy-MM-dd" />"
																type="text"
																onFocus="WdatePicker({dateFmt:'yyyy-MM-dd'})" readonly
																class="Wdate input-text" style="width: 132px;" /></td>
													</tr>
													<tr>
														<td class="menuTd">GSM工参数据文件(EXCEL)<span
															class="txt-impt">*</span>
														</td>
														<td style="width: 50%; font-weight: bold" colspan="0"><input
															type="file" style="width: 44%;" name="file" id='fileid'
															class="canclear required" /> <span
															style="color:red;width:100px;font-family:华文中宋;text-align:center"
															id="fileDiv"></span> <a
															href="fileDownloadAction?fileName=GSM小区信息导入模板.xlsx"
															title="点击下载模板" id="downloadHref">GSM小区信息导入模板</a></br></td>
													</tr>
												</tbody>

											</table>
										</div>
										<div class="container-bottom" style="padding-top: 10px">
											<table style="width: 60%; margin: auto" align="left">
												<tr>
													<td><input type="button" value="导    入"
														style="width: 90px;" id="importBtn" name="import" /> </br>
														<div id="uploadMsgDiv" style="display:none"></div></td>
												</tr>
											</table>
											<div id="importResultDiv" class="container-bottom"
												style="padding-top: 10px">
												<div id="progressInfoDiv" style="width:50%;display:none">
													<h2 id="progressNum">0%</h2>
													<div id="progressbar"></div>
												</div>
											</div>
										</div>
									</form>
								</div>

								<div id="importListDiv" class="sectionDivCls">
									<bold>文件上传记录列表</bold>
									<%--<a onclick='javascript:toggleImport();'>新增导入</a> --%>
									<table id="importListTab" class="greystyle-standard"
										style="width:auto;margin-left:0">
										<thead>
											<%--<th>地市</th>--%>
											<th>上传日期</th>
											<th>文件名称</th>
											<th>文件大小</th>
											<th>开始时间</th>
											<th>完成时间</th>
											<th>上传账号</th>
											<th>状态</th>
										</thead>
									</table>
									<div class="paging_div" id="ncsImportRecPageDiv"
										style="border: 1px solid #ddd">
										<span class="mr10">共 <em id="emTotalCnt" class="blue">0</em>
											条记录
										</span> <a class="paging_link page-first" title="首页"
											onclick="showListViewByPage('first',queryImportDataRec,'searchImportForm','ncsImportRecPageDiv')"></a>
										<a class="paging_link page-prev" title="上一页"
											onclick="showListViewByPage('back',queryImportDataRec,'searchImportForm','ncsImportRecPageDiv')"></a>
										第 <input type="text" id="showCurrentPage"
											class="paging_input_text" value="0" /> 页/<em
											id="emTotalPageCnt">0</em>页 <a class="paging_link page-go"
											title="GO"
											onclick="showListViewByPage('num',queryImportDataRec,'searchImportForm','ncsImportRecPageDiv')">GO</a>
										<a class="paging_link page-next" title="下一页"
											onclick="showListViewByPage('next',queryImportDataRec,'searchImportForm','ncsImportRecPageDiv')"></a>
										<a class="paging_link page-last" title="末页"
											onclick="showListViewByPage('last',queryImportDataRec,'searchImportForm','ncsImportRecPageDiv')"></a>
									</div>
								</div>
							</div>
							<%--某项job的报告 --%>
							<div id="reportDiv" style="display:none">
								<form id="viewReportForm">
									<input type="hidden" name="attachParams['jobId']"
										id="hiddenJobId" value="" /> <input type="hidden"
										id="hiddenPageSize" name="page.pageSize" value="25" /> <input
										type="hidden" id="hiddenCurrentPage" name="page.currentPage"
										value="1" /> <input type="hidden" id="hiddenTotalPageCnt" />
									<input type="hidden" id="hiddenTotalCnt" value="-1"
										name="page.totalCnt" />
								</form>
								<ul id="icons" class="ui-widget ui-helper-clearfix"
									style="width:100px;cursor:pointer">
									<li class="ui-state-default ui-corner-all" title="返回列表"
										style="width:100px" onclick="javascript:returnToImportList();"><span
										class="ui-icon ui-icon-arrowreturnthick-1-w"
										style="width:20px"></span>返回列表</li>
								</ul>
								<table id="reportListTab" class="greystyle-standard"
									style="width:auto;margin-left:0">
									<thead>
										<th>阶段</th>
										<th>开始时间</th>
										<th>结束</th>
										<th>结果</th>
										<th>详细信息</th>
									</thead>
								</table>
								<div class="paging_div" id="reportListPageDiv"
									style="border: 1px solid #ddd">
									<span class="mr10">共 <em id="emTotalCnt" class="blue">0</em>
										条记录
									</span> <a class="paging_link page-first" title="首页"
										onclick="showListViewByPage('first',queryReportData,'viewReportForm','reportListPageDiv')"></a>
									<a class="paging_link page-prev" title="上一页"
										onclick="showListViewByPage('back',queryReportData,'viewReportForm','reportListPageDiv')"></a>
									第 <input type="text" id="showCurrentPage"
										class="paging_input_text" value="0" /> 页/<em
										id="emTotalPageCnt">0</em>页 <a class="paging_link page-go"
										title="GO"
										onclick="showListViewByPage('num',queryReportData,'viewReportForm','reportListPageDiv')">GO</a>
									<a class="paging_link page-next" title="下一页"
										onclick="showListViewByPage('next',queryReportData,'viewReportForm','reportListPageDiv')"></a>
									<a class="paging_link page-last" title="末页"
										onclick="showListViewByPage('last',queryReportData,'viewReportForm','reportListPageDiv')"></a>
								</div>
							</div>
						</div>
						<div id="div_tab_1" style="display: none;">
							<div>
								<form id="conditionForm" method="post">
									<input type="hidden" id="hiddenPageSize1" name="page.pageSize"
										value="25" /> <input type="hidden" id="hiddenCurrentPage1"
										name="page.currentPage" value="1" /> <input type="hidden"
										id="hiddenTotalPageCnt1" name="page.totalPageCnt" value="-1" />
									<input type="hidden" id="hiddenTotalCnt1" name="page.totalCnt"
										value="-1" />
									<table class="main-table1 half-width"
										style="width: 100%; padding-top: 10px">

										<tr>
											<td class="menuTd" style="text-align:center">区域</td>
											<td class="menuTd" style="text-align:center">BSC/NodeB</td>
											<td class="menuTd" style="text-align:center">CELL</td>
											<td class="menuTd" style="text-align:center">小区中文名</td>

											<td class="menuTd" style="text-align:center">重要级别</td>
											<td class="menuTd" style="text-align:center">覆盖范围</td>
											<td class="menuTd" style="text-align:center">覆盖类型</td>

										</tr>
										<tr>

											<td style="text-align:left">省：<select class="areaCls"
												name="queryCell.provinceId" class="required" id="provinceId"
												style="width:70px">
													<%-- option value="-1">请选择</option --%>
													<s:iterator value="provinceAreas" id="onearea">
														<option value="<s:property value='#onearea.area_id' />">
															<s:property value="#onearea.name" />
														</option>
													</s:iterator>
											</select><br /> 市：<select class="areaCls" name="queryCell.cityId"
												class="required" id="cityId" style="width:70px">
													<s:iterator value="cityAreas" id="onearea">
														<option value="<s:property value='#onearea.area_id' />">
															<s:property value="#onearea.name" />
														</option>
													</s:iterator>
											</select> <br />区：<select class="areaCls" name="queryCell.areaId"
												class="required" id="queryCellAreaId" style="width:70px">
													<option value="-1" selected="true">全部</option>
													<s:iterator value="countryAreas" id="onearea">
														<option value="<s:property value='#onearea.area_id' />">
															<s:property value="#onearea.name" />
														</option>
													</s:iterator>
											</select>
											</td>

											<td style=" text-align:left"><select id="queryCellBscId"
												name="queryCell.bscId">
													<option value="-1">请选择</option>
											</select></td>
											<td style="text-align: left"><input type="text"
												name="queryCell.label" id="celllabel" /> <span
												style="color:red;width:100px;font-family:华文中宋;text-align:center"
												id="celllabelDiv"></span></td>
											<td style="width: 10%; text-align: left"><input
												type="text" name="queryCell.name" id="cellname" /> <span
												style="color:red;width:100px;font-family:华文中宋;text-align:center"
												id="cellnameDiv"></span></td>

											<td style=" text-align: left"><select
												name="queryCell.importancegrade">
													<option value="">请选择</option>
													<s:iterator value="importancegrade" id="one">
														<option value="<s:property value='one' />">
															<s:property value='one' />
														</option>
													</s:iterator>

											</select></td>

											<td style="text-align: left"><select
												name="queryCell.coverarea">
													<option value="">请选择</option>
													<s:iterator value="coverareas" id="one">
														<option value="<s:property value='one' />">
															<s:property value='one' />
														</option>
													</s:iterator>
											</select></td>
											<td style=" text-align: left"><select
												name="queryCell.coverType">
													<option value="" selected="selected">请选择</option>
													<s:iterator value="covertypes" id="one">
														<option value="<s:property value='one' />">
															<s:property value='one' />
														</option>
													</s:iterator>
											</select></td>

										</tr>

										<tr>

											<td style="width: 10%; text-align: center" colspan="7">
												<input type="submit" onclick="" value="查  询" name="search" id="subBtn" />
											</td>
										</tr>
									</table>
								</form>
							</div>
							<%--查询结果  --%>
							<div style="padding-top: 10px">
								<table width="100%">
									<tr>
										<td style="width: 20%">
											<p>
												<font style="font-weight: bold">小区信息表：</font>
											</p>
										</td>


									</tr>

								</table>

							</div>
							<div style="padding-top: 10px">
								<table id="queryResultTab" class="greystyle-standard"
									width="100%">
									<th style="width: 8%">CELL</th>
									<th style="width: 10%">小区中文名</th>
									<th style="width: 8%">LAC</th>
									<th style="width: 8%">CI</th>
									<th style="width: 8%">BCCH</th>
									<th style="width: 10%">BSIC</th>
									<th style="width: 10%">TCH</th>

									<th style="width: 10%">操作</th>
									<%-- 
										<tr class="greystyle-standard-whitetr">
											<td>
												S7CDKG2
											</td>
											<td>
												东坑2
											</td>
											<td>
											</td>
											<td>
											</td>
											</td>
											<td>
											</td>
											<td>
											</td>
											<td>
											</td>
											<td>
												<a href="javascript://" onclick=""><p>
														<img src="../../images/edit-go.png" align="absmiddle"
															width="16" height="16" alt="查看/编辑明细" />
														修改
													</p> </a>
											</td>
										</tr>
--%>
								</table>
							</div>
							<div class="paging_div" style="border: 1px solid #ddd">
								<span class="mr10">共 <em id="emTotalCnt1" class="blue">0</em>
									条记录
								</span> <a class="paging_link page-first" title="首页"
									onclick="showListViewByPage('first')"></a> <a
									class="paging_link page-prev" title="上一页"
									onclick="showListViewByPage('back')"></a> 第 <input type="text"
									id="showCurrentPage1" class="paging_input_text" value="1" /> 页/<em
									id="emTotalPageCnt1">0</em>页 <a class="paging_link page-go"
									title="GO" onclick="showListViewByPage('num')">GO</a> <a
									class="paging_link page-next" title="下一页"
									onclick="showListViewByPage('next')"></a> <a
									class="paging_link page-last" title="末页"
									onclick="showListViewByPage('last')"></a>
							</div>
						</div>
					</div>
				</div>

			</div>



		</div>


	</div>
</body>