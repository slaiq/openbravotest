<%@page import="org.openbravo.base.secureApp.VariablesSecureApp"%>
<%@page import="org.codehaus.jettison.json.JSONObject, org.codehaus.jettison.json.JSONArray"%>
<%@page import="sa.elm.ob.scm.ad_reports.CustodyCardReport.CustodyCardReportVO"%>
<%@page import="sa.elm.ob.utility.util.Utility"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
    <%@page import="sa.elm.ob.scm.properties.Resource" errorPage="/web/jsp/ErrorPage.jsp" %>
    <%@ page import="java.util.List,java.util.ArrayList" %>
    <%
    String lang = ((String)session.getAttribute("#AD_LANGUAGE"));
    JSONObject benfList = (JSONObject) request.getAttribute("BeneficiaryTypeList");    
    JSONObject mirList = (JSONObject) request.getAttribute("MaterialIssueRequests");
    //JSONObject tagsList = (JSONObject) request.getAttribute("TagsList"); 
    
    JSONArray bnfTypeArray=benfList.getJSONArray("data");
    JSONArray mirArray=mirList.getJSONArray("RequestNos");
    //JSONArray tagArray=tagsList.getJSONArray("data"); 
    
    String style="../web/skins/ltr/org.openbravo.userinterface.skin.250to300Comp/250to300Comp/Openbravo_ERP.css";
    String toolBarStyle="../web/js/common/CommonFormLtr.css";
    if(lang.equals("ar_SA")){
     style="../web/skins/rtl/org.openbravo.userinterface.skin.250to300Comp/250to300Comp/Openbravo_ERP_250.css";
    toolBarStyle="../web/js/common/CommonFormRtl.css";
    }
    %>
<fmt:setLocale value="<%=new VariablesSecureApp(request).getLanguage()%>"/> 
<fmt:setBundle basename="sa.elm.ob.utility.properties.applicationresources"/>

<HTML xmlns="http://www.w3.org/1999/xhtml">
<HEAD>
<TITLE><%= Resource.getProperty("scm.custodybarcodelabel.report.window.title",lang) %></TITLE>
<META http-equiv="Content-Type" content="text/html; charset=utf-8"></META> 
<LINK rel="stylesheet" type="text/css" href="<%=style %>" id="paramCSS"></LINK>
<link rel="stylesheet" type="text/css" href="../web/js/jquery/jquery-ui-1.8.11.custom.css"></link>
<link rel="stylesheet" type="text/css" href="<%=toolBarStyle %>"></link>     
 <link rel="stylesheet" type="text/css" href="../web/js/common/select2.min.css"></link>
   
<script type="text/javascript" src="../web/js/common/common.js"></script>
<script type="text/javascript" src="../web/js/utils.js"></script>
<script type="text/javascript" src="../web/js/windowKeyboard.js"></script>
<script type="text/javascript" src="../web/js/searchs.js"></script>
<script type="text/javascript" id="paramDirectory">var baseDirectory = "../web/";</script>
<script type="text/javascript" id="paramLanguage">var defaultLang="en_US";</script>
<script type="text/javascript" src="../web/js/default/DateTextBox.js"></script>
<script type="text/javascript" src="../web/js/jscalendar/calendar.js"></script>
<script type="text/javascript" src="../web/js/jscalendar/lang/calendar-lang.js"></script>  
 
<style type="text/css">
  .ui-autocomplete-input {height: 21px; border-radius:0px !important; width:252px;font-size: 12px; padding: 0 0 0 3px; float: left;border:1px solid #CDD7BB;}
   .ui-button-icon-primary{left: .9em !important;} 
    ul{border:1px solid #FF9C30 !important;}        
</style>               

<script type="text/javascript">
function validate(action) {
    return true;
}
function onLoadDo() {
    /*<<<<<OB Code>>>>>*/
    this.windowTables = new Array(new windowTableId('client', 'buttonOK'));
    setWindowTableParentElement();
    this.tabsTables = new Array(new tabTableId('tdtopTabs'));
    setTabTableParentElement();
    setBrowserAutoComplete(false);
    setFocusFirstControl(null, 'inpMailTemplate');
    resizeArea();    
    updateMenuIcon('buttonMenu');
    /*<<<<<OB Code>>>>>*/
}
function onResizeDo() {
    resizeArea();
}
</script>
</HEAD>
   <BODY onresize="onResizeDo();" leftmargin="0" topmargin="0" marginwidth="0" marginheight="0" id="paramBody">
      <FORM id="form" method="post" action="" name="frmMain">
         <INPUT type="hidden" name="Command"></INPUT>
         <INPUT type="hidden" name="IsPopUpCall" value="1"></INPUT>
         <INPUT type="hidden" name="inpLastFieldChanged"></INPUT>
         <INPUT type="hidden" name="inpAction" id="inpAction" value=""></INPUT>
         <INPUT type="hidden" name="inpTagFromNo" id="inpTagFromNo" value=""></INPUT>
         <INPUT type="hidden" name="inpTagToNo" id="inpTagToNo" value=""></INPUT>
         <INPUT type="hidden" name="inpMIRid" id="inpMIRid" value=""></INPUT>         
         <INPUT type="hidden" name="inpBeneficiaryid" id="inpBeneficiaryid" value=""></INPUT>
         <INPUT type="hidden" name="inpBeneficiarytype" id="inpBeneficiarytype" value=""></INPUT>
         <TABLE height="100%" border="0" cellpadding="0" cellspacing="0" id="main">
            <TR>
               <TD valign="top">
                  <DIV class="Main_ContentPane_NavBar" id="tdtopButtons">
                     <TABLE class="Main_ContentPane_NavBar" id="tdtopNavButtons"></TABLE>
                  </DIV>
                   </TD>
           <TD valign="top">
               <TABLE width="100%" border="0" cellspacing="0" cellpadding="0" class="Main_ContentPane_Left">
                 <TR><TD class="Main_NavBa r_bg_left" id="tdleftSeparator"></TD></TR>
                 <TR><TD class="Main_ToolBar_bg_left" valign="top"></TD></TR>
                 <TR><TD class="Main_Client_bg_left"></TD></TR>
                 <TR><TD class="Main_Bottom_bg_left"></TD></TR>
               </TABLE>
           </TD> 
           <TD valign="top">
                  <DIV class="Main_ContentPane_ToolBar" id="paramToolBar">
                     <table class="Main_ContentPane_ToolBar Main_ToolBar_bg"
                        id="tdToolBar">
                        <tr>
                           <td class="Main_ToolBar_Separator_cell"><img
                              src="../web/images/blank.gif" class="Main_ToolBar_Separator"></img></td>
                           <td width="2%"><a href="javascript:void(0);"
                              onClick="onClickRefresh()" class="Main_ToolBar_Button"
                              onMouseOver="window.status='Reload Current Page';return true;"
                              onMouseOut="window.status='';return true;"
                              onclick="this.hideFocus=true" onblur="this.hideFocus=false"
                              id="buttonRefresh"><img
                              class="Main_ToolBar_Button_Icon Main_ToolBar_Button_Icon_Refresh"
                              src="../web/images/blank.gif" title="finance.reload"
                              border="0" id="linkButtonRefresh"></a></td>
                           <td class="Main_ToolBar_Separator_cell"><img
                              src="../web/images/blank.gif" class="Main_ToolBar_Separator"></img></td>
                           <td class="Main_ToolBar_Space"></td>
                        </tr>
                     </table>
                  </DIV>
                  <TABLE width="100%" border="0" cellspacing="0" cellpadding="0" class="Main_ContentPane_TabPane" id="tdtopTabs">
                     <TR id="paramParentTabContainer">
                        <td class="tabBackGroundInit"></td>
                     </TR>
                     <TR id="paramMainTabContainer">
                        <td class="tabBackGroundInit"><span style="background: none;" class="tabTitle"></span></td>
                     </TR>
                     <tr>
                        <td class="tabBackGround">
                           <div class="marginLeft">
                              <div>
                                 <span class="dojoTabcurrentfirst">
                                    <div>
                                       <span><a class="dojoTabLink" href="javascript:void(0);" onclick="return false;" onMouseOver="return true;" onMouseOut="return true;">
                                       <%= Resource.getProperty("scm.custodybarcodelabel.report.window.title",lang).replace("'", "\\\'") %>
                                       </a></span>
                                    </div>
                                 </span>
                              </div>
                           </div>
                        </td>
                     </tr>
                  </TABLE>
                  <DIV class="Main_ContentPane_Client" style="overflow: auto; display: none;" id="client">
                     <TABLE class="dojoTabContainer" border="0" cellpadding="0" cellspacing="0" width="100%" >
                        <TR>
                           <TD>
                              <TABLE class="dojoTabPaneWrapper" align="right" border="0" cellpadding="0" cellspacing="0" width="100%" summary="">
                                 <TR>
                                    <TD>
                                       <TABLE cellpadding="0" cellspacing="0" id="messageBoxID" class="MessageBox">
                                          <TBODY>
                                             <TR class="MessageBox_TopMargin" align="right">
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
                                                                     <TR>
                                                                        <TD class="MessageBox_TopLeft"></TD>
                                                                     </TR>
                                                                     <TR>
                                                                        <TD class="MessageBox_Left"></TD>
                                                                     </TR>
                                                                  </TBODY>
                                                               </TABLE>
                                                            </TD>
                                                            <TD class="MessageBox_bg">
                                                               <TABLE class="MessageBox_Top">
                                                                  <TBODY>
                                                                     <TR>
                                                                        <TD>
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
                                                                              <a style="color: yellow;font-size: 11.5px" href="javascript:void(0);" onClick="document.getElementById('messageBoxID').style.display = 'none';"><%= Resource.getProperty("finance.hide",lang) %></a>
                                                                           </div>
                                                                        </TD>
                                                                     </TR>
                                                                  </TBODY>
                                                               </TABLE>
                                                            </TD>
                                                            <TD class="MessageBox_RightTrans">
                                                               <TABLE style="width: 100%; height: 100%" cellpadding="0" cellspacing="0">
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
                                                <TD class="MessageBox_RightMargin">
                                                </TD>
                                             </TR>
                                          </TBODY>
                                       </TABLE>
                                       <div id="LoadingContent" style="display:none;">
                                          <div style="text-align: center;margin-top: 17%;"><img alt="Loading" src="../web/images/loading.gif"></div>
                                          <div style="color:#000000;font-family: sans-serif;font-size:14px;margin-top:10px;text-align: center;"><%= Resource.getProperty("scm.loading",lang).replace("'", "\\\'") %> </div>
                                       </div>
                                       <TABLE  align="center" width="100%">
                                          <TR>
                                             <TD>&nbsp;</TD>
                                          </TR>
                                          <tr>
                                             <td class="TitleCell"><span class="LabelText" id="inpMIRIdLabel"> <b> <%=Resource.getProperty("scm.custodybarcodelabel.field.label.mir", lang).replace("'", "\\\'")%>
                                                </b>
                                                </span>
                                             </td>
                                             <td class="TextBox_ContentCell">
                                                <select id="inpMIRId" class="ComboKey Combo_TwoCells_width" onchange="disableTags(this.value)">
                                                   <option value="0">--select--</option>
                                                   <%   if(mirArray.length() > 0){
                                                        for (int i = 0, size = mirArray.length(); i < size; i++){
                                                           JSONObject objectInArray = mirArray.getJSONObject(i);%>
                                                    <option value='<%= objectInArray.get("Id")%>' ><span><%= objectInArray.get("MIRNo")+"-"+ objectInArray.get("Beneficiary") %></span></option>
                                                   <%}}%>
                                                </select>
                                             </td>
                                          </tr>
                                          <tr>
                                             <td class="TitleCell"><span class="LabelText" id="inpTagFromLabel"> <b> <%=Resource.getProperty("scm.custodybarcodelabel.field.label.tagfrom", lang).replace("'", "\\\'")%>
                                                </b>
                                                </span>
                                             </td>
                                             <td class="TextBox_ContentCell">
                                                <select id="inpTagFrom" class="ComboKey Combo_TwoCells_width">
                                                   <option value="0"><span>--select--</span></option>
                                                     <%-- <%   if(tagArray.length() > 0){
                                                      System.out.println("tagsln>"+tagArray.length());
                                                        for (int i = 0, size = tagArray.length(); i < size; i++){
                                                           JSONObject objectInArray = tagArray.getJSONObject(i);%>
                                                    <option value='<%= objectInArray.get("custodyId")%>' ><span><%= objectInArray.get("tagNo")%></span></option>
                                                   <%}}%> --%> 
                                                </select>
                                             </td>
                                          </tr>
                                          <tr>
                                             <td class="TitleCell"><span class="LabelText" id="inpTagToLabel"> <b> <%=Resource.getProperty("scm.custodybarcodelabel.field.label.tagto", lang).replace("'", "\\\'")%>
                                                </b>
                                                </span>
                                             </td>
                                             <td class="TextBox_ContentCell">
                                                <select id="inpTagTo" class="ComboKey Combo_TwoCells_width">
                                                   <option value="0">--select--</option>
                                                     <%-- <%   if(tagArray.length() > 0){
                                                        for (int i = 0, size = tagArray.length(); i < size; i++){
                                                           JSONObject objectInArray = tagArray.getJSONObject(i);%>
                                                    <option value='<%= objectInArray.get("custodyId")%>' ><span><%= objectInArray.get("tagNo")%></span></option>
                                                   <%}}%> --%>  
                                                </select>
                                             </td>
                                          </tr>
                                          <tr>
                                             <td class="TitleCell"><span class="LabelText" id="inpBeneficiaryTypeLabel"> <b> <%=Resource.getProperty("scm.custodycard.Beneficiary.type", lang).replace("'", "\\\'")%>
                                                </b>
                                                </span>
                                             </td>
                                             <td class="TextBox_ContentCell">
                                                <select id="inpBeneficiaryType" class="ComboKey Combo_TwoCells_width" onchange="getbeneficiary(this.value)">
                                                <option value="0">--select--</option>
                                                   <%   if(bnfTypeArray.length() > 0){
                                                        for (int i = 0, size = bnfTypeArray.length(); i < size; i++){
                                                           JSONObject objectInArray = bnfTypeArray.getJSONObject(i);%>
                                                    <option value='<%= objectInArray.get("benftypevalue")%>' ><span><%= objectInArray.get("benftypename")%></span></option>
                                                   <%}}%>
                                                </select>
                                             </td>
                                          </tr>
                                          <tr>
                                             <td class="TitleCell"><span class="LabelText" id="inpBeneficiaryIdLabel"> <b> <%=Resource.getProperty("scm.custodycard.Beneficiary.idname", lang).replace("'", "\\\'")%>
                                                </b>
                                                </span>
                                             </td>
                                             <td class="TextBox_ContentCell">
                                                <select id="inpBeneficiaryId" class="ComboKey Combo_TwoCells_width">
                                                   <option value="0">--select--</option>
                                                </select>
                                             </td>
                                          </tr>
                                          <TR  align = "center">
                                             <td></td>
                                             <TD  id="submitButton">
                                                <BUTTON type="button" id="Submit_linkBTN" class="ButtonLink" onclick="generateReport();" style="margin: 10px 0;">
                                                   <TABLE class="Button">
                                                      <TR>
                                                         <TD class="Button_left"><IMG class="Button_Icon Button_Icon_process" alt="Submit" title="Submit" src="../web/images/blank.gif" border="0"></IMG></TD>
                                                         <TD class="Button_text" id="Submit_BTNname">
                                                            <%= Resource.getProperty("scm.printreport.submit",lang).replace("'", "\\\'") %>
                                                         </TD>
                                                         <TD class="Button_right"></TD>
                                                      </TR>
                                                   </TABLE>
                                                </BUTTON>
                                             </TD>
                                          </TR>
                                       </TABLE>
                                    </TD>
                                 </TR>
                              </TABLE>
                           </TD>
                        </TR>
                     </TABLE>
                  </DIV>
                  <TABLE width="100%" border="0" cellspacing="0" cellpadding="0" class="Menu_ContentPane_Bottom" id="tdbottomSeparator">
                 <TR>
                   <TD class="Main_Bottom_bg"><IMG src="../web/images/blank.gif" border="0"></IMG></TD>
                 </TR>
               </TABLE>
           </TD>
               <TD valign="top">
                   <TABLE width="100%" border="0" cellspacing="0" cellpadding="0" class="Main_ContentPane_Right" id="tdrightSeparator">
                     <TR><TD class="Main_NavBar_bg_right"></TD></TR>
                     <TR><TD class="Main_ToolBar_bg_right" valign="top"></TD></TR>
                     <TR><TD class="Main_Client_bg_right"></TD></TR>
                     <TR><TD class="Main_Bottom_bg_right"></TD></TR>
                   </TABLE>
               </TD>
            </TR>
         </TABLE>
      </FORM>
   </BODY>
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

$( document ).ready(function() {
	getFromTagList();
	getToTagList();  
});
function reloadWindow() {
    submitCommandForm('DEFAULT', false, null, 'CustodyBarcodeLabel', '_self', null, false);
    return false;
}
function onClickRefresh() {
    document.getElementById("inpAction").value = "";
    reloadWindow();
}
 //Process to call the respective report.
function generateReport() {
    if (($("#inpBeneficiaryType").val() == 0 && $("#inpBeneficiaryId").val() != 0)
            || ($("#inpBeneficiaryType").val() != 0 && ($("#inpBeneficiaryId").val() == 0 || $("#inpBeneficiaryId").val()=='undefined'))) {
        OBAlert('<%=Resource.getProperty("scm.custodybarcodelabel.selectbenftypeandbenfid",lang).replace("'","\\\'") %>');
        return false;
    }
    else if(($("#inpTagFrom").val()==0 && $("#inpTagTo").val()!=0) || ($("#inpTagFrom").val()!=0 && $("#inpTagTo").val()==0)){
        OBAlert('<%=Resource.getProperty("scm.custodybarcodelabel.selecttagfromandtagto",lang).replace("'","\\\'") %>');
        return false;
    }
    if ($("#inpMIRId").val() == 0 && $("#inpBeneficiaryType").val() == 0 && $("#inpTagFrom").val() == 0){
        OBAlert('<%=Resource.getProperty("scm.custodybarcodelabel.selecteithertagormirorbenf",lang).replace("'","\\\'") %>');
        return false;
    }
    if($("#inpTagFrom").val()!=0 && $("#inpTagTo").val()!=0){
        if(parseInt($("#inpTagFrom").val())>parseInt($("#inpTagTo").val())){
            OBAlert('<%=Resource.getProperty("scm.custodybarcodelabel.tagtoshouldbegreaterthantagfrom",lang).replace("'","\\\'") %>');
            return false;
        }
        document.getElementById("inpTagFromNo").value = $("#inpTagFrom").val();
        document.getElementById("inpTagToNo").value = $("#inpTagTo").val();
    }
    if($("#inpMIRId").val() !=0){
        document.getElementById("inpMIRid").value = $("#inpMIRId").val();
    }
    if($("#inpBeneficiaryType").val()!=0 && $("#inpBeneficiaryId").val()!=0){
        document.getElementById("inpBeneficiarytype").value = $("#inpBeneficiaryType").val();
        document.getElementById("inpBeneficiaryid").value = $("#inpBeneficiaryId").val();
    }
    <%-- if (document.getElementById("inpBeneficiaryId").value == 0) {
        OBAlert('<%=Resource.getProperty("scm.custodycard.select.Beneficiary.idname",lang).replace("'","\\\'") %>');
        return false;
    } --%>    
    <%-- if (document.getElementById("inpMIRId").value == 0) {
        if($("#inpTagFrom").val()==0 || $("#inpTagTo").val()==0){
            OBAlert('<%=Resource.getProperty("scm.custodybarcodelabel.selecteithertagormir",lang).replace("'","\\\'") %>');
            return false;
        }
        else{
            if(parseInt($("#inpTagFrom").val())>parseInt($("#inpTagTo").val())){
                OBAlert('<%=Resource.getProperty("scm.custodybarcodelabel.tagtoshouldbegreaterthantagfrom",lang).replace("'","\\\'") %>');
                return false;
            }
            document.getElementById("inpTagFromNo").value = $("#inpTagFrom").val();
            document.getElementById("inpTagToNo").value = $("#inpTagTo").val();
        }        
    }else{      
        document.getElementById("inpMIRid").value = $("#inpMIRId").val();
    } --%>  
    
    document.getElementById("inpAction").value = "Submit";        
    openServletNewWindow('PRINT_PDF', true, null, 'CustodyCardReport', null, false, '700', '1000', true);
    return false;
}
 
<%-- (function( $ ) {
    $.widget( "ui.combobox", {
        _create: function() {
            var self = this,select = this.element.hide(),selected = select.children( ":selected" ),value = selected.val() ? selected.text() : "";
            var input = this.input = $( "<input  placeholder=\"<%= Resource.getProperty("scm.select", lang) %>\">" )
                .insertAfter( select )
                .val( value )
                .css('background-color', '#FFC')
                .autocomplete({
                        delay: 0,
                        minLength: 1,
                        /* source: function( request, response ) {
                            var matcher = new RegExp( $.ui.autocomplete.escapeRegex(request.term), "i" );
                            response( select.children( "option" ).map(function() {
                                var text = $( this ).text();                                
                                if ( this.value && ( !request.term || matcher.test(text) ) )
                                    return {
                                        label: text.replace(
                                                new RegExp("(?![^&;]+;)(?!<[^<>]*)(" +
                                                        $.ui.autocomplete.escapeRegex(request.term) +
                                                        ")(?![^<>]*>)(?![^&;]+;)", "gi"), 
                                                        "<strong>$1</strong>" ),
                                                        value: text,
                                                        option: this};
                                }) );
                            },
                        select: function( event, ui ) {
                            ui.item.option.selected = true;
                            self._trigger( "selected", event, {
                                item: ui.item.option
                                });                    
                        }, */ 
                        source: function( request, response ) {
                          var matcher = new RegExp( $.ui.autocomplete.escapeRegex(request.term), "i" );
                          var select_el = select.get(0); // get dom element
                          var rep = new Array(); // response array
                          var maxRepSize = 10; // maximum response size 
                          // simple loop for the options
                          for (var i = 0; i < select_el.length; i++) {
                            var text = select_el.options[i].text;
                            if ( select_el.options[i].value && ( !request.term || matcher.test(text) ) )
                            // add element to result array
                            rep.push({
                                label: text, // no more bold
                                value: text,
                                option: select_el.options[i]
                            });
                            if ( rep.length > maxRepSize ) {
                                rep.push({
                                    label: "... more available",
                                    value: "maxRepSizeReached",
                                    option: ""
                                        });
                                break;
                                }
                            }
                            // send response
                            response( rep );
                        }, 
                        select: function( event, ui ) {                       	
                            if ( ui.item.value == "maxRepSizeReached") {
                                return false;
                            } else {
                                ui.item.option.selected = true;
                                self._trigger( "selected", event, {
                                    item: ui.item.option
                                    });
                            }
                        },  
                        change: function( event, ui ) {                            
                            if($(select).attr("id")=='inpBeneficiaryId')
                            {       
                                // Selected an item, nothing to do
                                if ( ui.item ) {
                                    return;
                                }

                                var typedValue = $('#inpBeneficiaryId').next().val();
                                // Search for a match (case-insensitive)
                                var value = typedValue,
                                    valueLowerCase = value.toLowerCase(),
                                    valid = false;
                                
                                $("#inpBeneficiaryId > option").each(function() {
                                    if ( this.text.toLowerCase() === valueLowerCase ) {
                                        valid = true;
                                        allowPrint = true;
                                        return false;
                                    }
                                    else 
                                        allowPrint = false;
                                });

                                // Found a match, nothing to do
                                if ( valid ) {
                                    return;
                                }
                                $('#inpBeneficiaryId').next().val( "" );
                            }
                            
                            if($(select).attr("id")=='inpMIRId')
                            {                           
                                // Selected an item, nothing to do
                                if ( ui.item ) {
                                    return;
                                }

                                var typedValue = $('#inpMIRId').next().val();
                                // Search for a match (case-insensitive)
                                var value = typedValue,
                                    valueLowerCase = value.toLowerCase(),
                                    valid = false;
                                
                                $("#inpMIRId > option").each(function() {
                                    if ( this.text.toLowerCase() === valueLowerCase ) {
                                        valid = true;
                                        allowPrint = true;
                                        return false;
                                    }
                                    else 
                                        allowPrint = false;
                                });

                                // Found a match, nothing to do
                                if ( valid ) {
                                    return;
                                }
                                $('#inpMIRId').next().val( "" );                        
                            }
                            if($(select).attr("id")=='inpTagFrom')
                            {       
                                // Selected an item, nothing to do
                                if ( ui.item ) {
                                    return;
                                }

                                var typedValue = $('#inpTagFrom').next().val();
                                // Search for a match (case-insensitive)
                                var value = typedValue,
                                    valueLowerCase = value.toLowerCase(),
                                    valid = false;
                                
                                $("#inpTagFrom > option").each(function() {
                                    if ( this.text.toLowerCase() === valueLowerCase ) {
                                        valid = true;
                                        allowPrint = true;
                                        return false;
                                    }
                                    else 
                                        allowPrint = false;
                                });

                                // Found a match, nothing to do
                                if ( valid ) {
                                    return;
                                }
                                $('#inpTagFrom').next().val( "" );
                            }
                            if($(select).attr("id")=='inpTagTo')
                            {       
                                // Selected an item, nothing to do
                                if ( ui.item ) {
                                    return;
                                }

                                var typedValue = $('#inpTagTo').next().val();
                                // Search for a match (case-insensitive)
                                var value = typedValue,
                                    valueLowerCase = value.toLowerCase(),
                                    valid = false;
                                
                                $("#inpTagTo > option").each(function() {
                                    if ( this.text.toLowerCase() === valueLowerCase ) {
                                        valid = true;
                                        allowPrint = true;
                                        return false;
                                    }
                                    else 
                                        allowPrint = false;
                                });

                                // Found a match, nothing to do
                                if ( valid ) {
                                    return;
                                }
                                $('#inpTagTo').next().val( "" );
                            }
                            if($(select).attr("id")=='inpBeneficiaryType')
                            {       
                                // Selected an item, nothing to do
                                if ( ui.item ) {
                                    return;
                                }

                                var typedValue = $('#inpBeneficiaryType').next().val();
                                // Search for a match (case-insensitive)
                                var value = typedValue,
                                    valueLowerCase = value.toLowerCase(),
                                    valid = false;
                                
                                $("#inpBeneficiaryType > option").each(function() {
                                    if ( this.text.toLowerCase() === valueLowerCase ) {
                                        valid = true;
                                        allowPrint = true;
                                        return false;
                                    }
                                    else 
                                        allowPrint = false;
                                });

                                // Found a match, nothing to do
                                if ( valid ) {
                                    return;
                                }
                                $('#inpBeneficiaryType').next().val( "" );
                            }
                            
                        },
                         search: function( event, ui ) { 
                            if($(select).attr("id")=='inpBeneficiaryId'  )
                            {           
                                if($("#inpBeneficiaryId").next().val()=="")
                                    $("#inpBeneficiaryId").val("");
                            } 
                            if($(select).attr("id")=='inpMIRId'  )
                            {           
                                if($("#inpMIRId").next().val()=="")
                                    $("#inpMIRId").val("");
                            }
                            if($(select).attr("id")=='inpTagFrom'  )
                            {           
                                if($("#inpTagFrom").next().val()=="")
                                    $("#inpTagFrom").val("");
                            }
                            if($(select).attr("id")=='inpTagTo'  )
                            {           
                                if($("#inpTagTo").next().val()=="")
                                    $("#inpTagTo").val("");
                            }
                            if($(select).attr("id")=='inpBeneficiaryType'  )
                            {           
                                if($("#inpBeneficiaryType").next().val()=="")
                                    $("#inpBeneficiaryType").val("");
                            } 
                        }                     
                });
            if($(select).attr("id")=='inpBeneficiaryId')                
                input.addClass( "ui-widget autocomplete-widget-content ui-corner-left" ); 
            else if($(select).attr("id")=='inpMIRId')                
                input.addClass( "ui-widget autocomplete-widget-content ui-corner-left" ); 
            else if($(select).attr("id")=='inpTagFrom')                
                input.addClass( "ui-widget autocomplete-widget-content ui-corner-left" ); 
            else if($(select).attr("id")=='inpTagTo')                
                input.addClass( "ui-widget autocomplete-widget-content ui-corner-left" );
            else if($(select).attr("id")=='inpBeneficiaryType')                
                input.addClass( "ui-widget autocomplete-widget-content ui-corner-left" );
            else
                input.addClass( "ui-widget ui-widget-content autocomplete-widget-content ui-corner-left autocomp_hght autocomp_wdth" );
            input.data( "autocomplete" )._renderItem = function( ul, item ) {
                return $( "<li></li>" )
                .data( "item.autocomplete", item )
                .append( "<a>" + item.label + "</a>" )
                .appendTo( ul );
            };
            input.click(function() {
                this.select();
            });
            this.div = $("<div class='divComboImage'></div>")
                    .insertAfter( input )
                    .append(  $( "<button type='button'>&nbsp;</button>" )                              
                        .css({'padding-top':"0px","height":"21px"}) 
                        .attr( "tabIndex", -1 )
                        .attr( "title", "Show All Items" )                      
                        .button({icons: {primary: "ui-icon-triangle-1-s"},text: false})
                        .removeClass( "ui-corner-all" )
                        .addClass( "ui-corner-right ui-button-icon" )
                        .addClass("ui_flip_image ")
                        .click(function() {
                            if ( input.autocomplete( "widget" ).is( ":visible" ) ) {
                                input.autocomplete( "close" );
                                return;
                            }
                            input.autocomplete( "search", "" );
                            input.focus();  
                        }) 
                    )
                    .css({'float':'left'});
            },
            
            destroy: function() {
                this.input.remove();
                this.div.remove();
                this.element.show();
                $.Widget.prototype.destroy.call( this );
            }
        
    });
    
    
})( jQuery ); --%>
$( "#inpMIRId").select2(selectBoxAjaxPaging({size: "small", placeholder: true}));
$( "#inpTagFrom").select2(selectBoxAjaxPaging({size: "small", placeholder: true}));
$( "#inpTagTo").select2(selectBoxAjaxPaging({size: "small", placeholder: true}));
$( "#inpBeneficiaryType").select2(selectBoxAjaxPaging({size: "small", placeholder: true}));
$( "#inpBeneficiaryId").select2(selectBoxAjaxPaging({size: "small", placeholder: true}));

document.getElementById("select2-inpMIRId-container").style.backgroundColor="#F5F7F1";
document.getElementById("select2-inpTagFrom-container").style.backgroundColor="#F5F7F1";
document.getElementById("select2-inpTagTo-container").style.backgroundColor="#F5F7F1";
document.getElementById("select2-inpBeneficiaryType-container").style.backgroundColor="#F5F7F1";
document.getElementById("select2-inpBeneficiaryId-container").style.backgroundColor="#F5F7F1";

function disableTags(mirId){
    if(mirId!='0' && mirId!='' && mirId!=null){ 
        $("#inpTagFrom").select2("val", "0");
        $("#inpTagTo").select2("val", "0");
        $("#inpTagFrom").prop("disabled", true);
        $("#inpTagTo").prop("disabled", true);  

        <%-- var url = "<%=request.getContextPath()%>/CustodyCardReportAjax?action=getBenfDetails&mirId="+mirId;
        $.getJSON(url, function(result){    
        }).done(function(data){
            $('#inpBeneficiaryType option[value='+data.BenfType+']').attr("selected", "selected");
            if(getbeneficiary(data.BenfType)){
                $("#inpBeneficiaryId").select2("val", data.BenfName);
                /* $('#inpBeneficiaryId').val(data.BenfName);           
                $('#inpBeneficiaryId').next().val($('#'+data.BenfName+':selected').text()); */          
            }           
        }); --%>
    }else{
        $("#inpTagFrom").prop("disabled", false);
        $("#inpTagTo").prop("disabled", false); 
    }
}
function getbeneficiary(type){
    document.getElementById("inpBeneficiaryId").value = "0";
        $("#inpBeneficiaryId").select2(selectBoxAjaxPaging({
            url : function() {
                return '<%=request.getContextPath()%>/CustodyCardReportAjax?action=getbeneficiary&inptype='+type
            },
            size : "small"
        }));
        document.getElementById("select2-inpBeneficiaryId-container").style.backgroundColor="#F5F7F1";
    return true;
} 

function getFromTagList(){	
	document.getElementById("inpTagFrom").value = "0";	
    $("#inpTagFrom").select2(selectBoxAjaxPaging({
       url : function() {
           return '<%=request.getContextPath()%>/CustodyCardReportAjax?action=getTagList'
       },
       size : "small"
   }));
   document.getElementById("select2-inpTagFrom-container").style.backgroundColor="#F5F7F1";  
   return true;
}
function getToTagList(){  
    document.getElementById("inpTagTo").value = "0";
    $("#inpTagTo").select2(selectBoxAjaxPaging({
       url : function() {
    	    return '<%=request.getContextPath()%>/CustodyCardReportAjax?action=getTagList'
       },
       size : "small"
   }));   
   document.getElementById("select2-inpTagTo-container").style.backgroundColor="#F5F7F1"; 
   return true;
}

</script>
</HTML>