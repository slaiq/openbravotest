/*
 *************************************************************************
 * All Rights Reserved.
 
 *************************************************************************
 */

var onSearch = 0;
var salaryGrid = jQuery("#HoldList");
//Load Salary Details Grid
var salaryUrl = '';
var isTxn = document.getElementById("isTxn").value;
if(isTxn=='Y'){
	salaryUrl = "&inpRDVTxnId=" + document.getElementById("inpRDVTxnId").value;
}else{
	salaryUrl = "&inpRDVTxnLineId=" + document.getElementById("inpRDVTxnLineId").value;
}
var lineno = document.getElementById("inplineno").value;
var bpartnername = document.getElementById("inpbpartnername").value;
var invoiceNo = document.getElementById("inpinvoiceNo").value;
var trxappNo = document.getElementById("inptrxappNo").value;
var matAmt = document.getElementById("inpmatamt").value;
var netAmt = document.getElementById("inpnetamt").value;
var holdType = document.getElementById("inpholdtype").value;
var thershold = document.getElementById("inpholdThershold").value;
var holdAmt = matAmt * (thershold / 100);
var inptodaydate = document.getElementById("inptodaydate").value;
var inpuniquecode = document.getElementById("inpuniquecode").value;
var inplineuniquecode = document.getElementById("inplineuniquecode").value;
var inplineuniquecodeName = document.getElementById("inplineuniquecodeName").value;
var inplineuniquecodeId = document.getElementById("inplineuniquecodeId").value;
var inpverstatus = document.getElementById("inpverstatus").value;

var inprdvtrxtype = document.getElementById("inprdvtrxtype").value;
var inpHoldAccountType = document.getElementById("inpDefaultHoldAcctType").value;

var lastsel = "";
var pendeducttype = "";
var successflag = false;
var saveFlag = 0;
jQuery(function () {
  salaryGrid.jqGrid({
    url: contextPath + '/RdvHoldActionAjax?action=GetHoldDetails' + salaryUrl,
	    colNames: [sequence, action, txnAppNo, action, actionDate, matchAmt,'netmatch', holdTypes, holdPercent, holdAmount, 
	    	actionReason, actionJustification, 'BpartnerId', 'BpartnerName', asociatedBp, bpName, freezeHold,
	    	amrasafNo, amrasafAmt, deductionAcctType, 
	    	'UniquecodeId', uniquecode, uniquecode, uniquecodeName,
	    	'deductiontype', 'holdRelId', 'orgHoldAmt','isTxn'],
    colModel: [{
      name: 'Sequence',
      index: 'Sequence',
      width: '10px',
      fixed: true,
      hidden: true,
      editable: true,
      editoptions: {
        readonly: "readonly",
        defaultValue: lineno
      },
      searchoptions: {
        clearSearch: true,
        attr: {
          size: 20,
          maxlength: 20,
          style: "width:150px;margin-top:1px;"
        }
      }
    }, {
      name: "actions",
      width: 100,
      formatter: "actions",
      hidden: true
    }, {
      name: 'appno',
      index: 'appno',
      width: '120px',
      fixed: true,
      editable: false,
      editoptions: {
        readonly: "readonly",
        defaultValue: trxappNo
      },
    }, {
      name: 'actiontype',
      index: 'actiontype',
      width: '70px',
      fixed: true,
      editable: true,
      edittype: 'select',
      formatter: 'select',
      editoptions: {
        value: "AD:" + add + ";RM:" + remove,
        dataEvents: [{
          type: 'change',
          fn: actiontypechange
        }]
      },
      searchoptions: {
        clearSearch: true,
        attr: {
          size: 60,
          maxlength: 90,
          style: "width:150px;margin-top:1px;"
        }
      }
    }, {
      name: 'actionDate',
      index: 'actionDate',
      width: '140px',
      fixed: true,
      editable: true,
      editoptions: {
        dataInit: function (e) {
          $(e).calendarsPicker({
            calendar: $.calendars.instance('ummalqura'),
            dateFormat: 'dd-mm-yyyy',

            showTrigger: '<img src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/calendar.gif" alt="Popup" class="trigger">',
            defaultDate: inptodaydate,
            selectDefaultDate: true,
            changeMonth: true,
            changeYear: true,
            onClose: function (dateText, inst) {},
          });
          e.style.width = "60%";
        },
      }
    }, {
      name: 'Amt',
      index: 'Amt',
      width: '100px',
      fixed: true,
      editable: false,
      editoptions: {
        readonly: "readonly",
        defaultValue: matAmt
      },
      formatter: 'number'
    }, {
		  name: 'NetAmt',
		  index: 'NetAmt',
		  width: '100px',
		  fixed: true,
		  hidden: true,
		  editable: false,
		  editoptions: {
		     readonly: "readonly",
		     defaultValue: netAmt
		  },
		  formatter: 'number'
	    }, {
      name: 'holdtype',
      index: 'holdtype',
      width: '250px',
      fixed: true,
      editable: true,
      edittype: 'select',
      formatter: 'select',
      editoptions: {
        value: holdType,
        dataEvents: [{
          type: 'change',
          data: {
            dateColumn: "holdpercentage"
          },
          fn: holdChange
        }]
      }
    }, {
      name: 'holdpercentage',
      index: 'holdpercentage',
      width: '80px',
      fixed: true,
      editable: true,
      editoptions: {
        defaultValue: "0.00",
        readonly: "readonly"
      },
    }, {
      name: 'holdamount',
      index: 'holdamount',
      width: '100px',
      fixed: true,
      editable: true,
      editoptions: {
        dataInit: function (element) {
          $(element).keypress(function (e) {
            if (e.which != 8 && e.which != 0 && e.which != 46 && e.which != 45 && (e.which < 48 || e.which > 57)) {
              return false;
            }
          });
        },
        defaultValue: "0.00"
      },
      //formatter: 'number'

    }, {
      name: 'actionreason',
      index: 'actionreason',
      width: '200px',
      fixed: true,
      editable: true
    }, {
      name: 'actionjustfication',
      index: 'actionjustfication',
      width: '200px',
      fixed: true,
      editable: true
    }, {
      name: 'bpartnerId',
      index: 'bpartnerId',
      width: '200px',
      fixed: true,
      hidden: true
    }, {
      name: 'bpartnername',
      index: 'bpartnername',
      width: '200px',
      fixed: true,
      hidden: true
    }, {
      name: 'associatedbp',
      index: 'associatedbp',
      width: '240px',
      fixed: true,
      editable: true,
      edittype: 'select',
      formatter: function (cellVal, opts, rowObj) {
        return cellVal;
      },
      editoptions: {
        value: [],
        dataEvents: [{
          type: 'change',
          data: {
            dateColumn: "bpname"
          },
          fn: bpChange
        }],
        style: "width:240px"
      }
    }, {
      name: 'bpname',
      index: 'bpname',
      width: '200px',
      fixed: true,
      editable: true,
      editoptions: {
        readonly: "readonly"
      }
    }, {
      name: 'freezehold',
      index: 'freezehold',
      align: 'center',
      width: '100px',
      fixed: true,
      editable: true,
      edittype: 'checkbox',
      editoptions: {
        value: "Y:N"
      },
      search: false,

      formatter: "checkbox"

    }, {
      name: 'amarsarfno',
      index: 'amarsarfno',
      width: '100px',
      fixed: true,
      hidden: true,
      editable: true,
      type: 'change',
      editoptions: {
        readonly: "readonly",
        defaultValue: null
      }

    }, {
      name: 'amarsarfamount',
      index: 'amarsarfamount',
      width: '100px',
      fixed: true,
      hidden: true,
      editable: true,
      formatter: 'number',
      editoptions: {
        readonly: "readonly",
        defaultValue: null
      },
    }, {
      name: 'accounttype',
      index: 'accounttype',
      width: '200px',
      hidden: true,
      fixed: true,
      editable: true,
      edittype: 'select',
      formatter: 'select',
      editoptions: {
        value: "E:" + expense + ";A:" + adjustment,
        dataEvents: [{
          type: 'change',
          data: {
            dateColumn: "uniquename"

          },
          fn: acttypeChange
        }]
      }
    }, {
      name: 'uniquecodeId',
      index: 'uniquecodeId',
      width: '200px',
      hidden: true,
      fixed: true,
      hidden: true
    }, {
      name: 'uniquecode',
      index: 'uniquecode',
      width: '350px',
      hidden: true,
      fixed: true,
      editable: true,
      edittype: 'select',
      formatter: function (cellVal, opts, rowObj) {
        return cellVal;
      },
      editoptions: {
        value: [],
        defaultValue: inplineuniquecode,
        dataEvents: [{
          type: 'change',
          data: {
            dateColumn: "uniquename"
          },
          fn: uniquecodeChange
        }],
        style: "width:350px"
      },
      searchoptions: {
        clearSearch: true,
        attr: {
          size: 80,
          maxlength: 90,
          style: "width:400px;margin-top:1px;"
        }
      }

    }, {
      name: 'uniquecodehid',
      index: 'uniquecodehid',
      width: '200px',
      fixed: true,
      hidden: true
    }, {
      name: 'uniquename',
      index: 'uniquename',
      width: '400px',
      fixed: true,
      hidden: true,
      editable: true,
      editoptions: {
        readonly: "readonly"
      }

    }, {
      name: 'deductiontype',
      index: 'deductiontype',
      width: '100px',
      hidden: true
    }, {
      name: 'holdrelId',
      index: 'holdrelId',
      width: '100px',
      hidden: true
    }, {
      name: 'orgHoldAmt',
      index: 'orgHoldAmt',
      width: '100px',
      hidden: true
    },{
        name: 'isTxn',
        index: 'isTxn',
        width: '100px',
        hidden: true
        }],

    editurl: contextPath + '/RdvHoldActionAjax?action=saveholdaction' + salaryUrl,
    rowNum: 20,
    rowList: [20, 50, 100, 200, 500],
    pager: '#pager',
    mtype: 'POST',
    datatype: 'json',
    rownumbers: true,
    iconsOverText: true,
    viewrecords: true,
    shrinkToFit: false,
    sortname: 'seqno',
    sortorder: "asc",
    height: "90%",
    pgbuttons: true,
    localReader: {
      repeatitems: false,
      page: function (obj) {
        return 0;
      }
    },
    jsonReader: {
      repeatitems: false,
      page: function (obj) {
        return obj.page;
      }
    },
    beforeSelectRow: function (id) {
    	var rowData = salaryGrid.getRowData(id);
    	var holdtype = rowData['isTxn'];
    		if(holdtype != isTxn){
    	        $("#HoldList_iledit").addClass(" ui-state-disabled");
    	        $("#HoldList_ildelete").addClass(" ui-state-disabled");
    			return false;
    		}else{
       	    $("#HoldList_iledit").removeClass(" ui-state-disabled");
    	        $("#HoldList_ildelete").removeClass(" ui-state-disabled");
    			return true;
    		}
      },
    onSelectRow: function (id) {
      var holdRelId = jQuery('#HoldList').jqGrid('getRowData', id).holdrelId;
      if (holdRelId != null && holdRelId != "" && holdRelId != "null" && holdRelId !="0") {
        $("#HoldList_iledit").addClass(" ui-state-disabled");
      } else {
        $("#HoldList_iledit").removeClass(" ui-state-disabled");
      }
    },
    loadComplete: function (data) {
      ChangeJQGridAllRowColor(salaryGrid);

      reSizeGrid();
      gridIds = jQuery("#HoldList").getDataIDs();
      gridIdsLength = gridIds.length;
      $("#HoldList_iladd").insertBefore("#del_HoldList");
      $("#HoldList_iledit").insertAfter("#HoldList_iladd");
      $("#HoldList_ilsave").insertAfter("#HoldList_iledit");
      $("#HoldList_ilcancel").insertAfter("#del_HoldList");
    },

    caption: ''
  });

  salaryGrid.jqGrid('navGrid', '#pager', {
    edit: false,
    add: false,
    del: true,
    search: false,
    view: false,
    refreshtext: reload,
    deltext: deleteicon,
    delfunc: function (rowids) {
      deleterow(rowids);
    },
    beforeRefresh: function () {
      reSizeGrid();
    }
  },

  {}, {}, {}, {});


  jQuery('#HoldList').jqGrid('inlineNav', '#pager', {
    edit: true,
    add: true,
    addtext: add,
    edittext: edit,
    savetext: save,
    canceltext: cancel,
    editParams: {
      oneditfunc: function (rowid, response, options) {
        saveFlag = 1;
/*var holdRelId =  jQuery('#SalaryList').jqGrid('getRowData', rowid).holdrelId;
	        if(holdRelId!=null && holdRelId!="" && holdRelId!="null" ){
	        	 disableAllFields(rowid,true);
	        }
	        else{
	        	disableAllFields(rowid,false);
	        }*/

        getBusinesspartner(rowid, response, options, "true");
        var accttype = $("#" + $.jgrid.jqID(rowid + "_accounttype")).val();
        var bpartnerId = jQuery('#HoldList').jqGrid('getRowData', rowid).bpartnerId;
        var holdtypeId = $("#" + $.jgrid.jqID(rowid + "_holdtype")).val();
        var holType = getHoldType(holdtypeId, rowid);
        var holdpercentage = $("#" + $.jgrid.jqID(rowid + "_holdpercentage")).val();

        if (holdpercentage != null && holdpercentage != "null" && holdpercentage != "" && holdpercentage != 0) $("#" + $.jgrid.jqID(rowid + "_holdamount")).prop("readonly", true);
        else $("#" + $.jgrid.jqID(rowid + "_holdamount")).prop("readonly", false);

        if (inprdvtrxtype == 'POS' || inprdvtrxtype == 'PO') {
          if (accttype == 'E') {
            $("#" + $.jgrid.jqID(rowid + "_uniquecode")).prop("disabled", true);
          }
        }

        if (inprdvtrxtype == 'POD') {
          getUniqueCode(rowid, response, options, accttype, bpartnerId, "false");
        } else {
          getUniqueCode(rowid, response, options, accttype, null, "false");
        }

        if (holType == "DP" || holType == "CH") {
          //$("#"+rowid + "_associatedbp").select2("val", "0");
          $("#" + $.jgrid.jqID(rowid + "_bpname")).val(null);
          $("#" + $.jgrid.jqID(rowid + "_associatedbp")).prop("disabled", true);
          $("#" + rowid + "_associatedbp").empty().trigger('change');
        }

        if (inprdvtrxtype == 'POD' & bpartnerId != null) {
          $("#" + $.jgrid.jqID(rowid + "_accounttype")).prop("disabled", true);
        } else {
          $("#" + $.jgrid.jqID(rowid + "_accounttype")).prop("disabled", false);
        }

/*if(inprdvtrxtype=='POS' || inprdvtrxtype=='PO'){
		      getLineUniqueCode(rowid, response, options); 
		    }*/
      },
      beforeSaveRow: function (options, rowid) {
        OBConfirm(askSave, function (a) {
          if (a) {
            successflag = saverow(options, rowid);
          }
        });
        return false;
      },
      successfunc: function () {
        var $self = $(this);
        setTimeout(function () {
          $self.trigger("reloadGrid");
        }, 50);
      }
    },
    addParams: {
      position: "last",
      addRowParams: {
        keys: true,
        //position: "last",
        oneditfunc: function (rowid, response, options) {
          saveFlag = 1;
          $("#" + $.jgrid.jqID(rowid + "_accounttype")).val(inpHoldAccountType);
          getBusinesspartner(rowid, response, options, "false");
          getUniqueCode(rowid, response, options, inpHoldAccountType, null, "false");
          if (inprdvtrxtype == 'POS' || inprdvtrxtype == 'PO') {
            //  $("#" + $.jgrid.jqID(rowid + "_accounttype")).prop("disabled",true);
            $("#" + $.jgrid.jqID(rowid + "_uniquecode")).prop("disabled", true);
            getLineUniqueCode(rowid, response, options);
          }
        },
        //aftersavefunc  oneditfunc
        beforeSaveRow: function (options, rowid) {
          OBConfirm(askSave, function (a) {
            if (a) {
              successflag = saverow(options, rowid);
            }
          });
          return false;
        },
        successfunc: function () {
          var $self = $(this);
          setTimeout(function () {
            $self.trigger("reloadGrid");
          }, 50);
        }
      }

    },
    reloadAfterSubmit: true,
  });


  jQuery("#HoldList").trigger("reloadGrid");
  salaryGrid.jqGrid('filterToolbar', {
    searchOnEnter: false
  });

});

function reloadGrid(result) {
  setTimeout(function () {
    $("#HoldList").trigger("reloadGrid");
    saveFlag = 0;
  }, 50);
}

var addOptions = '';

function saverow(options, rowid) {
  var cellId = rowid + "_actiontype";
  var action = $("#" + $.jgrid.jqID(rowid + "_actiontype")).val();
  var holdamount = $("#" + $.jgrid.jqID(rowid + "_holdamount")).val();
  var matchAmt = $("#" + $.jgrid.jqID(rowid + "_Amt")).val();
	  var netAmt = $("#" + $.jgrid.jqID(rowid + "_NetAmt")).val();
  var holdtype = $("#" + $.jgrid.jqID(rowid + "_holdtype")).val();
  var associatedbp = $("#" + $.jgrid.jqID(rowid + "_associatedbp")).val();
  var uniquecode = $("#" + $.jgrid.jqID(rowid + "_uniquecode")).val();


  if (inpverstatus != "DR") {
    OBAlert(versionstatusinvoice);
    return false;
  }
  if (holdtype == '0') {
    OBAlert(penamtshouldnotneg);
    return false;
  }
  if (holdamount == '') {
    OBAlert(holdamtnotblank);
    return false;
  }
  if (holdamount == 0) {
    OBAlert(holdAmtNotZero);
    return false;
  }
//  if (uniquecode == null || uniquecode == "0" || uniquecode == "" || uniquecode == null || uniquecode == "null") {
//    OBAlert(uniquecodemandatory);
//    return false;
//  }
  if (holdtype != null && holdtype != "0" && inprdvtrxtype != 'POD') {
    var deductype = getHoldType(holdtype, rowid);

    if (deductype == "ECA" || deductype == "IGI") {
      if (associatedbp == "0" || associatedbp == "" || associatedbp == null || associatedbp == "null") {
        OBAlert(bpmandatory);
        return false;
      }
    }
  }
  if (action == 'RM') {
	  var isAlreadyExists  = checkHoldTypeAlreadExistsInTxn(holdtype,rowid,action);
	  if (isAlreadyExists == 'Y') {
          OBAlert(holdTypeMore);
          return false;
	  }
    if (parseFloat(holdamount) > 0) {
      OBAlert(Removenegative);
      return false;
    } else if (document.getElementById("inpRDVTxnLineId").value != null || document.getElementById("inpRDVTxnId").value != null) {
      var existsnegval = chkHoldtypeGoingdownNegativeval(holdamount, holdtype, rowid, "add");
      if (existsnegval == 'Y') {
        OBAlert(holdamtshouldnotnegrdv);
        return false;
      }
    }
  } else if (action == 'AD') {
	  var isAlreadyExists  = checkHoldTypeAlreadExistsInTxn(holdtype,rowid,action);
        	  if (isAlreadyExists == 'Y') {
    	          OBAlert(holdTypeMore);
              return false;
          } 
    if (parseFloat(holdamount) < 0) {
      OBAlert(addpositive);
      return false;
    } 
   
    else {
	      if (parseFloat(netAmt) < parseFloat(holdamount)) {
        OBAlert(penamtgreterthanmatchamt);
        return false;
      } else {
        var existsgreaterMatchAmt = chkpenAmtgreaterthanMatchAmt(holdamount, rowid);
        if (existsgreaterMatchAmt == 'Y') {
	          OBAlert(holdAmtGreater);
          return false;
        }
      }
    }
  }
  jQuery("#HoldList").saveRow(rowid, {
	    aftersavefunc: reloadGrid
	  });
	  saveFlag = 0;
  return true;
  //return false;
}


function deleterow(rowids) {
  OBConfirm(askDelete, function (result) {
    if (result) {
      if (inpverstatus != "DR") {
        OBAlert(versionstatusinvoice);
        return false;
      }
      var existsnegval = delOptions(rowids);
      var successflag = "N";
      if (existsnegval == 'Y') {
        OBAlert(holdamtshouldnotnegrdv);
        return false;
      } else {
    	  var rowData = salaryGrid.getRowData(rowids);
    	  var holdtype =rowData['holdtype']
      var holdAction =rowData['actiontype'];
        $.ajax({
          url: contextPath + '/RdvHoldActionAjax?action=saveholdaction' + salaryUrl + "&oper=del&id=" + rowids +"&holdtype="+holdtype + "&holdAction=" + holdAction,
        }).done(function (msg) {
          successflag = "Y";
          if (successflag == 'Y') $("#HoldList").trigger("reloadGrid");
        });

      }
    }
/*   else
	        	return false;*/
  });
  return false;
}

function checkHoldTypeAlreadExistsInTxn(holdType,id,action) {
	 var alreadyExists = "N";
	  $.ajax({
	    type: 'GET',
	    url: contextPath + '/RdvHoldActionAjax?action=checkHoldTypeAlreadExistsInTxn',
	    data: {
	      holdType: holdType,
	      inpRDVTxnId: document.getElementById("inpRDVTxnId").value,
	      actionType:action,
	      id:id
	    },
	    dataType: 'json',
	    async: false
	  }).done(function (response) {
		  alreadyExists = response.isExists;
	  });
	  return alreadyExists;
}
function chkHoldtypeGoingdownNegativeval(holdAmt, holdType, response, type) {
  var Checkcontractvalidation = "N";
  $.ajax({
    type: 'GET',
    url: contextPath + '/RdvHoldActionAjax?action=chkHoldtypeGoingdownNegativeval',
    data: {
      inpRDVTxnLineId: document.getElementById("inpRDVTxnLineId").value,
      holdAmt: holdAmt,
      holdType: holdType,
      id: response,
      type: type,
      inpRDVTxnId: document.getElementById("inpRDVTxnId").value
    },
    dataType: 'json',
    async: false
  }).done(function (response) {
    Checkcontractvalidation = response.isExists;

  });
  return Checkcontractvalidation;
}

function chkpenAmtgreaterthanMatchAmt(holdAmt, response) {
  var ChkpenAmtgreaterMatchAmt = "N";
  $.ajax({
    type: 'GET',
    url: contextPath + '/RdvHoldActionAjax?action=getTotalMatchAmt',
    data: {
      inpRDVTxnLineId: document.getElementById("inpRDVTxnLineId").value,
      inpRDVTxnId: document.getElementById("inpRDVTxnId").value,
      holdAmt: holdAmt,
      id: response
    },
    dataType: 'json',
    async: false
  }).done(function (response) {
    ChkpenAmtgreaterMatchAmt = response.isExists;

  });
  return ChkpenAmtgreaterMatchAmt;
}
uniquecodeChange = function (e) {
  var $this = $(e.target),
      columnName = e.data.dateColumn,
      rowid, cellId;
  rowid = $this.closest("tr.jqgrow").attr("id");
  cellId = rowid + "_" + columnName;

  if ($this.val() != '0' && $this.val() != null) {
    $.ajax({
      type: 'GET',
      url: contextPath + '/RdvHoldActionAjax?action=getUniqueCodeName',
      data: {
        inpcomId: $this.val()
      },
      dataType: 'xml',
      async: false,
      success: function (data) {
        $(data).find("GetComName").each(function () {
          var result = $(this).find("value").text();
          $("#" + $.jgrid.jqID(cellId)).val(
          result);
        });
      }
    });
  } else {
    $("#" + $.jgrid.jqID(cellId)).val(
    null);
  }
};
actiontypechange = function (e) {
  var $this = $(e.target),
      rowid, cellId;
  rowid = $this.closest("tr.jqgrow").attr("id");
  var row = $this.closest('tr.jqgrow');
  if ($this.val() == 'RM') {
    var holdamt = $("#" + $.jgrid.jqID(rowid + "_holdamount")).val();
    if (holdamt > 0) holdamt = holdamt * -1;
    $("#" + $.jgrid.jqID(rowid + "_holdamount")).val(holdamt);

    $('select[id*="' + rowid + '_holdtype"]', row).empty();
    var valueslist = loadholdtype($this.val(), rowid);
    $('select[id*="' + rowid + '_holdtype"]', row).html(valueslist);
  } else {
    var holdamt = $("#" + $.jgrid.jqID(rowid + "_holdamount")).val();
    if (holdamt < 0) holdamt = holdamt * -1;
    $("#" + $.jgrid.jqID(rowid + "_holdamount")).val(holdamt);
  }
}

acttypeChange = function (e) {
  var $this = $(e.target),
      columnName = e.data.dateColumn,
      rowid, cellId;
  rowid = $this.closest("tr.jqgrow").attr("id");
  cellId = rowid + "_" + columnName;

  // $("#"+rowid + "_uniquecode").select2('data', null)
  $("#" + rowid + "_uniquecode").select2("val", "0");
  $("#" + $.jgrid.jqID(rowid + "_uniquename")).val(null);
  // getUniqueCode(rowid, null, null, $this.val(), null, "true");
  if (inprdvtrxtype == 'POS' || inprdvtrxtype == 'PO') {
    if ($this.val() == 'A') {
      $("#" + $.jgrid.jqID(rowid + "_uniquecode")).prop("disabled", false);
      getUniqueCode(rowid, null, null, $this.val(), null, "true");
    } else {
      $("#" + $.jgrid.jqID(rowid + "_uniquecode")).prop("disabled", true);
      $("#" + rowid + "_uniquecode").empty().trigger('change');
      var data = [{
        id: inplineuniquecodeId,
        text: inplineuniquecode
      }];
      $("#" + rowid + "_uniquecode").select2({
        data: data
      });
      $("#" + $.jgrid.jqID(rowid + "_uniquename")).val(inplineuniquecodeName);
    }
  } else {
    getUniqueCode(rowid, null, null, $this.val(), null, "true");
  }

}

bpChange = function (e) {
  var $this = $(e.target),
      columnName = e.data.dateColumn,
      rowid, cellId;
  rowid = $this.closest("tr.jqgrow").attr("id");
  cellId = rowid + "_" + columnName;
  if ($this.val() != '0' && $this.val() != null) {

    if (inprdvtrxtype == 'POD') {
      getUniqueCode(rowid, null, null, "A", $this.val(), "false");
      $("#" + $.jgrid.jqID(rowid + "_accounttype")).val("A");
      $("#" + $.jgrid.jqID(rowid + "_accounttype")).prop("disabled", true);
      $("#" + rowid + "_uniquecode").empty().trigger('change');
      var data = [{
        id: "0",
        text: select
      }];
      $("#" + rowid + "_uniquecode").select2({
        data: data
      });
    }

    $.ajax({
      type: 'GET',
      url: contextPath + '/RdvHoldActionAjax?action=getbpName',
      data: {
        inpbpId: $this.val()
      },
      dataType: 'xml',
      async: false,
      success: function (data) {
        $(data).find("GetBpName").each(function () {
          var result = $(this).find("value").text();
          $("#" + $.jgrid.jqID(cellId)).val(
          result);
          $("#" + $.jgrid.jqID(cellId)).prop("editable", false);

        });
      }
    });
  } else {
    $("#" + $.jgrid.jqID(cellId)).val(null);
    var holType = $("#" + $.jgrid.jqID(rowid + "_holdtype")).val();
    var pendedtype = getHoldType(holType, rowid);
    if (inprdvtrxtype == 'POD' && pendedtype != 'CH' && pendedtype != 'DP') {
      $("#" + rowid + "_uniquecode").empty().trigger('change');
      var data = [{
        id: "0",
        text: select
      }];
      $("#" + rowid + "_uniquecode").select2({
        data: data
      });
      $("#" + $.jgrid.jqID(rowid + "_uniquename")).val(null);
    }
    $("#" + $.jgrid.jqID(rowid + "_accounttype")).prop("disabled", false);
    getUniqueCode(rowid, null, null, $("#" + $.jgrid.jqID(rowid + "_accounttype")).val(), null, "false");
  }
};


function delOptions(id) {
  if (document.getElementById("inpRDVTxnLineId").value != null ||  document.getElementById("inpRDVTxnId").value != null ) {
	  var rowData = salaryGrid.getRowData(id);
    var cellId = id + "_actiontype";
    var action = rowData['actiontype']//$("#" + $.jgrid.jqID(id + "_actiontype")).val();
    var holdamount = rowData['holdamount']//$("#" + $.jgrid.jqID(id + "_holdamount")).val();HoldList_holdamount
    var holdtype =rowData['holdtype']// $("#" + $.jgrid.jqID(id + "_holdtype")).val();
    
    var existsnegval = chkHoldtypeGoingdownNegativeval(holdamount, holdtype, id, "del");
    return existsnegval;

  }
}



holdChange = function (e) {
  var $this = $(e.target),
      columnName = e.data.dateColumn,
      rowid, cellId;

  // inline editing
  rowid = $this.closest("tr.jqgrow").attr("id");
  cellId = rowid + "_" + columnName;

  $.ajax({
    type: 'GET',
    url: contextPath + '/RdvHoldActionAjax?action=getThershold',
    data: {
      inpholdTypeId: $this.val()
    },
    dataType: 'xml',
    async: false,
    success: function (data) {
      $(data).find("GetHold").each(function () {
        var result = $(this).find("value").text();
        var resultpresnt = $(this).find("valuepresent").text();
        var holdtype = $(this).find("type").text();
        pendeducttype = holdtype;
        var action = $("#" + $.jgrid.jqID(rowid + "_actiontype")).val();
        if (resultpresnt == 1) {
          var grid = jQuery("#HoldList");
          $("#" + $.jgrid.jqID(cellId)).val(result);


          if (result != null) {
            var holdamt = matAmt * (result / 100);
            if (action == 'RM') {
              if (holdamt > 0) holdamt = holdamt * -1;
            }

            $("#" + $.jgrid.jqID(rowid + "_holdamount")).val(holdamt);
            $("#" + $.jgrid.jqID(rowid + "_holdamount")).prop("readonly", true);
            $("#" + $.jgrid.jqID(rowid + "_actiontype")).prop("disabled", true);
          }
        } else {
          $("#" + $.jgrid.jqID(cellId)).val(null);
          $("#" + $.jgrid.jqID(rowid + "_holdamount")).val("0");
          $("#" + $.jgrid.jqID(rowid + "_holdamount")).prop("readonly", false);
          $("#" + $.jgrid.jqID(rowid + "_actiontype")).prop("disabled", false);
        }
        if (holdtype == "DP" || holdtype == "CH") {
          $("#" + $.jgrid.jqID(rowid + "_bpname")).prop("readonly", true);
          $("#" + $.jgrid.jqID(rowid + "_freezehold")).prop("disabled", true);
          $("#" + $.jgrid.jqID(rowid + "_bpname")).val(null);
          $("#" + $.jgrid.jqID(rowid + "_associatedbp")).prop("disabled", true);
          $("#" + rowid + "_associatedbp").empty().trigger('change');
          if (inprdvtrxtype == 'POD') {
            $("#" + $.jgrid.jqID(rowid + "_accounttype")).val("A");
            $("#" + $.jgrid.jqID(rowid + "_accounttype")).prop("disabled", true);
            getUniqueCode(rowid, null, null, "A", null, "true");
          }
        } else {
          $("#" + $.jgrid.jqID(rowid + "_bpname")).prop("readonly", false);
          $("#" + $.jgrid.jqID(rowid + "_freezehold")).prop("disabled", false);
          $("#" + $.jgrid.jqID(rowid + "_associatedbp")).prop("disabled", false);
          //$("#" + $.jgrid.jqID( rowid + "_associatedbp")).val(null);  
          if (inprdvtrxtype == 'POD') {
            $("#" + $.jgrid.jqID(rowid + "_accounttype")).prop("disabled", false);
            getUniqueCode(rowid, null, null, $("#" + $.jgrid.jqID(rowid + "_accounttype")).val(), null, "true");
          }
        }
      });
    }
  });
};


function reSizeGrid() {
  if (window.innerWidth) {
    gridW = window.innerWidth - 52;
    gridH = window.innerHeight - 241;
  } else if (document.body) {
    gridW = document.body.clientWidth - 52;
    gridH = document.body.clientHeight - 241;
  }
  if (onSearch == 1) {
    gridH = gridH - 23;
    if (navigator.userAgent.toLowerCase().indexOf("webkit") != -1) gridH++;
  } else if (parseInt(document.getElementById("client").scrollHeight) + 77 > parseInt(document.body.clientHeight)) gridW = gridW - 14;
  if (gridW < 800) gridW = 800;
  if (gridH < 200) gridH = 200;
  salaryGrid.setGridWidth(gridW, true);
  salaryGrid.setGridHeight(gridH, true);
}

function getBusinesspartner(rowid, response, options, edittype) {
  setTimeout(function () {
    $("#" + rowid + "_associatedbp").select2(selectBoxAjaxPaging({
      url: function () {
        return contextPath + '/RdvHoldActionAjax?action=getbusinesspartner'
      },
      data: [{
        id: jQuery('#HoldList').jqGrid('getRowData', rowid).bpartnerId,
        text: jQuery('#HoldList').jqGrid('getRowData', rowid).bpartnername
      }],
      size: "med/popup"
    }));

    $("#" + rowid + "_associatedbp").on("select2:unselecting", function (e) {
      document.getElementById(rowid + "_associatedbp").options.length = 0;
    });
    getUniqueCode(rowid, response, options, $("#" + $.jgrid.jqID(rowid + "_accounttype")).val(), null, "false");
  }, 100);
}

function getUniqueCode(rowid, response, options, type, bpartnerId, acctchange) {
  if (inprdvtrxtype == 'POD') {
    bpartnerId = $("#" + rowid + "_associatedbp").val();
  }
  setTimeout(function () {
    $("#" + rowid + "_uniquecode").select2(selectBoxAjaxPaging({

      url: function () {
        return contextPath + '/RdvHoldActionAjax?action=getUniquecodeList&type=' + type + '&bpartnerId=' + bpartnerId
      },
      data: [{
        id: jQuery('#HoldList').jqGrid('getRowData', rowid).uniquecodeId,
        text: jQuery('#HoldList').jqGrid('getRowData', rowid).uniquecodehid
      }],
      size: "med/popup"
    }));
    if (acctchange == "true" && $("#" + $.jgrid.jqID(rowid + "_accounttype")).val() == type) {
      $("#" + rowid + "_uniquecode").empty().trigger('change');
    }

    $("#" + rowid + "_uniquecode").on("select2:unselecting", function (e) {
      document.getElementById(rowid + "_uniquecode").options.length = 0;
    });
  }, 100);
}

function getLineUniqueCode(rowid, response, options) {
  var data = [{
    id: inplineuniquecodeId,
    text: inplineuniquecode
  }];
  $("#" + rowid + "_uniquecode").select2({
    data: data
  });
  // $("#" + rowid + "_uniquecode").prop("disabled", true);
  $("#" + rowid + "_uniquename").val(inplineuniquecodeName);
}

function getHoldType(inpholdTypeId, rowid) {
  var holdtype = "";

  var holdtype = "";
  $.ajax({
    type: 'GET',
    url: contextPath + '/RdvHoldActionAjax?action=getDeductionType',
    data: {
      inpholdTypeId: inpholdTypeId
    },
    dataType: 'json',
    async: false
  }).done(function (response) {
    var result = response.value;
    var resultpresnt = response.valuepresent;
    holdtype = response.type;
  });
  return holdtype;
}


function loadholdtype(action, rowid) {
  holdList = '<option role="option" value="0">' + select + '</option>';
  $.ajax({
    url: contextPath + '/RdvHoldActionAjax?action=getHoldTypeBaseAction',
    contentType: 'application/json; charset=utf-8',
    dataType: 'json',
    type: 'GET',
    async: false,
    data: {
      inpaction: action
    },
    success: function (result) {
      console.log(result.length);
      if (result != undefined && result.length > 0) {
        for (var i = 0; i < result.length; i++) {
          holdList += '<option role="option" value="' + result[i].ID + '">' + result[i].Name + '</option>';
        }
      }
    },
  });
  return holdList;
}

function disableAllFields(rowid, flag) {
  $("#" + $.jgrid.jqID(rowid + "_accounttype")).prop("disabled", flag);
  $("#" + $.jgrid.jqID(rowid + "_action")).prop("disabled", flag);
  $("#" + $.jgrid.jqID(rowid + "_associatedbp")).prop("disabled", flag);
  $("#" + $.jgrid.jqID(rowid + "_actiontype")).prop("disabled", flag);
  $("#" + $.jgrid.jqID(rowid + "_bpname")).prop("readonly", flag);
  $("#" + $.jgrid.jqID(rowid + "_freezehold")).prop("disabled", flag);
  $("#" + $.jgrid.jqID(rowid + "_actionreason")).prop("readonly", flag);
  $("#" + $.jgrid.jqID(rowid + "_actionjustfication")).prop("readonly", flag);
  $("#" + $.jgrid.jqID(rowid + "_uniquecode")).prop("disabled", flag);
  $("#" + $.jgrid.jqID(rowid + "_holdtype")).prop("disabled", flag);
  $("#" + $.jgrid.jqID(rowid + "_actionDate")).prop("readonly", true);
  if (flag == true) $("#" + $.jgrid.jqID(rowid + "_actionDate")).next().remove();
  $("#" + $.jgrid.jqID(rowid + "_actionDate")).prop("editable", false);
}

function refreshParent() {
  window.parent.parent.OB.MainView.TabSet.getSelectedTab().pane.activeView.dataSource.view.viewGrid.refreshGrid();
  window.parent.parent.OB.MainView.TabSet.getSelectedTab().pane.activeView.dataSource.view.viewForm.refresh();
}