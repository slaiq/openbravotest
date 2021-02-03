<!--
 *************************************************************************
 * All Rights Reserved.
 * Contributor(s): 
 *************************************************************************
-->
<%@page import="java.util.Calendar"%>
<%@ page import="sa.elm.ob.finance.properties.Resource" errorPage="/web/jsp/ErrorPage.jsp" %>
<%@page import="sa.elm.ob.finance.ad_process.RDVProcess.vo.PenaltyActionVO"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@page import="java.util.List,java.util.ArrayList"%>
 <%
    String lang = ((String)session.getAttribute("#AD_LANGUAGE"));
 String strbpartnerlist =  "0:" + Resource.getProperty("finance.Select", lang) + ";";
 String strpenaltyType =  "0:" + Resource.getProperty("finance.Select", lang) + ";";
 String strinvoicelist = "0:" + Resource.getProperty("finance.Select", lang) + ";";
 String struniquecodelist =  "0:" + Resource.getProperty("finance.Select", lang) + ";";

 String  thershold=null;
    String style="../web/skins/ltr/org.openbravo.userinterface.skin.250to300Comp/250to300Comp/Openbravo_ERP.css";
    String toolBarStyle="../web/js/common/CommonFormLtr.css";
    if(lang.equals("ar_SA")){
     style="../web/skins/rtl/org.openbravo.userinterface.skin.250to300Comp/250to300Comp/Openbravo_ERP_250.css";
    toolBarStyle="../web/js/common/CommonFormRtl.css";
    }
    %>
<HTML xmlns="http://www.w3.org/1999/xhtml">
  <HEAD>
   <META http-equiv="Content-Type" content="text/html; charset=utf-8"></META> 
    <script>
   var width=window.parent.screen.width;
   var height= window.parent.screen.height;
   window.parent.moveTo(50, 50);    
   window.parent.resizeTo(width-100, height-150); 

   </script> 
   <LINK rel="stylesheet" type="text/css" href="<%=style %>" id="paramCSS"></LINK>
   <link rel="stylesheet" type="text/css" href="<%=toolBarStyle %>"></link>
    <link rel="stylesheet"  href="../web/js/jquery/css/demos.css"></link>
    <link rel="stylesheet" type="text/css" href="../web/js/themes/base/jquery.ui.autocomplete.css"></link>
    <link rel="stylesheet" type="text/css" href="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.picker.css"></link>
    <link rel="stylesheet" type="text/css" href="../web/js/jquery/jquery-ui-1.8.11.custom.css"></link>
     <link rel="stylesheet" type="text/css" href="../web/js/jquery/ui.jqgrid.css"></link>
        <link rel="stylesheet" type="text/css" href="../web/js/common/htabs.css"></link>
     <link rel="stylesheet" type="text/css" href="../web/js/common/select2.min.css"></link>
    
    
    
        <style type="text/css">
    .grid-col {
  padding-right: 12px !important;
  padding-left:  20px !important;
}

.ui-jqgrid .ui-jqgrid-bdiv {
  position: relative; 
  margin: 0em; 
  padding:0; 
  /*overflow: auto;*/ 
  /*overflow-x:hidden; */
  overflow-y:auto; 
  text-align:left;
}
   
    //SalaryList_iladd  refresh_SalaryList  del_SalaryList del_SalaryList  SalaryList_ilsave  SalaryList_ilcancel


</style>
    <script type="text/javascript" src="../web/js/common/common.js?t=<%= Calendar.getInstance().getTimeInMillis() %>"></script>
    <script type="text/javascript" src="../web/js/utils.js"></script>
    <script type="text/javascript" src="../web/js/windowKeyboard.js"></script>
    <script type="text/javascript" src="../web/js/searchs.js"></script>
    <script type="text/javascript" id="paramDirectory">var baseDirectory = "../web/";</script>
    <script type="text/javascript" id="paramLanguage">var defaultLang="en_US";</script>
    
    <script type="text/javascript" src="../web/js/jquery-1.7.2.min.js"></script>
<script type="text/javascript" src="../web/js/ui/jquery.ui.core.js"></script>
<script type="text/javascript" src="../web/js/jquery/grid.locale-en.js"></script>
<script type="text/javascript" src="../web/js/jquery/jquery.jqGrid.min.js"></script>
<script type="text/javascript" src="../web/js/ui/jquery.ui.datepicker.js"></script>
 
    <script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.min_1.11.0.js"></script> 
    <script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.plugin.js"></script> 
    <script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.js"></script> 
    <script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.ummalqura.js"></script> 
    <script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.ummalqura-ar.js"></script> 
    <script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.plus.js"></script> 
    <script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.picker.js"></script> 
    <script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.picker.ext.js"></script>
    
    <script type="text/javascript">
    function validate(action) {return true;}
    function onLoadDo(){
        
        /*<<<<<OB Code>>>>>*/
        this.windowTables = new Array(new windowTableId('client', 'buttonOK'));
        setWindowTableParentElement();
        this.tabsTables = new Array(new tabTableId('tdtopTabs'));
        setTabTableParentElement();setBrowserAutoComplete(false);setFocusFirstControl(null, 'inpMailTemplate');
        resizeArea();updateMenuIcon('buttonMenu');onLoad();
        /*<<<<<OB Code>>>>>*/
          var calendarInstance = $.calendars.instance('ummalqura');
     
      
   
    }
    function onResizeDo(){
        resizeArea();reSizeGrid();}
    function onLoad()
    {
        <% request.setAttribute("SaveStatus",request.getAttribute("SaveStatus")==null?"":request.getAttribute("SaveStatus"));
        if(request.getAttribute("SaveStatus").equals("Add-True")) { %>displayMessage("S", "<%=Resource.getProperty("hcm.success",lang)%>","<%= Resource.getProperty("hcm.saveoreditsuccess",lang) %>");<% }
        else if(request.getAttribute("SaveStatus").equals("Update-True")) { %>displayMessage("S", "<%=Resource.getProperty("hcm.success",lang)%>","<%= Resource.getProperty("hcm.saveoreditsuccess",lang) %>");<% }
        else if(request.getAttribute("SaveStatus").equals("False")) { if(request.getAttribute("ErrorMsg")!=null){%>displayMessage("E", "<%=Resource.getProperty("hcm.error",lang)%>","<%= (String)request.getAttribute("ErrorMsg") %>") <%}else{%>displayMessage("E", "<%=Resource.getProperty("hcm.error",lang)%>","<%= Resource.getProperty("hcm.saveorediterror",lang) %>");<% }} %>
    }
    </script>
</HEAD>
<BODY onresize="onResizeDo();" leftmargin="0" topmargin="0" marginwidth="0" marginheight="0" id="paramBody" onunload="refreshParent();">
    <FORM id="form" method="post" action="" name="frmMain">
        <INPUT type="hidden" name="Command"></INPUT>
        <INPUT type="hidden" name="IsPopUpCall" value="1"></INPUT>
        <INPUT type="hidden" name="inpLastFieldChanged"></INPUT>
        <INPUT type="hidden" name="inpAction" id="inpAction" value=""></INPUT>
        <INPUT type="hidden" name="SubmitType" id="SubmitType" value=""></INPUT>
        <INPUT type="hidden" name="inpRDVTxnLineId" id="inpRDVTxnLineId" value="<%= request.getAttribute("inpRDVTxnLineId") %>"></INPUT>
          <INPUT type="hidden" name="inpbpartnername" id="inpbpartnername" value="<%= request.getAttribute("inpbpartnername") %>"></INPUT>
        <INPUT type="hidden" name="inptrxappNo" id="inptrxappNo" value="<%= request.getAttribute("inptrxappNo") %>"></INPUT>
        <INPUT type="hidden" name="inplineno" id="inplineno" value="<%= request.getAttribute("inplineno") %>"></INPUT>
         <INPUT type="hidden" name="inpmatamt" id="inpmatamt" value="<%= request.getAttribute("inpmatamt") %>"></INPUT>
         <INPUT type="hidden" name="inpnetamt" id="inpnetamt" value="<%= request.getAttribute("inpnetamt") %>"></INPUT>
         <INPUT type="hidden" name="inppenaltytype" id="inppenaltytype" value="<%= request.getAttribute("inppenaltytype") %>"></INPUT> 
          <INPUT type="hidden" name="inppenaltyThershold" id="inppenaltyThershold" value=""></INPUT>   
          <INPUT type="hidden" name="inptodaydate" id="inptodaydate" value="<%= request.getAttribute("today") %>"></INPUT> 
          <INPUT type="hidden" name="inpinvoiceNo" id="inpinvoiceNo" value=""></INPUT>  
                    <INPUT type="hidden" name="inpuniquecode" id="inpuniquecode" value="<%= request.getAttribute("inpuniquecode") %>"></INPUT>
                      <INPUT type="hidden" name="inplineuniquecode" id="inplineuniquecode" value="<%= request.getAttribute("inplineuniquecode") %>"></INPUT>
                      <INPUT type="hidden" name="inplineuniquecodeId" id="inplineuniquecodeId" value="<%= request.getAttribute("inplineuniquecodeId") %>"></INPUT>
              <INPUT type="hidden" name="inplineuniquecodeName" id="inplineuniquecodeName" value="<%= request.getAttribute("inplineuniquecodeName") %>"></INPUT>
           <INPUT type="hidden" name="inprdvtrxtype" id="inprdvtrxtype" value="<%= request.getAttribute("inprdvtrxtype") %>"></INPUT>
                     <INPUT type="hidden" name="inpverstatus" id="inpverstatus" value="<%= request.getAttribute("inpverstatus") %>"></INPUT>
          
         <input type="hidden" name = "inpDefaultPenaltyAcctType" id="inpDefaultPenaltyAcctType" value="<%= request.getAttribute("inpPenaltyAccountType") %>"></input>   
        <jsp:include page="/web/jsp/ProcessBar.jsp"/>
     <TABLE cellspacing="0" cellpadding="0" width="100%" id="table_header">
  <TR>
    <TD>
      <TABLE cellspacing="0" cellpadding="0" class="Popup_ContentPane_NavBar" id="tdToolBar">
        <TR class="Popup_NavBar_bg"><TD></TD>
          <TD class="Popup_NavBar_separator_cell"></TD>
          <TD class="Popup_NavBar_Popup_title_cell"><SPAN><%=Resource.getProperty("finance.penalty.action",lang)%></SPAN></TD>
          <TD class="Popup_NavBar_separator_cell"></TD>
        </TR>
      </TABLE>
    </TD>
    <div class="Popup_ContentPane_CircleLogo">
      <div class="Popup_WindowLogo">
        <img class="Popup_WindowLogo_Icon Popup_WindowLogo_Icon_process" src="../web/images/blank.gif" border="0/">
      </div>
    </div>
  </TR>
  <TR>
    <TD>
      <TABLE cellspacing="0" cellpadding="0" class="Popup_ContentPane_SeparatorBar" id="tdtopTabs">
        <TR>
          <TD class="Popup_SeparatorBar_bg"></TD>
        </TR>
      </TABLE>
    </TD>
  </TR>
  <tr>
  <td>
     <table cellspacing="0" cellpadding="0" class="Popup_ContentPane_InfoBar">
        <tbody><tr>
          <td class="Popup_InfoBar_Icon_cell"><img src="../web/images/blank.gif" border="0" class="Popup_InfoBar_Icon_info"></td>
          <td class="Popup_InfoBar_text_table">
            <table>
              <tbody><tr>
                <td class="Popup_InfoBar_text" id="processHelp"><%=Resource.getProperty("finance.penalty.action",lang)%> </td>
              </tr>
            </tbody></table>
          </td>
        </tr>
      </tbody></table>
    </td>
</tr>
</TABLE>
<TABLE cellspacing="0" cellpadding="0" width="100%">
      <TR>
       
            
        <TD valign="top">
         <DIV class="Main_ContentPane_NavBar" id="tdtopButtons"><TABLE class="Main_ContentPane_NavBar" id="tdtopNavButtons"></TABLE></DIV>
                    <DIV class="Main_ContentPane_Client" style="overflow: auto;" id="client">
                        <TABLE class="dojoTabContainer" border="0" cellpadding="0" cellspacing="0" width="100%" summary="">
                            <TR><TD>
                                <TABLE class="dojoTabPaneWrapper" border="0" cellpadding="0" cellspacing="0" width="100%" summary="">
                                    <TR><TD>
                                        <TABLE cellpadding="0" cellspacing="0" id="messageBoxID" class="MessageBox">    
                                            <TBODY>
                                                <TR class="MessageBox_TopMargin">
                                                    <TD class="MessageBox_LeftMargin"></TD>
                                                    <TD></TD>
                                                    <TD class="MessageBox_RightMargin"></TD>
                                                </TR>
                                                <TR>
                                                    <TD class="MessageBox_LeftMargin"></TD>
                                                    <TD>
                                                        <TABLE cellpadding="0" cellspacing="0" class="MessageBox_Container">
                                                        <TBODY>
                                                            <TR>
                                                                <TD class="MessageBox_LeftTrans">
                                                                    <TABLE style="width: 100%; height: 100%" cellpadding="0" cellspacing="0">
                                                                        <TBODY>
                                                                            <TR><TD class="MessageBox_TopLeft"></TD></TR>
                                                                            <TR><TD class="MessageBox_Left"></TD></TR>
                                                                        </TBODY>
                                                                    </TABLE>
                                                                </TD>
                                                                <TD class="MessageBox_bg">
                                                                    <TABLE class="MessageBox_Top"><TBODY>
                                                                        <TR><TD>
                                                                            <SPAN>
                                                                                <TABLE cellpadding="0" cellspacing="0" class="MessageBox_Body_ContentCell">
                                                                                    <TBODY>
                                                                                        <TR>
                                                                                            <TD class="MessageBox_Icon_ContentCell">
                                                                                                <DIV class="MessageBox_Icon"></DIV>
                                                                                            </TD>
                                                                                            <TD style="vertical-align: top;" id="messageBoxIDContent">
                                                                                                <SPAN>
                                                                                                    <DIV class="MessageBox_TextTitle" id="messageBoxIDTitle"></DIV>
                                                                                                    <DIV class="MessageBox_TextDescription" id="messageBoxIDMessage"></DIV>
                                                                                                    <DIV class="MessageBox_TextSeparator"></DIV>
                                                                                                </SPAN>
                                                                                            </TD>
                                                                                        </TR>
                                                                                    </TBODY>
                                                                                </TABLE>
                                                                            </SPAN>
                                                                            
                                                                            <div id="hideMessage" style="float: right; margin-top:-13px; margin-right:10px;">
                                                                                 <a style="color: yellow;font-size: 11.5px" href="javascript:void(0);" onClick="document.getElementById('messageBoxID').style.display = 'none';"><%=Resource.getProperty("hcm.hide",lang)%></a>
                                                                            </div>
                                                                     </TD></TR>
                                                                     </TBODY></TABLE>
                                                                </TD>
                                                                <TD class="MessageBox_RightTrans">
                                                                    <TABLE style="width: 100%; height: 100%" cellpadding="0" cellspacing="0">
                                                                        <TBODY>
                                                                            <TR><TD class="MessageBox_TopRight"></TD></TR>
                                                                            <TR><TD class="MessageBox_Right"></TD></TR>
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
                                                    <TD class="MessageBox_RightMargin">
                                                    </TD>
                                                </TR>
                                            </TBODY>
                                        </TABLE>
                                        
                                        <div id="LoadingContent" style="display:none;">
                                            <div style="text-align: center;margin-top: 17%;"><img alt="Loading" src="../web/images/loading.gif"></div>
                                            <div style="color:#000000;font-family: sans-serif;font-size:14px;margin-top:10px;text-align: center;"><%= Resource.getProperty("finance.loading", lang)%>... </div>
                                        </div>
                                        
                                        <div style="width:100%;">
                                            <div style="padding: 0 1%; width: 98%;">
                                                <div style="width:100%;" id="FormDetails" class="readonly">
                                              
                                                
                                                    <div align="center">
                                                        <TABLE style="width:80%; margin-top: 10px;">
                                                         
                                                          <TR>
                                                            <td colspan="3" rowspan="5">
                                                                <DIV id="SalaryGroup"  style="display:none;">
                                                                <table style="width: 100%;padding: 2px;">
                                                                <tr>
                                                                <td>
                                                                 <TABLE class="FieldGroup" cellspacing="0" cellpadding="0" border="0">
                                                                  <TBODY>
                                                                   <TR class="FieldGroup_TopMargin"></TR>
                                                                   <TR>
                                                                       <TD class="FieldGroupTitle_Left"><IMG class="FieldGroupTitle_Left_bg" border="0" src="../web/images/blank.gif"></IMG></TD>
                                                                       <TD class="FieldGroupTitle" id="subGridTitle"><%= Resource.getProperty("finance.salaryDetails", lang) %></TD>
                                                                       <TD class="FieldGroupTitle_Right"><IMG class="FieldGroupTitle_Right_bg" border="0" src="../web/images/blank.gif"></IMG></TD>
                                                                       <TD class="FieldGroupContent"></TD>
                                                                   </TR>    
                                                                   <TR class="FieldGroup_BottomMargin" style="display: none;"></TR>          
                                                                  </TBODY>
                                                                  </TABLE>
                                                                   </td>
                                                                  </tr>
                                                               </table>
                                                               </DIV>
                                                                <div id="jqgrid" style="width:100%; ">
                                                                   <div align="center"><table id="SalaryList" class="scroll" cellpadding="0" cellspacing="0" width="100%"></table><div id="pager" class="scroll" style="text-align: center;"></div></div>
                                                                </div>
                                                            </td>
                                                            </TR>

                                                       </TABLE>
                                                       </div>
                                                      
                                                </div>
                                            </div>
                                        </div>
                                    </TD></TR>
                                </TABLE>
                            </TD></TR>
                        </TABLE>
                    </DIV>  
                 
                      </TD>
              
                   
                    </TR>
                   
             </TABLE>  
    </FORM> 
</BODY>
<script type="text/javascript">onLoadDo();</script>
<script type="text/javascript">

var currentWindow='EMP',contextPath = '<%=request.getContextPath()%>';

 <% List<PenaltyActionVO> inpbank = (ArrayList<PenaltyActionVO>)  request.getAttribute("inpbpartnername");

//strbanklist = "{";
 /*for (PenaltyActionVO vo : inpbank){
    strbpartnerlist += vo.getBpartnerid() + ":" +  vo.getBpartnername() + ";";
} */
// strbanklist += "}";
 %> 

document.getElementById("inpbpartnername").value="<%=strbpartnerlist.substring(0,strbpartnerlist.lastIndexOf(";"))%>";

<% List<PenaltyActionVO> inppenaltyType = (ArrayList<PenaltyActionVO>)  request.getAttribute("inppenaltytype");

//strbanklist = "{";
for (PenaltyActionVO vo : inppenaltyType){
    if(thershold==null)
    thershold = vo.getThershold();
strpenaltyType += vo.getPenaltyId() + ":" +  vo.getPenaltyname() + ";";
}
//strbanklist += "}";
%>

<% List<PenaltyActionVO> inpinvoice = (ArrayList<PenaltyActionVO>)  request.getAttribute("inpinvoiceNo");
//strbanklist = "{";
for (PenaltyActionVO vo : inpinvoice){
strinvoicelist += vo.getInvoiceId() + ":" +  vo.getInvoicedocumentno() + ";";
}
//strbanklist += "}";
%>
document.getElementById("inpinvoiceNo").value="<%=strinvoicelist.substring(0,strinvoicelist.lastIndexOf(";"))%>";


 <% List<PenaltyActionVO> inpuniquecode = (ArrayList<PenaltyActionVO>)  request.getAttribute("inpuniquecode"); 
//strbanklist = "{";
/*for (PenaltyActionVO vo : inpuniquecode){
    struniquecodelist += vo.getCombId() + ":" +  vo.getCombUniquecode() + ";";

}*/
//strbanklist += "}";
 %> 
document.getElementById("inpuniquecode").value="<%=struniquecodelist.substring(0,struniquecodelist.lastIndexOf(";"))%>";

document.getElementById("inppenaltytype").value="<%=strpenaltyType.substring(0,strpenaltyType.lastIndexOf(";"))%>";
document.getElementById("inppenaltyThershold").value="<%=thershold%>";
var Removenegative='<%= Resource.getProperty("finance.penalty.action.remove.negative", lang) %>';
var addpositive='<%= Resource.getProperty("finance.penalty.action.add.positive", lang) %>';
var askSave='<%= Resource.getProperty("finance.penalty.action.askSave", lang) %>';
var askDelete='<%= Resource.getProperty("finance.penalty.action.askDelete", lang) %>';
var bpmandatory='<%= Resource.getProperty("finance.penalty.action.associatedbp.mandatory", lang) %>';
var penaltyamtshouldnotnegrdv='<%= Resource.getProperty("finance.penalty.action.penaltyamount.shouldnot.negative", lang) %>';
var penamtgreterthanmatchamt='<%= Resource.getProperty("finance.penalty.action.penaltyamt.greater.than.matchamt", lang) %>';
var penamtshouldnotneg='<%= Resource.getProperty("finance.penalty.type.should.mandatory", lang) %>';
var select ='<%= Resource.getProperty("finance.Select", lang) %>';
var valueschanged ='<%= Resource.getProperty("finance.penalty.valueschanged", lang) %>';
var versionstatusinvoice='<%= Resource.getProperty("finance.penalty.version.status.invoiced", lang) %>';
var penaltyamtnotblank  = '<%= Resource.getProperty("finance.penalty.penaltyamount.not.blank", lang) %>';
var uniquecodemandatory='<%= Resource.getProperty("finance.penalty.action.uniquecode.mandatory", lang) %>';
var select='<%= Resource.getProperty("finance.Select", lang) %>';
var sequence='<%= Resource.getProperty("finance.penalty.action.sequence", lang) %>';
var action='<%= Resource.getProperty("finance.penalty.action.action", lang) %>';
var txnAppNo='<%= Resource.getProperty("finance.penalty.action.txn.application.no", lang) %>';
var action='<%= Resource.getProperty("finance.penalty.action.action", lang) %>';
var actionDate='<%= Resource.getProperty("finance.penalty.action.action.date", lang) %>';
var matchAmt='<%= Resource.getProperty("finance.penalty.action.match.amt", lang) %>';
var penaltyTypes='<%= Resource.getProperty("finance.penalty.action.penalty.type", lang) %>';
var penaltyPercent='<%= Resource.getProperty("finance.penalty.action.penalty.percentage", lang) %>';
var penaltyAmount='<%= Resource.getProperty("finance.penalty.action.penalty.amount", lang) %>';
var actionReason='<%= Resource.getProperty("finance.penalty.action.action.reason", lang) %>';
var actionJustification='<%= Resource.getProperty("finance.penalty.action.action.justification", lang) %>';
var asociatedBp='<%= Resource.getProperty("finance.penalty.action.associated.bp", lang) %>';
var bpName='<%= Resource.getProperty("finance.penalty.action.bp.name", lang) %>';
var freezePenalty='<%= Resource.getProperty("finance.penalty.action.freeze.penalty", lang) %>';
var amrasafNo='<%= Resource.getProperty("finance.penalty.action.amrasaf.no", lang) %>';
var amrasafAmt='<%= Resource.getProperty("finance.penalty.action.amrasaf.amount", lang) %>';
var deductionAcctType='<%= Resource.getProperty("finance.penalty.action.penalty.deduction.acct.type", lang) %>';
var uniquecode='<%= Resource.getProperty("finance.penalty.action.penalty.uniquecode", lang) %>';
var uniquecodeName='<%= Resource.getProperty("finance.penalty.action.penalty.uniquecodename", lang) %>';
var penaltyAmtNotZero='<%= Resource.getProperty("finance.penalty.amount.should.not.zero", lang) %>';
var add='<%= Resource.getProperty("finance.penalty.add", lang) %>';
var remove='<%= Resource.getProperty("finance.penalty.remove", lang) %>';
var edit='<%= Resource.getProperty("finance.penalty.edit", lang) %>';
var save='<%= Resource.getProperty("finance.penalty.save", lang) %>';
var cancel='<%= Resource.getProperty("finance.penalty.cancel", lang) %>';
var reload='<%= Resource.getProperty("finance.penalty.reload", lang) %>';
var deleteicon='<%= Resource.getProperty("finance.penalty.delete", lang) %>';
var expense='<%= Resource.getProperty("finance.penalty.expense", lang) %>';
var adjustment='<%= Resource.getProperty("finance.penalty.adjustment", lang) %>';
var penaltyAmtGreater='<%= Resource.getProperty("finance.penalty.penatyamtgreater", lang) %>';

</script>
<script type="text/javascript" src="../web/js/jquery-1.7.2.min.js"></script>
<script type="text/javascript" src="../web/js/ui/jquery.ui.core.js"></script>
<script type="text/javascript" src="../web/js/jquery/grid.locale-en.js"></script>
<script type="text/javascript" src="../web/js/ui/jquery.ui.widget.js"></script>
<script type="text/javascript" src="../web/js/ui/jquery.ui.mouse.js"></script>
<script type="text/javascript" src="../web/js/ui/jquery.ui.button.js"></script>
<script type="text/javascript" src="../web/js/ui/jquery.ui.position.js"></script>
<script type="text/javascript" src="../web/js/ui/jquery.ui.autocomplete.js"></script>
<script type="text/javascript" src="../web/sa.elm.ob.finance/js/PenaltyAction/PenaltyAction.js"></script>
<script type="text/javascript" src="../web/js/inlineNavGrid.js"></script>  
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.plugin.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.ummalqura.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.ummalqura-ar.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.plus.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.picker.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.picker.ext.js"></script>
<script type="text/javascript" src="../web/js/common/select2.min.js"></script>
</HTML>