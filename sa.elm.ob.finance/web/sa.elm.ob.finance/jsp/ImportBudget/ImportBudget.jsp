 <%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
 <%@ page import="sa.elm.ob.finance.properties.Resource" errorPage="/web/jsp/ErrorPage.jsp"%>
 <%@page import="sa.elm.ob.utility.util.Utility"%>
<%@page import="java.util.List, java.util.ArrayList"%>
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
    <TITLE><%=Resource.getProperty("finance.import.budget.title",lang)%></TITLE>
    <LINK rel="stylesheet" type="text/css" href="<%=style %>" id="paramCSS"></LINK>
    
    <script type="text/javascript" id="paramDirectory">var baseDirectory = "../web/";</script> 
    <script type="text/javascript" id="paramLanguage">var defaultLang="en_US";</script>
    <script type="text/javascript" src="../web/js/utils.js"></script>
    <script type="text/javascript" src="../web/js/windowKeyboard.js"></script>
    <script type="text/javascript" src="../web/js/common/common.js"></script>
    
    <script type="text/javascript">
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
        var resultlist = (jsonObj!=null ? jsonObj.resultlist : "");
        var data =resultlist.List;
       
        var recordsFailed = (jsonObj!=null ? "("+jsonObj.recordsFailed+")" : "");
        var statusMessage = (jsonObj!=null ? jsonObj.statusMessage : "");
       var description="";
       for(var i in data){
           document.getElementById("messageBoxID").style.display="";
           description= description +"</br>"+data[i].uniquecode;
       }
        if(status!="" && status!=null){
            if("1"==status)
                {
                if(statusMessage=="CSV Validated Succesfully")
                    {
                     displayMessage("S", "Success",statusMessage);
                    }
                else {
                    displayMessage("S", "Success",statusMessage);
                    var myVar = setInterval(closePopUp, 2000);
                     }
                
                }
          
            else if("0"==status){
                //if status is 0, then get error count
               displayMessage("E", "Error", statusMessage+description);
            }
            
        }
        setWindowElementFocus('firstElement');
        
    }
    function onResizeDo(){
        resizeArea();
    }</SCRIPT>
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
   <INPUT type="hidden" name="inpBudgetId" id="inpBudgetId" value="<%= request.getAttribute("inpBudgetId")%>"></INPUT>
 <TABLE cellspacing="0" cellpadding="0" width="100%" id="table_header">
  <TR>
    <TD>
      <TABLE cellspacing="0" cellpadding="0" class="Popup_ContentPane_NavBar" id="tdToolBar">
        <TR class="Popup_NavBar_bg"><TD></TD>
          <TD class="Popup_NavBar_separator_cell"></TD>
          <TD class="Popup_NavBar_Popup_title_cell"><SPAN><%=Resource.getProperty("finance.import.budget.title",lang)%></SPAN></TD>
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
                <td class="Popup_InfoBar_text" id="processHelp"><%=Resource.getProperty("finance.import.budget.title",lang)%> </td>
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
        <TR>
        </TR>
     </br></br>
       
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
                    <TD class="TitleCell"><SPAN class="LabelText"><%=Resource.getProperty("finance.select.file",lang)%></SPAN></TD>
            
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
                    <td class="Button_text Button_width"><%=Resource.getProperty("finance.file.validate",lang)%></td>
                    <td class="Button_right"></td>
                </tr>
            </tbody></table>
        </button>
        <button type="button" id="buttonOK" class="ButtonLink" onclick="uploadCSV()" onfocus="buttonEvent('onfocus', this); window.status='Accept'; return true;" onblur="buttonEvent('onblur', this);" onkeyup="buttonEvent('onkeyup', this);" onkeydown="buttonEvent('onkeydown', this);" onkeypress="buttonEvent('onkeypress', this);" onmouseup="buttonEvent('onmouseup', this);" onmousedown="buttonEvent('onmousedown', this);" onmouseover="buttonEvent('onmouseover', this); window.status='Accept'; return true;" onmouseout="buttonEvent('onmouseout', this);">
            <table class="Button">
                <tbody><tr>
                    <td class="Button_left"><img class="Button_Icon Button_Icon_ok" alt="Accept" title="Accept" src="../web/images/blank.gif" border="0"></td>                  
                    <td class="Button_text Button_width"><%=Resource.getProperty("finance.file.upload",lang)%></td>                 
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

    if (!(/\.(xlsx)$/i).test(file)) {
          OBAlert('<%= Resource.getProperty("finance.upload.valid.xlsx.file", lang) %>');
        $(file).val('');
        return false;
    }
     
}

function validateFile() {
    if(validateFileExtension()==false)
        {
        return false;
        }
    document.forms['frmMain'].action= "<%=request.getContextPath() %>/sa.elm.ob.finance.ad_process.ImportBudget.header/ImportBudget?action=validateCsv&inpBudgetId="+document.getElementById('inpBudgetId').value;
        document.forms['frmMain'].submit();
   
}

function uploadCSV() {
    
    if(validateFileExtension()==false)
        {
        return false;
        }
    document.getElementById("buttonOK").disabled=true;
    document.forms['frmMain'].action= "<%=request.getContextPath() %>/sa.elm.ob.finance.ad_process.ImportBudget.header/ImportBudget?action=uploadCsv&inpBudgetId="+document.getElementById('inpBudgetId').value;
    document.forms['frmMain'].submit();
    
}
function closePopUp() {
    document.frmMain.action="<%=request.getContextPath()%>/sa.elm.ob.finance.ad_process.ImportBudget.header/ImportBudget?action=Close&pageType="+"<%= request.getAttribute("pageType") %>";
    document.frmMain.submit();
}

</script>
</HTML>
