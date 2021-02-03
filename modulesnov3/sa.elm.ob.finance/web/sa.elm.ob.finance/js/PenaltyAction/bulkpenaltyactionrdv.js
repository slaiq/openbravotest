/*
 *************************************************************************
 * All Rights Reserved.
 
 *************************************************************************
 */

var onSearch = 0;
var salaryGrid = jQuery("#SalaryList");
//Load Salary Details Grid
var salaryUrl = "&inpselectedRecordsId=" + document.getElementById("inpselectedRecordsId").value;
var lineno = document.getElementById("inplineno").value;
var bpartnername = document.getElementById("inpbpartnername").value;
var invoiceNo = document.getElementById("inpinvoiceNo").value;
var trxappNo = document.getElementById("inptrxappNo").value;
var matAmt = document.getElementById("inpmatamt").value;
var netAmt = document.getElementById("inpnetamt").value;
var penaltyType = document.getElementById("inppenaltytype").value;
var thershold = document.getElementById("inppenaltyThershold").value;
var penaltyAmt = matAmt * (thershold / 100);
var inptodaydate = document.getElementById("inptodaydate").value;
var inpuniquecode = document.getElementById("inpuniquecode").value;
var inplineuniquecode = document.getElementById("inplineuniquecode").value;
var inplineuniquecodeName = document.getElementById("inplineuniquecodeName").value;
var inplineuniquecodeId = document.getElementById("inplineuniquecodeId").value;
var inpverstatus = document.getElementById("inpverstatus").value;

var inprdvtrxtype = document.getElementById("inprdvtrxtype").value;
var inpPenaltyAccountType = document.getElementById("inpDefaultPenaltyAcctType").value;

var lastsel = "";
var pendeducttype = "";
var successflag = false;
var saveFlag = 0;
jQuery(function () {
  salaryGrid.jqGrid({
    url: contextPath + '/PenaltyActionAjax?action=GetSalaryDetails' + salaryUrl,
	    colNames: [action,bulkpenaltyamountlogic, actionDate, penaltyTypes, penaltyPercent, penaltyAmount, actionReason, actionJustification, 'BpartnerId', 'BpartnerName', asociatedBp, bpName,freezePenalty, amrasafNo, amrasafAmt, deductionAcctType, 'UniquecodeId', uniquecode, uniquecode, uniquecodeName, 'deductiontype', 'penaltyRelId', 'orgPenaltyAmt'],
    colModel: [ {
      name: "actions",
      width: 100,
      formatter: "actions",
      hidden: true
    },{
        name: 'bulkpenaltyamountlogic',
        index: 'bulkpenaltyamountlogic',
        width: '70px',
        fixed: true,
        editable: true,
        edittype: 'select',
        formatter: 'select',
        editoptions: {
          value: "AL:" + alllinepenalty + ";DIS:" + distribute
        },
        searchoptions: {
          clearSearch: true,
          attr: {
            size: 60,
            maxlength: 90,
            style: "width:150px;margin-top:1px;"
          }
        }
      },{
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
    },  {
      name: 'penaltytype',
      index: 'penaltytype',
      width: '250px',
      fixed: true,
      editable: true,
      edittype: 'select',
      formatter: 'select',
      editoptions: {
        value: penaltyType,
        dataEvents: [{
          type: 'change',
          data: {
            dateColumn: "penaltypercentage"
          },
         fn: peanltyChange
        }]
      }
    }, {
      name: 'penaltypercentage',
      index: 'penaltypercentage',
      width: '50px',
      fixed: true,
      editable: true,
      editoptions: {
        defaultValue: "0.00",
        readonly: "readonly"
      },
    }, {
      name: 'penaltyamount',
      index: 'penaltyamount',
      width: '70px',
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
     // formatter: 'number'

    }, {
      name: 'actionreason',
      index: 'actionreason',
      width: '150px',
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
      width: '150px',
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
    },
    {
        name: 'freezepenalty',
        index: 'freezepenalty',
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

      },{
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
      fixed: true,
      hidden: true
    }, {
      name: 'uniquecode',
      index: 'uniquecode',
      width: '350px',
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
      name: 'penaltyrelId',
      index: 'penaltyrelId',
      width: '100px',
      hidden: true
    }, {
      name: 'orgPenaltyAmt',
      index: 'orgPenaltyAmt',
      width: '100px',
      hidden: true
    }],

    editurl: contextPath + '/PenaltyActionAjax?action=savebulkpenaltyaction' + salaryUrl,
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
    onSelectRow: function (id) {

      var penaltyRelId = jQuery('#SalaryList').jqGrid('getRowData', id).penaltyrelId;
      if (penaltyRelId != null && penaltyRelId != "" && penaltyRelId != "null") {
        $("#SalaryList_iledit").addClass(" ui-state-disabled");
      } else {
        $("#SalaryList_iledit").removeClass(" ui-state-disabled");
      }
    },
    loadComplete: function (data) {
      ChangeJQGridAllRowColor(salaryGrid);

      reSizeGrid();
      gridIds = jQuery("#SalaryList").getDataIDs();
      gridIdsLength = gridIds.length;
      $("#SalaryList_iladd").insertBefore("#del_SalaryList");
      //$("#SalaryList_iledit").insertAfter("#SalaryList_iladd");
      $("#SalaryList_ilsave").insertAfter("#SalaryList_iledit");
    //  $("#SalaryList_ilcancel").insertAfter("#del_SalaryList");
    },

    caption: ''
  });

  salaryGrid.jqGrid('navGrid', '#pager', {
    edit: false,
    add: false,
    del: false,
    search: false,
    view: false,
    refreshtext: reload,
    beforeRefresh: function () {
      reSizeGrid();
    }
  },

  {}, {}, {}, {});


  jQuery('#SalaryList').jqGrid('inlineNav', '#pager', {
    edit: false,
    add: true,
    addtext: add,
    cancel:false,
    savetext: save,
    addParams: {
      position: "last",
      addRowParams: {
        keys: true,
        //position: "last",
        oneditfunc: function (rowid, response, options) {
          saveFlag = 1;
          $("#" + $.jgrid.jqID(rowid + "_accounttype")).val(inpPenaltyAccountType);
          getBusinesspartner(rowid, response, options, "false");
          getUniqueCode(rowid, response, options, inpPenaltyAccountType, null, "false");
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


  jQuery("#SalaryList").trigger("reloadGrid");
  salaryGrid.jqGrid('filterToolbar', {
    searchOnEnter: false
  });

});

function reloadGrid(result) {
  setTimeout(function () {
    $("#SalaryList").trigger("reloadGrid");
    saveFlag = 0;
  }, 50);
}

var addOptions = '';

function saverow(options, rowid) {
  var cellId = rowid + "_actiontype";
  var action = $("#" + $.jgrid.jqID(rowid + "_actiontype")).val();
  var penaltyamount = $("#" + $.jgrid.jqID(rowid + "_penaltyamount")).val();
  var matchAmt = $("#" + $.jgrid.jqID(rowid + "_Amt")).val();
	  var netAmt = $("#" + $.jgrid.jqID(rowid + "_NetAmt")).val();
  var penaltytype = $("#" + $.jgrid.jqID(rowid + "_penaltytype")).val();
  var associatedbp = $("#" + $.jgrid.jqID(rowid + "_associatedbp")).val();
  var uniquecode = $("#" + $.jgrid.jqID(rowid + "_uniquecode")).val();
  var penaltypercent = $("#" + $.jgrid.jqID(rowid + "_penaltypercentage")).val();
  var penaltydeductionaccttype = $("#" + $.jgrid.jqID(rowid + "_accounttype")).val();
  var bulkpenaltyamtlogic = $("#" + $.jgrid.jqID(rowid + "_bulkpenaltyamountlogic")).val();

  if (inpverstatus != "DR") {
    OBAlert(versionstatusinvoice);
    return false;
  }
  if (penaltytype == '0') {
    OBAlert(penamtshouldnotneg);
    return false;
  }
  if (penaltyamount == '') {
    OBAlert(penaltyamtnotblank);
    return false;
  }
  if (penaltyamount == 0 && penaltypercent==0) {
    OBAlert(penaltyAmtNotZero);
    return false;
  }
  if ((uniquecode == null || uniquecode == "0" || uniquecode == "" || uniquecode == null || uniquecode == "null") && penaltydeductionaccttype == 'A') {
    OBAlert(uniquecodemandatory);
    return false;
  }
  if (penaltytype != null && penaltytype != "0" && inprdvtrxtype != 'POD') {
    var deductype = getPenaltyType(penaltytype, rowid);

    if (deductype == "ECA" || deductype == "IGI") {
      if (associatedbp == "0" || associatedbp == "" || associatedbp == null || associatedbp == "null") {
        OBAlert(bpmandatory);
        return false;
      }
    }
  }
    
    if (parseFloat(penaltyamount) < 0) {
      OBAlert(addpositive);
      return false;
    } else {
//	      if (parseFloat(netAmt) < parseFloat(penaltyamount)) {
//        OBAlert(penamtgreterthanmatchamt);
//        return false;
//      } else {
        var existsgreaterMatchAmt = chkpenAmtgreaterthanMatchAmt(penaltyamount,penaltypercent,bulkpenaltyamtlogic);
        if (existsgreaterMatchAmt == 'N') {
	          OBAlert(bulkpenaltyAmtGreater);
          return false;
        //}
     }
    }
  
  jQuery("#SalaryList").saveRow(rowid, {

    aftersavefunc: reloadGrid
  });
  saveFlag = 0;
  window.parent.parent.OB.MainView.TabSet.getSelectedTab().pane.activeView.dataSource.view.viewForm.view.messageBar.setMessage("success", bulkpenaltysuccess);
  closePage();
  
  return true;
  //return false;
}

function chkPenaltytypeGoingdownNegativeval(penaltyAmt, penaltyType, response, type) {
  var Checkcontractvalidation = "N";
  $.ajax({
    type: 'GET',
    url: contextPath + '/PenaltyActionAjax?action=chkPenaltytypeGoingdownNegativeval',
    data: {
      inpRDVTxnLineId: document.getElementById("inpRDVTxnLineId").value,
      penaltyAmt: penaltyAmt,
      penaltyType: penaltyType,
      id: response,
      type: type
    },
    dataType: 'json',
    async: false
  }).done(function (response) {
    Checkcontractvalidation = response.isExists;

  });
  return Checkcontractvalidation;
}

function chkpenAmtgreaterthanMatchAmt(penaltyAmt, penaltypercentage, bulkpenaltyamtlogic) {
  var ChkpenAmtgreaterMatchAmt = "Y";
  $.ajax({
    type: 'GET',
    url: contextPath + '/PenaltyActionAjax?action=getTotalMatchAmtforSelectedRecords',
    data: {
    	inpselectedRecordsId: document.getElementById("inpselectedRecordsId").value,
      penaltyAmt: penaltyAmt,
      penaltypercentage: penaltypercentage,
      bulkpenaltyamtlogic: bulkpenaltyamtlogic
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

  if ($this.val() != '0' && $this.val() != null && $this.val() != "") {
    $.ajax({
      type: 'GET',
      url: contextPath + '/PenaltyActionAjax?action=getUniqueCodeName',
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
    "");
  }
};


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
      if(inplineuniquecodeName != null && inplineuniquecodeName !="" && inplineuniquecodeName !="null"){
      $("#" + $.jgrid.jqID(rowid + "_uniquename")).val(inplineuniquecodeName);
      }else{
    	  $("#" + $.jgrid.jqID(rowid + "_uniquename")).val("");
      }
      
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
      url: contextPath + '/PenaltyActionAjax?action=getbpName',
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
    var pentype = $("#" + $.jgrid.jqID(rowid + "_penaltytype")).val();
    var pendedtype = getPenaltyType(pentype, rowid);
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






peanltyChange = function (e) {
  var $this = $(e.target),
      columnName = e.data.dateColumn,
      rowid, cellId;

  // inline editing
  rowid = $this.closest("tr.jqgrow").attr("id");
  cellId = rowid + "_" + columnName;

  $.ajax({
    type: 'GET',
    url: contextPath + '/PenaltyActionAjax?action=getThershold',
    data: {
      inppenaltyTypeId: $this.val()
    },
    dataType: 'xml',
    async: false,
    success: function (data) {
      $(data).find("GetPenalty").each(function () {
        var result = $(this).find("value").text();
        var resultpresnt = $(this).find("valuepresent").text();
        var penaltytype = $(this).find("type").text();
        pendeducttype = penaltytype;
        var action = $("#" + $.jgrid.jqID(rowid + "_actiontype")).val();
        if (resultpresnt == 1) {
          var grid = jQuery("#SalaryList");
          $("#" + $.jgrid.jqID(cellId)).val(result);
          
          // if percentage greater than 0 then amount logic should be All Line type only 
          // NOTE: Distribute amount type will be only for penalty amount
          if(result != null && result != "null" && result != "" && result != 0 ){
        	  $("#" + $.jgrid.jqID(rowid + "_bulkpenaltyamountlogic")).val("AL");
        	  $("#" + $.jgrid.jqID(rowid + "_bulkpenaltyamountlogic")).prop("disabled", true);
          }

          if (result != null) {
//            var penaltyamt = matAmt * (result / 100);
//            if (action == 'RM') {
//              if (penaltyamt > 0) penaltyamt = penaltyamt * -1;
//            }

            $("#" + $.jgrid.jqID(rowid + "_penaltyamount")).val("0");
            $("#" + $.jgrid.jqID(rowid + "_penaltyamount")).prop("readonly", true);
            $("#" + $.jgrid.jqID(rowid + "_actiontype")).prop("disabled", true);
          }
        } else {
          $("#" + $.jgrid.jqID(cellId)).val(null);
          $("#" + $.jgrid.jqID(rowid + "_penaltyamount")).val("0");
          $("#" + $.jgrid.jqID(rowid + "_penaltyamount")).prop("readonly", false);
          $("#" + $.jgrid.jqID(rowid + "_actiontype")).prop("disabled", false);
          $("#" + $.jgrid.jqID(rowid + "_bulkpenaltyamountlogic")).prop("disabled", false);
        }
        if (penaltytype == "DP" || penaltytype == "CH") {
          $("#" + $.jgrid.jqID(rowid + "_bpname")).prop("readonly", true);
          $("#" + $.jgrid.jqID(rowid + "_freezepenalty")).prop("disabled", true);
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
          $("#" + $.jgrid.jqID(rowid + "_freezepenalty")).prop("disabled", false);
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
    $("#" + $.jgrid.jqID(rowid + "_bpname")).val(null);
    $("#" + rowid + "_associatedbp").empty().trigger('change');
    getBusinesspartner(rowid, null, null, "false");
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
	 var penaltytypeId = $("#" + $.jgrid.jqID(rowid + "_penaltytype")).val();
	 var rdvTxnLnId = document.getElementById("inpRDVTxnLineId").value;
	 var inpselectedRecordsId = document.getElementById("inpselectedRecordsId").value;
	 var lineArray = inpselectedRecordsId.split(",");
    $("#" + rowid + "_associatedbp").select2(selectBoxAjaxPaging({
      url: function () {
        return contextPath + '/PenaltyActionAjax?action=getbusinesspartner&pType='+penaltytypeId +'&rdvln='+lineArray[0]
      },
      data: [{
        id: jQuery('#SalaryList').jqGrid('getRowData', rowid).bpartnerId,
        text: jQuery('#SalaryList').jqGrid('getRowData', rowid).bpartnername
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
        return contextPath + '/PenaltyActionAjax?action=getUniquecodeList&type=' + type + '&bpartnerId=' + bpartnerId
      },
      data: [{
        id: jQuery('#SalaryList').jqGrid('getRowData', rowid).uniquecodeId,
        text: jQuery('#SalaryList').jqGrid('getRowData', rowid).uniquecodehid
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
  if(inplineuniquecodeName!=null && inplineuniquecodeName!="null" && inplineuniquecodeName!=""){
  $("#" + rowid + "_uniquename").val(inplineuniquecodeName);
}
  else{
	  $("#" + rowid + "_uniquename").val("");
  }
}

function getPenaltyType(inppenaltyTypeId, rowid) {
  var penaltytype = "";

  var penaltytype = "";
  $.ajax({
    type: 'GET',
    url: contextPath + '/PenaltyActionAjax?action=getDeductionType',
    data: {
      inppenaltyTypeId: inppenaltyTypeId
    },
    dataType: 'json',
    async: false
  }).done(function (response) {
    var result = response.value;
    var resultpresnt = response.valuepresent;
    penaltytype = response.type;
  });
  return penaltytype;
}




function disableAllFields(rowid, flag) {
  $("#" + $.jgrid.jqID(rowid + "_accounttype")).prop("disabled", flag);
  $("#" + $.jgrid.jqID(rowid + "_action")).prop("disabled", flag);
  $("#" + $.jgrid.jqID(rowid + "_associatedbp")).prop("disabled", flag);
  $("#" + $.jgrid.jqID(rowid + "_actiontype")).prop("disabled", flag);
  $("#" + $.jgrid.jqID(rowid + "_bpname")).prop("readonly", flag);
  $("#" + $.jgrid.jqID(rowid + "_freezepenalty")).prop("disabled", flag);
  $("#" + $.jgrid.jqID(rowid + "_actionreason")).prop("readonly", flag);
  $("#" + $.jgrid.jqID(rowid + "_actionjustfication")).prop("readonly", flag);
  $("#" + $.jgrid.jqID(rowid + "_uniquecode")).prop("disabled", flag);
  $("#" + $.jgrid.jqID(rowid + "_penaltytype")).prop("disabled", flag);
  $("#" + $.jgrid.jqID(rowid + "_actionDate")).prop("readonly", true);
  if (flag == true) $("#" + $.jgrid.jqID(rowid + "_actionDate")).next().remove();
  $("#" + $.jgrid.jqID(rowid + "_actionDate")).prop("editable", false);
}

function refreshParent() {
  window.parent.parent.OB.MainView.TabSet.getSelectedTab().pane.activeView.dataSource.view.viewGrid.refreshGrid();
  window.parent.parent.OB.MainView.TabSet.getSelectedTab().pane.activeView.dataSource.view.viewForm.refresh();
}