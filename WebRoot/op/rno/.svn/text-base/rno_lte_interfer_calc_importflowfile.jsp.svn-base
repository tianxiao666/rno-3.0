<%@ page language="java" import="java.util.*" pageEncoding="utf-8"%>
<%@ taglib uri="/struts-tags" prefix="s"%>
<%
String path = request.getContextPath();
String basePath = request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort()+path+"/";

%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
  <head>
    <title>lte干扰计算流量文件导入</title>
    
	<meta http-equiv="pragma" content="no-cache">
	<meta http-equiv="cache-control" content="no-cache">
	<meta http-equiv="expires" content="0">    
	<meta http-equiv="keywords" content="keyword1,keyword2,keyword3">
	<meta http-equiv="description" content="This is my page">
	<%--
	<link rel="stylesheet" type="text/css" href="styles.css">
	--%>
<%@include file="commonheader.jsp"%>
<link href="jslib/jquery-ui-1.11.1.custom/jquery-ui.css"
	rel="stylesheet">
<link
	href="jslib/jquery-ui-1.11.1.custom/jquery-ui-timepicker-addon.css"
	rel="stylesheet">
<script src="jslib/jquery-ui-1.11.1.custom/jquery-ui.js"></script>
<script type="text/javascript" src="js/rno_lte_interfer_calc.js?v=<%=(String)session.getAttribute("version")%>"></script>
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
	
	<form id='initNcsAnalysisPageForm' action="initLteInterferCalcPageAction" method='post' style="display:none">
		<input type="hidden" id="cityIdParam" name="cityId" value="<s:property value="#session.MRTASKINFO.taskInfo.cityId" />"/>
	</form>
    <font style="font-weight: bold;">当前位置： PCI优化 &gt;区域PCI翻频方案 &gt; 新建lte干扰计算任务</font>
	<br>
	<center>
	 <a href="initLteInterferCalcPageAction" style="text-decoration: underline;font-weight: bold;"><<返回任务列表</a><br>
  	 <font style="font-weight: bold;">任务信息>>参数配置>><font style="color: #31FF81;">流量文件</font>>>提交任务</font>
  	 <br/><br/>
  	</center>

		<div style="margin-bottom:15px;">
		<div class="divcenter" style="width: 800px;">
			<center>
				<font style="font-size: large; font-weight: bold;">流量文件</font>
				</h1>
			</center>
			<table style="width:500px;margin: 5px auto;">
			<tr>
			<td style="text-align:center"><input type="button" id="importFlowFilePreStep" name="" value="<上一步 "/></td>
			<td style="text-align:center"><input type="button" id="importFlowFileNextStep" name="" value="下一步>"/></td>
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
					<td width="19%">
						Ks修正值
					</td>
	                <td style="width: 30%">
					   <%-- <div id="ks" style="padding-top:4px">0.02 /^[^-+]?[^0-9]+(\^.[0-9]+)?$/
					   </div> --%>		
					   <input type="text" name="ks" id="ks" style="width: 50px" class="required" onkeyup="value=value.replace(/[^\.\d]/g,'')" value="0.02"/>		  	 
					   <span id="errorText" style="color:red"></span>
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
			</table>
			
		<form id="formImportPciPlanFile2" enctype="multipart/form-data"
					method="post">
				<input type="hidden" name="fileCode" id='fileCode2'
					value="RNO_PCI_PLAN_FLOW_FILE" /> 
				<input type="hidden" name="token"
					id="token2" value="" />
				<input type="hidden" id="uploadCityId2" name="cityId" 
						value="<s:property value="#session.MRTASKINFO.taskInfo.cityId" />"/>
				<input type="hidden" name="meaTime" class="" id='meatime2'
						value="<s:property value="#session.MRTASKINFO.taskInfo.startTime" />"/>
			    <input type="hidden" name="calType" id='calType'
					value="RNO_PCI_PLAN_FLOW_FILE" />
			<div id="importTitleDiv2">
					<table class="main-table1 half-width"
						style="margin-left:0;width:100%">
						<tbody>
							<tr>
							  <td><font style="color:red;">[ 可选  ]</font> 导入下行小区流量文件(csv)
							  <input type="file" style="width: 44%;" name="file" id="fileid2"
									class="canclear required" />
								<span style="color:red;width:100px;font-family:华文中宋;text-align:center" id="fileDiv2"></span>
								 <a
							href="fileDownloadAction?fileName=流量文件导入模板.csv" title="点击下载模板" id="downloadHref">下载流量文件导入模板</a><br /></td>
							</tr>
						</tbody>
					</table>
				</div>
				<div id="importDiv2" class="container-bottom2" style="display:none;padding-top: 10px">
					<%-- <table style="width: 60%; margin: auto" align="center">
						<tr>
							<td><input type="button" value="导    入"
								style="width: 90px;" id="importBtn" name="import" />
								<br />
								<div id="uploadMsgDiv" style="display:none"></div>	
								</td>
						</tr>
					</table> --%>
					<div id="uploadMsgDiv2" style="display:none"></div>
					<div id="importResultDiv2" class="container-bottom2"
						style="padding-top: 10px;height: 0px;">
						<div id="progressInfoDiv2" style="width:100%;display:none">
							<h2 id="progressNum2">0%</h2>
							<div id="progressbar2"></div>
						</div>
					</div>
				</div>
				
			</form>
			
			<table width="100%" border="0" cellspacing="0" cellpadding="0"  class="detailTable">
				<tr>
					<td style="text-align:right;padding-right:150px">
						<input type="button" id="importbtn" value="导入" />
					</td>
					<%-- <td style="padding-left: 2px">
						<input type="button" id="canclebtn" value="取消" />
					</td> --%>
				</tr>
			</table>
		</div>
		<div id="operInfo"
			style="display: none; top: 40px; left: 600px; z-index: 999; width: 400px; height: 40px; background-color: #7dff3f; filter: alpha(Opacity = 80); -moz-opacity: 0.5; opacity: 0.5; z-index: 9999; position: fixed;">
			<table height="100%" width="100%" style="text-align: center">
				<tr>
					<td>
						<span id="operTip"></span>
					</td>
				</tr>
			</table>

		</div>
		</div>
	</body>
	
	<script  type="text/javascript" >
 		//var lteCells = $("#lteCellsFromSession").val();
 		//$("#pciCell").val(lteCells);
 	</script> 
</html>
