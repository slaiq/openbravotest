<%@ page import="com.qualiantech.utility.properties.Resource" errorPage="/web/jsp/ErrorPage.jsp" %>
<% String lang = (((String)session.getAttribute("#AD_LANGUAGE")).split("_"))[0].toString(); %>
<HTML xmlns="http://www.w3.org/1999/xhtml">
  <HEAD>
   <META http-equiv="Content-Type" content="text/html; charset=utf-8"></META> 
    <link rel="stylesheet" type="text/css" href="../web/skins/ltr/org.openbravo.userinterface.skin.250to300Comp/250to300Comp/Openbravo_ERP.css" id="paramCSS"></link>
    <script type="text/javascript" src="../web/js/utils.js"></script>
    <script type="text/javascript" src="../web/js/windowKeyboard.js"></script>
	<script type="text/javascript">
    function validate(action) {return true;}
	function onLoadDo(){this.windowTables = new Array(new windowTableId('client', 'buttonOK'));setWindowTableParentElement();enableShortcuts('popup');setBrowserAutoComplete(false);onResizeDo();
	ErrorMessage();
	}
	function ErrorMessage()
	{
		document.getElementById("messageBoxID").className = "MessageBoxERROR";
		document.getElementById("messageBoxIDTitle").innerHTML="<%=Resource.getProperty("utility.error",lang)%>";
		document.getElementById("messageBoxIDMessage").innerHTML="<%=Resource.getProperty("utility.restrictpage.msg",lang)%>";
		document.getElementById("messageBoxID").style.display = "";
	}
	function onResizeDo(){resizeArea();}
    </script>
</HEAD>
<BODY onload="onLoadDo();" onresize="onResizeDo();" leftmargin="0" topmargin="0" marginwidth="0" marginheight="0" id="paramMessage">
<FORM method="post" action="" name="frmMain" id="form">
  <INPUT type="hidden" name="Command"></INPUT>
  <INPUT type="hidden" name="IsPopUpCall" value="1"></INPUT>
  <DIV class="Popup_ContentPane_CircleLogo"><DIV class="Popup_WindowLogo"><IMG class="Popup_WindowLogo_Icon Popup_WindowLogo_Icon_attribute" src="../web/images/blank.gif" border="0/"></IMG></DIV></DIV>
  <TABLE cellspacing="0" cellpadding="0" width="100%" id="table_header"><TR><TD><TABLE cellspacing="0" cellpadding="0" class="Popup_ContentPane_NavBar" id="tdToolBar"><TR class="Popup_NavBar_bg"><TD></TD><TD class="Popup_NavBar_separator_cell"></TD><TD class="Popup_NavBar_separator_cell"></TD></TR></TABLE></TD></TR><TR><TD><TABLE cellspacing="0" cellpadding="0" class="Popup_ContentPane_SeparatorBar" id="tdtopTabs"><TR><TD class="Popup_SeparatorBar_bg"></TD></TR></TABLE></TD></TR></TABLE>
  <TABLE cellspacing="0" cellpadding="0" width="100%"><TR><TD><DIV class="Popup_ContentPane_Client" style="overflow: auto; display: none;" id="client">
	<TABLE cellpadding="0" cellspacing="0" id="messageBoxID" class="MessageBox" style="width: 100%; margin-top: 2%;"><TBODY>
		<TR class="MessageBox_TopMargin"><TD class="MessageBox_LeftMargin"></TD><TD></TD><TD class="MessageBox_RightMargin"></TD></TR>
                 <TR><TD class="MessageBox_LeftMargin"></TD><TD><TABLE cellpadding="0" cellspacing="0" class="MessageBox_Container"><TBODY><TR>
			<TD class="MessageBox_LeftTrans"><TABLE style="width: 100%; height: 100%" cellpadding="0" cellspacing="0"><TBODY><TR><TD class="MessageBox_TopLeft"></TD></TR><TR><TD class="MessageBox_Left"></TD></TR></TBODY></TABLE></TD>
			<TD class="MessageBox_bg"><TABLE class="MessageBox_Top"><TBODY><TR><TD><SPAN><TABLE cellpadding="0" cellspacing="0" class="MessageBox_Body_ContentCell"><TBODY><TR><TD class="MessageBox_Icon_ContentCell"><DIV class="MessageBox_Icon"></DIV></TD>
			<TD style="vertical-align: top;" id="messageBoxIDContent"><SPAN><DIV class="MessageBox_TextTitle" id="messageBoxIDTitle"></DIV><DIV class="MessageBox_TextDescription" id="messageBoxIDMessage"></DIV><DIV class="MessageBox_TextSeparator"></DIV></SPAN></TD></TR></TBODY></TABLE></SPAN></TD></TR></TBODY></TABLE></TD>
			<TD class="MessageBox_RightTrans"><TABLE style="width: 100%; height: 100%" cellpadding="0" cellspacing="0"><TBODY><TR><TD class="MessageBox_TopRight"></TD></TR><TR><TD class="MessageBox_Right"></TD></TR></TBODY></TABLE></TD>
		</TR><TR><TD rowspan="2" class="MessageBox_BottomLeft"></TD><TD class="MessageBox_BottomTrans MessageBox_bg"></TD><TD rowspan="2" class="MessageBox_BottomRight"></TD></TR><TR><TD class="MessageBox_Bottom"></TD></TR></TBODY></TABLE></TD><TD class="MessageBox_RightMargin"></TD></TR><TR class="MessageBox_BottomMargin"><TD class="MessageBox_LeftMargin"></TD><TD></TD><TD class="MessageBox_RightMargin"></TD></TR></TBODY>
	</TABLE>
  </DIV></TD></TR></TABLE>
</FORM>
</BODY>
</HTML>