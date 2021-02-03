<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
    <%@page import="sa.elm.ob.finance.properties.Resource" errorPage="/web/jsp/ErrorPage.jsp" %>
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
<TITLE><%= Resource.getProperty("finance.funddept",lang) %></TITLE>
<META http-equiv="Content-Type" content="text/html; charset=utf-8"></META> 
 <link rel="stylesheet" type="text/css" href="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.picker.css"></link>
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
  
  <script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.min_1.11.0.js"></script> 
    <script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.plugin.js"></script> 
    <script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.js"></script> 
    <script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.ummalqura.js"></script> 
    <script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.ummalqura-ar.js"></script> 
    <script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.plus.js"></script> 
    <script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.picker.js"></script> 
    <script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.picker.ext.js"></script>
    <script type="text/javascript" src="../web/js/common/DateConverter.js"></script>
    <script type="text/javascript" src="../web/sa.elm.ob.utility/js/DateUtils.js"></script>
 
<style type="text/css">
  .ui-autocomplete-input {height: 21px; border-radius:0px !important; width:252px;font-size: 12px; padding: 0 0 0 3px; float: left;border:1px solid #CDD7BB;}
  .beneficiaryClass {}
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
    
    $('#inpDob').calendarsPicker({calendar:  
        $.calendars.instance('ummalqura'),dateFormat: 'dd-mm-yyyy',onSelect : function(dates){},showTrigger:  
    '<span class="inpDopImage" ><img src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/calendar.gif" alt="Popup" class="trigger" </span>'});
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
         <INPUT type="hidden" name="inpLoanPaid" id="inpLoanPaid" value=""></INPUT>
         <INPUT type="hidden" name="inpCitizenNameHide" id="inpCitizenNameHide" value=""></INPUT>
         <INPUT type="hidden" name="inpCitizenIdHide" id="inpCitizenIdHide" value=""></INPUT>
         <INPUT type="hidden" name="inpBankTypeHide" id="inpBankTypeHide" value=""></INPUT>
         <INPUT type="hidden" name="inpErrorNameHide" id="inpErrorNameHide" value=""></INPUT>
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
                              src="../web/images/blank.gif" title= <%= Resource.getProperty("finance.reload",lang).replace("'", "\\\'") %>
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
                                       <%= Resource.getProperty("finance.funddept",lang).replace("'", "\\\'") %>
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
                                       <TABLE  align="center" width="70%">
                                          <TR>
                                             <TD>&nbsp;</TD>
                                          </TR>
                                          <tr>
                                             <td class="TitleCell"><span class="LabelText" id="inpBankTypeLabel"> <b> <%=Resource.getProperty("finance.fd.banktype", lang).replace("'", "\\\'")%>
                                                </b>
                                                </span>
                                             </td>
                                             <td class="TextBox_ContentCell">
                                                <select id="inpBankTypeId" class="ComboKey Combo_TwoCells_width" onchange="onChangeBankType();">
                                                   <option value="0"><%= Resource.getProperty("finance.Select",lang).replace("'", "\\\'") %></option>
                                                   <option value="rst"><%= Resource.getProperty("finance.web.rst",lang).replace("'", "\\\'") %></option>
                                                   <option value="agri"><%= Resource.getProperty("finance.web.agri",lang).replace("'", "\\\'") %></option>
                                                   <option value="scsb"><%= Resource.getProperty("finance.web.scsb",lang).replace("'", "\\\'") %></option>
                                                </select>
                                             </td>
                                          </tr>
                                          <tr>
                                             <td class="TitleCell"><span class="LabelText" id="inpCitizenIdLable"> <b> <%=Resource.getProperty("finance.fd.citizenid", lang).replace("'", "\\\'")%>
                                                </b>
                                                </span>
                                             </td>
                                             <td class="TextBox_ContentCell">
                                               <input type="text" name="inpCitizenId" id="inpCitizenId" maxlength="10"  class="dojoValidateValid TextBox_TwoCells_width" onkeydown="return true;"   onkeyup="onChangeCitizen();"/>
                                             </td>
                                          </tr>
                                          
                                          <tr id="inpDobRow" style="display:none;">
                                             <td class="TitleCell"><span class="LabelText" id="inpDobLabel"> <b> <%=Resource.getProperty("finance.fd.citizedob", lang).replace("'", "\\\'")%>
                                                </b>
                                                </span>
                                             </td>
                                              <td class="TextBox_ContentCell">
                                                     <input type="text" id="inpDob"  class="dojoValidateValid TextBox_btn_OneCell_width"  onkeydown="return true;"    maxlength="10" name="inpDob" ></input> 
                                              </td>
                                          </tr>
                                          <tr id="inpLoanScsbRow" style="display:none;">
                                             <td class="TitleCell"><span class="LabelText" id="inpLoanNoLabel"> <b> <%=Resource.getProperty("finance.fd.loanNumber", lang).replace("'", "\\\'")%>
                                                </b>
                                                </span>
                                             </td>
                                              <td class="TextBox_ContentCell">
                                               <textarea rows="4" width=70 cols="50" id="inpLoanNo"  maxlength="2000" 
                                               name="inpLoan" class="TextBox_btn_TwoCells_width" readonly>
                                               </textarea>
                                              </td>
                                          </tr>
                                           <tr id="inpContractAgriRow" style="display:none;">
                                             <td class="TitleCell"><span class="LabelText" id="inpContractNoLabel"> <b> <%=Resource.getProperty("finance.fd.contractNumber", lang).replace("'", "\\\'")%>
                                                </b>
                                                </span>
                                             </td>
                                              <td class="TextBox_ContentCell">
                                               <textarea rows="4" width=70 cols="50" id="inpContractNo"  maxlength="2000" 
                                               name="inpContractNo" class="TextBox_btn_TwoCells_width" readonly>
                                               </textarea>
                                              </td>
                                          </tr>
                                          
                                          <tr>
                                             <td class="TitleCell"><span class="LabelText" id="inpCitizenNameLabel"> <b> <%=Resource.getProperty("finance.fd.citizenname", lang).replace("'", "\\\'")%>
                                                </b>
                                                </span>
                                             </td>
                                             <td class="TextBox_ContentCell">
                                               <input type="text" name="inpCitizenName" id="inpCitizenNameId" maxlength="30"  class="dojoValidateValid TextBox_TwoCells_width" readonly onkeydown="return true;" onkeyup="enableForm(this);" />
                                             </td>
                                          </tr>
                                           <tr id="inploanerTypeRow" style="display:none;">
                                              <td class="TitleCell"><span class="LabelText" id="inploanerTypeLabel"> <b> <%=Resource.getProperty("finance.web.loanerType", lang).replace("'", "\\\'")%>
                                              </b>
                                              </span>
                                              </td>
                                              <td class="TextBox_ContentCell">
                                               <input type="text" name="inploanerTypeName" id="inploanerTypeId" maxlength="30"  class="dojoValidateValid TextBox_TwoCells_width"  readonly onkeydown="return true;" onkeyup="enableForm(this);" />
                                             </td> 
                                          </tr>
                                          <tr id="inpScheduledRow" style="display:none;">
                                              <td class="TitleCell"><span class="LabelText" id="inpScheduledLabel"> <b> <%=Resource.getProperty("finance.web.scheduled", lang).replace("'", "\\\'")%>
                                              </b>
                                              </span>
                                              </td>
                                             <!--  <td class="TextBox_ContentCell" >
                                                <input type="checkbox" id="inpScheduledId" name="inpScheduledName" readonly></input>
                                              </td> 
                                               -->
                                              <td class="TextBox_ContentCell">
                                               <input type="text" name="inpScheduledName" id="inpScheduledId" maxlength="30"  class="dojoValidateValid TextBox_TwoCells_width"  readonly onkeydown="return true;" onkeyup="enableForm(this);" />
                                             </td> 
                                          </tr>
                                          <tr id="inpdemandedRow" style="display:none;">
                                              <td class="TitleCell"><span class="LabelText" id="inpdemandedLabel"> <b> <%=Resource.getProperty("finance.web.demanded", lang).replace("'", "\\\'")%>
                                              </b>
                                              </span>
                                              </td>
                                             <!--  <td class="TextBox_ContentCell" >
                                                <input type="checkbox" id="inpdemandedId" name="inpdemandedName" readonly></input>
                                              </td>  -->
                                              <td class="TextBox_ContentCell">
                                               <input type="text" name="inpdemandedName" id="inpdemandedId" maxlength="30"  class="dojoValidateValid TextBox_TwoCells_width"  readonly onkeydown="return true;" onkeyup="enableForm(this);" />
                                             </td> 
                                          </tr>
                                           
                                          <tr id="inpLoanAmountRow">
                                             <td class="TitleCell"><span class="LabelText" id="inpLoanAmountLable"> <b> <%=Resource.getProperty("finance.fd.loanAmount", lang).replace("'", "\\\'")%>
                                                </b>
                                                </span>
                                             </td>
                                             <td class="TextBox_ContentCell">
                                             
                                             <textarea rows="4" width=70 cols="50" id="inpLoanAmountId"  maxlength="2000" name="inpLoanAmount" class="TextBox_btn_TwoCells_width" readonly>
                                               </textarea>
                                             </td>
                                          </tr>
                                          <tr id="inpErrorCodeRow" style="display:none;">
                                             <td class="TitleCell"><span class="LabelText" id="inpErrorCodeLabel"> <b> <%=Resource.getProperty("finance.fd.errorCode", lang).replace("'", "\\\'")%>
                                                </b>
                                                </span>
                                             </td>
                                             <td class="TextBox_ContentCell">
                                               <input type="text" name="inpErrorCode" id="inpErrorCodeId" maxlength="30"  class="dojoValidateValid TextBox_TwoCells_width"  readonly onkeydown="return true;" onkeyup="enableForm(this);" />
                                             </td>
                                          </tr>
                                          <tr id="inpErrorMessageRow" style="display:none;">
                                             <td class="TitleCell"><span class="LabelText" id="inpErrorMsgLabel"> <b> <%=Resource.getProperty("finance.fd.errorMessage", lang).replace("'", "\\\'")%>
                                                </b>
                                                </span>
                                             </td>
                                             <td class="TextBox_ContentCell">
                                               <textarea rows="4" width=70 cols="50" id="inpErrorMsgId"  maxlength="2000" name="inpErrorMsg" class="TextBox_btn_TwoCells_width" readonly>
                                               </textarea>
                                             </td>
                                             
                                          </tr>
                                           <TR  align = "center" style="padding-left: 5px:">
                                              <td></td>
                                             <TD  id="submitButton" colspan=2>
                                             <div align="left">
                                                <BUTTON type="button" id="Submit_linkBTN" class="ButtonLink" onclick="fillLoanDetails();" style="margin: 10px 0;">
                                                   <TABLE class="Button">
                                                      <TR>
                                                         <TD class="Button_left"><IMG class="Button_Icon Button_Icon_process" alt="Submit" title="Submit" src="../web/images/blank.gif" border="0"></IMG></TD>
                                                         <TD class="Button_text" id="Submit_BTNname">
                                                            <%= Resource.getProperty("finance.web.search",lang).replace("'", "\\\'") %>
                                                         </TD>
                                                         <TD class="Button_right"></TD>
                                                      </TR>
                                                   </TABLE>
                                                </BUTTON>
                                                
                                                 <BUTTON type="button" id="Submit_linkBTN" class="ButtonLink" onclick="generateReport();" style="margin: 10px 0;margin-left: 20px">
                                                   <TABLE class="Button">
                                                      <TR>
                                                         <TD class="Button_left"><IMG class="Button_Icon Button_Icon_process" alt="Submit" title="Submit" src="../web/images/blank.gif" border="0"></IMG></TD>
                                                         <TD class="Button_text" id="Submit_BTNname">
                                                            <%= Resource.getProperty("finance.web.printReport",lang).replace("'", "\\\'") %>
                                                         </TD>
                                                         <TD class="Button_right"></TD>
                                                      </TR>
                                                   </TABLE>
                                                </BUTTON>
                                                </div>
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

function generateReport() {
    document.getElementById("inpAction").value = "generateReport"; 
    document.getElementById("inpLoanPaid").value = document.getElementById("inpLoanAmountId").value;
    document.getElementById("inpCitizenNameHide").value =document.getElementById("inpCitizenNameId").value;
    document.getElementById("inpCitizenIdHide").value = document.getElementById("inpCitizenId").value;
    document.getElementById("inpBankTypeHide").value = document.getElementById("inpBankTypeId").value;
    document.getElementById("inpErrorNameHide").value = document.getElementById("inpErrorMsgId").value;
    
    openServletNewWindow('PRINT_PDF', true, null, 'FundDeptEnquiry', null, false, '700', '1000', true);
    return false;
}


function fillLoanDetails(){
    
    if(document.getElementById("inpCitizenId").value===''){
        OBAlert('<%=Resource.getProperty("finance.fd.citizenMandatory",lang).replace("'","\\\'") %>');
        return false;   
    }
    var citizenId=document.getElementById("inpCitizenId").value;
    
    if(citizenId.length != 10){
        OBAlert('<%=Resource.getProperty("finance.web.citizenIdLength",lang).replace("'","\\\'") %>');
        return false;   
    }
        
    $.post('<%=request.getContextPath()%>/FundDeptEnquiryAjax', {
        action : 'getLoanDetails',
        inpLoanType : document.getElementById("inpBankTypeId").value,
        inpCitizenId : document.getElementById("inpCitizenId").value,
        inpdob : document.getElementById("inpDob").value
        
    }, function(data) {
        $(data).find("LoanDetails").each(function()
                {    
                    var bank_type=document.getElementById("inpBankTypeId").value;
                    var citizen=$(this).find("citizenName").text();
                    var loanAmount=$(this).find("loanAmount").text();
                    var loanRemainingAmount=$(this).find("loanRemainingAmount").text();
                    var errorCode=$(this).find("errorCode").text();
                    var errorMessage=$(this).find("errorMessage").text();
                    var paidAmount =$(this).find("paidAmount").text();
                    if(bank_type==="rst"){
                         var scheduled=$(this).find("scheduled").text();
                         var demanded=$(this).find("demanded").text();
                         var loanerType =$(this).find("loanerType").text();
                         document.getElementById("inploanerTypeId").value =loanerType;
                         document.getElementById("inpScheduledId").value =scheduled;
                         document.getElementById("inpdemandedId").value =demanded;
                    }
                    if(bank_type==="agri"){
                        var contractNo=$(this).find("contractNumber").text();
                        document.getElementById("inpContractNo").value=contractNo;
                    }
                    if(bank_type==="scsb"){
                        var loanNo=$(this).find("loanNumber").text();
                        document.getElementById("inpLoanNo").value=loanNo;
                    }
                    if(errorCode.length == 0){
                        $("#inpErrorCodeRow").hide();
                        $("#inpErrorMessageRow").hide();
                    }
                    else{
                        $("#inpErrorCodeRow").show();
                        $("#inpErrorMessageRow").show();    
                    }
                    document.getElementById("inpCitizenNameId").value =citizen;
                    document.getElementById("inpLoanAmountId").value =loanRemainingAmount;
                    document.getElementById("inpErrorCodeId").value =errorCode;
                    document.getElementById("inpErrorMsgId").value =errorMessage;
                    
                }); 
       
    });

}
function onChangeCitizen(){
    document.getElementById("inpCitizenNameId").value="";
    document.getElementById("inpErrorCodeId").value="";
    document.getElementById("inpLoanAmountId").value="";
    document.getElementById("inpContractNo").value="";
    document.getElementById("inpLoanNo").value="";
    document.getElementById("inpErrorMsgId").value="";
    document.getElementById("inploanerTypeId").value ="";
    document.getElementById("inpScheduledId").value ="";
    document.getElementById("inpdemandedId").value ="";
}
function onChangeBankType(){
     $("#inpErrorCodeRow").hide();
     $("#inpErrorMessageRow").hide();
    document.getElementById("inpCitizenNameId").value="";
    document.getElementById("inpErrorCodeId").value="";
    document.getElementById("inpLoanAmountId").value="";
    document.getElementById("inpContractNo").value="";
    document.getElementById("inpLoanNo").value="";
    document.getElementById("inpErrorMsgId").value="";
    document.getElementById("inploanerTypeId").value ="";
    document.getElementById("inpScheduledId").value ="";
    document.getElementById("inpdemandedId").value ="";

    var bank_type=document.getElementById("inpBankTypeId").value;
   
    if(bank_type != "rst"){
        $("#inpDobRow").hide();
        $("#inpDobLabel").hide();
        $("#inpDob").hide();
        $(".inpDopImage").hide();  
        $("#inpLoanAmountRow").show();
        $("#inploanerTypeRow").hide();
        $("#inpScheduledRow").hide()
        $("#inpdemandedRow").hide()
      
    }
    else{
        $("#inpLoanAmountRow").show();
        $("#inpDobRow").show();
        $("#inpDobLabel").show();
        $("#inpDob").show();
        $(".inpDopImage").show();
        $("#inploanerTypeRow").show();
        $("#inpScheduledRow").show()
        $("#inpdemandedRow").show()
    }
    if(bank_type==="agri"){
        $("#inpContractAgriRow").show();
        //document.getElementById("inpContractDateRow").style.display=""; 
    }
    else{
        $("#inpContractAgriRow").hide();
        //document.getElementById("inpContractDateRow").style.display="none"; 
    }
    if(bank_type==="scsb"){
        $("#inpLoanScsbRow").show();
        
    }
    else{
        $("#inpLoanScsbRow").hide();
        //document.getElementById("inpLoanDateRow").style.display="none"; 
    }
}
function reloadWindow() {
    submitCommandForm('DEFAULT', false, null, 'FundDeptEnquiry', '_self', null, false);
    return false;
}
function onClickRefresh() {
    document.getElementById("inpAction").value = "";
    reloadWindow();
}
   </script>
</HTML>