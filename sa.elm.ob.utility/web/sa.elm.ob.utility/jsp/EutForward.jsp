 <%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
 <%@page import="sa.elm.ob.utility.util.Utility"%>
 <%@page import="sa.elm.ob.scm.ad_reports.InvitationLetterReport.InvitationLetterReport"%>
 <%@ page import="sa.elm.ob.utility.properties.Resource" errorPage="/web/jsp/ErrorPage.jsp"%>
 <%@ page import="java.util.List,java.util.ArrayList" errorPage="/web/jsp/ErrorPage.jsp" %>
 <%@page import="sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoVO"%>
 <%     
 String lang = ((String)session.getAttribute("#AD_LANGUAGE"));
 String style="../web/skins/ltr/org.openbravo.userinterface.skin.250to300Comp/250to300Comp/Openbravo_ERP.css";
 String toolBarStyle="../web/js/common/CommonFormLtr.css";
 if(lang.equals("ar_SA")){
  style="../web/skins/rtl/org.openbravo.userinterface.skin.250to300Comp/250to300Comp/Openbravo_ERP_250.css";
 toolBarStyle="../web/js/common/CommonFormRtl.css";
 }
 %>

<HTML xmlns="http://www.w3.org/1999/xhtml">
<HEAD>
 <TITLE><%=Resource.getProperty("utility.forward",lang)%></TITLE>
 <META http-equiv="Content-Type" content="text/html; charset=utf-8"></META>
   
   <LINK rel="stylesheet" type="text/css" href="<%=style %>" id="paramCSS"></LINK>
<link rel="stylesheet" type="text/css" href="<%=toolBarStyle %>"></link> 
<link rel="stylesheet" type="text/css" href="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.picker.css"></link>
<link rel="stylesheet" type="text/css" href="../web/js/jquery/jquery-ui-1.8.11.custom.css"></link>   
<link rel="stylesheet" type="text/css" href="../web/js/common/htabs.css"></link>
<link rel="stylesheet" type="text/css" href="../web/js/common/select2.min.css"></link>

<script type="text/javascript" src="../web/js/common/common.js"></script>
<script type="text/javascript" src="../web/js/utils.js"></script>
<script type="text/javascript" src="../web/js/windowKeyboard.js"></script>
<script type="text/javascript" src="../web/js/searchs.js"></script>
<script type="text/javascript" id="paramDirectory">var baseDirectory = "../web/";</script>
<script type="text/javascript" id="paramLanguage">var defaultLang="en_US";</script>



<!-- <script type="text/javascript" src="../web/js/default/DateTextBox.js"></script>
<script type="text/javascript" src="../web/js/jscalendar/calendar.js"></script>
<script type="text/javascript" src="../web/js/jscalendar/lang/calendar-lang.js"></script>  -->  

<script type="text/javascript" src="../web/js/jquery-1.7.2.min.js"></script>
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.min_1.11.0.js"></script> 
    <script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.plugin.js"></script> 
    <script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.js"></script> 
    <script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.ummalqura.js"></script> 
    <script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.ummalqura-ar.js"></script> 
    <script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.plus.js"></script> 
    <script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.picker.js"></script> 
    <script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.picker.ext.js"></script>
 <!--    <style type="text/css">
    #client
    {
        height:auto !important;
    }
    </style> -->
    <style type="text/css">
  .ui-autocomplete-input {height: 21px; border-radius:0px !important; width:252px;font-size: 12px; padding: 0 0 0 3px; float: left;border:1px solid #CDD7BB;}
   .ui-button-icon-primary{left: .9em !important;} 
    ul{border:1px solid #FF9C30 !important;}        
</style> 
 <script type="text/javascript">

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
    
    $('#inpStartDate').calendarsPicker({calendar:  
        $.calendars.instance('ummalqura'),dateFormat: 'dd-mm-yyyy',onSelect : function(dates){},showTrigger:  
    '<img src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/calendar.gif" alt="Popup" class="trigger">'});
    $('#inpEndDate').calendarsPicker({calendar:  
        $.calendars.instance('ummalqura'),dateFormat: 'dd-mm-yyyy',onSelect : function(dates){},showTrigger:  
    '<img src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/calendar.gif" alt="Popup" class="trigger">'});
}
function onResizeDo() {
    resizeArea();
}
</script>
  
</HEAD>
<BODY onresize="onResizeDo();" leftmargin="0" topmargin="0" marginwidth="0" marginheight="0" id="paramBody">
<%-- <BODY leftmargin="0" topmargin="0" marginwidth="0" marginheight="0" onload="onLoadDo();" onresize="onResizeDo();" onfocus="window.close();" id="paramBody"> --%>
<%--<FORM name="frmMain" method="post" action=""> --%>
<FORM id="form" method="post" action="" name="frmMain">
  <INPUT type="hidden" name="Command"></INPUT>  
  <INPUT type="hidden" name="inpAction" id="inpAction" value=""></INPUT>
   
  <INPUT type="hidden" name="inpTabId" id=inpTabId value="<%= request.getAttribute("inpTabId")%>"></INPUT>
  <INPUT type="hidden" name="inpwindowId" id="inpwindowId" value="<%=request.getAttribute("inpwindowId")%>"></INPUT>
  <INPUT type="hidden" name="inpRecordId" id=inpRecordId value="<%= request.getAttribute("inpRecordId")%>"></INPUT>
  <INPUT type="hidden" name="inpAwardLookUps" id=inpAwardLookUps value=""></INPUT>
  <INPUT type="hidden" name="inpLetterCount" id=inpLetterCount value="<%= request.getAttribute("Count")%>"></INPUT>
  
  
       
 <TABLE cellspacing="0" cellpadding="0" width="100%" id="table_header">
  <TR>
    <TD>
      <TABLE cellspacing="0" cellpadding="0" class="Popup_ContentPane_NavBar" id="tdToolBar">
        <TR class="Popup_NavBar_bg"><TD></TD>
          <TD class="Popup_NavBar_separator_cell"></TD>
          <TD class="Popup_NavBar_Popup_title_cell"><SPAN><%=Resource.getProperty("utility.forward",lang)%></SPAN></TD>
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
                <td class="Popup_InfoBar_text" id="processHelp"><%=Resource.getProperty("utility.forward",lang)%> </td>
              </tr>
            </tbody></table>
          </td>
        </tr>
      </tbody></table>
    </td>
</tr>
</TABLE>


<div id="Request_info" style="width:100%;" align="center">
           
                    <table>
                        <tbody><tr>
                            <td class="TitleCell" align="center"><span class="LabelText"><%= Resource.getProperty("utility.emp",lang)%></span></td>
                            <td class="TextBox_ContentCell">
                                                <select id="inpUser" class="ComboKey Combo_TwoCells_width" onchange="getRoleList(this.value)">
                                                 <option value="0"><%= Resource.getProperty("utility.comboselect",lang).replace("'", "\\\'") %></option>
                                                
                                                </select>
                                             </td>
                        </tr>  
                               <tr>
                                             <td class="TitleCell"><span class="LabelText"><%=Resource.getProperty("utility.torole", lang).replace("'", "\\\'")%>
                                               
                                                </span>
                                             </td>
                                             <td class="TextBox_ContentCell">
                                                <select id="inpRole" class="ComboKey Combo_TwoCells_width">
                                                   <option value='0'><%= Resource.getProperty("utility.comboselect",lang).replace("'", "\\\'") %></option>
                                     
                                                </select>
                                             </td>
                                          </tr>   
                                          
                                           <tr>
                                             <td class="TitleCell"><span class="LabelText"><%=Resource.getProperty("utility.emp.department", lang).replace("'", "\\\'")%>
                                               
                                                </span>
                                             </td>
                                             <td class="TextBox_ContentCell">
                                                <%-- <select id="inpdept" class="Combo_TwoCells_width">
                                                   <option value='0'><%= Resource.getProperty("utility.comboselect",lang).replace("'", "\\\'") %></option>
                                     
                                                </select> --%>
                                                <input type="text" name="inpdept" id="inpdept"  maxlength="250"  class="TextBox_TwoCells_width"></input>
                                                
                                             </td>
                                          </tr>  
                                          
                                           <tr>
                                             <td class="TitleCell"><span class="LabelText"><%=Resource.getProperty("utility.emp.designation", lang).replace("'", "\\\'")%>
                                               
                                                </span>
                                             </td>
                                             <td class="TextBox_ContentCell">
                                                <%-- <select id="inpposition" class="Combo_TwoCells_width">
                                                   <option value='0'><%= Resource.getProperty("utility.comboselect",lang).replace("'", "\\\'") %></option>
                                     
                                                </select> --%>
                                                <input type="text" name="inpposition" id="inpposition"  maxlength="250"  class="TextBox_TwoCells_width"></input>
                                               
                                             </td>
                                          </tr>  
                                           <tr>
                            <td class="TitleCell"><span class="LabelText"><%= Resource.getProperty("utility.forward.comments",lang)%></span></td>
                            <td class="TextBox_ContentCell"><textarea rows="4" width="50" cols="50" id="inpcomments" maxlength="2000" name="inpcomments" class="TextBox_btn_TwoCells_width" ></textarea></td>
                                                     
                </tr>              
          
                   <TR  align = "center">
                                             <td></td>
                                             <TD  id="submitButton">
                                                <BUTTON type="button" id="Submit_linkBTN" class="ButtonLink" onclick="forward();" style="margin: 10px 0;">
                                                   <TABLE class="Button">
                                                      <TR>
                                                         <TD class="Button_left"><IMG class="Button_Icon Button_Icon_process" alt="Submit" title="Submit" src="../web/images/blank.gif" border="0"></IMG></TD>
                                                         <TD class="Button_text" id="Submit_BTNname">
                                                            <%= Resource.getProperty("utility.forward",lang).replace("'", "\\\'") %>
                                                         </TD>
                                                         <TD class="Button_right"></TD>
                                                      </TR>
                                                   </TABLE>
                                                </BUTTON>
                                             </TD>
                                          </TR>
                        <tr><td>&nbsp;</td></tr>
    
  
            </tbody></table>               
            

</div>





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
 <script type="text/javascript">
<%-- <% List<PersonalPaymentMethodVO> inpbank = (ArrayList<PersonalPaymentMethodVO>)  request.getAttribute("inpbank");
  //strbanklist = "{";
  for (PersonalPaymentMethodVO vo : inpbank){
    strbanklist += vo.getbankdetailId() + ":" +  vo.getbankname() + ";";
  }
  // strbanklist += "}";
    List<EfinBankBranch> inpbranch = (ArrayList<EfinBankBranch>)  request.getAttribute("inpbranch");
  for (EfinBankBranch vo : inpbranch){
    strbranchList += vo.getId() + ":" +  vo.getBranchCode() + " "+  vo.getBranchName() +";";
  }
  %>
  document.getElementById("strbank").value="<%=strbanklist.substring(0,strbanklist.lastIndexOf(";"))%>";
   document.getElementById("strbranch").value="<%=strbranchList.substring(0,strbranchList.lastIndexOf(";"))%>";  --%>
  
<%-- var changedvalue="<%= Resource.getProperty("hcm.changedvaluessave", lang) %>"; --%>

var contextpath= '<%=request.getContextPath()%>';
var lang ='<%=lang%>',
direction='ltr';
    if(lang=="ar_SA"){
        direction='rtl';
     }
document.getElementById("inpdept").readOnly  = true;
document.getElementById("inpposition").readOnly  = true;


</script>
<script type="text/javascript">
var empStatus = "<%=Resource.getProperty("utility.fill.employee", lang)%>";
var empRole = "<%=Resource.getProperty("utility.fill.role", lang)%>";
var disablePreference = "<%=Resource.getProperty("utility.disableForward", lang)%>";
var noOrgAccess = "<%=Resource.getProperty("utility.forward.noorgaccess", lang)%>";
var validUser = "<%=Resource.getProperty("utility.validUserList", lang)%>";
</script>
<script type="text/javascript" src="../web/js/jquery-1.7.2.min.js"></script>
<script type="text/javascript" src="../web/js/ui/jquery.ui.core.js"></script>
<script type="text/javascript" src="../web/js/jquery/grid.locale-en.js"></script>
<script type="text/javascript" src="../web/js/jquery/jquery.jqGrid.min.js"></script>
<script type="text/javascript" src="../web/js/ui/jquery.ui.datepicker.js"></script>
<script type="text/javascript" src="../web/js/common/select2.min.js"></script>
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/eut_forward.js"></script>
<script type="text/javascript" src="../web/js/inlineNavGrid.js"></script>  
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.plugin.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.ummalqura.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.ummalqura-ar.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.plus.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.picker.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.picker.ext.js"></script>

</HTML>