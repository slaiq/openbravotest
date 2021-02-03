<%@page import="sa.elm.ob.utility.util.Utility"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
    <%@page import="sa.elm.ob.finance.properties.Resource" errorPage="/web/jsp/ErrorPage.jsp" %>
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

    <TITLE><%= Resource.getProperty("finance.glreport.title",lang) %></TITLE>
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
            
            function validate(action) 
            {
                return true;
            }
            function onLoadDo()
            {
                /*<<<<<OB Code>>>>>*/
                this.windowTables = new Array(new windowTableId('client', 'buttonOK'));
                setWindowTableParentElement();
                this.tabsTables = new Array(new tabTableId('tdtopTabs'));
                setTabTableParentElement();setBrowserAutoComplete(false);setFocusFirstControl(null, 'inpMailTemplate');
                resizeArea();updateMenuIcon('buttonMenu');
                /*<<<<<OB Code>>>>>*/
            }
            function onResizeDo()
            {
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
            <INPUT type="hidden" name="inpglJournalId" id="inpglJournalId" value="${requestScope.inpglJournalId}"></INPUT>
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
                                                    <%= Resource.getProperty("finance.glreport.title",lang).replace("'", "\\\'") %>
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
                                                        <TD class="TitleCell" >
                                                                <span name="formtlabel" class="LabelText"  id="formtlabel" >
                                                                <%= Resource.getProperty("finance.glreport.field.label",lang).replace("'", "\\\'") %>
                                                                
                                                                </span> 
                                                            </TD>
                                                            
                                                        
                                                        <TD colspan=2 class="TextBox_ContentCell">
                                                                 <SELECT name="formattype" class="ComboKey Combo_TwoCells_width"  id="formattype" >
                                                                 
                                                                         <option value="pdf"><%= Resource.getProperty("finance.budgetdetailreport.button.generatepdf",lang) %></option>
                                                                         <option value="xls"><%= Resource.getProperty("finance.budgetdetailreport.button.generateexcel",lang) %></option>
                                                                         
                                                              
                                                                </SELECT> 
                                                            </TD>
                                                        </TR>
                                                        
                                                        
                                                        <TR  align = "center">
                                                            <td></td><TD  id="submitButton">
                                                                
                                                                     
                                                                    <BUTTON type="button" id="Submit_linkBTN" class="ButtonLink" onclick="generateReport();" style="margin: 10px 0;">
                                                                        <TABLE class="Button">
                                                                            <TR>
                                                                                <TD class="Button_left"><IMG class="Button_Icon Button_Icon_process" alt="Submit" title="Submit" src="../web/images/blank.gif" border="0"></IMG></TD>
                                                                                <TD class="Button_text" id="Submit_BTNname">
                                                                                <%= Resource.getProperty("finance.glreport.button.generatereport",lang).replace("'", "\\\'") %>
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
    function reloadWindow() 
    {
        submitCommandForm('DEFAULT', false, null, 'GLAdjustmentReport', '_self', null, false); 
        return false;
    }
    function onClickRefresh() 
    {  
        document.getElementById("inpAction").value="";
        reloadWindow();
    }
    function generateReport() 
    {
        document.getElementById("inpAction").value=document.getElementById("formattype").value;
        if(document.getElementById("formattype").value=='xls')
        {
            openServletNewWindow('PRINT_XLS', true,null, 'GLAdjustmentReport', null, false, '700', '1000', true);
        }
        else
        {
            openServletNewWindow('PRINT_PDF', true,null, 'GLAdjustmentReport', null, false, '700', '1000', true);
        }
        //submitCommandForm('DEFAULT', false, null, 'GLAdjustmentReport', '_self', null, false); 
    } 
   </script>
