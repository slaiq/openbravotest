<%@page import="sa.elm.ob.utility.util.Utility"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
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

    <TITLE><%= Resource.getProperty("scm.import.product.stock",lang) %></TITLE>
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
        function onloadFunctions() {
            
        }
        </script>
            <script language="JavaScript" type="text/javascript">function onLoadDo(){
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
            }
            </script>
    </HEAD>
    
    <BODY onresize="onResizeDo();" leftmargin="0" topmargin="0" marginwidth="0" marginheight="0" id="paramBody">
        <FORM id="form" method="post" action="" name="frmMain" enctype="multipart/form-data">
            <INPUT type="hidden" name="Command"></INPUT>
            <INPUT type="hidden" name="IsPopUpCall" value="1"></INPUT>
            <INPUT type="hidden" name="inpLastFieldChanged"></INPUT>
            <INPUT type="hidden" name="inpAction" id="inpAction" value=""></INPUT>
            <INPUT type="hidden" name="inpmInventoryId" id="inpmInventoryId" value="<%= request.getAttribute("inpmInventoryId")%>"></INPUT>
             <INPUT type="hidden" name="defaultfin" id="defaultfin" value="<%= request.getAttribute("defaultfin")%>"></INPUT>
            <TABLE height="100%" border="0" cellpadding="0" cellspacing="0" id="main">
                <TR>
                    
                    <TD valign="top">
                        
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
                                                    <%= Resource.getProperty("scm.import.product.stock",lang).replace("'", "\\\'") %>
                                                    </a></span>
                                                </div>
                                            </span>
                                        </div>
                                    </div>
                                </td>
                            </tr>
                        </TABLE>
                        <DIV class="Main_ContentPane_Client" style="overflow: auto;" id="client">
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
                                                        <TR >
                                                      <TD class="TitleCell"><SPAN class="LabelText"><%=Resource.getProperty("scm.select.file",lang)%></SPAN></TD>
            
                    <TD class="Combo_ContentCell" colspan="2">
                
                        <input type="file" name="uploadFile" id="uploadFile" class="dojoValidateValid required TextBox_TwoCells_width"/>
                    
                    </TD>
                                                        </TR>
                                                        
                                                        
                                                        <TR  align = "center">
                                                            <td></td><TD  id="submitButton">
                                                                
                                                                     
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
                                                                
                                                            </TD>
                                                        </TR>
                                                    </TABLE>
                                                   </TD>
                                                   </TR>
                                                   </TABLE>
                                                   </TD>
                                                   </TR></TABLE>
                                                   </DIV>
                                              
                                               
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
            	OBAlert('<%= Resource.getProperty("scm.upload.valid.csv.file", lang) %>');
                $(file).val('');
                return false;
            }
             
        }

        function validateFile() {
        	if(document.getElementById("defaultfin").value=='N'){
        		 OBAlert('<%= Resource.getProperty("scm.def.locator", lang) %>');
                 return false;
        	}
            if(validateFileExtension()==false)
                return false;
            document.forms['frmMain'].action= "<%=request.getContextPath() %>/sa.elm.ob.scm.ad_process.ImportProductStock.header/ImportProductStock?action=validateCsv&inpmInventoryId="+document.getElementById('inpmInventoryId').value;
            document.forms['frmMain'].submit();
        }

        function uploadCSV() {
        	 // showProcessBar(false);
        	  //showProcessBar(true, 2);
        	if(document.getElementById("defaultfin").value=='N'){
        		 OBAlert('<%= Resource.getProperty("scm.def.locator", lang) %>');
                return false;
           }
            if(validateFileExtension()==false)
                return false;
            
            document.forms['frmMain'].action= "<%=request.getContextPath() %>/sa.elm.ob.scm.ad_process.ImportProductStock.header/ImportProductStock?action=uploadCsv&inpmInventoryId="+document.getElementById('inpmInventoryId').value;
            document.forms['frmMain'].submit();
           
            
        }

   </script>
