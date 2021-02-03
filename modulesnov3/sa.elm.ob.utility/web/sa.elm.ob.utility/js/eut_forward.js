$("#inpUser").select2(selectBoxAjaxPaging({
  url: function() {
    return contextpath + '/sa.elm.ob.utility.ad_process.Forward/Forward?inpAction=getUserList&inpRecordId=' + document.getElementById("inpRecordId").value + '&inpwindowId=' + document.getElementById("inpwindowId").value
  },
  size: "small"
}));


function getRoleList(userId) {
  getAllRoles(userId);
  getUserDept(userId);
  getUserPosition(userId);
}

function getAllRoles(userId) {
  $.ajax({
    type: 'GET',
    url: contextpath + '/sa.elm.ob.utility.ad_process.Forward/Forward?inpAction=getDefaultRole&inpUserId=' + userId + '&inpRecordId=' + document.getElementById("inpRecordId").value + '&inpwindowId=' + document.getElementById("inpwindowId").value,
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
         return contextpath + '/sa.elm.ob.utility.ad_process.Forward/Forward?inpAction=getUserRoles&inpUserId=' + userId + '&inpRecordId=' + document.getElementById("inpRecordId").value + '&inpwindowId=' + document.getElementById("inpwindowId").value
       },
       size: "small"
     }));
     $("#inpRole").on("select2:unselecting", function(e) {
       document.getElementById("inpRole").options.length = 0;
     });
     },100);

     if(defaultRoleId!=null && defaultRoleName!=""){
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

function getUserDept(userId) {

  $.ajax({
    type: 'GET',
    url: contextpath + '/sa.elm.ob.utility.ad_process.Forward/Forward?inpAction=getUserDept&inpUserId=' + userId + '&inpRecordId=' + document.getElementById("inpRecordId").value + '&inpwindowId=' + document.getElementById("inpwindowId").value,
    dataType: 'json',
    async: false,
    success: function(data) {
      if (Object.keys(data).length > 0) {
        document.getElementById("inpdept").value = data.DeptName;
      } else {
        document.getElementById("inpdept").value = "";
      }
    }
  });
}

function getUserPosition(userId) {

  $.ajax({
    type: 'GET',
    url: contextpath + '/sa.elm.ob.utility.ad_process.Forward/Forward?inpAction=getUserPosition&inpUserId=' + userId + '&inpRecordId=' + document.getElementById("inpRecordId").value + '&inpwindowId=' + document.getElementById("inpwindowId").value,
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
		url: contextpath + '/sa.elm.ob.utility.ad_process.Forward/Forward?inpAction=checkPreference&inpUser='+ document.getElementById("inpUser").value +'&inpRole='+ document.getElementById("inpRole").value+'&inpRecordId='+ document.getElementById("inpRecordId").value +'&inpwindowId='+ document.getElementById("inpwindowId").value,
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

function checkOrgAccess() {
	var checkOrgAccess = false;
	$.ajax({
		type: 'GET', 
		url: contextpath + '/sa.elm.ob.utility.ad_process.Forward/Forward?inpAction=checkForwardToUserOrgAccess&inpRole='+ document.getElementById("inpRole").value+'&inpRecordId='+ document.getElementById("inpRecordId").value +'&inpwindowId='+ document.getElementById("inpwindowId").value,
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
		url: contextpath + '/sa.elm.ob.utility.ad_process.Forward/Forward?inpAction=checkUserValidation&inpUser='+ document.getElementById("inpUser").value+'&inpRecordId='+ document.getElementById("inpRecordId").value +'&inpwindowId='+ document.getElementById("inpwindowId").value,
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

function forward() {
   document.getElementById("Submit_linkBTN").disabled=true;
  var employee = document.getElementById("inpUser").value;
  var torole = document.getElementById("inpRole").value;
  if (employee == '0' || employee == "") {
    OBAlert(empStatus);
    return false;
  }
  if (torole == '0' || torole == "") {
    OBAlert(empRole);
    return false;
  }
  var checkPref = checkPreference();
  var orgAccess = checkOrgAccess();
 //var checkValidUser = checkUserValidation();
//if(checkValidUser){
  if(checkPref && orgAccess ){
	  var url = "inpToUser=" + document.getElementById("inpUser").value;
	  url += "&inpToRole=" + document.getElementById("inpRole").value;
	  url += "&inpRecordId=" + document.getElementById("inpRecordId").value;
	  url += "&inpwindowId=" + document.getElementById("inpwindowId").value;
	  url += "&inpAction=insertRecord";
	  //submitCommandForm('DEFAULT', true, null, contextpath+'/sa.elm.ob.utility.ad_process.Forward/Forward?inpAction=insertRecord&'+url, '_self', null, true);
	  //return contextpath+'/sa.elm.ob.utility.ad_process.Forward/Forward?inpAction=insertRecord&'+url
	  document.frmMain.action = contextpath + '/sa.elm.ob.utility.ad_process.Forward/Forward?' + url;
	  document.frmMain.submit();
	  //return false;
  }
//}
}