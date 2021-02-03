<%@page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page import="sa.elm.ob.finance.properties.Resource, sa.elm.ob.utility.util.Utility"
    errorPage="/web/jsp/ErrorPage.jsp"%>
 <%@ page import="sa.elm.ob.finance.ad_reports.IntegratedCostBudgetInquiry.vo.IntegratedCostBudgetInquiryVO, java.util.List,java.util.ArrayList" %>
 <%@page import="org.codehaus.jettison.json.JSONArray"%>
<%@page import="org.codehaus.jettison.json.JSONObject"%>

<%
    String lang = ((String) session.getAttribute("#AD_LANGUAGE"));
String style="../web/skins/ltr/org.openbravo.userinterface.skin.250to300Comp/250to300Comp/Openbravo_ERP.css";
if(lang.equals("ar_SA")){
     style="../web/skins/rtl/org.openbravo.userinterface.skin.250to300Comp/250to300Comp/Openbravo_ERP_250.css";
}
List<IntegratedCostBudgetInquiryVO> OrgList = (request.getAttribute("organization")==null?null:(ArrayList<IntegratedCostBudgetInquiryVO>)request.getAttribute("organization"));
List<IntegratedCostBudgetInquiryVO> SchemaList = (request.getAttribute("acctschema")==null?null:(ArrayList<IntegratedCostBudgetInquiryVO>)request.getAttribute("acctschema"));
List<IntegratedCostBudgetInquiryVO> FundsList = (request.getAttribute("funds")==null?null:(ArrayList<IntegratedCostBudgetInquiryVO>)request.getAttribute("funds"));
List<IntegratedCostBudgetInquiryVO> ClientList = (request.getAttribute("client")==null?null:(ArrayList<IntegratedCostBudgetInquiryVO>)request.getAttribute("client"));
List<IntegratedCostBudgetInquiryVO> AccountList = (request.getAttribute("account")==null?null:(ArrayList<IntegratedCostBudgetInquiryVO>)request.getAttribute("account"));
List<IntegratedCostBudgetInquiryVO> DeptList = (request.getAttribute("department")==null?null:(ArrayList<IntegratedCostBudgetInquiryVO>)request.getAttribute("department"));
List<IntegratedCostBudgetInquiryVO> SubAccountList = (request.getAttribute("subaccount")==null?null:(ArrayList<IntegratedCostBudgetInquiryVO>)request.getAttribute("subaccount"));


JSONObject LinesList=null;
if(request.getAttribute("LinesList")!=null){
 LinesList = new JSONObject(request.getAttribute("LinesList").toString());
}%>

<HTML xmlns:="http://www.w3.org/1999/xhtml">
<HEAD>
<META http-equiv="Content-Type" content="text/html; charset=utf-8"></META>
<LINK rel="stylesheet" type="text/css" href="<%=style %>" id="paramCSS"></LINK>
 <link rel="stylesheet" type="text/css" href="../web/js/common/select2.min.css"></link>
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
          //enableShortcuts('edition');
          setBrowserAutoComplete(false);
          setFocusFirstControl(null, 'inpMailTemplate');
          resizeArea();
          updateMenuIcon('buttonMenu');
      
         displaySectionResult();
       
}


function displaySectionResult(){
    var budgetType="<%=request.getAttribute("SelectedFunds")%>";
    var budgetTypeValue="<%=request.getAttribute("SelectedBudgetTypeValue")%>";
    
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
            
          
            mytable =mytable +" <tr class='DataGrid_Body_Row'><th width='232'  class='DataGrid_Header_Cell' colspan='1'  class='DataGrid_Header_Cell'>"+data[j].uniquecode+"</th><th width='232'  class='DataGrid_Header_Cell' colspan='1'  class='DataGrid_Header_Cell'></th><th width='232'  class='DataGrid_Header_Cell' colspan='1'  class='DataGrid_Header_Cell'></th><th width='232'  class='DataGrid_Header_Cell' colspan='2'  class='DataGrid_Header_Cell'></th><th width='232'  class='DataGrid_Header_Cell' colspan='2'  class='DataGrid_Header_Cell'></th><th width='232'  class='DataGrid_Header_Cell' colspan='1'  class='DataGrid_Header_Cell'></th><th width='232'  class='DataGrid_Header_Cell' colspan='1'  class='DataGrid_Header_Cell'></th><th width='232'  class='DataGrid_Header_Cell' colspan='1'  class='DataGrid_Header_Cell'></th><th width='232'  class='DataGrid_Header_Cell' colspan='1'  class='DataGrid_Header_Cell'></th>";
            if(budgetTypeValue=="F"){
                 mytable =mytable +"<th width='232'  class='DataGrid_Header_Cell' colspan='1'  class='DataGrid_Header_Cell'></th>";
            }
            if(budgetTypeValue!="F"){
            mytable =mytable +"</th><th width='232'  class='DataGrid_Header_Cell' colspan='1'  class='DataGrid_Header_Cell'></th><th width='232'  class='DataGrid_Header_Cell' colspan='1'  class='DataGrid_Header_Cell'>"+data[j].funduniquecode+"</th><th width='232'  class='DataGrid_Header_Cell' colspan='2'  class='DataGrid_Header_Cell'></th><th width='232'  class='DataGrid_Header_Cell' colspan='2'  class='DataGrid_Header_Cell'></th><th width='232'  class='DataGrid_Header_Cell' colspan='1'  class='DataGrid_Header_Cell'></th><th width='232'  class='DataGrid_Header_Cell' colspan='1'  class='DataGrid_Header_Cell'></th><th width='232'  class='DataGrid_Header_Cell' colspan='1'  class='DataGrid_Header_Cell'></th><th width='232'  class='DataGrid_Header_Cell' colspan='1'  class='DataGrid_Header_Cell'></th><th width='232'  class='DataGrid_Header_Cell' colspan='1'  class='DataGrid_Header_Cell'></th><th width='232'  class='DataGrid_Header_Cell' colspan='1'  class='DataGrid_Header_Cell'></th></tr>";
            }
            mytable =mytable +"<tr><th width='116' class='DataGrid_Header_Cell'   class='DataGrid_Header_Cell'></th> <th width='116' class='DataGrid_Header_Cell'  colspan='1' class='DataGrid_Header_Cell'><%= Resource.getProperty("finance.costbudgetFA",lang) %></th>";
            mytable =mytable +" <th width='232'  class='DataGrid_Header_Cell' colspan='1'  class='DataGrid_Header_Cell'><%= Resource.getProperty("finance.original.budget",lang) %></th><th width='116'  class='DataGrid_Header_Cell' colspan='2'  class='DataGrid_Header_Cell'><%= Resource.getProperty("finance.budget.adjustment",lang) %></th><th width='116'  class='DataGrid_Header_Cell' colspan='2'  class='DataGrid_Header_Cell'><%= Resource.getProperty("finance.budget.revision",lang) %></th><th width='116'  class='DataGrid_Header_Cell' colspan='2'  class='DataGrid_Header_Cell'><%= Resource.getProperty("finance.internal.distributions",lang) %></th><th width='116'  class='DataGrid_Header_Cell' colspan='1'  class='DataGrid_Header_Cell'><%= Resource.getProperty("finance.current.budget",lang) %></th><th width='116'  class='DataGrid_Header_Cell' colspan='1'  class='DataGrid_Header_Cell'><%= Resource.getProperty("finance.encumbrance",lang) %></th><th width='116'  class='DataGrid_Header_Cell' colspan='1'  class='DataGrid_Header_Cell'><%= Resource.getProperty("finance.funds.available",lang) %></th>";
            
            if(budgetTypeValue!="F"){
            mytable =mytable +" <th width='116'  class='DataGrid_Header_Cell' colspan='1'  class='DataGrid_Header_Cell'><%= Resource.getProperty("finance.original.budget",lang) %></th><th width='116'  class='DataGrid_Header_Cell' colspan='2'  class='DataGrid_Header_Cell'><%= Resource.getProperty("finance.budget.adjustment",lang) %></th><th width='116'  class='DataGrid_Header_Cell' colspan='2'  class='DataGrid_Header_Cell'><%= Resource.getProperty("finance.budget.revision",lang) %></th><th width='116'  class='DataGrid_Header_Cell' colspan='2'  class='DataGrid_Header_Cell'><%= Resource.getProperty("finance.internal.distributions",lang) %></th><th width='116'  class='DataGrid_Header_Cell' colspan='1'  class='DataGrid_Header_Cell'><%= Resource.getProperty("finance.current.budget",lang) %></th><th width='116'  class='DataGrid_Header_Cell' colspan='1'  class='DataGrid_Header_Cell'><%= Resource.getProperty("finance.encumbrance",lang) %></th><th width='116'  class='DataGrid_Header_Cell' colspan='1'  class='DataGrid_Header_Cell'><%= Resource.getProperty("finance.actual",lang) %></th><th width='116'  class='DataGrid_Header_Cell' colspan='1'  class='DataGrid_Header_Cell'><%= Resource.getProperty("finance.funds.available",lang) %></th>";
            }
            mytable =mytable +"</tr> <tr class='DataGrid_Body_Row'>  <th width='116' class='DataGrid_Header_Cell' class='DataGrid_Header_Cell'><%= Resource.getProperty("finance.year",lang) %></th>";
            mytable =mytable +"<th width='232'  class='DataGrid_Header_Cell'  class='DataGrid_Header_Cell'></th>";
            mytable =mytable +"<th width='116'  class='DataGrid_Header_Cell'   class='DataGrid_Header_Cell'></th>";
            mytable =mytable +"<th width='116'  class='DataGrid_Header_Cell'   class='DataGrid_Header_Cell'><%= Resource.getProperty("finance.increase",lang) %></th>"; 
            mytable =mytable +"<th width='232'  class='DataGrid_Header_Cell'  class='DataGrid_Header_Cell'><%= Resource.getProperty("finance.decrease",lang) %></th>";
            mytable =mytable +"<th width='116'  class='DataGrid_Header_Cell'   class='DataGrid_Header_Cell'><%= Resource.getProperty("finance.increase",lang) %></th>";
            mytable =mytable +"<th width='116'  class='DataGrid_Header_Cell'   class='DataGrid_Header_Cell'><%= Resource.getProperty("finance.decrease",lang) %></th>";
            mytable =mytable +"<th width='116'  class='DataGrid_Header_Cell'   class='DataGrid_Header_Cell'><%= Resource.getProperty("finance.increase",lang) %></th>";
            mytable =mytable +"<th width='116'  class='DataGrid_Header_Cell'   class='DataGrid_Header_Cell'><%= Resource.getProperty("finance.decrease",lang) %></th>";
            mytable =mytable +"<th width='232'  class='DataGrid_Header_Cell'  class='DataGrid_Header_Cell'></th>";
            mytable =mytable +"<th width='116'  class='DataGrid_Header_Cell'   class='DataGrid_Header_Cell'></th>  <th width='116'  class='DataGrid_Header_Cell'   class='DataGrid_Header_Cell'></th>";
           
            if(budgetTypeValue!="F"){
            mytable =mytable +"<th width='116'  class='DataGrid_Header_Cell'   class='DataGrid_Header_Cell'></th>";
            mytable =mytable +"<th width='116'  class='DataGrid_Header_Cell'   class='DataGrid_Header_Cell'><%= Resource.getProperty("finance.increase",lang) %></th>";
            mytable =mytable +"<th width='116'  class='DataGrid_Header_Cell'   class='DataGrid_Header_Cell'><%= Resource.getProperty("finance.decrease",lang) %></th>";
            mytable =mytable +"<th width='116'  class='DataGrid_Header_Cell'   class='DataGrid_Header_Cell'><%= Resource.getProperty("finance.increase",lang) %></th>";
            mytable =mytable +"<th width='116'  class='DataGrid_Header_Cell'   class='DataGrid_Header_Cell'><%= Resource.getProperty("finance.decrease",lang) %></th>";
            mytable =mytable +"<th width='116'  class='DataGrid_Header_Cell'   class='DataGrid_Header_Cell'><%= Resource.getProperty("finance.increase",lang) %></th>";
            mytable =mytable +"<th width='116'  class='DataGrid_Header_Cell'   class='DataGrid_Header_Cell'><%= Resource.getProperty("finance.decrease",lang) %></th>";
            mytable =mytable +"<th width='116'  class='DataGrid_Header_Cell'   class='DataGrid_Header_Cell'></th>";
            mytable =mytable +"<th width='116'  class='DataGrid_Header_Cell'   class='DataGrid_Header_Cell'></th>";
            mytable =mytable +"<th width='116'  class='DataGrid_Header_Cell'   class='DataGrid_Header_Cell'></th>";
            mytable =mytable +"<th width='116'  class='DataGrid_Header_Cell'   class='DataGrid_Header_Cell'></th>";
            mytable =mytable +"<th width='116'  class='DataGrid_Header_Cell'   class='DataGrid_Header_Cell'></th>";
            mytable =mytable +"<th width='116'  class='DataGrid_Header_Cell'   class='DataGrid_Header_Cell'></th>";
            mytable =mytable +"<th width='116'  class='DataGrid_Header_Cell'   class='DataGrid_Header_Cell'></th>";
            mytable =mytable +"<th width='116'  class='DataGrid_Header_Cell'   class='DataGrid_Header_Cell'></th>";
            mytable =mytable +"<th width='116'  class='DataGrid_Header_Cell'   class='DataGrid_Header_Cell'></th></tr>";                               
            }
            mytable =mytable+"</thead>";
            table.innerHTML=mytable;
                                      
            
            var costOriginalBudget="0.00",costBudgetRevInc="0.00",costBudgetRevDec="0.00",
            costBudgetDisInc="0.00",costBudgetDicDec="0.00",costCurrentBudget="0.00",
            costEncum="0.00",costFundsAvail="0.00",costactual="0.00",fundOriginalBudget="0.00",
            fundBudgetRevInc="0.00",fundBudgetRevDec="0.00",fundBudgetDisInc="0.00",fundBudgetDicDec="0.00",
            fundCurrentBudget="0.00",fundEncum="0.00",fundactual="0.00",fundFundsAvail="0.00",
            costBudgetAdjustIncrease="0.00",costBudgetAdjustDecrease="0.00",fundAdjustIncrease="0.00",fundAdjustDecrease="0.00";
            for(var p in object) {
                var processId="800001";
                var rowCount = table.rows.length;
                var row = table.insertRow(rowCount);
                var i=0;
                row.className = "DataGrid_Body_Row DataGrid_Body_Row_" + (( i + 1) % 2);
                var cellTotalNo=23;
                if(budgetTypeValue=="F"){
                    cellTotalNo=14;
                }
          for ( var k = 0; k <= cellTotalNo; k++) {
              var cell = row.insertCell(k);
              switch (k) {
            
              case 0:
                  cell.className = "DataGrid_Body_Cell";
                  cell.style.textAlign = "center";
                  cell.style.padding = "right: 5 px";
                  cell.innerHTML = object[p].year;
              break;
     
              case 1:
                  cell.className = "DataGrid_Body_Cell";
                  cell.style.textAlign = "right";
                  cell.style.padding = "right: 5 px";
                 // cell.innerHTML = "&nbsp;";    /* &nbsp; or ''  is for one blank space*/
                 // cell.innerHTML = "<a  class='LabelLink'  href='javascript:OpenGeneralLedgerReport(\"" + processId + "\", \"" + object[p].id + "\", \"" + object[p].periodid + "\",\"" + data[j].uniquecode + "\")'>" + object[p].previousyearbudget + "</a>";
                  cell.innerHTML = object[p].previousyearbudget; 
                  break;  
                  
              case 2:
                  cell.className = "DataGrid_Body_Cell";
                  cell.style.textAlign = "right";
                  cell.style.padding = "right: 5 px";
                  if(object[p].costOriginalBudget==0){
                      cell.innerHTML = costOriginalBudget;
                  }
                  else
                  cell.innerHTML = object[p].costOriginalBudget;
                  break;
                  
              case 3:
                  cell.className = "DataGrid_Body_Cell DataGrid_Header_Cell_Amount";
                  cell.style.textAlign = "right";
                  if(object[p].costBudgetAdjustIncrease==0){
                      cell.innerHTML = costBudgetAdjustIncrease;
                  }
                  else
                  cell.innerHTML = object[p].costBudgetAdjustIncrease;
                  break;
              case 4:
                  cell.className = "DataGrid_Body_Cell";
                  cell.style.textAlign = "right";
                  if(object[p].costBudgetAdjustDecrease==0){
                      cell.innerHTML = costBudgetAdjustDecrease;
                  }
                  else
                  cell.innerHTML = object[p].costBudgetAdjustDecrease;
                  break;
                  
              case 5:
                  cell.className = "DataGrid_Body_Cell DataGrid_Header_Cell_Amount";
                  cell.style.textAlign = "right";
                  if(object[p].costBudgetRevInc==0){
                      cell.innerHTML = costBudgetRevInc;
                  }
                  else
                  cell.innerHTML = object[p].costBudgetRevInc;
                  break;
              case 6:
                  cell.className = "DataGrid_Body_Cell";
                  cell.style.textAlign = "right";
                  if(object[p].costBudgetRevDec==0){
                      cell.innerHTML = costBudgetRevDec;
                  }
                  else
                  cell.innerHTML = object[p].costBudgetRevDec;
                  break;
              case 7:
                  cell.className = "DataGrid_Body_Cell";
                  cell.style.textAlign = "right";
                  if(object[p].costBudgetDisInc==0){
                      cell.innerHTML = costBudgetDisInc;
                  }
                  else
                  cell.innerHTML = object[p].costBudgetDisInc;
                  break;
              case 8:
                  cell.className = "DataGrid_Body_Cell";
                  cell.style.textAlign = "right";
                  if(object[p].costBudgetDicDec==0){
                      cell.innerHTML = costBudgetDicDec;
                  }
                  else
                  cell.innerHTML = object[p].costBudgetDicDec;
                  break;
               
              case 9:
                  cell.className = "DataGrid_Body_Cell";
                  cell.style.textAlign = "right";
                  if(object[p].costCurrentBudget==0){
                      cell.innerHTML = costCurrentBudget;
                  }
                  else
                  cell.innerHTML = object[p].costCurrentBudget;
                  break;
              case 10:
                  cell.className = "DataGrid_Body_Cell";
                  cell.style.textAlign = "right";
                  if(object[p].costEncum==0){
                      cell.innerHTML = costEncum;
                  }
                  else
                  cell.innerHTML = object[p].costEncum;
                  break;
              case 11:
                  cell.className = "DataGrid_Body_Cell";
                  cell.style.textAlign = "right";
                  if(object[p].costFundsAvail==0){
                      cell.innerHTML = costFundsAvail;
                  }
                  else
                  cell.innerHTML = object[p].costFundsAvail;
                  break;
              case 12:
                  cell.className = "DataGrid_Body_Cell";
                  cell.style.textAlign = "right";
                  if(object[p].fundOriginalBudget==0){
                      cell.innerHTML = fundOriginalBudget;
                  }
                  else
                  cell.innerHTML = object[p].fundOriginalBudget;
                  break;
              case 13:
                  cell.className = "DataGrid_Body_Cell";
                  cell.style.textAlign = "right";
                  if(object[p].fundAdjustIncrease==0){
                      cell.innerHTML = fundAdjustIncrease;
                  }
                  else
                  cell.innerHTML = object[p].fundAdjustIncrease;
                  break;
              case 14:
                  cell.className = "DataGrid_Body_Cell";
                  cell.style.textAlign = "right";
                  if(object[p].fundAdjustDecrease==0){
                      cell.innerHTML = fundAdjustDecrease;
                  }
                  else
                  cell.innerHTML = object[p].fundAdjustDecrease;
                  break;
              case 15:
                  cell.className = "DataGrid_Body_Cell";
                  cell.style.textAlign = "right";
                  if(object[p].fundBudgetRevInc==0){
                      cell.innerHTML = fundBudgetRevInc;
                  }
                  else
                  cell.innerHTML = object[p].fundBudgetRevInc;
                  break;
              case 16:
                  cell.className = "DataGrid_Body_Cell";
                  cell.style.textAlign = "right";
                  if(object[p].fundBudgetRevDec==0){
                      cell.innerHTML = fundBudgetRevDec;
                  }
                  else
                  cell.innerHTML = object[p].fundBudgetRevDec;
                  break;
              case 17:
                  cell.className = "DataGrid_Body_Cell";
                  cell.style.textAlign = "right";
                  if(object[p].fundBudgetDisInc==0){
                      cell.innerHTML = fundBudgetDisInc;
                  }
                  else
                  cell.innerHTML = object[p].fundBudgetDisInc;
                  break;
              case 18:
                  cell.className = "DataGrid_Body_Cell";
                  cell.style.textAlign = "right";
                  if(object[p].fundBudgetDicDec==0){
                      cell.innerHTML = fundBudgetDicDec;
                  }
                  else
                  cell.innerHTML = object[p].fundBudgetDicDec;
                  break;
              case 19:
                  cell.className = "DataGrid_Body_Cell";
                  cell.style.textAlign = "right";
                  if(object[p].fundCurrentBudget==0){
                      cell.innerHTML = fundCurrentBudget;
                  }
                  else
                  cell.innerHTML = object[p].fundCurrentBudget;
                  break;
              case 20:
                  cell.className = "DataGrid_Body_Cell";
                  cell.style.textAlign = "right";
                  if(object[p].fundEncum==0){
                      cell.innerHTML = fundEncum;
                  }
                  else
                  cell.innerHTML = object[p].fundEncum;
                  break;
              case 21:
                  cell.className = "DataGrid_Body_Cell";
                  cell.style.textAlign = "right";
                  if(object[p].fundactual==0){
                      cell.innerHTML = fundactual;
                  }
                  else
                  cell.innerHTML = object[p].fundactual;
                  break;
              case 22:
                  cell.className = "DataGrid_Body_Cell";
                  cell.style.textAlign = "right";
                  if(object[p].fundFundsAvail==0){
                      cell.innerHTML = fundFundsAvail;
                  }
                  else
                  cell.innerHTML = object[p].fundFundsAvail;
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

    
function onClickRefresh() {
    document.getElementById("inpAction").value="";
    reloadWindow();
    return false;
}  

function reloadWindow() 
{
   submitCommandForm('DEFAULT', false, null, 'IntegratedCostBudgetInquiry', '_self', null, false); 
    return false;
}
function getDetail(){

     if (document.getElementById("inpOrg").value == 0) {
            OBAlert('<%=Resource.getProperty("finance.gladjustmentreport.alert.select.organization",lang).replace("'","\\\'") %>');
            return false;
        }
    document.getElementById("inpAction").value="getData";
    reloadWindow();
}
function getReport(){
    document.getElementById("inpAction").value="getReport";
    submitCommandForm('DEFAULT', false, null, 'IntegratedCostBudgetInquiry', 'background_target', null, false); 
}
function getReportExcel(){
    document.getElementById("inpAction").value="getReportExcel";
    submitCommandForm('DEFAULT', false, null, 'IntegratedCostBudgetInquiry', 'background_target', null, false); 
}

</script>
</HEAD>
 <body onresize="onResizeDo();" leftmargin="0" topmargin="0" marginwidth="0" marginheight="0" >
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
      <INPUT type="hidden" name="inpaccount" id="inpaccount" value=""></INPUT>
     
    
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
                                        title="<%=Resource.getProperty("finance.reload", lang)%>"
                                        border="0" id="linkButtonRefresh">
                                </a>
                                </td>
                                <td width="2%" ><a href="#" onClick="getReport();return false;return false;" class="Main_ToolBar_Button" onMouseOver="window.status='Print Record';return true;" onMouseOut="window.status='';return true;" onclick="this.hideFocus=true" onblur="this.hideFocus=false" id="linkButtonPrint"><img class="Main_ToolBar_Button_Icon Main_ToolBar_Button_Icon_Print" src="../web/images/blank.gif" title="<%=Resource.getProperty("finance.printreport", lang)%>" border="0" id="buttonPrint"></a></td>
                                  <td width="2%" ><a href="#" onClick="getReportExcel();return false;return false;" class="Main_ToolBar_Button" onMouseOver="window.status='Export to Excel';return true;" onMouseOut="window.status='';return true;" onclick="this.hideFocus=true" onblur="this.hideFocus=false" id="linkButtonExcel"><img class="Main_ToolBar_Button_Icon Main_ToolBar_Button_Icon_Excel" src="../web/images/blank.gif" title="<%=Resource.getProperty("finance.printreport.excel", lang)%>" border="0" id="buttonExcel"></a></td>
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
                    <td class="FieldGroupTitle"><%= Resource.getProperty("finance.filters",lang)%>&nbsp;&nbsp;&nbsp;&nbsp;</td>
                    <td class="FieldGroupTitle_Right"><img class="FieldGroupTitle_Right_bg" border="0" src="../web/images/blank.gif"/></td>
                    <td class="FieldGroupContent"/>
                  </tr>
                  <tr class="FieldGroup_BottomMargin"/>
                </tbody>
                </table>
              </td>
            </tr>
               <tr>
              <td class="TitleCell"><span class="LabelText"><%= Resource.getProperty("finance.client",lang)%></span></td>
               <td class="Combo_ContentCell">
                <select  name="inpClient" id="inpClient" class="ComboKey Combo_TwoCells_width" required="true">
                 <%   if(ClientList!=null && ClientList.size()>0) { %>
               <% for(IntegratedCostBudgetInquiryVO vo : ClientList) {%>
               <% if(request.getAttribute("SelectedClient")!=null){ %>
                <option value="<%= vo.getclientId() %>" <%if(request.getAttribute("SelectedClient").equals(vo.getclientId())){ %> selected <%} %>><span><%= Utility.escapeHTML(vo.getclientName())%></span>
                </option><% }
               else{%>
               <option value="<%= vo.getclientId() %>" ><span><%=Utility.escapeHTML(vo.getclientName())%></span></option>
                <%}}}%>
                </select></td> 
              
            </tr>
            
                  <tr>
              <td class="TitleCell"><span class="LabelText"><%= Resource.getProperty("finance.generalledger",lang)%></span></td>
              <td class="Combo_ContentCell" colspan="2"> <select name="inpcAcctSchemaId" id="inpcAcctSchemaId" class="ComboKey Combo_TwoCells_width" required="true">
                   <%   if(SchemaList!=null && SchemaList.size()>0) { %>
               <% for(IntegratedCostBudgetInquiryVO vo : SchemaList) {%>
               <% if(request.getAttribute("SelectedSchema")!=null){ %>
                <option value="<%= vo.getAcctschemaId() %>" <%if(request.getAttribute("SelectedSchema").equals(vo.getAcctschemaId())){ %> selected <%} %>><span><%= Utility.escapeHTML(vo.getAcctschemaName())%></span>
                </option><% }
               else{%>
               <option value="<%= vo.getAcctschemaId() %>" ><span><%= Utility.escapeHTML(vo.getAcctschemaName())%></span></option>
                <%}}}%>
                  
                </select>
              </td>            
            </tr>
   
             <%-- for list drop down --%>
        <%--     <tr>
              <td class="TitleCell"><span class="LabelText"><%= Resource.getProperty("finance.organization",lang)%></span></td>
              <td class="Combo_ContentCell">
                <select  name="inpOrg" id="inpOrg" class="ComboKey Combo_TwoCells_width" required="true">
                 <%   if(OrgList!=null && OrgList.size()>0) { %>
               <% for(IntegratedCostBudgetInquiryVO vo : OrgList) {%>
               <% if(request.getAttribute("SelectedOrg")!=null){ %>
                <option value="<%= vo.getOrgId() %>" <%if(request.getAttribute("SelectedOrg").equals(vo.getOrgId())){ %> selected <%} %>><span><%= Utility.escapeHTML(vo.getOrgName())%></span>
                </option><% }
               else{%>
               <option value="<%= vo.getOrgId() %>" ><span><%=Utility.escapeHTML(vo.getOrgName())%></span></option>
                <%}}}%>
              
                </select></td>
                    
            </tr> --%>
            
               <tr>
              <td class="TitleCell"><span class="LabelText"> <b> <%=Resource.getProperty("finance.organization", lang).replace("'", "\\\'")%>
                </b>
                </span>
                 </td>
                 <td class="TextBox_ContentCell">
                  <select id="inpOrg" name ="inpOrg"  class="ComboKey Combo_TwoCells_width">
                <%--<option value="0" selected></option> --%> 
                  <option value="<%=request.getAttribute("SelectedOrg") %>"  selected>
                  <span><%= request.getAttribute("SelectedOrgName")%></span>
                </option> 
                  </select>
                 </td>
                 </tr>
            
            
              <tr>
              <td class="TitleCell"><span class="LabelText"> <b> <%=Resource.getProperty("finance.account", lang).replace("'", "\\\'")%>
                </b>
                </span>
                 </td>
                 <td class="TextBox_ContentCell">
                  <select id="inpAccount" name ="inpAccount" class="ComboKey Combo_TwoCells_width">
                 <option value="<%=request.getAttribute("SelectedAccount")==null?"0":request.getAttribute("SelectedAccount") %>"  selected>
                  <span><%= request.getAttribute("SelectedAccountName")==null?"Selected":request.getAttribute("SelectedAccountName")%></span>  
                  </select>
                 </td>
                 </tr>
         
            
             <tr>
              <td class="TitleCell"><span class="LabelText"> <b> <%=Resource.getProperty("finance.department", lang).replace("'", "\\\'")%>
                </b>
                </span>
                 </td>
                 <td class="TextBox_ContentCell">
                  <select id="inpDept" name ="inpDept" class="ComboKey Combo_TwoCells_width">
                  <option value="<%=request.getAttribute("SelectedDept") %>"  selected>
                  <span><%= request.getAttribute("SelectedDeptName")%></span>
                  </select>
                 </td>
                 </tr>
            
      
            <tr>
              <td class="TitleCell"><span class="LabelText"> <b> <%=Resource.getProperty("finance.subaccount", lang).replace("'", "\\\'")%>
                </b>
                </span>
                 </td>
                 <td class="TextBox_ContentCell">
                  <select id="inpSubAccount" name ="inpSubAccount" class="ComboKey Combo_TwoCells_width" >
                  <option value="<%=request.getAttribute("SelectedSubAccount")==null?"0":request.getAttribute("SelectedSubAccount") %>"  selected>
                  <span><%= request.getAttribute("SelectedSubAccountName")==null?"Selected":request.getAttribute("SelectedSubAccountName")%></span>
                  </select>
                 </td>
                 </tr>
            
            <tr>
        
            </tr>
           

            <tr id="Filter2">
              <td class="TitleCell"><span class="LabelText"><%= Resource.getProperty("finance.budgetdetailreport.field.budgettyp",lang) %></span></td>
               <td class="Combo_ContentCell" colspan="2"> <select name="inpcCampaignId_IN" id="inpcCampaignId_IN" class="Combo Combo_TwoCells_width" >
                <%   if(FundsList!=null && FundsList.size()>0) { %>
               <% for(IntegratedCostBudgetInquiryVO vo : FundsList) {%>
               <% if(request.getAttribute("SelectedFunds")!=null){ %>
                <option value="<%= vo.getBudgetTypeId() %>" <%if(request.getAttribute("SelectedFunds").equals(vo.getBudgetTypeId())){ %> selected <%} %>><span><%= Utility.escapeHTML(vo.getBudgetTypeName())%></span>
                </option><% }
               else{%>
               <option value="<%= vo.getBudgetTypeId() %>" ><span><%= Utility.escapeHTML(vo.getBudgetTypeName())%></span>
                <%}}}%>
                </select>
              </td> 
                
              <td class="ContentCell"></td>
              <td class="ContentCell"></td>
              <td class="ContentCell"></td>
            </tr>
          
            
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
                    onclick="getDetail()" 
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

<script type="text/javascript">onLoadDo();</script>
<script type="text/javascript" src="../web/js/jquery-1.7.2.min.js"></script>
<script type="text/javascript" src="../web/js/ui/jquery.ui.core.js"></script>
<script type="text/javascript" src="../web/js/ui/jquery.ui.widget.js"></script>
<script type="text/javascript" src="../web/js/ui/jquery.ui.position.js"></script>
<script type="text/javascript" src="../web/js/ui/jquery.ui.button.js"></script>
<script type="text/javascript" src="../web/js/jquery/grid.locale-en.js"></script>
<script type="text/javascript" src="../web/js/jquery/datejs/jquery-ui.min.js"></script>
<script type="text/javascript" src="../web/js/ui/jquery.ui.autocomplete.js"></script>  
 <script type="text/javascript" src="../web/js/common/select2.min.js"></script> 
 <script type="text/javascript">
 
 var orgId ="";
 var orgName ="";
 var accountId ="";
 var accountName ="";
 var deptId ="";
 var deptName ="";
 var subAcctId ="";
 var subAcctName ="";

 
 getParentAccount();
 function getParentAccount(){
     accountId = '<%=request.getAttribute("SelectedAccount")==null?"0": request.getAttribute("SelectedAccount")%>';
     accountName = '<%=request.getAttribute("SelectedAccountName")==null? Resource.getProperty("finance.Select",lang) : request.getAttribute("SelectedAccountName") %>';
     document.getElementById("inpAccount").value=accountId;
     setTimeout(function () {
         $("#inpAccount").select2(selectBoxAjaxPaging({
             url : function() {
                
                 return '<%=request.getContextPath()%>/sa.elm.ob.finance.ad_reports.IntegratedCostBudgetInquiry/IntegratedCostBudgetInquiry?inpAction=getParentAccount'
            },
            data: [{
                id: accountId,
                text: accountName
            }],
            size : "med/popup"
        }));
         document.getElementById("select2-inpAccount-container").style.backgroundColor="#F5F7F1";
         $("#inpAccount").on("select2:unselecting", function (e) {
             document.getElementById("inpAccount").options.length = 0;
     });
         
 }, 100);
  }
 
 // Set SubAccount Default Value
 //When Account Changed
 
 $('#inpAccount').change(function(){
   var value = $(this).val();
   $.ajax({
       type:'GET',
       url:"<%=request.getContextPath()%>/sa.elm.ob.finance.ad_reports.IntegratedCostBudgetInquiry/IntegratedCostBudgetInquiry?inpAction=getSubAccountAgainstAccount",            
       data:{inpAccountParam:document.getElementById("inpAccount").value},
       dataType:'json',
       async:false,
       success:function(response)
       {       
           if(response){
               document.getElementById("inpSubAccount").options.length = 0;
               getSubAccount();
          // $('#inpSubAccount').select2('data', {id:response.sub_account_id , text:response.name});
           if(typeof response.sub_account_id !=="undefined" ){
                   var data_set = [{
                       id: response.sub_account_id,
                        text:response.name
                      }];
                      $("#inpSubAccount").select2({
                        data: data_set
                      });
               }
           else{
               var data_set = [{
                   id:'0',
                   text:'<%=Resource.getProperty("finance.Select",lang) %>'
                 }];
                 $("#inpSubAccount").select2({
                   data: data_set
                 }); 
           }
       }
       else{
           document.getElementById("inpSubAccount").options.length = 0;
           getSubAccount();
           var data_set = [{
                id:'0',
                text:'<%=Resource.getProperty("finance.Select",lang) %>'
              }];
              $("#inpSubAccount").select2({
                data: data_set
              });
       }
       }
        
    });
   

 });
 getDepartment();
 function getDepartment(){
     deptId = '<%=request.getAttribute("SelectedDept")==null?"0": request.getAttribute("SelectedDept")%>';
     deptName = '<%=request.getAttribute("SelectedDeptName")==null? Resource.getProperty("finance.Select",lang) : request.getAttribute("SelectedDeptName") %>';
     document.getElementById("inpDept").value=deptId;
     setTimeout(function () {
         $("#inpDept").select2(selectBoxAjaxPaging({
             url : function() {
                
                 return '<%=request.getContextPath()%>/sa.elm.ob.finance.ad_reports.IntegratedCostBudgetInquiry/IntegratedCostBudgetInquiry?inpAction=getDepartment'
            },
            data: [{
                id: deptId,
                text: deptName
            }],
            size : "med/popup"
        }));
         document.getElementById("select2-inpDept-container").style.backgroundColor="#F5F7F1";
         $("#inpDept").on("select2:unselecting", function (e) {
             document.getElementById("inpDept").options.length = 0;
     });
 }, 100);
  }
 
 getSubAccount();
 function getSubAccount(){
    
     subAcctId = '<%=request.getAttribute("SelectedSubAccount")==null?"0": request.getAttribute("SelectedSubAccount")%>';
     subAcctName ='<%=request.getAttribute("SelectedSubAccountName")==null?  Resource.getProperty("finance.Select",lang) : request.getAttribute("SelectedSubAccountName")%>'; 
     document.getElementById("inpSubAccount").value=subAcctId;
     setTimeout(function () {
         $("#inpSubAccount").select2(selectBoxAjaxPaging({
             url : function() {
                
                 return '<%=request.getContextPath()%>/sa.elm.ob.finance.ad_reports.IntegratedCostBudgetInquiry/IntegratedCostBudgetInquiry?inpAction=getSubAccount&inpAccountParam='+document.getElementById("inpAccount").value
            },
            data: [{
                id: subAcctId,
                text: subAcctName
            }],
            size : "med/popup"
        }));
         document.getElementById("select2-inpSubAccount-container").style.backgroundColor="#F5F7F1";

         $("#inpSubAccount").on("select2:unselecting", function (e) {
             document.getElementById("inpSubAccount").options.length = 0;
     });
 }, 100);
  }
 
 getOrganization();
 function getOrganization(){
     orgId = '<%=request.getAttribute("SelectedOrg")==null?"0": request.getAttribute("SelectedOrg")%>';
     orgName = '<%=request.getAttribute("SelectedOrgName")==null? Resource.getProperty("finance.Select",lang) : request.getAttribute("SelectedOrgName") %>';
     document.getElementById("inpOrg").value=orgId;
     setTimeout(function () {
         $("#inpOrg").select2(selectBoxAjaxPaging({
             url : function() {
            
                
                 return '<%=request.getContextPath()%>/sa.elm.ob.finance.ad_reports.IntegratedCostBudgetInquiry/IntegratedCostBudgetInquiry?inpAction=getOrganization'
            },
            data: [{
                id: orgId,
                text: orgName
            }],
            size : "small"
        }));
         $("#inpOrg").on("select2:unselecting", function (e) {
             document.getElementById("inpOrg").options.length = 0;
     });
 }, 100);
     
 
  }
 
 
 
 
 </script>

</HTML>