 <%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
 <%@ page import="sa.elm.ob.scm.properties.Resource" errorPage="/web/jsp/ErrorPage.jsp"%>
 <%     
    String lang = ((String)session.getAttribute("#AD_LANGUAGE"));
     String style="../web/skins/ltr/org.openbravo.userinterface.skin.250to300Comp/250to300Comp/Openbravo_ERP.css";
     if(lang.equals("ar_SA")){
         style="../web/skins/rtl/org.openbravo.userinterface.skin.250to300Comp/250to300Comp/Openbravo_ERP_250.css";
     }
 %>
<HTML>
<HEAD>
<META http-equiv="Content-Type" content="text/html; charset=utf-8"></META>
<TITLE><%=Resource.getProperty("scm.productload.title",lang)%></TITLE>

<LINK rel="shortcut icon" href="../web/images/favicon.ico" type="image/x-icon"></LINK>
<LINK rel="stylesheet" type="text/css" href="<%=style %>" id="paramCSS"></LINK>
<SCRIPT language="JavaScript" type="text/javascript" id="paramDirectory">var baseDirectory = "../web/";</SCRIPT>
<SCRIPT language="JavaScript" src="../utility/DynamicJS.js" type="text/javascript"></SCRIPT>
<SCRIPT language="JavaScript" type="text/javascript" id="paramLanguage">defaultLang="en_US";</SCRIPT>
<SCRIPT language="JavaScript" src="../web/js/utils.js" type="text/javascript"></SCRIPT>
<SCRIPT language="JavaScript" src="../web/js/windowKeyboard.js" type="text/javascript"></SCRIPT>
<script type="text/javascript" src="../web/js/common/common.js"></script>
<SCRIPT language="JavaScript" type="text/javascript">
function onloadFunctions() {
        
}</SCRIPT>
    <SCRIPT language="JavaScript" type="text/javascript">function onLoadDo(){
        this.windowTables = new Array(
          new windowTableId('client', 'buttonOK')
        );
        setWindowTableParentElement();
        this.tabsTables = new Array(
          new tabTableId('tdtopTabs')
        );
        setTabTableParentElement();
        enableShortcuts('edition');
        setBrowserAutoComplete(false);
        
        resizeArea();
        try {
          onloadFunctions();
        } catch (e) {}
        xx();
        var jsonObj = <%= request.getAttribute("Result") %>;
        var status = (jsonObj!=null ? jsonObj.status : "");
        var statusMessage = (jsonObj!=null ? jsonObj.statusMessage : "");
        if(status!="" && status!=null){
            if("1"==status)
                displayMessage("S", "Success",statusMessage);
            else if("0"==status){
                //if status is 1, then get error count
                var recordsFailed = (jsonObj!=null ? "("+jsonObj.recordsFailed+")" : "");
                displayMessage("E", "Error "+recordsFailed,statusMessage);
            }
        }
        setWindowElementFocus('firstElement');
    }

    function onResizeDo(){
        resizeArea();
    }</SCRIPT>
</HEAD>
<BODY leftmargin="0" topmargin="0" marginwidth="0" marginheight="0" onload="onLoadDo();" onresize="onResizeDo();" id="paramMessage">
<FORM id="form" method="post" action="" name="frmMain" enctype="multipart/form-data">
  <INPUT type="hidden" name="Command"></INPUT>
<INPUT type="hidden" name="IsPopUpCall" value="1"></INPUT>
  <INPUT type="hidden" name="inpLastFieldChanged"></INPUT>
  
  
    <TABLE height="100%" border="0" cellpadding="0" cellspacing="0" id="main">
    <TR>
      <TD valign="top" id="tdleftTabs"><table cellpadding="0" cellspacing="0" class="Main_ContentPane_LeftTabsBar" id="tdLeftTabsBars">
</table>
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
        <DIV class="Main_ContentPane_NavBar" id="tdtopButtons"><TABLE class="Main_ContentPane_NavBar" id="tdtopNavButtons">
</TABLE></DIV>
        <DIV class="Main_ContentPane_ToolBar" id="paramToolBar"><table class="Main_ContentPane_ToolBar Main_ToolBar_bg" id="tdToolBar">
<tr>
<td class="Main_ToolBar_Separator_cell" ><img src="../web/images/blank.gif" class="Main_ToolBar_Separator"></td>
<td width="2%" ><a href="#" onClick="submitCommandForm('DEFAULT', false, null, 'ProductUploadTemplate', '_self', null, false);return false;" class="Main_ToolBar_Button" onMouseOver="window.status='Reload Current Page';return true;" onMouseOut="window.status='';return true;" onclick="this.hideFocus=true" onblur="this.hideFocus=false" id="buttonRefresh"><img class="Main_ToolBar_Button_Icon Main_ToolBar_Button_Icon_Refresh" src="../web/images/blank.gif" title="Reload Current Page" border="0" id="linkButtonRefresh"></a></td>
<td class="Main_ToolBar_Separator_cell" ><img src="../web/images/blank.gif" class="Main_ToolBar_Separator"></td>
<td class="Main_ToolBar_Space"></td>
</tr>
</table>
</DIV>
        <TABLE width="100%" border="0" cellspacing="0" cellpadding="0" class="Main_ContentPane_TabPane" id="tdtopTabs">
          <TR id="paramParentTabContainer"><td class="tabBackGroundInit"></td></TR>
          <TR id="paramMainTabContainer"><td class="tabBackGroundInit">
  <div>
  <span class="tabTitle">
    <div class="tabTitle_background">
      <span class="tabTitle_elements_container">
        <span class="tabTitle_elements_text" id="tabTitle_text"><%=Resource.getProperty("scm.productload.title",lang)%></span>
        <span class="tabTitle_elements_separator"><div class="tabTitle_elements_separator_icon"></div></span>
        <span class="tabTitle_elements_image"><div class="tabTitle_elements_image_normal_icon" id="TabStatusIcon"></div></span>
      </span>
    </div>
  </span>
</div>
</td></tr><tr><td class="tabBackGround">
  <div class="marginLeft">
   <div>
     <span class="dojoTabcurrentfirst">
        <div>
            <span><a class="dojoTabLink" href="javascript:void(0);" onclick="return false;" onMouseOver="return true;" onMouseOut="return true;">
           <%=Resource.getProperty("scm.productload.title",lang)%>
            </a></span>
         </div>
    </span>
   </div>
  </div>
</td>
</TR>
          <TR id="paramChildTabContainer"><td class="tabTabbarBackGround"></td></TR>
        </TABLE>
        <DIV class="Main_ContentPane_Client" style="overflow: auto;" id="client">
                <TABLE class="dojoTabContainer" border="0" cellpadding="0" cellspacing="0" width="100%" summary="">
                  <TR>
                    <TD>
                      <TABLE class="dojoTabPaneWrapper" border="0" cellpadding="0" cellspacing="0" width="100%" summary="">
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
        
              <TABLE class="Main_Client_TableEdition">
                <TR>
                  <TD class="TableEdition_OneCell_width"></TD>
                  <TD class="TableEdition_OneCell_width"></TD>
                  <TD class="TableEdition_OneCell_width"></TD>
                  <TD class="TableEdition_OneCell_width"></TD>
                  <TD class="TableEdition_OneCell_width"></TD>
                  <TD class="TableEdition_OneCell_width"></TD>
                </TR>
                
                <TR>
                <TD></TD>
                    <TD class="TitleCell"><SPAN class="LabelText"><%=Resource.getProperty("scm.select.file",lang)%></SPAN></TD>
            
                    <TD class="Combo_ContentCell" colspan="2">
                
                        <input type="file" name="uploadFile" id="uploadFile" class="dojoValidateValid required TextBox_TwoCells_width"/>
                    
                    </TD>
                <TD colspan="2"></TD>
            </TR>
                
                
              </TABLE>
 
                <table width="100%">            
        <tbody><tr> <td style="padding-left: 304px;padding-top: 15px;"> 
    <!-- <div style="padding: 90px 0px 10px 0px;"> -->
    <button type="button" id="ButtonLink" class="ButtonLink" onclick="validateFile()" onfocus="buttonEvent('onfocus', this); window.status='Validate'; return true;" onblur="buttonEvent('onblur', this);" onkeyup="buttonEvent('onkeyup', this);" onkeydown="buttonEvent('onkeydown', this);" onkeypress="buttonEvent('onkeypress', this);" onmouseup="buttonEvent('onmouseup', this);" onmousedown="buttonEvent('onmousedown', this);" onmouseover="buttonEvent('onmouseover', this); window.status='Close'; return true;" onmouseout="buttonEvent('onmouseout', this);">
            <table class="Button">
                <tbody><tr>
                    <td class="Button_left"><img class="Button_Icon Button_Icon_cancel" alt="Validate" title="Validate" src="../web/images/blank.gif" border="0"></td>
                    <td class="Button_text Button_width"><%=Resource.getProperty("scm.file.validate",lang)%></td>
                    <td class="Button_right"></td>
                </tr>
            </tbody></table>
        </button>
        <button type="button" id="buttonOK" class="ButtonLink" onclick="uploadCSV()" onfocus="buttonEvent('onfocus', this); window.status='Accept'; return true;" onblur="buttonEvent('onblur', this);" onkeyup="buttonEvent('onkeyup', this);" onkeydown="buttonEvent('onkeydown', this);" onkeypress="buttonEvent('onkeypress', this);" onmouseup="buttonEvent('onmouseup', this);" onmousedown="buttonEvent('onmousedown', this);" onmouseover="buttonEvent('onmouseover', this); window.status='Accept'; return true;" onmouseout="buttonEvent('onmouseout', this);">
            <table class="Button">
                <tbody><tr>
                    <td class="Button_left"><img class="Button_Icon Button_Icon_ok" alt="Accept" title="Accept" src="../web/images/blank.gif" border="0"></td>                  
                    <td class="Button_text Button_width"><%=Resource.getProperty("scm.file.upload",lang)%></td>                 
                    <td class="Button_right"></td>
                </tr>
            </tbody></table>
        </button>
    <!-- </div> -->
    </td></tr>
     </tbody></table>
                      
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
        </TABLE></TD>
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
<script type="text/javascript" src="../web/js/jquery-1.7.2.min.js"></script>
<script type="text/javascript">

function displayMessage(type, title, message) {
    var id = "messageBoxID";
    if (document.getElementById(id)) {
        if (type == "S")
            type = "MessageBoxSUCCESS";
        else if (type == "E")
            type = "MessageBoxERROR";
        else if (type == "I")
            type = "MessageBoxINFO";
        else if (type == "W")
            type = "MessageBoxWARNING";
        document.getElementById(id).style.display = "";
        document.getElementById(id).className = type;
        document.getElementById(id + "Title").innerHTML = title;
        document.getElementById(id + "Message").innerHTML = message;
    }
}

function validateFileExtension() {
    
    var file=$('#uploadFile').val();

    if (!(/\.(csv)$/i).test(file)) {
        OBAlert('Please upload valid csv file .csv only.');
        $(file).val('');
        return false;
    }
     
}

function validateFile() {
    if(validateFileExtension()==false)
        return false;
    document.forms['frmMain'].action= "<%=request.getContextPath() %>/sa.elm.ob.scm.ad_forms.productload.header/ProductUploadTemplate?action=validateCsv";
    document.forms['frmMain'].submit();
}

function uploadCSV() {
    if(validateFileExtension()==false)
        return false;
    
    document.forms['frmMain'].action= "<%=request.getContextPath() %>/sa.elm.ob.scm.ad_forms.productload.header/ProductUploadTemplate?action=uploadCsv";
    document.forms['frmMain'].submit();
    
}

</script>
</HTML>
