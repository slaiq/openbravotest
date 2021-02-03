<!--
 *************************************************************************
 * All Rights Reserved.
 
 *************************************************************************
-->
<%@ page import="sa.elm.ob.finance.properties.Resource" errorPage="/web/jsp/ErrorPage.jsp" %>
<%@page import="sa.elm.ob.utility.util.Utility"%>
<%@page import="java.util.List, java.util.ArrayList"%>
<%@page import="sa.elm.ob.finance.ad_forms.clearbankcheque.vo.ClearChequeVO"%>
 <%
    String lang = ((String)session.getAttribute("#AD_LANGUAGE"));

    String style="../web/skins/ltr/org.openbravo.userinterface.skin.250to300Comp/250to300Comp/Openbravo_ERP.css";
    String DialogBoxStyle="../web/js/common/CommonDialogFormLtr.css";
    String toolBarStyle="../web/js/common/CommonFormLtr.css";

    if(lang.equals("ar_SA")){
      style="../web/skins/rtl/org.openbravo.userinterface.skin.250to300Comp/250to300Comp/Openbravo_ERP_250.css";
     toolBarStyle="../web/js/common/CommonFormRtl.css";
     DialogBoxStyle="../web/js/common/CommonDialogFormRtl.css";
    }
    %>
<HTML xmlns="http://www.w3.org/1999/xhtml">
  <HEAD>
   <META http-equiv="Content-Type" content="text/html; charset=utf-8"></META> 
    <LINK rel="stylesheet" type="text/css" href="<%=style %>" id="paramCSS"></LINK>
      <link rel="stylesheet" type="text/css" href="../web/org.openbravo.userinterface.smartclient/openbravo/skins/Default/org.openbravo.client.application/ob-toolbar-styles.css"></link>
     <link rel="stylesheet" type="text/css" href="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.picker.css"></link>
       <link rel="stylesheet" type="text/css" href="../web/js/jquery/jquery-ui-1.8.11.custom.css"></link>
    <link rel="stylesheet" type="text/css" href="../web/js/jquery/ui.jqgrid.css"></link>
    <link rel="stylesheet" type="text/css" href="<%=toolBarStyle %>"></link>
    <link rel="stylesheet" type="text/css" href="../web/js/common/htabs.css"></link>
    <link rel="stylesheet" type="text/css" href="../web/js/common/select2.min.css"></link>
    <style type="text/css">
     .calendars-popup {
      z-index: 50001;
      position: absolute;
    }
    td div .readonly, td div .readonly_focus { border: 1px solid transparent !important; color: #222222 !important; }
    table.ui-jqgrid-htable > thead > tr.ui-jqgrid-labels > th.ui-state-default > div {
        white-space: nowrap !important;
        overflow: hidden !important;
        text-overflow: ellipsis !important;
    }
    .select2-dropdown { z-index: 100001 !important; }
    </style>
    <script type="text/javascript" src="../web/js/common/common.js"></script>
    <script type="text/javascript" src="../web/js/utils.js"></script>
    <script type="text/javascript" src="../web/js/windowKeyboard.js"></script>
    <script type="text/javascript" src="../web/js/searchs.js"></script>
    <script type="text/javascript" id="paramDirectory">var baseDirectory = "../web/";</script>
    <script type="text/javascript" id="paramLanguage">var defaultLang="en_US";</script>
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
    }
    function onResizeDo(){
           resizeArea();
           reSizeGrid();
    }
    function onLoad()
    {


 <% request.setAttribute("savemsg",request.getAttribute("savemsg")==null?"":request.getAttribute("savemsg")); %>
        <%  if(request.getAttribute("savemsg").equals("Success")) { %>
          displayMessage("S", "<%=Resource.getProperty("hcm.success",lang)%>","<%= Resource.getProperty("hcm.saveoreditsuccess", lang) %>");
          <%}%>
    }
    </script>
</HEAD>

<BODY onresize="onResizeDo();" leftmargin="0" topmargin="0" marginwidth="0" marginheight="0" id="paramBody">
    <FORM id="form" method="post" action="" name="frmMain">
        <INPUT type="hidden" name="Command"></INPUT>
        <INPUT type="hidden" name="IsPopUpCall" value="1"></INPUT>
        <INPUT type="hidden" name="inpLastFieldChanged"></INPUT>
        <INPUT type="hidden" name="inpAction" id="inpAction" value=""></INPUT>
        <INPUT type="hidden" name="inpLockH" id="inpLockH" value="N"></INPUT>
        <INPUT type="hidden" name="inpPaymentList" id="inpPaymentList" value=""></INPUT>
        <INPUT type="hidden" name="inpPaymentGridId" id="inpPaymentGridId" value=""></INPUT>
        <INPUT type="hidden" name="defaultOrg" id="defaultOrg" value='<%= request.getAttribute("org") %>'></INPUT>
        <INPUT type="hidden" name="defaultBank" id="defaultBank" value='<%= request.getAttribute("bank") %>'></INPUT>
        <INPUT type="hidden" name="defaultAcct" id="defaultAcct" value='<%= request.getAttribute("bankAcct") %>'></INPUT>
        <INPUT type="hidden" name="defaultPayment" id="defaultPayment" value='<%= request.getAttribute("paymentMethod") %>'></INPUT>
        <INPUT type="hidden" name="chqStatus" id="chqStatus" value='<%= request.getAttribute("chqStatus") %>'></INPUT>
        

       <jsp:include page="/web/jsp/ProcessBar.jsp"/>
        <div id="DivBlackOverlay" style="display: none; position: absolute; z-index: 50000; width: 100%; height: 100%; top: 0px; left: 0px; background-color: black; opacity: 0.5;"></div>

<div id="ApplySequenceContent" style="display: none; width: 1000px; height: 260px; overflow: auto; background: white; z-index: 50001; border: 2px solid #ff9c30; box-shadow: 0 0 20px #333; border-radius: 5px; position: absolute; margin: auto; top: 0; right: 0; bottom: 0; left: 0;">
       <div style="width: 100%;" align="center">
              <div style="width: 90%; overflow: auto; padding-top: 10px;" align="left">       
                <span class="LabelText" style="font-size: 9pt;"><%= Resource.getProperty("finance.chquece.applyAll", lang)%></span>                       
              </div>
              <div style="width: 98%; overflow: auto; padding-top: 20px;" align="center">
                    <table>
                      <TR>
                      <TD class="TitleCell" align="left" style="width:110px;"><span class="LabelText" ><%= Resource.getProperty("finance.cheque.applySeq.startingChequeSeqNo", lang)%></span>
                      </TD>
                      <TD class="TextBox_ContentCell">
                          <input type="text" name="inpStartingChequeSeqNo" id="inpStartingChequeSeqNo" maxlength="60" class="dojoValidateValid TextBox_TwoCells_width" onkeypress="return isNumberKey(event);" />
                      </TD>
                      
                       <TD class="TitleCell" align="left" style="width:110px;"><span class="LabelText" ><%= Resource.getProperty("finance.clearBankChqDate", lang)%></span>
                      </TD>
                      <TD class="TextBox_btn_ContentCell">
                          <table border="0" cellspacing="0" cellpadding="0" summary=""  style="padding-top: 0px;">
                            <tr>
                               <td class="TextBox_ContentCell">
                                   <input type="text" id="inpPopClearBankChqDate" class="dojoValidateValid TextBox_btn_OneCell_width " onkeydown="return true;" onchange="document.getElementById('messageBoxID').style.display = 'none';enableForm(this) " value="" maxlength="10" ></input> 
                               </td>
                           </tr>
                          </table>
                      </TD>
                  </TR>
                  <TR>
                     
                      <TD class="TitleCell" align="left" style="width:110px;"><span class="LabelText" ><%= Resource.getProperty("finance.cheque.applySeq.endingChequeSeqNo", lang)%></span>
                      </TD>
                      <TD class="TextBox_ContentCell">
                          <input type="text" name="inpEndingChequeSeqNo" id="inpEndingChequeSeqNo" maxlength="60" class="dojoValidateValid TextBox_TwoCells_width" onkeypress="return isNumberKey(event);" />
                      </TD>
                      
                      <TD class="TitleCell" align="left" style="width:110px;"><span class="LabelText" ><%= Resource.getProperty("finance.clearBank", lang)%></span>
                      </TD>
                      <TD class="TextBox_ContentCell">
                          <input type="text" name="inpPopClearBank" id="inpPopClearBank" maxlength="60" class="dojoValidateValid TextBox_TwoCells_width" />
                      </TD>
                  </TR>
                  
                  <TR>
                      <TD class="TitleCell" align="left" style="width:110px;"><span class="LabelText" ><%= Resource.getProperty("finance.cheque.applySeq.banksentdate", lang)%></span>
                      </TD>
                     <TD class="TextBox_btn_ContentCell">
                          <table border="0" cellspacing="0" cellpadding="0" summary=""  style="padding-top: 0px;">
                            <tr>
                               <td class="TextBox_ContentCell">
                                   <input type="text" id="inpBankSentdate" class="dojoValidateValid TextBox_btn_OneCell_width " onkeydown="return true;" onchange="document.getElementById('messageBoxID').style.display = 'none';enableForm(this) " value="" maxlength="10" ></input> 
                               </td>
                           </tr>
                          </table>
                      </TD>
                      
                      <TD class="TitleCell" align="left" style="width:110px;"><span class="LabelText" ><%= Resource.getProperty("finance.chequereceivedate", lang)%></span>
                      </TD>
                       <TD class="TextBox_btn_ContentCell">
                          <table border="0" cellspacing="0" cellpadding="0" summary=""  style="padding-top: 0px;">
                            <tr>
                               <td class="TextBox_ContentCell">
                                   <input type="text" id="inpChequeReceiveDate" class="dojoValidateValid TextBox_btn_OneCell_width " onkeydown="return true;" onchange="document.getElementById('messageBoxID').style.display = 'none';enableForm(this) " value="" maxlength="10" ></input> 
                               </td>
                           </tr>
                          </table>
                      </TD>
                  </TR>
                  <TR>
                    <TD class="TitleCell" align="left" style="width:110px;"><span class="LabelText" ><%= Resource.getProperty("finance.cheque.status", lang)%></span>
                    </TD>
                    <TD class="TextBox_btn_ContentCell">
                        <table border="0" cellspacing="0" cellpadding="0" summary=""  style="padding-top: 0px;">
                            <tr>
                                <td class="TextBox_ContentCell">
                                    <SELECT id="inpPopChequeStatus" name="inpPopChequeStatus" class="ComboKey Combo_TwoCells_width" onkeydown="return true;"  onchange="document.getElementById('messageBoxID').style.display = 'none';">
                                    </SELECT>                                                 
                                </td>
                            </tr>
                        </table>
                    </TD>
                  </TR>
                  <TR>
                      <TD colspan="4" align="center">
                        <div style="padding-top: 10px;">                                             
                           <BUTTON type="button" id="inpApplySeqRange" class="ButtonLink" onclick="applySeqRange();"  style="margin: 10px 0;">
                               <TABLE class="Button">
                                   <TR>
                                       <TD class="Button_left"><IMG class="Button_Icon Button_Icon_process" alt="ApplySeqRange" title="ApplySeqRange" src="../web/images/blank.gif" border="0"></IMG></TD>
                                       <TD class="Button_text" id="Apply_BTNname"><%= Resource.getProperty("finance.cheque.applySeq.apply",lang)%></TD>
                                       <TD class="Button_right"></TD>
                                   </TR>
                               </TABLE>
                           </BUTTON>
                           <BUTTON type="button" id="inpSeqPopClear" class="ButtonLink" onclick="applySequenceClear();" style="margin: 10px 0;">
                               <TABLE class="Button">
                                   <TR>
                                       <TD class="Button_left"><IMG class="Button_Icon Button_Icon_process" alt="Clear" title="Clear" src="../web/images/blank.gif" border="0"></IMG></TD>
                                       <TD class="Button_text" id="Clear_BTNname"><%= Resource.getProperty("finance.clear",lang)%></TD>
                                       <TD class="Button_right"></TD>
                                   </TR>
                               </TABLE>
                           </BUTTON>
                           <BUTTON type="button" id="inpBack" class="ButtonLink" onclick="applySequenceBack();" style="margin: 10px 0;">
                               <TABLE class="Button">
                                   <TR>
                                       <TD class="Button_left"><IMG class="Button_Icon Button_Icon_process" alt="Back" title="Back" src="../web/images/blank.gif" border="0"></IMG></TD>
                                       <TD class="Button_text" id="Back_BTNname"><%= Resource.getProperty("finance.cheque.applySeq.back",lang)%></TD>
                                       <TD class="Button_right"></TD>
                                   </TR>
                               </TABLE>
                           </BUTTON>
                       </div>
                       </TD> 
                   </TR>
        </table>
        </div>
        </div>
</div>

        <TABLE height="100%" border="0" cellpadding="0" cellspacing="0" id="main">
            <TR>
                <TD valign="top" id="tdleftTabs">
                    <table cellpadding="0" cellspacing="0" class="Main_ContentPane_LeftTabsBar" id="tdLeftTabsBars"></table>
                </TD>
                <TD valign="top">
                    <TABLE width="100%" border="0" cellspacing="0" cellpadding="0" class="Main_ContentPane_Left">
                      <TR><TD class="Main_NavBar_bg_left" id="tdleftSeparator"></TD></TR>
                      <TR><TD class="Main_ToolBar_bg_left" valign="top"></TD></TR>
                      <TR><TD class="Main_Client_bg_left"></TD></TR>
                      <TR><TD class="Main_Bottom_bg_left"></TD></TR>
                    </TABLE>
                </TD>
                <TD valign="top">
                    <DIV class="Main_ContentPane_NavBar" id="tdtopButtons"><TABLE class="Main_ContentPane_NavBar" id="tdtopNavButtons"></TABLE></DIV>
                    <DIV class="Main_ContentPane_ToolBar" id="paramToolBar">
                        <table class="Main_ContentPane_ToolBar Main_ToolBar_bg" id="tdToolBar">
                            <tr>
                                <td class="Main_ToolBar_Separator_cell" ><img src="../web/images/blank.gif" class="Main_ToolBar_Separator"></td>
                                <td width="2%" ><a href="javascript:void(0);" onClick="onClickRefresh()" class="Main_ToolBar_Button" onMouseOver="window.status='Reload Current Page';return true;" onMouseOut="window.status='';return true;" onclick="this.hideFocus=true" onblur="this.hideFocus=false" id="buttonRefresh"><img class="Main_ToolBar_Button_Icon Main_ToolBar_Button_Icon_Refresh" src="../web/images/blank.gif" border="0" id="linkButtonRefresh"></a></td> 
                                 <td class="Main_ToolBar_Separator_cell" ><img src="../web/images/blank.gif" class="Main_ToolBar_Separator"></td>
                                 <td class="Main_ToolBar_Space"></td>
                            </tr>
                        </table>
                    </DIV>
                    <TABLE width="100%" border="0" cellspacing="0" cellpadding="0" class="Main_ContentPane_TabPane" id="tdtopTabs">
                          <TR id="paramParentTabContainer"><td class="tabBackGroundInit"></td></TR>
                          <TR id="paramMainTabContainer"><td class="tabBackGroundInit"><span style="background: none;" class="tabTitle"></span></td></TR>
                          <tr><td class="tabBackGround">
                            <div class="marginLeft">
                            <div><span class="dojoTabcurrentfirst"><div>
                                <span><a class="dojoTabLink" href="javascript:void(0);" onclick="return false;" onMouseOver="return true;" onMouseOut="return true;"><%= Resource.getProperty("finance.clearbankcheque",lang)%></a></span>
                            </div></span></div>
                            </div>
                        </td></tr>
                    </TABLE>
                      
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
                                                                    <TABLE class="MessageBox_Top">
                                                                    <TBODY>
                                                                        <TR><TD>
                                                                                <TABLE style="cellpadding:0; cellspacing:0;" class="MessageBox_Body_ContentCell">
                                                                                    <TBODY>
                                                                                        <TR>
                                                                                            <TD class="MessageBox_Icon_ContentCell">
                                                                                                <DIV class="MessageBox_Icon"></DIV>
                                                                                            </TD>
                                                                                            <TD style="vertical-align: top;" id="messageBoxIDContent">
                                                                                                    <DIV class="MessageBox_TextTitle" id="messageBoxIDTitle"></DIV>
                                                                                                    <DIV class="MessageBox_TextDescription" id="messageBoxIDMessage"></DIV>
                                                                                                    <DIV class="MessageBox_TextSeparator"></DIV>
                                                                                            </TD>
                                                                                        </TR>
                                                                                    </TBODY>
                                                                                </TABLE>
                                                                            
                                                                            <div id="hideMessage">
                                                                                 <a style="color: yellow;font-size: 11.5px" href="javascript:void(0);" onClick="document.getElementById('messageBoxID').style.display = 'none';reSizeGrid();"><%=Resource.getProperty("finance.hide",lang)%></a>
                                                                            </div>
                                                                     </TD></TR>
                                                                     </TBODY>
                                                                     </TABLE>
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
                                         <div align="center">
                        <table><tbody>
                        
                        </tbody></table>
                        <div style="margin-bottom: 5px;"></div>
                    </div>
                                        <div id="LoadingContent" style="display:none;">
                                            <div style="text-align: center;margin-top: 17%;"><img alt="Loading" src="../web/images/loading.gif"></div>
                                            <div style="color:#000000;font-family: sans-serif;font-size:14px;margin-top:10px;text-align: center;"><%= Resource.getProperty("finance.loading", lang)%>... </div>
                                        </div>
                                        
                                        <div style="width:100%;">
                                            <div style="padding: 0 1%; width: 98%;">
                                                <div align="center" style="width:100%;" id="FormDetails">
                                                        <!-- /*<<<<<Your Code Here>>>>>*/ -->
                                   <TABLE style="width:90%; margin-top: 10px;">
                                    <TR>
                                         <TD class="TitleCell" align="left" style="width:110px;"><span class="LabelText" ><%= Resource.getProperty("finance.organization", lang)%></span>
                                        </TD>
                                        <TD class="TextBox_ContentCell">
                                            <SELECT id="inpOrg" name="inpOrg" class="ComboKey Combo_TwoCells_width" onkeydown="return true;"  onchange="document.getElementById('messageBoxID').style.display = 'none';">
                                            </SELECT>
                                        </TD>
                                        <TD class="TitleCell" align="left" style="width:150px;"><span class="LabelText" ><%= Resource.getProperty("finance.clearBankChqNo", lang)%></span>
                                        </TD>
                                        <TD class="TextBox_ContentCell">
                                            <input type="text" name="inpClearBankCqNo" id="inpClearBankCqNo" maxlength="60" class="dojoValidateValid TextBox_TwoCells_width" onkeydown="return true;" />
                                           </TD>
                                        <TD class="TitleCell" align="left" style="width:110px;"><span class="LabelText" ><%= Resource.getProperty("finance.reconciledDate", lang)%></span>
                                        </TD>
                                        <TD class="TextBox_btn_ContentCell">
                                            <table border="0" cellspacing="0" cellpadding="0" summary=""  style="padding-top: 0px;">
                                              <tr>
                                                 <td class="TextBox_ContentCell">
                                                     <input type="text" id="inpReconciledDate" class="dojoValidateValid TextBox_btn_OneCell_width " onkeydown="return true;" onchange="document.getElementById('messageBoxID').style.display = 'none';" value="" maxlength="10" ></input> 
                                                 </td>
                                             </tr>
                                            </table>
                                        </TD>
                                    </TR>
                                    <TR>
                                         <TD class="TitleCell" align="left" style="width:110px;"><span class="LabelText" ><%= Resource.getProperty("finance.bank", lang)%></span>
                                        </TD>
                                        <TD class="TextBox_ContentCell">
                                            <SELECT id="inpBank" name="inpBank" class="ComboKey Combo_TwoCells_width" onkeydown="return true;"  onchange="document.getElementById('messageBoxID').style.display = 'none';onChangeBank()">
                                            </SELECT>
                                        </TD>
                                        <TD class="TitleCell" align="left" style="width:110px;"><span class="LabelText" ><%= Resource.getProperty("finance.clearBankChqDate", lang)%></span>
                                        </TD>
                                        <TD class="TextBox_btn_ContentCell">
                                            <table border="0" cellspacing="0" cellpadding="0" summary=""  style="padding-top: 0px;">
                                              <tr>
                                                 <td class="TextBox_ContentCell">
                                                     <input type="text" id="inpClearBankCqDate" class="dojoValidateValid TextBox_btn_OneCell_width " onkeydown="return true;" onchange="document.getElementById('messageBoxID').style.display = 'none';" value="" maxlength="10" ></input> 
                                                 </td>
                                             </tr>
                                            </table>
                                        </TD>
                                        <TD class="TitleCell" align="left" style="width:110px;"><span class="LabelText" ><%= Resource.getProperty("finance.reconcileJournalNo", lang)%></span>
                                        </TD>
                                        <TD class="TextBox_ContentCell">
                                            <input type="text" name="inpReconcileNo" id="inpReconcileNo" maxlength="30" class="dojoValidateValid TextBox_TwoCells_width" onkeydown="return true;" />
                                        </TD>
                                    </TR>
                                    <TR>
                                        <TD class="TitleCell" align="left" style="width:110px;"><span class="LabelText" ><%= Resource.getProperty("finance.bankAcctNo", lang)%></span>
                                        </TD>
                                        <TD class="TextBox_ContentCell">
                                            <SELECT id="inpAcctNo" name="inpAcctNo" class="ComboKey Combo_TwoCells_width" onkeydown="return true;"  onchange="document.getElementById('messageBoxID').style.display = 'none';onChangeAccount()">
                                            </SELECT>
                                        </TD>
                                        <TD class="TitleCell" align="left" style="width:110px;"><span class="LabelText" ><%= Resource.getProperty("finance.clearBank", lang)%></span>
                                        </TD>
                                        <TD class="TextBox_ContentCell">
                                            <input type="text" name="inpClearBank" id="inpClearBank" maxlength="30" class="dojoValidateValid TextBox_TwoCells_width" onkeydown="return true;" />
                                        </TD>
                                        <TD class="TitleCell" align="left" style="width:110px;"><span class="LabelText" ><%= Resource.getProperty("finance.lockedIndicator", lang)%></span>
                                        </TD>
                                        <TD class="TextBox_btn_ContentCell">
                                            <div class="TitleCell" style="text-align: left;">
                                            <input type="checkbox" id="inpLock" name="inpLock" ></input>
                                            </div>
                                        </TD>
                                    </TR>
                                    <TR>
                                       <TD class="TitleCell" align="left" style="width:110px;"><span class="LabelText" ><%= Resource.getProperty("finance.paymentMethod", lang)%></span>
                                       </TD>
                                        <TD class="TextBox_ContentCell">
                                            <SELECT id="inpPaymentMethod" name="inpPaymentMethod" class="ComboKey Combo_TwoCells_width" onkeydown="return true;"  onchange="document.getElementById('messageBoxID').style.display = 'none';">
                                            </SELECT>
                                        </TD>
                                        <TD class="TitleCell" align="left" style="width:110px;"><span class="LabelText" ><%= Resource.getProperty("finance.sentBankDate", lang)%></span>
                                        </TD>
                                        <TD class="TextBox_btn_ContentCell">
                                            <table border="0" cellspacing="0" cellpadding="0" summary=""  style="padding-top: 0px;">
                                              <tr>
                                                 <td class="TextBox_ContentCell">
                                                     <input type="text" id="inpSentBankDate" class="dojoValidateValid TextBox_btn_OneCell_width " onkeydown="return true;" onchange="document.getElementById('messageBoxID').style.display = 'none';" value="" maxlength="10" ></input> 
                                                 </td>
                                             </tr>
                                            </table>
                                        </TD>
                                        <TD class="TitleCell" align="left" style="width:110px;"><span class="LabelText" ><%= Resource.getProperty("finance.cheque.status", lang)%></span>
                                        </TD>
                                        <TD class="TextBox_btn_ContentCell">
                                            <table border="0" cellspacing="0" cellpadding="0" summary=""  style="padding-top: 0px;">
                                              <tr>
                                                 <td class="TextBox_ContentCell">
                                                    <SELECT id="inpChequeStatus" name="inpChequeStatus" class="ComboKey Combo_TwoCells_width" onkeydown="return true;"  onchange="document.getElementById('messageBoxID').style.display = 'none';">
                                                    </SELECT>                                                 
                                                 </td>
                                             </tr>
                                            </table>
                                        </TD>
                                    </TR>
                                    <TR>
                                         <TD class="TitleCell" align="left" style="width:110px;"><span class="LabelText" ><%= Resource.getProperty("finance.fromPaymentSeq", lang)%></span>
                                        </TD>
                                        <TD class="TextBox_ContentCell">
                                            <input type="text" name="inpFromPaySeq" id="inpFromPaySeq" maxlength="30" class="dojoValidateValid TextBox_TwoCells_width" onkeydown="return true;" />
                                        </TD>
                                        <TD class="TitleCell" align="left" style="width:110px;"><span class="LabelText" ><%= Resource.getProperty("finance.fromPaymentDate", lang)%></span>
                                        </TD>
                                        <TD class="TextBox_btn_ContentCell">
                                            <table border="0" cellspacing="0" cellpadding="0" summary=""  style="padding-top: 0px;">
                                              <tr>
                                                 <td class="TextBox_ContentCell">
                                                     <input type="text" id="inpFromPayDate" class="dojoValidateValid TextBox_btn_OneCell_width " onkeydown="return true;" onchange="document.getElementById('messageBoxID').style.display = 'none';enableForm(this) " value="" maxlength="10" ></input> 
                                                 </td>
                                             </tr>
                                            </table>
                                        </TD>
                                    </TR>
                                    <TR>
                                         <TD class="TitleCell" align="left" style="width:110px;"><span class="LabelText" ><%= Resource.getProperty("finance.toPaymentSeq", lang)%></span>
                                        </TD>
                                        <TD class="TextBox_ContentCell">
                                            <input type="text" name="inpToPaySeq" id="inpToPaySeq" maxlength="30" class="dojoValidateValid TextBox_TwoCells_width" onkeydown="return true;" />
                                        </TD>
                                        <TD class="TitleCell" align="left" style="width:110px;"><span class="LabelText" ><%= Resource.getProperty("finance.toPaymentDate", lang)%></span>
                                        </TD>
                                        <TD class="TextBox_btn_ContentCell">
                                            <table border="0" cellspacing="0" cellpadding="0" summary=""  style="padding-top: 0px;">
                                              <tr>
                                                 <td class="TextBox_ContentCell">
                                                     <input type="text" id="inpToPayDate" class="dojoValidateValid TextBox_btn_OneCell_width " onkeydown="return true;" onchange="document.getElementById('messageBoxID').style.display = 'none';enableForm(this) " value="" maxlength="10" ></input> 
                                                 </td>
                                             </tr>
                                            </table>
                                        </TD>
                                    </TR>
                                    <TR>
                                        <TD colspan="6" align="center">
                                          <div>
                                             <BUTTON type="button" id="inpFind" class="ButtonLink" onclick="searchPaymentList();" style="margin: 10px 0;">
                                                 <TABLE class="Button">
                                                     <TR>
                                                         <TD class="Button_left"><IMG class="Button_Icon Button_Icon_process" alt="Submit" title="Submit" src="../web/images/blank.gif" border="0"></IMG></TD>
                                                         <TD class="Button_text" id="Find_BTNname"><%= Resource.getProperty("finance.search",lang)%></TD>
                                                         <TD class="Button_right"></TD>
                                                     </TR>
                                                 </TABLE>
                                             </BUTTON>
                                             <BUTTON type="button" id="inpApplySeq" class="ButtonLink" onclick="applySequence();" style="margin: 10px 0;">
                                                 <TABLE class="Button">
                                                     <TR>
                                                         <TD class="Button_left"><IMG class="Button_Icon Button_Icon_process" alt="ApplySeq" title="ApplySeq" src="../web/images/blank.gif" border="0"></IMG></TD>
                                                         <TD class="Button_text" id="Apply_BTNname"><%= Resource.getProperty("finance.cheque.applySeq",lang)%></TD>
                                                         <TD class="Button_right"></TD>
                                                     </TR>
                                                 </TABLE>
                                             </BUTTON>
                                             <BUTTON type="button" id="inpClear" class="ButtonLink" onclick="clearvalues();" style="margin: 10px 0;">
                                                 <TABLE class="Button">
                                                     <TR>
                                                         <TD class="Button_left"><IMG class="Button_Icon Button_Icon_process" alt="Clear" title="Clear" src="../web/images/blank.gif" border="0"></IMG></TD>
                                                         <TD class="Button_text" id="Clear_BTNname"><%= Resource.getProperty("finance.clear",lang)%></TD>
                                                         <TD class="Button_right"></TD>
                                                     </TR>
                                                 </TABLE>
                                             </BUTTON>
                                         </div>
                                         </TD> 
                                     </TR>
                                     
                                        
                                  <!--   </TABLE>
                                  </DIV>    -->
                                                          
                                                            
                                     
                                                     
                                                     </TABLE>
                                        <div id="LoadingContent" style="display:none;">
                                            <div style="text-align: center;margin-top: 17%;"><img alt="Loading" src="../web/images/loading.gif"></div>
                                            <div style="color:#000000;font-family: sans-serif;font-size:14px;margin-top:10px;text-align: center;"><%= Resource.getProperty("finance.loading", lang)%>... </div>
                                        </div>
                                        <div id="paymentgrid" style="width:100%;">
                                            <div align="center"><table id="PaymentList" class="scroll" cellpadding="0" cellspacing="0" width="100%"></table><div id="pager" class="scroll" style="text-align: center;"></div></div>
                                        </div>
                                        <div style="width:100%;">
                                        <table>
                                        <TR>
                                        <TD colspan="6" align="center">
                                          <div>
                                             <BUTTON type="button" id="inpLock" class="ButtonLink" onclick="lock();" style="margin: 10px 0;">
                                                 <TABLE class="Button">
                                                     <TR>
                                                         <TD class="Button_left"><IMG class="Button_Icon Button_Icon_process" alt="Lock" title="Lock" src="../web/images/blank.gif" border="0"></IMG></TD>
                                                         <TD class="Button_text" id="Lock_BTNname"><%= Resource.getProperty("finance.cheque.lock",lang)%></TD>
                                                         <TD class="Button_right"></TD>
                                                     </TR>
                                                 </TABLE>
                                             </BUTTON>
                                             <BUTTON type="button" id="inpUnlock" class="ButtonLink" onclick="unlock();" style="margin: 10px 0;">
                                                 <TABLE class="Button">
                                                     <TR>
                                                         <TD class="Button_left"><IMG class="Button_Icon Button_Icon_process" alt="UnLock" title="UnLock" src="../web/images/blank.gif" border="0"></IMG></TD>
                                                         <TD class="Button_text" id="Unlock_BTNname"><%= Resource.getProperty("finance.cheque.unlock",lang)%></TD>
                                                         <TD class="Button_right"></TD>
                                                     </TR>
                                                 </TABLE>
                                             </BUTTON>
                                             <BUTTON type="button" id="inpSave" class="ButtonLink" onclick="save();" style="margin: 10px 0;">
                                                 <TABLE class="Button">
                                                     <TR>
                                                         <TD class="Button_left"><IMG class="Button_Icon Button_Icon_process" alt="Save" title="Save" src="../web/images/blank.gif" border="0"></IMG></TD>
                                                         <TD class="Button_text" id="Save_BTNname"><%= Resource.getProperty("finance.save",lang)%></TD>
                                                         <TD class="Button_right"></TD>
                                                     </TR>
                                                 </TABLE>
                                             </BUTTON>
                                             <BUTTON type="button" id="inpSaveAll" class="ButtonLink" onclick="saveAll();" style="margin: 10px 0; display: none;">
                                                 <TABLE class="Button">
                                                     <TR>
                                                         <TD class="Button_left"><IMG class="Button_Icon Button_Icon_process" alt="SaveAll" title="SaveAll" src="../web/images/blank.gif" border="0"></IMG></TD>
                                                         <TD class="Button_text" id="Save_BTNname"><%= Resource.getProperty("finance.saveAll",lang)%></TD>
                                                         <TD class="Button_right"></TD>
                                                     </TR>
                                                 </TABLE>
                                             </BUTTON>
                                             <BUTTON type="button" id="inpClearTmp" class="ButtonLink" onclick="clearTemp();" style="margin: 10px 0;">
                                                 <TABLE class="Button">
                                                     <TR>
                                                         <TD class="Button_left"><IMG class="Button_Icon Button_Icon_process" alt="Clear" title="Clear" src="../web/images/blank.gif" border="0"></IMG></TD>
                                                         <TD class="Button_text" id="Save_BTNname"><%= Resource.getProperty("finance.clear",lang)%></TD>
                                                         <TD class="Button_right"></TD>
                                                     </TR>
                                                 </TABLE>
                                             </BUTTON>
                                         </div>
                                         </TD> 
                                        </TR>
                                        </div>
                                        </table>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    </TD></TR>
                                </TABLE>
                            </TD></TR>
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
<script type="text/javascript" src="../web/js/jquery/grid.locale-en.js"></script>
<script type="text/javascript" src="../web/js/jquery/jquery.jqGrid.min.js"></script>
<script type="text/javascript" src="../web/js/common/select2.min.js"></script>

<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.plugin.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.ummalqura.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.ummalqura-ar.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.plus.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.picker.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.picker.ext.js"></script>
<script type="text/javascript">
//Main Functionss
var contextPath = '<%=request.getContextPath()%>';
var todaydate = '<%= request.getAttribute("today") %>';
var saveRecord = '<%= Resource.getProperty("finance.cheque.saveRecord",lang)%>';
var saveAllRecord = '<%= Resource.getProperty("finance.cheque.saveAllRecord",lang)%>';
var sureLock = '<%= Resource.getProperty("finance.cheque.sureLock",lang)%>';
var sureUnLock = '<%= Resource.getProperty("finance.cheque.sureUnLock",lang)%>';
var clearRecord = '<%= Resource.getProperty("finance.cheque.clearChanges",lang)%>';
var filterCriteria = {};


var defaultOrg = document.getElementById("defaultOrg").value;
var jsonOrg = JSON &&JSON.parse(defaultOrg) || $.parseJSON(defaultOrg);
if(jsonOrg.recordIdentifier) {
    var setOrg = new Option(jsonOrg.recordIdentifier,jsonOrg.id, true, true);
    $('#inpOrg').append(setOrg);  
}

var defaultBank = document.getElementById("defaultBank").value;
var jsonbank = JSON &&JSON.parse(defaultBank) || $.parseJSON(defaultBank);
if(jsonbank.recordIdentifier) {
  var setBank = new Option(jsonbank.recordIdentifier,jsonbank.id, true, true);
  $('#inpBank').append(setBank);  
}

//select 2 dropdown
function initiateSelect2Org() {
    setTimeout(function () {
          $("#inpOrg").select2(selectBoxAjaxPaging({
            url: function () {
              return '<%=request.getContextPath()%>/ClearChequeAjax?action=getOrg'
            },
            size: "small"
          }));
          $("#inpOrg").on("select2:unselecting", function (e) {
            document.getElementById("inpOrg").options.length = 0;
          });
        }, 100);
}

function initiateSelect2Bank() {
setTimeout(function () {
  $("#inpBank").select2(selectBoxAjaxPaging({
    url: function () {
      return '<%=request.getContextPath()%>/ClearChequeAjax?action=getBank'
    },
    size: "small"
  }));
  $("#inpBank").on("select2:unselecting", function (e) {
    $("#inpBank").empty();
    $("#inpAcctNo").empty();
    $("#inpPaymentMethod").empty();
  });
  onChangeBank();
}, 100);
}

function initiateSelect2Acct() {
setTimeout(function () {
  $("#inpAcctNo").select2(selectBoxAjaxPaging({
    url: function () {
      return '<%=request.getContextPath()%>/ClearChequeAjax?action=getAccountNo&bankId=' + document.getElementById("inpBank").value
    },
    size: "small"
  }));
  $("#inpAcctNo").on("select2:unselecting", function (e) {
        $("#inpAcctNo").empty();
        $("#inpPaymentMethod").empty();
  });
  //document.getElementById("select2-inpAcctNo-container").style.backgroundColor = "#F5F7F1";
}, 100);
}

function initiateSelect2PaymentMethod() {
    setTimeout(function () {
      var orgId = '00';
      $("#inpPaymentMethod").select2(selectBoxAjaxPaging({
        url: function () {
          return '<%=request.getContextPath()%>/ClearChequeAjax?action=getPaymentMethod&bankId=' + document.getElementById("inpBank").value + '&acctId='+document.getElementById("inpAcctNo").value
        },
        size: "small"
      }));
      $("#inpPaymentMethod").on("select2:unselecting", function (e) {
            $("#inpPaymentMethod").empty();
      });
    }, 100);
    }
    
function initiateSelect2ChequeStatus() {
    setTimeout(function () {
      $("#inpChequeStatus").select2(selectBoxAjaxPaging({
        url: function () {
          return '<%=request.getContextPath()%>/ClearChequeAjax?action=getChequeStatus'
        },
        size: "small"
      }));
      $("#inpChequeStatus").on("select2:unselecting", function (e) {
            $("#inpChequeStatus").empty();
      });
      document.getElementById("select2-inpChequeStatus-container").style.backgroundColor = "#F5F7F1";
    }, 100);
    }
    
function initiateSelect2PopChequeStatus() {
    setTimeout(function () {
      $("#inpPopChequeStatus").select2(selectBoxAjaxPaging({
        url: function () {
          return '<%=request.getContextPath()%>/ClearChequeAjax?action=getChequeStatus'
        },
        size: "small"
      }));
      $("#inpPopChequeStatus").on("select2:unselecting", function (e) {
            $("#inpPopChequeStatus").empty();
      });
      document.getElementById("select2-inpPopChequeStatus-container").style.backgroundColor = "#F5F7F1";
    }, 100);
    }
    


initiateSelect2Org();
initiateSelect2Bank();
initiateSelect2Acct();
initiateSelect2PaymentMethod();
initiateSelect2ChequeStatus();
initiateSelect2PopChequeStatus();

function onChangeBank(){
    var bankId = $('#inpBank').val();
    $("#inpAcctNo").empty();
    $("#inpPaymentMethod").empty();
    if(bankId && bankId.length===32) {
    $.post('<%=request.getContextPath()%>/ClearChequeAjax', {
        action: 'getAcctPayMethod',
        inpBank: bankId,
        inpAcctNo: ''
      }, function (jsonObj) {
          if (jsonObj.acctId) {
              var setAcct = new Option(jsonObj.acctNo,jsonObj.acctId, true, true);
              $('#inpAcctNo').append(setAcct);
          }
          if (jsonObj.paymentId) {
                var setPayment = new Option(jsonObj.paymentName,jsonObj.paymentId, true, true);
             $('#inpPaymentMethod').append(setPayment);
             }
         initiateSelect2Acct();
         initiateSelect2PaymentMethod();
      });
    } else {
        initiateSelect2Acct();
        initiateSelect2PaymentMethod();
    }
}

function onChangeAccount(){
    var bankId = $('#inpBank').val();
    var acctNo = $('#inpAcctNo').val();
    $("#inpPaymentMethod").empty();
    if(acctNo && acctNo.length===32) {
    $.post('<%=request.getContextPath()%>/ClearChequeAjax', {
        action: 'getAcctPayMethod',
        inpBank: bankId,
        inpAcctNo: acctNo
      }, function (jsonObj) {
          if (jsonObj.paymentId) {
            var setPayment = new Option(jsonObj.paymentName,jsonObj.paymentId, true, true);
            $('#inpPaymentMethod').append(setPayment); 
          }
          initiateSelect2PaymentMethod();
      });
    } else {
        initiateSelect2PaymentMethod();
    }
}

//calender js
$('#inpReconciledDate').calendarsPicker({
  calendar: $.calendars.instance('ummalqura'),
  dateFormat: 'dd-mm-yyyy',
  showTrigger: '<img src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/calendar.gif" alt="Popup" class="trigger">'
});

$('#inpClearBankCqDate').calendarsPicker({
  calendar: $.calendars.instance('ummalqura'),
  dateFormat: 'dd-mm-yyyy',
  showTrigger: '<img src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/calendar.gif" alt="Popup" class="trigger">'
});

$('#inpSentBankDate').calendarsPicker({
  calendar: $.calendars.instance('ummalqura'),
  dateFormat: 'dd-mm-yyyy',
  showTrigger: '<img src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/calendar.gif" alt="Popup" class="trigger">'
});

$('#inpFromPayDate').calendarsPicker({
  calendar: $.calendars.instance('ummalqura'),
  dateFormat: 'dd-mm-yyyy',
  showTrigger: '<img src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/calendar.gif" alt="Popup" class="trigger">'
});

$('#inpToPayDate').calendarsPicker({
  calendar: $.calendars.instance('ummalqura'),
  dateFormat: 'dd-mm-yyyy',
  showTrigger: '<img src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/calendar.gif" alt="Popup" class="trigger">'
});

$('#inpPopClearBankChqDate').calendarsPicker({
  calendar: $.calendars.instance('ummalqura'),
  dateFormat: 'dd-mm-yyyy',
  showTrigger: '<img src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/calendar.gif" alt="Popup" class="trigger">'
});

$('#inpPopClearBankChqDate').calendarsPicker({
  calendar: $.calendars.instance('ummalqura'),
  dateFormat: 'dd-mm-yyyy',
  showTrigger: '<img src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/calendar.gif" alt="Popup" class="trigger">'
});

$('#inpChequeReceiveDate').calendarsPicker({
  calendar: $.calendars.instance('ummalqura'),
  dateFormat: 'dd-mm-yyyy',
  showTrigger: '<img src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/calendar.gif" alt="Popup" class="trigger">'
});
$('#inpBankSentdate').calendarsPicker({
  calendar: $.calendars.instance('ummalqura'),
  dateFormat: 'dd-mm-yyyy',
  showTrigger: '<img src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/calendar.gif" alt="Popup" class="trigger">'
});
$("#inpLock").change(function () {
  if ($(this).attr('checked')) {
    document.getElementById('inpLockH').value = 'Y';
  } else {
    document.getElementById('inpLockH').value = 'N';
  }
});

//jqgrid formation
var paymentGrid = jQuery("#PaymentList");
var jsonPaymentList = {};
jsonPaymentList.List = [];
var gGridIdList, gGridIdListCount = 0;
var lang = '<%=lang%>',
    direction = 'ltr';
if (lang == "ar_SA") {
  direction = 'rtl';
}

jQuery(function () {
paymentGrid.jqGrid({
  direction: direction,
  url: '<%=request.getContextPath()%>/ClearChequeAjax?action=GetPaymentList',
  colNames: ['<%= Resource.getProperty("finance.cheque.paydocseqNo", lang) %>', '<%= Resource.getProperty("finance.sentBankDate", lang) %>', '<%= Resource.getProperty("finance.chequereceivedate", lang) %>', '<%= Resource.getProperty("finance.cheque.payDocNo", lang) %>', '<%= Resource.getProperty("finance.cheque.paymentDate", lang) %>', '<%= Resource.getProperty("finance.clearBankChqNo", lang) %>', '<%= Resource.getProperty("finance.clearBankChqDate", lang) %>','<%= Resource.getProperty("finance.cheque.status", lang) %>', '<%= Resource.getProperty("finance.clearBank", lang) %>', '<%= Resource.getProperty("finance.reconciledDate", lang) %>', '<%= Resource.getProperty("finance.reconcileJournalNo", lang) %>', '<%= Resource.getProperty("finance.lockedIndicator", lang) %>', ''],
  colModel: [{
    name: 'paydocSeqNo',
    index: 'paydocSeqNo',
    search: false,
    width: 110
  }, {
    name: 'sentBankDate',
    index: 'sentBankDate',
    search: false,
    formatter: formatSentDate,
    width: 90
  }, {
    name: 'chequereceivedate',
    index: 'chequereceivedate',
    search: false,
    formatter: formatReceiveDate,
    width: 90
  }, {
    name: 'paydocNo',
    index: 'paydocNo',
    search: false,
    width: 110
  }, {
    name: 'paymentDate',
    index: 'paymentDate',
    search: false,
    width: 80
  }, {
    name: 'clearBankChqNo',
    index: 'clearBankChqNo',
    search: false,
    formatter: formatChequeNo,
    width: 160
  }, {
    name: 'clearBankChqDate',
    index: 'clearBankChqDate',
    search: false,
    formatter: formatChequeDate,
    width: 90
  },{
     name: 'chequeStatus',
     index: 'chequeStatus',
     search: false,
     formatter: customSelectBox,
     width: 150
  },{
    name: 'clearBank',
    index: 'clearBank',
    search: false,
    formatter: formatBank,
    width: 180
  }, {
    name: 'reconciledate',
    index: 'reconciledate',
    search: false,
    width: 80
  }, {
    name: 'reconcileNo',
    index: 'reconcileNo',
    search: false,
    width: 120
  }, {
    name: 'lockedIndicatorValue',
    index: 'lockedIndicatorValue',
    search: false,
    formatter: formatLockedIndicator,
    width: 50
  }, {
    name: 'lockedIndicator',
    index: 'lockedIndicator',
    search: false,
    hidden: true
  }],
  rowNum: 50,
  mtype: 'POST',
  rowList: [20, 50, 100, 200, 500],
  pager: '#pager',
  sortname: 'paydocNo',
  datatype: 'local',
  rownumbers: true,
  viewrecords: true,
  multiselect: true,
  sortorder: "asc",
  ignoreCase: true,
  headertitles: true,
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
  beforeRequest: function () {
    paymentGrid.setPostDataItem("inpOrgId", filterCriteria.inpOrgId);
    paymentGrid.setPostDataItem("inpPaymentMethod", filterCriteria.inpPaymentMethod);
    paymentGrid.setPostDataItem("inpBank", filterCriteria.inpBank);
    paymentGrid.setPostDataItem("inpAcctNo", filterCriteria.inpAcctNo);
    paymentGrid.setPostDataItem("inpFromPaySeq", filterCriteria.inpFromPaySeq);
    paymentGrid.setPostDataItem("inpToPaySeq", filterCriteria.inpToPaySeq);
    paymentGrid.setPostDataItem("inpClearBankCqNo", filterCriteria.inpClearBankCqNo);
    paymentGrid.setPostDataItem("inpClearBankCqDate", filterCriteria.inpClearBankCqDate);
    paymentGrid.setPostDataItem("inpClearBank", filterCriteria.inpClearBank);
    paymentGrid.setPostDataItem("inpSentBankDate", filterCriteria.inpSentBankDate);
    paymentGrid.setPostDataItem("inpFromPayDate", filterCriteria.inpFromPayDate);
    paymentGrid.setPostDataItem("inpToPayDate", filterCriteria.inpToPayDate);
    paymentGrid.setPostDataItem("inpReconciledDate", filterCriteria.inpReconciledDate);
    paymentGrid.setPostDataItem("inpReconcileNo", filterCriteria.inpReconcileNo);
    paymentGrid.setPostDataItem("inpLock", filterCriteria.inpLock);
    paymentGrid.setPostDataItem("inpChequeStatus", filterCriteria.inpChequeStatus);
    paymentGrid.setPostDataItem("_search", 'true');
    paymentGrid.setPostDataItem("getAllRecords", 'false');
  },
  beforeSelectRow: function (id, status) {
    return true;
  },
  onSelectRow: function (id, status) {
    if (status) {
      selectGridRow(id, true);
    } else {
      unselectGridRow(id, true);
    }
  },
  onSelectAll: function (id, status) {
    var ids = "" + id,
        idList = ids.split(","),
        i = 0;
    if (jQuery.inArray(",", idList) == 0) {
      i = 1;
    }
    for (; i < idList.length; i++) {
      if (status == true) {
        selectGridRow(idList[i], true);
      } else if (status == false) {
        unselectGridRow(idList[i], true);
      }
    }
    if (parseInt(paymentGrid.getGridParam("records")) > gGridIdListCount) {
      if (status == true) {
        OBAsk('<%= Resource.getProperty("finance.cheque.selectAll", lang) %>', function (r) {
          if (r) {
            showProcessBar(true, 2);
            $.post('<%=request.getContextPath()%>/ClearChequeAjax', {
              action: 'GetPaymentList',
              getAllRecords: true,
              inpOrgId: filterCriteria.inpOrgId,
              inpPaymentMethod: filterCriteria.inpPaymentMethod,
              inpBank: filterCriteria.inpBank,
              inpAcctNo: filterCriteria.inpAcctNo,
              inpFromPaySeq: filterCriteria.inpFromPaySeq,
              inpToPaySeq: filterCriteria.inpToPaySeq,
              inpClearBankCqNo: filterCriteria.inpClearBankCqNo,
              inpClearBankCqDate: filterCriteria.inpClearBankCqDate,
              inpClearBank: filterCriteria.inpClearBank,
              inpSentBankDate: filterCriteria.inpSentBankDate,
              inpFromPayDate: filterCriteria.inpFromPayDate,
              inpToPayDate: filterCriteria.inpToPayDate,
              inpReconciledDate: filterCriteria.inpReconciledDate,
              inpReconcileNo: filterCriteria.inpReconcileNo,
              inpLock: filterCriteria.inpLock,
              inpChequeStatus: filterCriteria.inpChequeStatus,
              rows: 0,
              page: 0,
              sidx: 'paydocNo',
              sord: 'asc',
              _search: 'true'
            }, function (jsonObj) {
              var data = jsonObj.rows;
              for (var i in data) {
                selectGridRow(data[i].id, false);
              }
              showProcessBar(false);
            });
          }
        });
      } else if (status == false) {
        OBAsk('<%= Resource.getProperty("finance.cheque.deSelectAll", lang) %>', function (r) {
          if (r) {
            showProcessBar(true, 2);
            $.post('<%=request.getContextPath()%>/ClearChequeAjax', {
                action: 'GetPaymentList',
                getAllRecords: true,
                inpOrgId: filterCriteria.inpOrgId,
                inpPaymentMethod: filterCriteria.inpPaymentMethod,
                inpBank: filterCriteria.inpBank,
                inpAcctNo: filterCriteria.inpAcctNo,
                inpFromPaySeq: filterCriteria.inpFromPaySeq,
                inpToPaySeq: filterCriteria.inpToPaySeq,
                inpClearBankCqNo: filterCriteria.inpClearBankCqNo,
                inpClearBankCqDate: filterCriteria.inpClearBankCqDate,
                inpClearBank: filterCriteria.inpClearBank,
                inpSentBankDate: filterCriteria.inpSentBankDate,
                inpFromPayDate: filterCriteria.inpFromPayDate,
                inpToPayDate: filterCriteria.inpToPayDate,
                inpReconciledDate: filterCriteria.inpReconciledDate,
                inpReconcileNo: filterCriteria.inpReconcileNo,
                inpLock: filterCriteria.inpLock,
                inpChequeStatus: filterCriteria.inpChequeStatus,
                rows: 0,
                page: 0,
                sidx: 'paydocNo',
                sord: 'asc',
                _search: 'true'
              }, function (jsonObj) {
                var data = jsonObj.rows;
                for (var i in data) {
                  unselectGridRow(data[i].id, true);
                }
                showProcessBar(false);
              });
            }
          });
        }
      }
    },
    loadComplete: function () {
      var selectedRowObj, selectedRows = jsonPaymentList.List;
      gGridIdList = paymentGrid.getDataIDs();
      gGridIdListCount = gGridIdList.length;
      for (selectedRowObj in selectedRows) {
        selectPaymentGridRow(selectedRows[selectedRowObj].id);
      }
      $('input[role*="checkbox"]').on("click", function () {
        selectPaymentGridRow($(this).parent().parent().attr('id'));
      });
      ChangeJQGridAllRowColor(paymentGrid);
      $("#paymentgrid").show();
      //$('#cb_PaymentList').hide();
      reSizeGrid();
    }
  });
  paymentGrid.jqGrid('navGrid', '#pager', {
    edit: false,
    add: false,
    del: false,
    search: false,
    view: false,
    refresh: true,
    beforeRefresh: function () {
      jsonPaymentList.List = [];
    }
  }, {}, {}, {}, {});
  changeJQGridDisplay("PaymentList", "inpPaymentGridId");
  reSizeGrid();
});

function selectGridRow(id, editFields) {
  ChangeJQGridSelectMultiRowColor(id, 'S');
  var rowData = paymentGrid.getRowData(id);
  var canAdd = true;
  var jsonStr = JSON.stringify(jsonPaymentList);
  if (jsonStr.indexOf(id) >= 0) {
    canAdd = false;
  }
  if (canAdd) {
    var len = jsonPaymentList.List.length;
    jsonPaymentList.List[len] = {};
    jsonPaymentList.List[len]['id'] = id;
  }
  var selectedIds = "" + paymentGrid.jqGrid('getGridParam', 'selarrrow'), selectedIdList = selectedIds.split(",").filter(function(v) { return v; }), selectedIdListLength = selectedIdList.length;
  if (selectedIds.indexOf(",") == 0) {
    selectedIdListLength--;
  }
  if (selectedIdListLength == gGridIdListCount) {
    document.getElementById("cb_PaymentList").checked = true;
  } else {
    document.getElementById("cb_PaymentList").checked = false;
  }
  if (!editFields) {
    return false;
  }
  if (rowData.lockedIndicator === 'Y') {
    return false;
  }
  var chqNo = $('#chequeNo_' + id);
  var bank = $('#bank_' + id);
  var chqDate = $('#chequeDate_' + id);
  var sentDate = $('#sentDate_' + id);
  var receiveDate = $('#receiveDate_' + id);
  var chqStatus = $('#inpChqStatus_' + id);
  
  jqueryEnableTextBox(chqNo, true, false);
  jqueryEnableTextBox(bank, true, false);
  jqueryEnableTextBox(chqDate, true, false);
  jqueryEnableTextBox(sentDate, true, false);
  jqueryEnableTextBox(receiveDate, true, false);
  jqueryEnableTextBox(chqStatus, true, false);

  chqNo.on("change", function () {
    var id = ($(this).attr('id')).replace('chequeNo_', '');
    findAndUpdateJSONObj(jsonPaymentList.List, 'id', id, 'chequeNo', $(this).val());
  });
  bank.on("change", function () {
    var id = ($(this).attr('id')).replace('bank_', '');
    findAndUpdateJSONObj(jsonPaymentList.List, 'id', id, 'bank', $(this).val());
  });
  chqDate.on("change", function () {
    var id = ($(this).attr('id')).replace('chequeDate_', '');
    findAndUpdateJSONObj(jsonPaymentList.List, 'id', id, 'chequeDate', $(this).val());
  });
  sentDate.on("change", function () {
    var id = ($(this).attr('id')).replace('sentDate_', '');
    findAndUpdateJSONObj(jsonPaymentList.List, 'id', id, 'sentDate', $(this).val());
  });
  receiveDate.on("change", function () {
    var id = ($(this).attr('id')).replace('receiveDate_', '');
    findAndUpdateJSONObj(jsonPaymentList.List, 'id', id, 'receiveDate', $(this).val());
  });
  chqStatus.on("change", function () {
        var id = ($(this).attr('id')).replace('inpChqStatus_', '');
        findAndUpdateJSONObj(jsonPaymentList.List, 'id', id, 'ChqStatus', $(this).val());
  });
  $('#chequeDate_' + id).calendarsPicker({
    calendar: $.calendars.instance('ummalqura'),
    dateFormat: 'dd-mm-yyyy',
    onSelect: function () {
      var id = ($(this).attr('id')).replace('chequeDate_', '');
      findAndUpdateJSONObj(jsonPaymentList.List, 'id', id, 'chequeDate', $(this).val());
    }
  });
  $('#sentDate_' + id).calendarsPicker({
    calendar: $.calendars.instance('ummalqura'),
    dateFormat: 'dd-mm-yyyy',
    onSelect: function () {
      var id = ($(this).attr('id')).replace('sentDate_', '');
      findAndUpdateJSONObj(jsonPaymentList.List, 'id', id, 'sentDate', $(this).val());
    }
  });
  $('#receiveDate_' + id).calendarsPicker({
    calendar: $.calendars.instance('ummalqura'),
    dateFormat: 'dd-mm-yyyy',
    onSelect: function () {
      var id = ($(this).attr('id')).replace('receiveDate_', '');
      findAndUpdateJSONObj(jsonPaymentList.List, 'id', id, 'receiveDate', $(this).val());
    }
  });
}

function unselectGridRow(id, editFields) {
  ChangeJQGridSelectMultiRowColor(id, 'US');
  findAndRemoveJSONObj(jsonPaymentList.List, 'id', id);
  document.getElementById("cb_PaymentList").checked = false;
  if (!editFields) {
    return false;
  }
  var chqNo = $('#chequeNo_' + id);
  var bank = $('#bank_' + id);
  var chqDate = $('#chequeDate_' + id);
  var sentDate = $('#sentDate_' + id);
  var receiveDate = $('#receiveDate_' + id);
  var chqStatus = $('#inpChqStatus_' + id);

  jqueryEnableTextBox(chqNo, false, false);
  jqueryEnableTextBox(bank, false, false);
  jqueryEnableTextBox(chqDate, false, false);
  jqueryEnableTextBox(sentDate, false, false);
  jqueryEnableTextBox(receiveDate, false, false);
  jqueryEnableTextBox(chqStatus, false, false);
}

function reSizeGrid() {
  var w, h;
  if (window.innerWidth) {
    w = window.innerWidth - 66;
    h = window.innerHeight - 300;
  } else if (document.body) {
    w = document.body.clientWidth - 66;
    h = document.body.clientHeight - 300;
  }
  if (w <= 1000) w = 1000;
  if (h <= 250) h = 250;
  paymentGrid.setGridWidth(w, true);
  paymentGrid.setGridHeight(440, true);
  ChangeJQGridAllRowColor(paymentGrid);
}

function formatChequeNo(cellVal, opt, rowObj) {
  var html, tempVal = findAndReturnJSONValue(jsonPaymentList.List, 'id', opt.rowId, 'chequeNo');
  if (tempVal && tempVal != 'undefined' && tempVal != '' && tempVal != null) {
    cellVal = tempVal;
  }
  html = "<div title='' style='display:inline-block; position:relative;' class='JQGridRowInputBox'>";
  html += "<input type='text' role='jqtextbox' id='chequeNo_" + opt.rowId + "' ";
  html += "name='chequeNo_" + opt.rowId + "'" + " class='dojoValidateValid readonly BorderWidth0' disabled = 'disabled' maxlength='60' ";
  if (cellVal && cellVal != 'undefined' && cellVal != '' && cellVal != null) {
    html += "value='" + cellVal + "' ";
  }
  html += "onkeydown='return isFloatOnKeyDown(this, event)' ";
  html += "onfocus='if(parseFloat(this.value)==0){this.value=\"\";}this.select();' ";
  html += "onblur='numberInputEvent(\"onblur\", this);' ";
  html += "style='text-align:left; width: 100%;' outputformat='qtyEdition'></input>";
  html += "<div style='position:absolute; left:0; right:0; top:0; bottom:0;' class='JQGridRowInputBoxOverlay'></div>";
  html += "</div>";
  return html;
}

function formatBank(cellVal, opt, rowObj) {
  var html, tempVal = findAndReturnJSONValue(jsonPaymentList.List, 'id', opt.rowId, 'bank');
  if (tempVal && tempVal != 'undefined' && tempVal != '' && tempVal != null) {
    cellVal = tempVal;
  }
  html = "<div title='' style='display:inline-block; position:relative; width:100%;' class='JQGridRowInputBox' >";
  html += "<input type='text' role='jqtextbox' id='bank_" + opt.rowId + "' ";
  html += "name='bank_" + opt.rowId + "'" + " class='dojoValidateValid readonly BorderWidth0' disabled = 'disabled' maxlength='60' ";
  if (cellVal && cellVal != 'undefined' && cellVal != '' && cellVal != null) {
    html += "value='" + cellVal + "' ";
  }
  html += "style='text-align:left; width: 100%;'></input>";
  html += "<div style='position:absolute; left:0; right:0; top:0; bottom:0;' class='JQGridRowInputBoxOverlay'></div>";
  html += "</div>";
  return html;
}

function formatChequeDate(cellVal, opt, rowObj) {
  var html, tempVal = findAndReturnJSONValue(jsonPaymentList.List, 'id', opt.rowId, 'chequeDate');
  if (tempVal && tempVal != 'undefined' && tempVal != '' && tempVal != null) {
    cellVal = tempVal;
  }
  html = "<div title='' style='display:inline-block; position:relative;' class='JQGridRowInputBox'>";
  html += "<input type='text' role='jqtextbox' id='chequeDate_" + opt.rowId + "' ";
  html += "name='chequeDate_" + opt.rowId + "'" + " class='dojoValidateValid readonly BorderWidth0' disabled = 'disabled' ";
  if (cellVal && cellVal != 'undefined' && cellVal != '' && cellVal != null) {
    html += "value='" + cellVal + "' ";
  }
  html += "style='text-align:left; width: 100%;'></input>";
  html += "<div style='position:absolute; left:0; right:0; top:0; bottom:0;' class='JQGridRowInputBoxOverlay'></div>";
  html += "</div>";
  return html;
}

function formatSentDate(cellVal, opt, rowObj) {
  var html, tempVal = findAndReturnJSONValue(jsonPaymentList.List, 'id', opt.rowId, 'sentDate');
  if (tempVal && tempVal != 'undefined' && tempVal != '' && tempVal != null) {
    cellVal = tempVal;
  }
  html = "<div title='' style='display:inline-block; position:relative;' class='JQGridRowInputBox'>";
  html += "<input type='text' role='jqtextbox' id='sentDate_" + opt.rowId + "' ";
  html += "name='sentDate_" + opt.rowId + "'" + " class='dojoValidateValid readonly BorderWidth0' disabled = 'disabled' ";
  if (cellVal && cellVal != 'undefined' && cellVal != '' && cellVal != null) {
    html += "value='" + cellVal + "' ";
  }
  html += "style='text-align:left; width: 100%;'></input>";
  html += "<div style='position:absolute; left:0; right:0; top:0; bottom:0;' class='JQGridRowInputBoxOverlay'></div>";
  html += "</div>";
  return html;
}

function formatReceiveDate(cellVal, opt, rowObj) {
  var html, tempVal = findAndReturnJSONValue(jsonPaymentList.List, 'id', opt.rowId, 'receiveDate');
  if (tempVal && tempVal != 'undefined' && tempVal != '' && tempVal != null) {
    cellVal = tempVal;
  }
  html = "<div title='' style='display:inline-block; position:relative;' class='JQGridRowInputBox'>";
  html += "<input type='text' role='jqtextbox' id='receiveDate_" + opt.rowId + "' ";
  html += "name='receiveDate_" + opt.rowId + "'" + " class='dojoValidateValid readonly BorderWidth0' disabled = 'disabled' ";
  if (cellVal && cellVal != 'undefined' && cellVal != '' && cellVal != null) {
    html += "value='" + cellVal + "' ";
  }
  html += "style='text-align:left; width: 100%;'></input>";
  html += "<div style='position:absolute; left:0; right:0; top:0; bottom:0;' class='JQGridRowInputBoxOverlay'></div>";
  html += "</div>";
  return html;
}

function customSelectBox(el, cellval, opts)
{   
    var rowKey = cellval.rowId;
    var headerId ="";
    //var select = "";
    var tempVal = findAndReturnJSONValue(jsonPaymentList.List, 'id', cellval.rowId, 'ChqStatus');
    if (tempVal && tempVal != 'undefined' && tempVal != '' && tempVal != null) {
      el = tempVal;
    }
    var gridChqStatus = document.getElementById("chqStatus").value;
    var jsonGridChqStatus = JSON &&JSON.parse(gridChqStatus) || $.parseJSON(gridChqStatus);
    if (el && el != 'undefined' && el != '' && el != null) {
         headerId = el;
      }
    var select = "<div title='' style='display:inline-block; position:relative;' class='JQGridRowInputBox'>";
    select += '<select name='+rowKey+' id="inpChqStatus_'+rowKey+'" class="Combo Combo_OneCell_width readonly BorderWidth0" disabled = "disabled" style="text-align:left; width: 100%;">';
    if(headerId==''){
        select +='<option value="0" selected><span>'+" "+'</span></option>';
    }
    if(jsonGridChqStatus!=undefined && jsonGridChqStatus!=null){
           var status = jsonGridChqStatus.data;
           for(i in status){
               if(headerId ==status[i].id){
                   select +='<option value='+status[i].id+' selected ><span>'+status[i].recordIdentifier+'</span></option>';   
               }else{
               select +='<option value='+status[i].id+'><span>'+status[i].recordIdentifier+'</span></option>';   
               }
           }
    }           
    select += "</select>";
    select += "<div style='position:absolute; left:0; right:0; top:0; bottom:0;' class='JQGridRowInputBoxOverlay'></div>";
    select += "</div>";
    return select;  
}

function formatLockedIndicator(cellVal, opt, rowObj) {
  return rowObj.lockedIndicator === 'Y' ? '<%= Resource.getProperty("finance.yes", lang) %>' : '<%= Resource.getProperty("finance.no", lang) %>';
}

function selectPaymentGridRow(id) {
  paymentGrid.setSelection(id);
}

function jqueryEnableTextBox(index, enable, required) {
  if (!index) {
    return false;
  }
  if (enable) {
    index.removeAttr("disabled");
    index.removeClass("dojoValidateValid dojoValidateValid_focus readonly readonly_focus BorderWidth0");
    index.addClass("dojoValidateValid_focus " + (required ? "required " : "") + "TextBox_focus");
    $(index).next().hide();
  } else {
    index.attr("disabled", "disabled");
    index.removeClass("dojoValidateValid dojoValidateValid_focus required required_focus TextBox TextBox_focus");
    index.addClass("dojoValidateValid readonly BorderWidth0");
    $(index).next().show();
  }
}

function onClickRefresh() {
    //clearTemp();
  $.post('<%=request.getContextPath()%>/ClearChequeAjax', {
    action: 'clearTemp',
    inpOrgId: filterCriteria.inpOrgId,
    inpPaymentMethod: filterCriteria.inpPaymentMethod,
    inpBank: filterCriteria.inpBank,
    inpAcctNo: filterCriteria.inpAcctNo,
    inpFromPaySeq: filterCriteria.inpFromPaySeq,
    inpToPaySeq: filterCriteria.inpToPaySeq,
    inpClearBankCqNo: filterCriteria.inpClearBankCqNo,
    inpClearBankCqDate: filterCriteria.inpClearBankCqDate,
    inpClearBank: filterCriteria.inpClearBank,
    inpSentBankDate: filterCriteria.inpSentBankDate,
    inpFromPayDate: filterCriteria.inpFromPayDate,
    inpToPayDate: filterCriteria.inpToPayDate,
    inpReconciledDate: filterCriteria.inpReconciledDate,
    inpReconcileNo: filterCriteria.inpReconcileNo,
    inpLock: filterCriteria.inpLock,
    inpChequeStatus: filterCriteria.inpChequeStatus
  }, function (data) {});
  submitCommandForm('DEFAULT', true, null, contextPath + '/sa.elm.ob.finance.ad_forms.clearbankcheque.header/ClearBankCheque', '_self', null, true);
  return false;
}

function searchPaymentList() {
  jsonPaymentList.List = [];
  if (document.getElementById("inpOrg").value == '0' || document.getElementById("inpOrg").value == '') {
    OBAlert('<%= Resource.getProperty("finance.cheque.select.organization", lang) %>');
    paymentGrid.setGridParam({
      datatype: 'local'
    }).trigger("reloadGrid");
    return false;
  } else if (document.getElementById("inpBank").value == '0' || document.getElementById("inpBank").value == '') {
    OBAlert('<%= Resource.getProperty("finance.cheque.select.bank", lang) %>');
    paymentGrid.setGridParam({
      datatype: 'local'
    }).trigger("reloadGrid");
    return false;
  } else if (document.getElementById("inpAcctNo").value == '0' || document.getElementById("inpAcctNo").value == '') {
    OBAlert('<%= Resource.getProperty("finance.cheque.select.bankAccNo", lang) %>');
    paymentGrid.setGridParam({
      datatype: 'local'
    }).trigger("reloadGrid");
    return false;
  } else if (document.getElementById("inpPaymentMethod").value == '0' || document.getElementById("inpPaymentMethod").value == '') {
    OBAlert('<%= Resource.getProperty("finance.cheque.select.paymentMethod", lang) %>');
    paymentGrid.setGridParam({
      datatype: 'local'
    }).trigger("reloadGrid");
    return false;
  } else {
    paymentGrid.setGridParam({
       datatype: 'local'
    }).trigger("reloadGrid");
    clearTempByFilter(function() {
        filterCriteria.inpOrgId = document.getElementById("inpOrg").value;
        filterCriteria.inpPaymentMethod = document.getElementById("inpPaymentMethod").value;
        filterCriteria.inpBank = document.getElementById("inpBank").value;
        filterCriteria.inpAcctNo = document.getElementById("inpAcctNo").value;
        filterCriteria.inpFromPaySeq = document.getElementById("inpFromPaySeq").value;
        filterCriteria.inpToPaySeq = document.getElementById("inpToPaySeq").value;
        filterCriteria.inpClearBankCqNo = document.getElementById("inpClearBankCqNo").value;
        filterCriteria.inpClearBankCqDate = document.getElementById("inpClearBankCqDate").value;
        filterCriteria.inpClearBank = document.getElementById("inpClearBank").value;
        filterCriteria.inpSentBankDate = document.getElementById("inpSentBankDate").value;
        filterCriteria.inpFromPayDate = document.getElementById("inpFromPayDate").value;
        filterCriteria.inpToPayDate = document.getElementById("inpToPayDate").value;
        filterCriteria.inpReconciledDate = document.getElementById("inpReconciledDate").value;
        filterCriteria.inpReconcileNo = document.getElementById("inpReconcileNo").value;
        filterCriteria.inpLock = document.getElementById("inpLockH").value;
        filterCriteria.inpChequeStatus = document.getElementById("inpChequeStatus").value;
        reloadPaymentGrid();
    });
  }
}

function reloadPaymentGrid() {
  paymentGrid.setGridParam({
    datatype: 'json'
  }).trigger("reloadGrid");
}

function applySequence() {
  if(paymentGrid.getDataIDs().length == 0){
        OBAlert('<%= Resource.getProperty("finance.cheque.NoRecord", lang) %>');
    paymentGrid.setGridParam({
      datatype: 'local'
    }).trigger("reloadGrid");
    return false;
  }else{
    document.getElementById('ApplySequenceContent').style.display = '';
    document.getElementById('DivBlackOverlay').style.display = '';
  }
}

function applySequenceBack() {
  applySequenceClear();
  document.getElementById('inpPopClearBankChqDate').value = '';
  document.getElementById('ApplySequenceContent').style.display = 'none';
  document.getElementById('DivBlackOverlay').style.display = 'none';
  document.getElementById('inpBankSentdate').value = '';
  document.getElementById('inpChequeReceiveDate').value = '';
}

function applySequenceClear() {
  document.getElementById('inpStartingChequeSeqNo').value = '';
  document.getElementById('inpPopClearBankChqDate').value = '';
  document.getElementById('inpEndingChequeSeqNo').value = '';
  document.getElementById('inpPopClearBank').value = '';
  document.getElementById('inpBankSentdate').value = '';
  document.getElementById('inpChequeReceiveDate').value = '';
  $("#inpPopChequeStatus").empty().trigger('change');
 }

function isNumberKey(evt) {
  var charCode = (evt.which) ? evt.which : event.keyCode;
  if ((charCode < 48 || charCode > 57)) return false;
  return true;
}


function clearvalues() {
  $('#inpFromPaySeq').val('');
  $('#inpToPaySeq').val('');
  $('#inpClearBank').val('');
  $('#inpClearBankCqNo').val('');
  $('#inpReconcileNo').val('');
  $('#inpLock').prop('checked', false);
  $('#inpClearBankCqDate').val('');
  $('#inpSentBankDate').val('');
  $('#inpFromPayDate').val('');
  $('#inpToPayDate').val('');
  $('#inpReconciledDate').val('');
  document.getElementById('inpLockH').value = 'N';
/*     $('#inpClearBankCqDate').val(todaydate);
    $('#inpSentBankDate').val(todaydate);
    $('#inpFromPayDate').val(todaydate);
    $('#inpToPayDate').val(todaydate);
    $('#inpReconciledDate').val(todaydate); */
  var setOrg = new Option("--select--", "0", true, true);
  $('#inpOrg').append(setOrg).trigger('change');

  var setPaymentMethod = new Option("--select--", "0", true, true);
  $('#inpPaymentMethod').append(setPaymentMethod).trigger('change');

  var setBank = new Option("--select--", "0", true, true);
  $('#inpBank').append(setBank).trigger('change');

  var setAcctNo = new Option("--select--", "0", true, true);
  $('#inpAcctNo').append(setAcctNo).trigger('change');
}

function save() {
  OBAsk(saveRecord, function (yes) {
    if (yes) {
      showProcessBar(true, 2);
      $.post('<%=request.getContextPath()%>/ClearChequeAjax', {
        action: 'Save',
        inpPaymentList: JSON.stringify(jsonPaymentList),
        inpAcctNo: document.getElementById("inpAcctNo").value
      }, function (data) {
            showProcessBar(false);
        if (data.msg == 'true') {
          reloadPaymentGrid();
          displayMessage("S", '<%=Resource.getProperty("finance.cheque.saveScs",lang)%>', 'success');
        } else if (data.msg == 'duplicate') {
          //reloadPaymentGrid();
          displayMessage("E", '<%=Resource.getProperty("finance.cheque.duplicateSeq",lang)%>', 'error');
        } else if (data.msg == 'noSelect') {
          //reloadPaymentGrid();
          displayMessage("E", '<%=Resource.getProperty("finance.cheque.noSelect",lang)%>', 'error');
        } else if (data.msg == 'futureDate') {
          //reloadPaymentGrid();
          displayMessage("E", '<%=Resource.getProperty("finance.cheque.futureDate",lang)%>'+' '+ '<%=Resource.getProperty("finance.cheque.pleaseCheck",lang)%>' + data.docNo, 'error');
        } else {
          reloadPaymentGrid();
          displayMessage("E", '<%=Resource.getProperty("finance.cheque.sysadmin",lang)%>', 'error');
        }
        document.getElementById('client').scrollTop = 0;
      });
    }
  });
}

function lock() {
      var selectedIds = "" + paymentGrid.jqGrid('getGridParam', 'selarrrow'), selectedIdList = selectedIds.split(",").filter(function(v) { return v; }), selectedIdListLength = selectedIdList.length;
      if (selectedIds.indexOf(",") == 0) {
        selectedIdListLength--;
      }
      if(selectedIdListLength > 0){
      OBAsk(sureLock, function (yes) {
        if (yes) {
              showProcessBar(true, 2);
          $.post('<%=request.getContextPath()%>/ClearChequeAjax', {
            action: 'lock',
            inpPaymentList: JSON.stringify(jsonPaymentList)
          }, function (data) {
            showProcessBar(false);
            if (data.msg == 'true') {
              reloadPaymentGrid();
              displayMessage("S", '<%=Resource.getProperty("finance.cheque.sureLockScs",lang)%>', 'success');
            } else if (data.msg == 'notsaved') {
              reloadPaymentGrid();
              OBAlert('<%= Resource.getProperty("finance.cheque.recordnotsaved", lang) %>');
            } else if(data.msg == 'fieldMandatory'){
              reloadPaymentGrid();
              OBAlert('<%=Resource.getProperty("finance.cheque.mandatory",lang)%>'+' '+ '<%=Resource.getProperty("finance.cheque.pleaseCheckPaySeq",lang)%>' +' '+ data.docNo);
            } else {
              reloadPaymentGrid();
              displayMessage("S", '<%=Resource.getProperty("finance.cheque.sysadmin",lang)%>', 'success');
            }
            document.getElementById('client').scrollTop = 0;
          });
        }
      });
  }else{
         OBAlert('<%= Resource.getProperty("finance.cheque.noSelect", lang) %>');
         return false;
  }
}

function unlock() {
    var selectedIds = "" + paymentGrid.jqGrid('getGridParam', 'selarrrow'), selectedIdList = selectedIds.split(",").filter(function(v) { return v; }), selectedIdListLength = selectedIdList.length;
    if (selectedIds.indexOf(",") == 0) {
      selectedIdListLength--;
    }
    if(selectedIdListLength > 0){
      OBAsk(sureUnLock, function (yes) {
        if (yes) {
              showProcessBar(true, 2);
          $.post('<%=request.getContextPath()%>/ClearChequeAjax', {
            action: 'unlock',
            inpPaymentList: JSON.stringify(jsonPaymentList)
          }, function (data) {
              showProcessBar(false);
            if (data.msg == 'true') {
              reloadPaymentGrid();
              displayMessage("S", '<%=Resource.getProperty("finance.cheque.sureUnLockScs",lang)%>', 'success');
            } else if (data.msg == 'notsaved') {
              reloadPaymentGrid();
              OBAlert('<%= Resource.getProperty("finance.cheque.recordnotsaved", lang) %>');
            } else {
              reloadPaymentGrid();
              displayMessage("S", '<%=Resource.getProperty("finance.cheque.sysadmin",lang)%>', 'success');
            }
            document.getElementById('client').scrollTop = 0;
          });
    }
  });
    }else{
        OBAlert('<%= Resource.getProperty("finance.cheque.noSelect", lang) %>');
        return false;
    }
}

function saveAll() {
  OBAsk(saveAllRecord, function (yes) {
    if (yes) {
      $.post('<%=request.getContextPath()%>/ClearChequeAjax', {
        action: 'SaveAll',
        inpOrgId: document.getElementById("inpOrg").value,
        inpPaymentMethod: document.getElementById("inpPaymentMethod").value,
        inpBank: document.getElementById("inpBank").value,
        inpAcctNo: document.getElementById("inpAcctNo").value,
        inpFromPaySeq: document.getElementById("inpFromPaySeq").value,
        inpToPaySeq: document.getElementById("inpToPaySeq").value,
        inpClearBankCqNo: document.getElementById("inpClearBankCqNo").value,
        inpClearBankCqDate: document.getElementById("inpClearBankCqDate").value,
        inpClearBank: document.getElementById("inpClearBank").value,
        inpSentBankDate: document.getElementById("inpSentBankDate").value,
        inpFromPayDate: document.getElementById("inpFromPayDate").value,
        inpToPayDate: document.getElementById("inpToPayDate").value,
        inpReconciledDate: document.getElementById("inpReconciledDate").value,
        inpReconcileNo: document.getElementById("inpReconcileNo").value,
        inpLock: document.getElementById("inpLockH").value,
        inpPaymentList: JSON.stringify(jsonPaymentList)
      }, function (data) {
        if (data.msg == 'true') {
          reloadPaymentGrid();
          displayMessage("S", '<%=Resource.getProperty("finance.cheque.saveScs",lang)%>', 'success');
        } else if (data.msg == 'duplicate') {
          reloadPaymentGrid();
          displayMessage("E", '<%=Resource.getProperty("finance.cheque.duplicateSeq",lang)%>', 'error');
        } else if (data.msg == 'error') {
          reloadPaymentGrid();
          displayMessage("E", '<%=Resource.getProperty("finance.cheque.sysadmin",lang)%>', 'error');
        }
        document.getElementById('client').scrollTop = 0;
      });
    }
  });
}

function clearTemp() {
      OBAsk(clearRecord, function (yes) {
            if (yes) {
                   showProcessBar(true, 2);
                   clearTempByFilter(function(data) {
                       showProcessBar(false);
                       if (data.msg == 'true') {
                         reloadPaymentGrid();
                         displayMessage("S", '<%=Resource.getProperty("finance.chquece.applyValueCleared",lang)%>', 'success');
                       } else if (data.msg == 'noChange') {
                         reloadPaymentGrid();
                         displayMessage("E", '<%=Resource.getProperty("finance.cheque.noclear",lang)%>', 'error');
                       }else if (data.msg == 'error') {
                         reloadPaymentGrid();
                         displayMessage("E", '<%=Resource.getProperty("finance.cheque.sysadmin",lang)%>', 'error');
                       }
                       document.getElementById('client').scrollTop = 0;
                   });
            }
      });
}

function clearTempByFilter(callback) {
      $.post('<%=request.getContextPath()%>/ClearChequeAjax', {
        action: 'clearTemp',
        inpOrgId: filterCriteria.inpOrgId,
        inpPaymentMethod: filterCriteria.inpPaymentMethod,
        inpBank: filterCriteria.inpBank,
        inpAcctNo: filterCriteria.inpAcctNo,
        inpFromPaySeq: filterCriteria.inpFromPaySeq,
        inpToPaySeq: filterCriteria.inpToPaySeq,
        inpClearBankCqNo: filterCriteria.inpClearBankCqNo,
        inpClearBankCqDate: filterCriteria.inpClearBankCqDate,
        inpClearBank: filterCriteria.inpClearBank,
        inpSentBankDate: filterCriteria.inpSentBankDate,
        inpFromPayDate: filterCriteria.inpFromPayDate,
        inpToPayDate: filterCriteria.inpToPayDate,
        inpReconciledDate: filterCriteria.inpReconciledDate,
        inpReconcileNo: filterCriteria.inpReconcileNo,
        inpLock: filterCriteria.inpLock,
        inpChequeStatus: filterCriteria.inpChequeStatus
      }, function (data) {
           if(callback) {
               callback(data);
           }
      });
 }

function applySeqRange() {
    showProcessBar(true, 2);
  $.post('<%=request.getContextPath()%>/ClearChequeAjax', {
    action: 'applySeq',
    inpOrgId: filterCriteria.inpOrgId,
    inpPaymentMethod: filterCriteria.inpPaymentMethod,
    inpBank: filterCriteria.inpBank,
    inpAcctNo: filterCriteria.inpAcctNo,
    inpFromPaySeq: filterCriteria.inpFromPaySeq,
    inpToPaySeq: filterCriteria.inpToPaySeq,
    inpClearBankCqNo: filterCriteria.inpClearBankCqNo,
    inpClearBankCqDate: filterCriteria.inpClearBankCqDate,
    inpClearBank: filterCriteria.inpClearBank,
    inpSentBankDate: filterCriteria.inpSentBankDate,
    inpFromPayDate: filterCriteria.inpFromPayDate,
    inpToPayDate: filterCriteria.inpToPayDate,
    inpReconciledDate: filterCriteria.inpReconciledDate,
    inpReconcileNo: filterCriteria.inpReconcileNo,
    inpLock: filterCriteria.inpLock,
    inpChequeStatus: filterCriteria.inpChequeStatus,
    inpStartSeq: document.getElementById("inpStartingChequeSeqNo").value,
    inpEndSeq: document.getElementById("inpEndingChequeSeqNo").value,
    inpPopChqDate: document.getElementById("inpPopClearBankChqDate").value,
    inpPopBank: document.getElementById("inpPopClearBank").value,
    inpBankSentdate: document.getElementById("inpBankSentdate").value,
    inpChequeReceiveDate: document.getElementById("inpChequeReceiveDate").value,
    inpPopChequeStatus: document.getElementById("inpPopChequeStatus").value

  }, function (data) {
      showProcessBar(false);
    if (data.msg == 'true') {
      applySequenceBack();
      reloadPaymentGrid();
      displayMessage("S", '<%=Resource.getProperty("finance.cheque.seqAppyScs",lang)%>', 'success');
      jsonPaymentList.List = [];
    } else if (data.msg == 'NoChanges') {
      applySequenceBack();
      reloadPaymentGrid();
      displayMessage("E", '<%=Resource.getProperty("finance.cheque.NothingtoApply",lang)%>', 'error');
    } else if (data.msg == 'invalidSeq') {
      applySequenceBack();
      reloadPaymentGrid();
      displayMessage("E", '<%=Resource.getProperty("finance.cheque.invalidSeq",lang)%>', 'error');
    } else if (data.msg == 'NoSeq') {
      applySequenceBack();
      reloadPaymentGrid();
      displayMessage("E", '<%=Resource.getProperty("finance.cheque.seqNotEnough",lang)%>', 'error');
    } else if (data.msg == 'SeqNotEnough') {
      applySequenceBack();
      reloadPaymentGrid();
      displayMessage("E", '<%=Resource.getProperty("finance.cheque.seqNotEnough",lang)%>', 'error');
    } else if (data.msg == 'SeqNotEnoughDuplicate') {
      applySequenceBack();
      reloadPaymentGrid();
      displayMessage("E", '<%=Resource.getProperty("finance.cheque.seqNotEnoughDuplicate",lang)%>', 'error');
    } else if (data.msg == 'futureDate') {
      applySequenceBack();
      reloadPaymentGrid();
      displayMessage("E", '<%=Resource.getProperty("finance.cheque.futureDate",lang)%>', 'error');
    } else if (data.msg == 'error') {
      applySequenceBack();
      reloadPaymentGrid();
      displayMessage("E", '<%=Resource.getProperty("finance.cheque.sysadmin",lang)%>', 'error');
    }
    document.getElementById('client').scrollTop = 0;
  });
}
</script>
</HTML>