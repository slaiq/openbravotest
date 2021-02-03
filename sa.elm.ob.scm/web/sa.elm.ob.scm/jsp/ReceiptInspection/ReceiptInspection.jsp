<!-- /*
 ************************************************************************************
 * Copyright (C) 2015 Qualian Technologies.
 ************************************************************************************
 */ -->
 <%@ page import="sa.elm.ob.scm.properties.Resource" errorPage="/web/jsp/ErrorPage.jsp" %>
  <%@ page import="java.util.Date" %>
 <%
    String lang = ((String)session.getAttribute("#AD_LANGUAGE"));

    String style="../web/skins/ltr/org.openbravo.userinterface.skin.250to300Comp/250to300Comp/Openbravo_ERP.css";
    String toolBarStyle="../web/js/common/CommonFormLtr.css";
    if(lang.equals("ar_SA")){
     style="../web/skins/rtl/org.openbravo.userinterface.skin.250to300Comp/250to300Comp/Openbravo_ERP_250.css";
    toolBarStyle="../web/js/common/CommonFormRtl.css";
    }
    %>
<HTML>
<HEAD>
<META http-equiv="Content-Type" content="text/html; charset=utf-8"></META>
<LINK rel="stylesheet" type="text/css" href="<%=style %>" id="paramCSS"></LINK>
<link rel="stylesheet" type="text/css" href="<%=toolBarStyle %>"></link>
    <link rel="stylesheet" type="text/css" href="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.picker.css"></link>

<TITLE id="processName"><%= Resource.getProperty("scm.receipt.inspection", lang) %></TITLE>
<LINK rel="shortcut icon" href="../web/images/favicon.ico" type="image/x-icon"></LINK>
<link rel="stylesheet" type="text/css" href="../web/js/jquery/jquery-ui-1.8.11.custom.css"></link>
<link rel="stylesheet" type="text/css" href="../web/js/jquery/ui.jqgrid.css"></link>
<script type="text/javascript" src="../web/js/common/common.js"></script>
<style type="text/css">
.textcolor {
background-color: #ffc !important;

}
</style>

<SCRIPT language="JavaScript" type="text/javascript" id="paramDirectory">var baseDirectory = "../web/";</SCRIPT>
<SCRIPT language="JavaScript" src="../utility/DynamicJS.js" type="text/javascript"></SCRIPT>
<SCRIPT language="JavaScript" type="text/javascript" id="paramLanguage">defaultLang = "en_US";</SCRIPT>
<SCRIPT language="JavaScript" src="../web/js/utils.js" type="text/javascript"></SCRIPT>
<SCRIPT language="JavaScript" src="../web/js/windowKeyboard.js" type="text/javascript"></SCRIPT>
<SCRIPT language="JavaScript" src="../web/js/appStatus.js" type="text/javascript"></SCRIPT> 
<SCRIPT type="text/javascript" id="reloadOpener">window.onunload = reloadOpener; // Reload opener when closing/canceling</SCRIPT>
 
    <SCRIPT language="JavaScript" type="text/javascript">function onLoadDo(){
        
        setProcessingMode('popup', false);
        this.windowTables = new Array(
          new windowTableId('client', 'linkButtonOk')
        );
        setWindowTableParentElement();
        enableShortcuts('popup');
        setBrowserAutoComplete(false);
        
       // resizeGrid();
        setWindowElementFocus('firstElement');
        self.parent.resizeTo(screen.width-200, screen.height-130);
        var left = parseInt((screen.availWidth/2) - ((screen.width-200)/2));
        var top = parseInt((screen.availHeight/2) - ((screen.height-130)/2));
        self.parent.moveTo(left, top);
    }
    
    </SCRIPT>
</HEAD>
<BODY leftmargin="0" topmargin="0" marginwidth="0" marginheight="0" onload="onLoadDo();">
<IFRAME name="hiddenFrame" style="display:none;"></IFRAME>
<FORM id="form" method="post" action="" name="frmMain">
<INPUT type="hidden" name="Command"></INPUT>
<INPUT type="hidden" name="inpLastFieldChanged"></INPUT>
<INPUT type="hidden" name="IsPopUpCall" value="1"></INPUT>
<input type="hidden" name="inpReceiptId" id="inpReceiptId" value="<%=request.getAttribute("inpReceiptId")%>"/>
<input type="hidden" name="inpSelList" id="inpSelList" value=""/>
<input type="hidden"  name="action" id="action" value=""/>
<DIV class="Popup_ContentPane_CircleLogo">
  <DIV class="Popup_WindowLogo">
    <IMG class="Popup_WindowLogo_Icon Popup_WindowLogo_Icon_process" src="../web/images/blank.gif" border="0/"></IMG>
  </DIV>
</DIV>
<TABLE cellspacing="0" cellpadding="0" width="100%">
  <TR>
    <TD>
      <TABLE cellspacing="0" cellpadding="0" class="Popup_ContentPane_NavBar">
        <TR class="Popup_NavBar_bg"><TD></TD>
          <TD class="Popup_NavBar_separator_cell"></TD>
          <TD class="Popup_NavBar_bg_logo_left"></TD>
          <TD class="Popup_NavBar_bg_logo" width="1" onclick="openNewBrowser('http://www.openbravo.com', 'Openbravo');return false;"><IMG src="../web/images/blank.gif" alt="Openbravo" title="Openbravo" border="0" id="openbravoLogo" class="Popup_NavBar_logo"></IMG></TD>
          <TD class="Popup_NavBar_bg_logo_right"></TD>
          <TD class="Popup_NavBar_Popup_title_cell"><SPAN id="processDescription"><%= Resource.getProperty("scm.receipt.inspection", lang) %></SPAN></TD>
          <TD class="Popup_NavBar_separator_cell"></TD>
        </TR>
      </TABLE>
    </TD>
  </TR>
  
  <TR>
    <TD>
      <TABLE cellspacing="0" cellpadding="0" class="Popup_ContentPane_SeparatorBar">
        <TR>
          <TD class="Popup_SeparatorBar_bg"></TD>
        </TR>
      </TABLE>
    </TD>
  </TR>
  <TR>
    <TD>
      <TABLE cellspacing="0" cellpadding="0" class="Popup_ContentPane_InfoBar">
        <TR>
          <TD class="Popup_InfoBar_Icon_cell"><IMG src="../web/images/blank.gif" border="0" class="Popup_InfoBar_Icon_info"></IMG></TD>
          <TD class="Popup_InfoBar_text_table">
            <TABLE>
              <TR>
                <TD class="Popup_InfoBar_text" id="processHelp"><%= Resource.getProperty("scm.receipt.inspection", lang) %></TD>
              </TR>
            </TABLE>
          </TD>
        </TR>
      </TABLE>
    </TD>
  </TR>
    <TR>
    <TD>
      
       <TABLE cellpadding="0" cellspacing="0" id="messageBoxID" class="MessageBoxHIDDEN">
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
                    <TD rowspan="2" class="MessageBox_BottomLeft">
                    </TD>
                    <TD class="MessageBox_BottomTrans MessageBox_bg">
                    </TD>
                    <TD rowspan="2" class="MessageBox_BottomRight">
                    </TD>
                  </TR>
                  <TR>
                    <TD class="MessageBox_Bottom">
                    </TD>
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
    </TD>
  </TR>
  
  <TR>
    <TD>
      <DIV class="Popup_ContentPane_Client" style="overflow: auto;" id="client">     
        <TABLE cellspacing="0" cellpadding="0" class="Popup_Client_TablePopup" border="0">

        <TR>
        <td></td>
        <td> 
            <div id="jqgrid" >
                     <table id="receiptGrid" class="scroll" cellpadding="0" cellspacing="0" width="100%"></table>  
                     <div id="receiptGridPager" class="scroll" style="text-align: center;"></div>                          
            </div>

        </td>
        </TR>
                     
        </TABLE>
        
        <div align="center">
        
            <BUTTON type="button" id="buttonOK" class="ButtonLink"  onclick="UpdateLines()" onfocus="buttonEvent('onfocus', this); window.status='OK'; return true;" onblur="buttonEvent('onblur', this);" onkeyup="buttonEvent('onkeyup', this);" onkeydown="buttonEvent('onkeydown', this);" onkeypress="buttonEvent('onkeypress', this);" onmouseup="buttonEvent('onmouseup', this);" onmousedown="buttonEvent('onmousedown', this);" onmouseover="buttonEvent('onmouseover', this); window.status='OK'; return true;" onmouseout="buttonEvent('onmouseout', this);">
                  <TABLE class="Button">
                    <TR>
                      <TD class="Button_left"><IMG class="Button_Icon Button_Icon_ok" alt="OK" title="OK" src="../web/images/blank.gif" border="0" id="imgButtonOK"></IMG></TD>
                      <TD class="Button_text Button_width" id="tdButtonOK"><%= Resource.getProperty("scm.ok", lang) %></TD>
                      <TD class="Button_right"></TD>
                    </TR>
                  </TABLE>
                </BUTTON>&nbsp;&nbsp;&nbsp;&nbsp;
             <BUTTON type="button" id="buttonCancel" class="ButtonLink" onclick="closePage();return false;" onfocus="buttonEvent('onfocus', this); window.status='Cancel'; return true;" onblur="buttonEvent('onblur', this);" onkeyup="buttonEvent('onkeyup', this);" onkeydown="buttonEvent('onkeydown', this);" onkeypress="buttonEvent('onkeypress', this);" onmouseup="buttonEvent('onmouseup', this);" onmousedown="buttonEvent('onmousedown', this);" onmouseover="buttonEvent('onmouseover', this); window.status='Cancel'; return true;" onmouseout="buttonEvent('onmouseout', this);">
                  <TABLE class="Button">
                    <TR>
                      <TD class="Button_left"><IMG class="Button_Icon Button_Icon_cancel" alt="Cancel" title="Cancel" src="../web/images/blank.gif" border="0" id="imgButtonCancel"></IMG></TD>
                      <TD class="Button_text Button_width" id="tdButtonCancel"><%= Resource.getProperty("scm.cancel", lang) %></TD>
                      <TD class="Button_right"></TD>
                    </TR>
                  </TABLE>
                </BUTTON>
        </div>
        
        
      </DIV>
    </TD>
  </TR>
</TABLE>
</FORM>
</BODY>

<script type="text/javascript" src="../web/js/jquery-1.7.2.min.js"></script>
<script type="text/javascript" src="../web/js/ui/jquery.ui.core.js"></script>
<script type="text/javascript" src="../web/js/jquery/grid.locale-en.js"></script>
<script type="text/javascript" src="../web/js/ui/jquery.ui.widget.js"></script>
<script type="text/javascript" src="../web/js/ui/jquery.ui.position.js"></script>
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.plugin.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.ummalqura.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.ummalqura-ar.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.plus.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.picker.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.picker.ext.js"></script>
<script type="text/javascript" src="../web/sa.elm.ob.scm/js/ReceiptInspection.js?t=<%= new Date().getTime() %>"></script>
<script type="text/javascript" src="../web/js/jquery/jquery.jqGrid.min.js"></script>

<script type="text/javascript">
var contextPath="<%=request.getContextPath()%>";
var selectatlstonelne = '<%= Resource.getProperty("scm.ri.selectatlstonelne", lang) %>' ;
var acceptgtrejqty = '<%= Resource.getProperty("scm.ri.acptgtrejqty", lang) %>' ;
</script>

</HTML>