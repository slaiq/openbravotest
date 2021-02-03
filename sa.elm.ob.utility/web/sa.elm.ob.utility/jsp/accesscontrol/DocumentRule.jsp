<%@page pageEncoding="UTF-8" contentType="text/html; charset=UTF-8"%>
<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page import="org.codehaus.jettison.json.JSONObject"%>
<%@ page import="sa.elm.ob.utility.properties.Resource"
    errorPage="/web/jsp/ErrorPage.jsp"%>
<%@page
    import="sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.vo.DocumentRuleVO"%>
<% String lang=((String)session.getAttribute( "#AD_LANGUAGE")); 
                            String style="../web/skins/ltr/org.openbravo.userinterface.skin.250to300Comp/250to300Comp/Openbravo_ERP.css";
                            String ListBoxStyle="../web/js/common/CommonListBoxLtr.css";
                            if(lang.equals("ar_SA")){
                             style="../web/skins/rtl/org.openbravo.userinterface.skin.250to300Comp/250to300Comp/Openbravo_ERP_250.css";
                             ListBoxStyle="../web/js/common/CommonListBoxRtl.css";
                            }
                            List<DocumentRuleVO> typeList = (ArrayList <DocumentRuleVO>)request.getAttribute("DocumentTypeList"); 
                            List <DocumentRuleVO> ruleList = (ArrayList <DocumentRuleVO>)request.getAttribute("RoleList"); 
                            List <JSONObject> organizationList = (ArrayList <JSONObject>)request.getAttribute("OrganizationList"); 
                            int ruleApprovalLength = Integer.parseInt(request.getAttribute("RuleApprovalLength").toString()); %>
<HTML xmlns:="http://www.w3.org/1999/xhtml">

<HEAD>
<META http-equiv="Content-Type" content="text/html; charset=utf-8"></META>
<LINK rel="stylesheet" type="text/css" href="<%=style %>" id="paramCSS"></LINK>
<LINK rel="stylesheet" type="text/css" href="<%=ListBoxStyle %>"></LINK>
<link rel="stylesheet" type="text/css"
    href="../web/js/jquery/jquery-ui-1.8.11.custom.css" />
<link rel="stylesheet" type="text/css"
    href="../web/js/jquery/ui.jqgrid.css" />
<style>
#hideMessage {
    float: right;
    margin-top: -13px;
    margin-right: 10px;
}

#RoleSortable {
    list-style-type: none;
    margin: 0;
    padding: 0;
    width: 90%;
    text-align: center;
}

#RoleSortable li {
    border: 1px solid #A7ABB4 !important;
    margin: 0 5px 5px 5px;
    padding: 5px;
    font-size: 1.1em;
    height: 1em;
}

div.RoleBoxOuter {
    border: 1px solid #d9d9d9;
    background: #f5f5f5;
    display: inline-block;
    border-radius: 3px;
    padding: 2px 0px 2px 5px;
    width: 96%;
}

div.RoleBoxOuter div.RoleBoxText {
    font-family: Arial;
    color: #222222;
    font-size: 13px;
    font-weight: 700;
    white-space: normal;
    float: left;
    width: 85%;
}

div.RoleBoxOuter div.RoleBoxImg {
    margin: 4px 5px 0px 10px;
    cursor: pointer;
    display: inline-block;
    width: 8px;
    height: 8px;
    background: url('../web/images/Grey_Cross_8x8.png') no-repeat;
    float: right;
}

div.DivTDRole {
    padding: 2px 2px 20px 2px;
    position: relative;
    min-height: 80px;
}

div.RoleAddLink {
    text-align: right;
    cursor: pointer;
    text-decoration: underline;
    font-family: Arial;
    color: #222222;
    font-size: 11px;
    font-weight: 700;
    position: absolute;
    bottom: 3px;
    right: 5px;
}

div.allowReservation {
    text-align: right;
    font-family: Arial;
    color: #222222;
    font-size: 11px;
    font-weight: 700;
    position: absolute;
    bottom: 14px;
    right: 5px;
}

table.ui-jqgrid-btable tr td {
    position: relative;
}

.ui-state-highlight {
    height: 1em;
    line-height: 1.1em;
}

.RoleCheckBox {
    cursor: pointer;
}

.ui-jqgrid .ui-jqgrid-hbox-rtl {
    padding-left: 1
}
</style>
<script type="text/javascript" src="../web/js/utils.js"></script>
<script type="text/javascript" src="../web/js/windowKeyboard.js"></script>
<script type="text/javascript" src="../web/js/common/common.js"></script>
<script type="text/javascript" src="../web/js/common/json2.js"></script>
<script type="text/javascript">
                                                        function onLoadDo() {
                                                            this.windowTables = new Array(new windowTableId('client', 'buttonOK'));
                                                            setWindowTableParentElement();
                                                            this.tabsTables = new Array(new tabTableId('tdtopTabs'));
                                                            setTabTableParentElement();
                                                            setBrowserAutoComplete(false);
                                                            setFocusFirstControl(null, 'inpMailTemplate');
                                                            resizeArea();
                                                            updateMenuIcon('buttonMenu');
                                                            onLoadFn();
                                                        }

                                                        function onResizeDo() {
                                                            resizeArea();
                                                            reSizeGrid();
                                                        }

                                                        function validate(action) {
                                                            return true;
                                                        }

                                                        function onLoadFn() {
                                                            document.getElementById("LoadingContent").style.display = "none";
                                                        }

                                                        function onLoadComplete() {
                                                            $("#RuleGrid").show();
                                                        }
                                                    </script>
</HEAD>

<BODY onload="onLoadComplete();" onresize="onResizeDo();" leftmargin="0"
    topmargin="0" marginwidth="0" marginheight="0" id="paramMessage">
    <FORM method="post" action="" name="frmMain" id="form">
        <INPUT type="hidden" name="Command"></INPUT> <INPUT type="hidden"
            name="IsPopUpCall" value="1"></INPUT> <INPUT type="hidden"
            name="inpType" value="" id="paramType"></INPUT> <INPUT type="hidden"
            name="inpLastFieldChanged"></INPUT> <INPUT type="hidden"
            name="inpAction" id="inpAction" value=""></INPUT> <INPUT
            type="hidden" name="inpLanguage" id="inpLanguageId" value="<%=lang%>"></INPUT>
        <jsp:include page="/web/jsp/ProcessBar.jsp" />
        <div id="OverlayDiv"
            style="display: none; position: absolute; z-index: 50000; background-color: black; opacity: .5; top: 0px; left: 0px; width: 100%; height: 100%; filter: alpha(opacity =       50)"></div>
        <div id="RoleListDialog"
            style="display: none; width: 500px; height: 350px; background: white; z-index: 50001; border: 2px solid #ff9c30; box-shadow: 0 0 20px #333; border-radius: 5px; position: absolute; margin: auto; top: 0; right: 0; bottom: 0; left: 0;">
            <img
                style="float: right; cursor: pointer; margin: 4px 4px; width: 11px; height: 11px;"
                onclick="onClosePopup()" src="../web/images/Cross.jpg" />
            <div id="DivRoleHeader"
                style="text-align: center; margin-top: 10px; display: none;">
                <span style="font-weight: bold; font-size: 14px;"><%= Resource.getProperty("utility.accesscontrol.documentrule.rolelist", lang) %></span>
            </div>
            <div style="height: 250px; overflow: auto; margin-top: 20px;">
                <div id="DivNoRole" style="text-align: center; display: none;">
                    <span style="font-weight: bold; font-size: 13px;"><%= Resource.getProperty("utility.accesscontrol.documentrule.norole", lang) %>...</span>
                </div>
                <div id="RoleContainer" align="center">
                    <ul id="RoleSortable"></ul>
                </div>
            </div>
            <div class="ui-dialog-buttonset"
                style="float: right; margin-right: 20px; margin-top: 15px;">
                <button type="button"
                    class="ui-button ui-corner-all ui-button-text-only" role="button"
                    style="background: url(&amp;quot;../web/Grid/Modern/backgroundHeader.png&amp;quot;) repeat-x scroll 50% 100% transparent; font-size: 12px; font-weight: normal; border: 1px solid rgb(211, 211, 211); color: rgb(85, 85, 85); font-family: Verdana, Arial, sans-serif;"
                    onmouseover="this.style.border='1px solid #999999';this.style.color='#212121';"
                    onmouseout="this.style.border='1px solid #D3D3D3';this.style.color='#555555';"
                    onclick='onClickAddRole();'>
                    <span class="ui-button-text" style="padding: 0.35em 1em;"><%= Resource.getProperty("utility.ok", lang)%></span>
                </button>
                &nbsp;
                <button type="button"
                    class="ui-button ui-corner-all ui-button-text-only" role="button"
                    style="background: url(&amp;quot;../web/Grid/Modern/backgroundHeader.png&amp;quot;) repeat-x scroll 50% 100% transparent; font-size: 12px; font-weight: normal; border: 1px solid rgb(211, 211, 211); color: rgb(85, 85, 85); font-family: Verdana, Arial, sans-serif;"
                    onmouseover="this.style.border='1px solid #999999';this.style.color='#212121';"
                    onmouseout="this.style.border='1px solid #D3D3D3';this.style.color='#555555';"
                    onclick="onClosePopup();">
                    <span class="ui-button-text" style="padding: 0.35em 1.5em;"><%= Resource.getProperty("utility.cancel", lang)%></span>
                </button>
            </div>
        </div>
        <TABLE height="100%" border="0" cellpadding="0" cellspacing="0"
            id="main">
            <TR>
                <TD valign="top" id="tdleftTabs">
                    <table cellpadding="0" cellspacing="0"
                        class="Main_ContentPane_LeftTabsBar" id="tdLeftTabsBars"></table>
                </TD>
                <TD valign="top">
                    <TABLE width="100%" border="0" cellspacing="0" cellpadding="0"
                        class="Main_ContentPane_Left">
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
                        <table class="Main_ContentPane_ToolBar Main_ToolBar_bg"
                            id="tdToolBar">
                            <tr>
                                <td class="Main_ToolBar_Separator_cell"><img
                                    src="../web/images/blank.gif" class="Main_ToolBar_Separator">
                                </td>
                                <td width="2%"><a href="javascript:void(0);"
                                    onClick="onClickRefresh()" class="Main_ToolBar_Button"
                                    onMouseOver="window.status='Reload Current Page';return true;"
                                    onMouseOut="window.status='';return true;" id="buttonRefresh"><img
                                        class="Main_ToolBar_Button_Icon Main_ToolBar_Button_Icon_Refresh"
                                        src="../web/images/blank.gif"
                                        title="<%= Resource.getProperty("construction.reload", lang) %>"
                                        border="0" id="linkButtonRefresh"></a></td>
                                <td class="Main_ToolBar_Separator_cell"><img
                                    src="../web/images/blank.gif" class="Main_ToolBar_Separator">
                                </td>
                                <td class="Main_ToolBar_Space"></td>
                            </tr>
                        </table>
                    </DIV>
                    <TABLE width="100%" border="0" cellspacing="0" cellpadding="0"
                        class="Main_ContentPane_TabPane" id="tdtopTabs">
                        <TR id="paramParentTabContainer">
                            <td class="tabBackGroundInit"></td>
                        </TR>
                        <TR id="paramMainTabContainer">
                            <td class="tabBackGroundInit"><span
                                style="background: none;" class="tabTitle"></span></td>
                        </tr>
                        <TR>
                            <TD class="tabBackGround">
                                <div class="marginLeft">
                                    <div>
                                        <span class="dojoTabcurrentfirst"><div>
                                                <span><a class="dojoTabLink"
                                                    href="javascript:void(0);" onclick="return false;"
                                                    onMouseOver="return true;" onMouseOut="return true;"><%= Resource.getProperty("utility.accesscontrol.documentrule", lang) %></a></span>
                                            </div> </span>
                                    </div>
                                </div>
                            </TD>
                        </TR>
                    </TABLE>
                    <DIV class="Main_ContentPane_Client" style="overflow: auto;"
                        id="client">
                        <TABLE class="dojoTabContainer" border="0" cellpadding="0"
                            cellspacing="0" width="100%" summary="">
                            <TR>
                                <TD>
                                    <TABLE class="dojoTabPaneWrapper" border="0" cellpadding="0"
                                        cellspacing="0" width="100%" summary="">
                                        <TR>
                                            <TD>
                                                <TABLE cellpadding="0" cellspacing="0" id="messageBoxID"
                                                    class="MessageBox">
                                                    <TBODY>
                                                        <TR class="MessageBox_TopMargin">
                                                            <TD class="MessageBox_LeftMargin"></TD>
                                                            <TD></TD>
                                                            <TD class="MessageBox_RightMargin"></TD>
                                                        </TR>
                                                        <TR>
                                                            <TD class="MessageBox_LeftMargin"></TD>
                                                            <TD>
                                                                <TABLE cellpadding="0" cellspacing="0"
                                                                    class="MessageBox_Container">
                                                                    <TBODY>
                                                                        <TR>
                                                                            <TD class="MessageBox_LeftTrans">
                                                                                <TABLE style="width: 100%; height: 100%"
                                                                                    cellpadding="0" cellspacing="0">
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
                                                                                            <TD><SPAN><TABLE cellpadding="0"
                                                                                                        cellspacing="0"
                                                                                                        class="MessageBox_Body_ContentCell">
                                                                                                        <TBODY>
                                                                                                            <TR>
                                                                                                                <TD class="MessageBox_Icon_ContentCell"><DIV
                                                                                                                        class="MessageBox_Icon"></DIV></TD>
                                                                                                                <TD style="vertical-align: top;"
                                                                                                                    id="messageBoxIDContent"><SPAN><DIV
                                                                                                                            class="MessageBox_TextTitle"
                                                                                                                            id="messageBoxIDTitle"></DIV>
                                                                                                                        <DIV class="MessageBox_TextDescription"
                                                                                                                            id="messageBoxIDMessage"></DIV>
                                                                                                                        <DIV class="MessageBox_TextSeparator"></DIV></SPAN>
                                                                                                                </TD>
                                                                                                            </TR>
                                                                                                        </TBODY>
                                                                                                    </TABLE> </SPAN>
                                                                                                <div id="hideMessage">
                                                                                                    <a style="color: yellow; font-size: 11.5px"
                                                                                                        href="javascript:void(0);"
                                                                                                        onClick="document.getElementById('messageBoxID').style.display = 'none';reSizeGrid();">
                                                                                                        <%=Resource.getProperty( "utility.hide",lang)%>
                                                                                                    </a>
                                                                                                </div></TD>
                                                                                        </TR>
                                                                                    </TBODY>
                                                                                </TABLE>
                                                                            </TD>
                                                                            <TD class="MessageBox_RightTrans">
                                                                                <TABLE style="width: 100%; height: 100%"
                                                                                    cellpadding="0" cellspacing="0">
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
                                                            <TD class="MessageBox_RightMargin"></TD>
                                                        </TR>
                                                    </TBODY>
                                                </TABLE>
                                                <div id="LoadingContent"
                                                    style="position: absolute; top: 40%; left: 48%; text-align: center;">
                                                    <div>
                                                        <img alt="Loading" src="../web/images/loading.gif">
                                                    </div>
                                                    <div
                                                        style="color: #000000; font-family: sans-serif; font-size: 14px; margin-top: 15px;">
                                                        <%=Resource.getProperty( "utility.loading", lang) %>...
                                                    </div>
                                                </div>
                                                <table>
                                                    <tr>
                                                        <td>
                                                            <div class="ListBox">
                                                                <SPAN class="LabelText"><%= Resource.getProperty("utility.organization",lang)%></SPAN>&nbsp;&nbsp;&nbsp;
                                                                <SELECT name="inpOrganization" id="inpOrganization"
                                                                    class="ComboKey Combo_TwoCells_width"
                                                                    onchange="getDocumentRule()">
                                                                    <% if(organizationList!=null && organizationList.size()>0) { for(JSONObject jsonObject : organizationList) { %>
                                                                    <option value='<%= jsonObject.getString("id") %>'>
                                                                        <%=jsonObject.getString( "name") %>
                                                                    </option>
                                                                    <% } } %>
                                                                </SELECT>
                                                            </div>
                                                        </td>
                                                        <td>
                                                            <div class="ListBox">
                                                                <SPAN class="LabelText"><%= Resource.getProperty("utility.accesscontrol.documentrule.docmenttype",lang)%></SPAN>&nbsp;&nbsp;&nbsp;
                                                                <SELECT name="inpDocumentType" id="inpDocumentType"
                                                                    class="ComboKey Combo_TwoCells_width"
                                                                    onchange="getDocumentRule()">
                                                                    <option value='0' selected="selected">
                                                                        <%=Resource.getProperty( "utility.comboselect",lang)%>
                                                                    </option>
                                                                    <% if(typeList!=null && typeList.size()>0) { for(DocumentRuleVO ruleVO : typeList) { %>
                                                                    <option value='<%= ruleVO.getNo() %>'>
                                                                        <%=ruleVO.getName() %>
                                                                    </option>
                                                                    <% } } %>
                                                                </SELECT>
                                                            </div>
                                                        </td>
                                                        <td>
                                                            <div class="ListBox">
                                                                <SPAN class="LabelText"><%=Resource.getProperty("utility.accesscontrol.documentrule.label.multirule", lang) %></SPAN>&nbsp;&nbsp;&nbsp;
                                                                <input type="checkbox" name="inpIsMultiRule"
                                                                    id="inpIsMultiRule" value="Y" class="Checkbox"
                                                                    onclick="return false;" onkeydown="return false;"
                                                                    disabled="disabled">
                                                            </div>
                                                        </td>
                                                    </tr>
                                                </table>
                                                <div align="center">
                                                    <div id="RuleGrid"
                                                        style="display: none; margin-top: 10px; margin-bottom: 13px;">
                                                        <table id="DocumentRuleGrid" class="scroll"
                                                            cellpadding="0" cellspacing="0" width="100%"></table>
                                                        <DIV id="DocumentRulePager" class="scroll"
                                                            style="text-align: center;"></DIV>
                                                    </div>
                                                </div>
                                                <div id="AddRuleButton" align="right"
                                                    class="ui-dialog-buttonset"
                                                    style="margin-top: -6px; margin-right: 20px; text-align: right; display: none;">
                                                    <%-- <button type="button" class="ui-button ui-corner-all ui-button-text-only" role="button" style="background: url(&quot;../web/Grid/Modern/backgroundHeader.png&quot;) repeat-x scroll 50% 100% transparent; font-size: 12px; font-weight: normal; border: 1px solid rgb(211, 211, 211); color: rgb(85, 85, 85); font-family: Verdana,Arial,sans-serif;" onmouseover="this.style.border='1px solid #999999';this.style.color='#212121';" onmouseout="this.style.border='1px solid #D3D3D3';this.style.color='#555555';" onclick='onClickAddRule();'>
                                                                                            <span class="ui-button-text" style="padding: 0.25em 1em;"><%= Resource.getProperty("utility.accesscontrol.documentrule.addrule", lang)%></span>
                                                                                        </button> --%>
                                                </div>
                                                <div align="center">
                                                    <table>
                                                        <tr>
                                                            <td>
                                                                <div id="SubmitButton" align="center"
                                                                    style="width: 100%; max-height: 30px; display: none;">
                                                                    <button type="button" class="ButtonLink"
                                                                        onclick="onClickSubmit()"
                                                                        style="margin-right: 18px; padding-bottom: 10px;">
                                                                        <table class="Button">
                                                                            <tr>
                                                                                <td class="Button_left"><img
                                                                                    class="Button_Icon Button_Icon_process"
                                                                                    alt="Submit" title="Submit"
                                                                                    src="../web/images/blank.gif" border="0"></img></td>
                                                                                <td class="Button_text"><%=Resource.getProperty( "utility.submit", lang)%>
                                                                                </td>
                                                                                <td class="Button_right"></td>
                                                                            </TR>
                                                                        </table>
                                                                    </button>
                                                                </div>
                                                            </td>
                                                            <td>
                                                                <div id="addButton"
                                                                    style="width: 100%; max-height: 30px; display: none;">
                                                                    <button type="button" class="ButtonLink"
                                                                        onclick="onClickAddRule()"
                                                                        style="margin-right: 18px; padding-bottom: 10px;">
                                                                        <table class="Button">
                                                                            <tr>
                                                                                <td class="Button_left"><img
                                                                                    class="Button_Icon Button_Icon_process"
                                                                                    alt="Add Rule" title="Add Rule"
                                                                                    src="../web/images/blank.gif" border="0"></img></td>
                                                                                <td class="Button_text"><%=Resource.getProperty( "utility.accesscontrol.documentrule.addrule", lang)%>
                                                                                </td>
                                                                                <td class="Button_right"></td>
                                                                            </TR>
                                                                        </table>
                                                                    </button>
                                                                </div>
                                                            </td>
                                                        </tr>
                                                    </table>
                                                </div>
                                            </TD>
                                        </TR>
                                    </TABLE>
                                </TD>
                            </TR>
                        </TABLE>
                    </DIV>
                    <TABLE width="100%" border="0" cellspacing="0" cellpadding="0"
                        class="Menu_ContentPane_Bottom" id="tdbottomSeparator">
                        <TR>
                            <TD class="Main_Bottom_bg"><IMG
                                src="../web/images/blank.gif" border="0"></IMG></TD>
                        </TR>
                    </TABLE>
                </TD>
                <TD valign="top">
                    <TABLE width="100%" border="0" cellspacing="0" cellpadding="0"
                        class="Main_ContentPane_Right" id="tdrightSeparator">
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
<script type="text/javascript">
                                                    onLoadDo();
                                                </script>
<script type="text/javascript" src="../web/js/jquery-1.7.2.min.js"></script>
<script type="text/javascript" src="../web/js/jquery/grid.locale-en.js"></script>
<script type="text/javascript"
    src="../web/js/jquery/jquery.jqGrid.min.js"></script>
<script type="text/javascript" src="../web/js/jquery/jquery-ui.js"></script>
<script type="text/javascript">
                                                    var isDialogOpen = 0,
                                                        lastAddedRowId, lastAddedRoleCol, totalNoOfRule = 0;
                                                    var ruleGrid = jQuery("#DocumentRuleGrid"),
                                                        ruleJSONObj = null,
                                                        ruleApprovalLength = <%= ruleApprovalLength %> ;
                                                    var roleListJSON = '<%= request.getAttribute("RoleListJSON").toString().replace("\\", "\\\\").replace("'", "\\\'") %>';
                                                    var roleJSONObj = JSON && JSON.parse(roleListJSON) || $.parseJSON(roleListJSON);
                                                    var allowReservation = false;
                                                    var contractCategoryRole = false;
                                                    var isMultiRuleRole = 'N';
                                                    var docType = null;
                                                    var allowReservationLabel = '<%= Resource.getProperty("utility.accesscontrol.documentrule.radio.label.reservefunds", lang) %>';
                                                    var contractCategoryLabel ='<%= Resource.getProperty("utility.accesscontrol.contractcategorylabel", lang) %>';
                                                    var roleLabel = "<%= Resource.getProperty("utility.role",lang) %>";
                                                    var requesterArray =[], isRequesterDuplicate=false, isRequester='N',ismultipleRequester=false;
                                                    
                                                    function onClickRefresh() {
                                                        submitCommandForm('DEFAULT', true, null, 'DocumentRule', '_self', null, true);
                                                        return false;
                                                    }

                                                    function reSizeGrid() {
                                                        var w, h;
                                                        if (window.innerWidth) {
                                                            w = window.innerWidth - 65;
                                                            h = window.innerHeight - 300;
                                                        } else if (document.body) {
                                                            w = document.body.clientWidth - 65;
                                                            h = document.body.clientHeight - 300;
                                                        }
                                                        if (w <= 1000)
                                                            w = 1000;
                                                        if (h <= 250)
                                                            h = 250;
                                                        ruleGrid.setGridWidth(w, true);
                                                        ruleGrid.setGridHeight(h, true);
                                                        ChangeJQGridAllRowColor(ruleGrid);
                                                        $(".DivTDRole").parent().css("vertical-align", "top");
                                                        $("#DocumentRuleGrid td").attr("title", "");
                                                    }

                                                    function hideMessageBar() {
                                                        document.getElementById("messageBoxID").style.display = "none";
                                                    }

                                                    function getRoleName(id) {
                                                        var rl = roleJSONObj.RoleList.length;
                                                        for (var x = 0; x < rl; x++) {
                                                            if (id == roleJSONObj.RoleList[x].id) {
                                                                return roleJSONObj.RoleList[x].name.toString();
                                                            }
                                                        }
                                                    }
                                                    
                                                    $("#inpIsMultiRule").change(function(){
                                                        if($("#inpIsMultiRule").is(':checked')){
                                                            isMultiRuleRole = 'Y';
                                                            ruleGrid.jqGrid('hideCol',["value"]);
                                                            $("#addButton").show();
                                                        }
                                                        else{
                                                            $("#addButton").hide();
                                                            ruleGrid.jqGrid('showCol',["value"]);
                                                            isMultiRuleRole = 'N';
                                                        }
                                                        reSizeGrid();
                                                    });
    
                                                    function getDocumentRule() {
                                                        hideMessageBar();
                                                        totalNoOfRule = 0;
                                                        ruleGrid.jqGrid('clearGridData');
                                                        docType = document.getElementById("inpDocumentType").value;
                                                        var org = document.getElementById("inpOrganization").value;
                                                        
                                                        if (docType == "EUT_101" || docType == "EUT_109" || docType == "EUT_110" || docType == "EUT_111" || docType == "EUT_118" || docType=="EUT_119" || docType=="EUT_104" || docType=="EUT_120" || docType=="EUT_105" || docType == "EUT_121" || docType == "EUT_106")
                                                            allowReservation = true;
                                                        else
                                                            allowReservation = false;
                                                        
                                                        
                                                        if(docType == "EUT_127" ){
                                                            contractCategoryRole=true;
                                                        }else{
                                                            contractCategoryRole =false;
                                                        }
                                                        
                                                        
                                                        if(docType=="EUT_119" || docType=="EUT_104"|| docType=="EUT_120" || docType=="EUT_105" || docType=="EUT_121" || docType == "EUT_106"){
                                                            isMultiRuleRole = 'N';
                                                            ruleGrid.jqGrid('hideCol',["value"]);
                                                            $("#addButton").hide();
                                                        }
                                                        else if(docType == "EUT_112"|| docType == "EUT_111" || docType == "EUT_118" || docType == "EUT_116") {
                                                            isMultiRuleRole = 'Y';
                                                            ruleGrid.jqGrid('hideCol',["value"]);
                                                            $("#addButton").show();
                                                        }
                                                        else if(docType == "EUT_108" || docType == "EUT_117" || docType == "EUT_122" || docType == "EUT_125"){
                                                            $("#addButton").show();
                                                            ruleGrid.jqGrid('showCol',["value"]);
                                                            isMultiRuleRole = 'N';
                                                        }
                                                        else{
                                                            $("#addButton").hide();
                                                            ruleGrid.jqGrid('showCol',["value"]);
                                                            isMultiRuleRole = 'N';
                                                        }
                                                        
                                                        if(docType == "EUT_103" || docType == "EUT_120" || docType == "EUT_121"){
                                                            document.getElementById("inpOrganization").value = '0';
                                                            $('#inpOrganization').attr("disabled",true);
                                                        }else{
                                                            $('#inpOrganization').attr("disabled",false);
                                                        }
                                                        
                                                        if (docType == "0" || org == "00") {
                                                            $("#AddRuleButton, #SubmitButton,#addButton").hide();
                                                        } else {
                                                            
                                                            showProcessBar(true, 1);
                                                            $.post('<%= request.getContextPath().toString() + request.getAttribute("ServletPath").toString() %>', {
                                                                inpAction: 'GetRuleList',
                                                                requestType: 'A',
                                                                inpDocumentType: docType,
                                                                inpOrganization: document.getElementById("inpOrganization").value
                                                            }, function(data) {
                                                                ruleJSONObj = data;
                                                                isMultiRuleRole = data.MultiRule;
                                                                addRule(docType, ruleJSONObj);
                                                                $("#AddRuleButton, #SubmitButton").show();
                                                                if(isMultiRuleRole == 'Y')
                                                                    $("#inpIsMultiRule").prop('checked', true);
                                                                else
                                                                    $("#inpIsMultiRule").prop('checked', false);
                                                                if(docType == "EUT_119" || docType=="EUT_104" || docType=="EUT_120" || docType=="EUT_105" || docType=="EUT_121" || docType == "EUT_106"){
                                                                    $("#addButton").hide();
                                                                }
                                                                else if(allowReservation || isMultiRuleRole == 'Y' || docType == "EUT_108" ||docType == "EUT_117"||docType == "EUT_122" || docType == "EUT_125" || docType == "EUT_127"||docType == "EUT_131"){
                                                                      $("#addButton").show();
                                                                     
                                                                }
                                                                else
                                                                    $("#addButton").hide();
                                                                showProcessBar(false);
                                                            });
                                                        }
                                                        reSizeGrid();
                                                    }

                                                    function addRule(docType, jsonObj) {
                                                        totalNoOfRule = 0;
                                                        ruleGrid.jqGrid('clearGridData');
                                                        var data = jsonObj['DT' + docType];
                                                        for (var i in data) {
                                                             var value = data[i].value,
                                                             rowNo = data[i].row,
                                                             rowID = data[i].id,
                                                             rowReservationRole = data[i].Reservation_role,
                                                             rowcontractcategory= data[i].contractcategory_role,
                                                             roleHTML = "";
                                                         var roleList = data[i].roleList,
                                                             roleListLen = roleList.length;
                                                         var ruleLine = {};
                                                         ruleLine.id = rowID;
                                                         ruleLine.deleterule = "1";
                                                         ruleLine['value'] = value;
                                                         ruleLine['row'] = rowNo;
                                                         ruleLine['Reservation_role'] =  rowReservationRole;
                                                         ruleLine['contractcategory_role'] =  rowcontractcategory;
                                                            for (var j = 1; j <= ruleApprovalLength; j++) {
                                                                roleHTML = '<div class="DivTDRole">';
                                                                if (roleList.length > 0) {
                                                                    var roleObj = data[i].roleList[j - 1];
                                                                    for (var k in roleObj) {
                                                                        var roleListObj = roleObj[k];
                                                                        for (var m in roleListObj) {
                                                                            roleHTML += '<div style="padding: 2px;"><div class="RoleBoxOuter"><div class="RoleBoxText">' + getRoleName(roleListObj[m].role) + '</div><div onclick="deleteRole(this, \'' + rowID + '\', \'' + j + '\', \'' + roleListObj[m].role + '\')" class="RoleBoxImg"></div></div></div>';
                                                                        }
                                                                    }
                                                                }
                                                                roleHTML += '</div>';
                                                                roleHTML += '<div title="" onclick="openRolePopup(\'' + rowID + '\', \'' + j + '\')" class="RoleAddLink"><%= Resource.getProperty("utility.add", lang) %></div>';

                                                                /* if (allowReservation)
                                                                    roleHTML += '<div id="AllowReservationDiv" class="allowReservation"> ' + allowReservationLabel + ' <input type="radio" name="AllowReservation" id="' + rowID + '_' + j + '"/></div>'; */

                                                                ruleLine['role' + j] = roleHTML;
                                                            }
                                                            //console.log("Rule Line: "+JSON.stringify(ruleLine));
                                                            ruleGrid.jqGrid('addRowData', rowID, ruleLine);
                                                            totalNoOfRule++;
                                                        }
                                                        if(isMultiRuleRole == 'Y' || docType == 'EUT_103')
                                                            ruleGrid.jqGrid('hideCol',["value"]);
                                                        else
                                                            ruleGrid.jqGrid('showCol',["value"]);
                                                        
                                                        reSizeGrid();
                                                    }
                                                    $(function() {
                                                        var direction="ltr";
                                                        var inpLanguage=document.getElementById("inpLanguageId").value;
                                                            if(inpLanguage=="ar_SA"){
                                                                    direction="rtl";
                                                                 }
                                                        function formatDeleteRule(el, cellval, opts) {
                                                            if (el != "undefined" && el != undefined && el != "" && el != " ")
                                                                return "<div style='text-align: center; margin-top: 1px;'><img style='cursor: pointer;' title='<%= Resource.getProperty("utility.delete", lang) %>' src='../web/images/remove.png' onclick=\"deleteRule('" + cellval.rowId + "')\"></img></div>";
                                                        }

                                                        function formatRuleValue(el, cellval, opts) {
                                                                if (el != "undefined" && el != undefined && el != "" && el != " ") {
                                                                    var reserveFundsHTML = "";
                                                                    var optionsHTML = "<option value='0'>" + "<%= Resource.getProperty("utility.comboselect", lang)%>" + "</option>";
                                                                    /* 
                                                                    console.log("opts: "+JSON.stringify(opts));                                                                 
                                                                    console.log("el: "+JSON.stringify(cellval));
                                                                    console.log("el: "+el); */
                                                                        var htmlString = "<div style='padding: 3px;'><input type='text' role='jqtextbox' disabled='disabled' id='value_" + cellval.rowId + "' name='value_" + cellval.rowId + "' value='" + el + "' class='dojoValidateValid number' " +
                                                                        "outputformat='priceEdition' style='text-align:right; width: 100%;' onkeydown=\"return isFloatOnKeyDown(event);\" onfocus=\"numberInputEvent('onblur', this);\" onblur=\"numberInputEvent('onblur', this);\"></input></div>";
                                                                       
                                                                        if (allowReservation) {
                                                                         htmlString = "<div style='padding: 3px;'><input type='text' role='jqtextbox' id='value_" + cellval.rowId + "' name='value_" + cellval.rowId + "' value='" + el + "' class='dojoValidateValid required number' " +
                                                                            "outputformat='priceEdition' style='text-align:right; width: 100%;' onkeydown=\"return isFloatOnKeyDown(event);\" onfocus=\"numberInputEvent('onblur', this);\" onblur=\"numberInputEvent('onblur', this);\"></input></div>";
                                                                        
                                                                        reserveFundsHTML = reserveFundsHTML + " <div style='padding: 3px;'> <span class='LabelText'>" + allowReservationLabel + "</span>";
                                                                        reserveFundsHTML = reserveFundsHTML + " <select name='inpReserveFunds_" + cellval.rowId + "'  id='inpReserveFunds_" + cellval.rowId + "'  class='ComboKey'>";


                                                                        for (var i = 1; i <= ruleApprovalLength; i++) {
                                                                            if("Role"+i == opts.Reservation_role)
                                                                                optionsHTML = optionsHTML + "<option selected='selected' value=Role" + i + ">" + roleLabel + "" + i + "</option>";
                                                                            else
                                                                                optionsHTML = optionsHTML + "<option value=Role" + i + ">" + roleLabel + "" + i + "</option>";
                                                                        }

                                                                        reserveFundsHTML = reserveFundsHTML + optionsHTML + " </select>";
                                                                        reserveFundsHTML = reserveFundsHTML + "</div> ";
                                                                    }
                                                                        
                                                                        if (contractCategoryRole) {
                                                                           
                                                                           reserveFundsHTML = reserveFundsHTML + " <div style='padding: 3px;'> <span class='LabelText'>" + contractCategoryLabel + "</span>";
                                                                           reserveFundsHTML = reserveFundsHTML + " <select name='inpcategorycheck_" + cellval.rowId + "'  id='inpcategorycheck_" + cellval.rowId + "'  class='ComboKey'>";


                                                                           for (var i = 1; i <= ruleApprovalLength; i++) {
                                                                               if("Role"+i == opts.contractcategory_role)
                                                                                   optionsHTML = optionsHTML + "<option selected='selected' value=Role" + i + ">" + roleLabel + "" + i + "</option>";
                                                                               else
                                                                                   optionsHTML = optionsHTML + "<option value=Role" + i + ">" + roleLabel + "" + i + "</option>";
                                                                           }

                                                                           reserveFundsHTML = reserveFundsHTML + optionsHTML + " </select>";
                                                                           reserveFundsHTML = reserveFundsHTML + "</div> ";
                                                                       }
                                                                        
                                                                        
                                                                    if (( allowReservation   && (docType == 'EUT_111' || docType == 'EUT_118')) || (docType == 'EUT_108') || (docType == 'EUT_117') || (docType == 'EUT_122') || (docType == 'EUT_125') ) {
                                                                        htmlString = "<div style='padding: 3px;'><input type='text' role='jqtextbox' id='value_" + cellval.rowId + "' name='value_" + cellval.rowId + "' value='" + el + "' class='dojoValidateValid required number' " +
                                                                        "outputformat='priceEdition' style='text-align:right; width: 100%;' onkeydown=\"return isFloatOnKeyDown(event);\" onfocus=\"numberInputEvent('onblur', this);\" onblur=\"numberInputEvent('onblur', this);\"></input></div>";
                                                                        reserveFundsHTML = "";
                                                                    }
                                                                    if(allowReservation && (docType=='EUT_119' || docType=='EUT_120' || docType=="EUT_105" || docType=='EUT_104' ||  docType == "EUT_121" || docType == "EUT_106")){
                                                                        var htmlString = "<div style='padding: 3px;display:none;'><input type='text' role='jqtextbox' disabled='disabled' id='value_" + cellval.rowId + "' name='value_" + cellval.rowId + "' value='" + el + "' class='dojoValidateValid number' " +
                                                                        "outputformat='priceEdition' style='text-align:right; width: 100%;' onkeydown=\"return isFloatOnKeyDown(event);\" onfocus=\"numberInputEvent('onblur', this);\" onblur=\"numberInputEvent('onblur', this);\"></input></div>";
                                                                    }
                                                                    
                                                                    if( (docType == 'EUT_127')){
                                                                         htmlString = "<div style='padding: 3px;'><input type='text' role='jqtextbox' id='value_" + cellval.rowId + "' name='value_" + cellval.rowId + "' value='" + el + "' class='dojoValidateValid required number' " +
                                                                         "outputformat='priceEdition' style='text-align:right; width: 100%;' onkeydown=\"return isFloatOnKeyDown(event);\" onfocus=\"numberInputEvent('onblur', this);\" onblur=\"numberInputEvent('onblur', this);\"></input></div>";                                                                 
                                                                    }
                                                                   
                                                                }
                                                                return  htmlString + reserveFundsHTML;
                                                            }
                                                            // JQGrid
                                                        ruleGrid.jqGrid({
                                                            direction :direction,
                                                            colNames: ["", "<%= Resource.getProperty("utility.value",lang) %>" <%
                                                                for (int i = 1; i <= ruleApprovalLength; i++) { %> , "<%= Resource.getProperty("utility.role",lang) %> <%= i %>" <% } %>
                                                            ],
                                                            colModel: [{
                                                                    name: 'deleterule',
                                                                    index: 'deleterule',
                                                                    formatter: formatDeleteRule,
                                                                    width: 18
                                                                }, {
                                                                    name: 'value',
                                                                    index: 'value',
                                                                    formatter: formatRuleValue,
                                                                    width: 120
                                                                } <%
                                                                for (int i = 1; i <= ruleApprovalLength; i++) { %> , {
                                                                        name: 'role<%= i %>',
                                                                        index: 'role<%= i %>'
                                                                    } <%
                                                                } %>
                                                            ],
                                                            pager: '#DocumentRulePager',
                                                            datatype: "jsonstring",
                                                            rownumbers: true,
                                                            viewrecords: true,
                                                            autowidth: true,
                                                            shrinkToFit: true,
                                                            sortname: 'position',
                                                            sortorder: "asc",
                                                            height: "100%",
                                                            pgbuttons: false,
                                                            pgtext: '',
                                                            jsonReader: {
                                                                repeatitems: false
                                                            },
                                                            recordtext: "",
                                                            emptyrecords: "",
                                                            editurl: 'clientArray',
                                                            hoverrows: false,
                                                            beforeSelectRow: function(id, e) {
                                                                return false;
                                                            },
                                                            onRightClickRow: function(id) {
                                                                ruleGrid.jqGrid('resetSelection');
                                                                return false;
                                                            },
                                                            loadComplete: function() {
                                                                ruleGrid.trigger("resize");
                                                                //this.grid.hDiv.scrollLeft = this.grid.bDiv.scrollLeft;                                                                
                                                            },
                                                            caption: ''
                                                        });
                                                        ruleGrid.jqGrid('navGrid', '#DocumentRulePager', {
                                                            edit: false,
                                                            add: false,
                                                            del: false,
                                                            search: false,
                                                            view: false,
                                                            refresh: false
                                                        }, {}, {}, {});
                                                        ruleGrid.unbind("contextmenu");
                                                        changeJQGridDisplay("DocumentRuleGrid");
                                                        reSizeGrid();

                                                        $("#RoleSortable").sortable({
                                                            placeholder: "ui-state-highlight"
                                                        });
                                                        $("#RoleSortable").disableSelection();
                                                        $("#RoleSortable").sortable({
                                                            update: function(event, ui) {
                                                                var sorted = $('#RoleSortable').sortable('toArray');
                                                                for (var i = 0; i < sorted.length; i++) {
                                                                    var roleCB = false;
                                                                    if (document.getElementById("RoleCB" + sorted[i]).checked == true) roleCB = true;
                                                                    var roleHTML = '<div style="float: left; text-overflow: ellipsis; overflow:hidden; white-space:nowrap; width: 90%;"><label style="cursor: pointer;" for="RoleCB' + sorted[i] + '"><span>' + (i + 1) + '</span>. <span>' + getRoleName(sorted[i]) + '</span></label></div>';
                                                                    roleHTML += '<div style="float: right; margin-right: 10px;"><input type="checkbox" name="RoleCheckBox" class="RoleCheckBox"' + (roleCB ? ' checked="checked"' : '') + ' id="RoleCB' + sorted[i] + '"></div>';
                                                                    document.getElementById(sorted[i]).innerHTML = roleHTML;
                                                                }
                                                            }
                                                        });
                                                    });
                                                    document.onkeyup = function(e) {
                                                        e = e || window.event;
                                                        var charCode = e.keyCode || e.which;
                                                        if (charCode == 27 && isDialogOpen == 1) onClosePopup();
                                                    };

                                                    function onClosePopup() {
                                                        isDialogOpen = 0;
                                                        document.getElementById("OverlayDiv").style.display = "none";
                                                        document.getElementById('RoleListDialog').style.display = 'none';
                                                    }

                                                    function openRolePopup(currentRowId, currentRoleCol) {
                                                        var selectRoles = "",
                                                            omitRoles = "";
                                                        var docType = document.getElementById("inpDocumentType").value;
                                                        lastAddedRowId = currentRowId;
                                                        lastAddedRoleCol = currentRoleCol;
                                                        var data = ruleJSONObj['DT' + docType];
                                                        for (var i in data) {
                                                            var value = data[i].value,
                                                                rowNo = data[i].row,
                                                                rowID = data[i].id;
                                                            if (rowID != currentRowId) continue;
                                                            var roleList = data[i].roleList,
                                                                roleListLen = roleList.length;
                                                            for (var j = 1; j <= ruleApprovalLength; j++) {
                                                                if (roleList.length > 0) {
                                                                    var roleObj = data[i].roleList[j - 1];
                                                                    for (var k in roleObj) {
                                                                        var roleListObj = roleObj[k];
                                                                        var cRolCol = parseInt(k.replace("role", ""));
                                                                        for (var m in roleListObj) {
                                                                            if (cRolCol == currentRoleCol) {
                                                                                selectRoles += "," + roleListObj[m].role;
                                                                            }
                                                                            omitRoles += "," + roleListObj[m].role;
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        openRoleListDialog(selectRoles, omitRoles);
                                                    }

                                                    function openRoleListDialog(selectRoles, omitRoles) {
                                                        isDialogOpen = 1;
                                                        generateRoleDialog(selectRoles, omitRoles);
                                                        document.getElementById("OverlayDiv").style.display = "";
                                                        document.getElementById('RoleListDialog').style.display = "";
                                                    }

                                                    function generateRoleDialog(selectRoles, omitRoles) {
                                                        var rl = roleJSONObj.RoleList.length,
                                                            roleHTML = "",
                                                            no = 1;
                                                        for (var i = 0; i < rl; i++) {
                                                            if (selectRoles.indexOf("" + roleJSONObj.RoleList[i].id) != -1) {
                                                                roleHTML += '<li id="' + roleJSONObj.RoleList[i].id + '" class="ui-state-default" style="text-align: left">';
                                                                roleHTML += '<div style="float: left; text-overflow: ellipsis; overflow:hidden; white-space:nowrap; width: 90%;"><label style="cursor: pointer;" for="RoleCB' + roleJSONObj.RoleList[i].id + '"><span>' + (no++) + '</span>. <span>' + roleJSONObj.RoleList[i].name + '</span></label></div>';
                                                                roleHTML += '<div style="float: right; margin-right: 10px;"><input type="checkbox" checked="checked" name="RoleCheckBox" class="RoleCheckBox" id="RoleCB' + roleJSONObj.RoleList[i].id + '"></div>';
                                                                roleHTML += '</li>';
                                                            }
                                                        }
                                                        for (var i = 0; i < rl; i++) {
                                                            if (omitRoles.indexOf("" + roleJSONObj.RoleList[i].id) != -1)
                                                                continue;
                                                            roleHTML += '<li id="' + roleJSONObj.RoleList[i].id + '" class="ui-state-default" style="text-align: left">';
                                                            roleHTML += '<div style="float: left; text-overflow: ellipsis; overflow:hidden; white-space:nowrap; width: 90%;"><label style="cursor: pointer;" for="RoleCB' + roleJSONObj.RoleList[i].id + '"><span>' + (no++) + '</span>. <span>' + roleJSONObj.RoleList[i].name + '</span></label></div>';
                                                            roleHTML += '<div style="float: right; margin-right: 10px;"><input type="checkbox" name="RoleCheckBox" class="RoleCheckBox" id="RoleCB' + roleJSONObj.RoleList[i].id + '"></div>';
                                                            roleHTML += '</li>';
                                                        }
                                                        $("#RoleSortable").html(roleHTML);
                                                        var count = $("#RoleSortable").children().length;
                                                        if (count == 0) {
                                                            $("#DivNoRole").show();
                                                            $("#DivRoleHeader").hide();
                                                        } else {
                                                            $("#DivNoRole").hide();
                                                            $("#DivRoleHeader").show();
                                                        }
                                                    }

                                                    function onClickAddRule() {
                                                        var rowNo = (totalNoOfRule + 1);
                                                        var rowID = "RuleID" + rowNo;
                                                        var ruleLine = {};
                                                        ruleLine.id = rowID;
                                                        ruleLine.deleterule = "1";
                                                        ruleLine['value'] = "0.00";
                                                        ruleLine['row'] = rowNo;
                                                        for (var j = 1; j <= ruleApprovalLength; j++) {
                                                            roleHTML = '<div class="DivTDRole">';
                                                            roleHTML += '</div>';
                                                            roleHTML += '<div title="" onclick="openRolePopup(\'' + rowID + '\', \'' + j + '\')" class="RoleAddLink"><%= Resource.getProperty("utility.add", lang) %></div>';

                                                            /* if (allowReservation)
                                                                roleHTML += '<div id="AllowReservationDiv" class="allowReservation"> ' + allowReservationLabel + ' <input type="radio" name="AllowReservation" id="' + rowID + '_' + j + '"/></div>'; */

                                                            ruleLine['role' + j] = roleHTML;
                                                        }
                                                        ruleGrid.jqGrid('addRowData', rowID, ruleLine);
                                                        totalNoOfRule++;
                                                        var roleObj = {};
                                                        roleObj = {
                                                            "id": rowID,
                                                            "value": 0,
                                                            "row": rowNo,
                                                            "roleList": []
                                                        };
                                                        var docType = document.getElementById("inpDocumentType").value;
                                                        var data = ruleJSONObj['DT' + docType];
                                                        data.splice(data.length, 0, roleObj);
                                                        $("#SubmitButton").show();
                                                        if(allowReservation)
                                                             $("#addButton").show();

                                                    }

                                                    function deleteRule(currentRowId) {
                                                        OBConfirm('<%= Resource.getProperty("utility.accesscontrol.documentrule.delete.confirm", lang) %>', function(r) {
                                                            if (r) {
                                                                var docType = document.getElementById("inpDocumentType").value;
                                                                var data = ruleJSONObj['DT' + docType];
                                                                requesterArray.splice(0,requesterArray.length);
                                                                for (var i in data) {
                                                                    var value = data[i].value,
                                                                        rowNo = data[i].row,
                                                                        rowID = data[i].id;
                                                                    if (rowID == currentRowId) {
                                                                        data.splice(i, 1);
                                                                        ruleGrid.delRowData(currentRowId);
                                                                        totalNoOfRule--;
                                                                    }
                                                                }
                                                            }
                                                        });
                                                    }

                                                    function deleteRole(index, currentRowId, currentRoleCol, roleId) {
                                                        var docType = document.getElementById("inpDocumentType").value;
                                                        var data = ruleJSONObj['DT' + docType];
                                                        requesterArray.splice(0,requesterArray.length);
                                                        for (var i in data) {
                                                            var value = data[i].value,
                                                                rowNo = data[i].row,
                                                                rowID = data[i].id;
                                                            if (rowID != currentRowId)
                                                                continue;
                                                            var roleList = data[i].roleList,
                                                                roleListLen = roleList.length;
                                                            for (var j = 1; j <= ruleApprovalLength; j++) {
                                                                if (roleList.length > 0) {
                                                                    var roleObj = data[i].roleList[j - 1];
                                                                    for (var k in roleObj) {
                                                                        var roleListObj = roleObj[k];
                                                                        var cRolCol = parseInt(k.replace("role", ""));
                                                                        if (cRolCol == currentRoleCol) {
                                                                            for (var m in roleListObj) {
                                                                                if (("" + roleListObj[m].role) == roleId) {
                                                                                    roleListObj.splice(m, 1);
                                                                                    var deleteNode = index.parentNode.parentNode;
                                                                                    deleteNode.parentNode.removeChild(deleteNode);
                                                                                    break;
                                                                                }
                                                                            }
                                                                        }
                                                                        if (cRolCol > currentRoleCol)
                                                                            break;
                                                                        for (var m in roleListObj) {
                                                                            if (cRolCol == currentRoleCol) {
                                                                                roleListObj[m].no = (parseInt(m) + 1);
                                                                            }
                                                                        }
                                                                        if (roleListObj.length == 0) {
                                                                            data[i].roleList.splice(j - 1, 1);
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }

                                                    function onClickAddRole() {
                                                        var docType = document.getElementById("inpDocumentType").value;
                                                        var ckdRole = document.getElementsByName("RoleCheckBox"),
                                                            chkRoleList = "";
                                                        requesterArray.splice(0,requesterArray.length);
                                                        for (var j = 0; j < ckdRole.length; j++) {
                                                            if (ckdRole[j].checked == true) {
                                                                chkRoleList += "," + ckdRole[j].id;
                                                            }
                                                        }

                                                        if (chkRoleList == "") {
                                                            var data = ruleJSONObj['DT' + docType];
                                                            for (var i in data) {
                                                                if (lastAddedRowId == data[i].id) {
                                                                    var roleList = data[i].roleList,
                                                                        roleListLen = roleList.length;
                                                                    for (var j = 1; j <= ruleApprovalLength; j++) {
                                                                        if (roleList.length > 0) {
                                                                            var roleObj = data[i].roleList[j - 1];
                                                                            for (var k in roleObj) {
                                                                                var cRolCol = parseInt(k.replace("role", ""));
                                                                                if (cRolCol == lastAddedRoleCol) {
                                                                                    data[i].roleList.splice(j - 1, 1);
                                                                                    var roleHTML = '<div class="DivTDRole">';
                                                                                    roleHTML += '</div>';
                                                                                    roleHTML += '<div title="" onclick="openRolePopup(\'' + lastAddedRowId + '\', \'' + lastAddedRoleCol + '\')" class="RoleAddLink"><%= Resource.getProperty("utility.add", lang) %></div>';

                                                                                    /* if (allowReservation)
                                                                                        roleHTML += '<div id="AllowReservationDiv" class="allowReservation"> ' + allowReservationLabel + ' <input type="radio" name="AllowReservation" id="' + lastAddedRowId + '_' + lastAddedRoleCol + '"/></div>'; */

                                                                                    var rowData = ruleGrid.getRowData(lastAddedRowId),
                                                                                        cRow = 'role' + lastAddedRoleCol;
                                                                                    ruleGrid.jqGrid('setCell', lastAddedRowId, cRow, roleHTML);
                                                                                    break;
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        } else {
                                                            var chdRolArr = chkRoleList.split(",");
                                                            var data = ruleJSONObj['DT' + docType],
                                                                ruleRow = 0;
                                                            for (var i in data) {
                                                                if (lastAddedRowId == data[i].id) {
                                                                    ruleRow = i;
                                                                    break;
                                                                }
                                                            }
                                                            var roleList = data[ruleRow].roleList,
                                                                roleListLen = roleList.length;
                                                            var roleObj = {},
                                                                rolArr = [];
                                                            var roleHTML = '<div class="DivTDRole">';
                                                            var canReserve = false;
                                                            var selectValue ="",selectId= "";
                                                                var rl = roleJSONObj.RoleList.length;
                                                            for (var e = 1; e < chdRolArr.length; e++) {
                                                                /* console.log("chdRolArr[e]: "+chdRolArr[e]);
                                                                
                                                                if(allowReservation){
                                                                    selectId = "inpReserveFunds_"+lastAddedRowId;
                                                                    console.log("lastAddedRoleCol: "+lastAddedRowId);
                                                                    selectValue = $("#"+selectId).val();
                                                                    console.log("Select Value: "+selectValue);                                                              }
                                                                else 
                                                                    canReserve = false; */
                                                                    
                                                                if(chdRolArr.length >2){
                                                                    for (var i = 0; i < rl; i++) {
                                                                        if (roleJSONObj.RoleList[i].id === chdRolArr[e].replace("RoleCB", "") && roleJSONObj.RoleList[i].isDummyRole ) {
                                                                             OBAlert('<%= Resource.getProperty("utility.dummyrole", lang)%>'); 
                                                                             return false;      
                                                                        }
    
                                                                    }

                                                                    
                                                                }
                                                                
                                                                if ("" == chdRolArr[e])
                                                                    continue;
                                                                var roleId = chdRolArr[e].replace("RoleCB", "");
                                                                rolArr[e - 1] = {
                                                                    "no": (e),
                                                                    "role": roleId,
                                                                    "reservation":canReserve,
                                                                    "requester":isRequester
                                                                };
                                                                roleHTML += '<div style="padding: 2px;"><div class="RoleBoxOuter"><div class="RoleBoxText">' + getRoleName(roleId) + '</div><div onclick="deleteRole(this, \'' + lastAddedRowId + '\', \'' + lastAddedRoleCol + '\', \'' + roleId + '\')" class="RoleBoxImg"></div></div></div>';
                                                            }
                                                            if (roleListLen == 0) {
                                                                var roleId ="";
                                                                roleObj['role' + lastAddedRoleCol] = rolArr;
                                                                roleList.splice(0, 0, roleObj);
                                                                roleId = rolArr[0].role;
                                                                rolArr[0].requester = 'Y';
                                                                                                                               
                                                                if($.inArray(roleId, requesterArray) >= 0){
                                                                    isRequesterDuplicate = true;
                                                                }
                                                                else {
                                                                    data[ruleRow].Requester_Role = roleId;
                                                                    requesterArray.push(roleId);        
                                                                }
                                                            } else {
                                                                var roleAdded = 0;
                                                                /* var roleId ="";
                                console.log(roleList.splice(0, 0, roleObj));
                                                                roleObj['role' + lastAddedRoleCol] = rolArr;
                                                                roleList.splice(0, 0, roleObj);
                                                                roleId = rolArr[0].role;
                                                                rolArr[0].requester = 'Y';
                                                                 
                                                                if($.inArray(roleId, requesterArray) >= 0){
                                                                    isRequesterDuplicate = true;
                                                                }
                                                                else {
                                                                    data[ruleRow].Requester_Role = roleId;
                                                                    requesterArray.push(roleId);        
                                                                } */
                                                                for (var i in roleList) {
                                                                    for (var j in roleList[i]) {
                                                                        var cRolCol = parseInt(j.replace("role", ""));
                                                                        if (lastAddedRoleCol == cRolCol) {
                                                                            roleAdded = 1;
                                                                            break;
                                                                        }
                                                                    }
                                                                }
                                                                if (roleAdded == 0) {
                                                                    roleObj['role' + lastAddedRoleCol] = rolArr;
                                                                    roleList.splice(lastAddedRoleCol - 1, 0, roleObj);
                                                                } else {
                                                                    var breakFlag = false;
                                                                    roleObj['role' + lastAddedRoleCol] = rolArr;
                                                                    for (var j = 1; j <= ruleApprovalLength; j++) {
                                                                        if (roleList.length > 0) {
                                                                            var cRoleObj = data[ruleRow].roleList[j - 1];
                                                                            for (var k in cRoleObj) {
                                                                                var roleListObj = cRoleObj[k];
                                                                                var cRolCol = parseInt(k.replace("role", ""));
                                                                                if (cRolCol == lastAddedRoleCol) {
                                                                                    roleList.splice(j - 1, 1, roleObj);
                                                                                    breakFlag = true;
                                                                                    break;
                                                                                }
                                                                                if (breakFlag)
                                                                                    break;
                                                                            }
                                                                        }
                                                                        if (breakFlag)
                                                                            break;
                                                                    }
                                                                }
                                                            }
                                                            roleHTML += '</div>';
                                                            roleHTML += '<div title="" onclick="openRolePopup(\'' + lastAddedRowId + '\', \'' + lastAddedRoleCol + '\')" class="RoleAddLink"><%= Resource.getProperty("utility.add", lang) %></div>';

                                                            /* if (allowReservation)
                                                                roleHTML += '<div id="AllowReservationDiv" class="allowReservation"> ' + allowReservationLabel + ' <input type="radio" name="AllowReservation" id="' + lastAddedRowId + '_' + lastAddedRoleCol + '"/></div>'; */

                                                            var rowData = ruleGrid.getRowData(lastAddedRowId),
                                                                cRow = 'role' + lastAddedRoleCol;
                                                            ruleGrid.jqGrid('setCell', lastAddedRowId, cRow, roleHTML);
                                                        }
                                                        onClosePopup();
                                                    }

                                                    function onClickSubmit() { 
                                                        var valid = true; 
                                                        var docType = document.getElementById('inpDocumentType').value; 
                                                        var organization = document.getElementById('inpOrganization').value;
                                                        var isMultiRuleSelected;
                                                        if($("#inpIsMultiRule").is(':checked'))
                                                            isMultiRuleSelected = 'Y';
                                                        else
                                                            isMultiRuleSelected = 'N';
                                                        
                                                       var data = ruleJSONObj['DT' + docType];
                                                        for(var k in data){
                                                            var roleList=data[k].roleList;
                                                            if($("#inpIsMultiRule").is(':checked')){
                                                            if(roleList.length > 0){
                                                                var roleIdLen=roleList[0].role1.length;
                                                                 data[k].Requester_Role = roleList[0].role1[0].role;
                                                                if(roleIdLen > 1){
                                                                    OBAlert('<%= Resource.getProperty("utility.accesscontrol.documentrule.alreadyrole.define", lang)%>'); 
                                                                    return false; 
                                                                }
                                                            }
                                                            }
                                                            
                                                            //break;
                                                       // }
                                                        } 
                                                        
                                                        // Validate Same Value
                                                        {
                                                            var idList = ruleGrid.getDataIDs(),idCount = idList.length; 
                                                            if (idCount == 0) {
                                                            /* OBAlert('<%= Resource.getProperty("utility.accesscontrol.documentrule.value0mandatory", lang)%>', function(r) {
                                                            onClickAddRule();
                                                            });
                                                            return false; */ 
                                                        } 
                                                        var rowData = '', id = '', selectId='',selectValue = "", valueArr = new Array(), errorMsg = ""; 
                                                        for (var i = 0; i < idList.length; i++) { 
                                                            rowData = ruleGrid.getRowData(idList[i]); 
                                                            id = 'value_' + idList[i]; 
                                                            console.log(id);
                                                            var val = document.getElementById(id).value.replace(/,/g, ''); 
                                                            if(isMultiRuleSelected == 'N'){
                                                                if (val == '' || isNaN(parseInt(val))) { 
                                                                    document.getElementById(id).value = ''; 
                                                                    OBAlert('<%= Resource.getProperty("utility.accesscontrol.documentrule.valuenotvalid", lang)%> ' + (parseInt(i) + 1)); 
                                                                    document.getElementById(id).focus(); 
                                                                    return false; 
                                                                } else { 
                                                                    val = parseFloat(val); 
                                                                    if (valueArr.indexOf(val) == -1) { 
                                                                        valueArr[i] = val; 
                                                                    } else { 
                                                                        document.getElementById(id).value = ''; 
                                                                        OBAlert('<%= Resource.getProperty("utility.accesscontrol.documentrule.valueexists", lang)%> ' + (parseInt(i) + 1)); 
                                                                        document.getElementById(id).focus(); 
                                                                        return false; 
                                                                    } 
                                                                }
                                                            }
                                                            else {
                                                                
                                                            }
                                                            if(allowReservation && ( docType != 'EUT_111' && docType != 'EUT_118')) { 
                                                                selectId = "inpReserveFunds_"+idList[i]; 
                                                                errorMsg = '<%= Resource.getProperty("utility.accesscontrol.documentrule.validation.error.emptyreservation", lang)%>'; 
                                                                if($("#"+selectId).val()=="0"){ 
                                                                    OBAlert(errorMsg.replace("@@",i+1)); 
                                                                    document.getElementById(selectId).focus(); 
                                                                    return false; 
                                                                }else{ 
                                                                    console.log(selectValue);
                                                                    selectValue = $("#"+selectId).val(); 
                                                                    selectValue = selectValue.replace("Role",""); 
                                                                } 
                                                            } 
                                                            
                                                            if(contractCategoryRole) { 
                                                                selectId = "inpcategorycheck_"+idList[i]; 
                                                                errorMsg = '<%= Resource.getProperty("utility.accesscontrol.documentrule.validation.error.emptycategory", lang)%>'; 
                                                                if($("#"+selectId).val()=="0"){ 
                                                                    OBAlert(errorMsg.replace("@@",i+1)); 
                                                                    document.getElementById(selectId).focus(); 
                                                                    return false; 
                                                                }else{ 
                                                                    console.log(selectValue);
                                                                    selectValue = $("#"+selectId).val(); 
                                                                    selectValue = selectValue.replace("Role",""); 
                                                                } 
                                                            } 
                                                                                                                    
                                                            
                                                        }
                                                        if (idCount > 0 && valueArr.indexOf(0) == -1 && (isMultiRuleSelected == 'N')) { 
                                                            OBAlert('<%= Resource.getProperty("utility.accesscontrol.documentrule.value0mandatory", lang)%>'); 
                                                            return false; 
                                                        } 
                                                        if(isMultiRuleSelected == 'Y' && isRequesterDuplicate){
                                                            OBAlert('<%=Resource.getProperty("utility.accesscontrol.documentrule.validation.error.duplicatemultirole", lang) %>');
                                                            return false;
                                                        }
                                                    }   

                                                    // Checking Empty Data rule by rule 
                                                    if (validateEmptyValues(docType) == false) 
                                                        valid = false; 
                                                    else 
                                                        valid = true; 
                                                    valid = true; 
                                                    if (valid) { 
                                                        OBConfirm('<%= Resource.getProperty("utility.accesscontrol.documentrule.submit", lang) %>', function(r) { 
                                                            if (r) { 
                                                                var data = ruleJSONObj['DT' + docType];
                                                                console.log(JSON.stringify(data));
                                                                for (var i in data) { 
                                                                    var valueId = 'value_' + data[i].id; 
                                                                    data[i].value = document.getElementById(valueId).value; 
                                                                    data[i].Reservation_role=$("#inpReserveFunds_"+data[i].id).val();
                                                                } 
                                                                showProcessBar(true, 2); 
                                                                $.post('<%= request.getContextPath().toString() + request.getAttribute("ServletPath").toString() %>', { 
                                                                    inpAction: 'Submit', 
                                                                    requestType: 'A', 
                                                                    inpDocumentType: docType, 
                                                                    inpOrganization: organization, 
                                                                    inpMultiRule: isMultiRuleSelected,
                                                                    inpRuleData: JSON.stringify(ruleJSONObj) 
                                                                }, function(data) { 
                                                                    var resObj = data;                                                                    
                                                                    displayMessage(resObj.msgtype, resObj.msgtypetitle, resObj.msg); 
                                                                    if (resObj.result == '1') { 
                                                                        ruleJSONObj = JSON.parse(resObj.ruleList); 
                                                                        addRule(docType, ruleJSONObj); 
                                                                    } 
                                                                    showProcessBar(false); 
                                                                }); 
                                                            } 
                                                        }); 
                                                    } 
                                                    } 

                                                    function validateEmptyValues(docType) {
                                                        var data = ruleJSONObj['DT' + docType];
                                                        var selectValue = "", errorMsg="";
                                                         
                                                        for (var i in data) {
                                                            var roleList = data[i].roleList,roleListLen = roleList.length;
                                                             if (allowReservation && (docType != 'EUT_111'&& docType != 'EUT_118')){
                                                                selectValue = $("#inpReserveFunds_"+data[i].id).val();
                                                                selectValue = selectValue.replace("Role","");
                                                            }
                                                            else{
                                                                selectValue = "";
                                                            } 
                                                            
                                                            
                                                            var lastEmptyCol = 0,lastSavedCol = 1;
                                                            if (roleListLen == 0) { 
                                                                OBAlert('<%= Resource.getProperty("utility.accesscontrol.documentrule.roleempty", lang)%> ' + (parseInt(i) + 1)); 
                                                                return false; 
                                                            } else if (roleListLen > 0) { 
                                                                var savedArr = new Array(); 
                                                                for (var j = 1; j <= ruleApprovalLength; j++) { 
                                                                    var roleObj = data[i].roleList[j - 1]; 
                                                                    for (var k in roleObj) { 
                                                                        var roleListObj = roleObj[k]; 
                                                                        if (roleListObj.length == 0) 
                                                                            continue; 
                                                                        var cRolCol = parseInt(k.replace("role", "")); 
                                                                        savedArr[savedArr.length] = cRolCol; 
                                                                    } 
                                                                } 
                                                                
                                                                for (var j = 1; j <= savedArr.length; j++) { 
                                                                    if (j != savedArr[j - 1] && savedArr[j - 1] > savedArr.length) { 
                                                                        OBAlert('<%= Resource.getProperty("utility.accesscontrol.documentrule.roleorder", lang)%> ' + (parseInt(i) + 1)); 
                                                                        return false; 
                                                                    } 
                                                                }
                                                                
                                                                
                                                                if(allowReservation && (docType != 'EUT_111' && docType != 'EUT_118')){
                                                                    if(allowReservation && $.inArray(Number(selectValue),savedArr)<0){
                                                                        errorMsg = '<%= Resource.getProperty("utility.accesscontrol.documentrule.validation.error.noreservationrole", lang)%>'; 
                                                                        errorMsg = errorMsg.replace("@@",selectValue).replace("$$",data[i].id.replace("RuleID",""));
                                                                        OBAlert(errorMsg); 
                                                                        return false;
                                                                    }else {
                                                                        var roleListLength = JSON.stringify(data[i].roleList.length);
                                                                        for(var index = 0; index<roleListLength; index++){
                                                                            //console.log("Role: "+JSON.stringify(data[i].roleList[index]));
                                                                            $.each(data[i].roleList[index], function(k, v) {
                                                                                for(var vlen = 0; vlen<v.length; vlen++) {
                                                                                     if(k==$("#inpReserveFunds_"+data[i].id).val().toLowerCase()) {
                                                                                         //console.log(k + ' is ' + v.length);
                                                                                         v[vlen].reservation = true;        
                                                                                    }
                                                                                     else 
                                                                                         v[vlen].reservation = false;
                                                                                }
                                                                            });
                                                                        }
                                                                    }
                                                                }
                                                                
                                                                
                                                           
                                                               
                                                                
                                                                if(contractCategoryRole){
                                                                    
                                                                    var roleList = data[i].roleList,roleListLen = roleList.length;
                                                                        selectValue = $("#inpcategorycheck_"+data[i].id).val();
                                                                        selectValue = selectValue.replace("Role","");
                                                                    
                                                                    
                                                                    if(contractCategoryRole && $.inArray(Number(selectValue),savedArr)<0){
                                                                        errorMsg = '<%= Resource.getProperty("utility.accesscontrol.documentrule.validation.error.nocontractcategory", lang)%>'; 
                                                                        errorMsg = errorMsg.replace("@@",selectValue).replace("$$",data[i].id.replace("RuleID",""));
                                                                        OBAlert(errorMsg); 
                                                                        return false;
                                                                    }else {
                                                                        var roleListLength = JSON.stringify(data[i].roleList.length);
                                                                        for(var index = 0; index<roleListLength; index++){
                                                                            //console.log("Role: "+JSON.stringify(data[i].roleList[index]));
                                                                            $.each(data[i].roleList[index], function(k, v) {
                                                                                for(var vlen = 0; vlen<v.length; vlen++) {
                                                                                     if(k==$("#inpcategorycheck_"+data[i].id).val().toLowerCase()) {
                                                                                         //console.log(k + ' is ' + v.length);
                                                                                         v[vlen].contractcategory = true;        
                                                                                    }
                                                                                     else 
                                                                                         v[vlen].contractcategory = false;
                                                                                }
                                                                            });
                                                                        }
                                                                    }
                                                                }
                                                                
                                                                
                                                            } 
                                                        }
                                                        ruleJSONObj['DT' + docType] = data;
                                                        //console.log("Data: "+JSON.stringify(ruleJSONObj));
                                                        return true; 
                                                    }  
                                                </script>

</HTML>