<%@page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page import="sa.elm.ob.finance.properties.Resource, sa.elm.ob.utility.util.Utility"
    errorPage="/web/jsp/ErrorPage.jsp"%>
 <%@ page import="sa.elm.ob.finance.ad_forms.journalapproval.vo.GLJournalApprovalVO, java.util.List,java.util.ArrayList" %>
 <%@page import="org.codehaus.jettison.json.JSONArray"%>
<%@page import="org.codehaus.jettison.json.JSONObject"%>
<%
    String lang = ((String) session.getAttribute("#AD_LANGUAGE"));
String style="../web/skins/ltr/org.openbravo.userinterface.skin.250to300Comp/250to300Comp/Openbravo_ERP.css";
if(lang.equals("ar_SA")){
     style="../web/skins/rtl/org.openbravo.userinterface.skin.250to300Comp/250to300Comp/Openbravo_ERP_250.css";
}
List<GLJournalApprovalVO> OrgList = (request.getAttribute("organization")==null?null:(ArrayList<GLJournalApprovalVO>)request.getAttribute("organization"));
List<GLJournalApprovalVO> SchemaList = (request.getAttribute("acctschema")==null?null:(ArrayList<GLJournalApprovalVO>)request.getAttribute("acctschema"));
List<GLJournalApprovalVO> PeriodList = (ArrayList<GLJournalApprovalVO>)request.getAttribute("period");
List<GLJournalApprovalVO> FundsList = (ArrayList<GLJournalApprovalVO>)request.getAttribute("funds");
List<GLJournalApprovalVO> deptList = (ArrayList<GLJournalApprovalVO>)request.getAttribute("deptList");
List<GLJournalApprovalVO> projList = (ArrayList<GLJournalApprovalVO>)request.getAttribute("projList");
List<GLJournalApprovalVO> actList = (ArrayList<GLJournalApprovalVO>)request.getAttribute("actList");
List<GLJournalApprovalVO> user1List = (ArrayList<GLJournalApprovalVO>)request.getAttribute("user1List");
List<GLJournalApprovalVO> user2List = (ArrayList<GLJournalApprovalVO>)request.getAttribute("user2List");
List<GLJournalApprovalVO> bpList = (ArrayList<GLJournalApprovalVO>)request.getAttribute("bpList");

JSONObject LinesList=null;
if(request.getAttribute("LinesList")!=null){
 LinesList = new JSONObject(request.getAttribute("LinesList").toString());
}%>

<HTML xmlns:="http://www.w3.org/1999/xhtml">
<HEAD>
<META http-equiv="Content-Type" content="text/html; charset=utf-8"></META>
<LINK rel="stylesheet" type="text/css" href="<%=style %>" id="paramCSS"></LINK>
<link rel="stylesheet" type="text/css"
    href="../web/js/jquery/jquery-ui-1.8.11.custom.css" />
<link rel="stylesheet" type="text/css"
    href="../web/js/themes/base/jquery.ui.tabs.css"></link>
<link rel="stylesheet" type="text/css"
    href="../web/js/jquery/ui.jqgrid.css" />
    <style>
     th.DataGrid_Header_Cell_Amount {
  font-family: 'lucida sans', sans-serif;
  font-size: 11px;
  text-align: center;
  border-top: 1px solid #CDD7BB;
  border-bottom: 1px solid #A7ABB4;
  border-right: 1px solid #BBBFB6;
  border-left : 1px solid #BBBFB6;
  background-position: bottom left;
  background-image: url(Common/DataGrid/backgroundHeader.normal.png);  
  background-color: #ECEEE9;
  _background-color: #ECEEE9;
    }
    </style>

<script type="text/javascript" src="../web/js/utils.js"></script>
<script type="text/javascript" src="../web/js/windowKeyboard.js"></script>
<script type="text/javascript" src="../web/js/common/common.js"></script>
<script  type="text/javascript" src="../web/js/shortcuts.js"></script>
      <script  type="text/javascript" src="../web/js/searchs.js" ></script>
      <script  type="text/javascript" src="../web/js/ajax.js" ></script>
      <script  type="text/javascript" src="../web/js/default/MessageBox.js" ></script>
      <script  type="text/javascript" src="../web/js/messages.js" ></script>
      <script  type="text/javascript" src="../utility/DynamicJS.js" ></script>
      <script  type="text/javascript" src="../web/js/jscalendar/calendar.js" ></script>
      <script  type="text/javascript" src="../web/js/jscalendar/lang/calendar-es.js"  id="fieldCalendar"></script>
      <script  type="text/javascript" src="../web/js/default/DateTextBox.js" ></script>
      <script  type="text/javascript" src="../web/js/dojoConfig.js" ></script>
      <script  type="text/javascript" src="../web/js/dojo/dojo.js" ></script>
      <script type="text/javascript" src="../web/js/jquery.js"></script>    
<script type="text/javascript" src="../web/js/jquery-1.7.2.min.js"></script>
<script type="text/javascript" src="../web/js/jquery/jquery-ui-1.8.6.custom.min.js"></script>
<script type="text/javascript" src="../web/js/jquery/grid.locale-en.js"></script>
<script type="text/javascript" src="../web/js/jquery/jquery.jqGrid.min.js"></script>
<script type="text/javascript" src="../web/js/ui/jquery.ui.core.js"></script>
<script type="text/javascript" src="../web/js/ui/jquery.ui.widget.js"></script>
<script type="text/javascript" src="../web/js/ui/jquery.ui.datepicker.js"></script>
<script type="text/javascript" src="../web/js/common/json2.js"></script>
<script type="text/javascript">
function validate(action) {
    return true;
}
function onResizeDo() {
    resizeArea();
}
function onLoadDo() {
    this.windowTables = new Array(
            new windowTableId('client', 'buttonHTML')
          );
          setWindowTableParentElement();
          this.tabsTables = new Array(
            new tabTableId('tdtopTabs')
          );
          setTabTableParentElement();
          enableShortcuts('edition');
          setBrowserAutoComplete(false);
/* 
          var selectedGroupBy = document.getElementById("paramSelectedGroupBy").value;
          var groupby = document.getElementById("inpGroupBy");
          for (i=0;i<groupby.length;i++){
            if (groupby[i].value == selectedGroupBy)
              groupby[i].selected = true;
          } */
          var invalidDate = "<%=request.getAttribute("InvalidDate")==null ? "" : request.getAttribute("InvalidDate")%>";
            if(invalidDate=="Y"){
                OBAlert("<%= Resource.getProperty("finance.invalidperiods",lang)%>");
            }
          
          try {
            onloadFunctions();
          } catch (e) {}
          resizeArea();
          updateMenuIcon('buttonMenu');

          setWindowElementFocus('firstElement');
          displayAdvancedFilters();
          displaySectionResult();
}
function onloadFunctions() {
    
      displayLogic(); 
      keyArray[keyArray.length] = new keyArrayItem("ENTER", "openSearch(null, null, '../info/EfinAccountElementValue.html', 'SELECTOR_ACCOUNTELEMENTVALUE', false, 'frmMain', 'inpcElementValueIdFrom', 'inpElementValueIdFrom_DES', document.frmMain.inpElementValueIdFrom_DES.value, 'inpcAcctSchemaId', document.frmMain.inpcAcctSchemaId.value, 'Command', 'KEY');", "inpElementValueIdFrom_DES", "null");
      keyArray[keyArray.length] = new keyArrayItem("ENTER", "openSearch(null, null, '../info/EfinAccountElementValue.html', 'SELECTOR_ACCOUNTELEMENTVALUE', false, 'frmMain', 'inpcElementValueIdTo', 'inpElementValueIdTo_DES', document.frmMain.inpElementValueIdTo_DES.value, 'inpcAcctSchemaId', document.frmMain.inpcAcctSchemaId.value, 'Command', 'KEY');", "inpElementValueIdTo_DES", "null");
    }
function displayLogic() {
   displayLogicElement('AcctFromTo', true);
   displayLogicElement('Filter1', true);
   displayLogicElement('Filter2', true);
   displayLogicElement('Filter3', true);
   displayLogicElement('Filter4', true);
   displayLogicElement('Filter5', true);
   displayLogicElement('Filter6', true);
}
function displaySectionResult(){
    
    <% if(LinesList != null){%>
       var lineListJSON = '<%= LinesList.toString().replace("\\", "\\\\").replace("'", "\\\'") %>'; 
       
      var lineListJSONObj = JSON && JSON.parse(lineListJSON) || $.parseJSON(lineListJSON);
      
      var data = lineListJSONObj.list;
      for ( var j in data) {
        
      var object =  data[j].transaction;
          object.sort(function(a,b){
            return (new Date(a.date).getTime() - new Date(b.date).getTime());
          });
      }
      document.getElementById("sectionGridView").style.display=""
      var newdiv = document.getElementById("sectionGridView");
        for ( var j in data) {
            var transaction = data[j].transaction.length;
            var object =  data[j].transaction;
            
            var table = document.createElement("table");
           table.setAttribute("class", "DataGrid_Header_Table DataGrid_Body_Table");
            table.width="100%";                               
            table.id="UniquecCode_"+data[j].uniquecode;                                 
           //table.style.margin = "0 0 0 80px";
           table.style.layou="auto";
            newdiv.appendChild(table);
            
          
            
            var mytable ="<thead>";
            
          
            mytable =mytable +" <tr class='DataGrid_Body_Row'><th width='232'  class='DataGrid_Header_Cell' colspan='1'  class='DataGrid_Header_Cell'>"+data[j].uniquecode+"</th><th width='232'  class='DataGrid_Header_Cell' colspan='3'  class='DataGrid_Header_Cell'></th><th width='232'  class='DataGrid_Header_Cell' colspan='3'  class='DataGrid_Header_Cell'></th><th width='232'  class='DataGrid_Header_Cell' colspan='3'  class='DataGrid_Header_Cell'></th></tr>";
            mytable =mytable +"<th width='116' class='DataGrid_Header_Cell'   class='DataGrid_Header_Cell'></th> <th width='116' class='DataGrid_Header_Cell'  colspan='3' class='DataGrid_Header_Cell'><%= Resource.getProperty("finance.ptd.o/b",lang) %></th>";
            mytable =mytable +" <th width='232'  class='DataGrid_Header_Cell' colspan='3'  class='DataGrid_Header_Cell'><%= Resource.getProperty("finance.ptd.ptd",lang) %></th><th width='116'  class='DataGrid_Header_Cell' colspan='3'  class='DataGrid_Header_Cell'><%= Resource.getProperty("finance.ptd.ytd",lang) %></th>";
            mytable =mytable +"</tr> <tr class='DataGrid_Body_Row'>  <th width='116' class='DataGrid_Header_Cell' class='DataGrid_Header_Cell'><%= Resource.getProperty("finance.period",lang) %></th> <th width='232'  class='DataGrid_Header_Cell'  class='DataGrid_Header_Cell'><%= Resource.getProperty("finance.ptd.dr",lang) %></th>";
            mytable =mytable +"<th width='116'  class='DataGrid_Header_Cell'   class='DataGrid_Header_Cell'><%= Resource.getProperty("finance.ptd.cr",lang) %></th><th width='116'  class='DataGrid_Header_Cell'   class='DataGrid_Header_Cell'><%= Resource.getProperty("finance.ptd.net",lang) %></th>  <th width='232'  class='DataGrid_Header_Cell'  class='DataGrid_Header_Cell'><%= Resource.getProperty("finance.ptd.dr",lang) %></th>";
            mytable =mytable +"<th width='116'  class='DataGrid_Header_Cell'   class='DataGrid_Header_Cell'><%= Resource.getProperty("finance.ptd.cr",lang) %></th><th width='116'  class='DataGrid_Header_Cell'   class='DataGrid_Header_Cell'><%= Resource.getProperty("finance.ptd.net",lang) %></th> <th width='232'  class='DataGrid_Header_Cell'  class='DataGrid_Header_Cell'><%= Resource.getProperty("finance.ptd.dr",lang) %></th>";
            mytable =mytable +"<th width='116'  class='DataGrid_Header_Cell'   class='DataGrid_Header_Cell'><%= Resource.getProperty("finance.ptd.cr",lang) %></th>  <th width='116'  class='DataGrid_Header_Cell'   class='DataGrid_Header_Cell'><%= Resource.getProperty("finance.ptd.net",lang) %></th> </tr>  ";                               
            mytable =mytable+"</thead>";
            table.innerHTML=mytable;
                                      
            
            var intialbalDr="0.00",intialbalCr="0.00",intialbalNet="0.00";
            for(var p in object) {
                var processId="800001";
                var rowCount = table.rows.length;
                var row = table.insertRow(rowCount);
                var i=0;
                row.className = "DataGrid_Body_Row DataGrid_Body_Row_" + (( i + 1) % 2);
          for ( var k = 0; k <= 9; k++) {
              var cell = row.insertCell(k);
              switch (k) {
              case 0:
                  cell.className = "DataGrid_Body_Cell";
                  cell.style.textAlign = "center";
                  cell.innerHTML = "<a  class='LabelLink'  href='javascript:OpenGeneralLedgerReport(\"" + processId + "\", \"" + object[p].id + "\", \"" + object[p].periodid + "\",\"" + data[j].uniquecode + "\")'>" + object[p].startdate + "</a>";
                 // cell.innerHTML = "<a  href='#' class='LabelLink' onclick='OpenGeneralLedgerReport('800001',\"" + object[p].id+ "\")'>" + object[p].startdate + "</a>";
                  break;
              case 1:
                  cell.className = "DataGrid_Body_Cell";
                  cell.style.textAlign = "right";
                  cell.style.padding = "right: 5 px";
                  if(object[p].type==0){
                      cell.innerHTML = intialbalDr;
                  }
                  else
                  cell.innerHTML = object[p].initialDr;
                  break;
              case 2:
                  cell.className = "DataGrid_Body_Cell DataGrid_Header_Cell_Amount";
                  cell.style.textAlign = "right";
                  if(object[p].type==0){
                      cell.innerHTML = intialbalCr;
                  }
                  else
                  cell.innerHTML = object[p].initialCr;
                  break;
              case 3:
                  cell.className = "DataGrid_Body_Cell";
                  cell.style.textAlign = "right";
                  if(object[p].type==0){
                      cell.innerHTML = intialbalNet;
                  }
                  else
                  cell.innerHTML = object[p].initialNet;
                  break;
              case 4:
                  cell.className = "DataGrid_Body_Cell";
                  cell.style.textAlign = "right";
                  cell.innerHTML = object[p].perDr;
                  break;
              case 5:
                  cell.className = "DataGrid_Body_Cell";
                  cell.style.textAlign = "right";
                  cell.innerHTML = object[p].perCr;
                  break;
              case 6:
                  cell.className = "DataGrid_Body_Cell";
                  cell.style.textAlign = "right";
                  cell.innerHTML = object[p].finalpernet;
                  break;
              case 7:
                  cell.className = "DataGrid_Body_Cell";
                  cell.style.textAlign = "right";
                  if(object[p].type==1){
                  cell.innerHTML = object[p].finaldr;
                  intialbalDr=object[p].finaldr;
                    }
                  else
                  cell.innerHTML = intialbalDr;
                  break;
              case 8:
                  cell.className = "DataGrid_Body_Cell";
                  cell.style.textAlign = "right";
                  if(object[p].type==1){
                  cell.innerHTML = object[p].finalcr;
                  intialbalCr= object[p].finalcr;
                  }
                  else
                      cell.innerHTML = intialbalCr;  
                  break;
              case 9:
                  cell.className = "DataGrid_Body_Cell";
                  cell.style.textAlign = "right";
                  if(object[p].type==1){
                  cell.innerHTML = object[p].finalnet;
                  intialbalNet=object[p].finalnet;
                  }
                  cell.innerHTML = intialbalNet;  
                  break;
                  
              }
          } 
         
        }
        }
            
    <%}%>
}
function reSizeGrid() {
    var gridW, gridH = 154;
    if (window.innerWidth) {
        gridW = window.innerWidth - 52;
        gridH = window.innerHeight - 187;
    }
    else if (document.body) {
        gridW = document.body.clientWidth - 52;
        gridH = document.body.clientHeight - 187;
    }
    if (parseInt(document.getElementById("client").scrollHeight) + 77 > parseInt(document.body.clientHeight)) {
        gridW = gridW - 15;
        gridH = gridH - 30;
    }
   /*  if (gridW <= 800)
        gridW = 800;
    if (gridH <= 300)
        gridH = 300; */
   
}
function OpenGeneralLedgerReport(procId, recordId,periodId,uniquecode){
      var frm = document.frmMain;
      markCheckedAllElements(frm.inpcBPartnerId_IN);
      markCheckedAllElements(frm.inpcProjectId_IN);
      markCheckedAllElements(frm.inpmProductId_IN);
      markCheckedAllElements(frm.inpcSalesregionId_IN);
      markCheckedAllElements(frm.inpcActivityId_IN);
      markCheckedAllElements(frm.inpcProjectId_IN);
      markCheckedAllElements(frm.inpcUser1Id_IN);
      markCheckedAllElements(frm.inpcUser2Id_IN);



      var myframe = getFrame('LayoutMDI') || top.opener;
      if (myframe) {      
        myframe.OB.RemoteCallManager.call('org.openbravo.client.application.ComputeTranslatedNameActionHandler', {}, {'processId': procId}, 
        function(response, data, request){
          myframe.OB.Layout.ViewManager.openView('OBClassicWindow', {
            command: 'FINDLINK',
            icon: '[SKINIMG]../../org.openbravo.client.application/images/application-menu/iconReport.png',
            id: procId,
           
             obManualURL: '/ad_reports/EfinReportGeneralLedger.html?inpcFromPeriodId=' + periodId
            + '&inpcToPeriodId=' + periodId + '&inpOrg=' + frm.inpOrg.value + '&inpcAcctSchemaId=' 
            + frm.inpcAcctSchemaId.value +  '&inpNotInitialBalance=Y&inpcCampaignId_IN=' + frm.inpcCampaignId_IN.value +'&inpcElementValueIdFrom=' + recordId + '&inpcElementValueIdTo=' + recordId + '&inpUniqueCode=' + uniquecode ,
             
            processId: procId,
            tabTitle: data.processTitle,
            type: 'report',
            viewId: 'OBClassicWindow'
          });                        
        });
      }
    }
    
function onClickRefresh() {
    submitCommandForm('DEFAULT', true, null, 'ReportTrialBalancePTD', '_self', null, true);
    return false;
}  
function displayAdvancedFilters(){
     if (advancedFiltersrow1.style.display == 'none') {
        displayLogicElement('advancedFiltersrow1', true);
        displayLogicElement('advancedFiltersrow2', true);
        displayLogicElement('AcctFromTo', true);
    }
    else{ 
        displayLogicElement('advancedFiltersrow1', false);
        displayLogicElement('advancedFiltersrow2', false);
        displayLogicElement('AcctFromTo', false);
      
    }     
}
function reloadWindow() 
{
    submitCommandForm('DEFAULT', false, null, 'ReportTrialBalancePTD', '_self', null, false); 
    return false;
}
function getPTDData(){
     var sel = document.getElementById("inpcSalesregionId_IN");
         var opts = sel.options;
         var Dept = "";
         var array = new Array();
         for(i = 0; i < opts.length; i++)
         {
                 Dept +=",'"+opts[i].value +"'";
         }
         var sel = document.getElementById("inpcActivityId_IN");
         var opts = sel.options;
         var activity = "";
         for(i = 0; i < opts.length; i++)
         {
             activity +=",'"+opts[i].value +"'";
         }         
         var sel = document.getElementById("inpcBPartnerId_IN");
         var opts = sel.options;
         var entity = "";
         for(i = 0; i < opts.length; i++)
         {
        	 entity +=",'"+opts[i].value +"'";
         }         
         var sel = document.getElementById("inpcProjectId_IN");
         var opts = sel.options;
         var project = "";
         for(i = 0; i < opts.length; i++)
         {
             project +=",'"+opts[i].value +"'";
         }
         var sel = document.getElementById("inpcUser1Id_IN");
         var opts = sel.options;
         var user1 = "";
         for(i = 0; i < opts.length; i++)
         {
             user1 +=",'"+opts[i].value +"'";
         }
         var sel = document.getElementById("inpcUser2Id_IN");
         var opts = sel.options;
         var user2 = "";
         for(i = 0; i < opts.length; i++)
         {
             user2 +=",'"+opts[i].value +"'";
         }
         document.getElementById("CBPartnerId_IN").value=entity;
         document.getElementById("CSalesRegionId_IN").value=Dept;
         document.getElementById("CActivityId_IN").value=activity;
         document.getElementById("CProjectId_IN").value=project;
         document.getElementById("CUser1Id_IN").value=user1;
         document.getElementById("CUser2Id_IN").value=user2;
    document.getElementById("inpAction").value="getPTDData";
    reloadWindow();
}
</script>
</HEAD>
 <body leftmargin="0" topmargin="0" marginwidth="0" marginheight="0" onload="onLoadDo();" onresize="onResizeDo();">
<form method="post" action="" name="frmMain"  target="_self">
  <input type="hidden" name="Command"></input>
  <input type="hidden" name="inpcAccountId"></input>
  <input type="hidden" name ="inpAction" id ="inpAction">
   <input type="hidden" id="paramFromDate" name="paramFromDate" value=""></input>
    <input type="hidden" id="paramToDate" name="paramToDate" value =""></input>
     <input type="hidden" id="paramDefRegion" name="paramDefRegion" value =""></input>
     <input type="hidden" id="CSalesRegionId_IN" name="CSalesRegionId_IN" value =""></input>
     <input type="hidden" id="CActivityId_IN" name="CActivityId_IN" value =""></input>
      <input type="hidden" id="CProjectId_IN" name="CProjectId_IN" value =""></input>
     <input type="hidden" id="CUser1Id_IN" name="CUser1Id_IN" value =""></input>
     <input type="hidden" id="CUser2Id_IN" name="CUser2Id_IN" value =""></input>     
     <input type="hidden" id="CBPartnerId_IN" name="CBPartnerId_IN" value =""></input>
    
  <table height="100%" border="0" cellpadding="0" cellspacing="0" id="main">
    <tr>
      <td valign="top" id="tdleftTabs"></td>
      <td valign="top">
        <table width="100%" border="0" cellspacing="0" cellpadding="0" class="Main_ContentPane_Left">
          <tr><td class="Main_NavBar_bg_left" id="tdleftSeparator"></td></tr>
          <tr><td class="Main_ToolBar_bg_left" valign="top"></td></tr>
          <tr><td class="Main_Client_bg_left"></td></tr>
          <tr><td class="Main_Bottom_bg_left"></td></tr>
        </table>
      </td>
      <td valign="top">
        <div class="Main_ContentPane_NavBar" id="tdtopButtons"></div>
        <div class="Main_ContentPane_ToolBar" id="paramToolBar">
          <table class="Main_ContentPane_ToolBar Main_ToolBar_bg"
                            id="tdToolBar">
                            <tr>
                                <td class="Main_ToolBar_Separator_cell"><img
                                    src="../web/images/blank.gif" class="Main_ToolBar_Separator">
                                </td>
                                <td width="2%"><a href="javascript:void(0);"
                                    onClick="onClickRefresh()" class="Main_ToolBar_Button"
                                    onMouseOver="window.status='Reload Current Page';return true;"
                                    onMouseOut="window.status='';return true;"
                                    onclick="this.hideFocus=true" onblur="this.hideFocus=false"
                                    id="buttonRefresh"><img
                                        class="Main_ToolBar_Button_Icon Main_ToolBar_Button_Icon_Refresh"
                                        src="../web/images/blank.gif"
                                        title="<%=Resource.getProperty("utility.reload", lang)%>"
                                        border="0" id="linkButtonRefresh">
                                </a>
                                </td>
                                 <td class="Main_ToolBar_Space"></td> 
                            </tr>
                        </table>
        </div>
        <div class="Main_ContentPane_Client" style="overflow: auto; display: none;" id="client">

          <TABLE cellpadding="0" cellspacing="0" id="messageBoxID"
                                                    class="MessageBox">
                                                    <TBODY>
                                                        <TR class="MessageBox_TopMargin">
                                                            <TD class="MessageBox_LeftMargin"></TD>
                                                            <TD></TD>
                                                            <TD class="MessageBox_RightMargin"></TD>
                                                        </TR>
                                                        <TR>
                                                            <TD class="MessageBox_LeftMargin"></TD>
                                                            <TD><TABLE cellpadding="0" cellspacing="0"
                                                                    class="MessageBox_Container">
                                                                    <TBODY>
                                                                        <TR>
                                                                            <TD class="MessageBox_LeftTrans"><TABLE
                                                                                    style="width: 100%; height: 100%" cellpadding="0"
                                                                                    cellspacing="0">
                                                                                    <TBODY>
                                                                                        <TR>
                                                                                            <TD class="MessageBox_TopLeft"></TD>
                                                                                        </TR>
                                                                                        <TR>
                                                                                            <TD class="MessageBox_Left"></TD>
                                                                                        </TR>
                                                                                    </TBODY>
                                                                                </TABLE>
                                                                            </TD>
                                                                            <TD class="MessageBox_bg"><TABLE
                                                                                    class="MessageBox_Top">
                                                                                    <TBODY>
                                                                                        <TR>
                                                                                            <TD><SPAN><TABLE cellpadding="0"
                                                                                                        cellspacing="0"
                                                                                                        class="MessageBox_Body_ContentCell">
                                                                                                        <TBODY>
                                                                                                            <TR>
                                                                                                                <TD class="MessageBox_Icon_ContentCell"><DIV
                                                                                                                        class="MessageBox_Icon"></DIV>
                                                                                                                </TD>
                                                                                                                <TD style="vertical-align: top;"
                                                                                                                    id="messageBoxIDContent"><SPAN><DIV
                                                                                                                            class="MessageBox_TextTitle"
                                                                                                                            id="messageBoxIDTitle"></DIV>
                                                                                                                        <DIV class="MessageBox_TextDescription"
                                                                                                                            id="messageBoxIDMessage"></DIV>
                                                                                                                        <DIV class="MessageBox_TextSeparator"></DIV>
                                                                                                                </SPAN>
                                                                                                                </TD>
                                                                                                            </TR>
                                                                                                        </TBODY>
                                                                                                    </TABLE>
                                                                                            </SPAN>
                                                                                                <div id="hideMessage"
                                                                                                    style="float: right; margin-top: -13px; margin-right: 10px;">
                                                                                                    <a style="color: yellow; font-size: 11.5px"
                                                                                                        href="javascript:void(0);"
                                                                                                        onClick="document.getElementById('messageBoxID').style.display = 'none';reSizeGrid();"><%=Resource.getProperty("utility.hide", lang)%></a>
                                                                                                </div>
                                                                                            </TD>
                                                                                        </TR>
                                                                                    </TBODY>
                                                                                </TABLE>
                                                                            </TD>
                                                                            <TD class="MessageBox_RightTrans"><TABLE
                                                                                    style="width: 100%; height: 100%" cellpadding="0"
                                                                                    cellspacing="0">
                                                                                    <TBODY>
                                                                                        <TR>
                                                                                            <TD class="MessageBox_TopRight"></TD>
                                                                                        </TR>
                                                                                        <TR>
                                                                                            <TD class="MessageBox_Right"></TD>
                                                                                        </TR>
                                                                                    </TBODY>
                                                                                </TABLE>
                                                                            </TD>
                                                                        </TR>
                                                                        <TR>
                                                                            <TD rowspan="2" class="MessageBox_BottomLeft"></TD>
                                                                            <TD class="MessageBox_BottomTrans MessageBox_bg"></TD>
                                                                            <TD rowspan="2" class="MessageBox_BottomRight"></TD>
                                                                        </TR>
                                                                        <TR>
                                                                            <TD class="MessageBox_Bottom"></TD>
                                                                        </TR>
                                                                    </TBODY>
                                                                </TABLE>
                                                            </TD>
                                                            <TD class="MessageBox_RightMargin"></TD>
                                                        </TR>
                                                        <TR class="MessageBox_BottomMargin">
                                                            <TD class="MessageBox_LeftMargin"></TD>
                                                            <TD></TD>
                                                            <TD class="MessageBox_RightMargin"></TD>
                                                        </TR>
                                                    </TBODY>
                                                </TABLE>

          <!-- USER CONTROLS -->
          <table class="Main_Client_TableEdition">
            <tr>
              <td class="TableEdition_OneCell_width"></td>
              <td class="TableEdition_OneCell_width"></td>
              <td class="TableEdition_OneCell_width"></td>
              <td class="TableEdition_OneCell_width"></td>
              <td class="TableEdition_OneCell_width"></td>
              <td class="TableEdition_OneCell_width"></td>
            </tr>
            <tr>
              <td colspan="6">
                <table class="FieldGroup" cellspacing="0" cellpadding="0" border="0">
                <tbody>
                  <tr class="FieldGroup_TopMargin"/>
                  <tr>
                    <td class="FieldGroupTitle_Left"><img class="FieldGroupTitle_Left_bg" border="0" src="../web/images/blank.gif"/></td>
                    <td class="FieldGroupTitle"><%= Resource.getProperty("finance.PrimaryFilters",lang)%>&nbsp;&nbsp;&nbsp;&nbsp;</td>
                    <td class="FieldGroupTitle_Right"><img class="FieldGroupTitle_Right_bg" border="0" src="../web/images/blank.gif"/></td>
                    <td class="FieldGroupContent"/>
                  </tr>
                  <tr class="FieldGroup_BottomMargin"/>
                </tbody>
                </table>
              </td>
            </tr>
            <tr>
              <td class="TitleCell"><span class="LabelText"><%= Resource.getProperty("finance.fromperiod",lang)%></span></td>
               <td class="Combo_ContentCell" colspan="2"> <select name="inpcFromPeriodId" id="inpcFromPeriodId" class="ComboKey Combo_TwoCells_width" required="true">
               <%   if(PeriodList!=null && PeriodList.size()>0) { %>
               <% for(GLJournalApprovalVO vo : PeriodList) {%>
               <% if(request.getAttribute("SelectedPeriod")!=null){ %>
                <option value="<%= vo.getPeriod() %>" <%if(request.getAttribute("SelectedPeriod").equals(vo.getPeriod())){ %> selected <%} %>><span><%= vo.getPeriodName()%></span>
                </option><% }
               else{%>
               <option value="<%= vo.getPeriod() %>" ><span><%= vo.getPeriodName()%></span></option>
                <%}}}%>
               <!--    <div id="reportC_FROMPERIOD_ID"></div> -->
               
                </select>
              </td>  
              <!-- <td class="TextBox_btn_ContentCell"> 
                <table border="0" cellspacing="0" cellpadding="0" summary=""  style="padding-top: 0px;">
                  <tr>  
                    <td class="TextBox_ContentCell">
                     <table style="border: 0px none; border-collapse: collapse;">
                        <tr><td style="padding-top: 0px;">
                      <input dojoType="openbravo:DateTextbox" lowerThan="paramDateTo" displayFormat="xx" saveFormat="yy"  class="TextBox_btn_OneCell_width required" required="true" type="text" name="inpDateFrom" id="paramDateFrom" size="10" maxlength="10" value="" onkeyup="autoCompleteDate(this.textbox, this.displayFormat);return true;"></input><script>djConfig.searchIds.push("paramDateFrom");</script>
                    </td></tr>
                    </table>
                     <td class="FieldButton_ContentCell">
                      <a class="FieldButtonLink" href="#" onfocus="setWindowElementFocus(this); window.status='Calendar'; return true;" onblur="window.status=''; return true;" onkeypress="this.className='FieldButtonLink_active'; return true;" onkeyup="this.className='FieldButtonLink_focus'; return true;"  onclick="showCalendar('frmMain.inpDateFrom', document.frmMain.inpDateFrom.value, false);return false;">
                      <table class="FieldButton" onmouseout="this.className='FieldButton';window.status='';return true;" onmouseover="this.className='FieldButton_hover';window.status='Show calendar';return true;" onmousedown="this.className='FieldButton_active';return true;" onmouseup="this.className='FieldButton';return true;">
                        <tr>
                          <td class="FieldButton_bg">
                            <img alt="Calendar" class="FieldButton_Icon FieldButton_Icon_Calendar" title="Calendar" src="../web/images/blank.gif" border="0"></img>
                          </td>
                        </tr>
                      </table>
                      </a>
                    </td>
                  </tr>
                </table>
             </td> -->
              <td class="TitleCell"><span class="LabelText"><%= Resource.getProperty("finance.toperiod",lang)%></span></td>
               <td class="Combo_ContentCell" colspan="2"> <select name="inpcToPeriodId" id="inpcToPeriodId" class="ComboKey Combo_TwoCells_width" required="true">
               <%   if(PeriodList!=null && PeriodList.size()>0) { %>
               <% for(GLJournalApprovalVO vo : PeriodList) {%>
               <% if(request.getAttribute("SelectedToPeriod")!=null){ %>
                <option value="<%= vo.getPeriod() %>" <%if(request.getAttribute("SelectedToPeriod").equals(vo.getPeriod())){ %> selected <%} %>><span><%= vo.getPeriodName()%></span>
                </option><% }
               else{%>
               <option value="<%= vo.getPeriod() %>" ><span><%= vo.getPeriodName()%></span>
                <%}}}%>
                 <!--  <div id="reportC_TOPERIOD_ID"></div> -->
                </select>
              </td>  
              <!-- <td class="TextBox_btn_ContentCell">
                <table border="0" cellspacing="0" cellpadding="0" summary=""  style="padding-top: 0px;">
                  <tr>
                    <td class="TextBox_ContentCell">
                    <table style="border: 0px none; border-collapse: collapse;">
                        <tr><td style="padding-top: 0px;">
                      <input dojoType="openbravo:DateTextbox" greaterThan="paramDateFrom" displayFormat="xx" saveFormat="yy"  class="TextBox_btn_OneCell_width required" required="true" type="text" name="inpDateTo" id="paramDateTo" size="10" maxlength="10" value="" onkeyup="autoCompleteDate(this.textbox, this.displayFormat);return true;"></input><script>djConfig.searchIds.push("paramDateTo");</script>
                    </td></tr>
                    </table>
                    <td class="FieldButton_ContentCell">
                      <a class="FieldButtonLink" href="#" onfocus="setWindowElementFocus(this); window.status='Calendar'; return true;" onblur="window.status=''; return true;" onkeypress="this.className='FieldButtonLink_active'; return true;" onkeyup="this.className='FieldButtonLink_focus'; return true;" onclick="showCalendar('frmMain.inpDateTo', document.frmMain.inpDateTo.value, false);return false;" >
                      <table class="FieldButton" onmouseout="this.className='FieldButton';window.status='';return true;" onmouseover="this.className='FieldButton_hover';window.status='Show calendar';return true;" onmousedown="this.className='FieldButton_active';return true;" onmouseup="this.className='FieldButton';return true;">
                        <tr>
                          <td class="FieldButton_bg">
                            <img alt="Calendar" class="FieldButton_Icon FieldButton_Icon_Calendar" title="Calendar" src="../web/images/blank.gif" border="0"></img>
                          </td>
                        </tr>
                      </table>
                      </a>
                    </td>
                  </tr>
                </table>
              </td> -->
            </tr>
            <tr>
              <td class="TitleCell"><span class="LabelText"><%= Resource.getProperty("finance.organization",lang)%></span></td>
              <td class="Combo_ContentCell">
                <select  name="inpOrg" id="inpOrg" class="ComboKey Combo_TwoCells_width" required="true">
                 <%   if(OrgList!=null && OrgList.size()>0) { %>
               <% for(GLJournalApprovalVO vo : OrgList) {%>
               <% if(request.getAttribute("SelectedOrg")!=null){ %>
                <option value="<%= vo.getOrgId() %>" <%if(request.getAttribute("SelectedOrg").equals(vo.getOrgId())){ %> selected <%} %>><span><%= Utility.escapeHTML(vo.getOrgName())%></span>
                </option><% }
               else{%>
               <option value="<%= vo.getOrgId() %>" ><span><%=Utility.escapeHTML(vo.getOrgName())%></span></option>
                <%}}}%>
                <!--  <div id="reportAD_ORGID"></div> -->
                </select></td>
              <td class="TitleCell"/></td>
              <td class="TitleCell"><span class="LabelText"><%= Resource.getProperty("finance.generalledger",lang)%></span></td>
              <td class="Combo_ContentCell" colspan="2"> <select name="inpcAcctSchemaId" id="inpcAcctSchemaId" class="ComboKey Combo_TwoCells_width" required="true">
                   <%   if(SchemaList!=null && SchemaList.size()>0) { %>
               <% for(GLJournalApprovalVO vo : SchemaList) {%>
               <% if(request.getAttribute("SelectedSchema")!=null){ %>
                <option value="<%= vo.getAcctschemaId() %>" <%if(request.getAttribute("SelectedSchema").equals(vo.getAcctschemaId())){ %> selected <%} %>><span><%= Utility.escapeHTML(vo.getAcctschemaName())%></span>
                </option><% }
               else{%>
               <option value="<%= vo.getAcctschemaId() %>" ><span><%= Utility.escapeHTML(vo.getAcctschemaName())%></span></option>
                <%}}}%>
                  <!-- <div id="reportC_ACCTSCHEMA_ID"></div> -->
                </select>
              </td>            
            </tr>
            <tr>
              <td colspan="6">
                <table class="FieldGroup" cellspacing="0" cellpadding="0" border="0">
                <tbody>
                  <tr class="FieldGroup_TopMargin"/>
                  <tr>
                    <td class="FieldGroupTitle_Left" onclick="displayAdvancedFilters();return false;"><img class="FieldGroupTitle_Left_bg" border="0" src="../web/images/blank.gif"/></td>
                    <td class="FieldGroupTitle" onclick="displayAdvancedFilters();return false;" ><%= Resource.getProperty("finance.advancedfilters",lang)%></td>
                    <td class="FieldGroupTitle_Right" onclick="displayAdvancedFilters();return false;"><img class="FieldGroupTitle_Right_bg" border="0" src="../web/images/blank.gif"/></td>
                    <td class="FieldGroupContent"/>
                  </tr>
                  <tr class="FieldGroup_BottomMargin"/>
                </tbody>
                </table>
              </td>
            </tr>
             <tr id=advancedFiltersrow1> 
              <!-- <td width="25%" class="TitleCell"><span class="LabelText">Opening Entry Amount to Initial Balance</span></td>
              <td width="20%" class="Radio_Check_ContentCell"><input name="inpNotInitialBalance" type="checkbox" id="fieldNotInitialBalance" value="Y" checked></input></td>
              <td width="35%" class="ContentCell" colspan="2"></td>
              <td class="TitleCell"></td>
              <td class="TitleCell"><span class="LabelText">Account Level</span></td>
              <td class="Combo_ContentCell"> 
                <select  name="inpLevel" id="inpLevel" class="ComboKey Combo_OneCell_width" required="true" onchange="displayLogic(); return true;">
                  <div id="reportLevel"></div>
                </select></td>
                <td class="TitleCell"/></td>
              <td class="TitleCell"><span class="LabelText">Initial Page Number</span></td>
              <td class="TextBox_ContentCell"> <input dojoType="openbravo:Textbox"  class="dojoValidateValid TextBox_OneCell_width" type="text" name="inpPageNo" id="paramPageNo" size="10" maxlength="10" value=""></input><script>djConfig.searchIds.push("paramPageNo");</script>
              </td>
              <td class="TitleCell"/></td> -->
            </tr>
           
            <tr id="AcctFromTo"> 
              <td class="TitleCell"><span class="LabelText"><%= Resource.getProperty("finance.g/lreport.fromaccount",lang)%></span></td>
              <td class="TextBox_btn_ContentCell" colspan="2">
                <table border="0" cellspacing="0" cellpadding="0" summary=""  style="padding-top: 0px;">
                  <tr>
                    <td class="TextBox_ContentCell">
                      <input type="hidden" name="inpcElementValueIdFrom" id="paramElementvalueIdFrom" value="<% if(request.getAttribute("inpcElementValueIdFrom")!=null){ %><%= request.getAttribute("inpcElementValueIdFrom")%><%}%>"></input>
                      <input class="dojoValidateValid TextBox_btn_TwoCells_width" type="text"  name="inpElementValueIdFrom_DES" id="inpElementValueIdFrom_DES" maxlength="20" value="<% if(request.getAttribute("inpElementValueIdFrom_DES")!=null){ %><%= Utility.escapeHTML(request.getAttribute("inpElementValueIdFrom_DES"))%><%}%>"></input>
                    </td>
                    <td class="FieldButton_ContentCell">
                      <a class="FieldButtonLink" href="#" onfocus="setWindowElementFocus(this); window.status='Account'; return true;" onblur="window.status=''; return true;" onkeypress="this.className='FieldButtonLink_active'; return true;" onkeyup="this.className='FieldButtonLink_focus'; return true;" onclick="openSearch(null, null, '../info/EfinAccountElementValue.html', 'SELECTOR_ACCOUNTELEMENTVALUE', false, 'frmMain', 'inpcElementValueIdFrom', 'inpElementValueIdFrom_DES', document.frmMain.inpElementValueIdFrom_DES.value, 'inpcAcctSchemaId', document.frmMain.inpcAcctSchemaId.value);return false;">
                      <table class="FieldButton"  onmouseout="this.className='FieldButton';window.status='';return true;" onmouseover="this.className='FieldButton_hover';window.status='Search';return true;" onmousedown="this.className='FieldButton_active';return true;" onmouseup="this.className='FieldButton';return true;">
                        <tr>
                          <td class="FieldButton_bg">
                            <img alt="Account" class="FieldButton_Icon FieldButton_Icon_Account" title="Account" src="../web/images/blank.gif" border="0"></img>
                          </td>
                        </tr>
                      </table>
                      </a>
                    </td>
                  </tr>
                </table>
              </td>
              <td class="TitleCell"><span class="LabelText"><%= Resource.getProperty("finance.toaccount",lang) %></span></td>
              <td class="TextBox_btn_ContentCell" colspan="2">
                <table border="0" cellspacing="0" cellpadding="0" summary=""  style="padding-top: 0px;">
                  <tr>
                    <td class="TextBox_ContentCell">
                      <input type="hidden" name="inpcElementValueIdTo" id="paramElementvalueIdTo" value="<% if(request.getAttribute("inpcElementValueIdTo")!=null){ %><%= request.getAttribute("inpcElementValueIdTo")%><%}%>"></input>
                      <input class="dojoValidateValid TextBox_btn_TwoCells_width" type="text"  name="inpElementValueIdTo_DES" id="inpElementValueIdTo_DES" maxlength="20" value="<% if(request.getAttribute("inpElementValueIdTo_DES")!=null){ %><%= Utility.escapeHTML(request.getAttribute("inpElementValueIdTo_DES"))%><%}%>"></input>
                    </td>
                    <td class="FieldButton_ContentCell">
                      <a class="FieldButtonLink" href="#" onfocus="setWindowElementFocus(this); window.status='Account'; return true;" onblur="window.status=''; return true;" onkeypress="this.className='FieldButtonLink_active'; return true;" onkeyup="this.className='FieldButtonLink_focus'; return true;" onclick="openSearch(null, null, '../info/EfinAccountElementValue.html', 'SELECTOR_ACCOUNTELEMENTVALUE', false, 'frmMain', 'inpcElementValueIdTo', 'inpElementValueIdTo_DES', document.frmMain.inpElementValueIdTo_DES.value, 'inpcAcctSchemaId', document.frmMain.inpcAcctSchemaId.value);return false;">
                      <table class="FieldButton"  onmouseout="this.className='FieldButton';window.status='';return true;" onmouseover="this.className='FieldButton_hover';window.status='Search';return true;" onmousedown="this.className='FieldButton_active';return true;" onmouseup="this.className='FieldButton';return true;">
                        <tr>
                          <td class="FieldButton_bg">
                            <img alt="Account" class="FieldButton_Icon FieldButton_Icon_Account" title="Account" src="../web/images/blank.gif" border="0"></img>
                          </td>
                        </tr>
                      </table>
                      </a>
                    </td>
                  </tr>
                </table>
              </td>
            </tr>
            <tr id="Filter1">
              <td colspan="6">
                <table class="FieldGroup" cellspacing="0" cellpadding="0" border="0">
                <tbody>
                  <tr class="FieldGroup_TopMargin"/>
                  <tr>
                    <td class="FieldGroupTitle_Left" onclick="showFilters();return false;"><img class="FieldGroupTitle_Left_bg" border="0" src="../web/images/blank.gif"/></td>
                    <td class="FieldGroupTitle"  onclick="showFilters();return false;"><%= Resource.getProperty("finance.dimensions",lang) %>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td>
                    <td class="FieldGroupTitle_Right" onclick="showFilters();return false;"><img class="FieldGroupTitle_Right_bg" border="0" src="../web/images/blank.gif"/></td>
                    <td class="FieldGroupContent"/>
                  </tr>
                  <tr class="FieldGroup_BottomMargin"/>
                </tbody>
                </table>
              </td>
            </tr>         
           <tr id="Filter2">
              <td class="TitleCell"><span class="LabelText">Entity</span></td>
                <td class="List_ContentCell" colspan="2">

                  <table border="0" cellspacing="0" cellpadding="0">
                    <tr>
                      <td>
                        <select class="List_width List_height" name="inpcBPartnerId_IN" multiple="" id="inpcBPartnerId_IN">
                       <%   if(bpList!=null && bpList.size()>0) { %>
               <% for(GLJournalApprovalVO vo : bpList) {%>
               <option value="<%= vo.getId() %>" ><span><%= Utility.escapeHTML(vo.getName())%></span></option>
                <%}}%>
                    </select>
                        <!-- <select class="List_width List_height" name="inpcBPartnerId_IN" multiple="" id="reportCBPartnerId_IN"></select> -->
                      </td>
                      <td class="List_Button_ContentCell">
                        <table border="0" cellspacing="0" cellpadding="0">
                          <tr>
                            <td>
                              <a class="List_Button_TopLink" href="#"
                                onclick="openMultiSearch(null, null, '../info/BusinessPartnerMultiple.html', 'SELECTOR_BUSINESS', false, 'frmMain', 'inpcBPartnerId_IN');return false;"
                                onfocus="setWindowElementFocus(this); window.status='Add'; return true;"
                                onblur="window.status=''; return true;"
                                onkeypress="this.className='List_Button_TopLink_active'; return true;"
                                onkeyup="this.className='List_Button_TopLink_focus'; return true;">
                              <table class="List_Button_Top"
                                onmousedown="this.className='List_Button_Top_active'; return true;"
                                onmouseup="this.className='List_Button_Top'; return true;"
                                onmouseover="this.className='List_Button_Top_hover'; window.status='Add'; return true;"
                                onmouseout="this.className='List_Button_Top'; window.status=''; return true;">
                                <tr>
                                  <td class="List_Button_Top_bg">
                                    <img class="List_Button_Icon List_Button_Icon_Add" src="../web/images/blank.gif" alt="Add" title="Add"/>
                                  </td>
                                </tr>
                              </table>
                              </a>
                            </td>
                          </tr>
                          <tr>
                            <td class="List_Button_Separator"></td>
                          </tr>
                          <tr>
                            <td>
                              <a class="List_Button_MiddleLink" href="#"
                                onclick="clearSelectedElements(document.frmMain.inpcBPartnerId_IN);return false;"
                                onfocus="setWindowElementFocus(this); window.status='Delete selected elements'; return true;"
                                onblur="window.status=''; return true;"
                                onkeypress="this.className='List_Button_MiddleLink_active'; return true;"
                                onkeyup="this.className='List_Button_MiddleLink_focus'; return true;">
                                <table class="List_Button_Middle"
                                  onmousedown="this.className='List_Button_Middle_active'; return true;"
                                  onmouseup="this.className='List_Button_Middle'; return true;"
                                  onmouseover="this.className='List_Button_Middle_hover'; window.status='Delete selected elements'; return true;"
                                  onmouseout="this.className='List_Button_Middle'; window.status=''; return true;">
                                  <tr>
                                    <td class="List_Button_Middle_bg">
                                      <img class="List_Button_Icon List_Button_Icon_Delete" src="../web/images/blank.gif" alt="Delete selected elements" title="Delete selected elements"/>
                                    </td>
                                  </tr>
                                </table>
                              </a>
                            </td>
                          </tr>
                          <tr>
                            <td class="List_Button_Separator"></td>
                          </tr>
                          <tr>
                            <td>
                              <a class="List_Button_BottomLink" href="#"
                                onclick="clearList(document.frmMain.inpcBPartnerId_IN);return false;"
                                onfocus="setWindowElementFocus(this); window.status='Delete all elements'; return true;"
                                onblur="window.status=''; return true;"
                                onkeypress="this.className='List_Button_BottomLink_active'; return true;"
                                onkeyup="this.className='List_Button_BottomLink_focus'; return true;">
                                <table class="List_Button_Bottom"
                                  onmousedown="this.className='List_Button_Bottom_active'; return true;"
                                  onmouseup="this.className='List_Button_Bottom'; return true;"
                                  onmouseover="this.className='List_Button_Bottom_hover'; window.status='Delete all elements'; return true;"
                                  onmouseout="this.className='List_Button_Bottom'; window.status=''; return true;">
                                  <tr>
                                    <td class="List_Button_Bottom_bg">
                                      <img class="List_Button_Icon List_Button_Icon_DeleteAll" src="../web/images/blank.gif" alt="Delete all elements" title="Delete all elements"/>
                                    </td>
                                  </tr>
                                </table>
                              </a>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </td>
                 <!-- Project multiple selector -->
            
            <td class="TitleCell"><span class="LabelText"><%= Resource.getProperty("finance.department",lang) %></span></td>
              <td class="List_ContentCell" colspan="2">

                <table border="0" cellspacing="0" cellpadding="0">
                  <tr>
                    <td>
                      <select class="List_width List_height" name="inpcSalesregionId_IN" multiple="" id="inpcSalesregionId_IN">
                       <%   if(deptList!=null && deptList.size()>0) { %>
               <% for(GLJournalApprovalVO vo : deptList) {%>
               <option value="<%= vo.getId() %>" ><span><%= Utility.escapeHTML(vo.getName())%></span></option>
                <%}}%>
                    </select>
                    </td>
                    <td class="List_Button_ContentCell">
                      <table border="0" cellspacing="0" cellpadding="0">
                        <tr>
                          <td>
                            <a class="List_Button_TopLink" href="#"
                              onclick="openMultiSearch(null, null, '../info/DeptMultiple.html', 'SELECTOR_DEPARTMENT', false, 'frmMain', 'inpcSalesregionId_IN');return false;"
                              onfocus="setWindowElementFocus(this); window.status='Add'; return true;"
                              onblur="window.status=''; return true;"
                              onkeypress="this.className='List_Button_TopLink_active'; return true;"
                              onkeyup="this.className='List_Button_TopLink_focus'; return true;">
                              <table class="List_Button_Top"
                                onmousedown="this.className='List_Button_Top_active'; return true;"
                                onmouseup="this.className='List_Button_Top'; return true;"
                                onmouseover="this.className='List_Button_Top_hover'; window.status='Add'; return true;"
                                onmouseout="this.className='List_Button_Top'; window.status=''; return true;">
                                <tr>
                                  <td class="List_Button_Top_bg">
                                    <img class="List_Button_Icon List_Button_Icon_Add" src="../web/images/blank.gif" alt="Add" title="Add"/>
                                  </td>
                                </tr>
                              </table>
                            </a>
                          </td>
                        </tr>
                        <tr>
                          <td class="List_Button_Separator">
                          </td>
                        </tr>
                        <tr>
                          <td>
                            <a class="List_Button_MiddleLink" href="#"
                              onclick="clearSelectedElements(document.frmMain.inpcSalesregionId_IN);return false;"
                              onfocus="setWindowElementFocus(this); window.status='Delete selected elements'; return true;"
                              onblur="window.status=''; return true;"
                              onkeypress="this.className='List_Button_MiddleLink_active'; return true;"
                              onkeyup="this.className='List_Button_MiddleLink_focus'; return true;">
                              <table class="List_Button_Middle"
                                onmousedown="this.className='List_Button_Middle_active'; return true;"
                                onmouseup="this.className='List_Button_Middle'; return true;"
                                onmouseover="this.className='List_Button_Middle_hover'; window.status='Delete selected elements'; return true;"
                                onmouseout="this.className='List_Button_Middle'; window.status=''; return true;">
                                <tr>
                                  <td class="List_Button_Middle_bg">
                                    <img class="List_Button_Icon List_Button_Icon_Delete" src="../web/images/blank.gif" alt="Delete selected elements" title="Delete selected elements"/>
                                  </td>
                                </tr>
                              </table>
                            </a>
                          </td>
                        </tr>
                        <tr>
                          <td class="List_Button_Separator">
                          </td>
                        </tr>
                        <tr>
                          <td>
                            <a class="List_Button_BottomLink" href="#"
                              onclick="clearList(document.frmMain.inpcSalesregionId_IN);return false;"
                              onfocus="setWindowElementFocus(this); window.status='Delete all elements'; return true;"
                              onblur="window.status=''; return true;"
                              onkeypress="this.className='List_Button_BottomLink_active'; return true;"
                              onkeyup="this.className='List_Button_BottomLink_focus'; return true;">
                              <table class="List_Button_Bottom"
                                onmousedown="this.className='List_Button_Bottom_active'; return true;"
                                onmouseup="this.className='List_Button_Bottom'; return true;"
                                onmouseover="this.className='List_Button_Bottom_hover'; window.status='Delete all elements'; return true;"
                                onmouseout="this.className='List_Button_Bottom'; window.status=''; return true;">
                                <tr>
                                  <td class="List_Button_Bottom_bg">
                                    <img class="List_Button_Icon List_Button_Icon_DeleteAll" src="../web/images/blank.gif" alt="Delete all elements" title="Delete all elements"/>
                                  </td>
                                </tr>
                              </table>
                            </a>
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>
                </table>
              </td> 
               </tr>
                 <!-- Product multiple selector
                <td class="TitleCell"><span class="LabelText">Product</span></td>
                <td class="List_ContentCell" colspan="2">
                  <table border="0" cellspacing="0" cellpadding="0">
                  <tr>
                    <td>
                      <select class="List_width List_height" name="inpmProductId_IN" multiple="" id="reportMProductId_IN"></select>
                    </td>
                    <td class="List_Button_ContentCell">
                      <table border="0" cellspacing="0" cellpadding="0">
                        <tr>
                          <td>
                            <a class="List_Button_TopLink" href="#"
                            onclick="openMultiSearch(null, null, '../info/ProductMultiple.html', 'SELECTOR_PRODUCT', false, 'frmMain', 'inpmProductId_IN');return false;"
                            onfocus="setWindowElementFocus(this); window.status='Add'; return true;"
                            onblur="window.status=''; return true;"
                            onkeypress="this.className='List_Button_TopLink_active'; return true;"
                            onkeyup="this.className='List_Button_TopLink_focus'; return true;">
                            <table class="List_Button_Top"
                              onmousedown="this.className='List_Button_Top_active'; return true;"
                              onmouseup="this.className='List_Button_Top'; return true;"
                              onmouseover="this.className='List_Button_Top_hover'; window.status='Add'; return true;"
                              onmouseout="this.className='List_Button_Top'; window.status=''; return true;">
                              <tr>
                                <td class="List_Button_Top_bg">
                                  <img class="List_Button_Icon List_Button_Icon_Add" src="../web/images/blank.gif" alt="Add" title="Add"/>
                                </td>
                              </tr>
                            </table>
                            </a>
                          </td>
                        </tr>
                        <tr>
                          <td class="List_Button_Separator">
                          </td>
                        </tr>
                        <tr>
                          <td>
                            <a class="List_Button_MiddleLink" href="#"
                              onclick="clearSelectedElements(document.frmMain.inpmProductId_IN);return false;"
                              onfocus="setWindowElementFocus(this); window.status='Delete selected elements'; return true;"
                              onblur="window.status=''; return true;"
                              onkeypress="this.className='List_Button_MiddleLink_active'; return true;"
                              onkeyup="this.className='List_Button_MiddleLink_focus'; return true;">
                              <table class="List_Button_Middle"
                                onmousedown="this.className='List_Button_Middle_active'; return true;"
                                onmouseup="this.className='List_Button_Middle'; return true;"
                                onmouseover="this.className='List_Button_Middle_hover'; window.status='Delete selected elements'; return true;"
                                onmouseout="this.className='List_Button_Middle'; window.status=''; return true;">
                                <tr>
                                  <td class="List_Button_Middle_bg">
                                    <img class="List_Button_Icon List_Button_Icon_Delete" src="../web/images/blank.gif" alt="Delete selected elements" title="Delete selected elements"/>
                                  </td>
                                </tr>
                              </table>
                            </a>
                          </td>
                        </tr>
                        <tr>
                          <td class="List_Button_Separator">
                          </td>
                        </tr>
                        <tr>
                          <td>
                            <a class="List_Button_BottomLink" href="#"
                              onclick="clearList(document.frmMain.inpmProductId_IN);return false;"
                              onfocus="setWindowElementFocus(this); window.status='Delete all elements'; return true;"
                              onblur="window.status=''; return true;"
                              onkeypress="this.className='List_Button_BottomLink_active'; return true;"
                              onkeyup="this.className='List_Button_BottomLink_focus'; return true;">
                              <table class="List_Button_Bottom"
                                onmousedown="this.className='List_Button_Bottom_active'; return true;"
                                onmouseup="this.className='List_Button_Bottom'; return true;"
                                onmouseover="this.className='List_Button_Bottom_hover'; window.status='Delete all elements'; return true;"
                                onmouseout="this.className='List_Button_Bottom'; window.status=''; return true;">
                                <tr>
                                  <td class="List_Button_Bottom_bg">
                                    <img class="List_Button_Icon List_Button_Icon_DeleteAll" src="../web/images/blank.gif" alt="Delete all elements" title="Delete all elements"/>
                                  </td>
                                </tr>
                              </table>
                            </a>
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>
                </table>
              </td>
            </tr> -->    
              
               <!-- <td class="TitleCell"><span class="LabelText">Budget Type</span></td>
              <td class="List_ContentCell" colspan="2">

                <table border="0" cellspacing="0" cellpadding="0">
                  <tr>
                    <td>
                      <select class="List_width List_height" name="inpcCampaignId_IN" multiple="" id="reportCCampaignId_IN"></select>
                    </td>
                    <td class="List_Button_ContentCell">
                      <table border="0" cellspacing="0" cellpadding="0">
                        <tr>
                          <td>
                            <a class="List_Button_TopLink" href="#"
                              onclick="openMultiSearch(null, null, '../info/BudgetTypeMultiple.html', 'SELECTOR_PROJECT', false, 'frmMain', 'inpcCampaignId_IN');return false;"
                              onfocus="setWindowElementFocus(this); window.status='Add'; return true;"
                              onblur="window.status=''; return true;"
                              onkeypress="this.className='List_Button_TopLink_active'; return true;"
                              onkeyup="this.className='List_Button_TopLink_focus'; return true;">
                              <table class="List_Button_Top"
                                onmousedown="this.className='List_Button_Top_active'; return true;"
                                onmouseup="this.className='List_Button_Top'; return true;"
                                onmouseover="this.className='List_Button_Top_hover'; window.status='Add'; return true;"
                                onmouseout="this.className='List_Button_Top'; window.status=''; return true;">
                                <tr>
                                  <td class="List_Button_Top_bg">
                                    <img class="List_Button_Icon List_Button_Icon_Add" src="../web/images/blank.gif" alt="Add" title="Add"/>
                                  </td>
                                </tr>
                              </table>
                            </a>
                          </td>
                        </tr>
                        <tr>
                          <td class="List_Button_Separator">
                          </td>
                        </tr>
                        <tr>
                          <td>
                            <a class="List_Button_MiddleLink" href="#"
                              onclick="clearSelectedElements(document.frmMain.inpcCampaignId_IN);return false;"
                              onfocus="setWindowElementFocus(this); window.status='Delete selected elements'; return true;"
                              onblur="window.status=''; return true;"
                              onkeypress="this.className='List_Button_MiddleLink_active'; return true;"
                              onkeyup="this.className='List_Button_MiddleLink_focus'; return true;">
                              <table class="List_Button_Middle"
                                onmousedown="this.className='List_Button_Middle_active'; return true;"
                                onmouseup="this.className='List_Button_Middle'; return true;"
                                onmouseover="this.className='List_Button_Middle_hover'; window.status='Delete selected elements'; return true;"
                                onmouseout="this.className='List_Button_Middle'; window.status=''; return true;">
                                <tr>
                                  <td class="List_Button_Middle_bg">
                                    <img class="List_Button_Icon List_Button_Icon_Delete" src="../web/images/blank.gif" alt="Delete selected elements" title="Delete selected elements"/>
                                  </td>
                                </tr>
                              </table>
                            </a>
                          </td>
                        </tr>
                        <tr>
                          <td class="List_Button_Separator">
                          </td>
                        </tr>
                        <tr>
                          <td>
                            <a class="List_Button_BottomLink" href="#"
                              onclick="clearList(document.frmMain.inpcCampaignId_IN);return false;"
                              onfocus="setWindowElementFocus(this); window.status='Delete all elements'; return true;"
                              onblur="window.status=''; return true;"
                              onkeypress="this.className='List_Button_BottomLink_active'; return true;"
                              onkeyup="this.className='List_Button_BottomLink_focus'; return true;">
                              <table class="List_Button_Bottom"
                                onmousedown="this.className='List_Button_Bottom_active'; return true;"
                                onmouseup="this.className='List_Button_Bottom'; return true;"
                                onmouseover="this.className='List_Button_Bottom_hover'; window.status='Delete all elements'; return true;"
                                onmouseout="this.className='List_Button_Bottom'; window.status=''; return true;">
                                <tr>
                                  <td class="List_Button_Bottom_bg">
                                    <img class="List_Button_Icon List_Button_Icon_DeleteAll" src="../web/images/blank.gif" alt="Delete all elements" title="Delete all elements"/>
                                  </td>
                                </tr>
                              </table>
                            </a>
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>
                </table>
              </td>  -->          
            
            <tr id="Filter3">
            <td class="TitleCell"><span class="LabelText"><%= Resource.getProperty("finance.project",lang) %></span></td>
              <td class="List_ContentCell" colspan="2">

                <table border="0" cellspacing="0" cellpadding="0">
                  <tr>
                    <td>
                      <select class="List_width List_height" name="inpcProjectId_IN" multiple="" id="inpcProjectId_IN">
                       <%   if(projList!=null && projList.size()>0) { %>
               <% for(GLJournalApprovalVO vo : projList) {%>
               <option value="<%= vo.getId() %>" ><span><%= Utility.escapeHTML(vo.getName())%></span></option>
                <%}}%>
                    </select>
                    </td>
                    <td class="List_Button_ContentCell">
                      <table border="0" cellspacing="0" cellpadding="0">
                        <tr>
                          <td>
                            <a class="List_Button_TopLink" href="#"
                              onclick="openMultiSearch(null, null, '../info/ProjectMultiples.html', 'SELECTOR_PROJECT', false, 'frmMain', 'inpcProjectId_IN');return false;"
                              onfocus="setWindowElementFocus(this); window.status='Add'; return true;"
                              onblur="window.status=''; return true;"
                              onkeypress="this.className='List_Button_TopLink_active'; return true;"
                              onkeyup="this.className='List_Button_TopLink_focus'; return true;">
                              <table class="List_Button_Top"
                                onmousedown="this.className='List_Button_Top_active'; return true;"
                                onmouseup="this.className='List_Button_Top'; return true;"
                                onmouseover="this.className='List_Button_Top_hover'; window.status='Add'; return true;"
                                onmouseout="this.className='List_Button_Top'; window.status=''; return true;">
                                <tr>
                                  <td class="List_Button_Top_bg">
                                    <img class="List_Button_Icon List_Button_Icon_Add" src="../web/images/blank.gif" alt="Add" title="Add"/>
                                  </td>
                                </tr>
                              </table>
                            </a>
                          </td>
                        </tr>
                        <tr>
                          <td class="List_Button_Separator">
                          </td>
                        </tr>
                        <tr>
                          <td>
                            <a class="List_Button_MiddleLink" href="#"
                              onclick="clearSelectedElements(document.frmMain.inpcProjectId_IN);return false;"
                              onfocus="setWindowElementFocus(this); window.status='Delete selected elements'; return true;"
                              onblur="window.status=''; return true;"
                              onkeypress="this.className='List_Button_MiddleLink_active'; return true;"
                              onkeyup="this.className='List_Button_MiddleLink_focus'; return true;">
                              <table class="List_Button_Middle"
                                onmousedown="this.className='List_Button_Middle_active'; return true;"
                                onmouseup="this.className='List_Button_Middle'; return true;"
                                onmouseover="this.className='List_Button_Middle_hover'; window.status='Delete selected elements'; return true;"
                                onmouseout="this.className='List_Button_Middle'; window.status=''; return true;">
                                <tr>
                                  <td class="List_Button_Middle_bg">
                                    <img class="List_Button_Icon List_Button_Icon_Delete" src="../web/images/blank.gif" alt="Delete selected elements" title="Delete selected elements"/>
                                  </td>
                                </tr>
                              </table>
                            </a>
                          </td>
                        </tr>
                        <tr>
                          <td class="List_Button_Separator">
                          </td>
                        </tr>
                        <tr>
                          <td>
                            <a class="List_Button_BottomLink" href="#"
                              onclick="clearList(document.frmMain.inpcProjectId_IN);return false;"
                              onfocus="setWindowElementFocus(this); window.status='Delete all elements'; return true;"
                              onblur="window.status=''; return true;"
                              onkeypress="this.className='List_Button_BottomLink_active'; return true;"
                              onkeyup="this.className='List_Button_BottomLink_focus'; return true;">
                              <table class="List_Button_Bottom"
                                onmousedown="this.className='List_Button_Bottom_active'; return true;"
                                onmouseup="this.className='List_Button_Bottom'; return true;"
                                onmouseover="this.className='List_Button_Bottom_hover'; window.status='Delete all elements'; return true;"
                                onmouseout="this.className='List_Button_Bottom'; window.status=''; return true;">
                                <tr>
                                  <td class="List_Button_Bottom_bg">
                                    <img class="List_Button_Icon List_Button_Icon_DeleteAll" src="../web/images/blank.gif" alt="Delete all elements" title="Delete all elements"/>
                                  </td>
                                </tr>
                              </table>
                            </a>
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>
                </table>

              </td> 
              <td class="TitleCell"><span class="LabelText"><%= Resource.getProperty("finance.functionalclassification",lang) %></span></td>
              <td class="List_ContentCell" colspan="2">

                <table border="0" cellspacing="0" cellpadding="0">
                  <tr>
                    <td>
                      <select class="List_width List_height" name="inpcActivityId_IN" multiple="" id="inpcActivityId_IN">
                        <%   if(actList!=null && actList.size()>0) { %>
               <% for(GLJournalApprovalVO vo : actList) {%>
               <option value="<%= vo.getId() %>" ><span><%= Utility.escapeHTML(vo.getName())%></span></option>
                <%}}%>
                 </select>
                    </td>
                    <td class="List_Button_ContentCell">
                      <table border="0" cellspacing="0" cellpadding="0">
                        <tr>
                          <td>
                            <a class="List_Button_TopLink" href="#"
                              onclick="openMultiSearch(null, null, '../info/ActivityMultiple.html', 'SELECTOR_PROJECT', false, 'frmMain', 'inpcActivityId_IN');return false;"
                              onfocus="setWindowElementFocus(this); window.status='Add'; return true;"
                              onblur="window.status=''; return true;"
                              onkeypress="this.className='List_Button_TopLink_active'; return true;"
                              onkeyup="this.className='List_Button_TopLink_focus'; return true;">
                              <table class="List_Button_Top"
                                onmousedown="this.className='List_Button_Top_active'; return true;"
                                onmouseup="this.className='List_Button_Top'; return true;"
                                onmouseover="this.className='List_Button_Top_hover'; window.status='Add'; return true;"
                                onmouseout="this.className='List_Button_Top'; window.status=''; return true;">
                                <tr>
                                  <td class="List_Button_Top_bg">
                                    <img class="List_Button_Icon List_Button_Icon_Add" src="../web/images/blank.gif" alt="Add" title="Add"/>
                                  </td>
                                </tr>
                              </table>
                            </a>
                          </td>
                        </tr>
                        <tr>
                          <td class="List_Button_Separator">
                          </td>
                        </tr>
                        <tr>
                          <td>
                            <a class="List_Button_MiddleLink" href="#"
                              onclick="clearSelectedElements(document.frmMain.inpcActivityId_IN);return false;"
                              onfocus="setWindowElementFocus(this); window.status='Delete selected elements'; return true;"
                              onblur="window.status=''; return true;"
                              onkeypress="this.className='List_Button_MiddleLink_active'; return true;"
                              onkeyup="this.className='List_Button_MiddleLink_focus'; return true;">
                              <table class="List_Button_Middle"
                                onmousedown="this.className='List_Button_Middle_active'; return true;"
                                onmouseup="this.className='List_Button_Middle'; return true;"
                                onmouseover="this.className='List_Button_Middle_hover'; window.status='Delete selected elements'; return true;"
                                onmouseout="this.className='List_Button_Middle'; window.status=''; return true;">
                                <tr>
                                  <td class="List_Button_Middle_bg">
                                    <img class="List_Button_Icon List_Button_Icon_Delete" src="../web/images/blank.gif" alt="Delete selected elements" title="Delete selected elements"/>
                                  </td>
                                </tr>
                              </table>
                            </a>
                          </td>
                        </tr>
                        <tr>
                          <td class="List_Button_Separator">
                          </td>
                        </tr>
                        <tr>
                          <td>
                            <a class="List_Button_BottomLink" href="#"
                              onclick="clearList(document.frmMain.inpcActivityId_IN);return false;"
                              onfocus="setWindowElementFocus(this); window.status='Delete all elements'; return true;"
                              onblur="window.status=''; return true;"
                              onkeypress="this.className='List_Button_BottomLink_active'; return true;"
                              onkeyup="this.className='List_Button_BottomLink_focus'; return true;">
                              <table class="List_Button_Bottom"
                                onmousedown="this.className='List_Button_Bottom_active'; return true;"
                                onmouseup="this.className='List_Button_Bottom'; return true;"
                                onmouseover="this.className='List_Button_Bottom_hover'; window.status='Delete all elements'; return true;"
                                onmouseout="this.className='List_Button_Bottom'; window.status=''; return true;">
                                <tr>
                                  <td class="List_Button_Bottom_bg">
                                    <img class="List_Button_Icon List_Button_Icon_DeleteAll" src="../web/images/blank.gif" alt="Delete all elements" title="Delete all elements"/>
                                  </td>
                                </tr>
                              </table>
                            </a>
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>
                </table>

              </td> 
                
              <td class="ContentCell"></td>
              <td class="ContentCell"></td>
              <td class="ContentCell"></td>
            </tr>
            <tr id="Filter4">
             <td class="TitleCell"><span class="LabelText"><%= Resource.getProperty("finance.future1",lang) %></span></td>
              <td class="List_ContentCell" colspan="2">

                <table border="0" cellspacing="0" cellpadding="0">
                  <tr>
                    <td>
                      <select class="List_width List_height" name="inpcUser1Id_IN" multiple="" id="inpcUser1Id_IN">
                        <%   if(user1List!=null && user1List.size()>0) { %>
               <% for(GLJournalApprovalVO vo : user1List) {%>
               <option value="<%= vo.getId() %>" ><span><%= Utility.escapeHTML(vo.getName())%></span></option>
                <%}}%>
                    </select>
                    </td>
                    <td class="List_Button_ContentCell">
                      <table border="0" cellspacing="0" cellpadding="0">
                        <tr>
                          <td>
                            <a class="List_Button_TopLink" href="#"
                              onclick="openMultiSearch(null, null, '../info/User1Multiple.html', 'SELECTOR_DEPARTMENT', false, 'frmMain', 'inpcUser1Id_IN');return false;"
                              onfocus="setWindowElementFocus(this); window.status='Add'; return true;"
                              onblur="window.status=''; return true;"
                              onkeypress="this.className='List_Button_TopLink_active'; return true;"
                              onkeyup="this.className='List_Button_TopLink_focus'; return true;">
                              <table class="List_Button_Top"
                                onmousedown="this.className='List_Button_Top_active'; return true;"
                                onmouseup="this.className='List_Button_Top'; return true;"
                                onmouseover="this.className='List_Button_Top_hover'; window.status='Add'; return true;"
                                onmouseout="this.className='List_Button_Top'; window.status=''; return true;">
                                <tr>
                                  <td class="List_Button_Top_bg">
                                    <img class="List_Button_Icon List_Button_Icon_Add" src="../web/images/blank.gif" alt="Add" title="Add"/>
                                  </td>
                                </tr>
                              </table>
                            </a>
                          </td>
                        </tr>
                        <tr>
                          <td class="List_Button_Separator">
                          </td>
                        </tr>
                        <tr>
                          <td>
                            <a class="List_Button_MiddleLink" href="#"
                              onclick="clearSelectedElements(document.frmMain.inpcUser1Id_IN);return false;"
                              onfocus="setWindowElementFocus(this); window.status='Delete selected elements'; return true;"
                              onblur="window.status=''; return true;"
                              onkeypress="this.className='List_Button_MiddleLink_active'; return true;"
                              onkeyup="this.className='List_Button_MiddleLink_focus'; return true;">
                              <table class="List_Button_Middle"
                                onmousedown="this.className='List_Button_Middle_active'; return true;"
                                onmouseup="this.className='List_Button_Middle'; return true;"
                                onmouseover="this.className='List_Button_Middle_hover'; window.status='Delete selected elements'; return true;"
                                onmouseout="this.className='List_Button_Middle'; window.status=''; return true;">
                                <tr>
                                  <td class="List_Button_Middle_bg">
                                    <img class="List_Button_Icon List_Button_Icon_Delete" src="../web/images/blank.gif" alt="Delete selected elements" title="Delete selected elements"/>
                                  </td>
                                </tr>
                              </table>
                            </a>
                          </td>
                        </tr>
                        <tr>
                          <td class="List_Button_Separator">
                          </td>
                        </tr>
                        <tr>
                          <td>
                            <a class="List_Button_BottomLink" href="#"
                              onclick="clearList(document.frmMain.inpcUser1Id_IN);return false;"
                              onfocus="setWindowElementFocus(this); window.status='Delete all elements'; return true;"
                              onblur="window.status=''; return true;"
                              onkeypress="this.className='List_Button_BottomLink_active'; return true;"
                              onkeyup="this.className='List_Button_BottomLink_focus'; return true;">
                              <table class="List_Button_Bottom"
                                onmousedown="this.className='List_Button_Bottom_active'; return true;"
                                onmouseup="this.className='List_Button_Bottom'; return true;"
                                onmouseover="this.className='List_Button_Bottom_hover'; window.status='Delete all elements'; return true;"
                                onmouseout="this.className='List_Button_Bottom'; window.status=''; return true;">
                                <tr>
                                  <td class="List_Button_Bottom_bg">
                                    <img class="List_Button_Icon List_Button_Icon_DeleteAll" src="../web/images/blank.gif" alt="Delete all elements" title="Delete all elements"/>
                                  </td>
                                </tr>
                              </table>
                            </a>
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>
                </table>

              </td>
              <td class="TitleCell"><span class="LabelText"><%= Resource.getProperty("finance.future2",lang) %></span></td>
              <td class="List_ContentCell" colspan="2">

                <table border="0" cellspacing="0" cellpadding="0">
                  <tr>
                    <td>
                      <select class="List_width List_height" name="inpcUser2Id_IN" multiple="" id="inpcUser2Id_IN">
                       <%   if(user2List!=null && user2List.size()>0) { %>
               <% for(GLJournalApprovalVO vo : user2List) {%>
               <option value="<%= vo.getId() %>" ><span><%=Utility.escapeHTML( vo.getName())%></span></option>
                <%}}%>
                    </td>
                    <td class="List_Button_ContentCell">
                      <table border="0" cellspacing="0" cellpadding="0">
                        <tr>
                          <td>
                            <a class="List_Button_TopLink" href="#"
                              onclick="openMultiSearch(null, null, '../info/User2Multiple.html', 'SELECTOR_PROJECT', false, 'frmMain', 'inpcUser2Id_IN');return false;"
                              onfocus="setWindowElementFocus(this); window.status='Add'; return true;"
                              onblur="window.status=''; return true;"
                              onkeypress="this.className='List_Button_TopLink_active'; return true;"
                              onkeyup="this.className='List_Button_TopLink_focus'; return true;">
                              <table class="List_Button_Top"
                                onmousedown="this.className='List_Button_Top_active'; return true;"
                                onmouseup="this.className='List_Button_Top'; return true;"
                                onmouseover="this.className='List_Button_Top_hover'; window.status='Add'; return true;"
                                onmouseout="this.className='List_Button_Top'; window.status=''; return true;">
                                <tr>
                                  <td class="List_Button_Top_bg">
                                    <img class="List_Button_Icon List_Button_Icon_Add" src="../web/images/blank.gif" alt="Add" title="Add"/>
                                  </td>
                                </tr>
                              </table>
                            </a>
                          </td>
                        </tr>
                        <tr>
                          <td class="List_Button_Separator">
                          </td>
                        </tr>
                        <tr>
                          <td>
                            <a class="List_Button_MiddleLink" href="#"
                              onclick="clearSelectedElements(document.frmMain.inpcUser2Id_IN);return false;"
                              onfocus="setWindowElementFocus(this); window.status='Delete selected elements'; return true;"
                              onblur="window.status=''; return true;"
                              onkeypress="this.className='List_Button_MiddleLink_active'; return true;"
                              onkeyup="this.className='List_Button_MiddleLink_focus'; return true;">
                              <table class="List_Button_Middle"
                                onmousedown="this.className='List_Button_Middle_active'; return true;"
                                onmouseup="this.className='List_Button_Middle'; return true;"
                                onmouseover="this.className='List_Button_Middle_hover'; window.status='Delete selected elements'; return true;"
                                onmouseout="this.className='List_Button_Middle'; window.status=''; return true;">
                                <tr>
                                  <td class="List_Button_Middle_bg">
                                    <img class="List_Button_Icon List_Button_Icon_Delete" src="../web/images/blank.gif" alt="Delete selected elements" title="Delete selected elements"/>
                                  </td>
                                </tr>
                              </table>
                            </a>
                          </td>
                        </tr>
                        <tr>
                          <td class="List_Button_Separator">
                          </td>
                        </tr>
                        <tr>
                          <td>
                            <a class="List_Button_BottomLink" href="#"
                              onclick="clearList(document.frmMain.inpcUser2Id_IN);return false;"
                              onfocus="setWindowElementFocus(this); window.status='Delete all elements'; return true;"
                              onblur="window.status=''; return true;"
                              onkeypress="this.className='List_Button_BottomLink_active'; return true;"
                              onkeyup="this.className='List_Button_BottomLink_focus'; return true;">
                              <table class="List_Button_Bottom"
                                onmousedown="this.className='List_Button_Bottom_active'; return true;"
                                onmouseup="this.className='List_Button_Bottom'; return true;"
                                onmouseover="this.className='List_Button_Bottom_hover'; window.status='Delete all elements'; return true;"
                                onmouseout="this.className='List_Button_Bottom'; window.status=''; return true;">
                                <tr>
                                  <td class="List_Button_Bottom_bg">
                                    <img class="List_Button_Icon List_Button_Icon_DeleteAll" src="../web/images/blank.gif" alt="Delete all elements" title="Delete all elements"/>
                                  </td>
                                </tr>
                              </table>
                            </a>
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>
                </table>

              </td> 
              <td class="ContentCell"></td>
              <td class="ContentCell"></td>
              <td class="ContentCell"></td>
              </tr>
            <tr id="Filter2">
              <td class="TitleCell"><span class="LabelText"><%= Resource.getProperty("finance.budgetdetailreport.field.budgettyp",lang) %></span></td>
               <td class="Combo_ContentCell" colspan="2"> <select name="inpcCampaignId_IN" id="inpcCampaignId_IN" class="Combo Combo_TwoCells_width" >
                <%   if(FundsList!=null && FundsList.size()>0) { %>
               <% for(GLJournalApprovalVO vo : FundsList) {%>
               <% if(request.getAttribute("SelectedFunds")!=null){ %>
                <option value="<%= vo.getFundId() %>" <%if(request.getAttribute("SelectedFunds").equals(vo.getFundId())){ %> selected <%} %>><span><%= Utility.escapeHTML(vo.getFundName())%></span>
                </option><% }
               else{%>
               <option value="<%= vo.getFundId() %>" ><span><%= Utility.escapeHTML(vo.getFundName())%></span>
                <%}}}%>
                  <!-- <div id="reportCCampaignId_IN"></div> -->
                </select>
              </td> 
                
              <td class="ContentCell"></td>
              <td class="ContentCell"></td>
              <td class="ContentCell"></td>
            </tr>
            <!-- Group By -->
            <!--  <tr id="Filter5">
              <td class="TitleCell"><span class="LabelText">Group By</span></td>
              <td class="Combo_ContentCell"> <select name="inpGroupBy" id="inpGroupBy" class="Combo Combo_OneCell_width">
                  <option value=""></option>
                  <option value="BPartner">Business Partner</option>
                  <option value="Product">Product</option>
                  <option value="Department">Department</option>
                   <option value="BudgetType">Budget Type</option>
                    <option value="Project">Project</option>
                     <option value="FunClass">Functional Classification</option>
                      <option value="Future1">Future 1</option>
                       <option value="Future2">Future 2</option>
                </select>
                <input type="hidden" value="" id="paramSelectedGroupBy"/>
              </td>
              <td width="35%" class="TitleCell" colspan="2"><span class="LabelText">Include Zero Figures</span></td>
              <td width="20%" class="Radio_Check_ContentCell">
                <span class="Checkbox_container_NOT_Focused">
                  <input type="checkbox" name="inpIncludeZeroFigures" id="paramIncludeZeroFigures" value="Y"></input>
                </span>
              </td>
              <td class="TitleCell"></td>
            </tr>  -->
            
            <tr>
              <td colspan="6">
                <table class="FieldGroup" cellspacing="0" cellpadding="0" border="0">
                <tbody>
                  <tr class="FieldGroup_TopMargin"/>
                  <tr>
                    <td class="FieldGroupTitle_Left"><img class="FieldGroupTitle_Left_bg" border="0" src="../web/images/blank.gif"/></td>
                    <td class="FieldGroupTitle"><%= Resource.getProperty("finance.viewresults",lang) %>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td>
                    <td class="FieldGroupTitle_Right"><img class="FieldGroupTitle_Right_bg" border="0" src="../web/images/blank.gif"/></td>
                    <td class="FieldGroupContent"/>
                  </tr>
                  <tr class="FieldGroup_BottomMargin"/>
                </tbody>
                </table>
              </td>
            </tr>
            <tr><td>&nbsp;</td></tr>
            <tr>
              <td>
                <div>
                  <button type="button" 
                    id="buttonHTML" 
                    class="ButtonLink" 
                    onclick="getPTDData()" 
                    onfocus="buttonEvent('onfocus', this); window.status='View Results in a New Window'; return true;" 
                    onblur="buttonEvent('onblur', this);" 
                    onkeyup="buttonEvent('onkeyup', this);" 
                    onkeydown="buttonEvent('onkeydown', this);" 
                    onkeypress="buttonEvent('onkeypress', this);" 
                    onmouseup="buttonEvent('onmouseup', this);" 
                    onmousedown="buttonEvent('onmousedown', this);" 
                    onmouseover="buttonEvent('onmouseover', this); window.status='View Results in a New Window'; return true;" 
                    onmouseout="buttonEvent('onmouseout', this);">
                    <table class="Button">
                      <tr>
                        <td class="Button_left"><img class="Button_Icon Button_Icon_search" alt="Search" title="Search" src="../web/images/blank.gif" border="0" /></td>
                        <td class="Button_text"><%= Resource.getProperty("finance.search",lang) %></td>
                        <td class="Button_right"></td>
                      </tr>
                    </table>
                  </button>
                </div>
              </td>
              <td class="ContentCell"></input>
              </td>
              <td class="TitleCell"></td>
              <td class="ContentCell"></td>
              <td class="ContentCell"></td>
            </tr>
<tr><td>&nbsp;</td></tr>
<tr><td>&nbsp;</td></tr>
            <tr>
          </table>
          
           
          <table class="Main_Client_TableEdition">
            <tr>
              <td colspan="6">
                <div id="sectionGridView" style="display:none;">
                 <!--  <table cellspacing="0" cellpadding="0" width="100%" id="ResultSet" class="DataGrid_Header_Table DataGrid_Body_Table" style="table-layout: auto;">
                   <tr class="DataGrid_Body_Row">
                      <th width="392" colspan="2" class="DataGrid_Header_Cell">Account</th>
                      <th colspan="3" class="DataGrid_Header_Cell" >YTD as of<span>  </span></span><span id="paramToPeriodName">&nbsp;&nbsp; xx12/12/2003</span></th>
                    </tr>
                    <tr class="DataGrid_Body_Row">
                   <th width="116" class="DataGrid_Header_Cell"   class="DataGrid_Header_Cell"></th>
                    </tr>
                  <tr class="DataGrid_Body_Row">
                   <th width="116" class="DataGrid_Header_Cell"   class="DataGrid_Header_Cell"></th>
                      <th width="116" class="DataGrid_Header_Cell"  colspan="3" class="DataGrid_Header_Cell">O/B</th>
                      <th width="232"  class="DataGrid_Header_Cell" colspan="3"  class="DataGrid_Header_Cell">PTD</th>
                      <th width="116"  class="DataGrid_Header_Cell" colspan="3"  class="DataGrid_Header_Cell">YTD</th>
                    </tr>
                    <tr class="DataGrid_Body_Row">
                      <th width="116" class="DataGrid_Header_Cell" class="DataGrid_Header_Cell">Period</th>
                      <th width="232"  class="DataGrid_Header_Cell"  class="DataGrid_Header_Cell">DR</th>
                      <th width="116"  class="DataGrid_Header_Cell"   class="DataGrid_Header_Cell">CR</th>
                       <th width="116"  class="DataGrid_Header_Cell"   class="DataGrid_Header_Cell">NET</th>
                       <th width="232"  class="DataGrid_Header_Cell"  class="DataGrid_Header_Cell">DR</th>
                      <th width="116"  class="DataGrid_Header_Cell"   class="DataGrid_Header_Cell">CR</th>
                       <th width="116"  class="DataGrid_Header_Cell"   class="DataGrid_Header_Cell">NET</th>
                       <th width="232"  class="DataGrid_Header_Cell"  class="DataGrid_Header_Cell">DR</th>
                      <th width="116"  class="DataGrid_Header_Cell"   class="DataGrid_Header_Cell">CR</th>
                       <th width="116"  class="DataGrid_Header_Cell"   class="DataGrid_Header_Cell">NET</th>
                    </tr>
                   
                    <div   id="sectionParentAccount" style="display:none;" > 
                    <tr class="DataGrid_Body_Cell ParentAccountTR" style="display:none;" >
                        <th colspan="2" class="DataGrid_Header_Cell" id="fieldparentAccount" style="text-align:left;"><span id="fieldParentAccount">xx70000</span></th>
                         <th width="116" id="fieldTotalAmtacctdr"  class="DataGrid_Header_Cell_Amount" style= "text-align:right;">xx14500.34</th>
                      <th width="116" id="fieldTotalAmtacctcr"  class="DataGrid_Header_Cell_Amount" style= "text-align:right;">xx14500.34</th>
                      <th width="116" id="fieldTotalSaldoFinal" class="DataGrid_Header_Cell_Amount" style= "text-align:right;">xx14500.34</th>
                      </tr> 
                      
                    <tr class="DataGrid_Body_Row">
                      <th width="392" colspan="2" class="DataGrid_Header_Cell"></th>
                      <th width="116" class="DataGrid_Header_Cell">Balance As Of</th>
                      <th width="232" colspan="2" class="DataGrid_Header_Cell">Activity</th>
                      <th width="116" class="DataGrid_Header_Cell">Balance As Of</th>
                    </tr>
                    <tr class="DataGrid_Body_Row">
                      <th width="101" class="DataGrid_Header_Cell" style="text-align:left;">Account No.</th>
                      <th width="291" class="DataGrid_Header_Cell" style="text-align:left;"> Name </th>
                      <th width="116" class="DataGrid_Header_Cell"> <span id="paramDate_From">xx12/12/2003</span></th>
                      <th width="116" class="DataGrid_Header_Cell">Debit</th>
                      <th width="116" class="DataGrid_Header_Cell">Credit</th>
                      <th width="116" class="DataGrid_Header_Cell"><span id="paramDate_To">xx12/12/2003</span></th>
                     
                    </tr>
                    <div id="sectionDiscard">
                    <div id="sectionDetail">
                      <tr class="DataGrid_Body_Row DataGrid_Body_Row_yy" id="funcEvenOddRow1xx">
                        <td width="90" class="DataGrid_Body_Cell">     
                            <a href="#" onclick="openTabToGeneralLedgerReport('800001', 'xx');return false;" onmouseover="window.status='General Ledger';return true;" onmouseout="window.status='';return true;" class="LabelLink" id="fieldId1">
                              <span id="fieldAccount">xx70000</span>
                            </a>
                            <span id="fieldDescAccount">xx600</span>
                            <span id="showExpand" style="display: table-cell; display: -moz-inline-box;"><a href="#" onclick="updateData('OPEN', 'hhqq');return false;" onMouseOver="window.status='Open';return true;" onMouseOut="window.status='';return true;" id="expandButton">
                              <span id="buttonTreemmm" class="datawarehouseclose"></span>
                            </a></span>
                        </td>
                        <td width="280" id="fieldAccountName" class="DataGrid_Body_Cell">xxChecking Unallocated Receipts</td>
                        <td width="116" id="fieldSaldoInicial" class="DataGrid_Body_Cell_Amount">xx14500.34</td>
                        <td width="116" id="fieldAmtacctdr" class="DataGrid_Body_Cell_Amount">xx14500.34</td>
                        <td width="116" id="fieldAmtacctcr" class="DataGrid_Body_Cell_Amount">xx14500.34</td>
                        <td width="116" id="fieldSaldoFinal" class="DataGrid_Body_Cell_Amount">xx14500.34</td>
                      </tr>
                    </div>
                    </div>
                     </div>
                     <tr class="DataGrid_Body_Row"> 
                      <th  class="DataGrid_Header_Cell"></th>
                      <th  class="DataGrid_Header_Cell" style="text-align:left;">Totals</th>
                      <th width="116" id="fieldTotalSaldoInicial" class="DataGrid_Header_Cell_Amount" style= "text-align:right;">xx14500.34</th>
                      <th width="116" id="fieldTotalAmtacctdr"  class="DataGrid_Header_Cell_Amount" style= "text-align:right;">xx14500.34</th>
                      <th width="116" id="fieldTotalAmtacctcr"  class="DataGrid_Header_Cell_Amount" style= "text-align:right;">xx14500.34</th>
                      <th width="116" id="fieldTotalSaldoFinal" class="DataGrid_Header_Cell_Amount" style= "text-align:right;">xx14500.34</th>
                    </tr> 
                  </table>
                  </div>
                  <br/>
                </div>
              </td>
            </tr>
          </table>  -->
                                    <!-- USER CONTROLS -->

        </div>
        </td>
        <tr><td>&nbsp;</td></tr>
<tr><td>&nbsp;</td></tr>
        </tr>
        </table>
        </div>
         <table width="100%" border="0" cellspacing="0" cellpadding="0" class="Menu_ContentPane_Bottom" id="tdbottomSeparator">
          <tr>
           <td class="Main_Bottom_bg"><img src="../web/images/blank.gif" border="0"></td> 
          </tr>
        </table></td> 
       <td valign="top">
        <table width="100%" border="0" cellspacing="0" cellpadding="0" class="Main_ContentPane_Right" id="tdrightSeparator">
         <tr><td class="Main_NavBar_bg_right"></td></tr>
          <tr><td class="Main_ToolBar_bg_right" valign="top"></td></tr>
          <tr><td class="Main_Client_bg_right"></td></tr>
          <tr><td class="Main_Bottom_bg_right"></td></tr> 
        </table>
      </td> 
    </tr>
  </table>
</form>
</body>
</HTML>