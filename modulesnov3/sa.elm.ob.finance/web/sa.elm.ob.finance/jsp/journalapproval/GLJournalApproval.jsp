<%@page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page import="sa.elm.ob.finance.properties.Resource"
    errorPage="/web/jsp/ErrorPage.jsp"%>
<%
    String lang = ((String) session.getAttribute("#AD_LANGUAGE"));
	String style="../web/skins/ltr/org.openbravo.userinterface.skin.250to300Comp/250to300Comp/Openbravo_ERP.css";
	String toolBarStyle="../web/js/common/CommonFormLtr.css";
	String DialogBoxStyle="../web/js/common/CommonDialogFormLtr.css";
	if(lang.equals("ar_SA")){
	 style="../web/skins/rtl/org.openbravo.userinterface.skin.250to300Comp/250to300Comp/Openbravo_ERP_250.css";
	 toolBarStyle="../web/js/common/CommonFormRtl.css";
	 DialogBoxStyle="../web/js/common/CommonDialogFormRtl.css";
	}
%>
<HTML xmlns:="http://www.w3.org/1999/xhtml">
<HEAD>
<META http-equiv="Content-Type" content="text/html; charset=utf-8"></META>
<LINK rel="stylesheet" type="text/css" href="<%=style %>" id="paramCSS"></LINK>
<link rel="stylesheet" type="text/css" href="<%=toolBarStyle %>" />
<link rel="stylesheet" type="text/css" href="<%=DialogBoxStyle %>" />
<link rel="stylesheet" type="text/css"
    href="../web/js/jquery/jquery-ui-1.8.11.custom.css" />
<link rel="stylesheet" type="text/css"
    href="../web/js/themes/base/jquery.ui.tabs.css"></link>
<link rel="stylesheet" type="text/css"
    href="../web/js/jquery/ui.jqgrid.css" />
<style type="text/css">
table {
    font-size: 12px;
}

.ui-button:HOVER {
    border: 1px solid #999999 !important;
    color: #212121 !important;
}

.ContentDetail {
    color: #404040;
    font-size: 12px;
    font-weight: normal;
    font-family: tahoma, arial, helvetica, sans-serif;
    white-space: nowrap;
}

.ContentDetailBold {
    font-weight: bold;
}

div.CommentsLink {
    text-align: right;
    cursor: pointer;
    text-decoration: underline;
    font-family: Arial;
    color: #222222;
    font-size: 8px;
    font-weight: 700;
}

div.NoHistoryDialog {
    height: 150px;
    width: 400px;
    left: 35%;
    top: 20%;
    background: white;
    z-index: 5000001;
    border: 2px solid #ff9c30;
    position: absolute;
    border-radius: 5px;
    box-shadow: 0 0 20px #333;
}

div.CommentsHistoryDialog {
    height: 395px;
    width: 750px;
    left: 25%;
    top: 10%;
    background: white;
    z-index: 5000001;
    border: 6px solid #D6D6D6;
    position: absolute;
    border-radius: 5px;
    box-shadow: 0 0 20px #333;
}

div.RoleBoxOuter {
    top: 1px;
    display: inline-block;
    border: 1px solid #d9d9d9;
    border-bottom: 0px;
    background: #f5f5f5;
    border-radius: 3px 3px 0px 0px;
    padding: 2px 5px 2px 5px;
}

div.RoleBoxOuter span.RoleBoxText {
    font-family: Arial;
    color: #222222;
    font-size: 13px;
    font-weight: 700;
    white-space: normal;
}

div.NotesHistory {
    height: 240px;
}

table {
    border-collapse: collapse;
}

span.text {
    white-space: normal !important;
}

td#prev_PaymentOutPager {
    border-right: 0px;
}
</style>
<script type="text/javascript" src="../web/js/utils.js"></script>
<script type="text/javascript" src="../web/js/windowKeyboard.js"></script>
<script type="text/javascript" src="../web/js/common/common.js"></script>
<script type="text/javascript">
function validate(action) {
    return true;
}
function onResizeDo() {
    resizeArea();
    reSizeGrid();
}
function onLoadDo() {
    this.windowTables = new Array(new windowTableId('client', 'buttonOK'));
    setWindowTableParentElement();
    this.tabsTables = new Array(new tabTableId('tdtopTabs'));
    setTabTableParentElement();
    setBrowserAutoComplete(false);
    setFocusFirstControl(null, 'inpMailTemplate');
    updateMenuIcon('buttonMenu');
    resizeArea();
}
function onClickRefresh() {
    submitCommandForm('DEFAULT', true, null, 'GLjournalApproval', '_self', null, true);
    return false;
}   
</script>
</HEAD>
<BODY onresize="onResizeDo();" onunload="" leftmargin="0" topmargin="0"
    marginwidth="0" marginheight="0" id="paramBody">
    <FORM id="form" method="post" action="" name="frmMain">
        <INPUT type="hidden" name="Command"></INPUT> <INPUT type="hidden"
            name="IsPopUpCall" value="1"></INPUT> <INPUT type="hidden"
            name="inpLastFieldChanged"></INPUT> <INPUT type="hidden" name="act"
            id="act" value=""></INPUT> <INPUT type="hidden" name="inpJournalId"
            id="inpJournalId" value=""></INPUT> <INPUT type="hidden" name="type"
            id="type" value=""></INPUT> <INPUT type="hidden" name="inpGrandTotal"
            id="inpGrandTotal" value=""></INPUT> <INPUT type="hidden"
            name="inpComments" id="inpComments" value=""></INPUT> <INPUT
            type="hidden" name="inpEutNextRoleId" id="inpEutNextRoleId" value=""></INPUT>
        <INPUT type="hidden" name="inpFromUserRole" id="inpFromUserRole"
            value=""></INPUT> <INPUT type="hidden" name="inpToUserRole"
            id="inpToUserRole" value=""></INPUT> <INPUT type="hidden"
            name="inpOrgId" id="inpOrgId" value=""></INPUT> <INPUT type="hidden"
            name="ValidatePrice" id="ValidatePrice" outputformat="priceEdition"></INPUT>
        <INPUT type="hidden" name="ValidatePrice1" id="ValidatePrice1"
            outputformat="priceRelation"></INPUT>
       <INPUT type="hidden" name="inpLanguage" id="inpLanguageId"
            value="<%=lang%>"></INPUT>
        <jsp:include page="/web/jsp/ProcessBar.jsp" />
        <div id="GLJournalLines" title="GLJournalLines"
            style="display: none; padding: 1em; overflow-x: hidden;">
            <div align="center"
                style="width: 100%; margin: 0 auto; text-align: center;">
                <table
                    style="width: 70%; height: auto; border-spacing: 8px; border-collapse: separate; text-align: center; margin: 0 auto; margin-left: 5%;">
                    <tr>
                        <td class="DialogContent"><span
                            class="ContentDetailBold"><%= Resource.getProperty("finance.gljournalno",lang) %></span>
                        </td>
                        <td style="text-align: center;"><span
                            class="ContentDetailBold">:</span>
                        </td>
                        <td class="DialogContentDetail"><span
                            class="ContentDetailBold" id="spanJournalNo"></span>
                        </td>
                        <td class="DialogContent"><span
                            class="ContentDetailBold"><%= Resource.getProperty("finance.generalledger",lang) %></span>
                        </td>
                        <td style="text-align: center;"><span
                            class="ContentDetailBold">:</span>
                        </td>
                        <td class="DialogContentDetail"><span
                            class="ContentDetailBold" id="spanLedger"></span>
                        </td>
                    </tr>
                    <tr>
                        <td class="DialogContent"><span
                            class="ContentDetailBold"><%= Resource.getProperty("finance.documentdate",lang) %></span>
                        </td>
                        <td style="text-align: center;"><span
                            class="ContentDetailBold">:</span>
                        </td>
                        <td class="DialogContentDetail"><span class="ContentDetailBold"
                            id="spanDocumentDate"></span>
                        </td>
                        <td class="DialogContent"><span
                            class="ContentDetailBold"><%= Resource.getProperty("finance.accountingdate",lang) %></span>
                        </td>
                        <td style="text-align: center;"><span
                            class="ContentDetailBold">:</span>
                        </td>
                        <td class="DialogContentDetail"><span class="ContentDetailBold"
                            id="spanAccountDate"></span>
                        </td>

                    </tr>
                    <tr>

                        <td class="DialogContent"><span
                            class="ContentDetailBold"><%= Resource.getProperty("finance.period",lang) %></span>
                        </td>
                        <td style="text-align: center;"><span
                            class="ContentDetailBold">:</span>
                        </td>
                        <td class="DialogContentDetail"><span class="ContentDetailBold"
                            id="spanPeriod"></span>
                        </td>
                        <td class="DialogContent"><span
                            class="ContentDetailBold"><%= Resource.getProperty("finance.currency",lang) %></span>
                        </td>
                        <td style="text-align: center;"><span
                            class="ContentDetailBold">:</span>
                        </td>
                        <td class="DialogContentDetail"><span class="ContentDetailBold"
                            id="spanCurrency"></span>
                        </td>

                    </tr>
                    <tr>

                        <td class="DialogContent"><span
                            class="ContentDetailBold"><%= Resource.getProperty("finance.totaldebitamount",lang) %></span>
                        </td>
                        <td style="text-align: center;"><span
                            class="ContentDetailBold">:</span>
                        </td>
                        <td class="DialogContentDetail"><span class="ContentDetailBold"
                            id="spanDebitAmount"></span>
                        </td>
                        
                        <td class="DialogContent"><span
                            class="ContentDetailBold"><%= Resource.getProperty("finance.totalcreditamount",lang) %></span>
                        </td>
                        <td style="text-align: center;"><span
                            class="ContentDetailBold">:</span>
                        </td>
                        <td class="DialogContentDetail"><span class="ContentDetailBold"
                            id="spanCreditAmount"></span>
                        </td> 
                    </tr>
                    <tr>
                        <td class="DialogContent"><span
                            class="ContentDetailBold"><%= Resource.getProperty("finance.requesttype",lang) %></span>
                        </td>
                        <td style="text-align: center;"><span
                            class="ContentDetailBold">:</span>
                        </td>
                        <td class="DialogContentDetail"><span class="ContentDetailBold"
                            id="spanReqType"></span>
                        </td>
                    </tr>
                    <tr id="TRDescription">
                        <td class="DialogContent"><span
                            class="ContentDetailBold"><%= Resource.getProperty("finance.description",lang) %></span>
                        </td>
                        <td style="text-align: center;"><span
                            class="ContentDetailBold">:</span>
                        </td>
                        <td class="DialogContentDetail" colspan="4"><span
                            class="ContentDetailBold" id="spanDescription"></span>
                        </td>
                    </tr>
                </table>
            </div>
            <div>
                <TABLE id="JournalLineFG" class="FieldGroup" cellspacing="0"
                    cellpadding="0" border="0">
                    <TBODY>
                        <TR class="FieldGroup_TopMargin"></TR>
                        <TR>
                            <TD class="FieldGroupTitle_Left"><IMG
                                class="FieldGroupTitle_Left_bg" border="0"
                                src="../web/images/blank.gif"></IMG>
                            </TD>
                            <TD class="FieldGroupTitle"><%= Resource.getProperty("finance.g/ljournalline",lang) %></TD>
                            <TD class="FieldGroupTitle_Right"><IMG
                                class="FieldGroupTitle_Right_bg" border="0"
                                src="../web/images/blank.gif"></IMG>
                            </TD>
                            <TD class="FieldGroupContent"></TD>
                        </TR>
                        <TR class="FieldGroup_BottomMargin"></TR>
                    </TBODY>
                </TABLE>
            </div>
            <div>
                <table id="JournalLineList"
                    class="DataGrid_Header_Table_focus DataGrid_Body_Table_focus"
                    style="width: 100%;">
                    <thead>
                        <tr class="DataGrid_Body_Row">
                            <th class="DataGrid_Header_Cell" width="5%"
                                style="text-align: center;"><%= Resource.getProperty("finance.sno",lang) %></th>
                            <th class="DataGrid_Header_Cell" width="23%"
                                style="text-align: center;"><%= Resource.getProperty("finance.accountno",lang) %></th>
                            <th class="DataGrid_Header_Cell" width="17%"
                                style="text-align: center;"><%= Resource.getProperty("finance.description",lang) %></th>
                            <th class="DataGrid_Header_Cell" width="9%"
                                style="text-align: center;"><%= Resource.getProperty("finance.debit",lang) %></th>
                            <th class="DataGrid_Header_Cell" width="9%"
                                style="text-align: center;"><%= Resource.getProperty("finance.credit",lang) %></th>
                            <th class="DataGrid_Header_Cell" width="13%"
                                style="text-align: center; display:none;"><%= Resource.getProperty("finance.uom",lang) %></th>
                            <th class="DataGrid_Header_Cell" width="13%"
                                style="text-align: center; display:none;"><%= Resource.getProperty("finance.quantity",lang) %></th>
                        </tr>
                    </thead>
                    <tbody></tbody>
                </table>
            </div>
            <div align="center" style="width: 100%; margin: 0 auto;">
                <table
                    style="margin: 0 auto; width: 90%; text-align: center; border-spacing: 8px; border-collapse: separate;">
                    <tr id="TRRequesterRemarks" style="display: none;">
                        <td style="text-align: right; min-width: 110px;"><span
                            class="ContentDetailBold">
                                <%= Resource.getProperty("finance.requesterremarks",lang) %>&nbsp;&nbsp;&nbsp;</span>
                        </td>
                        <td><textarea id="inpRemarks"
                                class="dojoValidateValid TextArea_FiveCells_width TextArea_Medium_height"
                                readonly="readonly" disabled="disabled"></textarea>
                        </td>
                    </tr>
                    <tr>
                        <td style="text-align: right; min-width: 110px;"><span
                            class="ContentDetailBold"><%= Resource.getProperty("finance.comments",lang) %>&nbsp;&nbsp;&nbsp;</span>
                        </td>
                        <td><textarea id="inpComment"
                                class="dojoValidateValid TextArea_FiveCells_width TextArea_Medium_height"></textarea>
                        </td>
                    </tr>
                </table>
            </div>
            <div align="center" style="padding-top: 10px;">
                <BUTTON type="button" class="ButtonLink"
                    onclick="submitApproval('A');">
                    <TABLE class="Button">
                        <TR>
                            <TD class="Button_left"><IMG
                                class="Button_Icon Button_Icon_process"
                                src="../web/images/blank.gif" border="0"></IMG>
                            </TD>
                            <TD class="Button_text"><%=Resource.getProperty("finance.approve", lang)%></TD>
                            <TD class="Button_right"></TD>
                        </TR>
                    </TABLE>
                </BUTTON>
                &nbsp;&nbsp;&nbsp;&nbsp;
                <BUTTON type="button" class="ButtonLink"
                    onclick="submitApproval('RW');">
                    <TABLE class="Button">
                        <TR>
                            <TD class="Button_left"><IMG
                                class="Button_Icon Button_Icon_process"
                                src="../web/images/blank.gif" border="0"></IMG>
                            </TD>
                            <TD class="Button_text"><%=Resource.getProperty("finance.reworkreject", lang)%></TD>
                            <TD class="Button_right"></TD>
                        </TR>
                    </TABLE>
                </BUTTON>
                &nbsp;&nbsp;&nbsp;&nbsp;
                <BUTTON type="button" class="ButtonLink"
                    onclick="$('#GLJournalLines').dialog('close');">
                    <TABLE class="Button">
                        <TR>
                            <TD class="Button_left"><IMG
                                class="Button_Icon Button_Icon_process"
                                src="../web/images/blank.gif" border="0"></IMG>
                            </TD>
                            <TD class="Button_text"><%=Resource.getProperty("finance.cancel", lang)%></TD>
                            <TD class="Button_right"></TD>
                        </TR>
                    </TABLE>
                </BUTTON>
            </div>
          <div>
        <TABLE class="FieldGroup" cellspacing="0" cellpadding="0" border="0"><TBODY>
            <TR class="FieldGroup_TopMargin"></TR>
            <TR><TD class="FieldGroupTitle_Left"><IMG class="FieldGroupTitle_Left_bg" border="0" src="../web/images/blank.gif"></IMG></TD>
                <TD class="FieldGroupTitle"><%= Resource.getProperty("finance.approvalhistory",lang) %></TD>
            <TD class="FieldGroupTitle_Right"><IMG class="FieldGroupTitle_Right_bg" border="0" src="../web/images/blank.gif"></IMG></TD>
            <TD class="FieldGroupContent"></TD></TR>
            <TR class="FieldGroup_BottomMargin"></TR>
            <tr>
            </tr>
        </TBODY>
        </TABLE>
        <table>
        <tr>
         <TD><span id="ShowHistory" class="Content ContentBold" style="color: blue; text-decoration: underline; cursor: pointer;"><%= Resource.getProperty("finance.approvalhistory",lang) %></span></TD>
        </tr>
        </table>
    </div>
            <div id="DivApprovalList">
                <table id="ApprovalList"
                    class="DataGrid_Header_Table_focus DataGrid_Body_Table_focus"
                    style="width: 100%;">
                    <thead>
                        <tr class="DataGrid_Body_Row">
                            <th class="DataGrid_Header_Cell" width="5%"
                                style="text-align: center;"><%= Resource.getProperty("finance.sno",lang) %></th>
                            <th class="DataGrid_Header_Cell" width="20%"
                                style="text-align: center;"><%= Resource.getProperty("finance.datetime",lang) %></th>
                            <th class="DataGrid_Header_Cell" width="25%"
                                style="text-align: center;"><%= Resource.getProperty("finance.user",lang) %></th>
                            <th class="DataGrid_Header_Cell" width="20%"
                                style="text-align: center;"><%= Resource.getProperty("finance.status",lang) %></th>
                            <th class="DataGrid_Header_Cell" width="30%"
                                style="text-align: center;"><%= Resource.getProperty("finance.comments",lang) %></th>
                        </tr>
                    </thead>
                    <tbody></tbody>
                </table>
            </div>
        </div>
        <div id="MultiPaymentPopup" title="GLJournalLines"
            style="display: none; padding: 1em; overflow-x: hidden;">
            <div id="DIVMultiPaymentPopup" style="display: none;">
                <div id="DivMultiPaymentList"
                    style="max-height: 200px; overflow: auto; width: 100%;">
                    <table id="MultiPaymentList"
                        class="DataGrid_Header_Table_focus DataGrid_Body_Table_focus"
                        style="width: 100%;">
                        <tr class="DataGrid_Body_Row">
                            <th class="DataGrid_Header_Cell" width="5%"
                                style="text-align: center;"><%= Resource.getProperty("finance.gljournalno",lang) %></th>
                            <th class="DataGrid_Header_Cell" width="10%"
                                style="text-align: center;"><%= Resource.getProperty("finance.generalledger",lang) %></th>
                            <th class="DataGrid_Header_Cell" width="10%"
                                style="text-align: center;"><%= Resource.getProperty("finance.documentno",lang) %></th>
                            <th class="DataGrid_Header_Cell" width="16%"
                                style="text-align: center;"><%= Resource.getProperty("finance.documentdate",lang) %></th>
                            <th class="DataGrid_Header_Cell" width="17%"
                                style="text-align: center;"><%= Resource.getProperty("finance.accountingdate",lang) %></th>
                            <th class="DataGrid_Header_Cell" width="15%"
                                style="text-align: center;"><%= Resource.getProperty("finance.period",lang) %></th>
                            <th class="DataGrid_Header_Cell" width="7%"
                                style="text-align: center;"><%= Resource.getProperty("finance.currency",lang) %></th>
                            <th class="DataGrid_Header_Cell" width="10%"
                                style="text-align: center;"><%= Resource.getProperty("finance.totaldebitamount",lang) %></th>
                            <th class="DataGrid_Header_Cell" width="10%"
                                style="text-align: center;"><%= Resource.getProperty("finance.totalcreditamount",lang) %></th>
                        </tr>
                        <tbody></tbody>
                    </table>
                </div>
            <!--    <div>
                    <table id="MultiPaymentTotalList" style="width: 100%;">
                        <tr class="DataGrid_Body_Row">
                            <th width="85%" style="text-align: right;"><span
                                class="ContentDetailBold" style="font-size: 13px;">Total</span>
                            </th>
                            <th width="15%" style="text-align: right;"><span
                                class="ContentDetailBold" style="font-size: 13px;"
                                id="MultiPaymentTotalAmount">00</span>
                            </th>
                        </tr>
                        <tbody></tbody>
                    </table>
                </div> -->
                <div align="center" style="width: 100%; margin: 0 auto;">
                    <table
                        style="margin: 0 auto; width: 90%; text-align: center; border-spacing: 8px; border-collapse: separate;">
                        <tr>
                            <td style="text-align: right; min-width: 110px;"><span
                                class="ContentDetailBold"><%= Resource.getProperty("finance.comments",lang) %>&nbsp;&nbsp;&nbsp;</span>
                            </td>
                            <td><textarea id="inpMultiComment"
                                    class="dojoValidateValid TextArea_FiveCells_width TextArea_Medium_height"></textarea>
                            </td>
                        </tr>
                    </table>
                </div>
                <div align="center" style="padding-top: 10px;">
                    <BUTTON type="button" id="buttonApproveMultiPayment"
                        class="ButtonLink" onclick="approveMultiPayment('A');">
                        <TABLE class="Button">
                            <TR>
                                <TD class="Button_left"><IMG
                                    class="Button_Icon Button_Icon_process"
                                    src="../web/images/blank.gif" border="0"></IMG>
                                </TD>
                                <TD class="Button_text"><%=Resource.getProperty("finance.approve", lang)%></TD>
                                <TD class="Button_right"></TD>
                            </TR>
                        </TABLE>
                    </BUTTON>
                    &nbsp;&nbsp;&nbsp;&nbsp;
                    <BUTTON type="button" id="buttonReworkMultiPayment"
                        class="ButtonLink" onclick="approveMultiPayment('RW');">
                        <TABLE class="Button">
                            <TR>
                                <TD class="Button_left"><IMG
                                    class="Button_Icon Button_Icon_process"
                                    src="../web/images/blank.gif" border="0"></IMG>
                                </TD>
                                <TD class="Button_text"><%=Resource.getProperty("finance.reworkreject", lang)%></TD>
                                <TD class="Button_right"></TD>
                            </TR>
                        </TABLE>
                    </BUTTON>
                    &nbsp;&nbsp;&nbsp;&nbsp;
                    <BUTTON type="button" class="ButtonLink"
                        onclick="$('#MultiPaymentPopup').dialog('close');">
                        <TABLE class="Button">
                            <TR>
                                <TD class="Button_left"><IMG
                                    class="Button_Icon Button_Icon_process"
                                    src="../web/images/blank.gif" border="0"></IMG>
                                </TD>
                                <TD class="Button_text"><%=Resource.getProperty("finance.cancel", lang)%></TD>
                                <TD class="Button_right"></TD>
                            </TR>
                        </TABLE>
                    </BUTTON>
                </div>
            </div>
        </div>
        <TABLE height="100%" border="0" cellpadding="0" cellspacing="0"
            id="main">
            <TR>
                <TD valign="top" id="tdleftTabs"><table cellpadding="0"
                        cellspacing="0" class="Main_ContentPane_LeftTabsBar"
                        id="tdLeftTabsBars"></table>
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
                    </TABLE></TD>
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
                                    onMouseOut="window.status='';return true;"
                                    onclick="this.hideFocus=true" onblur="this.hideFocus=false"
                                    id="buttonRefresh"><img
                                        class="Main_ToolBar_Button_Icon Main_ToolBar_Button_Icon_Refresh"
                                        src="../web/images/blank.gif"
                                        title="<%=Resource.getProperty("finance.reload", lang)%>"
                                        border="0" id="linkButtonRefresh">
                                </a>
                                </td>
                                <td class="Main_ToolBar_Separator_cell"><img
                                    src="../web/images/blank.gif" class="Main_ToolBar_Separator">
                                </td>
                                <td   style="width: 100%;">
									<div id="ButtonProcess" class="MainButtonDiv" style="display:none;">
                                        <div style="position: relative; top: 10px;">
                                            <button onclick="openMultiPayment('A');" class="ButtonLink"
                                                type="button">
                                                <table class="Button">
                                                    <tbody>
                                                        <tr>
                                                            <td class="Button_left"><img border="0"
                                                                src="../web/images/blank.gif"
                                                                class="Button_Icon Button_Icon_process">
                                                            </td>
                                                            <td id="Submit_BTNname" class="Button_text"><%=Resource.getProperty("finance.approve", lang)%></td>
                                                            <td class="Button_right"><img border="0"
                                                                src="../web/images/blank.gif"
                                                                class="Button_Icon Button_Icon_process">
                                                            </td>
                                                        </tr>
                                                    </tbody>
                                                </table>
                                            </button>
                                            &nbsp;&nbsp;&nbsp;
                                            <button onclick="openMultiPayment('R');" class="ButtonLink"
                                                type="button">
                                                <table class="Button">
                                                    <tbody>
                                                        <tr>
                                                            <td class="Button_left"><img border="0"
                                                                src="../web/images/blank.gif"
                                                                class="Button_Icon Button_Icon_process">
                                                            </td>
                                                            <td id="Submit_BTNname" class="Button_text"><%=Resource.getProperty("finance.reworkreject", lang)%></td>
                                                            <td class="Button_right"><img border="0"
                                                                src="../web/images/blank.gif"
                                                                class="Button_Icon Button_Icon_process">
                                                            </td>
                                                        </tr>
                                                    </tbody>
                                                </table>
                                            </button>
                                        </div>
                                    </div></td>
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
                                style="background: none;" class="tabTitle"></span>
                            </td>
                        </tr>
                        <tr>
                            <td class="tabBackGround">
                                <div class="marginLeft">
                                    <div>
                                        <span class="dojoTabcurrentfirst"><div>
                                                <span><a class="dojoTabLink"
                                                    href="javascript:void(0);" onclick="return false;"
                                                    onMouseOver="return true;" onMouseOut="return true;"><%= Resource.getProperty("finance.gljournalapproval",lang) %></a>
                                                </span>
                                            </div>
                                        </span>
                                    </div>
                                </div></td>
                        </TR>
                    </TABLE>
                    <DIV class="Main_ContentPane_Client"
                        style="overflow: auto; width: 100%; height: 100%" id="client">
                        <TABLE class="dojoTabContainer" border="0" cellpadding="0"
                            cellspacing="0" width="100%" summary="">
                            <TR>
                                <TD><TABLE class="dojoTabPaneWrapper" border="0"
                                        cellpadding="0" cellspacing="0" width="100%" summary="">
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
                                                            <TD><TABLE cellpadding="0" cellspacing="0"
                                                                    class="MessageBox_Container">
                                                                    <TBODY>
                                                                        <TR>
                                                                            <TD class="MessageBox_LeftTrans"><TABLE
                                                                                    style="width: 100%; height: 100%" cellpadding="0"
                                                                                    cellspacing="0">
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
                                                                            <TD class="MessageBox_bg"><TABLE
                                                                                    class="MessageBox_Top">
                                                                                    <TBODY>
                                                                                        <TR>
                                                                                            <TD><SPAN><TABLE cellpadding="0"
                                                                                                        cellspacing="0"
                                                                                                        class="MessageBox_Body_ContentCell">
                                                                                                        <TBODY>
                                                                                                            <TR>
                                                                                                                <TD class="MessageBox_Icon_ContentCell"><DIV
                                                                                                                        class="MessageBox_Icon"></DIV>
                                                                                                                </TD>
                                                                                                                <TD style="vertical-align: top;"
                                                                                                                    id="messageBoxIDContent"><SPAN><DIV
                                                                                                                            class="MessageBox_TextTitle"
                                                                                                                            id="messageBoxIDTitle"></DIV>
                                                                                                                        <DIV class="MessageBox_TextDescription"
                                                                                                                            id="messageBoxIDMessage"></DIV>
                                                                                                                        <DIV class="MessageBox_TextSeparator"></DIV>
                                                                                                                </SPAN>
                                                                                                                </TD>
                                                                                                            </TR>
                                                                                                        </TBODY>
                                                                                                    </TABLE>
                                                                                            </SPAN>
                                                                                                <div id="hideMessage"
                                                                                                    style="float: right; margin-top: -13px; margin-right: 10px;">
                                                                                                    <a style="color: yellow; font-size: 11.5px"
                                                                                                        href="javascript:void(0);"
                                                                                                        onClick="document.getElementById('messageBoxID').style.display = 'none';reSizeGrid();"><%=Resource.getProperty("finance.hide", lang)%></a>
                                                                                                </div>
                                                                                            </TD>
                                                                                        </TR>
                                                                                    </TBODY>
                                                                                </TABLE>
                                                                            </TD>
                                                                            <TD class="MessageBox_RightTrans"><TABLE
                                                                                    style="width: 100%; height: 100%" cellpadding="0"
                                                                                    cellspacing="0">
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
                                                        style="color: #000000; font-family: sans-serif; font-size: 14px; margin-top: 15px;"><%=Resource.getProperty("finance.loading", lang)%>...
                                                    </div>
                                                </div>

                                                <div align="center" id="PaymentOutGrid"
                                                    style="display: none;">
                                                    <div>
                                                        <table id="GlJournal" class="scroll" cellpadding="0"
                                                            cellspacing="0" width="100%"></table>
                                                        <div id="GlJournalPager" class="scroll"
                                                            style="text-align: center;"></div>
                                                    </div>
                                                </div></TD>
                                        </TR>
                                    </TABLE></TD>
                            </TR>
                        </TABLE>
                    </DIV>
                    <TABLE width="100%" border="0" cellspacing="0" cellpadding="0"
                        class="Menu_ContentPane_Bottom" id="tdbottomSeparator">
                        <TR>
                            <TD class="Main_Bottom_bg"><IMG
                                src="../web/images/blank.gif" border="0"></IMG>
                            </TD>
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
                    </TABLE></TD>
            </TR>
        </TABLE>
    </FORM>
    <script type="text/javascript">onLoadDo();</script>
    <script type="text/javascript" src="../web/js/jquery-1.7.2.min.js"></script>
    <script type="text/javascript"
        src="../web/js/jquery/jquery-ui-1.8.6.custom.min.js"></script>
    <script type="text/javascript" src="../web/js/jquery/grid.locale-en.js"></script>
    <script type="text/javascript"
        src="../web/js/jquery/jquery.jqGrid.min.js"></script>
    <script type="text/javascript" src="../web/js/ui/jquery.ui.core.js"></script>
    <script type="text/javascript" src="../web/js/ui/jquery.ui.widget.js"></script>
    <script type="text/javascript"
        src="../web/js/ui/jquery.ui.datepicker.js"></script>
    <script type="text/javascript" src="../web/js/common/json2.js"></script>
    <script type="text/javascript" src="../web/js/common/OBToolTip.js"></script>
    <script type="text/javascript">
var BigDecimal = parent.parent.parent.parent.BigDecimal;
var onScroll = 0;
var keyFlag = 0, isDialogOpen = 0;
var msgEqualTo = '<%=Resource.getProperty("finance.equalto", lang)%>';
var msgGTEqualTo = '<%=Resource.getProperty("finance.gtequalto", lang)%>';
var msgLTEqualTo = '<%=Resource.getProperty("finance.ltequalto", lang)%>';
function reSizeGrid() {
    var gridW, gridH = 154;
    if (window.innerWidth) {
        gridW = window.innerWidth - 52;
        gridH = window.innerHeight - 187;
    }
    else if (document.body) {
        gridW = document.body.clientWidth - 52;
        gridH = document.body.clientHeight - 187;
    }
    if (parseInt(document.getElementById("client").scrollHeight) + 77 > parseInt(document.body.clientHeight)) {
        gridW = gridW - 15;
        gridH = gridH - 30;
    }
    if (gridW <= 800)
        gridW = 800;
    if (gridH <= 300)
        gridH = 300;
    paymentOutGrid.setGridWidth(gridW, true);
    paymentOutGrid.setGridHeight(gridH, true);
    /* if ($('tr.footrow'))
        $('tr.footrow').children()[10].style.width = (parseInt($('tr.footrow').children()[9].style.width.replace('px', '')) + parseInt($('tr.footrow').children()[10].style.width.replace('px', '')) + 2) + 'px'; */
    $("#GLJournalLines").dialog("option", "height", gridH + 120);
    $("#GLJournalLines").dialog("option", "position", "center");
}
var paymentOutGrid = jQuery("#GlJournal");
jQuery(function() {
    var lastSelectedId = "";
    var lastSelectedId2 = "";
    var direction="ltr";
    var inpLanguage=document.getElementById("inpLanguageId").value;
    if(inpLanguage=="ar_SA"){
    	direction="rtl";
    }
    paymentOutGrid.jqGrid({
        url : '<%=request.getContextPath()%>/GLJournalApprovalAjax?act=GetGlJournalList',
        direction:direction,
        mtype : 'POST',
        colNames : [ '', 'OrgId', 'Organization','General Ledger', 'Document No','Description','Document Date', 'Accounting Date', 'Period', 'Opening', 'Currency','Total Debit Amount','Total Credit Amount','Requested By', 'Requested Time',
                'EutNextRoleId' ,'GLJournalBatchId'],
        colModel : [ {
            name : 'multiselect',
            index : 'multiselect',
            sortable : false,
            search : false,
            width : 30,
            formatter : formatMultiSelect
        }, {
            name : 'ad_org_id',
            index : 'ad_org_id',
            hidden : true,
            
        }, {
            name : 'orgname',
            index : 'orgname',
            sortable : true,
            width : 90,
        }, {
            name : 'ledger',
            index : 'ledger',
            sortable : true,
            width : 90,
        }, 
        {
            name : 'documentno',
            index : 'documentno',
            sortable : true,
            width : 70,
            formatter : formatDocumenNo
        },
        {
            name : 'description',
            index : 'description',
            sortable : true,
            width : 70
        },
        
        {
            name : 'documentdate',
            index : 'documentdate',
            width : 90,
            sortable : true,
            searchoptions : {
                dataInit : function(e) {
                    $(e).datepicker({
                        dateFormat : 'dd-mm-yy',
                        changeMonth : true,
                        changeYear : true,
                        onClose : function(dateText, inst) {
                            if (dateText != "")
                                paymentOutGrid[0].triggerToolbar();
                        }
                    });
                    e.style.width = "55%";
                    setTimeout(function() {
                        var html = '<select onchange="searchPaymentOut(\'DD\');" style="width: 40%; padding: 0px; margin: 2px 0px 0px; height: 17px;" class="Combo" id="gs_documentdate_s">';
                        html += '<option value=\"=\">Equal to</option><option value=\">=\">Greater than or Equal to</option>';
                        html += '<option value=\"<=\">Lesser than or Equal to</option></select>';
                        $('#gs_documentdate').before(html);
                    }, 10);
                }
            }
        }, 
        {
            name : 'accountdate',
            index : 'accountdate',
            width : 90,
            sortable : true,
            searchoptions : {
                dataInit : function(e) {
                    $(e).datepicker({
                        dateFormat : 'dd-mm-yy',
                        changeMonth : true,
                        changeYear : true,
                        onClose : function(dateText, inst) {
                            if (dateText != "")
                                paymentOutGrid[0].triggerToolbar();
                        }
                    });
                    e.style.width = "55%";
                    setTimeout(function() {
                        var html = '<select onchange="searchPaymentOut(\'AD\');" style="width: 40%; padding: 0px; margin: 2px 0px 0px; height: 17px;" class="Combo" id="gs_accountdate_s">';
                        html += '<option value=\"=\">Equal to</option><option value=\">=\">Greater than or Equal to</option>';
                        html += '<option value=\"<=\">Lesser than or Equal to</option></select>';
                        $('#gs_accountdate').before(html);
                    }, 10);
                }
            }
        },
        {
            name : 'period',
            index : 'period',
            sortable : true,
            width : 50
        },  {
            name : 'Opening',
            index : 'Opening',
            width : 50
        },
        {
            name : 'currency',
            index : 'currency',
            width : 50
        },
        {
            name : 'debitamount',
            index : 'debitamount',
            width : 50,
            align : 'right'
        },
        {
            name : 'creditamount',
            index : 'creditamount',
            width : 50,
            align : 'right'
        },{
            name : 'requester',
            index : 'requester',
            width : 150,
            sortable : true
        }, {
            name : 'requestdate',
            index : 'requestdate',
            width : 110,
            sortable : true,
            searchoptions : {
                dataInit : function(e) {
                    $(e).datepicker({
                        dateFormat : 'dd-mm-yy',
                        changeMonth : true,
                        changeYear : true,
                        onClose : function(dateText, inst) {
                            if (dateText != "")
                                paymentOutGrid[0].triggerToolbar();
                        }
                    });
                    e.style.width = "55%";
                    setTimeout(function() {
                        var html = '<select onchange="searchPaymentOut(\'RD\');" style="width: 40%; padding: 0px; margin: 2px 0px 0px; height: 17px;" class="Combo" id="gs_requestdate_s">';
                        html += '<option value=\"=\">Equal to</option><option value=\">=\">Greater than or Equal to</option>';
                        html += '<option value=\"<=\">Lesser than or Equal to</option></select>';
                        $('#gs_requestdate').before(html);
                    }, 10);
                }
            }
        }, {
            name : 'eutNextRoleId',
            index : 'eutNextRoleId',
            hidden : true
        },
        {
            name:'JournalBatch',
            index:'JournalBatch',
            hidden:true
        }],
        pager : '#GlJournalPager',
        sortorder : "desc",
        sortname : 'documentno',
        datatype : 'json',
        viewrecords : true,
        autowidth : true,
        rownumbers : true,
        shrinkToFit : false,
        rowNum : '10000',
        //footerrow : true,
        userDataOnFooter : true,
        rowNum : 100,
        rowList : [ 100, 200, 300, 500 ],
        jsonReader : {
            repeatitems: false 
        },
        beforeRequest : function() {
            if ("" + paymentOutGrid.getPostDataItem("_search") == "true") {
                if ("" + paymentOutGrid.getPostDataItem("documentdate") != "") {
                    var date = OBValidateDate("" + paymentOutGrid.getPostDataItem("documentdate"));
                    if (date == false)
                        paymentOutGrid.setPostDataItem("documentdate", "");
                }
                if ("" + paymentOutGrid.getPostDataItem("accountdate") != "") {
                    var date = OBValidateDate("" + paymentOutGrid.getPostDataItem("accountdate"));
                    if (date == false)
                        paymentOutGrid.setPostDataItem("accountdate", "");
                }
                if ("" + paymentOutGrid.getPostDataItem("requestdate") != "") {
                    var date = OBValidateDate("" + paymentOutGrid.getPostDataItem("requestdate"));
                    if (date == false)
                        paymentOutGrid.setPostDataItem("requestdate", "");
                }
                paymentOutGrid.setPostDataItem("documentdate_s", document.getElementById("gs_documentdate_s").value);
                paymentOutGrid.setPostDataItem("accountdate_s", document.getElementById("gs_accountdate_s").value);
                paymentOutGrid.setPostDataItem("requestdate_s", document.getElementById("gs_requestdate_s").value);
            }
        },
        beforeSelectRow : function(id, e) {
            if ((e.target && ('' + e.target.id).indexOf('DIV_MS_') >= 0) || (e.target.firstChild && (e.target.firstChild.id && ('' + e.target.firstChild.id).indexOf('MS_') >= 0))) {
                return false;
            }
            return true;
        },
        onSelectRow : function(id) {
            ChangeJQGridSelectRowColor(document.getElementById("inpJournalId").value, id);
            if (id) {
                document.getElementById("inpJournalId").value = id;
                var rowData = paymentOutGrid.getRowData(id);
                document.getElementById("inpOrgId").value = rowData['ad_org_id'];
                getPaymentOutLineDetails(id);
            }
            
            
        },
        loadComplete : function() {
            lastSelectedId = "";
            ChangeJQGridAllRowColor(paymentOutGrid);

      /*        var amount = 0.00, currency = "";
            var gridIds = paymentOutGrid.getDataIDs();
            var gridIdsLength = gridIds.length;
            var totalAmt = {};
            for ( var i = 0; i < gridIdsLength; i++) {
                var rowData = paymentOutGrid.getRowData(gridIds[i]);
                amount = rowData['amt'].replace(",", "").trim();
                currency = rowData['currency'].trim();
                if(totalAmt[currency])
                    totalAmt[currency] = parseFloat(totalAmt[currency]) + parseFloat(amount);
                else
                    totalAmt[currency] = parseFloat(amount);
            }
            
            var htmlTotalAmt = '';
            for (var key in totalAmt) {
                $('#ValidatePrice1').val(totalAmt[key]);
                numberInputEvent('onblur', document.getElementById("ValidatePrice1"));
                htmlTotalAmt += ('<div>' + $('#ValidatePrice1').val() + ' ' + key + '</div>'); 
            }

            jQuery('#GlJournal').jqGrid('footerData', 'set', {
                payto : 'TOTAL',
                amount : htmlTotalAmt
            });

            $('tr.footrow').children()[7].style.textAlign = "right";
            $('tr.footrow').children()[9].style.display = "none";
            $('tr.footrow').children()[10].style.textAlign = "right";
            $('tr.footrow').children()[10].style.width = (parseInt($('tr.footrow').children()[9].style.width.replace('px', '')) + parseInt($('tr.footrow').children()[10].style.width.replace('px', '')) + 2) + 'px'; 
             */
            
            showProcessBar(false);

            reSizeGrid();
            setApprovalNotification();
            
            $("#LoadingContent").hide();
            $("#PaymentOutGrid").show();
            $("#gs_debitamount").attr('onkeydown', 'return isFloatOnKeyDown(this, event);');
            $("#gs_creditamount").attr('onkeydown', 'return isFloatOnKeyDown(this, event);'); 
            
            setTimeout(function(){
                reSizeGrid();
            }, 100);
        }
    });
    paymentOutGrid.jqGrid('navGrid', '#GlJournalPager', {
        edit : false,
        add : false,
        del : false,
        search : false,
        view : false,
        beforeRefresh : function() {
            document.getElementById("inpJournalId").value = "";
            $('#gs_requestdate_s, #gs_documentdate_s,gs_accountdate_s').val('=');
            paymentOutGrid[0].clearToolbar();
            reSizeGrid();
        }
    }, {}, {}, {}, {});
    paymentOutGrid.jqGrid('filterToolbar', {
        searchOnEnter : false
    });
    paymentOutGrid[0].triggerToolbar();
    changeJQGridDisplay("GlJournal", 'inpJournalId');
    reSizeGrid();
});

function checkboxOnchg(){
	var atLeastOneIsChecked = $('input[name="PaymentOutMultiSelect"]:checked').length > 0;
    if(atLeastOneIsChecked){
    	   document.getElementById("ButtonProcess").style.display="";
    }
    else{
    	   document.getElementById("ButtonProcess").style.display="none";
    }
}

$("#PaymentOutGrid").find('input[type=checkbox]').each(function() {
    $(this).change( function(){
        var colid = $(this).parents('tr:last').attr('id');
        if( $(this).is(':checked')) {
           $("#PaymentOutGrid").jqGrid('setSelection', colid );
           $(this).prop('checked',true);
        }
        return true;
    });
});
function formatMultiSelect(el, cellval, opts) {
    var html = '<div id="DIV_MS_' + cellval.rowId
            + '" align="center" style="width: 100%; height: 100%; position: relative; padding-top: 4px;"><input type="checkbox" name="PaymentOutMultiSelect" id="MS_' + cellval.rowId
            + '" onchange="checkboxOnchg();" ;></input></div>';
    return html;
}
function formatDocumenNo(cellvalue, options, rowObject) {
    return '<div style="color: #0066CC; cursor: pointer; text-decoration: underline;" onclick="openGlJournal(\'' + options.rowId + '\',\'' +rowObject.JournalBatch + '\')">' + cellvalue + '</div><input type="hidden" id="DOC_'
            + options.rowId + '" value="' + cellvalue + '">';
}
function openGlJournal(journalId,batchId) {
    var myframe = getFrame('LayoutMDI') || top.opener;
    if (myframe) {
    	if(batchId !=null && batchId !="undefined")
        myframe.OB.Utilities.openDirectTab('160', journalId);
    	else{
            myframe.OB.Utilities.openDirectTab('5A6F0ED7230C462BA4010653BA3F816A', journalId);
    }
    }
}
function searchPaymentOut(type) {
    if (type == "DD" && document.getElementById("gs_documentdate").value != "")
        paymentOutGrid[0].triggerToolbar();
    else if (type == "AD" && document.getElementById("gs_accountdate").value != "")
        paymentOutGrid[0].triggerToolbar();
    else if (type == "RD" && document.getElementById("gs_requestdate").value != "")
        paymentOutGrid[0].triggerToolbar();
}


function getPaymentOutLineDetails(id) {

    showProcessBar(true);
    $.post('<%=request.getContextPath()%>/GLJournalApprovalAjax', {
        act : 'GetJournalLines',
        inpJournalId : id
    }, function(result) {
        $('#messageBoxID, #JournalLineFG, #JournalLineList,#DivApprovalList').hide();
        $('#TRRequesterRemarks').hide();
        $('#inpComment').val('');


        var rowData = paymentOutGrid.getRowData(id);

        document.getElementById("inpEutNextRoleId").value = rowData.quNextRoleId;
        //document.getElementById("inpGrandTotal").value = result.amount;
        // Header Details
        $('#spanJournalNo').html(rowData.documentno);
        $('#spanLedger').html(rowData.ledger);
        $('#spanDocumentDate').html(rowData.documentdate);
        $('#spanAccountDate').html(rowData.accountdate);
        $('#spanPeriod').html(rowData.period);
        $('#spanCurrency').html(rowData.currency);
        $('#spanReqType').html(rowData.JournalBatch == "" ? 'Simple G/L Journal' : 'G/L Journal');
      //  $('#spanReqType').html(result.reqType == 'N' ? 'New Request' : 'Rework Request');
      // $('#spanDebitAmount').html(result.debitAmount + ' ' + result.currency);
      //  $('#spanCreditAmount').html(result.creditAmount + ' ' + result.currency);
         $('#spanDebitAmount').html(result.debitAmount);
        $('#spanCreditAmount').html(result.creditAmount);
        $('#spanDescription').html(createTooltipEle(result.Description, 50));

        if (result.Description.trim() != '')
            $('#TRDescription').show();
        else
            $('#TRDescription').hide();
        
        if (result.reqType == 'R')
            $('#spanReqType').css('color', 'blue');
        else
            $('#spanReqType').css('color', 'black');
        

        if (parseFloat(result.debitAmount) == 0)
            $('#spanDebitAmount').css('color', 'black');
        else
            $('#spanDebitAmount').css('color', 'blue');

        if (parseFloat(result.creditAmount) == 0)
            $('#spanCreditAmount').css('color', 'black');
        else
            $('#spanCreditAmount').css('color', 'blue');

        
        // Line Details
        deleteTableRow('JournalLineList');

        var table = document.getElementById("JournalLineList"), rowCount = table.rows.length;
        var data = result.journalList;
        if (data.length > 0)
            $('#JournalLineFG, #JournalLineList').show();
        else
            $('#JournalLineFG, #JournalLineList').hide();
        for ( var i in data) {
            var row = table.insertRow(parseInt(i) + 1), cell = null;
            row.className = "DataGrid_Body_Row DataGrid_Body_Row_" + ((parseInt(i) + 1) % 2);
            cell = row.insertCell(0);
            cell.className = "DataGrid_Body_Cell";
            cell.style.textAlign = "center";
            cell.innerHTML = "<span>" + (parseInt(i) + 1) + "</span>";
            cell = row.insertCell(1);
            cell.className = "DataGrid_Body_Cell";
            cell.style.textAlign = "left";
            cell.innerHTML = "<span class='text'>" + data[i].account + "</span>";
            cell = row.insertCell(2);
            cell.className = "DataGrid_Body_Cell";
            cell.style.textAlign = "left";
            cell.innerHTML = "<span class='text'>" + data[i].Description + "</span>";
            cell = row.insertCell(3);
            cell.className = "DataGrid_Body_Cell";
            cell.style.textAlign = "center";
            cell.innerHTML = "<span class='text'>" + data[i].linedebit + "</span>";
            cell = row.insertCell(4);
            cell.className = "DataGrid_Body_Cell";
            cell.style.textAlign = "center";
            cell.innerHTML = "<span class='text'>" + data[i].linecredit + "</span>";
            cell = row.insertCell(5);
            cell.className = "DataGrid_Body_Cell";
            cell.style.textAlign = "center";
            cell.style.display="none";
            cell.innerHTML = "<span class='text'>" + data[i].uom + "</span>";
            cell = row.insertCell(6);
            cell.className = "DataGrid_Body_Cell";
            cell.style.textAlign = "center";
            cell.style.display="none";
            cell.innerHTML = "<span class='text'>" + data[i].qty + "&nbsp;&nbsp</span>";
        }

        // Approval Details
        $('#ShowHistory').text('Approval History');
        $('#DivApprovalList').hide();
        deleteTableRow('ApprovalList');
        var table = document.getElementById("ApprovalList"), rowCount = table.rows.length;
        var data = result.approvalList;
        for ( var i in data) {
            var row = table.insertRow(parseInt(i) + 1), cell = null;
            row.className = "DataGrid_Body_Row DataGrid_Body_Row_" + ((parseInt(i) + 1) % 2);
            cell = row.insertCell(0);
            cell.className = "DataGrid_Body_Cell";
            cell.style.textAlign = "center";
            cell.innerHTML = "<span>" + (parseInt(i) + 1) + "</span>";
            cell = row.insertCell(1);
            cell.className = "DataGrid_Body_Cell";
            cell.style.textAlign = "center";
            cell.innerHTML = "<span>" + data[i].date + "</span>";
            cell = row.insertCell(2);
            cell.className = "DataGrid_Body_Cell";
            cell.style.textAlign = "left";
            cell.innerHTML = "<span class='text'>" + data[i].userName + "</span>";
            cell = row.insertCell(3);
            cell.className = "DataGrid_Body_Cell";
            cell.style.textAlign = "left";
            cell.innerHTML = "<span class='text'>" + data[i].status + "</span>";
            cell = row.insertCell(4);
            cell.className = "DataGrid_Body_Cell";
            cell.style.textAlign = "left";
            cell.innerHTML = "<span class='text'>" + data[i].comments + "</span>";
        }
        showProcessBar(false);
        openGLJournalLinesPopup();
    });
}
$("#ShowHistory").click(function() {
    if ($("#DivApprovalList").is(":hidden")) {
        $("#DivApprovalList").slideDown("fast", function() {
            var offset = $("#ShowHistory").offset();
            $("#GLJournalLines").scrollTop(offset.top);
        });
        $('#ShowHistory').text('Hide History');
    }
    else {
        $("#DivApprovalList").slideUp("fast");
        $('#ShowHistory').text('Show History');
    }
});
function deleteTableRow(tableId) {
    var table = document.getElementById(tableId);
    var rowCount = table.rows.length;
    for ( var i = rowCount - 1; i >= 1; i--)
        table.deleteRow(i);
}
function openGLJournalLinesPopup() {
    var gridW, gridH;
    if (window.innerWidth) {
        gridH = window.innerHeight - 167;
    }
    else if (document.body) {
        gridH = document.body.clientHeight - 167;
    }
    if (gridH <= 300)
        gridH = 300;
    $("#GLJournalLines").dialog({
        modal : true,
        width : 1000,
        height : gridH + 120,
        position : 'center',
        title : 'G/L Journal Lines',
        show : 'blind',
        resizable : false,
        open : function(event, ui) {
            setTimeout(function() {
                document.getElementById("inpComment").focus();
            }, 500);
        },
        close : function() {
            paymentOutGrid.jqGrid('resetSelection');
            var gGridIdList = paymentOutGrid.getDataIDs(), gGridIdListCount = gGridIdList.length;
            for ( var i = 0; i < gGridIdListCount; i++) {
                ChangeJQGridSelectMultiRowColor(gGridIdList[i], 'US');
            }
            ChangeJQGridAllRowColor(paymentOutGrid);
        }
    });
}
var glJournalList = [];
function submitApproval(type) {
    document.getElementById("inpComments").value = document.getElementById("inpComment").value;
    glJournalList = [];
    var rowJSON = {}, id = document.getElementById("inpJournalId").value;
    var rowData = paymentOutGrid.getRowData(id);
    rowJSON.id = id;
    rowJSON.documentNo = $('#DOC_' + id).val();
    rowJSON.orgId = rowData.ad_org_id;
    rowJSON.eutNextRoleId = rowData.eutNextRoleId;
    /* var amount=rowData.creditamount;
    var index = amount.indexOf(rowData.currency);
    rowJSON.amount=(amount.substring(0, index));
    rowJSON.amount =parseFloat(rowJSON.amount.replace(/,/,"")); */
    rowJSON.amount=  rowData.creditamount;
    var len = glJournalList.length;
    glJournalList[len] = rowJSON;
    processPaymentOut(type);
}
function approveMultiPayment(type) {
    document.getElementById("inpComments").value = document.getElementById("inpMultiComment").value;
    processPaymentOut(type);
}
function openMultiPayment(type) {
    
    $('#buttonApproveMultiPayment, #buttonReworkMultiPayment').hide();
    if (type == 'A')
        $('#buttonApproveMultiPayment').show();
    else
        $('#buttonReworkMultiPayment').show();
    var chkPayment = $('input[name=PaymentOutMultiSelect]:checked');
    if (chkPayment.length == 0) {
        OBAlert('Please select atleast one Journal Entry');
    }
    else {
        showProcessBar(true);
        var idList = '';
        chkPayment.each(function() {
            idList += ",'" + $(this).attr('id').substring(3) + "'";
        });
        $.post('<%=request.getContextPath()%>/GLJournalApprovalAjax', {
            act : 'GetMultiPaymentOutDetails',
            inpFinPaymentIdList : idList.substring(1)
        }, function(result) {
            document.getElementById("inpMultiComment").value = '';
            glJournalList = [];
            deleteTableRow('MultiPaymentList');
            var totalAmt = {};
            var table = document.getElementById("MultiPaymentList"), rowCount = table.rows.length, i = 0;
            chkPayment.each(function() {
                var id = $(this).attr('id').substring(3);
                var rowData = paymentOutGrid.getRowData(id);
                var row = table.insertRow(parseInt(i) + 1), cell = null;
                row.className = "DataGrid_Body_Row DataGrid_Body_Row_" + ((parseInt(i) + 1) % 2);
                cell = row.insertCell(0);
                cell.className = "DataGrid_Body_Cell";
                cell.style.textAlign = "center";
                cell.innerHTML = "<span>" + (parseInt(i) + 1) + "</span>";
                cell = row.insertCell(1);
                cell.className = "DataGrid_Body_Cell";
                cell.style.textAlign = "center";
                cell.innerHTML = "<span class='text'>" + rowData.ledger + "</span>";
                cell = row.insertCell(2);
                cell.className = "DataGrid_Body_Cell";
                cell.style.textAlign = "center";
                cell.innerHTML = "<span class='text'>" + rowData.documentno + "</span>";
                cell = row.insertCell(3);
                cell.className = "DataGrid_Body_Cell";
                cell.style.textAlign = "left";
                cell.innerHTML = "<span class='text'>" + rowData.documentdate+ "</span>";
                cell = row.insertCell(4);
                cell.className = "DataGrid_Body_Cell";
                cell.style.textAlign = "left";
                cell.innerHTML = "<span class='text'>" + rowData.accountdate + "</span>";
                cell = row.insertCell(5);
                cell.className = "DataGrid_Body_Cell";
                cell.style.textAlign = "left";
                cell.innerHTML = "<span class='text'>" + rowData.period + "</span>";
                cell = row.insertCell(6);
                cell.className = "DataGrid_Body_Cell";
                cell.style.textAlign = "center";
                cell.innerHTML = "<span class='text'>" + rowData.currency + "</span>";
                cell = row.insertCell(7);
                cell.className = "DataGrid_Body_Cell";
                cell.style.textAlign = "right";
                cell.innerHTML = "<span class='text'>" + rowData.debitamount + "&nbsp;</span>";
                cell = row.insertCell(8);
                cell.className = "DataGrid_Body_Cell";
                cell.style.textAlign = "right";
                cell.innerHTML = "<span class='text'>" + rowData.creditamount + "&nbsp;</span>";
                i++;

                var rowJSON = {};
                rowJSON.id = id;
                rowJSON.orgId = rowData.ad_org_id;
                rowJSON.documentNo = $('#DOC_' + id).val();
                rowJSON.quNextRoleId = rowData.eutNextRoleId;
               // var amount =rowData.creditamount;
              //  var index = amount.indexOf(rowData.currency);
               // rowJSON.amount=(amount.substring(0, index));
               // rowJSON.amount= parseFloat(rowJSON.amount.replace(/,/,""))
                 rowJSON.amount=  rowData.creditamount;
                var len = glJournalList.length;
                glJournalList[len] = rowJSON;
            });
              
            showProcessBar(false);
            openMultiPaymentOutPopup();
        });
    }
}
function openMultiPaymentOutPopup() {
    var gridW, gridH;
    if (window.innerWidth) {
        gridH = window.innerHeight - 167;
    }
    else if (document.body) {
        gridH = document.body.clientHeight - 167;
    }
    if (gridH <= 300)
        gridH = 300;
    $("#MultiPaymentPopup").dialog({
        modal : true,
        width : 1000,
        height : gridH + 120,
        position : 'center',
        title : 'G/L Journal',
        show : 'blind',
        resizable : false,
        open : function(event, ui) {
            $('#DivMultiPaymentList').css('max-height', (parseInt($('#MultiPaymentPopup').height()) - 160) + 'px'); 
            $('#DIVMultiPaymentPopup').show();
            setTimeout(function() {
                document.getElementById("inpMultiComment").focus();
            }, 700);
            document.getElementById("inpMultiComment").focus();
        },
        close : function() {
            $('#DIVMultiPaymentPopup').hide();
        }
    });
}
function processPaymentOut(type) {
    var msg = 'Are you sure to approve the payment?';
    if (type == "RW")
        msg = 'Are you sure to assign rework for the payment?';
    $('#GLJournalLines').dialog('close');
    $('#MultiPaymentPopup').dialog('close');
    showProcessBar(true, 2);
    $.post('<%=request.getContextPath()%>/GLJournalApprovalAjax', {
        act : "MultiSubmit",
        type : type,
        reqType : "P",
        inpComments : document.getElementById("inpComments").value,
        journalList : JSON.stringify(glJournalList)
    }, function(result) {
        if (result.result == "1")
            displayMessage('S', result.resultMsg, result.resultDesc);
        else if (result.result == "0")
            displayMessage('E', result.resultMsg, result.resultDesc);
        else if (result.result == "2") {
            var desc = '';
            for ( var i in result.journalList) {
                desc += '<B>' + result.journalList[i].documentNo + '</B> : ' + result.journalList[i].resultCoreMsg + '<br/>';
            }
            displayMessage('S', result.resultMsg, desc);
        }
        paymentOutGrid.trigger("reloadGrid");
    });
}
var approvalNotifier = null, OB = parent.parent.parent.parent.OB;
function setApprovalNotification() {
    // OB.QPOE_Notify.countOLD = parseInt(paymentOutGrid.getGridParam("records"));
}
function checkApprovals() {
    var chkPayment = $('input[name=PaymentOutMultiSelect]:checked');
    if (chkPayment.length == 0 && document.getElementById("inpJournalId").value.trim() == '') {
        if (OB.QPOE_Notify.reloadPOApprovalGrid == true) {
            paymentOutGrid.trigger("reloadGrid");
            OB.QPOE_Notify.reloadPOApprovalGrid = false;
        }
    }
}
function startApprovalNotify() {
    approvalNotifier = setInterval(function() {
        checkApprovals();
    }, 5000);
}
//startApprovalNotify();
</script>
</BODY>
</HTML>


