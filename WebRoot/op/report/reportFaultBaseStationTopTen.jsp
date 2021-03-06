<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
	<head>
		<link rel="stylesheet" href="../../css/base.css" type="text/css" />
		<link rel="stylesheet" href="../../css/public.css" type="text/css" />
		<link rel="stylesheet" href="../../jslib/jquery/css/jquery.treeview.css" type="text/css" />
		<link rel="stylesheet" href="../../css/input.css" type="text/css" />
		<link rel="stylesheet" href="../../jslib/jquery/css/jquery.treeview.css" type="text/css" />
		<link rel="stylesheet" href="../../jslib/dialog/dialog.css" type="text/css" />
		<link rel="stylesheet" href="css/statements.css" type="text/css" />
		<link rel="stylesheet" href="../../jslib/paging/iscreate-paging.css" type="text/css" />
		<link rel="stylesheet" type="text/css" href="../css/leftMenu.css"></script>

		<script type="text/javascript" src="../../jslib/jquery/jquery-1.6.2.min.js"></script>
		<script type="text/javascript" src="../../jslib/jquery/jquery.treeview.js"></script>
		<script type="text/javascript" src="../js/tab.js" ></script>
		<script type="text/javascript" src="../../jslib/highcharts/highcharts.src.js"></script>
		<script type="text/javascript" src="../../jslib/highcharts/exporting.src.js"></script>
		<script type="text/javascript" src="js/report.js"></script>
		<script type="text/javascript" src="js/reportFaultBaseStationTopTen.js"></script>
		<script type="text/javascript" src="../../jslib/jquery/jquery.treeview.js" ></script>
		<script type="text/javascript" src="../../op/jslib/generateTree.js"></script>
		<script type="text/javascript" src="../../jslib/common.js"></script>
		<title>故障数top-10最差基站排名</title>
		<script type="text/javascript">

$(function(){
	//评论弹出框
	    $(".comment_dialog_show").click(function(){
		    $("#comment_Dialog").show();
			$(".black").show();
	    })
	    $(".dialog_closeBtn").click(function(){
		    $("#comment_Dialog").hide();
			$(".black").hide();
	    })
		
	   /*tab切换*/
	    $(".tab_menu ul li").each(function(index){
			$(this).click(function(){
				//if(this.id!="reportByProject"){}
				$(".tab_menu ul li").removeClass("ontab");
				$(this).addClass("ontab");
				$(".tab_content").hide();
				$(".tab_content").eq(index).show();
			});
		});
		
		/*显示/隐藏报表设置*/
	    $(".show_report_settings").click(function(){
			$(".report_settings").slideToggle("fast");
		});
		
		/*
		
		//统计组织树
		$("#reportOrgTree").treeview({
			collapsed: true,   // true为默认折叠 false为默认展开
			animated: "fast",  //动画---快
		});
		$("#reportOrgTree li span").each(function(){
			$(this).click(function(){
				$("#reportOrgTree li span").removeClass("on_selected");
				$(this).addClass("on_selected");
			})
		});
		
		//显示-隐藏组织树
		$(".reportOrg_treeButton").click(function(){
			$("#reportOrgTree").slideToggle("fast");
		});
		*/
		
		/*绑定按钮，弹出组织架构树*/
		$("#chooseAreaButton").click(function(){
			$("#treeDiv").toggle("fast");
		});
		
		/*绑定按钮，弹出组织架构树*/
		$("#chooseProjectButton").click(function(){
			$("#treeDiv2").toggle("fast");
		});
		
});

</script>
	</head>
	<body>
		<%--主体开始--%>
		<div id="statements_content">
			<%--右边报表开始--%>
			<div class="statements_right">
				<div class="statements_menu_title clearfix">
					<p class="fl">
						基站故障数Top10排名
					</p>
					<p class="fr">
						<span title="指标说明" class="text-icon explain"></span>
						<span title="报表评论"
							class="text-icon comment_list comment_dialog_show"></span>
						<span title="导出EXCEL" class="text-icon excel"></span>
						<span title="自定义查询" class="text-icon search show_report_settings"></span>
					</p>
				</div>
				<%--显示/隐藏报表设置--%>
				<div class="main_output_table report_settings"
					style="display: none;">
					<table>
						<tbody>
							<tr>
								<td class="menuTd">
									考核组织：
								</td>
								<td>
									<input type="text" class="input_text" />
									<a href="#" class="check_treeButton orgButton"></a>
									<ul id="checkTree" class="filetree checkTree">
										<li>
											<span class="folder">第一事业部</span>
											<ul>
												<li>
													<span class="folder">海珠一体化项目</span>
													<ul>
														<li>
															<span class="folder">东部片区</span>
															<ul>
																<li>
																	<span class="file">东一片区</span>
																</li>
																<li>
																	<span class="file">东二片区</span>
																</li>
															</ul>
														</li>
														<li>
															<span class="folder">中部片区</span>
														</li>
														<li>
															<span class="folder">西部片区</span>
														</li>
													</ul>
												</li>
											</ul>
										</li>
									</ul>
								</td>
								<td class="menuTd">
									统计时间：
								</td>
								<td>
									<input type="text" class="input_text" />
									<a href="#" class="dateButton"></a>&nbsp;到
									<input type="text" class="input_text" />
									<a href="#" class="dateButton"></a>
								</td>
							</tr>
							<tr>
								<td class="menuTd">
									业务数据筛选：
								</td>
								<td colspan="3">
									<select>
										<option value="故障类型" selected="">
											故障类型
										</option>
										<option value="告警级别">
											告警级别
										</option>
										<option value="是否紧急故障">
											是否紧急故障
										</option>
										<option value="基站类型">
											基站类型
										</option>
										<option value="故障发生地市">
											故障发生地市
										</option>
									</select>
									<select>
										<option value="等于" selected="">
											等于
										</option>
										<option value="不等于">
											不等于
										</option>
									</select>
									<select>
										<option value="传输故障" selected="">
											传输故障
										</option>
										<option value="投诉">
											投诉
										</option>
										<option value="A2">
											A2
										</option>
										<option value="A2">
											A2
										</option>
										<option value="轮询">
											轮询
										</option>
									</select>
									<input type="button" class="input_button" value="提交考核结果" />
								</td>
							</tr>
							<tr>
								<td class="menuTd vt">
									已选筛选条件：
								</td>
								<td colspan="3">
									<div class="settings_role">
										<ul>
											<li class="clearfix">
												<p class="settings_role_info">
													<span>@故障类型</span>
													<span>等于</span>
													<span>#投诉</span>
												</p>
												<a class="settings_role_del">×</a>
											</li>
											<li class="clearfix">
												<p class="settings_role_info">
													<span>@故障类型</span>
													<span>等于</span>
													<span>#投诉</span>
												</p>
												<a class="settings_role_del">×</a>
											</li>
											<li class="clearfix">
												<p class="settings_role_info">
													<span>@故障类型</span>
													<span>等于</span>
													<span>#投诉</span>
												</p>
												<a class="settings_role_del">×</a>
											</li>
										</ul>
									</div>
								</td>
							</tr>
							<tr class="tc">
								<td colspan="4">
									<input type="button" class="input_button" value="确定" />
									&nbsp;
									<input type="button" class="input_button" value="取消" />
								</td>
							</tr>
						</tbody>
					</table>
				</div>
				<div class="statements_search_date">
					<p>&lt;周期&gt;</p>
					<span class="time_tool_left" onclick="getLastMonth()"></span>
					<span class="time_tool_main" id="DateTimeSpan">2012年23月</span>
					<span class="time_tool_right" onclick="getNextMonth()"></span>
				</div>

				<div id="compareTab" class="compareTab_menu">
					<ul class="clearfix">
						<%-- <li class="selected">
							对比
						</li>
						<li>
							同比
						</li>
						<li>
							环比
						</li> --%>
					</ul>
				</div>
				<div class="compare_info">
					<div id="compareTab_0">
						<div class="statements_tab">
							<div class="tab_menu" id="typeTabDiv">
								<ul>
									<li class="ontab" id="reportByOrg">按组织</li>
									<%-- <li>按地域</li> --%>
									<li id="reportByProject">按项目</li>
								</ul>
							</div>
							
							<div class="tab_container">
								<div class="tab_content">
									<div class="statements_select1" style=" z-index:999;">
									<select>
										<option selected="" value="全部">
											全部
										</option>
									</select>
								
							</div>
									<div class="statements_classify" style="display:none;">
										<span class="statements_classify_title"><input
												type="checkbox" />分类统计</span>
										<div class="statements_classify_info">
											<input type="radio" name="1" />
											<em>按基站类型</em>
											<input type="radio" name="1" />
											<em>按故障类型</em>
											<input type="radio" name="1" />
											<em>按告警级别</em>
											<input type="radio" name="1" />
											<em>按受理专业</em>
											<input type="radio" name="1" />
											<em>按故障大类</em>
										</div>
									</div>
									
									<%-- <div>
										<input type="text" class="search_zuzhi" id="searchBizunitNameText" onfocus="$('#wrap_treeDiv').toggle('fast');" />
	              				 		<a href="javascript:void(0);" style="margin-top:2px; margin-left:-29px;" class="select_button selectWorkPlaceButton" id="chooseAreaButton" title="选择组织"></a>
									
									</div> --%>
									<div class="tab_reportOrgTree">
										统计范围：<input type="hidden" id="searchOrgId" />
										<input type="text" class="input_text" id="searchOrgName" />
										<a href="#" class="reportOrg_treeButton orgButton" id="chooseAreaButton" title="选择组织"></a>
										<%-- <ul id="reportOrgTree" class="filetree reportOrgTree">
											<li><span class="folder">第一事业部</span>
												<ul>
													<li><span class="folder">海珠一体化项目</span>
														<ul>
															<li><span class="folder">东部片区</span>
																<ul>
																	<li><span class="file">东一片区</span></li>
																	<li><span class="file">东二片区</span></li>
																</ul>
															</li>
															<li><span class="folder">中部片区</span></li>
															<li><span class="folder">西部片区</span></li>
														</ul>
													</li>
												</ul>
											</li>
										</ul> --%>
										<div id="treeDiv" class="select_tree_div" style="display:none;">
					    					<%-- 放置组织架构树 --%>
					    				</div>
									</div>
									
									<%--报表图形
									<div class="clearfix">
										<div class="statements_img">
											<div id="resultReport" style="height:200px;"></div>
										</div>
									</div>--%>
									
									<%--报表详情--%>
									<div style="display:none;">
										<div class="statements_table_top">
											<span><a class=""><em>[-]</em>展开表格</a>
											</span>
											<span>工单总数：<em id="uCount">31</em>
											</span>
											<span>平均工单数量：<em id="upCount">5.1</em>
											</span>
											<span>工单处理历时：<em id="uProcessTime">0.31</em>
											</span>
										</div>
										<div class="statements_main_table" style="display: block;">
											<table class="main_table tc">
												<tbody>
													<tr>
														<th style="width: 55px;">
															按故障类型
														</th>
														<th>
															工单数
														</th>
														<th>
															工单处理历时(小时)
														</th>
														<th>
															故障处理及时率
														</th>
													</tr>
													<tr>
														<td>
															CELL
														</td>
														<td>
															18
														</td>
														<td>
															1.42
														</td>
														<td>
															100
														</td>
													</tr>
													<tr>
														<td>
															CELL
														</td>
														<td>
															18
														</td>
														<td>
															1.42
														</td>
														<td>
															100
														</td>
													</tr>
													<tr>
														<td>
															CELL
														</td>
														<td>
															18
														</td>
														<td>
															1.42
														</td>
														<td>
															100
														</td>
													</tr>
													<tr>
														<td>
															CELL
														</td>
														<td>
															18
														</td>
														<td>
															1.42
														</td>
														<td>
															100
														</td>
													</tr>
												</tbody>
											</table>
										</div>
									</div>
									
								</div>
								<div class="tab_content" style="display: none;">
									<div class="tab_reportOrgTree">
										统计范围：<input type="hidden" id="searchProjectId" />
										<input type="text" class="input_text" id="searchProjectName" />
										<a href="#" class="reportOrg_treeButton orgButton" id="chooseProjectButton" title="选择组织"></a>
										<div id="treeDiv2" class="select_tree_div" style="display:none;">
					    					<%-- 放置组织架构树 --%>
					    				</div>
									</div>
								</div>
								<div class="tab_content" style="display: none;">
									
								</div>
							</div>
							
							<%-- 共用 --%>
							<%--报表图形--%>
							<div class="clearfix">
								<div class="statements_img">
									<em class="st_full_screen" onclick="addDiv('ireportDiv');"></em>
									<div id="resultReport" style="height:200px;"></div>
								</div>
							</div>
							
							<%--评论位置--%>
							<div class="statements_comment" id="reportComment" name="reportComment">
													
							</div>
							
						</div>
					</div>
					<div id="compareTab_1" style="display: none">
						1
					</div>
					<div id="compareTab_2" style="display: none">
						2
					</div>
				</div>
			</div>
			<%--右边报表结束--%>
		</div>
		
		<%-- 报表隐藏域（放大版） 开始 --%>
		<div id="ireportDiv" style="height: 500px; width: 1000px; display: none;"></div>
		<%-- 报表隐藏域（放大版） 结束 --%>
	</body>
</html>