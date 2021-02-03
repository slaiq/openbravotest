// Core Variables
// var BigDecimal = parent.parent.parent.parent.BigDecimal;
// var OB = parent.parent.parent.parent.OB;
//Add Common CSS
var css = document.createElement("link");
css.setAttribute("rel", "stylesheet");
css.setAttribute("type", "text/css");
css.setAttribute("href", "../web/js/common/common.css");
document.getElementsByTagName('head')[0].appendChild(css);
// Add OBAlert JS
var script = document.createElement('script');
script.src = "../web/js/common/OBAlert.js";
script.type = 'text/javascript';
document.getElementsByTagName('head')[0].appendChild(script);
// Add Json JS
var script = document.createElement('script');
script.src = "../web/js/common/json2.js";
script.type = 'text/javascript';
document.getElementsByTagName('head')[0].appendChild(script);
// Add Json JS
var script = document.createElement('script');
script.src = "../web/js/common/DateUtil.js";
script.type = 'text/javascript';
document.getElementsByTagName('head')[0].appendChild(script);
// Add TreeGrid CSS
css = document.createElement("link");
css.setAttribute("rel", "stylesheet");
css.setAttribute("type", "text/css");
css.setAttribute("href", "../web/Grid/AddiGridCSS.css");
document.getElementsByTagName('head')[0].appendChild(css);

function generateUUID() {
	var d = new Date().getTime();
	var uuid = 'xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
		var r = (d + Math.random() * 16) % 16 | 0;
		d = Math.floor(d / 16);
		return (c == 'x' ? r : (r & 0x7 | 0x8)).toString(16);
	});
	return uuid.toUpperCase();
};
// HTTP Object Creation Method
function getXMLObject() {
	var httpReq = false;
	try {
		httpReq = new ActiveXObject("Msxml2.XMLHTTP");
	} catch (e) {
		try {
			httpReq = new ActiveXObject("Microsoft.XMLHTTP");
		} catch (e2) {
			httpReq = false;
		}
	}
	if (!httpReq && typeof XMLHttpRequest != 'undefinedss') {
		httpReq = new XMLHttpRequest();
	}
	return httpReq;
}
// TreeGrid
function removeTreeGridMessage() {
	if (navigator.appName.toLowerCase().indexOf("microsoft") != -1) {
		$("table.GMMainTable").prev().remove();
		addCSS("table.GMSection tr:first-child { display: block !important; }");
	}
	$("span:contains('EJS TreeGrid')").remove();
	setTimeout('removeDisplay()', 1);
}
// Removing Trial Message, Disable RightClickOption
function removeDisplay() {
	if ($('div[onmousemove="this.parentNode.parentNode.removeChild(this.parentNode);"]'))
		$('div[onmousemove="this.parentNode.parentNode.removeChild(this.parentNode);"]').remove();
	SetEvent("OnRightClick", null, function() {
		return true;
	});
}
// Remove All Grid in a Page
function removeTreeGrid() {
	DisposeGrids();
}
function addCSS(code) {
	var obj = document.createElement("style");
	obj.type = "text/css";
	if (obj.styleSheet)
		obj.styleSheet.cssText = code;
	else
		obj.appendChild(document.createTextNode(code));
	document.getElementsByTagName('head')[0].appendChild(obj);
}
// JQGrid
function changeJQGridDisplay(gridId, rowId) {
	var grid = jQuery("#" + gridId);
	if (document.getElementById("jqgh_" + gridId + "_rn"))
		document.getElementById("jqgh_" + gridId + "_rn").innerHTML = "#"; // Create
	// # in
	// Header
	if (navigator.platform.toLowerCase().indexOf("win") == 0 || navigator.userAgent.toLowerCase().indexOf("(window") == -1)
		if (navigator.appName.toLowerCase().indexOf("microsoft") == -1)
			$('div.ui-jqgrid-hdiv').css('cssText', 'border-bottom: 0px !important;');
	if (navigator.appName.toLowerCase().indexOf("microsoft") != -1)
		$(".ui-jqgrid .ui-jqgrid-bdiv").css({
			'top' : "-1px"
		});
	if (typeof rowId != "undefined" && document.getElementById(rowId))
		$(document).keydown(function(e) {
			if ((("" + $("*:focus").attr("id")) != document.getElementById(rowId).value) || document.getElementById(rowId).value == "" || isOBDialogOpen == true)
				return true;
			e = e || window.event;
			var c = e.keyCode || e.which;
			var cur = $("#" + document.getElementById(rowId).value);
			if ((c == 37 || c == 38) && cur.prev().hasClass("jqgrow")) {
				var jqGDH = $("div#gview_" + gridId + " div.ui-jqgrid-bdiv").height(), nexId = cur.prev().attr("id");
				grid.setSelection(nexId);
				grid.closest(".ui-jqgrid-bdiv").scrollTop(22 * (grid.getInd(nexId) - (parseInt(jqGDH / 44) + 1)));
				if (typeof document.getElementById("client") != "undefined")
					document.getElementById("client").scrollTop = 0;
				document.getElementById(nexId).focus();
				return false;
			}
			else if ((c == 39 || c == 40) && cur.next().hasClass("jqgrow")) {
				var jqGDH = $("div#gview_" + gridId + " div.ui-jqgrid-bdiv").height(), nexId = cur.next().attr("id");
				grid.setSelection(nexId);
				if (parseInt(jqGDH / 22) < grid.getInd(nexId))
					grid.closest(".ui-jqgrid-bdiv").scrollTop(22 * (grid.getInd(nexId) - (parseInt(jqGDH / 44) + 1)));
				if (typeof document.getElementById("client") != "undefined")
					document.getElementById("client").scrollTop = 0;
				document.getElementById(nexId).focus();
				return false;
			}
			else if (c == 46 && typeof onClickDelete === "function")
				onClickDelete();
		});
	$(window).bind('resize', function() {
		if (typeof reSizeGrid === "function")
			reSizeGrid();
	}).trigger('resize');
}
// grid = JQGrid Object (ex) jQuery("Equipment")
function ChangeJQGridAllRowColor(grid) {
	var idList = grid.getDataIDs();
	var idCount = idList.length;
	for (var i = 0; i < idCount; i++)
		if (i % 2 != 0)
			$('tr[id="' + idList[i] + '"]').addClass("DataGrid_Body_Row_Odd");
		else
			$('tr[id="' + idList[i] + '"]').addClass("DataGrid_Body_Row_Even");
}
// oldId=Last Selected Id, newId=Current Selected Id
function ChangeJQGridSelectRowColor(oldId, newId) {
	if (oldId != "") {
		$('tr[id="' + oldId + '"]').attr('style', 'border: 1px solid #CDD7BB;');
	}
	if (newId != "") {
		$('tr[id="' + newId + '"]').removeClass('ui-state-highlight');
		$('tr[id="' + newId + '"]').attr('style', 'background: none; background-color: #FFE1C0; border: 1px solid #CDD7BB;');
	}
}
function ChangeJQGridSelectRowColorMandate(oldId, newId) {
	if (oldId != "") {
		$('tr[id="' + oldId + '"]').attr('style', 'background: none; border: 1px solid #CDD7BB !important;');
	}
	if (newId != "") {
		$('tr[id="' + newId + '"]').removeClass('ui-state-highlight');
		$('tr[id="' + newId + '"]').attr('style', 'background: none; background-color: #FFE1C0 !important; border: 1px solid #CDD7BB !important;');
	}
}
// type S -> Select US-> UnSelect
function ChangeJQGridSelectMultiRowColor(rowId, type) {
	if (type == "S") {
		$('tr[id="' + rowId + '"]').removeClass('ui-state-highlight ui-widget-content');
		$('tr[id="' + rowId + '"]').attr('style', 'background: none; background-color: #FFE1C0; border: 1px solid #CDD7BB;');
	}
	else if (type == "US") {
		$('tr[id="' + rowId + '"]').attr('style', 'border: 1px solid #CDD7BB;');
	}
}
function displayMessage(type, title, message) {
	var id = "messageBoxID";
	if (document.getElementById(id)) {
		if (type == "S")
			type = "MessageBoxSUCCESS";
		else if (type == "E")
			type = "MessageBoxERROR";
		else if (type == "I")
			type = "MessageBoxINFO";
		else if (type == "W")
			type = "MessageBoxWARNING";
		document.getElementById(id).style.display = "";
		document.getElementById(id).className = type;
		document.getElementById(id + "Title").innerHTML = title;
		document.getElementById(id + "Message").innerHTML = message;
	}
}
function setMessage(id, type, title, message) {
	if (document.getElementById(id)) {
		if (type == "S")
			type = "MessageBoxSUCCESS";
		else if (type == "E")
			type = "MessageBoxERROR";
		else if (type == "I")
			type = "MessageBoxINFO";
		else if (type == "W")
			type = "MessageBoxWARNING";
		document.getElementById(id).style.display = "";
		document.getElementById(id).className = type;
		document.getElementById(id + "Title").innerHTML = title;
		document.getElementById(id + "Message").innerHTML = message;
	}
}
// Key Events
function noKeyInput(e) {
	e = e || window.event;
	var charCode = e.keyCode || e.which;
	if (e.ctrlKey == true && (charCode == 68 || charCode <= 86))
		return true;
	if (e.altKey == true || (charCode >= 112 && charCode <= 123))
		return true;
	if (charCode == 9 || (e.shiftKey == true && charCode == 9))
		return true;
	return false;
}
function isIntOnKeyDown(e) {
	e = e || window.event;
	var charCode = e.keyCode || e.which;
	if (e.ctrlKey == true && (charCode == 68 || charCode <= 86))
		return true;
	if (e.altKey == true || (charCode >= 112 && charCode <= 123))
		return true;
	if (e.shiftKey == true && (charCode == 9 || charCode == 35 || charCode == 36 || charCode == 37 || charCode == 39))
		return true;
	else if (e.shiftKey == true)
		return false;
	if (typeof enableForm == 'function' && (charCode == 8 || charCode == 127 || (charCode >= 48 && charCode <= 57)))
		enableForm();
	if (charCode == 8 || charCode == 9 || charCode == 16 || charCode == 35 || charCode == 36 || charCode == 37 || charCode == 39 || charCode == 46 || charCode == 127
			|| (charCode >= 48 && charCode <= 57) || (charCode >= 96 && charCode <= 105))
		return true;
	return false;
}
function isFloatOnKeyDown(index, e) {
	e = e || window.event;
	var charCode = e.keyCode || e.which;
	if (e.ctrlKey == true && (charCode == 68 || charCode <= 86))
		return true;
	if (e.altKey == true || (charCode >= 112 && charCode <= 123))
		return true;
	if (e.shiftKey == true && (charCode == 9 || charCode == 35 || charCode == 36 || charCode == 37 || charCode == 39))
		return true;
	else if (e.shiftKey == true)
		return false;
	if ((charCode == 110 || charCode == 190) && index.value != "" && index.value.indexOf(".") != -1)
		return false;
	if (typeof enableForm == 'function' && (charCode == 8 || charCode == 190 || charCode == 127 || (charCode >= 48 && charCode <= 57)))
		enableForm();
	if (charCode == 8 || charCode == 9 || charCode == 16 || charCode == 35 || charCode == 36 || charCode == 37 || charCode == 39 || charCode == 46 || charCode == 110 || charCode == 190
			|| charCode == 127 || (charCode >= 48 && charCode <= 57) || (charCode >= 96 && charCode <= 105))
		return true;
	return false;
}
function onChangeEvent(e) {
	e = e || window.event;
	var charCode = e.keyCode || e.which;
	if (charCode == 0 || charCode == 16 || charCode == 18 || charCode == 20 || charCode == 27 || charCode == 35 || charCode == 36 || charCode == 37 || charCode == 38 || charCode == 39
			|| charCode == 40 || charCode == 45 || (charCode >= 112 && charCode <= 123) || charCode == 16 ||charCode == 17  ||charCode == 18 ||charCode == 67 )
		return true;
	if (typeof enableForm == 'function' && charCode != 9)
		enableForm();
	return true;
}
var globalRecordId, globalFormId;
function enableQDMAttachmentIcon(recordId, formId, flag, contextPath) {
	if (document.getElementById('buttonQDM_Attachment')) {
		globalRecordId = recordId;
		globalFormId = formId;
		ajaxReq = getXMLObject();
		if (flag == true && ajaxReq) {
			ajaxReq.open("GET", contextPath + "/AttachmentAjax?action=GetAttachmentCount&FormId=" + formId + "&RecordId=" + recordId, false);
			ajaxReq.onreadystatechange = function() {
				if (ajaxReq.readyState == 4 && ajaxReq.status == 200) {
					var countList = ajaxReq.responseXML.getElementsByTagName("AttachmentCount");
					if (countList.length > 0) {
						for (var i = 0; i < countList.length; i++) {
							var count = countList[i].getElementsByTagName("Count")[0].childNodes[0].nodeValue;
							if (flag == true && count > 0) {
								document.getElementById('linkButtonQDM_Attachment').className = 'Main_ToolBar_Button';
								document.getElementById('buttonQDM_Attachment').className = 'Main_ToolBar_Button_Icon Main_ToolBar_Button_Icon_AttachmentExists';
							}
							else if (flag == true && count == 0) {
								document.getElementById('linkButtonQDM_Attachment').className = 'Main_ToolBar_Button';
								document.getElementById('buttonQDM_Attachment').className = 'Main_ToolBar_Button_Icon Main_ToolBar_Button_Icon_Attachment';
							}
							else if (flag == false) {
								document.getElementById('linkButtonQDM_Attachment').className = 'Main_ToolBar_Button_disabled';
								document.getElementById('buttonQDM_Attachment').className = 'Main_ToolBar_Button_Icon Main_ToolBar_Button_Icon_Attachment_disabled';
							}
						}
					}
				}
			};
			ajaxReq.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
			ajaxReq.send(null);
		}
		else if (flag == false) {
			document.getElementById('linkButtonQDM_Attachment').className = 'Main_ToolBar_Button_disabled';
			document.getElementById('buttonQDM_Attachment').className = 'Main_ToolBar_Button_Icon Main_ToolBar_Button_Icon_Attachment_disabled';
		}
	}
}

function openQDMAttachmentGlobal() {
	if (document.getElementById('buttonQDM_Attachment')) {
		if (document.getElementById('linkButtonQDM_Attachment').className == 'Main_ToolBar_Button') {
			var recordId = globalRecordId;
			var formId = globalFormId;
			var action = 'openAttachmentDialog';
			var OB = parent.parent.parent.parent.OB;
			OB.Layout.ClassicOBCompatibility.Popup.open(this.id, 900, 650, OB.Utilities.applicationUrl('com.qualiantech.documentmanagement.ad_forms.attachment.header/Attachment') + '?Command='
					+ 'DEFAULT' + '&inpRecordId=' + recordId + '&action=' + action + '&inpTabId=null' + '&inpFormID=' + formId + '&pageType=Grid', '', null, false, false, true);
		}
	}
}
function formatTime(d) {
	return (parseInt(d) < 10) ? '0' + d : d;
}
function convertMinutetoTimeString(val) {
	if (isNaN(val))
		return '00:00';
	else
		return (formatTime(parseInt(val / 60)) + ':' + formatTime(val % 60));
}
function convertTimeStringToMinutes(val) {
	try {
		var str = val.split(":");
		return (parseInt(str[0]) * 60 + parseInt(str[1]));
	} catch (e) {
		return 0;
	}
}
function isTimeOnKeyDown(index, e) {
	e = e || window.event;
	var charCode = e.keyCode || e.which;
	if (charCode == 186 && index.value != "" && index.value.indexOf(":") != -1)
		return false;
	if (e.ctrlKey == true && (charCode == 68 || charCode <= 86))
		return true;
	if (e.altKey == true || (charCode >= 112 && charCode <= 123))
		return true;
	if (e.shiftKey == true && (charCode == 9 || charCode == 35 || charCode == 36 || charCode == 37 || charCode == 39 || charCode == 186))
		return true;
	else if (e.shiftKey == true)
		return false;
	if (charCode == 8 || charCode == 9 || charCode == 16 || charCode == 35 || charCode == 36 || charCode == 37 || charCode == 39 || charCode == 46 || charCode == 186 || charCode == 127
			|| (charCode >= 48 && charCode <= 57) || (charCode >= 96 && charCode <= 105))
		return true;
	return false;
}
function onBlurTimeField(index, type24Hrs) {
	var val = index.value;
	if (isNaN(val.replace(':', '')) || val == '')
		index.value = '00:00';
	else {
		if (val.indexOf(":") != -1) {
			var valArr = val.split(":");
			var val1 = parseInt(valArr[0] == '' ? '0' : valArr[0]);
			var val2 = parseInt(valArr[1] == '' ? '0' : (valArr[1].length > 2 ? valArr[1].substring(0, 2) : valArr[1]));
			if (type24Hrs && val1 >= 24) {
				index.value = '23:59';
				return;
			}
			if (isNaN(val1) || val2 == 60) {
				index.value = (val1 < 10 ? ('0' + val1) : val1) + ":00";
			}
			else if (parseInt(val2) >= 60) {
				index.value = (val1 < 10 ? ('0' + val1) : val1) + ":00";
			}
			else if (val2 < 10) {
				index.value = (val1 < 10 ? ('0' + val1) : val1) + ":0" + val2;
			}
			else {
				index.value = (val1 < 10 ? ('0' + val1) : val1) + ":" + (val2 < 10 ? ('0' + val2) : val2);
			}
		}
		else if (val.indexOf(":") == -1) {
			val = parseInt(val);
			if (type24Hrs && val >= 24) {
				index.value = '23:59';
				return;
			}
			index.value = (val < 10 ? ('0' + val) : val) + ':00';
		}
	}
}
function escapeQuote(val) {
	if (val == null || '' == val)
		return '';
	return val.replace(/\"/g, "&quot;").replace(/\'/g, "&#039;");
}
function escapeHTML(val) {
	if (val == null || '' == val)
		return '';
	var result = '';
	val = val.trim();
	for (var i = 0; i < val.length; i++) {
		var partial = val[i].charCodeAt(0).toString(16);
		while (partial.length !== 4)
			partial = "0" + partial;
		result += "\\u" + partial;
	}
	return result.replace(/\\u/g, "&#x");
}
function unescapeHTML(val) {
	var e = document.getElementById('inpEscapeHTMLValue');
	if (!e) {
		var ele = document.createElement("div");
		ele.id = 'inpEscapeHTMLValue';
		ele.style.display = "none";
		document.body.appendChild(ele);
		e = document.getElementById('inpEscapeHTMLValue');
	}
	e.innerHTML = ('' + ((!val || val == null) ? '' : val)).trim();
	return e.innerHTML.replace(/&amp;/g, "&").replace(/&lt;/g, "<").replace(/&gt;/g, ">").replace(/&quot;/g, "\"").replace(/&#039;/g, "'");
}
function convertASCIItoString(value) {
	var baseStr = "";
	if (value != null)
		for (var i = 0; i < value.length; i++)
			if (value.charAt(i) == '&' && value.charAt(i + 1) == '#') {
				var s = value.substring(i + 2, value.indexOf(";", i + 1));
				try {
					if (parseInt(s) > 0) {
						baseStr += String.fromCharCode(s);
						i += s.length + 2;
					}
				} catch (e) {
					baseStr += value.charAt(i);
				}
			}
			else
				baseStr += value.charAt(i);
	return baseStr;
}
function setTextBoxValue(val, oEle) {
	var e = document.getElementById('inpEscapeHTMLValue');
	if (!e) {
		var ele = document.createElement("div");
		ele.id = 'inpEscapeHTMLValue';
		document.body.appendChild(ele);
		e = document.getElementById('inpEscapeHTMLValue');
	}
	e.innerHTML = val.trim();
	if (typeof oEle != "undefined" && oEle && oEle.getAttribute('value'))
		oEle.value = e.innerHTML;
	return e.innerHTML;
}
// jQuery based Methods
function findJSONObj(array, property, value) {
	var ret = false;
	$.each(array, function(index, result) {
		if (result[property] && result[property] == value) {
			ret = true;
			return false;
		}
	});
	return ret;
}
function findAndUpdateJSONObj(array, rowCol, rowId, property, value) {
	$.each(array, function(index, result) {
		if (result[rowCol] && result[rowCol] == rowId) {
			result[property] = value;
			return false;
		}
	});
}
function findAndRemoveJSONObj(array, property, value) {
	$.each(array, function(index, result) {
		if (result[property] && result[property] == value) {
			array.splice(index, 1);
			return false;
		}
	});
}
function findAndReturnJSONValue(array, property1, value1, property2) {
	var value2 = "";
	$.each(array, function(index, result) {
		if (result[property1] && result[property1] == value1) {
			value2 = result[property2];
			return false;
		}
	});
	return value2;
}
function initiateSelect2Box() {
	// Add JQuery Select2 CSS
	var css = document.createElement("link");
	css.setAttribute("rel", "stylesheet");
	css.setAttribute("type", "text/css");
	css.setAttribute("href", "../web/js/common/select2.min.css");
	document.getElementsByTagName('head')[0].appendChild(css);
	// Add JQuery Select2 JS
	var script = document.createElement('script');
	script.src = "../web/js/common/select2.min.js";
	script.type = 'text/javascript';
	document.getElementsByTagName('head')[0].appendChild(script);
}
//check date validation
function checkdatevalidation(startValue,endValue,fillstartenddate,endgtstartdate) 

{   
	
	if((startValue.length=='0')||endValue.length=='0')
		{
		
		OBAlert(fillstartenddate);
		
		return false;
		}
	
	var fdate = startValue.split("-");
	var fromdate=new Date();
	     
	fromdate.setDate(fdate[0]);
	fromdate.setMonth(fdate[1]-1);
	fromdate.setFullYear(fdate[2]);

	 
	var tdte = endValue.split("-");
	var todate=new Date();
	     
	todate.setDate(tdte[0]);
	todate.setMonth(tdte[1]-1);
	todate.setFullYear(tdte[2]);
	
 
	if (fromdate.getTime() > todate.getTime())
	{

		OBAlert(endgtstartdate);
		
		 return 1;
	} 
	else
		{
		return 0;
		}
}
function dateCompare(fromDate,toDate) 
{   
	
	
	var fdate = fromDate.split("-");
	var fromdate=new Date();
	     
	fromdate.setDate(fdate[0]);
	fromdate.setMonth(fdate[1]-1);
	fromdate.setFullYear(fdate[2]);

	 
	var tdte = toDate.split("-");
	var todate=new Date();
	     
	todate.setDate(tdte[0]);
	todate.setMonth(tdte[1]-1);
	todate.setFullYear(tdte[2]);
	
 
	if (todate.getTime() > fromdate.getTime())
	{
		 return 1;
	} 
	else if (todate.getTime() < fromdate.getTime())
	{
		 return -1;
	} 
	else
		{
		return 0;
		}
}
// Select2 for changing regular select box to Ajax, Paging based selector
function selectBoxAjaxPaging(options) {
	  var remoteDataConfig = "";
	  remoteDataConfig = {
	    placeholder: {
	      id: "0",
	      text: "-- Select --"
	    },
	    allowClear: true,
	    escapeMarkup: function (markup) {
	      return markup;
	    },
	    minimumInputLength: options.minimumInputLength || 0,
	    data: options.data || []
	  };
	  if (options.url) {
	    function formatRepo(repo) {
	      if (repo.loading) return repo.text;
	      var markup = repo.recordIdentifier;
	      return markup;
	    }

	    function formatRepoSelection(repo) {
	      return repo.recordIdentifier || repo.text;
	    }

	    remoteDataConfig.templateResult = formatRepo;
	    remoteDataConfig.templateSelection = formatRepoSelection;
	    remoteDataConfig.ajax = {
	      url: options.url,
	      dataType: 'json',
	      delay: 300,
	      data: function (params) {
	        return {
	          pageLimit: 20,
	          searchTerm: params.term || "",
	          // search term
	          page: params.page || 1
	        };
	      },
	      processResults: function (data, params) {
	        // parse the results into the format expected by Select2
	        // since we are using custom formatting functions we do not need
	        // to alter the remote JSON data, except to indicate that
	        // infinite scrolling can be used
	        params.page = params.page || 1;
	        return {
	          results: data.data,
	          pagination: {
	            more: (params.page * 20) < data.totalRecords
	          }
	        };
	      },
	      cache: true
	    };
	  }
	  if ("small" == options.size) {
	    var smallCss = '.select2-selection--single,.select2-results__options,.select2-search__field{'
	    smallCss += 'background-color: #FFFFCC !important;'
	    smallCss += '}'
	    smallCss += 'span.select2-selection{'
	    smallCss += 'height: 22px !important;'
	    smallCss += '}'
	    smallCss += 'span.select2-dropdown.select2-dropdown--above {'
	    smallCss += 'z-index: 1000000 !important;'
	    smallCss += 'border-radius: 0px !important;'
	    smallCss += '}'
	    smallCss += 'span.select2-selection, span.select2-selection--single {'
	    smallCss += 'border-radius: 0px !important;'
	    smallCss += '}'
	    smallCss += 'span.select2-selection__rendered, span.select2-selection__arrow {'
	    smallCss += 'color: black !important; '
	    smallCss += 'line-height: 20px !important;'
	    smallCss += 'height: 20px !important;'
	    smallCss += '}'
	    smallCss += 'span.select2-selection__clear {'
	    smallCss += 'font-size: 14px !important;'
	    smallCss += '}';
	    addCSS(smallCss);
	  } else if ("med/popup" == options.size) {
	    var smallCss = 'span.select2-selection--single {'
	    smallCss += 'text-align: left;'
	    smallCss += '}'
	    addCSS(smallCss);
	  }
	  return remoteDataConfig;
	}
//disable combo box
function disableComboBox(id){
	var compId = '"#"'+id;
	$(compId).next().attr("disabled", "disabled");
	$(compId).next().next().children().attr("disabled","disabled");
}
//disable dom elements
function disableDoms(id){
	document.getElementById("id").setAttribute("disabled", "true");
}
