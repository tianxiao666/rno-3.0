<%@ page language="java" import="java.util.*" pageEncoding="utf-8"%>
<%@ taglib uri="/struts-tags" prefix="s"%>
<%
String path = request.getContextPath();
String basePath = request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort()+path+"/";

%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
  <head>
    <title>新lte干扰计算流量文件导入</title>
    
	<meta http-equiv="pragma" content="no-cache">
	<meta http-equiv="cache-control" content="no-cache">
	<meta http-equiv="expires" content="0">    
	<meta http-equiv="keywords" content="keyword1,keyword2,keyword3">
	<meta http-equiv="description" content="This is my page">
	<%--
	<link rel="stylesheet" type="text/css" href="styles.css">
	--%>
<%@include file="commonheaderwithoutmap.jsp"%>
<link href="jslib/jquery-ui-1.11.1.custom/jquery-ui.css" rel="stylesheet">
<link href="jslib/jquery-ui-1.11.1.custom/jquery-ui-timepicker-addon.css" rel="stylesheet">
<script src="jslib/jquery-ui-1.11.1.custom/jquery-ui.js"></script>
<script type="text/javascript" src="js/rno_lte_interfer_calc_new.js?v=<%=(String)session.getAttribute("version")%>&t=<%=new Date().getTime()%>"></script>
  <style type="text/css">
  	.divcenter { MARGIN-RIGHT: auto; MARGIN-LEFT: auto; }
  	.detailTable{border-collapse:separate;border-spacing:10px;} 
  	.container-bottom1 {
    text-align: center;
    padding: 0px 0px 20px;
}
.container-bottom2 {
    text-align: center;
    padding: 0px 0px 20px;
}
  </style>
  </head>
  
  <body>
  	<%-- 数据加载遮罩层 --%>
	<div class="loading_cover" id="loadingDataDiv2" style="display: none">
		<div class="cover"></div>
		<h4 class="loading">
			 <em class="loading_fb" id="loadContentId2"></em>,请稍侯...
		</h4>
	</div>
	
	<input type="hidden" id="lteCellsFromSession" value="<s:property value="#session.MRTASKINFO.taskInfo.lteCells"/>"/>
	
	<form id='initNcsAnalysisPageForm' action="initNewLteInterferCalcPageAction" method='post' style="display:none">
		<input type="hidden" id="cityIdParam" name="cityId" value="<s:property value="#session.MRTASKINFO.taskInfo.cityId" />"/>
	</form>
    <font style="font-weight: bold;">当前位置： PCI优化 &gt;区域PCI翻频方案(新算法) &gt; 新建lte干扰计算任务</font>
	<br>
	<center>
	 <a href="initNewLteInterferCalcPageAction" style="text-decoration: underline;font-weight: bold;"><<返回任务列表</a><br>
  	 <font style="font-weight: bold;">任务信息>>参数配置>>流量文件>><font style="color: #31FF81;">扫频数据</font>>>提交任务</font>
  	 <br/><br/>
  	</center>

		<div style="margin-bottom:15px;">
		<div class="divcenter" style="width: 800px;">
			<center>
				<font style="font-size: large; font-weight: bold;">扫频数据</font>
				</h1>
			</center>
			<table style="width:500px;margin: 5px auto;">
			<tr>
			<td style="text-align:center"><input type="button" id="sweepPreStep" name="" value="<上一步 "/></td>
			<td style="text-align:center"><input type="button" id="sweepNextStep" name="" value="下一步>"/></td>
			</tr>
			</table>
			<div style="height: 2px; border-bottom: 1px #000 solid;"></div>
			<table width="100%" border="0" cellspacing="0" cellpadding="0" class="detailTable">
				<tr>
					<td colspan="2" align="center" style="font-size: medium;">
						任务信息
					</td>
				</tr>
				<tr>
					<td width="19%">
						任务名称
					</td>
					<td width="81%">
						<s:property value="#session.MRTASKINFO.taskInfo.taskName" />
					</td>
				</tr>
				<tr>
					<td width="19%">
						分析地市
					</td>
					<td width="81%">
						<s:property value="#session.MRTASKINFO.taskInfo.provinceName" />
						<s:property value="#session.MRTASKINFO.taskInfo.cityName" />
					</td>
				</tr>
				<tr>
					<td width="19%">
						测量时间
					</td>
					<td width="81%">
						<s:property value="#session.MRTASKINFO.taskInfo.startTime" /> - 
						<s:property value="#session.MRTASKINFO.taskInfo.endTime" />
					</td>
				</tr>
				<tr>
					<td>
						任务描述
					</td>
					<td>
						<s:property value="#session.MRTASKINFO.taskInfo.taskDesc" />
					</td>
				</tr>
				<tr>
					<td>启用扫频：</td>
					<td><input id="isUseSf" class="forcheck" type="checkbox" name="cond['isUseSf']" />扫频数据</td>
				</tr>
				<tr>
					<td>运行D1D2频率调整：</td>
					<td><input id="isFreqAdj" class="forcheck" type="checkbox" name="cond['isFreqAdj']" />频率调整</td>
				</tr>
				<tr>
					<td>D1D2频率调整方案</td>
					<td>
						<label for="samefreq"><input type="radio" id="samefreq" name="cond[freqAdjType]" class="freqAdjType" value="ROAD_SAMEFREQ_NETWORKCONSTRUC">道路同频组网</label>
						<label for="interval"><input type="radio" id="interval" name="cond[freqAdjType]" class="freqAdjType" value="ROAD_INTERVAL_NETWORKCONSTRUC">道路插花组网</label>
					</td>
				</tr>
				<tr>
					<td>D1D2频率范围</td>
					<td>
						D1频<input class="freqinput" type="text" id="d1Freq"><br/>
						D2频<input class="freqinput" type="text" id="d2Freq">
					</td>
				</tr>
			</table>
			<table id="structureParamTable" class="greystyle-standard"
						style="width:auto;margin-left:0">
				<thead>
					<tr>
						<th>选择</th>
						<th>文件</th>
						<th>测量时间</th>
						<th>数据量</th>
					</tr>
				</thead>
				<tbody>
				<s:iterator value="#session.MRTASKINFO.sfFileInfo" id="sfFileInfo">
					<tr>
						<td><input class="forfilecheck" type="checkbox" /></td>
						<td class='filename'><s:property value="#sfFileInfo.get('FILE_NAME')" /></td>
						<td><s:property value="#sfFileInfo.get('MEA_TIME')"/></td>
						<td><s:property value="#sfFileInfo.get('RECORD_COUNT')"/></td>
					</tr>
				</s:iterator>
				</tbody>
				</table>
		</div>
		</div>
	</body>
</html>
