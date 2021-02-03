<%@ page import="sa.elm.ob.utility.properties.Resource, org.apache.log4j.Logger" isErrorPage="true" contentType="text/html"%>
<%
	Logger.getLogger("").error("", exception);
	String lang = (((String)session.getAttribute("#AD_LANGUAGE")).split("_"))[0].toString();
%>
<HTML xmlns:="http://www.w3.org/1999/xhtml">
  <HEAD>
    <META http-equiv="Content-Type" content="text/html; charset=utf-8"></META>
	<link rel="stylesheet" type="text/css" href="../web/skins/ltr/org.openbravo.userinterface.skin.250to300Comp/250to300Comp/Openbravo_ERP.css" id="paramCSS" />
    <script type="text/javascript" src="../web/js/utils.js"></script>
    <script type="text/javascript" src="../web/js/windowKeyboard.js"></script>
    <script type="text/javascript">
    function validate(action) {return true;}
	function onLoadDo()
	{
		this.windowTables = new Array(new windowTableId('client', 'buttonOK'));setWindowTableParentElement();this.tabsTables = new Array(new tabTableId('tdtopTabs'));setTabTableParentElement();setBrowserAutoComplete(false);setFocusFirstControl(null, 'inpMailTemplate');resizeArea();updateMenuIcon('buttonMenu');
		ErrorMessage("<%=Resource.getProperty("utility.error",lang)%>", "<%=Resource.getProperty("utility.errorpage.msg",lang)%>");
	}
	function onResizeDo(){resizeArea();}
	function ErrorMessage(title, message)
	{
		document.getElementById("messageBoxID").style.display = "";
		document.getElementById("messageBoxID").className="MessageBoxERROR";
		document.getElementById("messageBoxIDTitle").innerHTML=title;
		document.getElementById("messageBoxIDMessage").innerHTML=message;
	}
</script>
</HEAD>
<BODY onload="onLoadDo();" onresize="onResizeDo();" leftmargin="0" topmargin="0" marginwidth="0" marginheight="0" id="paramMessage">
<FORM method="post" action="" name="frmMain" id="form">
  <INPUT type="hidden" name="Command"></INPUT>
  <INPUT type="hidden" name="IsPopUpCall" value="1"></INPUT>
  <INPUT type="hidden" name="inpType" value="" id="paramType"></INPUT>
  <INPUT type="hidden" name="inpLastFieldChanged"></INPUT>
	<TABLE height="100%" border="0" cellpadding="0" cellspacing="0" id="main">
		<TR>
			<TD valign="top" id="tdleftTabs"><table cellpadding="0" cellspacing="0" class="Main_ContentPane_LeftTabsBar" id="tdLeftTabsBars"></table></TD>
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
            <DIV class="Main_ContentPane_ToolBar" id="paramToolBar"><table class="Main_ContentPane_ToolBar Main_ToolBar_bg" id="tdToolBar"><tr><td class="Main_ToolBar_Space"></td></tr></table></DIV>
			<TABLE width="100%" border="0" cellspacing="0" cellpadding="0" class="Main_ContentPane_TabPane" id="tdtopTabs">
		          <TR id="paramParentTabContainer"><td class="tabBackGroundInit"></td></TR>
		          <TR id="paramMainTabContainer"><td class="tabBackGroundInit"><span style="background: none;" class="tabTitle"></span></td></tr>
				  <TR><TD class="tabBackGround"><div class="marginLeft"></div></TD></TR>
        	</TABLE>
            <DIV class="Main_ContentPane_Client" style="overflow: auto;" id="client">
              <TABLE class="dojoTabContainer" border="0" cellpadding="0" cellspacing="0" width="100%" summary="">
                <TR>
                  <TD>
                    <TABLE class="dojoTabPaneWrapper" border="0" cellpadding="0" cellspacing="0" width="100%" summary="">
                      <TR>
                        <TD>
                         <TABLE cellpadding="0" cellspacing="0" id="messageBoxID" class="MessageBox" style="width: 100%;"><TBODY>
                        	<TR class="MessageBox_TopMargin"><TD class="MessageBox_LeftMargin"></TD><TD></TD><TD class="MessageBox_RightMargin"></TD></TR>
                            <TR><TD class="MessageBox_LeftMargin"></TD><TD>
                                  <TABLE cellpadding="0" cellspacing="0" class="MessageBox_Container"><TBODY>
                                      <TR>
                                      	<TD class="MessageBox_LeftTrans"><TABLE style="width: 100%; height: 100%" cellpadding="0" cellspacing="0"><TBODY><TR><TD class="MessageBox_TopLeft"></TD></TR><TR><TD class="MessageBox_Left"></TD></TR></TBODY></TABLE></TD>
                                        <TD class="MessageBox_bg"><TABLE class="MessageBox_Top"><TBODY><TR><TD><SPAN><TABLE cellpadding="0" cellspacing="0" class="MessageBox_Body_ContentCell"><TBODY><TR><TD class="MessageBox_Icon_ContentCell"><DIV class="MessageBox_Icon"></DIV></TD>
                                        <TD style="vertical-align: top;" id="messageBoxIDContent"><SPAN><DIV class="MessageBox_TextTitle" id="messageBoxIDTitle"></DIV><DIV class="MessageBox_TextDescription" id="messageBoxIDMessage"></DIV><DIV class="MessageBox_TextSeparator"></DIV></SPAN></TD></TR></TBODY></TABLE></SPAN></TD></TR></TBODY></TABLE></TD>
                                        <TD class="MessageBox_RightTrans"><TABLE style="width: 100%; height: 100%" cellpadding="0" cellspacing="0"><TBODY><TR><TD class="MessageBox_TopRight"></TD></TR><TR><TD class="MessageBox_Right"></TD></TR></TBODY></TABLE></TD>
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
                                </TD><TD class="MessageBox_RightMargin"></TD>
							</TR>
                            <TR class="MessageBox_BottomMargin"><TD class="MessageBox_LeftMargin"></TD><TD></TD><TD class="MessageBox_RightMargin"></TD></TR></TBODY>
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
</HTML>