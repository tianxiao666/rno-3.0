
//projectId 项目id(jsp页面数据)
//projectBaseInfo

$(document).ready(function(){
	/***************** 项目基本信息 begin ******************/
	//显示项目信息
	//showProjectInfo(4);
	/***************** 项目基本信息 end ********************/
	
	
	
	/***************** 项目网络资源 begin ******************/
	
	
	$("#project_network_areatree_div :checkbox").live( "change" ,function(){
		var disabled = $(this).attr("disabled");
		var checked = $(this).attr("checked");
		if ( !disabled ) {
			if ( !checked ) {
				$(this).attr({"checked":"checked"});
			} else {
				$(this).removeAttr("checked");
			}
		}
	});
	
	$("#network_area_type_info").live("change",function(){
		var a = $("#project_network_areatree_div").find(":checkbox:checked");
		$(a).attr({"disabled":"disabled"});
		var areaName = $("#project_network_areatree_div").find(".treeViewClick").text();
		var cb = $("#project_network_areatree_div").find(":checkbox[c='" + areaName + "']");
		$(cb).attr({
			"checked" : "checked"
		}).removeAttr("disabled");
	});
	
	$("#project_network_button").click(function(){
		var areaId = $("#network_resource_areaId").val();
		var ch = $(":not(:disabled)[name='a'],:not(:disabled)[name='b']");
		var checkedres = "";
		var nocheckedres = "";
		$(ch).each(function( index ){
			var checkbox = $(ch[index]).val();
			var res_str = "";
			res_str += checkbox;
			if ( index != $(ch).length - 1 ) {
				res_str += ",";
			}
			if ( $(ch[index]).is(":checked") ) {
				checkedres += res_str ;
			} else {
				nocheckedres += res_str ;
			}
		});
		var orgId = $("#orgId").val();
		
		$.ajax({
			"url" : "networkresourcemanage_ajax!saveProjectResource.action" , 
			"type" : "post" , 
			"data" : { "areaId" : areaId , "orgId" : orgId , "projectId" : projectId , "checkedres" : checkedres , "nocheckedres" : nocheckedres } , 
			"async" : true ,
			"success" : function( result ) {
				result = parseInt(result);
				if ( result > 0 ) {
					alert("操作成功!");
					if ( !checkedres || checkedres.length == 0 ) {
						$("#project_network_areatree_div").find("li[cid='" + areaId + "'] >:checkbox").removeAttr("stat").removeAttr("checked").removeAttr("disabled");
					} else {
						$("#project_network_areatree_div").find("li[cid='" + areaId + "'] >:checkbox").attr({"checked":"checked","disabled":"disabled","stat":"cd"});
					}
					
				}
			}
		});
	});
	
	/***************** 项目网络资源 end ********************/
	
	
	
	/***************** 维护组织划分 begin ******************/
	
	$("#server_treeDiv :checkbox,#repair_area_tree_div :checkbox").live( "change" ,function(){
		var disabled = $(this).attr("disabled");
		var checked = $(this).attr("checked");
		if ( !disabled ) {
			if ( !checked ) {
				$(this).attr({"checked":"checked"});
			} else {
				$(this).removeAttr("checked");
			}
		}
	});
	
	$("#repair_area_type_info").live("change",function(){
		var a = $("#repair_area_tree_div").find(":checkbox:checked");
		$(a).attr({"disabled":"disabled"});
		var areaName = $("#repair_area_tree_div").find(".treeViewClick").text();
		var cb = $("#repair_area_tree_div").find(":checkbox[c='" + areaName + "']");
		$(cb).attr({
			"checked" : "checked"
		});
	});
	
	
	
	//组织架构树
	serverProviderOrgTree();
	
	$("#project_repair_button").click(function(){
		var areaId = $("#network_repair_resource_areaId").val();
		var ch = $(":not(:disabled)[name='c'],:not(:disabled)[name='d']");
		var checkedres = "";
		var nocheckedres = "";
		$(ch).each(function( index ){
			var checkbox = $(ch[index]).val();
			var res_str = "";
			res_str += checkbox;
			if ( index != $(ch).length - 1 ) {
				res_str += ",";
			}
			if ( $(ch[index]).is(":checked") ) {
				checkedres += res_str ;
			} else {
				nocheckedres += res_str ;
			}
		});
		
		var orgId = $("#orgId").val();
		
		$.ajax({
			"url" : "networkresourcemanage_ajax!saveProjectResourceStatus.action" , 
			"type" : "post" , 
			"data" : { "areaId" : areaId , "orgId" : orgId , "projectId" : projectId , "checkedres" : checkedres , "nocheckedres" : nocheckedres } , 
			"async" : true ,
			"success" : function( result ) {
				result = parseInt(result);
				if ( result > 0 ) {
					alert("操作成功!");
					loadRepairOrg();
					loadRepairNetworkRes(projectId);
				}
			}
		});
		
		
	});
	/***************** 维护组织划分 end ********************/
	
})

/***************** 项目基本信息 begin ******************/

function showProjectInfo (projectId) {
	$.ajax({
			"url" : "projectmanage_ajax!findSingleProjectInfo.action" , 
			"type" : "post" , 
			"data" : { "id" : projectId } , 
			"async" : true ,
			"success" : function( result ) {
				result = eval ( "(" + result + ")" ) ;
				projectBaseInfo = new Project( result );
				projectBaseInfo.putInfo("#network_repair_div");
				//区域树
				areaTree(projectId);
			}
	});
}  

var isAjax = true;
//区域树
function areaTree ( projectId ,cityId ) {
	var url = "information_area_ajax!areaTreeAction.action";
	var orgId = $("#orgId").val();
	if(isAjax == false){
		//return;
	}else{
		//isAjax = false;
	}
	var projectId = $("#p_projectId").val();
	$.ajax({
		"url" : url , 
		"type" : "post" , 
		"data" : { "max" : 4 , "provinceId" : cityId,"projectId":projectId } , 
		"async" : true , 
		"success" : function( result ) {
			result = eval ( "(" + result + ")" );
			//项目网络资源	
			createCheckBoxTreeInformationmanage(result,"repair_area_tree_div","network_repair_resource_areatree","a","netWorkRepairResourceTreeClick");										//,"network_repair_resource_areatree_checkBox","netWorkRepairResourceTreeClick"	
			$("#repair_area_tree>li>span").eq(0).click();
		}
	});
}

function netWorkRepairResourceTreeClick(data, tableId){
	var projectId = $("#p_projectId").val();
	data = eval("(" + data + ")");
	//alert("net:"+data.areaId);
			var orgId = $("#network_repair_resource_orgId").val();
								$("#network_repair_resource_areaId").val(data.areaId);
								createOrgResourceDiv( $("#repair_divide_resource_div") , projectId , "c", true );															
								createOrgLineDiv( $("#repair_divide_line_div") , projectId , "d", true );
								
								var checkb = $("#repair_area_tree_div :checkbox");
								$( checkb ).each(function(){
									var disabled = $(this).attr("disabled");
									var checked = $(this).attr("checked");
									var stat = $(this).attr("stat");
									if ( stat && stat == "cd" ) {
										$(this).attr({ "checked":"checked" , "disabled":"disabled" });
									} else if ( !disabled ) {
										if ( checked ) {
											$(this).removeAttr("checked");
										}
									}
								});
	}
 


/***************** 项目基本信息 end ********************/


/***************** 项目网络资源 begin ******************/


function loadNetworkRes () {
	var li = $("#project_network_areatree_div li");
	$(li).each(function(){
		var areaId = $(this).attr("cid");
		var ins = this;
		var orgId = $("#network_resource_orgId").val();
		$.ajax({
			"url" : "networkresourcemanage_ajax!checkProjectResourceIsexists.action" , 
			"type" : "post" , 
			"data" : { "areaId" : areaId , "orgId" : orgId , "projectId" : projectId } , 
			"async" : true , 
			"success" : function( result ){
				var data = eval( "(" + result + ")" );
				for ( var key in data ) {
					$(ins).find(">:checkbox").attr({"checked":"checked","disabled":"disabled","stat":"cd"});
					break;
				}
			}
		});
	});
}


function createResourceDiv ( resourceDiv , projectId , areaId , orgId , id , isdisplay ) {
	$.ajax({
		"url" : "networkresourcemanage_ajax!checkProjectResourceIsexists.action" , 
		"type" : "post" , 
		"data" : { "areaId" : areaId , "orgId" : orgId , "projectId" : projectId } , 
		"async" : true , 
		"success" : function( result ){
			result = eval ( "(" + result + ")" );
			$(resourceDiv).empty();
			var choiceAllInput = $("<input type='checkbox' />");
			var span = $("<span/>").css({"cursor":"pointer"}).text("全选");
			var ul = $("<ul/>");   
			$(span).click(function(){
				$(choiceAllInput).attr({"checked":!$(choiceAllInput).attr("checked")});
				var ch = !!$(choiceAllInput).attr("checked");
				$(ul).find(":checkbox:not(:disabled)").attr({"checked":ch});
			});
			$(choiceAllInput).click(function(){
				var ch = !!$(this).attr("checked");
				$(ul).find(":checkbox:not(:disabled)").attr({"checked":ch});
			});
			$(resourceDiv).append($(choiceAllInput));     
			$(resourceDiv).append($(span));    
			$(resourceDiv).append($(ul)); 
			
			var arr = {
				"Station" : { "en_name" : "Station" ,  "name" : "站址" } , 
				"GeneralBaseStation" : { "en_name" : "GeneralBaseStation" ,  "name" : "基站" } , 
				"ManWell" : { "en_name" : "ManWell" ,  "name" : "人井" } , 
				"Pole" : { "en_name" : "Pole" ,  "name" : "电杆" } , 
				"HangWall" : { "en_name" : "HangWall" ,  "name" : "挂墙点" } , 
				"MarkPost" : { "en_name" : "MarkPost" ,  "name" : "挂墙" } , 
				"FiberCrossCabinet" : { "en_name" : "FiberCrossCabinet" ,  "name" : "光交接箱" } , 
				"FiberDistributionCabinet" : { "en_name" : "FiberDistributionCabinet" ,  "name" : "光分纤箱" } , 
				"FiberTerminalCase" : { "en_name" : "FiberTerminalCase" ,  "name" : "终端盒" } 
			}
			
			var i = 0;
			for ( var key in arr ) {
				var en_resName = key;
				var resObj = arr[en_resName];
				var resName = resObj.name;
				var li = $("<li/>");
				var attr = "";
				var style = "";
				if ( result[en_resName] && !isdisplay ) {
					attr = " checked='checked'";
				} else if ( isdisplay ) {
					var obj = result[en_resName];
					if ( !obj ) {
						style = "style='display:none;'";
						continue;
					} else if ( obj.status > 0 ) {
						attr = " checked='checked'";
					}
				}
				var liInput = $("<input " + style + " type='checkbox' " + attr + ">").attr({"id": id+i , "name" : id }).val(resObj.en_name);
				var label = $("<label " + style + " />").attr({"for":id+i}).text(resName);
				$(li).appendTo($(ul));
				$(liInput).appendTo($(li));
				$(label).appendTo($(li));
				i++;
			}
		}
	});
}


function createLineDiv ( LineDiv , projectId , areaId , orgId , id , isdisplay ) {
$.ajax({
		"url" : "networkresourcemanage_ajax!checkProjectResourceIsexists.action" , 
		"type" : "post" , 
		"data" : { "areaId" : areaId , "orgId" : orgId , "projectId" : projectId } , 
		"async" : true , 
		"success" : function( result ){
			result = eval ( "(" + result + ")" );
			$(LineDiv).empty();
			var choiceAllInput = $("<input type='checkbox' />");
			var span = $("<span/>").css({"cursor":"pointer"}).text("全选");
			var ul = $("<ul/>");   
			$(span).click(function(){
				$(choiceAllInput).attr({"checked":!$(choiceAllInput).attr("checked")});
				var ch = !!$(choiceAllInput).attr("checked");
				$(ul).find(":checkbox:not(:disabled)").attr({"checked":ch});
			});
			$(choiceAllInput).click(function(){
				var ch = !!$(this).attr("checked");
				$(ul).find(":checkbox:not(:disabled)").attr({"checked":ch});
			});
			$(LineDiv).append($(choiceAllInput));
			$(LineDiv).append($(span));
			$(LineDiv).append($(ul));
			
			var arr = {
				"PipeRoute" : { "name" : "管线网" , "en_name" : "PipeRoute" , "facility_han" : "含：管道、杆路、管道段、直埋段、吊线段" } , 
				"Fiber" : { "name" : "光缆网" , "en_name" : "Fiber" , "facility_han" : "含：光缆、光缆段、纤芯" } , 
				"OpticalRoute" : { "name" : "光路网" , "en_name" : "OpticalRoute" , "facility_han" : "含：光路、光路纤芯、局向纤芯" } , 
				"TransmissionNetwork" : { "name" : "传输网" , "en_name" : "TransmissionNetwork" , "facility_han" : "含：传输系统、传输段" }
			};
			
			var i = 0;
			for ( var key in arr ) {
				var en_resName = key;
				var resObj = arr[en_resName];
				var resName = resObj.name;
				var li = $("<li/>");
				var checked = "";
				var attr = "";
				if ( result[en_resName] && !isdisplay ) {
					attr = " checked='checked'";
				} else if ( isdisplay ) {
					var obj = result[en_resName];
					if ( !obj ) {
						attr = " disabled='disabled'";
						continue;
					} else if ( obj.status > 0 ) {
						attr = " checked='checked'";
					}
				}
				var liInput = $("<input type='checkbox' " + attr + ">").attr({"id": id+i , "name" : id }).val(resObj.en_name);
				var label = $("<label/>").attr({"for":id+i}).text(resName);
				$(li).appendTo($(ul));
				$(liInput).appendTo($(li));
				$(label).appendTo($(li));
				i++;
			}
		}
	});
}




/***************** 项目网络资源 end ********************/


/***************** 维护组织划分 begin ******************/



function createOrgResourceDiv ( resourceDiv , projectId , id , isdisplay ) {
	var areaId = $("#network_repair_resource_areaId").val();
	if ( !areaId ) {
		setTimeout(function () {
			createOrgResourceDiv ( resourceDiv , projectId , id , isdisplay );
		} , 1000);
		return;
	}
	var orgId = $("#orgId").val();
	$.ajax({
		"url" : "networkresourcemanage_ajax!checkProjectResourceIsexists.action" , 
		"type" : "post" , 
		"data" : { "areaId" : areaId , "orgId" : orgId , "projectId" : projectId } , 
		"async" : true , 
		"success" : function( result ){
			result = eval ( "(" + result + ")" );
			$(resourceDiv).empty();
			var choiceAllInput = $("<input type='checkbox' />");
			var span = $("<span/>").css({"cursor":"pointer"}).text("全选");
			var ul = $("<ul/>");   
			$(span).click(function(){
				$(choiceAllInput).attr({"checked":!$(choiceAllInput).attr("checked")});
				var ch = !!$(choiceAllInput).attr("checked");
				$(ul).find(":checkbox:not(:disabled)").attr({"checked":ch});
			});
			$(choiceAllInput).click(function(){
				var ch = !!$(this).attr("checked");
				$(ul).find(":checkbox:not(:disabled)").attr({"checked":ch});
			});
			$(resourceDiv).append($(choiceAllInput));     
			$(resourceDiv).append($(span));     
			$(resourceDiv).append($(ul)); 
			
			var arr = {
				"Station" : { "en_name" : "Station" ,  "name" : "站址" } , 
				"GeneralBaseStation" : { "en_name" : "GeneralBaseStation" ,  "name" : "基站" } , 
				"ManWell" : { "en_name" : "ManWell" ,  "name" : "人井" } , 
				"Pole" : { "en_name" : "Pole" ,  "name" : "电杆" } , 
				"HangWall" : { "en_name" : "HangWall" ,  "name" : "挂墙点" } , 
				"MarkPost" : { "en_name" : "MarkPost" ,  "name" : "挂墙" } , 
				"FiberCrossCabinet" : { "en_name" : "FiberCrossCabinet" ,  "name" : "光交接箱" } , 
				"FiberDistributionCabinet" : { "en_name" : "FiberDistributionCabinet" ,  "name" : "光分纤箱" } , 
				"FiberTerminalCase" : { "en_name" : "FiberTerminalCase" ,  "name" : "终端盒" }
			}
			
			var i = 0;
			for ( var key in arr ) {
				var en_resName = key;
				var resObj = arr[en_resName];
				var resName = resObj.name;
				var li = $("<li/>");
				var attr = "";
				var style = "";
				if ( result[en_resName] && !isdisplay ) {
					attr = " checked='checked'";
				} else if ( isdisplay ) {
					var obj = result[en_resName];
					if ( !obj ) {
						style = "style='display:none;'";
						continue;
					} else if ( obj.status > 0 ) {
						attr = "";
					}
				}
				var liInput = $("<input " + style + " type='checkbox' " + attr + ">").attr({"id": id+i , "name" : id }).val(resObj.en_name);
				var label = $("<label " + style + " />").attr({"for":id+i}).text(resName);
				$(li).appendTo($(ul));
				$(liInput).appendTo($(li));
				$(label).appendTo($(li));
				i++;
			}
			
			$.ajax({
				"url" : "networkresourcemanage_ajax!checkProjectResourceIsexists.action" , 
				"type" : "post" , 
				"data" : { "areaId" : $("#network_repair_resource_areaId").val() , "orgId" : $("#orgId").val() , "projectId" : projectId } , 
				"async" : true , 
				"success" : function( data ){
					data = eval ( "(" + data + ")" );
					for ( var key in data ) {
						var status = data[key].status;
						if ( status == 1 ) {
							$("#repair_divide_resource_div :checkbox[value='" + key + "']").attr({"checked":"checked","disabled":"disabled"});
						}
					}
				}
			});
			
		}
	});
}


function createOrgLineDiv ( LineDiv , projectId , id , isdisplay ) {
var areaId = $("#network_repair_resource_areaId").val();
var orgId = $("#orgId").val();
$.ajax({
		"url" : "networkresourcemanage_ajax!checkProjectResourceIsexists.action" , 
		"type" : "post" , 
		"data" : { "areaId" : areaId , "orgId" : orgId , "projectId" : projectId } , 
		"async" : true , 
		"success" : function( result ){
			result = eval ( "(" + result + ")" );
			$(LineDiv).empty();
			var choiceAllInput = $("<input type='checkbox' />");
			var span = $("<span/>").css({"cursor":"pointer"}).text("全选");
			var ul = $("<ul/>");   
			$(span).click(function(){
				$(choiceAllInput).attr({"checked":!$(choiceAllInput).attr("checked")});
				var ch = !!$(choiceAllInput).attr("checked");
				$(ul).find(":checkbox:not(:disabled)").attr({"checked":ch});
			});
			$(choiceAllInput).click(function(){
				var ch = !!$(this).attr("checked");
				$(ul).find(":checkbox:not(:disabled)").attr({"checked":ch});
			});
			$(LineDiv).append($(choiceAllInput));
			$(LineDiv).append($(span));
			$(LineDiv).append($(ul));
			
			var arr = {
				"PipeRoute" : { "name" : "管线网" , "en_name" : "PipeRoute" , "facility_han" : "含：管道、杆路、管道段、直埋段、吊线段" } , 
				"Fiber" : { "name" : "光缆网" , "en_name" : "Fiber" , "facility_han" : "含：光缆、光缆段、纤芯" } , 
				"OpticalRoute" : { "name" : "光路网" , "en_name" : "OpticalRoute" , "facility_han" : "含：光路、光路纤芯、局向纤芯" } , 
				"TransmissionNetwork" : { "name" : "传输网" , "en_name" : "TransmissionNetwork" , "facility_han" : "含：传输系统、传输段" }
			};
			
			var i = 0;
			for ( var key in arr ) {
				var en_resName = key;
				var resObj = arr[en_resName];
				var resName = resObj.name;
				var li = $("<li/>");
				var checked = "";
				var attr = "";
				if ( result[en_resName] && !isdisplay ) {
					attr = " checked='checked'";
				} else if ( isdisplay ) {
					var obj = result[en_resName];
					if ( !obj ) {
						attr = " disabled='disabled'";
						continue;
					} else if ( obj.status > 0 ) {
						attr = "";
					}
				}
				var liInput = $("<input type='checkbox' " + attr + ">").attr({"id": id+i , "name" : id }).val(resObj.en_name);
				var label = $("<label/>").attr({"for":id+i}).text(resName);
				$(li).appendTo($(ul));
				$(liInput).appendTo($(li));
				$(label).appendTo($(li));
				i++;
			}
			
			$.ajax({
				"url" : "networkresourcemanage_ajax!checkProjectResourceIsexists.action" , 
				"type" : "post" , 
				"data" : { "areaId" : $("#network_repair_resource_areaId").val() , "orgId" : $("#orgId").val() , "projectId" : projectId } , 
				"async" : true , 
				"success" : function( data ){
					data = eval ( "(" + data + ")" );
					for ( var key in data ) {
						var status = data[key].status;
						if ( status == 1 ) {
							$("#repair_divide_line_div :checkbox[value='" + key + "']").attr({"checked":"checked","disabled":"disabled"});
						}
					}
				}
			});
			
		}
	});
}

function loadRepairOrg () {
	var li = $("#server_treeDiv li");
	var areaId = $("#network_repair_resource_areaId").val();
	$(li).each(function(){
		var orgId = $(this).find(">:checkbox").val();
		var ins = this;
		$.ajax({
			"url" : "networkresourcemanage_ajax!checkProjectResourceIsexists.action" , 
			"type" : "post" , 
			"data" : { "areaId" : null , "orgId" : orgId , "projectId" : projectId } , 
			"async" : true , 
			"success" : function( result ){
				var data = eval( "(" + result + ")" );
				$(ins).find(">:checkbox").removeAttr("checked").removeAttr("disabled").removeAttr("stat");
				for ( var key in data ) {
					var status = data[key].status;
					if ( status == 1 ) {
						$(ins).find(">:checkbox").attr({"checked":"checked","disabled":"disabled","stat":"cd"});
						break;
					}
				}
			}
		});
	});
}

function loadRepairNetworkRes ( projectId ) {
	var li = $("#repair_area_tree_div li");
	var orgId = $("#orgId").val();
	$(li).each(function(){
		var areaId = $(this).attr("cid");
		var ins = this;
		$.ajax({
			"url" : "networkresourcemanage_ajax!checkProjectResourceIsexists.action" , 
			"type" : "post" , 
			"data" : { "areaId" : areaId , "orgId" : orgId , "projectId" : projectId } , 
			"async" : true , 
			"success" : function( result ){
				var data = eval( "(" + result + ")" );
				$(ins).find(">:checkbox").removeAttr("checked").removeAttr("disabled").removeAttr("stat");
				for ( var key in data ) {
					var status = data[key].status;
					if ( status == 1 ) {
						$(ins).find(">:checkbox").attr({"checked":"checked","disabled":"disabled","stat":"cd"});
						break;
					}
				}
			}
		});
	});
	isAjax = true;
}


//生成服务商的组织架构树
function serverProviderOrgTree(){
	var orgId = $("#orgId").val();
	if(orgId==null||orgId==""){
		setTimeout(function(){
			serverProviderOrgTree();
		},100);
		return;
	}
	var values = {"orgId":orgId}
	var myUrl = "../organization/getProviderOrgTreeByOrgIdAction";
	$.post(myUrl,values,function(data){
		var result = eval( "(" + data.toSource() + ")" ) ;
		if ( result && result.length > 0 && result[0] ) {
			$("#network_repair_resource_orgId").val(result[0].orgId);
		}
		createCheckBoxTree(data,"server_treeDiv","server_tree","a","serverOrgTreeClick");
		$("#server_tree>li>span").eq(0).click();
		loadRepairOrg();
	},"json");
}

function a(){} 

//显示服务商的组织信息
function serverOrgTreeClick(dataStr,tableId){
	var data = eval( "(" + dataStr + ")" ) ;
	var orgId = data.orgId;
	$("#network_repair_resource_orgId").val(orgId);
	var orgId = $("#network_repair_resource_orgId").val();
	createOrgResourceDiv( $("#repair_divide_resource_div") , projectId , "c", true );															
	createOrgLineDiv( $("#repair_divide_line_div") , projectId , "d", true );	
	loadRepairNetworkRes(projectId);
}



/***************** 维护组织划分 end ********************/



/**************** 美工代码 begin ******************/
$(function(){
	// 项目网络资源-地理区域树
	$("#networkAreaTree").treeview({
		collapsed: false,
		animated: "fast"
	});
	//维护组织划分-地理区域树
	$("#orgAreaTree").treeview({
		collapsed: true,
		animated: "fast"
	});
	//服务商维护组织树
	$("#serviceOrgTree").treeview({
		collapsed: true,
		animated: "fast"
	});
	//伸缩展示
	$(".projectSserve_content li h2").click(function(){
		if($(this).siblings(".projectSserve_info").length > 0) {
			var $ishidden = false;
			if ($(this).siblings(".projectSserve_info").is(":hidden")) {
				$ishidden = true;
			}
			if ($ishidden) {
				$(".projectSserve_content li .projectSserve_info").slideUp("normal");
				$(this).siblings(".projectSserve_info").slideDown("normal");
				$(".projectSserve_content li h2 em.parent").css("background", "url(images/ico_hide.gif) no-repeat");
				$(this).children("em").css("background", "url(images/ico_show.gif) no-repeat");
			} else {
				$(this).siblings(".projectSserve_info").slideUp("normal");
				$(this).children("em").css("background", "url(images/ico_hide.gif) no-repeat");
			}
		} else {
			$(".projectSserve_content li .projectSserve_info").slideUp("normal");
			$(".projectSserve_content li h2 em.parent").css("background","url(images/ico_hide.gif) no-repeat");
		}
	});
})

/****************** 美工代码 end *******************/