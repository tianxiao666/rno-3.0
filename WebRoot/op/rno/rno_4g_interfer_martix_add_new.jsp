<%@ page language="java" import="java.util.*" pageEncoding="utf-8"%>
<%@ taglib uri="/struts-tags" prefix="s"%>
<%
String path = request.getContextPath();
String basePath = request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort()+path+"/";

%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
  <head>
    <title>新增4g干扰矩阵(新算法)</title>
    
	<meta http-equiv="pragma" content="no-cache">
	<meta http-equiv="cache-control" content="no-cache">
	<meta http-equiv="expires" content="0">    
	<meta http-equiv="keywords" content="keyword1,keyword2,keyword3">
	<meta http-equiv="description" content="This is my page">
	<%--
	<link rel="stylesheet" type="text/css" href="styles.css">
	--%>
<%@include file="commonheaderwithoutmap.jsp"%>
<script type="text/javascript" src="js/rno_lte_interfer_martix_add_new.js?v=<%=(String)session.getAttribute("version")%>"></script>

  </head>
  
  <body>
	<%-- 数据加载遮罩层 --%>
	<div class="loading_cover" id="loadingDataDiv" style="display: none">
		<div class="cover"></div>
		<h4 class="loading">
			 <em class="loading_fb" id="loadContentId"></em>,请稍侯...
		</h4>
	</div>
	
	<font style="font-weight: bold;">当前位置： PCI优化 &gt; 干扰矩阵计算(新算法) &gt; 新计算</font>
	<br>
  	<%-- 干扰矩阵查询条件 --%>
    <div style="width: 80%;margin-top: 5px; margin-left: 6%;">
 	   	<div style="margin-bottom: 5px;text-align:center">
 	 	<a href="initNewLteInterferMartixManageAction" style="text-decoration: underline;font-weight: bold;"><<返回任务列表</a><br/>
 	 	</div>
 	 	<div style="margin-top: 10px;">
	    <form id="interferMartixAddMrForm" method="post">
	    	<input type="hidden" id="hiddenPageSize" name="page.pageSize" value="25" /> 
			<input type="hidden" id="hiddenCurrentPage" name="page.currentPage" value="1" /> 
			<input type="hidden" id="hiddenTotalPageCnt" /> 
			<input type="hidden" id="hiddenTotalCnt" />
	   	 	<input type="hidden" id="sffiles" name="cond.sffilenames" value="" />
 
		    <table class="main-table1 half-width" style="padding-top: 10px;">
					<tr>
						<td  colspan="2" style="text-align: left">
							省：<select name="provinceId"
								class="required" id="provinceId2">
									<%-- option value="-1">请选择</option --%>
									<s:iterator value="provinceAreas" id="onearea">
										<option value="<s:property value='#onearea.area_id' />">
											<s:property value="#onearea.name" />
										</option>
									</s:iterator>
							</select>
							市：<select name="cond['cityId']" class="required"
								id="cityId2">
									<s:iterator value="cityAreas" id="onearea">
										<option value="<s:property value='#onearea.area_id' />">
											<s:property value="#onearea.name" />
										</option>
									</s:iterator>
							</select>
							&nbsp;&nbsp;&nbsp;
							<span id="isCalculateTip" 
								style="font:13px/1.5 Tahoma,'Microsoft Yahei','Simsun';color :red"></span>
						</td>
					</tr>
					<tr>
						<td style="text-align: left;">
							<div style="margin: 11px"></div>
							<span style ="white-space:nowrap;">数据测量时间&nbsp;&nbsp;从
							<s:set name="begtime" value="new java.util.Date()" /> 
							<input id="beginTime" name="cond['begTime']" value="<s:property value="lastMonday"/>" type="text"
								onFocus="WdatePicker({dateFmt:'yyyy-MM-dd HH:mm:ss',onpicked:function(){latestAllowedTime.focus();},maxDate:'#F{$dp.$D(\'latestAllowedTime\')}'})"
								readonly class="Wdate input-text" style="width: 132px;" />
							到 
							<s:set name="endtime" value="new java.util.Date()" />
							<input id="latestAllowedTime" name="cond['endTime']" value="<s:property value="lastSunday" />" type="text"
								onFocus="WdatePicker({dateFmt:'yyyy-MM-dd HH:mm:ss',minDate:'#F{$dp.$D(\'beginTime\')}'})"
								readonly class="Wdate input-text" style="width: 132px;" />
								&nbsp;&nbsp;任务名<input id="taskName" name="cond['taskName']"  maxlength="30"  type="text"  style="width: 132px;" onblur="checkProviderTaskName();"/>
								&nbsp;&nbsp;<a href="javascript:checkProviderTaskName();">检查唯一性</a>
								&nbsp;&nbsp;<span id="taskName_error" class="red"></span><span id="taskName_flag" style="color:#008000;"></span>
							</span>
							<%-- &nbsp;&nbsp;数据类型<span class="txt-impt">*</span> --%>
							<select id="dataType" name="cond['dataType']" hidden="hidden">
								<option value='ALL' selected="selected">全部</option>
								<option value='MR'>MR</option>
								<option value='HO'>HO</option>
							</select>
						</td>
						<td style="text-align: left;">
						<div style="margin: 11px"></div>
							<input id="showMrData" type="button" name="search"
							 style="width: 90px;" value="查看MR数据" onclick="">
							 <span id="isDateRightTip" 
								style="font:13px/1.5 Tahoma,'Microsoft Yahei','Simsun';color :red"></span>
							<input id="showHoData" type="button" name="search"
							 style="width: 90px;" value="查看HO数据" onclick="">
							 <span id="isHoDateRightTip" 
								style="font:13px/1.5 Tahoma,'Microsoft Yahei','Simsun';color :red"></span>
							<input id="isUseSf" class="forcheck" type="checkbox" name="cond['isUseSf']" />使用扫频数据
						</td>
					</tr>
					<tr>
						<td colspan="2" style=" text-align: left;">
						<span style ="white-space:nowrap;">同频小区相关系数权值Kss:
						<input id="SAMEFREQCELLCOEFWEIGHT" name="cond['SAMEFREQCELLCOEFWEIGHT']" value="0.80" type="text" />
						Kss=1-Kho，修改其中一个，另一个自动调整 
						</span>
						</td>
					</tr>
					<tr>
						<td colspan="2" style=" text-align: left;">
						<span style ="white-space:nowrap;">切换比例权值Kho:
						<input id="SWITCHRATIOWEIGHT" name="cond['SWITCHRATIOWEIGHT']" value="0.20" type="text" />
						Kho=1-Kss，修改其中一个，另一个自动调整 
						</span>
						</td>
					</tr>
					<tr>
						<td colspan="2" style=" text-align: left;">
							<div style="margin: 11px"></div>
							<input type="hidden" id="hiddenPageSize" name="cond['jobType']" value="CALC_LTE_INTERFER_MATRIX_NEW" /> 
							<input id="calculateInterMartix" type="button" style="width: 90px;" value="开始计算"  onclick="this.disabled=true;"/>
						</td>
					</tr>
			</table>
		</form>
		</div>
	</div>
	
	
	<%-- MR数据详情列表 --%>
	<div style="width: 80%;margin-left: 6%;padding-top: 10px">
		<table id="mrDataTable" class="greystyle-standard" width="80%">
		</table>
	</div>
	<div class="paging_div" id="mrDataPageDiv"
		style="border: 1px solid #ddd; width: 80%;margin-left: 6%;">
		<span class="mr10">共 <em id="emTotalCnt" class="blue">0</em>
			条记录
		</span> <a class="paging_link page-first" title="首页"
			onclick="showListViewByPage('first',loadMrData,'interferMartixAddMrForm','mrDataPageDiv')"></a>
		<a class="paging_link page-prev" title="上一页"
			onclick="showListViewByPage('back',loadMrData,'interferMartixAddMrForm','mrDataPageDiv')"></a>
		第 <input type="text" id="showCurrentPage" class="paging_input_text"
			value="0" /> 页/<em id="emTotalPageCnt">0</em>页 <a
			class="paging_link page-go" title="GO"
			onclick="showListViewByPage('num',loadMrData,'interferMartixAddMrForm','mrDataPageDiv')">GO</a>
		<a class="paging_link page-next" title="下一页"
			onclick="showListViewByPage('next',loadMrData,'interferMartixAddMrForm','mrDataPageDiv')"></a>
		<a class="paging_link page-last" title="末页"
			onclick="showListViewByPage('last',loadMrData,'interferMartixAddMrForm','mrDataPageDiv')"></a>
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
