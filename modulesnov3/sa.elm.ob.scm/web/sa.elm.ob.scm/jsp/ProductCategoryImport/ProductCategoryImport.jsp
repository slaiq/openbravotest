<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
     <%@page import="sa.elm.ob.utility.util.Utility"%>
    <%@page import="sa.elm.ob.scm.properties.Resource" errorPage="/web/jsp/ErrorPage.jsp" %>
    <%@ page import="java.util.List,java.util.ArrayList" %>
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
<TITLE><%= Resource.getProperty("scm.product.category.upload",lang) %></TITLE>
<META http-equiv="Content-Type" content="text/html; charset=utf-8"></META> 
<LINK rel="stylesheet" type="text/css" href="<%=style %>" id="paramCSS"></LINK>
<link rel="stylesheet" type="text/css" href="../web/js/jquery/jquery-ui-1.8.11.custom.css"></link>
<link rel="stylesheet" type="text/css" href="<%=toolBarStyle %>"></link> 


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
    var errormsg = "<%= Utility.escapeQuote(request.getAttribute("errormsg"))%>";
    var length= errormsg.split("@").length;
    var errorflag = "<%= request.getAttribute("errorflag")%>";
    if(errorflag=="1"){
    	var table = document.getElementById("TBPrdCat");
    	document.getElementById("DivUploadError").style.display="";
    	 // $('#DivUploadError').show();
    	for(var i=0; i < length ; i++){
    		
            addRow(table, errormsg.split("@")[i]);

    	}
    	openUploadErrorDialog();
    	
    }
    else
    	{
        <% request.setAttribute("successmsg",request.getAttribute("successmsg")==null?"":request.getAttribute("successmsg"));%>
         <%  if(request.getAttribute("successflag")=="1") { %>
           displayMessage("S", "<%=Resource.getProperty("scm.success",lang)%>",  "<%= request.getAttribute("successmsg")%>");
           <%}%>
    	}
    /*<<<<<OB Code>>>>>*/
}
function onResizeDo() {
    resizeArea();
}
function addRow(table, value) {
    var rowCount = 0, cell = null, row = null;
    rowCount = table.rows.length;
    row = table.insertRow(rowCount);
    row.className = "DataGrid_Body_Row DataGrid_Body_Row_" + (rowCount % 2);
    
    cell = row.insertCell(0);
    cell.className = "DataGrid_Body_Cell";
    cell.style.textAlign = "center";
    var element = document.createElement('span');
    element.innerHTML = value;
    cell.appendChild(element);

}
function openUploadErrorDialog() {
    isUploadErrorDialogOpen = 1;
    document.getElementById("PopupOverlay").style.display = "";
    document.getElementById("UploadErrorDialog").style.display = "";
}
</script>
</HEAD>
   <BODY onresize="onResizeDo();" leftmargin="0" topmargin="0" marginwidth="0" marginheight="0" id="paramBody">
      <FORM id="frmMain" method="post" action="" name="frmMain"  enctype='multipart/form-data'>
         <INPUT type="hidden" name="Command"></INPUT>
         <INPUT type="hidden" name="IsPopUpCall" value="1"></INPUT>
         <INPUT type="hidden" name="inpLastFieldChanged"></INPUT>
         <INPUT type="hidden" name="inpAction" id="inpAction" value=""></INPUT>
         <INPUT type="hidden" id="inpFileName" name="inpFileName" value=""></INPUT>
         <INPUT type="hidden" id="errormsg" name="errormsg" value="<%= request.getAttribute("errormsg")%>"></INPUT>
         <INPUT type="hidden" id="errorflag" name="errorflag" value="<%= request.getAttribute("errorflag")%>"></INPUT>
         <div id="PopupOverlay" style="display: none; position: absolute; z-index: 50000; background-color: black; opacity: .5; top: 0px; left: 0px; width: 100%; height: 100%; filter: alpha(opacity=50)"></div>
    <div id="UploadErrorDialog" style="display: none; width: 60%; height: 460px; background: white; z-index: 50001; border: 2px solid #ff9c30; box-shadow: 0 0 20px #333; border-radius: 5px; position: absolute; margin: auto; top: 0; right: 0; bottom: 0; left: 0;">
        <img style="float: right; cursor: pointer; margin: 4px 4px; width: 11px; height: 11px;" onclick="onClosePopup()" src="../web/images/Cross.jpg"></img>
        <div align="center" style="margin-top: 10px;"><span class="LabelText" style="font-size: 14px;"><%= Resource.getProperty("scm.product.category.upload.error.msg",lang)%></span></div>
        <div style="padding: 15px; padding-top: 0px;">
        <div style="overflow: auto; height: 390px;">
            <div align="center" id="DivUploadError">
                <TABLE id="fgProdCat" class="FieldGroup" cellspacing="0" cellpadding="0" border="0" style="width: 100%; text-align: center;"><TBODY><TR class="FieldGroup_TopMargin"></TR><TR><TD class="FieldGroupTitle_Left"><IMG class="FieldGroupTitle_Left_bg" border="0" src="../web/images/blank.gif"></IMG></TD><TD class="FieldGroupTitle"><%= Resource.getProperty("scm.product.category.upload", lang) %></TD><TD class="FieldGroupTitle_Right"><IMG class="FieldGroupTitle_Right_bg" border="0" src="../web/images/blank.gif"></IMG></TD><TD class="FieldGroupContent"></TD></TR><TR class="FieldGroup_BottomMargin"></TR></TBODY></TABLE>
                <TABLE id="TBPrdCat" class="DataGrid_Header_Table_focus DataGrid_Body_Table_focus" width="100%" cellpadding="0" cellspacing="0" border="0" align="center"><tbody>
                <tr class="Popup_Client_Selector_DataGrid_HeaderRow">
                <th width="90%" class="DataGrid_Header_Cell"><%= Resource.getProperty("scm.description", lang) %></th></tr></tbody></TABLE>
            </div>
            
        </div>
        </div>
        <div id="DivUploadError" align="center" style="width: 100%;">
            <button type="button" class="ui-button ui-corner-all ui-button-text-only" role="button" style="background: url('../web/Grid/Modern/backgroundHeader.png') repeat-x scroll 50% 100% transparent; font-size: 12px; font-weight: normal; border: 1px solid rgb(211, 211, 211); color: rgb(85, 85, 85); font-family: Verdana,Arial,sans-serif;"
                onmouseover="this.style.border='1px solid #999999';this.style.color='#212121';" onmouseout="this.style.border='1px solid #D3D3D3';this.style.color='#555555';" 
                onclick='onClosePopup();'>
                <span class="ui-button-text" style="padding: 0.35em 1em;"><%= Resource.getProperty("scm.close", lang)%></span></button>
        </div>
    </div>
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
                                       <%= Resource.getProperty("scm.product.category.upload",lang).replace("'", "\\\'") %>
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
                                          <div style="color:#000000;font-family: sans-serif;font-size:14px;margin-top:10px;text-align: center;"><%= Resource.getProperty("finance.loading",lang).replace("'", "\\\'") %> </div>
                                       </div>
                                       <TABLE  align="center" width="100%">
                                          <TR>
                                             <TD>&nbsp;</TD>
                                          </TR>
                                             <TR id="TRUploadFile">
                            <TD class="TitleCell" align="right" style="min-width:100px;"><span class="LabelText"><%= Resource.getProperty("scm.file", lang)%></span></TD>
                            <TD class="TextBox_ContentCell">
                                <INPUT type="file" class="dojoValidateValid required TextBox_TwoCells_width" id="inpFile" name="inpFile" style="padding: 0px; height: 22px;"></INPUT>
                            </TD>
                        </TR>
                                          <TR  align = "center">
                                              <td></td> 
                                              <TD  id="submitButton" style="min-width:25px;" align = "center">
                                                <BUTTON type="button" id="Submit_linkBTN" class="ButtonLink" onclick="Validate();">
                                                   <TABLE class="Button">
                                                      <TR>
                                                         <TD class="Button_left"><IMG class="Button_Icon Button_Icon_process" alt="Submit" title="Submit" src="../web/images/blank.gif" border="0"></IMG></TD>
                                                         <TD class="Button_text" id="Submit_BTNname">
                                                            <%= Resource.getProperty("scm.product.category.validate.button",lang).replace("'", "\\\'") %>
                                                         </TD>
                                                         <TD class="Button_right"></TD>
                                                      </TR>
                                                   </TABLE>
                                                </BUTTON>
                                                <BUTTON type="button" id="Submit_linkBTN" class="ButtonLink" onclick="Upload();" >
                                                   <TABLE class="Button">
                                                      <TR>
                                                         <TD class="Button_left"><IMG class="Button_Icon Button_Icon_process" alt="Submit" title="Submit" src="../web/images/blank.gif" border="0"></IMG></TD>
                                                         <TD class="Button_text" id="Submit_BTNname">
                                                            <%= Resource.getProperty("scm.product.category.upload.button",lang).replace("'", "\\\'") %>
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
<script type="text/javascript">
function reloadWindow() {
    submitCommandForm('DEFAULT', false, null, 'ProductCategoryImport', '_self', null, false);
    return false;
}
function onClickRefresh() {
    document.getElementById("inpAction").value = "";
    reloadWindow();
}
function validateFileExtension() {
    
    var file=$('#inpFileName').val();

    if (!(/\.(csv)$/i).test(file)) {
          OBAlert('<%= Resource.getProperty("scm.upload.valid.csv.file", lang) %>');
        $(file).val('');
        return false;
    }
     
}
function Validate() {
	  
	  if(validateFileExtension()==false)
	        return false;
	   $("#inpFileName").val($('input[type=file]')[0].files[0].name);

	document.getElementById("inpAction").value = "Validate";
	 var url =  '&inpFileName=' + $("#inpFileName").val();
	    
	    document.frmMain.action = '<%=request.getContextPath()%>/sa.elm.ob.scm.ad_process.ProductCategoryImport/ProductCategoryImport?inpAction=' + $("#inpAction").val() + url;
	    document.frmMain.submit();
}
function Upload() {
	 if(validateFileExtension()==false)
         return false;
    
    $("#inpFileName").val($('input[type=file]')[0].files[0].name);
    document.getElementById("inpAction").value = "Upload";
     var url =  '&inpFileName=' + $("#inpFileName").val();
        
        document.frmMain.action = '<%=request.getContextPath()%>/sa.elm.ob.scm.ad_process.ProductCategoryImport/ProductCategoryImport?inpAction=' + $("#inpAction").val() + url;
        document.frmMain.submit();
}
function onClosePopup() {
    isUploadErrorDialogOpen = 0;
    document.getElementById("PopupOverlay").style.display = 'none';
    document.getElementById("UploadErrorDialog").style.display = "none";
}


   </script>
</HTML>