<%@page import="sa.elm.ob.utility.util.Utility"%>
<%@page import="sa.elm.ob.finance.ad_reports.budgetdetails.vo.BudgetDetailsReportVO"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
    <%@page import="sa.elm.ob.finance.properties.Resource" errorPage="/web/jsp/ErrorPage.jsp" %>
    <%@ page import="java.util.List,java.util.ArrayList" %>
    <%
    List<BudgetDetailsReportVO> orgList = (request.getAttribute("OrgList")==null?null:(ArrayList<BudgetDetailsReportVO>)request.getAttribute("OrgList"));
List<BudgetDetailsReportVO> yearList = (request.getAttribute("YearList")==null?null:(ArrayList<BudgetDetailsReportVO>)request.getAttribute("YearList"));
    List<BudgetDetailsReportVO> groupList = (request.getAttribute("GroupList")==null?null:(ArrayList<BudgetDetailsReportVO>)request.getAttribute("GroupList"));
    List<BudgetDetailsReportVO> typeList = (request.getAttribute("TypeList")==null?null:(ArrayList<BudgetDetailsReportVO>)request.getAttribute("TypeList"));
    List<BudgetDetailsReportVO> deptList = (request.getAttribute("deptList")==null?null:(ArrayList<BudgetDetailsReportVO>)request.getAttribute("deptList"));

 String lang = ((String)session.getAttribute("#AD_LANGUAGE"));
 String style="../web/skins/ltr/org.openbravo.userinterface.skin.250to300Comp/250to300Comp/Openbravo_ERP.css";
 if(lang.equals("ar_SA")){
     style="../web/skins/rtl/org.openbravo.userinterface.skin.250to300Comp/250to300Comp/Openbravo_ERP_250.css";
 }
 %>
<HTML xmlns="http://www.w3.org/1999/xhtml">
    <HEAD>
        <META http-equiv="Content-Type" content="text/html; charset=utf-8"></META> 
         <LINK rel="stylesheet" type="text/css" href="<%=style %>" id="paramCSS"></LINK>
        <link rel="stylesheet" type="text/css" href="../web/js/jquery/jquery-ui-1.8.11.custom.css"></link>
        
        <script type="text/javascript" src="../web/js/common/common.js"></script>
        <script type="text/javascript" src="../web/js/utils.js"></script>
        <script type="text/javascript" src="../web/js/windowKeyboard.js"></script>
        <script type="text/javascript" src="../web/js/searchs.js"></script>
        <script type="text/javascript" id="paramDirectory">var baseDirectory = "../web/";</script>
        <script type="text/javascript" id="paramLanguage">var defaultLang="<%= ((String)session.getAttribute("#AD_LANGUAGE")) %>";</script>
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
            <INPUT type="hidden" name="inpOrg1" id="inpOrg1" value=""></INPUT>
            <INPUT type="hidden" name="inpOrg1name" id="inpOrg1name" value=""></INPUT>
            <INPUT type="hidden" name="inpYear1" id="inpYear1" value=""></INPUT>
            <INPUT type="hidden" name="inpBudgetTypeId1" id="inpBudgetTypeId1" value=""></INPUT>
            <INPUT type="hidden" name="inpBudgetGroupId1" id="inpBudgetGroupId1" value=""></INPUT>
            <INPUT type="hidden" name="inpHideZeroValue" id="inpHideZeroValue" value=""></INPUT>
             <INPUT type="hidden" name="inpbudgetInitiName" id="inpbudgetInitiName" value=""></INPUT>
            <TABLE height="100%" border="0" cellpadding="0" cellspacing="0" id="main">
                <TR>
                    <TD valign="top" id="tdleftTabs">
                        <table cellpadding="0" cellspacing="0" class="Main_ContentPane_LeftTabsBar" id="tdLeftTabsBars"></table>
                    </TD>
                    <TD valign="top">
                        <TABLE width="100%" border="0" cellspacing="0" cellpadding="0" class="Main_ContentPane_Left">
                            <TR>
                                <TD class="Main_NavBar_bg_left" id="tdleftSeparator"></TD>
                            </TR>
                            <TR>
                                <TD class="Main_ToolBar_bg_left" valign="top"></TD>
                            </TR>
                            <TR>
                                <TD class="Main_Client_bg_left"></TD>
                            </TR>
                            <TR>
                                <TD class="Main_Bottom_bg_left"></TD>
                            </TR>
                        </TABLE>
                    </TD>
                    <TD valign="top">
                        <DIV class="Main_ContentPane_NavBar" id="tdtopButtons">
                            <TABLE class="Main_ContentPane_NavBar" id="tdtopNavButtons"></TABLE>
                        </DIV>
                        <DIV class="Main_ContentPane_ToolBar" id="paramToolBar">
                            <table class="Main_ContentPane_ToolBar Main_ToolBar_bg" id="tdToolBar">
                                <tr>
                                    <td class="Main_ToolBar_Separator_cell" ><img src="../web/images/blank.gif" class="Main_ToolBar_Separator"></img></td>
                                    <td width="2%" ><a href="javascript:void(0);" onClick="onClickRefresh()" class="Main_ToolBar_Button" onMouseOver="window.status='Reload Current Page';return true;" onMouseOut="window.status='';return true;" onclick="this.hideFocus=true" onblur="this.hideFocus=false" id="buttonRefresh"><img class="Main_ToolBar_Button_Icon Main_ToolBar_Button_Icon_Refresh" src="../web/images/blank.gif" title="finance.reload" border="0" id="linkButtonRefresh"></a></td>
                                    <td class="Main_ToolBar_Separator_cell" ><img src="../web/images/blank.gif" class="Main_ToolBar_Separator"></img></td>
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
                                                    <span><a class="dojoTabLink" href="javascript:void(0);" onclick="return false;" onMouseOver="return true;" onMouseOut="return true;"><%= Resource.getProperty("finance.budget.budgetdetails",lang) %></a></span>
                                                </div>
                                            </span>
                                        </div>
                                    </div>
                                </td>
                            </tr>
                        </TABLE>
                        <DIV class="Main_ContentPane_Client" style="overflow: auto;" id="client">
                            <TABLE class="dojoTabContainer" border="0" cellpadding="0" cellspacing="0" width="100%" summary="">
                                <TR>
                                    <TD>
                                        <TABLE class="dojoTabPaneWrapper" border="0" cellpadding="0" cellspacing="0" width="100%" summary="">
                                            <TR>
                                                <TD>
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
                                                        <div style="color:#000000;font-family: sans-serif;font-size:14px;margin-top:10px;text-align: center;"><%= Resource.getProperty("finance.loading",lang) %></div>
                                                    </div>
                                                    <TABLE>
                                                    
                                                    
                                                            
                                                            <TR>
                                                            <TD>&nbsp;</TD>
                                                        </TR>
                                                        <TR>
                                                        <TD class="TitleCell">
                                                                <span name="inpOrgListLabel" class="LabelText"  id="inpOrgListLabel" >
                                                                 
                                                                <%= Resource.getProperty("finance.organization",lang).replace("'", "\\\'") %>
                                                                
                                                                </span>
                                                            </TD>
                                                            
                                                        
                                                        <TD class="TextBox_ContentCell">
                                                                <SELECT name="inpOrgList" class="ComboKey Combo_TwoCells_width"  id="inpOrgList" onchange="getDept(this);" >
                                                                 <% 
                                                                 if (orgList!=null && orgList.size() > 0) 
                                                                     for(BudgetDetailsReportVO vo : orgList) {%>
                                                                    
                                                                      <option value="<%= vo.getOrgId() %>"><%= Utility.escapeHTML(vo.getOrgName())%></option>
                                                                 <% } %>
                                                                 
                                                                </SELECT>
                                                            </TD>
                                                        </TR>
                                                        
                                                        <TR>
                                                        <TD class="TitleCell">
                                                                <span name="inpYearListLabel" class="LabelText"  id="inpYearListLabel" >
                                                                 
                                                                <%= Resource.getProperty("finance.budgetdetailreport.field.budgetDef",lang).replace("'", "\\\'") %>
                                                                
                                                                </span>
                                                            </TD>
                                                            
                                                        
                                                        <TD class="TextBox_ContentCell">
                                                                <SELECT name="inpYearList" class="ComboKey Combo_TwoCells_width"  id="inpYearList" >
                                                                 <% 
                                                                 if (yearList!=null && yearList.size() > 0) {
                                                                     for(BudgetDetailsReportVO vo : yearList) {
                                                                    %>
                                                                         <option value="<%= vo.getYearId() %>"><%= vo.getYearName()%></option>
                                                                         
                                                                 <% }} %>
                                                                 
                                                                </SELECT>
                                                            </TD>
                                                        </TR>
                                                        
                                                        <TR>
                                                        <TD class="TitleCell">
                                                                <span name="inpBudgetType" class="LabelText"  id="inpBudgetTypeLabel" >
                                                                <b> 
                                                                <%= Resource.getProperty("finance.budgetdetailreport.field.budgettyp",lang).replace("'", "\\\'") %>
                                                                </b>
                                                                
                                                                </span>
                                                            </TD>
                                                            
                                                        
                                                        <TD class="TextBox_ContentCell">
                                                                <SELECT name="inpBudgetTypeList" class="ComboKey Combo_TwoCells_width"  id="inpBudgetTypeList" onchange="getGroup(this);">
                                                                  <option value='0'><%=Resource.getProperty("finance.Select",lang)%></option>
                                                                 <%  
                                                                 if (typeList!=null && typeList.size() > 0) 
                                                                     for(BudgetDetailsReportVO votyp : typeList) {   %>
                                                                         <option value="<%= votyp.getBudgetTypeId() %>"><%= Utility.escapeHTML(votyp.getBudgetTypeName())%></option>
                                                                         
                                                                 <% } %>
                                                                 
                                                                </SELECT>
                                                            </TD>
                                                        </TR>
                                                        <TR>
                                                        <TD class="TitleCell">
                                                                <span name="inpBudgetGroup" class="LabelText"  id="inpBudgetGroupLabel" >
                                                                <b> 
                                                                <%= Resource.getProperty("finance.budgetdetailreport.field.budgetgrp",lang).replace("'", "\\\'") %>
                                                                </b>
                                                                
                                                                </span>
                                                            </TD>
                                                            
                                                        
                                                        <TD class="TextBox_ContentCell">
                                                                <SELECT name="inpBudgetGroupList" class="ComboKey Combo_TwoCells_width"  id="inpBudgetGroupList" >
                                                                
                                                                 <option value='0'><%=Resource.getProperty("finance.Select",lang)%></option>
                                                                 
                                                                </SELECT>
                                                            </TD>
                                                        </TR>
                                                        
                                                          <TR>
                                                        <TD class="TitleCell">
                                                                <span name="inpviewlevel" class="LabelText"  id="inpviewlevel" >
                                                                 
                                                                <%= Resource.getProperty("finance.budgetdetailreport.field.view",lang).replace("'", "\\\'") %>
                                                                
                                                                </span>
                                                            </TD>
                                                            
                                                        
                                                        <TD class="TextBox_ContentCell">
                                                                <SELECT name="inpview" class="ComboKey Combo_TwoCells_width"  id="inpview" onchange="getCostCenterField(this);">
                                                               <option value='0'><%=Resource.getProperty("finance.Select",lang)%></option>
                                                               <option value='MoF'><%=Resource.getProperty("finance.budgetdetailreport.field.mof",lang)%></option>
                                                               <option value='MoT'><%=Resource.getProperty("finance.budgetdetailreport.field.mot",lang)%></option>
                                                                 
                                                                </SELECT>
                                                            </TD>
                                                        </TR>
                                                 
                                                          <TR>
                                                        <TD class="TitleCell" id="inpFromDeptLabel" style="display:none;">
                                                                <span name="inpfromdeptListLabel"  class="LabelText"  id="inpfromdeptListLabel" >
                                                                 
                                                                <%= Resource.getProperty("finance.budgetdetailreport.field.fromcostcenter",lang).replace("'", "\\\'") %>
                                                                
                                                                </span>
                                                            </TD>
                                                            
                                                        
                                                        <TD class="TextBox_ContentCell" id="inpFromDeptName" style="display:none;">
                                                                <SELECT name="inpfromdeptList" class="ComboKey Combo_TwoCells_width"  id="inpfromdeptList" >
                                                                  <option value='0'><%=Resource.getProperty("finance.Select",lang)%></option>
                                                                 <%-- <% 
                                                                 if (deptList!=null && deptList.size() > 0) 
                                                                     for(BudgetDetailsReportVO vo : deptList) {%>
                                                                    
                                                                      <option value="<%= vo.getDeptId() %>"><%= Utility.escapeHTML(vo.getDeptName())%></option>
                                                                 <% } %> --%>
                                                                 
                                                                </SELECT>
                                                            </TD>
                                                        </TR>
                                                         <TR>
                                                        <TD class="TitleCell" id="inpToDeptLabel" style="display:none;">
                                                                <span name="inptodeptListLabel" class="LabelText"  id="inptodeptListLabel" >
                                                                <%= Resource.getProperty("finance.budgetdetailreport.field.tocostcenter",lang).replace("'", "\\\'") %>
                                                                
                                                                </span>
                                                            </TD>
                                                            
                                                        
                                                        <TD class="TextBox_ContentCell" id="inpToDeptName" style="display:none;">
                                                                <SELECT name="inptodeptList" class="ComboKey Combo_TwoCells_width"  id="inptodeptList" >
                                                                 <option value='0'><%=Resource.getProperty("finance.Select",lang)%></option>
                                                                <%--  <% 
                                                                 if (deptList!=null && deptList.size() > 0) 
                                                                     for(BudgetDetailsReportVO vo : deptList) {%>
                                                                    
                                                                      <option value="<%= vo.getDeptId() %>"><%= Utility.escapeHTML(vo.getDeptName())%></option>
                                                                 <% } %> --%>
                                                                 
                                                                </SELECT>
                                                            </TD>
                                                        </TR>
                                                                                   
                                                        <tr>
                                                            <TD class="TitleCell"><span name="inpHideZeroValue_lbl" class="LabelText" id="inpHideZeroValue_lbl" ><b><%= Resource.getProperty("finance.budgetdetailreport.field.hidezerovalue",lang).replace("'", "\\\'") %></b></span></TD>
                                                            <td><div style="margin-left: 5px;margin-top: 7px"><input type="checkbox" name="inpHideZeroValueAmt" id="inpHideZeroValueAmt" value=''></div></td>
                                                        </tr>
                                                        <TR>
                                                            <TD colspan="4" align="center">
                                                                <div>
                                                                    <BUTTON type="button" id="Submit_linkBTN" class="ButtonLink" onclick="generateReport();" style="margin: 10px 0;">
                                                                        <TABLE class="Button">
                                                                            <TR>
                                                                                <TD class="Button_left"><IMG class="Button_Icon Button_Icon_process" alt="Submit" title="Submit" src="../web/images/blank.gif" border="0"></IMG></TD>
                                                                                <TD class="Button_text" id="Submit_BTNname"><%= Resource.getProperty("finance.budgetdetailreport.button.generatepdf",lang).replace("'", "\\\'") %></TD>
                                                                                <TD class="Button_right"></TD>
                                                                            </TR>
                                                                        </TABLE>
                                                                    </BUTTON>
                                                                    <BUTTON type="button" id="Submit_linkBTN" class="ButtonLink" onclick="excelformat();" style="margin: 10px 0;">
                                                                        <TABLE class="Button">
                                                                            <TR>
                                                                                <TD class="Button_left"><IMG class="Button_Icon Button_Icon_process" alt="Submit" title="Submit" src="../web/images/blank.gif" border="0"></IMG></TD>
                                                                                <TD class="Button_text" id="Submit_BTNname"><%= Resource.getProperty("finance.budgetdetailreport.button.generateexcel",lang).replace("'", "\\\'") %></TD>
                                                                                <TD class="Button_right"></TD>
                                                                            </TR>
                                                                        </TABLE>
                                                                    </BUTTON>
                                                                </div>
                                                            </TD>
                                                        </TR>
                                                    </TABLE>
                                                   </TD>
                                                   </TR>
                                                   </TABLE>
                                                   </TD>
                                                   </TR></TABLE>
                                                   </DIV>
                                               <TABLE width="100%" border="0" cellspacing="0" cellpadding="0" class="Menu_ContentPane_Bottom" id="tdbottomSeparator">
                                                    <TR>
                                                        <TD class="Main_Bottom_bg"><IMG src="../web/images/blank.gif" border="0"></IMG></TD>
                                                   </TR>
                                              </TABLE>
                                               </TD>
                                                    <TD valign="top">
                                                    <TABLE width="100%" border="0" cellspacing="0" cellpadding="0" class="Main_ContentPane_Right" id="tdrightSeparator">
                                                           <TR>
                                                               <TD class="Main_NavBar_bg_right"></TD>
                                                          </TR>
                                                           <TR>
                                                                <TD class="Main_ToolBar_bg_right" valign="top"></TD>
                                                           </TR>
                                                            <TR>
                                                                 <TD class="Main_Client_bg_right"></TD>
                                                            </TR>
                                                             <TR>
                                                                 <TD class="Main_Bottom_bg_right"></TD>
                                                            </TR>
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
    function reloadWindow() 
    {
        submitCommandForm('DEFAULT', false, null, 'BudgetDetailsReport', '_self', null, false); 
        return false;
    }
    function onClickRefresh() 
    {
        
            
        document.getElementById("inpAction").value="";
        reloadWindow();
    }
    function generateReport() 
    {
        document.getElementById("inpAction").value="generateReport";
        document.getElementById("inpOrg1").value=document.getElementById("inpOrgList").value;
        document.getElementById("inpOrg1name").value=$("#inpOrgList option:selected").text();
        document.getElementById("inpYear1").value=document.getElementById("inpYearList").value;
        document.getElementById("inpBudgetGroupId1").value=document.getElementById("inpBudgetGroupList").value;
        document.getElementById("inpBudgetTypeId1").value=document.getElementById("inpBudgetTypeList").value;
        if($("#inpHideZeroValueAmt").is(':checked')){
            $("#inpHideZeroValue").val("Y");    
        }else
            $("#inpHideZeroValue").val("N");
        
        groupval=document.getElementById("inpBudgetGroupList").value;
        typeval = document.getElementById("inpBudgetTypeList").value;
        view = document.getElementById("inpview").value;
        FromCostCenter = document.getElementById("inpfromdeptList").value;
        ToCostCenter = document.getElementById("inptodeptList").value;
        
        document.getElementById("inpbudgetInitiName").value= $("#inpYearList option:selected").text();
        if(typeval=='0')
            {
                //alert("Please select Budget Type");
                  OBAlert('<%= Resource.getProperty("finance.budgetdetailsreport.alert.select.budgettyp",lang).replace("'", "\\\'") %>');

                      //  OBAlert("Please select Budget Type");
                        return false;
                    
            }
        else if(groupval == '0')
            {
             OBAlert('<%= Resource.getProperty("finance.budgetdetailsreport.alert.select.budgetgroup",lang).replace("'", "\\\'") %>');
              //OBAlert("Please select Budget Group");
                return false;
            }
        else if(view == '0')
        {
         OBAlert('<%= Resource.getProperty("finance.budgetdetailsreport.alert.select.view",lang).replace("'", "\\\'") %>');
          //OBAlert("Please select View");
            return false;
        }
        else if (view == 'MoT'){
            if(FromCostCenter == '0'){
            OBAlert('<%= Resource.getProperty("finance.budgetdetailsreport.alert.select.FromCostCenter",lang).replace("'", "\\\'") %>');
            //OBAlert("Please select From Cost Center");
              return false;
            }
            else if(ToCostCenter == '0'){
                OBAlert('<%= Resource.getProperty("finance.budgetdetailsreport.alert.select.TOCostCenter",lang).replace("'", "\\\'") %>');
                //OBAlert("Please select To Cost Center");
                 return false;
                }
    
        }
    
        openServletNewWindow('PRINT_PDF', true,null, 'BudgetDetailsReport', null, false, '700', '1000', true);
  
       
    }
    function excelformat() 
    {
        document.getElementById("inpAction").value="xls";
        document.getElementById("inpOrg1").value=document.getElementById("inpOrgList").value;
        document.getElementById("inpOrg1name").value=$("#inpOrgList option:selected").text();
        document.getElementById("inpYear1").value=document.getElementById("inpYearList").value;
        document.getElementById("inpBudgetGroupId1").value=document.getElementById("inpBudgetGroupList").value;
        document.getElementById("inpBudgetTypeId1").value=document.getElementById("inpBudgetTypeList").value;
        groupval=document.getElementById("inpBudgetGroupList").value;
        typeval = document.getElementById("inpBudgetTypeList").value;
        view = document.getElementById("inpview").value;
        FromCostCenter = document.getElementById("inpfromdeptList").value;
        ToCostCenter = document.getElementById("inptodeptList").value;
        document.getElementById("inpbudgetInitiName").value= $("#inpYearList option:selected").text();
        if(typeval=='0')
            {
                //alert("Please select Budget Type");
                  OBAlert('<%= Resource.getProperty("finance.budgetdetailsreport.alert.select.budgettyp",lang).replace("'", "\\\'") %>');

                      //  OBAlert("Please select Budget Type");
                        return false;
                    
            }
        else if(groupval == '0')
            {
             OBAlert('<%= Resource.getProperty("finance.budgetdetailsreport.alert.select.budgetgroup",lang).replace("'", "\\\'") %>');
              //OBAlert("Please select Budget Group");
                return false;
            }
        else if(view == '0')
        {
         OBAlert('<%= Resource.getProperty("finance.budgetdetailsreport.alert.select.view",lang).replace("'", "\\\'") %>');
          //OBAlert("Please select View");
            return false;
        }
        else if (view == 'MoT'){
            if(FromCostCenter == '0'){
            OBAlert('<%= Resource.getProperty("finance.budgetdetailsreport.alert.select.FromCostCenter",lang).replace("'", "\\\'") %>');
            //OBAlert("Please select From Cost Center");
              return false;
            }
             else if(ToCostCenter == '0'){
                OBAlert('<%= Resource.getProperty("finance.budgetdetailsreport.alert.select.TOCostCenter",lang).replace("'", "\\\'") %>');
                //OBAlert("Please select To Cost Center");
                  return false;
                }
        }
      
        openServletNewWindow('PRINT_XLS', true,null, 'BudgetDetailsReport', null, false, '700', '1000', true);
           
    }

    function getGroup() {

        $.post('<%=request.getContextPath()%>/sa.elm.ob.finance.ad_reports.budgetdetails.header/BudgetDetailsReport', {
            inpAction : 'GetGroupTypeList',
            inpBudgetTypeId : document.getElementById("inpBudgetTypeList").value
        }, function(data) {

            // Getting Payroll Period
             var budgetgroup = data.getElementsByTagName("BudgetGroup");
            var inpBudgetGroup = document.getElementById("inpBudgetGroupList");
            inpBudgetGroup.options.length = 1;
             for ( var i = 0; i < budgetgroup.length; i++) {
                   if(i==0)
                    {
                    var val = budgetgroup[i].getElementsByTagName("ID")[0].childNodes[0].nodeValue;

                    
                    }   
                 $(inpBudgetGroup).append($('<option>', {
                    
                    value : budgetgroup[i].getElementsByTagName("ID")[0].childNodes[0].nodeValue,
                    html : budgetgroup[i].getElementsByTagName("Name")[0].childNodes[0].nodeValue
                })); 
            }
                $('#inpBudgetGroupList option[value=' + val + ']').attr("selected", "selected");
        });

            
    
}
    getDept();
    function getDept() {

        $.post('<%=request.getContextPath()%>/sa.elm.ob.finance.ad_reports.budgetdetails.header/BudgetDetailsReport', {
            inpAction : 'GetCostCenterList',
            inpOrgId : document.getElementById("inpOrgList").value
        }, function(data) {

            // Getting Payroll Period
             var costcenterdept = data.getElementsByTagName("CostCenterDept");
            var inpFromDept = document.getElementById("inpfromdeptList");
            var inpToDept = document.getElementById("inptodeptList");
            inpFromDept.options.length = 1;
            inpToDept.options.length = 1;
             for ( var i = 0; i < costcenterdept.length; i++) {
                   if(i==0)
                    {
                    var val = costcenterdept[i].getElementsByTagName("ID")[0].childNodes[0].nodeValue;

                    
                    }   
                 $(inpFromDept).append($('<option>', {
                    
                    value : costcenterdept[i].getElementsByTagName("ID")[0].childNodes[0].nodeValue,
                    html : costcenterdept[i].getElementsByTagName("Name")[0].childNodes[0].nodeValue
                })); 
                 $(inpToDept).append($('<option>', {
                     
                     value : costcenterdept[i].getElementsByTagName("ID")[0].childNodes[0].nodeValue,
                     html : costcenterdept[i].getElementsByTagName("Name")[0].childNodes[0].nodeValue
                 })); 
            }
               // $('#inpfromdeptList option[value=' + val + ']').attr("selected", "selected");
             //   $('#inptodeptList option[value=' + val + ']').attr("selected", "selected");
        });

            
    
}
    getCostCenterField();
    function getCostCenterField() {
         if (document.getElementById("inpview").value == "MoT") {
             document.getElementById("inpToDeptLabel").style.display="";
             document.getElementById("inpToDeptName").style.display="";
             document.getElementById("inpFromDeptLabel").style.display="";
             document.getElementById("inpFromDeptName").style.display="";
         } else{ 
             
             document.getElementById("inpToDeptLabel").style.display="none";
             document.getElementById("inpToDeptName").style.display="none";
             document.getElementById("inpFromDeptLabel").style.display="none";
             document.getElementById("inpFromDeptName").style.display="none";
             document.getElementById("inpfromdeptList").value="0";
             document.getElementById("inptodeptList").value="0";
         }
         }
          
 
    </script>