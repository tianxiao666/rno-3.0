<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>
<%
	String path = request.getContextPath();
	String basePath = request.getScheme() + "://"
			+ request.getServerName() + ":" + request.getServerPort()
			+ path + "/";
%>
<%@ taglib prefix="s" uri="/struts-tags"%>

<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<meta http-equiv="pragma" content="no-cache" />
<meta http-equiv="cache-control" content="no-cache" />
<meta http-equiv="expires" content="0" />
<meta http-equiv="keywords" content="" />
<meta http-equiv="description" content="" />
<title>怡创网优云服务平台</title>
<%@include file="commonheader.jsp"%>

<link rel="stylesheet" type="text/css" href="../../css/base.css" />
<link rel="stylesheet" type="text/css" href="../../css/public.css" />
<link rel="stylesheet" type="text/css" href="../home/resource/css/portal-style.css"/>
<link rel="stylesheet" type="text/css" href="../home/resource/jquery-easyui/themes/default/easyui.css"/>
<link rel="stylesheet" type="text/css" href="../home/resource/navigation_tab_plugin/css/TabPanel.css"/>
<link rel="stylesheet" type="text/css" href="css/index.css?version=<%=(String)session.getAttribute("version")%>"/>

<script src="../../jslib/jquery/jquery-1.6.2.min.js"></script>
<script src="../home/resource/js/jquery.corner.js"></script>
<script src="../home/resource/jquery-easyui/jquery.easyui.min.js"></script>
<script src="../home/resource/navigation/navi.js"></script>
<script src="js/index.js?version=<%=(String)session.getAttribute("version")%>"></script>

<script>
$(document).ready(function(){
	//初始化门户主体高度
	windowsResize();
	onLoadOpenHomeTab();
});
//浏览器大小变化时修改门户主体高度
$(window).resize(function() {
	if (window.timerLayout)
		clearTimeout(window.timerLayout);
	window.timerLayout = null;
	if ($.browser.msie) {
		window.timerLayout = setTimeout(windowsResize, 100);
	} else {
		window.timerLayout = setTimeout(windowsResize, 100);
	}
});

//获取门户主体高度
function windowsResize() {
		var docH = document.documentElement.clientHeight;
		$("#maindiv").css("height", docH - 95 - 40);
}

function openHomeTab(url,me){
	$("#showWorkZoneSiteListDiv li").removeClass("main_nav_on");
	$(me).addClass("main_nav_on");
	$("#maininnerdivif").attr("src",url);
}

function onLoadOpenHomeTab(){
	//$("#showWorkZoneSiteListDiv > ul > li").eq(0).children("ul").children("li").eq(0).click();
	// 从HTML5本地存储中取值
    var selectedMenuId = localStorage.SelectedMenuId;
    if (selectedMenuId == "") {
        $("#showWorkZoneSiteListDiv > ul > li").eq(0).children("ul").children("li").eq(0).click();
    } else {
        //document.getElementById(selectedMenuId).click();
        $("#"+selectedMenuId).click();
    }
}
 
function openHomeTabSelf(url,me){
	window.open(url);
}

function getLiObj(obj){
	$("#SelectedMenuId").val($(obj).eq(0).attr('id'));
	localStorage['SelectedMenuId']=$(obj).eq(0).attr('id');
}
</script>
</head>

<body style="overflow: hidden;">
<!-- 当前页面菜单ID -->
<input id="SelectedMenuId" name="SelectedMenuId" type="hidden" value="" />
	<%-- ----头部导航--begin---- --%>
	<div class="header">
		<div class="system_name"></div>
		<div class="header_nav">
			<div class="user_config_nav">
				<div id="userConfigDiv" class="user_config_div">
					<div style="margin-top:3px;"></div>
					<input id="saveUserConfigBtn" type="button" style="margin-top:1px;" value="设置默认"/>
					&nbsp;默认区域：
					<select id="provincemenu">
					<s:iterator value="provinceAreas" id="onearea">
						<option value="<s:property value='#onearea.area_id' />">
						<s:property value="#onearea.name" />
						</option>
					</s:iterator>
					</select>
					<select id="citymenu" name="attachParams['cityId']">
					<s:iterator value="cityAreas" id="onearea">
						<option value="<s:property value='#onearea.area_id' />">
						<s:property value="#onearea.name" />
						</option>
					</s:iterator>
					</select>
				</div>
			</div>
			<div class="tool_bar">
				<em class="f-bold">${sysOrgUser.name }，欢迎您！</em>
				<a href="javascript:void(0)"  onclick="$('#maininnerdivif').attr('src','../../../ops/op/selfservice/loadSelfServiceHomePageAction.action');">我的设置</a>
				<span class="quit"><a href="rnoLogoutAction.action">退出</a></span>
			</div>
		</div>
		<div class="now_time">
			<%-- 2012年12月14日 星期五 16:36:54 --%>
			<div id="nowTime"></div>
		</div>
	</div>
	<%-- ----头部导航--end---- --%>

	<div id="container">
		<div class="logo_div" id="logo_div"></div>

		<%-- ----主菜单--begin---- --%>
		<div class="main_nav" id="showWorkZoneSiteListDiv" style="position:absolute;z-index:30;padding:0 5px">
			<ul id="navigationMenu">
				<s:iterator value="menuList" var="map">
					 	<li id="<s:property value="#map.text"/>" class="firstLevelMenu"><div><s:property value="#map.text"/></div>
					 	<ul style="display:none">
								<s:iterator value="#map.children" var="children">
								   <s:if test="#children.leaf">
								   <li id="<s:property value="#children.TEXT"/>" class="secondLevelMenu"><s:property value="#children.TEXT"/>
								   <div style="float:right;"><span class="arrow"></span></div>
								    <ul style="display:none">
									    <s:iterator value="#children.grandchild" var="grandchild">
									       <li onclick="getLiObj(this)" id="<s:property value="#grandchild.TEXT"/>" class="thirdLevelMenu"><s:property value="#grandchild.TEXT"/>
									       <s:if test="(#grandchild.URL).startsWith('http')">
									       <input type="hidden" value="<s:property value='#grandchild.URL' />?account=${session.userId}">
									       </s:if>
									       <s:else>
									       <input type="hidden" value="../../../<s:property value='#grandchild.URL' />?account=${session.userId}">
									       </s:else>
									       </li>
									    </s:iterator>
									    
								</ul>
								</s:if>
								<s:else>
								<li onclick="getLiObj(this)" id="<s:property value="#children.TEXT"/>" class="secondLevelMenu"><s:property value="#children.TEXT"/>
								<s:if test="(#children.URL).startsWith('http')">
									<input type="hidden" value="<s:property value='#children.URL' />?account=${session.userId}">
								</s:if>
							    <s:else>
									<input type="hidden" value="../../../<s:property value='#children.URL' />?account=${session.userId}">
								</s:else>
								</s:else>
								</li>
							   </s:iterator>
							</ul>
						</li>
				</s:iterator>
			</ul>
		</div>
		<%-- ----主菜单--end---- --%>

		<%-- Liang YJ 2014-4-1 修改  之前有两个id id="workZonesiteFrame1318581008644" --%>
		<div id="maindiv" style="position:absolute;height:600px;top:100px;width:100%">
			<iframe scrolling="anto" id="maininnerdivif" frameborder="0" src=""
				style="width: 100%; height: 100%;" name="workZonesiteFrame1318581008644"></iframe>
			<%-- ----各工作空间对应工作区--end---- --%>
		</div>
	</div>
	<%-- ----友情链接及底部版权信息区--begin---- --%>
	<div id="footermaindiv">
		<div class="footer">
			<ul>
				<li class="info">
				<%=(String)session.getAttribute("projectName")+" "+(String)session.getAttribute("version")%>&nbsp;&nbsp;
				<a href="fileDownloadAction?fileName=rno_user_manual.zip" style="text-decoration: underline;color:#666; " target="_blank">使用说明书下载 </a>
				&nbsp;&nbsp;维护热线020-28817300-201&nbsp;Copyright&nbsp;©&nbsp;<%=(String)session.getAttribute("corporationName")%>&nbsp;|&nbsp;
				<a href="http://www.miitbeian.gov.cn/" target="_blank" style="text-decoration: underline;color:#666; ">粤ICP备12023904号</a>&nbsp;&nbsp;
				</li>
			</ul>
		</div>
	</div>
	<%-- ----友情链接及底部版权信息区--end---- --%>

</body>
</html>