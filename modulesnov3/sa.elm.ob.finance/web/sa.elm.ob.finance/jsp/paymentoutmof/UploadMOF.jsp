 <%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
 <%@page import="sa.elm.ob.utility.util.Utility"%>
 <%@ page import="sa.elm.ob.finance.properties.Resource, sa.elm.ob.finance.ad_process.paymentoutmof.UploadMOFVO" errorPage="/web/jsp/ErrorPage.jsp"%>
 <%@ page import="java.util.List,java.util.ArrayList" errorPage="/web/jsp/ErrorPage.jsp" %>
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
 	<TITLE>Upload MOF</TITLE>
    <LINK rel="stylesheet" type="text/css" href="<%=style %>" id="paramCSS"></LINK>
	
	<script type="text/javascript" id="paramDirectory">var baseDirectory = "../web/";</script> 
	<script type="text/javascript" id="paramLanguage">var defaultLang="en_US";</script>
	<script type="text/javascript" src="../web/js/utils.js"></script>
	<script type="text/javascript" src="../web/js/windowKeyboard.js"></script>
	<script type="text/javascript" src="../web/js/common/common.js"></script>
	<style type="text/css">
	#client
	{
		height:auto !important;
	}
	</style>
	<script type="text/javascript">
	function onLoadDo()
	{
		this.windowTables = new Array(
		  new windowTableId('client', 'buttonOK')
		);
		setWindowTableParentElement();
		enableShortcuts('popup');
		setBrowserAutoComplete(false);
		onResizeDo();			
	}
	function onResizeDo()
	{
		resizePopup();
	}
	function uploadMOF()
	{
		var filename = document.getElementById("inpFile").value;
		if (filename == '') 
        {
			OBAlert("<%= Resource.getProperty("finance.filepathnotempty",lang) %>");
			return false;
        }
		else {
			var extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
			if (extension == "xls" || extension == "xlsx" || extension == "csv") {
				document.getElementById("inpFileType").value = extension;				
					    		
				//var url="&docs="+document.getElementById("docs").value;				
				document.frmMain.action = '<%=request.getContextPath()%>/sa.elm.ob.finance.ad_process.paymentoutmof/UploadMOF?action=UploadMOF';
				//$("#LoadingContent").show();
				document.frmMain.submit();
				return false;
			}
			else {
				OBAlert('<%= Resource.getProperty("finance.invalidfile",lang) %>');
			}
		}
	}
	
	</script>
</HEAD>
<BODY leftmargin="0" topmargin="0" marginwidth="0" marginheight="0" onload="onLoadDo();" onresize="onResizeDo();" id="paramBody">
<FORM name="frmMain" method="post" action="" enctype="multipart/form-data" id="SubContractForm">
  <INPUT type="hidden" name="Command"></INPUT>
  <INPUT type="hidden" name="inpFileType" id="inpFileType" value=""></INPUT>
  <INPUT type="hidden" name="action" id="action" value=""></INPUT>
   
  <INPUT type="hidden" name="inpTabId" id=inpTabId value="<%= request.getAttribute("inpTabId")%>"></INPUT>
  <INPUT type="hidden" name="inpWindowID" id="inpWindowID" value="<%=request.getAttribute("inpWindowID")%>"></INPUT>
  <INPUT type="hidden" name="inpTableId" id=inpTableId value="<%= request.getAttribute("inpTableId")%>"></INPUT>
  <INPUT type="hidden" name="inpFormID" id="inpFormID" value="<%=request.getAttribute("inpFormID")%>"></INPUT>
   
 <TABLE cellspacing="0" cellpadding="0" width="100%" id="table_header">
  <TR>
    <TD>
      <TABLE cellspacing="0" cellpadding="0" class="Popup_ContentPane_NavBar" id="tdToolBar">
        <TR class="Popup_NavBar_bg"><TD></TD>
          <TD class="Popup_NavBar_separator_cell"></TD>
          <TD class="Popup_NavBar_Popup_title_cell"><SPAN><%=Resource.getProperty("finance.paymentout.uploadmof",lang)%></SPAN></TD>
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
                <td class="Popup_InfoBar_text" id="processHelp"><%=Resource.getProperty("finance.paymentout.uploadmof",lang)%> </td>
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
	    <TD>
	      <DIV class="Popup_ContentPane_Client" style="overflow: auto;" id="client">      	       
	      <TABLE cellspacing="0" cellpadding="0" class="Popup_Client_TablePopup">	      	
			<TR>
			<td>
				<table width="100%" border="0" style="margin:0px;padding-top: 20px" >
					<tr style="width: 100%">
						<TD class="TitleCell" ><span><%=Resource.getProperty("finance.paymentout.uploadmof",lang)%></SPAN></TD>
					    <TD class="TextBox_ContentCell">
					     <INPUT type="text" style="font-size: 0pt; margin: 0; padding: 0; border:0; width:0px; height:0px;" onfocus="isInputFile=true;" onblur="isInputFile=false;"></INPUT>
					     <INPUT type="file" class="dojoValidateValid TextBox_TwoCells_width" id="inpFile" name="inpFile" size="35"></INPUT>
					     <INPUT type="text" style="font-size: 0pt; margin: 0; padding: 0; border:0; width:0px; height:0px;" onfocus="isInputFile=true;" onblur="isInputFile=false;"></INPUT>
					    </TD>                  
				  	</TR>
				 </table> 	
			 
	            <div style="text-align: center;">
	                <BUTTON type="button" style="margin:auto;" id="buttonOK" class="ButtonLink" onclick="uploadMOF()" onfocus="buttonEvent('onfocus', this); window.status='Accept'; return true;" onblur="buttonEvent('onblur', this);" onkeyup="buttonEvent('onkeyup', this);" onkeydown="buttonEvent('onkeydown', this);" onkeypress="buttonEvent('onkeypress', this);" onmouseup="buttonEvent('onmouseup', this);" onmousedown="buttonEvent('onmousedown', this);" onmouseover="buttonEvent('onmouseover', this); window.status='Accept'; return true;" onmouseout="buttonEvent('onmouseout', this);">
	                  <TABLE class="Button" style="margin: 10px 0 0;margin-right: 10px;margin-left:20px;" >
	                    <TR>
	                      <TD class="Button_left"><IMG class="Button_Icon Button_Icon_ok" alt="Accept" title="Accept" src="../web/images/blank.gif" border="0"></IMG></TD>
	                      <TD class="Button_text Button_width"><%=Resource.getProperty("finance.upload",lang)%></TD>	
	                      <TD class="Button_right"></TD>
	                    </TR>
	                  </TABLE>
	                </BUTTON>
	                <BUTTON type="button" style="margin:auto;" id="buttonCancel" class="ButtonLink" onclick="closePopUp();" onfocus="buttonEvent('onfocus', this); window.status='Cancel'; return true;" onblur="buttonEvent('onblur', this);" onkeyup="buttonEvent('onkeyup', this);" onkeydown="buttonEvent('onkeydown', this);" onkeypress="buttonEvent('onkeypress', this);" onmouseup="buttonEvent('onmouseup', this);" onmousedown="buttonEvent('onmousedown', this);" onmouseover="buttonEvent('onmouseover', this); window.status='Cancel'; return true;" onmouseout="buttonEvent('onmouseout', this);">	
	                  <TABLE class="Button" >
	                    <TR>
	                      <TD class="Button_left"><IMG class="Button_Icon Button_Icon_cancel" alt="Cancel" title="Cancel" src="../web/images/blank.gif" border="0"></IMG></TD>
	                      <TD class="Button_text Button_width"><%=Resource.getProperty("finance.cancel",lang)%></TD>
	                      <TD class="Button_right"></TD>
	                    </TR>
	                  </TABLE>
	                </BUTTON>
	            </div> 
		                
	          </td></TR>
	        </TABLE>        
	      </DIV>
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
	<script src="../web/js/ui/jquery.ui.widget.js"></script>	
	<script src="../web/js/ui/jquery.ui.mouse.js"></script>
	<script src="../web/js/ui/jquery.ui.button.js"></script>
	<script src="../web/js/ui/jquery.ui.draggable.js"></script>
	<script src="../web/js/ui/jquery.ui.position.js"></script>
	<script src="../web/js/ui/jquery.ui.resizable.js"></script>
	<script src="../web/js/ui/jquery.ui.dialog.js"></script>
	<script type="text/javascript">
	function closePopUp() {
		document.frmMain.action="<%=request.getContextPath()%>/sa.elm.ob.finance.ad_process.paymentoutmof/UploadMOF?action=Close&pageType="+"<%= request.getAttribute("pageType") %>";
		document.frmMain.submit();
	}
	/* function closePopup()
	{	
		$('#uploaddialog').dialog('close'); 
		jsonRoles = {}; jsonRoles.List = [];
	}
	function onClosePopup() {
		isPayComponentDialogOpen = isEmployeeDialogOpen = 0;
		document.getElementById("PopupOverlay").style.display = 'none';	
		document.getElementById("AddNewRolesDialog").style.display = "none";
	} */
</script>
</HTML>
