
$("#inpUser").select2(selectBoxAjaxPaging({
  url: function() {
    return contextpath + '/sa.elm.ob.utility.ad_process.RequestMoreInformation/RequestMoreInformation?inpAction=getUserList&inpRecordId=' + document.getElementById("inpRecordId").value + '&inpwindowId=' + document.getElementById("inpwindowId").value
  },
  size: "small"
}));
$("#inpUser").on("select2:unselecting", function(e) {
  document.getElementById("inpUser").options.length = 0;
  document.getElementById("inpRole").options.length = 0;
});

function getRoleList(userId) {
	 var checkBox = document.getElementById("nestedRMICheck");
	 if (checkBox.checked == false && !checkRmiId){
    	 getAllResponseUser(userId);
     }
     else{
    	 getAllRoles(userId);
     }
  getUserDept(userId);
  getUserPosition(userId);
}

function getAllRoles(userId) {
  $.ajax({
    type: 'GET',
    url: contextpath + '/sa.elm.ob.utility.ad_process.RequestMoreInformation/RequestMoreInformation?inpAction=getDefaultRole&inpUserId=' + userId + '&inpRecordId=' + document.getElementById("inpRecordId").value + '&inpwindowId=' + document.getElementById("inpwindowId").value,
    dataType: 'json',
    async: false,
    success: function(data) {
      var defaultRoleId = "",
          defaultRoleName = "";
      document.getElementById("inpRole").options.length = 0;
      if (Object.keys(data).length > 0) {
        defaultRoleId = data.defaultRoleId;
        defaultRoleName = data.defaultRoleName;
      }
      $("#inpRole").val('').trigger('change');
setTimeout(function () {
      $("#inpRole").select2(selectBoxAjaxPaging({
        url: function() {
          return contextpath + '/sa.elm.ob.utility.ad_process.RequestMoreInformation/RequestMoreInformation?inpAction=getUserRoles&inpUserId=' + userId + '&inpRecordId=' + document.getElementById("inpRecordId").value + '&inpwindowId=' + document.getElementById("inpwindowId").value
        },
        size: "small"
      }));
      $("#inpRole").on("select2:unselecting", function(e) {
        document.getElementById("inpRole").options.length = 0;
      });
},100);

var checkBox = document.getElementById("nestedRMICheck");
 if(defaultRoleId!=null && defaultRoleName!="" && 
		 (document.getElementById("selectRoleId").value==null || document.getElementById("selectRoleId").value=="null" || document.getElementById("selectRoleId").value==""||  checkBox.checked == true)
		 ){
   var data = [{
       id: defaultRoleId,
       text: defaultRoleName
   }];
   $("#inpRole").select2({
       data: data
   });
    }
}
  });
}

function getAllResponseUser(userId) {
	 document.getElementById("inpRole").options.length = 0;
	 $.ajax({
        type: 'POST', 
        url: contextpath + '/sa.elm.ob.utility.ad_process.RequestMoreInformation/RequestMoreInformation?inpAction=getResponseUserRoles',
        data: {  
       	 inpUser:userId,
            inpRecordId:document.getElementById("inpRecordId").value,
            inpwindowId:document.getElementById("inpwindowId").value
        },
        dataType: 'json',
        async: false
    }).done(function (response) {
    	 var data= response.List;
         if(data.length>0){
       	  $("#inpRole").select2({
                 data: data
               });
       	  for(obj in data){
       		responseRoleId= data[0].id;
       	  }
         }
         getRequestDetails(userId,responseRoleId);
    });
	}

function  getRequestDetails(userId,roleId){
	 $.ajax({
	        type: 'POST', 
	        url: contextpath + '/sa.elm.ob.utility.ad_process.RequestMoreInformation/RequestMoreInformation?inpAction=getRmiRequestDetails',
	        data: {  
	       	 inpUser:userId,
	            inpRecordId:document.getElementById("inpRecordId").value,
	            inpwindowId:document.getElementById("inpwindowId").value,
	            inpRole:roleId,
	        },
	        dataType: 'json',
	        async: false
	    }).done(function (response) {
	    	document.getElementById("inprequest").value = response.reqMessage;
	    });
}

function getUserDept(userId) {

  $.ajax({
    type: 'GET',
    url: contextpath + '/sa.elm.ob.utility.ad_process.RequestMoreInformation/RequestMoreInformation?inpAction=getUserDept&inpUserId=' + userId + '&inpRecordId=' + document.getElementById("inpRecordId").value + '&inpwindowId=' + document.getElementById("inpwindowId").value,
    dataType: 'json',
    async: false,
    success: function(data) {
      if (Object.keys(data).length > 0) {
        document.getElementById("inpdepartment").value = data.DeptName;
      } else {
        document.getElementById("inpdepartment").value = "";
      }
    }
  });
}
function getUserPosition(userId) {

	  $.ajax({
	    type: 'GET',
	    url: contextpath + '/sa.elm.ob.utility.ad_process.RequestMoreInformation/RequestMoreInformation?inpAction=getUserPosition&inpUserId=' + userId + '&inpRecordId=' + document.getElementById("inpRecordId").value + '&inpwindowId=' + document.getElementById("inpwindowId").value,
	    dataType: 'json',
	    async: false,
	    success: function(data) {
	      if (Object.keys(data).length > 0) {
	        document.getElementById("inpposition").value = data.PositionName;
	      } else {
	        document.getElementById("inpposition").value = "";
	      }
	    }
	  });
}
function checkPreference() {
	var checkPreferenceId = false;
	$.ajax({
		type: 'GET', 
		url: contextpath + '/sa.elm.ob.utility.ad_process.RequestMoreInformation/RequestMoreInformation?inpAction=checkPreference&inpUser='+ document.getElementById("inpUser").value +'&inpRole='+ document.getElementById("inpRole").value+'&inpRecordId='+ document.getElementById("inpRecordId").value +'&inpwindowId='+ document.getElementById("inpwindowId").value,
		data: {  
		},
		dataType: 'json',
		async: false
	}).done(function (response) {
		checkPreferenceId = response.errorMsg;
	});
	if(checkPreferenceId){
		OBAlert(disablePreference);
		return false;
	}
	return true;
}

function checkPreValidation() {
	var checkPreValidation = false;
	var strResponse = document.getElementById("inpresponse").value;
	
	$.ajax({
		type: 'POST', 
		url: contextpath + '/sa.elm.ob.utility.ad_process.RequestMoreInformation/RequestMoreInformation?inpAction=checkPreValidation&inpUser='+ document.getElementById("inpUser").value +'&inpRole='+ document.getElementById("inpRole").value+'&inpRecordId='+ document.getElementById("inpRecordId").value +'&inpwindowId='+ document.getElementById("inpwindowId").value,
		data: {  
			strRes:strResponse
		},
		dataType: 'json',
		async: false
	}).done(function (response) {
		checkPreValidation = response.checkValidation;
	});
	if(!checkPreValidation){
		OBAlert(checkPreValidations);
		return false;
	}
	return true;
}

function checkIsRmiRevoked() {
	var isrmiRevoked = false;
	$.ajax({
		type: 'GET', 
		url: contextpath + '/sa.elm.ob.utility.ad_process.RequestMoreInformation/RequestMoreInformation?inpAction=checkIsRmiAlreadyRevoked&inpUser='+ document.getElementById("inpUser").value +'&inpRole='+ document.getElementById("inpRole").value+'&inpRecordId='+ document.getElementById("inpRecordId").value +'&inpwindowId='+ document.getElementById("inpwindowId").value+'&strRes='+ document.getElementById("inpresponse").value,
		data: {  
		},
		dataType: 'json',
		async: false
	}).done(function (response) {
		isrmiRevoked = response.checkIsRevoked;
	});
	if(isrmiRevoked){
		OBAlert(checkPreValidations);
		return false;
	}
	return true;
}
function checkOrgAccess() {
	var checkOrgAccess = false;
	$.ajax({
		type: 'GET', 
		url: contextpath + '/sa.elm.ob.utility.ad_process.RequestMoreInformation/RequestMoreInformation?inpAction=checkRmiToRoleOrgAccess&inpRole='+ document.getElementById("inpRole").value+'&inpRecordId='+ document.getElementById("inpRecordId").value +'&inpwindowId='+ document.getElementById("inpwindowId").value,
		data: {  
		},
		dataType: 'json',
		async: false
	}).done(function (response) {
		checkOrgAccess = response.haveOrgAccess;
	});
	if(!checkOrgAccess){
		OBAlert(noOrgAccess);
		return false;
	}
	return true;
}

function checkUserValidation() {
	var checkUserValidation = false;
	$.ajax({
		type: 'GET', 
		url: contextpath + '/sa.elm.ob.utility.ad_process.RequestMoreInformation/RequestMoreInformation?inpAction=checkUserValidation&inpUser='+ document.getElementById("inpUser").value+'&inpRecordId='+ document.getElementById("inpRecordId").value +'&inpwindowId='+ document.getElementById("inpwindowId").value,
		data: {  
		},
		dataType: 'json',
		async: false
	}).done(function (response) {
		checkUserValidation = response.errorMsg;
	});
	if(checkUserValidation){
		//OBAlert(validUser);
		return false;
	}
	return true;
}

function sendRequestMoreinfo() {
	document.getElementById("Submit_linkBTN").disabled=true;
  if (document.getElementById("inpUser").value == '0' || document.getElementById("inpUser").value == "") {
    OBAlert(empStatus);
    return false;
  }
  if (document.getElementById("inpRole").value == '0' || document.getElementById("inpRole").value == "") {
    OBAlert(empRole);
    return false;
  }
  if(checkRmiId || (!checkRmiId && document.getElementById("nestedRMICheck").checked==true)){
  	  if((document.getElementById("inprequest").value==null)||(document.getElementById("inprequest").value=='')){
  	    	 OBAlert(RequestFieldIsMandatory);
  	    	    return false;
  	     }
    }
if(!checkRmiId && document.getElementById("nestedRMICheck").checked==false){
  	  if((document.getElementById("inpresponse").value==null)||(document.getElementById("inpresponse").value=='')){
  	    	 OBAlert(ResponseFieldIsMandatory);
  	    	    return false;
  	     }
    }
var checkPref = checkPreference();
var checkPreValidate = checkPreValidation();
var orgAccess = checkOrgAccess();
//var checkValidUser = checkUserValidation();
//var checkIsRmiRevoke = checkIsRmiRevoked();

  if(checkPreValidate && orgAccess){ // && checkIsRmiRevoke /*&& checkValidUser*/
	  if(checkPref){
		  
		  
		  var url = "inpToUser=" + document.getElementById("inpUser").value;
		  url += "&inpToRole=" + document.getElementById("inpRole").value;
		  url += "&inpRecordId=" + document.getElementById("inpRecordId").value;
		  url += "&inpwindowId=" + document.getElementById("inpwindowId").value;
		  if(document.getElementById("nestedRMICheck").checked==false){
			  url += "&inpnestedRmi=N";
		  }
		  else{
			  url += "&inpnestedRmi=Y";
		  }
		  url += "&inpAction=insertRecord";
		  
		  //submitCommandForm('DEFAULT', true, null, contextpath+'/sa.elm.ob.utility.ad_process.Forward/Forward?inpAction=insertRecord&'+url, '_self', null, true);
		  //return contextpath+'/sa.elm.ob.utility.ad_process.Forward/Forward?inpAction=insertRecord&'+url
		  document.frmMain.action = contextpath + '/sa.elm.ob.utility.ad_process.RequestMoreInformation/RequestMoreInformation?' + url;
		  document.frmMain.submit();
	  }
	  }
	
}