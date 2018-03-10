/**
 *扩展验证框架里面没有的方法
 */
 
 //修改默认的提示消息
  jQuery.extend(jQuery.validator.messages, {
  required: "必填字段",
  remote: "请修正该字段",
  email: "输入格式错误",
  url: "输入格式错误",
  date: "输入格式错误",
  dateISO: "输入格式错误",
  number: "请输入数字",
  digits: "请输入整数",
  creditcard: "请输入合法的信用卡号",
  equalTo: "请再次输入相同的值",
  accept: "请输入拥有合法后缀名的字符串",
  maxlength: jQuery.validator.format("最多 {0} 字符"),
  minlength: jQuery.validator.format("最少 {0} 字符"),
  rangelength: jQuery.validator.format("长度介于 {0} 和 {1} 字符"),
  range: jQuery.validator.format("请输入一个介于 {0} 和 {1} 之间的值"),
  max: jQuery.validator.format("请输入一个最大为 {0} 的值"),
  min: jQuery.validator.format("请输入一个最小为 {0} 的值"),
  
  //自定义
  chineseNotAllowed: jQuery.validator.format("内容不能有中文")
  
});
  
  
 //不能有中文
jQuery.validator.addMethod("chineseNotAllowed", function(value, element, param) {
	 var re = /[\u4E00-\u9FA5]/g;
	 if(re.test(value)){
		 return false;
	 }else{
		 return true;
	 }
}); 


 //日期比较验证
jQuery.validator.addMethod("compareDate",
	function(value, element, param) {
	    
		var startDate = $(param).val();
		
        return startDate < value;
	},
	"结束时间必须大于开始时间");

//练习结束时间与考试开始时间比较验证
jQuery.validator.addMethod("compareExamDate",
	function(value, element, param) {
		var endDate = $(param).val();
        return endDate < value;
	},
	"考试开始时间比须大于练习结束时间 !");

jQuery.validator.addMethod("compareWorkOrderDate",
	function(value, element, param) {//value  故障发生时间
	   var startDate = $(param).val();
	   var dateSelect = value.split("-");
	   var dateNow = startDate.split("-");
	   var flag
	   if(dateSelect[0]<dateNow[0]){
		   flag = true;
	   }else if(dateSelect[0]>dateNow[0]){
		   flag = false;
	   }else if(dateSelect[0]==dateNow[0]){
		   if(dateSelect[1]<dateNow[1]){
			   flag = true;
		   }else if(dateSelect[1]>dateNow[1]){
			   flag = false;
		   }else if(dateSelect[1]==dateNow[1]){
			   if(dateSelect[2].split(" ")[0]<=dateNow[2].split(" ")[0]){
				   flag = true;
			   }else if(dateSelect[2].split(" ")[0]>dateNow[2].split(" ")[0]){
				   flag = false;
			   }
		   }
	   }
        return flag;
	},
	"必须小于当前时间");
	
	
jQuery.validator.addMethod("lessThanNowTime",
	function(value, element, param) {
	   var startDate = $(param).val();
        return startDate >= value;
	},
	"必须小于当前时间");	

jQuery.validator.addMethod("compareDealDate",
	function(value, element, param) {//value  故障发生时间
	   var startDate = $(param).val();
        return startDate < value;
	},
	"不能小于当前时间");
	
jQuery.validator.addMethod("compareDealDate1",
	function(value, element, param) {//value  故障发生时间
	   var startDate = $(param).val();
        return startDate <= value;
	},
	"不能小于当前时间");

jQuery.validator.addMethod("validateIsEmpty", function(value, element, param) {
	if(value==""||value=="请选择"||value=="-请选择-"){
		return false;
	}else{
		return true;
	}
}, "必选字段");

// 下拉框验证
jQuery.validator.addMethod("selectNone", function(value, element, param) {
	return value != "";
}, "必须");

//数字验证
jQuery.validator.addMethod("validateNumber", function(value, element, param) {
	return (/^\d+(\.\d+)?$/).test(value);
}, "输入数字"); 




//复选框验证
jQuery.validator.addMethod("validateCheckbox", function(value, element, param) {
	var isSelect=false;
	var elementList=$("input[name='"+value+"']");
	for(var i=0;i<elementList.length;i++){
		var checkbox=elementList[i];
		var isChecked=checkbox.checked;
		if(checkbox.checked){
			isSelect=true;
		}
	}
	if(isSelect){
		return true;
	}else{
		return false;
	}
}, "必选字段");
//加强email对全角字符的判断
jQuery.validator.addMethod("uemail", function(value, element) {
	//允许特殊字符用户名和中文域名
    //var reg =  /^((([a-z]|\d|[!#\$%&'\*\+\-\/=\?\^_`{\|}~]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])+(\.([a-z]|\d|[!#\$%&'\*\+\-\/=\?\^_`{\|}~]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])+)*)|((\x22)((((\x20|\x09)*(\x0d\x0a))?(\x20|\x09)+)?(([\x01-\x08\x0b\x0c\x0e-\x1f\x7f]|\x21|[\x23-\x5b]|[\x5d-\x7e]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(\\([\x01-\x09\x0b\x0c\x0d-\x7f]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF]))))*(((\x20|\x09)*(\x0d\x0a))?(\x20|\x09)+)?(\x22)))@((([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.)+(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.?$/;
	//允许特殊字符用户名
    //var reg = /^((([a-z]|\d|[!#\$%&'\*\+\-\/=\?\^_`{\|}~]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])+(\.([a-z]|\d|[!#\$%&'\*\+\-\/=\?\^_`{\|}~]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])+)*)|((\x22)((((\x20|\x09)*(\x0d\x0a))?(\x20|\x09)+)?(([\x01-\x08\x0b\x0c\x0e-\x1f\x7f]|\x21|[\x23-\x5b]|[\x5d-\x7e]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(\\([\x01-\x09\x0b\x0c\x0d-\x7f]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF]))))*(((\x20|\x09)*(\x0d\x0a))?(\x20|\x09)+)?(\x22)))@([a-zA-Z0-9]+(-[a-zA-Z0-9]+)*(\.[a-zA-Z0-9]+(-[a-zA-Z0-9]+)*)*(\.[a-zA-Z]{2,4}))$/;
    //严格控制用户名和域名
    var reg = /^[a-zA-Z0-9]+[_a-zA-Z0-9-]*(\.[_a-zA-Z0-9-]+)*@([a-zA-Z0-9]+(-[a-zA-Z0-9]+)*(\.[a-zA-Z0-9]+(-[a-zA-Z0-9]+)*)*(\.[a-zA-Z]{2,4}))$/;
    return  this.optional(element) || (reg.test(value));
}, "请输入正确的邮箱");
//不能重复
jQuery.validator.addMethod( "notEqualTo", function( value, element, param ) {
	return this.optional(element) || !$.validator.methods.equalTo.call( this, value, element, param );
}, "不能输入重复值" );