/*
 *************************************************************************
 * All Rights Reserved.
 * Contributor(s): Gopalakrishnan
 *************************************************************************
*/

/*function resizeGrid() {

    var gridW, gridH;
    if (window.innerWidth) {
    	  gridW = window.innerWidth - 52;
          gridH = window.innerHeight - 241;
    }
    else if (document.body) {
    	  gridW = window.innerWidth - 52;
          gridH = window.innerHeight - 241;
    }
    
    $("#receiptGrid").setGridWidth(gridW, true);
    $("#receiptGrid").setGridHeight(gridH, true);
}*/
function resizeGrid() {
	/*var w, h;
	if (window.innerWidth) {
		w = window.innerWidth;
		h = window.innerHeight;
	}
	else if (document.body) {
		w = document.body.clientWidth;
		h = document.body.clientHeight;
	}
	$("#receiptGrid").setGridWidth(w - 52, true);
	$("#receiptGrid").setGridHeight(h - 250, true);*/
	$("#receiptGrid").setGridWidth(1140, true);
	$("#receiptGrid").setGridHeight(350, true);
}
/*function reSizeGrid() {
    if (window.innerWidth) {
        gridW = window.innerWidth - 52;
        gridH = window.innerHeight - 241;
    }
    else if (document.body) {
        gridW = document.body.clientWidth - 52;
        gridH = document.body.clientHeight - 241;
    }
    if (gridW < 800)
        gridW = 800;
    if (gridH < 200)
        gridH = 200;
    $("#receiptGrid").setGridWidth(gridW, true);
    $("#receiptGrid").setGridHeight(gridH, true);
}*/ 
/*
 *  Function to Fetch the Associated Cost Begins Here
 */

function ChangeJQGridAllRowColor(grid) {
	var idList = grid.getDataIDs();
	var idCount = idList.length;
	for ( var i = 0; i < idCount; i++)
		if (i % 2 != 0)
			$('tr[id="' + idList[i] + '"]').addClass("DataGrid_Body_Row_Odd");
		else
			$('tr[id="' + idList[i] + '"]').addClass("DataGrid_Body_Row_Even");
}
 var lastSelId="";    
function getDatePicker(rowKey) {
		$('#' + rowKey).datepicker({
			dateFormat : 'dd-mm-yy'
		});
		jQuery('#' + rowKey).datepicker().datepicker("show");
		;
}
function getSelectedRow(rowKey) {

	lastSelId = rowKey;
	jQuery('#receiptGrid').editRow(rowKey, true);
	$('tr[id="' + rowKey + '"]').attr('style', 'background: none; background-color: #FFE1C0; border: 1px solid #CDD7BB;');

}
/*
 * Function to fetch the Receipt Inspection Start Here.
 */
var jsonInspList = {};
jsonInspList.List = [];
$(function() {
	var inpReceiptId = $("#inpReceiptId").val();
	$("#receiptGrid").jqGrid({
		url : contextPath + "/ReceiptInspectionAjax?action=loadReceiptInspection&inpReceiptId=" + inpReceiptId,
		mtype : 'POST',
		colNames : [ '', 'Line', 'Inspection Id', 'Item', 'Initial Qty', 'UOM', 'Status', 'Quantity', 'Inspection Date', 'Quality Code', 'Inspected By', 'Notes', 'InitialId' ],// 'S.No.',,'Status'
		colModel : [ {
			name : 'multiselect',
			index : 'multiselect',
			sortable : false,
			search : false,
			width : 30,
			formatter : formatMultiSelect,
			cellattr : arrtSetting
		}, {
			name : 'line',
			index : 'line',
			width : 100,
			sortable : false,
			cellattr : arrtSetting
		}, {
			name : 'id',
			index : 'id',
			sortable : true,
			hidden : true
		}, {
			name : 'name',
			index : 'name',
			width : 150,
			sortable : true,
			cellattr : arrtSetting
		}, {
			name : 'initialQty',
			index : 'initialQty',
			width : 100,
			sortable : false,
			formatoptions : {
				decimalPlaces : 2
			},
			cellattr : arrtSetting
		}, {
			name : 'uom',
			index : 'uom',
			width : 50,
			align : 'right',
			cellattr : arrtSetting
		}, {
			name : 'status',
			index : 'status',
			width : 100,
			sortable : false
		}, {
			name : 'qty',
			index : 'qty',
			align : 'right',
			formatter : formatQTY,
			formatoptions : {
				decimalPlaces : 2
			},
			sortable : false
		},
		/*  {name:'qty',index:'qty', width:150,sortable:false,editable:true, editoptions: {  
		   		dataEvents: 
			   		[
			             {type: 'keyup',
							fn: function(e) 
							{	      
								////alert(e.currentTarget.value);
							var rowId = $(e.target).closest('tr.jqgrow').attr('id');
		  				   	onchangeQty(this.value,rowId);
							}    		  		   		            
						 },
						 { type: 'keypress', 
							 fn: function(e) 
							 { 	       		  		   							
								document.getElementById("messageBoxID").style.display = "none";		
			   					var charCode = (e.which) ? e.which : e.keyCode;			
			   					if(charCode == 8 || charCode == 9 || charCode == 35 || charCode == 36 || charCode == 37 || charCode == 39 || charCode == 46 || charCode == 127 || ( charCode >= 48 && charCode <= 57 ))
			   				   	{			
			   						var character = String.fromCharCode(charCode);			
			   						if(e.which!="0" && (character=="#" || character=="$" || character=="%" || character=="'" || character=="."))
			   				   			return false;			
			   						if(e.which=="0" && charCode == 46)
			   				   			return true;
			   						if(charCode == 46 && index.value.indexOf(".")!=-1)
			   							return false;
			   						return true;
			   				   	}
			   				   	return false;		            		  		   							
							 } 
						 },
						 {type:'focus',
							 fn:function(e)
							 {            		  		   							
							 	if(this.value=="0")
							 	{
							 		this.value='';
							 	}            		  		   						
							 }
						 },
		   				 {type:'blur',
		   						fn:function(e)
		   						{		       		  		   							
		   							if(this.value=='')
		   							{
		   								this.value="0";
		   							}
		   						}
						 	 }
			   		]
			   }}, */
		{
			name : 'inspectiondate',
			index : 'inspectiondate',
			width : 180,
			sortable : false,
			formatter : formatIsnpectiondate
		},
		/* {name:'inspectiondate',index:'inspectiondate', width:350,sortable:false, editable:true,  editoptions:{ dataInit : function(e) {
		     $(e).calendarsPicker({
		         calendar: $.calendars.instance('ummalqura'),
		         dateFormat: 'dd-mm-yyyy',
		         showTrigger:  
		             '<img src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/calendar.gif" alt="Popup" class="trigger">',
		         changeMonth : true,
		         changeYear : true,
		         onClose : function(dateText, inst) {
		         }
		     });
		     e.style.width = "25%";
		     setTimeout(
		             "$('#gs_inspectiondate').before('<select onchange=\"searchList(\\\'SD\\\');\" style=\"width: 40%; padding: 0px; margin: 2px 0px 0px; height: 17px;\" class=\"Combo\" id=\"gs_inspectiondate_s\"><option value=\"=\">Equal to</option><option value=\">=\">Greater than or Equal to</option><option value=\"<=\">Lesser than or Equal to</option></select>');",
		             10);
		}} },*/
		// {name:'qualitycode',index:'qualitycode', width:210,align:'left',
		// edittype: 'select',
		// formatter:'select', editable: true, editoptions:{value:"Ex:100
		// Excellent;M:50 Medium;p:10 Poor" }},
		{
			name : 'qualitycode',
			index : 'qualitycode',
			width : 190,
			sortable : false,
			search : false,
			hidden : false,
			title : false,
			formatter : customSelectBox
		},
		// {name:'inspectedby',index:'inspectedby', width:210,align:'left',
		// editable:true},
		{
			name : 'inspectedby',
			index : 'inspectedby',
			width : 210,
			align : 'left',
			formatter : formatinspected,
			sortable : false
		}, {
			name : 'notes',
			index : 'notes',
			width : 210,
			align : 'left',
			formatter : formatnotes,
			sortable : false
		}, {
			name : 'initialId',
			index : 'initialId',
			width : 210,
			align : 'left',
			hidden : true,
			sortable : false
		},

		],
		cmTemplate : {
			sortable : false
		},
		pager : '#receiptGridPager',
		rowNum : 10,
		cellEdit : true,
		rowList : [ 10,20,30],
		rownumbers : false,
		viewrecords : false,
		datatype : 'json',
		//multiselect: true,
		sortorder : "asc",
		sortname : 'line',
		cellsubmit : 'clientArray',
		editurl : "clientArray",
		hoverrows : false,
		localReader : {
			repeatitems : false,
			page : function(obj) {
				return 0;
			}
		},
		jsonReader : {
			repeatitems : false,
			page : function(obj) {
				return obj.page;
			},
		total: function (obj) { return obj.total; },
        records: function (obj) { return obj.records;  }   
		},
		loadComplete : function(data) {
			// ChangeJQGridAllRowColor(jQuery("#receiptGrid"));
			$("#cb_receiptGrid").hide();
			$('input[role*="checkbox"]').click(function() {
				jQuery("#receiptGrid").setSelection($(this).parent().parent().attr('id'));
			});

		},
		beforeSelectRow : function(id, e) {
			if ((e.target && ('' + e.target.id).indexOf('DIV_MS_') >= 0) || (e.target.firstChild && (e.target.firstChild.id && ('' + e.target.firstChild.id).indexOf('MS_') >= 0))) {
				return false;
			}
			return true;
		},
		/*beforeSelectRow:function(id, e)
		{
			if(document.getElementById("jqg_receiptGrid_"+id).checked)
				return false;
			else
				return true;
			jQuery('#receiptGrid').editRow(id, true);   
		},*/
		/*    onSelectCell: function (id) {
		    	jQuery("#receiptGrid").setSelection(id);
		     },*/
		/*   beforeEditCell: function (id) {
		   	jQuery("#receiptGrid").setSelection(id);
		   	jQuery('#receiptGrid').editRow(id, true);   

		       },*/
		afterEditCell : function(id) {
			jQuery("#receiptGrid").setSelection(id);
			// jQuery('#receiptGrid').editRow(id, true);

		},
		onSelectRow : function(id, status) {
			var ids = "" + jQuery("#receiptGrid").jqGrid('getGridParam', 'selarrrow');
			var idList = ids.split(",");
			var idListLength = idList.length;
			if (ids.indexOf(",") == 0)
				idListLength--;
			if (status == true) {
				ChangeJQGridSelectMultiRowColors(id, "S");
				var canAdd = true, data = jsonInspList.List;
				for ( var i in data) {
					if (('' + data[i].id) == id) {
						canAdd = false;
						break;
					}
				}
				if (canAdd) {
					var len = jsonInspList.List.length;
					jsonInspList.List[len] = {};
					var rowData = jQuery("#receiptGrid").getRowData(id);
					jsonInspList.List[len]['id'] = rowData['id'];
					jsonInspList.List[len]['qty'] = rowData['qty'];
					jsonInspList.List[len]['initialQty'] = rowData['initialQty'];
					jsonInspList.List[len]['initialId'] = rowData['initialId'];
					jsonInspList.List[len]['notes'] = rowData['notes'];
					jsonInspList.List[len]['inspectedby'] = rowData['inspectedby'];
					jsonInspList.List[len]['qualitycode'] = rowData['qualitycode'];
					jsonInspList.List[len]['inspectiondate'] = rowData['inspectiondate'];
					jsonInspList.List[len]['status'] = rowData['status'];
					jsonInspList.List[len]['isinspected'] = "Y";
				}
			}
			else if (status == false) {
				
				ChangeJQGridSelectMultiRowColors(id, "US");
				findAndRemoveJSONObj(jsonInspList.List, 'id', id);
			}
			if (lastSelId != "") {
				if (lastSelId != id)
					$('tr[id="' + lastSelId + '"]').attr('style', 'background: none; border: 1px solid #CDD7BB;');
			}

			// jQuery('#receiptGrid').editRow(id, true);

		},
		caption : "Receipt Inspection"
	});
	resizeGrid();

	jQuery("#receiptGrid").jqGrid('navGrid', '#receiptGridPager', {

		edit : false,
		add : false,
		del : false,
		search : false,
		view : false,
		beforeRefresh : function() {

		}
	});
	jQuery("#receiptGrid").jqGrid('filterToolbar', {
		searchOnEnter : false
	});
	jQuery("#receiptGrid")[0].triggerToolbar();

});
function formatQTY(el, cellval, opts) {
	if (el != "undefined" && el != undefined )
		return "<span><input type='text' role='jqtextbox' id='qty_"
				+ cellval.rowId
				+ "' name='qty_"
				+ cellval.rowId
				+ "' value='"
				+ parseFloat(el).toFixed(2)
				+ "'"
				+ "outputformat='priceEdition' style='text-align:right; width: 100%;'  onclick=\"getSelectRow('" + cellval.rowId + "');\" onchange=\" onchangeQty(this.value,'"
				+ cellval.rowId
				+ "');\"  onkeydown=\"return isFloatOnKeyDown(event);\" onfocus=\"numberInputEvent('onblur', this);\" onblur=\"numberInputEvent('onblur', this);\"></input></span>";
}

function getDatePicker(rowKey) {
	// $('#'+rowKey).datepicker({ dateFormat: 'dd-mm-yy' });
	// jQuery('#'+rowKey).datepicker().datepicker("show");;
	$('#' + rowKey).calendarsPicker({
		calendar : $.calendars.instance('ummalqura'),
		dateFormat : 'dd-mm-yyyy'
	});

}
function formatMultiSelect(el, cellval, opts) {
	var html = '<div id="DIV_MS_' + cellval.rowId
			+ '" align="center" style="width: 100%; height: 100%; position: relative; padding-top: 4px;"><input type="checkbox" name="multiSelect" id="multiSelect_'+cellval.rowId
			+ '" onchange="changecheckbox(\'' + cellval.rowId + '\');" ></input></div>';
	return html;
}
/*$('#multiSelect').onchange(function(){
    if($(this).is(':checked')){
        $('input[name="totalCost"]').val(10);
    } else {
        calculate();
    }
});*/
function formatIsnpectiondate(el, cellval, opts) {
	var rowKey = cellval.rowId;
	var dateTo = "";
	if (el != "undefined" && el != undefined ) {
		return "<span><input type='text' role='jqtextbox' id='inspectiondate_" + cellval.rowId + "' name='inspectiondate_" + cellval.rowId + "' value='" + el + "'"
				+ " style='text-align:right; width: 100%;' required='true' onmouseover=\"getDatePicker(this.id);\"  onfocus=\"getDatePicker(this.id);getSelectRow('" + cellval.rowId + "');\"   ></input></span>";
	}
	return dateTo;
}
function customSelectBox(el, cellval, opts) {
	var rowKey = cellval.rowId;
	var i = 0;
	var select = '<select name="qualitycode_' + rowKey + '" id="qualitycode_' + rowKey + '" onchange="getSelectRow(\'' + rowKey + '\');" class="Combo Combo_OneCell_width">';
	select += '<option value="Ex"><span>100 Excellent</span></option>';
	select += '<option value="M"><span>50 Medium</span></option>';
	select += '<option value="p"><span>10 Poor</span></option>';
	return select;
}
function formatinspected(el, cellval, opts) {

	if (el != "undefined" && el!=null && el != undefined)
		return "<span><input type='text' role='jqtextbox' id='inspectedby_" + cellval.rowId + "' name='inspectedby_" + cellval.rowId + "' value='" + el + "'" +

		" style='text-align:right; width: 100%;' onclick=\"getSelectRow('" + cellval.rowId + "');\"  ></input></span>";
}
function formatnotes(el, cellval, opts) {
	if (el != "undefined" && el != undefined) {
		return "<span><input type='text' role='jqtextbox' id='notes_" + cellval.rowId + "' name='notes_" + cellval.rowId + "' value='" + el + "'" +

		" style='text-align:right; width: 100%;' maxlength='100' onclick=\"getSelectRow('" + cellval.rowId + "');\"  ></input></span>";
	}
}function onchangeQty(e, rowId) {
	// jQuery("#receiptGrid").setSelection(rowId);
	// var ids = ""+jQuery("#receiptGrid").jqGrid('getGridParam','selarrrow');
	var ids = jQuery("#receiptGrid").getDataIDs();
	// var idList=ids.split(",");
	var idList = ids.length;
	for ( var i = 0; i < idList; i++) {// idList.length;
		var rowData = jQuery("#receiptGrid").getRowData(ids[i]);
		// var qty= $('#receiptGrid').jqGrid("getCell", ids[i] , 'qty') //$("#"
		// + ids[i] + "_qty" ).val();
		// var qty=document.getElementById("qty_"+ids[i]).value;
		if (rowId == rowData['id']) {
			var rowData = jQuery("#receiptGrid").getRowData(ids[i]);
			getSelectRow(ids[i]);

			var initialqty = rowData['initialQty'];
			var idx = jQuery("#receiptGrid").getInd(ids[i]);
			var dataIDs = jQuery("#receiptGrid").getDataIDs();
			if (rowData['status'] == 'Accept') {
				var nextID = (dataIDs.length < idx + 1) ? dataIDs[idx + 1] : dataIDs[idx];
			}
			else {
				var nextID = (dataIDs.length > idx - 1) ? dataIDs[idx - 2] : dataIDs[idx];
			}
			var rowDatanext = jQuery("#receiptGrid").getRowData(nextID);
			var remqty = initialqty - e;
			$("#receiptGrid").setCell(nextID, "qty", remqty);
			// var cm = jQuery("#receiptGrid").jqGrid('getColProp','qty');
			// cm.editable = true;
			// jQuery('#receiptGrid').editRow(nextID, true);

			// $("#receiptGrid").addClass('editable ui-jqgrid-celledit')
			// .setCell(nextID,'qty',remqty);

		}
	}

}
function arrtSetting(rowId, val, rawObject, cm) {
	var attr = rawObject.attr[0][cm.name][0], result;
	if (attr.rowspan) {
		result = ' rowspan=' + '"' + attr.rowspan + '"';
	}
	else if (attr.display) {
		result = ' style="display:' + attr.display + '"';
	}
	return result;
};
function chkselectbox(rowId){
//	alert(rowId);
	$('#multiSelect_'+rowId). prop("checked", "checked");
}

function changecheckbox(rowId){
	
	if(!$('#multiSelect_'+rowId). prop("checked")){
		 alert("hiiiichange");
		 ChangeJQGridSelectMultiRowColors(rowId, "US");
	}
	else{
		 getSelectRow(rowId) ;
	}
}
function selectrow(el, cellval, opts) {
	var rowKey = cellval.rowId;
	getSelectRow(rowKey);
}
function getRowSelected(rowKey) {   
    
    lastSelId=rowKey.split("$$")[1];
    jQuery('#receiptGrid').editRow(lastSelId, true);
    $('tr[id="' + lastSelId + '"]').attr('style', 'background: none; background-color: #FFE1C0; border: 1px solid #CDD7BB;');
   
}

function getSelectRow(rowKey) {
	var type="";
	
	//alert("getselectedrow");
	lastSelId = rowKey;
	jQuery('#receiptGrid').editRow(rowKey, true);
	$('tr[id="' + rowKey + '"]').attr('style', 'background: none; background-color: #FFE1C0; border: 1px solid #CDD7BB;');
	$("#jqg_receiptGrid_" + rowKey).prop("checked", true);
	 
	var rowData = jQuery("#receiptGrid").getRowData(rowKey);
	var canAdd = true, data = jsonInspList.List;
	for ( var p in data) {
		if (('' + data[p].id) == rowKey) {
			findAndRemoveJSONObj(jsonInspList.List, 'id', rowKey);
		}
	}
	if (canAdd) {
		var len = jsonInspList.List.length;
		jsonInspList.List[len] = {};
		jsonInspList.List[len]['id'] = rowData['id'];
		jsonInspList.List[len]['initialQty'] = rowData['initialQty'];
		jsonInspList.List[len]['initialId'] = rowData['initialId'];
		jsonInspList.List[len]['qty'] = $("#qty_" + rowKey).val(); // $("#" +
																	// nextID+
																	// "_qty"
																	// ).val();
		jsonInspList.List[len]['notes'] = $("#notes_" + rowKey).val();
		jsonInspList.List[len]['inspectedby'] = $("#inspectedby_" + rowKey).val();
		jsonInspList.List[len]['qualitycode'] = $("#qualitycode_" + rowKey).val();
		jsonInspList.List[len]['inspectiondate'] = $("#inspectiondate_" + rowKey).val();
		jsonInspList.List[len]['status'] = rowData['status'];
		jsonInspList.List[len]['isinspected'] = "Y";
	}
	if($('#multiSelect_'+rowKey). prop("checked")){
	type ="US";
	$("#multiSelect_" + rowKey).prop("checked",false);
}
else{
	type ="S";
	chkselectbox(rowKey);
}
	ChangeJQGridSelectMultiRowColors(rowKey, type);
	return;
}
function UpdateLines() {
	var data = jsonInspList.List;
	var initialId = "";
	var prelineqty = 0;
	var initialQty = "";
	if (data.length == 0 || data.length == 0) {
		
		OBAlert(selectatlstonelne);
		
		return false;
	}
	else {
		for ( var j in data) {
			var rowData = jQuery("#receiptGrid").getRowData(data[j].id);
			var idx = jQuery("#receiptGrid").getInd(data[j].id);
			data[j].qty = $("#qty_" + data[j].id).val();
			data[j].notes = $("#notes_" + data[j].id).val();
			data[j].inspectedby = $("#inspectedby_" + data[j].id).val();
			data[j].qualitycode = $("#qualitycode_" + data[j].id).val();
			data[j].inspectiondate = $("#inspectiondate_" + data[j].id).val();
			data[j].isinspected = "Y";
			if (initialId == "") {
				initialId = data[j].initialId;
				initialQty = data[j].initialQty;
				prelineqty = data[j].qty;
				if (parseFloat(prelineqty) > parseFloat(initialQty)) { 
					
					OBAlert(acceptgtrejqty);
					
					return false;
				}
			}
			else if (initialId == data[j].initialId) {
				initialQty = data[j].initialQty;
				prelineqty = parseFloat(data[j].qty) + parseFloat(prelineqty);
				if (prelineqty > initialQty) {
					
					OBAlert(acceptgtrejqty);
					
					return false;
				}
			}

		}
		document.getElementById("inpSelList").value = JSON.stringify(jsonInspList);
		document.getElementById("action").value = "UpdateInsLines";
		document.frmMain.submit();
	}
}function ChangeJQGridSelectMultiRowColors(rowId, type) {
	if (type == "S") {
		$('tr[id="' + rowId + '"]').removeClass('ui-state-highlight ui-widget-content');
		$('tr[id="' + rowId + '"]').attr('style', 'background: none; background-color: #FFE1C0; border: 1px solid #CDD7BB;');
	}
	else if (type == "US") {
		$('tr[id="' + rowId + '"]').attr('style', 'border: 1px solid #CDD7BB;');
	}
	var rowData = jQuery("#receiptGrid").getRowData(rowId);
	var idx = jQuery("#receiptGrid").getInd(rowId);
	var dataIDs = jQuery("#receiptGrid").getDataIDs();
	if (rowData['status'] == 'Accept') {
		var nextID = (dataIDs.length < idx + 1) ? dataIDs[idx + 1] : dataIDs[idx];
	}
	else {
		var nextID = (dataIDs.length > idx - 1) ? dataIDs[idx - 2] : dataIDs[idx];
	}
	if (type == "S") {
		$('tr[id="' + nextID + '"]').removeClass('ui-state-highlight ui-widget-content');
		$('tr[id="' + nextID + '"]').attr('style', 'background: none; background-color: #FFE1C0; border: 1px solid #CDD7BB;');
		$("#jqg_receiptGrid_" + nextID).prop("checked", true);
		 chkselectbox(nextID);
		// jQuery("#receiptGrid").setSelection(nextID,true);
		var canAdd = true, data = jsonInspList.List;
		for ( var p in data) {
			if (('' + data[p].id) == nextID) {
				findAndRemoveJSONObj(jsonInspList.List, 'id', nextID);
			}
		}
		if (canAdd) {
			var len = jsonInspList.List.length;
			jsonInspList.List[len] = {};
			var rowData = jQuery("#receiptGrid").getRowData(nextID);
			jsonInspList.List[len]['id'] = rowData['id'];
			jsonInspList.List[len]['initialQty'] = rowData['initialQty'];
			jsonInspList.List[len]['initialId'] = rowData['initialId'];
			jsonInspList.List[len]['qty'] = $("#qty_" + nextID).val(); // $("#"
																		// +
																		// nextID+
																		// "_qty"
																		// ).val();
			jsonInspList.List[len]['notes'] = $("#" + nextID + "_notes").val();
			jsonInspList.List[len]['inspectedby'] = $("#" + nextID + "_inspectedby").val();
			jsonInspList.List[len]['qualitycode'] = $("#qualitycode_" + nextID + "").val();
			jsonInspList.List[len]['inspectiondate'] = $("#" + nextID + "_inspectiondate").val();
			jsonInspList.List[len]['status'] = rowData['status'];
			jsonInspList.List[len]['isinspected'] = "Y";
		}

	}
	else if (type == "US") {
		$('tr[id="' + nextID + '"]').attr('style', 'border: 1px solid #CDD7BB;');
		$("#jqg_receiptGrid_" + nextID).prop("checked", false);
		$("#multiSelect_" + nextID).prop("checked", false);
		findAndRemoveJSONObj(jsonInspList.List, 'id', nextID);
	}
	return;
}
