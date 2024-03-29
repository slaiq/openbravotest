<!--
 *************************************************************************
 * All Rights Reserved.
 * Contributor(s): Gopalakrishnan
 *************************************************************************
-->
 <%@page import="sa.elm.ob.hcm.ad_forms.contract.vo.ContractVO"%>
<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="sa.elm.ob.utility.util.Utility"%>
<%@page import="java.util.List, java.util.ArrayList"%>
<%@page import="sa.elm.ob.hcm.ad_forms.employee.vo.EmployeeVO"%>
<%@page import="org.codehaus.jettison.json.JSONObject"%>
<%@page import="org.codehaus.jettison.json.JSONArray"%>
<%@ page import="sa.elm.ob.hcm.properties.Resource" errorPage="/web/jsp/ErrorPage.jsp" %>

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
   <META http-equiv="Content-Type" content="text/html; charset=utf-8"></META> 
  <LINK rel="stylesheet" type="text/css" href="<%=style %>" id="paramCSS"></LINK>
  <link rel="stylesheet" type="text/css" href="../web/js/jquery/jquery-ui-1.8.11.custom.css"></link>
  <link rel="stylesheet" type="text/css" href="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.picker.css"></link>
    <link rel="stylesheet" type="text/css" href="../web/js/jquery/ui.jqgrid.css"></link>
    <link rel="stylesheet" type="text/css" href="<%=toolBarStyle %>"></link>
    <link rel="stylesheet" type="text/css" href="../web/js/common/htabs.css"></link>
    <script type="text/javascript" src="../web/js/common/common.js"></script>
    <script type="text/javascript" src="../web/js/utils.js"></script>
    <script type="text/javascript" src="../web/js/windowKeyboard.js"></script>
            
    
    <style type="text/css">
    /* .gridOddRowClass { background-color: #FFE1C0; background-image: none; } */
    </style>
    <script type="text/javascript" src="../web/js/searchs.js"></script>
    <script type="text/javascript" id="paramDirectory">var baseDirectory = "../web/";</script>
    <script type="text/javascript" id="paramLanguage">var defaultLang="en_US";</script>
    <script type="text/javascript">
    function validate(action) {return true;}
    function onLoadDo(){
        /*<<<<<OB Code>>>>>*/
        this.windowTables = new Array(new windowTableId('client', 'buttonOK'));
        setWindowTableParentElement();
        this.tabsTables = new Array(new tabTableId('tdtopTabs'));
        setTabTableParentElement();setBrowserAutoComplete(false);setFocusFirstControl(null, 'inpMailTemplate');
        resizeArea();updateMenuIcon('buttonMenu');onLoad();
        /*<<<<<OB Code>>>>>*/
    }
    function onResizeDo(){resizeArea();reSizeGrid();}
    function onLoad()
    {
        <% request.setAttribute("SaveStatus",request.getAttribute("SaveStatus")==null?"":request.getAttribute("SaveStatus"));
        if(request.getAttribute("SaveStatus").equals("Add-True")) { %>displayMessage("S", "<%=Resource.getProperty("hcm.success",lang)%>","<%= Resource.getProperty("hcm.saveoreditsuccess",lang) %>");<% }
        else if(request.getAttribute("SaveStatus").equals("Update-True")) { %>displayMessage("S", "<%=Resource.getProperty("hcm.success",lang)%>","<%= Resource.getProperty("hcm.saveoreditsuccess",lang) %>");<% }
        else if(request.getAttribute("SaveStatus").equals("False")) { if(request.getAttribute("ErrorMsg")!=null){%>displayMessage("E", "<%=Resource.getProperty("hcm.error",lang)%>","<%= (String)request.getAttribute("ErrorMsg") %>") <%}else{%>displayMessage("E", "<%=Resource.getProperty("hcm.error",lang)%>","<%= Resource.getProperty("hcm.saveorediterror",lang) %>");<% }} %>
        if(document.getElementById('inpCancelHiring').value == "true" )
        {
         document.getElementById('linkButtonNew').className = 'Main_ToolBar_Button_Icon Main_ToolBar_Button_Icon_New_disabled';
        }
    }
    </script>
</HEAD>
<BODY onresize="onResizeDo();" leftmargin="0" topmargin="0" marginwidth="0" marginheight="0" id="paramBody">
    <FORM id="form" method="post" action="" name="frmMain">
        <INPUT type="hidden" name="Command"></INPUT>
    <INPUT type="hidden" name="IsPopUpCall" value="1"></INPUT>
    <INPUT type="hidden" name="inpLastFieldChanged"></INPUT>
    <INPUT type="hidden" id="inpAction" name="inpAction" value=""></INPUT>
    <INPUT type="hidden" name="inpEmployeeId" id="inpEmployeeId" value="<%= request.getAttribute("inpEmployeeId") %>"></INPUT>
    <INPUT type="hidden" name="inpempCategory" id="inpempCategory" value="<%= request.getAttribute("inpempCategory") %>"></INPUT>
    <INPUT type="hidden" name="inpContractId" id="inpContractId" value="<%= request.getAttribute("inpContractId") %>"></INPUT>
    <INPUT type="hidden" name="SubmitType" id="SubmitType" value=""></INPUT>
    <INPUT type="hidden" name="inpTrxStatus" id="inpTrxStatus" value=""></INPUT>
    <INPUT type="hidden" name="inpAddressId" id="inpAddressId" value="<%= request.getAttribute("inpAddressId") %>"></INPUT>
     <INPUT type="hidden" name="inpEmpStatus" id="inpEmpStatus" value="<%= request.getAttribute("inpEmpStatus") %>"></INPUT>
      <INPUT type="hidden" id="inpEmployeeStatus" name="inpEmployeeStatus" value="<%= request.getAttribute("inpEmployeeStatus") %>"></INPUT>
       <INPUT type="hidden" id="inpCancelHiring" name="inpCancelHiring" value="<%= request.getAttribute("CancelHiring") %>"></INPUT>
       <INPUT type="hidden" id="strStatusType" name="strStatusType" value=""></INPUT>

       
        <TABLE height="100%" border="0" cellpadding="0" cellspacing="0" id="main">
            <TR>
                <TD valign="top" id="tdleftTabs">
                    <table cellpadding="0" cellspacing="0" class="Main_ContentPane_LeftTabsBar" id="tdLeftTabsBars"></table>
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
                    <DIV class="Main_ContentPane_NavBar" id="tdtopButtons"><TABLE class="Main_ContentPane_NavBar" id="tdtopNavButtons"></TABLE></DIV>
                    <DIV class="Main_ContentPane_ToolBar" id="paramToolBar">
                        <table class="Main_ContentPane_ToolBar Main_ToolBar_bg" id="tdToolBar">
                            <tr>
                            <td class="Main_ToolBar_Separator_cell" ><img src="../web/images/blank.gif" class="Main_ToolBar_Separator"></td>
                    <td width="2%" ><a href="javascript:void(0);" onClick="onClickNew();" class="Main_ToolBar_Button" onMouseOver="window.status='Create a New Record';return true;" onMouseOut="window.status='';return true;" onclick="this.hideFocus=true" onblur="this.hideFocus=false" id="buttonNew"><img class="Main_ToolBar_Button_Icon Main_ToolBar_Button_Icon_New" src="../web/images/blank.gif" title="<%= Resource.getProperty("hr.new",lang)%>" border="0" id="linkButtonNew"></a></td>
                    <td class="Main_ToolBar_Separator_cell" ><img src="../web/images/blank.gif" class="Main_ToolBar_Separator"></td>
                    <td width="2%" ><a href="javascript:void(0);" onClick="onClickEditView()" class="Main_ToolBar_Button_disabled" onMouseOver="window.status='Form View';return true;" onMouseOut="window.status='';return true;" onclick="this.hideFocus=true" onblur="this.hideFocus=false" id="buttonEdition"><img class="Main_ToolBar_Button_Icon Main_ToolBar_Button_Icon_Edition_disabled" src="../web/images/blank.gif" title="<%= Resource.getProperty("hr.formview",lang) %>" border="0" id="linkButtonEdition"></a></td>
                    <td class="Main_ToolBar_Separator_cell" ><img src="../web/images/blank.gif" class="Main_ToolBar_Separator"></td>
                    <td width="2%" ><a href="javascript:void(0);" onClick="onClickDelete();" class="Main_ToolBar_Button_disabled" onMouseOver="window.status='Delete Record';return true;" onMouseOut="window.status='';return true;" id="buttonDelete"><img class="Main_ToolBar_Button_Icon Main_ToolBar_Button_Icon_Delete_disabled" src="../web/images/blank.gif" title="<%= Resource.getProperty("hr.delete", lang)  %>" border="0" id="linkButtonDelete"></a></td>
                    <td class="Main_ToolBar_Separator_cell" ><img src="../web/images/blank.gif" class="Main_ToolBar_Separator"></img></td>
                    <td width="2%" ><a href="javascript:void(0);" onClick="onClickRefresh()" class="Main_ToolBar_Button" onMouseOver="window.status='Reload Current Page';return true;" onMouseOut="window.status='';return true;" onclick="this.hideFocus=true" onblur="this.hideFocus=false" id="buttonRefresh"><img class="Main_ToolBar_Button_Icon Main_ToolBar_Button_Icon_Refresh" src="../web/images/blank.gif" title="<%= Resource.getProperty("hr.reload",lang) %>" border="0" id="linkButtonRefresh"></a></td>
                    <td class="Main_ToolBar_Separator_cell"><img src="../web/images/blank.gif" class="Main_ToolBar_Separator"></td>
                    <td width="2%" style="display :none"><a href="javascript:void(0);" onClick="onClickSearch()" class="Main_ToolBar_Button" onMouseOver="window.status='Filter Records';return true;" onMouseOut="window.status='';return true;" onclick="this.hideFocus=true" onblur="this.hideFocus=false" id="buttonSearch"><img class="Main_ToolBar_Button_Icon Main_ToolBar_Button_Icon_Search<% if("1".equals(request.getAttribute("inpSearchType"))) { %>Filtered<% } %>" src="../web/images/blank.gif" title="" border="0" id="linkButtonSearch"></a></td> 
                    <td style="display:none" class="Main_ToolBar_Separator_cell" ><img src="../web/images/blank.gif" class="Main_ToolBar_Separator"></td>
                   <td style="width: 100%;">
                   <div class="MainButtonDiv"  style=" position: relative; top: 10px; ">
                    <button onclick="cancelContract();" class="ButtonLink" style="display:none;" id="cancelButton" type="button">
                        <table class="Button">
                            <tbody><tr>
                                <td class="Button_left"><img border="0" src="../web/images/blank.gif" title="Cancel" alt="Cancel" class="Button_Icon Button_Icon_process"></td>
                                <td id="Submit_BTNname" class="Button_text"><%= Resource.getProperty("hcm.correction", lang) %></td>
                                <td class="Button_right"><img border="0" src="../web/images/blank.gif" title="Close" alt="Close" class="Button_Icon Button_Icon_process"></td>
                            </tr></tbody>
                        </table>
                    </button>
                    &nbsp;&nbsp;&nbsp;   
                 </div>
                 </td>
                    <td class="Main_ToolBar_Space"></td>
                            </tr>
                        </table>
                    </DIV>
                    <TABLE width="100%" border="0" cellspacing="0" cellpadding="0" class="Main_ContentPane_TabPane" id="tdtopTabs">
                          <TR id="paramParentTabContainer"><td class="tabBackGroundInit"></td></TR>
                          <TR id="paramMainTabContainer"><td class="tabBackGroundInit"><span style="background: none;" class="tabTitle"></span></td></tr>
                          <tr><td class="tabBackGround">
                            <div class="marginLeft">
                            <div><span class="dojoTabcurrentfirst"><div>
                                <span><a class="dojoTabLink" href="javascript:void(0);" onclick="return false;" onMouseOver="return true;" onMouseOut="return true;"><%= Resource.getProperty("hcm.empmntinfo",lang)%></a></span>
                            </div></span></div>
                            </div>
                        </td></TR>
                    </TABLE>
                    <DIV class="Main_ContentPane_Client" style="overflow: auto;" id="client">
                        <TABLE class="dojoTabContainer" border="0" cellpadding="0" cellspacing="0" width="100%" summary="">
                            <TR><TD>
                                <TABLE class="dojoTabPaneWrapper" border="0" cellpadding="0" cellspacing="0" width="100%" summary="">
                                    <TR><TD>
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
                                                                            <TR><TD class="MessageBox_TopLeft"></TD></TR>
                                                                            <TR><TD class="MessageBox_Left"></TD></TR>
                                                                        </TBODY>
                                                                    </TABLE>
                                                                </TD>
                                                                <TD class="MessageBox_bg">
                                                                    <TABLE class="MessageBox_Top"><TBODY>
                                                                        <TR><TD>
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
                                                                            
                                                                            <div id="hideMessage">
                                                                                <a style="color: yellow;font-size: 11.5px" href="javascript:void(0);" onClick="document.getElementById('messageBoxID').style.display = 'none';"><%=Resource.getProperty("hcm.hide",lang)%></a>
                                                                            </div>
                                                                     </TD></TR>
                                                                     </TBODY></TABLE>
                                                                </TD>
                                                                <TD class="MessageBox_RightTrans">
                                                                    <TABLE style="width: 100%; height: 100%" cellpadding="0" cellspacing="0">
                                                                        <TBODY>
                                                                            <TR><TD class="MessageBox_TopRight"></TD></TR>
                                                                            <TR><TD class="MessageBox_Right"></TD></TR>
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
                        <div align="center">
                            <table><tbody>
                             <tr id="TabList">
                                        <td><div id="TabEmployee" class="tabSelected"  onmouseover="this.className='tabSelectedHOVER';" onmouseout="this.className='tabSelected';" onclick="reloadTab('EMP');">
                                            <span class="LabelText"><%= Resource.getProperty("hcm.employee",lang)%></span></div>
                                        </td>
                
                                        <td><img id="ImgEmpInfo" class="imageSelected" alt="" src="../web/images/DoubleArrow.png"></img></td>
                                        <td><div id="TabEmpInfo" class="tabSelected" onmouseover="this.className='tabSelectedHOVER';" onmouseout="this.className='tabSelected';" onclick="reloadTab('EMPINF');"><span class="LabelText"><%= Resource.getProperty("hcm.empmntinfo",lang)%></span></div>
                                        </td>
                                        <td><img id="ImgEmpAddress" class="imageSelected" alt="" src="../web/images/DoubleArrow.png"></img></td>
                                        <td><div id="TabEmpAddress" class="tabSelected" onmouseover="this.className='tabSelectedHOVER';" onmouseout="this.className='tabSelected';" onclick="reloadTab('EMPADD');" ><span class="LabelText"><%= Resource.getProperty("hcm.empaddress",lang)%></span></div></td>
                                        <td><img  id="ImgqualInfo" class="imageSelected"   alt="" src="../web/images/DoubleArrow.png"></img></td>
                                         <td><div id="TabqualInfo" class="tabSelected"  onmouseout="this.className='tabSelected';" onmouseover="this.className='tabSelectedHOVER';" onclick="reloadTab('Qualification');"><span class="LabelText"><%= Resource.getProperty("hcm.empqualinfo",lang)%></span></div></td>
                                       <td><img  id="ImgDependent" class="imageSelected"   alt="" src="../web/images/DoubleArrow.png"></img></td>
                                       <td><div id="TabDependent" class="tabSelected" onmouseover="this.className='tabSelectedHOVER';" onmouseout="this.className='tabSelected';" onclick="reloadTab('Dependent');">
                                         <span class="LabelText"><%= Resource.getProperty("hcm.dependent",lang)%>
                                         </span>
                                          </div>
                                       </td>
                                        <td><img  id="ImgPreEmp" class="imageSelected"   alt="" src="../web/images/DoubleArrow.png"></img></td>
                                        <td><div id="Tabpreemp" class="tabSelected" onmouseover="this.className='tabSelectedHOVER';" onmouseout="this.className='tabSelected';" onclick="reloadTab('PREEMP');" ><span class="LabelText"><%= Resource.getProperty("hcm.emppreemp",lang)%></span></div></td>
                                       <td><img id="ImgEmpContract" class="imageSelected" alt="" src="../web/images/DoubleArrow.png"></img></td>
                                     <td><div id="TabEmpContract" class="tabCurrent"><span class="LabelText">Contract</span></div>
                                      <div style="text-align: center;"><img alt="" src="../web/images/OrangeArrowDown.png" style="margin-top: -1px;"></img></div>
                                     </td>
                                     </td>
                                      <td><img  id="ImgMedIns" class="imageSelected"   alt="" src="../web/images/DoubleArrow.png"></img></td>
                                     <td><div id="TabMedIns" class="tabSelected" onmouseover="this.className='tabSelectedHOVER';" onmouseout="this.className='tabSelected';" onclick="reloadTab('MEDIN');"><span class="LabelText"><%= Resource.getProperty("hcm.MedicalInsurance",lang)%></span></div>
                                    </td>
                                              <td><img  id="ImgAsset" class="imageSelected"   alt="" src="../web/images/DoubleArrow.png"></img></td>
                                                  <td><div id="TabAsset" class="tabSelected" onmouseover="this.className='tabSelectedHOVER';" onmouseout="this.className='tabSelected';" onclick="reloadTab('Asset');"><span class="LabelText"><%= Resource.getProperty("hcm.Facilities&Assets",lang)%></span></div>
                                                </td>
                                                  <td><img  id="ImgDocument" class="imageSelected"   alt="" src="../web/images/DoubleArrow.png"></img></td>
                                                     <td><div id="TabDocument" class="tabSelected" onmouseover="this.className='tabSelectedHOVER';" onmouseout="this.className='tabSelected';" onclick="reloadTab('DOC');"><span class="LabelText"><%= Resource.getProperty("hcm.documents",lang)%></span></div></td>
                          <td><img  id="ImgEmpPerPayMethod" class="imageSelected"   alt="" src="../web/images/DoubleArrow.png"></img></td>
                                   <td>
                                     <div id="TabEmpPerPayMethod" class="tabSelected"  onmouseover="this.className='tabSelectedHOVER';" onmouseout="this.className='tabSelected';" onclick="reloadTab('PERPAYMETHOD');"><span class="LabelText"><%= Resource.getProperty("hcm.EmpPerPayMethod",lang)%></span>
                                     </div>
                                    </td>
                          </tr>
                        </tbody></table>
                        <div style="margin-bottom: 5px;"></div>
                    </div>
                    <div style="width:100%;" id="FormDetails" align="center">
                        <div align="center">
                        <TABLE style="width:100%; margin-top: 10px;">
                            <TR>
                                 <TD class="TextBox_ContentCell" style="padding-left: 50px;width: 20px;">
                                    <input type="text" name="inpName1" id="inpName1" maxlength="59" value="<%= Utility.escapeQuote(request.getAttribute("inpName1")) %>" required="true" class="dojoValidateValid required TextBox_TwoCells_width" style="color: black; width:300px" onkeydown="return onKeydownHeadeValues(this, event);" onblur="resetHeaderValues(this);" dir="rtl"></input>
                                    <input type="hidden" name="inpHidName1" id="inpHidName1" value="<%= Utility.escapeQuote(request.getAttribute("inpName1")) %>"></input>
                                </TD>
                                <TD class="" align="left" style="min-width:70px;padding-left: 10px"><span class="LabelText" ><%= Resource.getProperty("hcm.arabicname", lang)%></span></TD>
                                <TD class="TitleCell" align="right" style="min-width:110px;"><span class="LabelText" ><%= Resource.getProperty("hcm.fullname", lang)%></span></TD>
                                <TD class="TextBox_ContentCell">
                                    <input type="text" name="inpName2" id="inpName2" maxlength="59" value="<%= Utility.escapeQuote(request.getAttribute("inpName2")) %>" required="true" class="dojoValidateValid required TextBox_TwoCells_width" style="color: black; width:300px" onkeydown="return onKeydownHeadeValues(this, event);" onblur="resetHeaderValues(this);"></input>
                                    <input type="hidden" name="inpHidName2" id="inpHidName2" value="<%= Utility.escapeQuote(request.getAttribute("inpName2")) %>"></input>
                                </TD>
                                <TD class="TitleCell" align="right" style="min-width:110px;"><span class="LabelText" ><%= Resource.getProperty("hcm.code", lang)%></span></TD>
                                <TD class="TextBox_ContentCell">
                                    <input type="text" name="inpEmpNo" id="inpEmpNo" maxlength="59" value="<%= Utility.escapeQuote(request.getAttribute("inpEmpNo")) %>" required="true" class="dojoValidateValid required TextBox_TwoCells_width" style="color: black; width:300px" onkeydown="return onKeydownHeadeValues(this, event);" onblur="resetHeaderValues(this);"></input>
                                    <input type="hidden" name="inpHidEmpNo" id="inpHidEmpNo" value="<%= Utility.escapeQuote(request.getAttribute("inpEmpNo")) %>"></input>
                                </TD>
                            </TR>
                        </TABLE>
                        </div>
                    </div>
                         <div id="LoadingContent" style="display:none;">
                           <div style="text-align: center;margin-top: 17%;"><img alt="Loading" src="../web/images/loading.gif"></div>
                            <div style="color:#000000;font-family: sans-serif;font-size:14px;margin-top:10px;text-align: center;"><%= Resource.getProperty("construction.loading", lang)%>... </div>
                         </div>
                      <div id="jqgrid" style="width:100%; display:none;">
                        <div align="center"><table id="ContractList" class="scroll" cellpadding="0" cellspacing="0" width="100%"></table><div id="pager" class="scroll" style="text-align: center;"></div></div>
                      </div>
                                    </TD></TR>
                                </TABLE>
                            </TD></TR>
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
<script type="text/javascript">
var currentWindow='EMP',contextPath = '<%=request.getContextPath()%>',
confirmdelete='<%= Resource.getProperty("hcm.confirmdelete", lang) %>',
error='<%=Resource.getProperty("hcm.error",lang)%>', 
deletefailure='<%= Resource.getProperty("hcm.deletefailure", lang) %>',
success='<%=Resource.getProperty("hr.success",lang)%>',
deletesuccess='<%= Resource.getProperty("hcm.deletesuccess", lang) %>',
contractType='<%= Resource.getProperty("hcm.contractType", lang) %>',
ContractNo='<%= Resource.getProperty("hcm.contractNo", lang) %>',
StartDate='<%= Resource.getProperty("hcm.startdate", lang) %>',
Duration='<%= Resource.getProperty("hcm.Duration", lang) %>',
ExpiryDate='<%= Resource.getProperty("hcm.expiryDate", lang) %>',
Grade='<%= Resource.getProperty("hcm.preemp.grade", lang) %>',
JobNo='<%= Resource.getProperty("hcm.emp.jobno", lang) %>',
LetterNo='<%= Resource.getProperty("hcm.letterNo", lang) %>',
LetterDate='<%= Resource.getProperty("hcm.letterDate", lang) %>',
DecisionNo='<%= Resource.getProperty("hcm.DecisionNo", lang) %>',
askCorrection='<%= Resource.getProperty("hcm.correctionAsk", lang) %>';
var lang ='<%=lang%>',
direction='ltr';
    if(lang=="ar_SA"){
        direction='rtl';
     }
    var equalto='<%= Resource.getProperty("hcm.Equalto", lang) %>';
    var greaterthanequalto='<%= Resource.getProperty("hcm.GreaterthanorEqualto", lang) %>';
    var lessthanequalto= '<%= Resource.getProperty("hcm.LesserthanorEqualto", lang) %>';
    <%   String strContractTypeList =""; %>

    <% List<ContractVO> status = (ArrayList<ContractVO>)request.getAttribute("inpContractType"); 

    strContractTypeList += '0'+ ":" +   Resource.getProperty("hcm.select",lang) + ";";
      for (ContractVO vo : status){
        strContractTypeList += vo.getContractNo() + ":" +  vo.getContractType() + ";";
     } 
   

     if(strContractTypeList!=null && strContractTypeList!=""){ %>
         document.getElementById("strStatusType").value="<%=strContractTypeList.substring(0,strContractTypeList.lastIndexOf(";"))%>"; 
    <% }%>
    
    </script>
<script type="text/javascript" src="../web/js/jquery-1.7.2.min.js"></script>
<script type="text/javascript" src="../web/js/ui/jquery.ui.core.js"></script>
<script type="text/javascript" src="../web/js/jquery/grid.locale-en.js"></script>
<script type="text/javascript" src="../web/js/ui/jquery.ui.widget.js"></script>
<script type="text/javascript" src="../web/js/ui/jquery.ui.position.js"></script>
<script type="text/javascript" src="../web/js/ui/jquery.ui.autocomplete.js"></script>
<script type="text/javascript" src="../web/sa.elm.ob.hcm/js/ContractEmployeeList.js"></script>
<script type="text/javascript" src="../web/sa.elm.ob.hcm/js/EmployeeList.js"></script>
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.plugin.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.ummalqura.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.ummalqura-ar.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.plus.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.picker.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.picker.ext.js"></script>
<script type="text/javascript" src="../web/sa.elm.ob.hcm/js/ContractList.js"></script>
<script type="text/javascript" src="../web/js/jquery/jquery.jqGrid.min.js"></script>
</HTML>