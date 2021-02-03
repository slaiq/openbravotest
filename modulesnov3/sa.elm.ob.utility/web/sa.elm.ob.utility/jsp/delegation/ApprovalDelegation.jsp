<%@page import="sa.elm.ob.utility.util.Utility"%>
<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="org.codehaus.jettison.json.JSONObject, sa.elm.ob.utility.properties.Resource"%>
<%@ page import="java.util.List,java.util.ArrayList" errorPage="/web/jsp/ErrorPage.jsp" %>
<%
String lang = ((String)session.getAttribute("#AD_LANGUAGE"));
String style="../web/skins/ltr/org.openbravo.userinterface.skin.250to300Comp/250to300Comp/Openbravo_ERP.css";
String toolBarStyle="../web/js/common/CommonFormLtr.css";
if(lang.equals("ar_SA")){
     style="../web/skins/rtl/org.openbravo.userinterface.skin.250to300Comp/250to300Comp/Openbravo_ERP_250.css";
     toolBarStyle="../web/js/common/CommonFormRtl.css";
}
    List<JSONObject> usrLs= (request.getAttribute("UserLs")!=null?(ArrayList<JSONObject>)request.getAttribute("UserLs"):null);
    List<JSONObject> toUsrLs= (request.getAttribute("ToUserLs")!=null?(ArrayList<JSONObject>)request.getAttribute("ToUserLs"):null);
    List<JSONObject> roleLs= (request.getAttribute("RoleLs")!=null?(ArrayList<JSONObject>)request.getAttribute("RoleLs"):null);
    String isAdminUser =  (request.getAttribute("isAdminUser")==null?"": request.getAttribute("isAdminUser").toString()) ;
    String userId =  (request.getAttribute("selectUsr")==null?"": request.getAttribute("selectUsr").toString()) ;
    
%>
<HTML xmlns:="http://www.w3.org/1999/xhtml">
  <HEAD>
    <META http-equiv="Content-Type" content="text/html; charset=utf-8">
    <TITLE><%= Resource.getProperty("utility.approvaldelegation.requestapprovaldelegation", lang)%></TITLE>
    <LINK rel="shortcut icon" href="../web/images/favicon.ico" type="image/x-icon"></LINK>
    <LINK rel="stylesheet" type="text/css" href="<%=style %>" id="paramCSS"></LINK>
    <link rel="stylesheet" type="text/css" href="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.picker.css"></link>
    <link rel="stylesheet" type="text/css" href="../web/js/jquery/jquery-ui-1.8.11.custom.css"></link>
    <link rel="stylesheet" type="text/css" href="../web/js/jquery/ui.jqgrid.css"></link>
    <link rel="stylesheet" type="text/css" href="<%=toolBarStyle %>" />
    <style type="text/css">
        ul{border:1px solid #FF9C30 !important;}
        .autocomp_hght{height: 18px !important;}
        .autocomp_wdth{width:375px !important;}
    </style>    
    <link rel="stylesheet"  href="../web/js/jquery/css/demos.css">
    <link rel="stylesheet" type="text/css" href="../web/js/themes/base/jquery.ui.autocomplete.css"></link>
    <script type="text/javascript" src="../web/js/common/common.js"></script>
    <script type="text/javascript" src="../web/js/utils.js"></script>
    <script type="text/javascript" src="../web/js/windowKeyboard.js"></script>
    <script type="text/javascript" id="paramDirectory">var baseDirectory = "../web/";</script>
    <script type="text/javascript" id="paramLanguage">var defaultLang="en_US";</script>
        <script type="text/javascript" src="../web/js/jquery-1.7.2.min.js"></script>
    
 <script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.plugin.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.ummalqura.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.ummalqura-ar.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.plus.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.picker.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.picker.ext.js"></script>
    
    <script type="text/javascript">
    function validate(action) {return true;}
    var changeFlag = 0;
    function onResizeDo()
    {
        resizeArea();
        reSizeGrid();
    }

    function onLoadDo()
    {
        this.windowTables = new Array(
          new windowTableId('client', 'buttonOK')
        );
        setWindowTableParentElement();
        this.tabsTables = new Array(
          new tabTableId('tdtopTabs')
        );
        setTabTableParentElement();
        setBrowserAutoComplete(false);
        setFocusFirstControl(null, 'inpMailTemplate');
        resizeArea();
        updateMenuIcon('buttonMenu');
                 
        $('#inpFromDate').calendarsPicker({calendar:  
            $.calendars.instance('ummalqura'),dateFormat: 'dd-mm-yyyy',onSelect : function(dates){onChangeField();},showTrigger:  
        '<img src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/calendar.gif" alt="Popup" class="trigger">'});
   
        $('#inpToDate').calendarsPicker({calendar:  
            $.calendars.instance('ummalqura'),dateFormat: 'dd-mm-yyyy',onSelect : function(dates){onChangeField();},showTrigger:  
        '<img src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/calendar.gif" alt="Popup" class="trigger">'});
        
        var save = "<%= (request.getAttribute("Save")==null?"":request.getAttribute("Save")) %>";
        if(save!="")
        {
            if(save=="0")
                displayMessage("E", "<%=Resource.getProperty("utility.error",lang)%>","<%=Resource.getProperty("utility.approvaldelegation.save.error",lang)%>");
            else if(save=="1")
                displayMessage("S", "<%=Resource.getProperty("utility.success",lang)%>","<%=Resource.getProperty("utility.approvaldelegation.save.success",lang)%>");
            else if(save=="2")
                displayMessage("E", "<%=Resource.getProperty("utility.error",lang)%>","<%=Resource.getProperty("utility.approvaldelegation.exists.error",lang)%>");
        }
    
        var selectUsr="<%=request.getAttribute("selectUsr")%>";
        var selectRol="<%=request.getAttribute("selectRol")%>";
        $('#inpFromUserId option[value='+selectUsr+']').attr("selected", "selected");
        $('#inpFromUserId').next().val($('#inpFromUserId :selected').text());
        
        $('#inpFromRoleId option[value='+selectRol+']').attr("selected", "selected");           
        $('#inpFromRoleId').next().val($('#inpFromRoleId :selected').text());
        
        var headId="<%=request.getAttribute("headerId")%>";
        if(headId!="" && headId!=null){
            var processed ="<%=request.getAttribute("Processed")%>";
            if(processed=="N")
                enableDeleteButton("true");
        }
        enableSaveButton("false");       
    }
    function enableDeleteButton(flag)
    {
        var processed ="<%=request.getAttribute("Processed")%>";
        if(flag=="true")
        {          
            if(processed=="N"){
                document.getElementById('buttonDelete').className = 'Main_ToolBar_Button';
                document.getElementById('linkButtonDelete').className = 'Main_ToolBar_Button_Icon Main_ToolBar_Button_Icon_Delete';         
            }            
        }
        else if(flag=='false')
        {               
            document.getElementById('buttonDelete').className = 'Main_ToolBar_Button_disabled';
            document.getElementById('linkButtonDelete').className = 'Main_ToolBar_Button_Icon Main_ToolBar_Button_Icon_Delete_disabled';
        }
    }

    function enableSaveButton(flag)
    {
        if(flag=="true")
        {               
            document.getElementById('linkButtonSave').className = 'Main_ToolBar_Button';
            document.getElementById('buttonSave').className = 'Main_ToolBar_Button_Icon Main_ToolBar_Button_Icon_Save';
        }
        else if(flag=='false')
        {               
            document.getElementById('linkButtonSave').className = 'Main_ToolBar_Button_disabled';
            document.getElementById('buttonSave').className = 'Main_ToolBar_Button_Icon Main_ToolBar_Button_Icon_Save_disabled';
        }
    }
    </script>

</HEAD>
<BODY onresize="onResizeDo();" leftmargin="0" topmargin="0" marginwidth="0" marginheight="0" id="paramBody" >
<FORM id="form" method="post" action="" name="frmMain">
  <INPUT type="hidden" name="Command"></INPUT>
  <INPUT type="hidden" name="IsPopUpCall" value="1"></INPUT>
  <INPUT type="hidden" name="inpLastFieldChanged"></INPUT>
  <INPUT type="hidden" name="inpAction" id="inpAction" value=""></INPUT>
  <INPUT type="hidden" name="inpDocList" id="inpDocList" value=""></INPUT>
  <INPUT type="hidden" name="inpSave" id="inpSave" value=""></INPUT>
  <INPUT type="hidden" name="headerId" id="headerId" value="<%=request.getAttribute("headerId")%>"></INPUT>
  <INPUT type="hidden" name="nowdate" id="nowdate" value="<%=request.getAttribute("inpNowDate")%>"></INPUT>
        
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
        <DIV class="Main_ContentPane_NavBar" id="tdtopButtons">
          <TABLE class="Main_ContentPane_NavBar" id="tdtopNavButtons"></TABLE>
        </DIV>
<!--  Manual Code   -->
        <DIV class="Main_ContentPane_ToolBar" id="paramToolBar">
          <table class="Main_ContentPane_ToolBar Main_ToolBar_bg" id="tdToolBar">
            <tr>            
              <td width="2%"><a id="linkButtonNew" onblur="this.hideFocus=false" onclick="onClickNew()" onmouseout="window.status='';return true;" onmouseover="window.status='<%=Resource.getProperty("utility.new",lang)%>';return true;" class="Main_ToolBar_Button" href="javascript:void(0);"><img border="0" id="buttonNew" title='<%=Resource.getProperty("construction.new",lang)%>' src="../web/images/blank.gif" class="Main_ToolBar_Button_Icon Main_ToolBar_Button_Icon_New"></a></td>
              <td class="Main_ToolBar_Separator_cell" ><img src="../web/images/blank.gif" class="Main_ToolBar_Separator"></td> 
              <td width="2%" ><a href="javascript:void(0);" onClick="onClickGridView()" class="Main_ToolBar_Button" onMouseOver="window.status='<%= Resource.getProperty("utility.gridview",lang).replaceAll("\"", "\\\\\\\"")%>';return true;" onMouseOut="window.status='';return true;" onclick="this.hideFocus=true" onblur="this.hideFocus=false" id="buttonRelation"><img class="Main_ToolBar_Button_Icon Main_ToolBar_Button_Icon_Relation" src="../web/images/blank.gif" title="<%= Resource.getProperty("construction.gridview",lang).replaceAll("\"", "\\\\\\\"")%>" border="0" id="linkButtonRelation"></a></td>
              <td class="Main_ToolBar_Separator_cell" ><img src="../web/images/blank.gif" class="Main_ToolBar_Separator"></td>       
              <td width="2%" ><a href="javascript:void(0);" onClick="saveApprovalDelegation();" class="Main_ToolBar_Button_disabled" onMouseOver="window.status='Save Record';return true;" onMouseOut="window.status='';return true;" id="linkButtonSave"><img class="Main_ToolBar_Button_Icon Main_ToolBar_Button_Icon_Save_disabled" src="../web/images/blank.gif" title="<%= Resource.getProperty("utility.save", lang)  %>" border="0" id="buttonSave"></a></td>  
              <td class="Main_ToolBar_Separator_cell" ><img src="../web/images/blank.gif" class="Main_ToolBar_Separator"></td>
              <td width="2%" ><a href="javascript:void(0);" onClick="onClickDelete()" class="Main_ToolBar_Button_disabled" onMouseOver="window.status='Delete Record';return true;" onMouseOut="window.status='';return true;" id="buttonDelete"><img class="Main_ToolBar_Button_Icon Main_ToolBar_Button_Icon_Delete_disabled" src="../web/images/blank.gif" title="<%= Resource.getProperty("utility.delete", lang) %>" border="0" id="linkButtonDelete"></a></td>
              <td class="Main_ToolBar_Separator_cell" ><img src="../web/images/blank.gif" class="Main_ToolBar_Separator"></td>
              <td width="2%" ><a href="javascript:void(0);" onClick="onClickRefresh()" class="Main_ToolBar_Button" onMouseOver="window.status='<%= Resource.getProperty("utility.reload",lang).replaceAll("\"", "\\\\\\\"")%>';return true;" onMouseOut="window.status='';return true;" onclick="this.hideFocus=true" onblur="this.hideFocus=false" id="buttonRefresh"><img class="Main_ToolBar_Button_Icon Main_ToolBar_Button_Icon_Refresh" src="../web/images/blank.gif" title="<%= Resource.getProperty("construction.reload",lang).replaceAll("\"", "\\\\\\\"")%>" border="0" id="linkButtonRefresh"></a></td>
              <td class="Main_ToolBar_Separator_cell" ><img src="../web/images/blank.gif" class="Main_ToolBar_Separator"></td>            
              <td class="Main_ToolBar_Space"></td>
            </tr>
          </table>
        </DIV>

       <TABLE width="100%" border="0" cellspacing="0" cellpadding="0" class="Main_ContentPane_TabPane" id="tdtopTabs">
                  <TR id="paramParentTabContainer"><td class="tabBackGroundInit"></td></TR>
                  <TR id="paramMainTabContainer"><td class="tabBackGroundInit"><span class="tabTitle"></span></td></tr>
                  <TR><TD class="tabBackGround">
                    <div class="marginLeft">
                    <div><span class="dojoTabcurrentfirst"><div>
                         <span><a class="dojoTabLink" href="javascript:void(0);" onclick="return false;" onMouseOver="return true;" onMouseOut="return true;"><%= Resource.getProperty("utility.approvaldelegation.requestapprovaldelegation", lang)%></a></span>
                    </div></span></div>
                    </div>
                  </TD></TR>
        </TABLE>

        <DIV class="Main_ContentPane_Client" style="overflow: auto;" id="client">
              <TABLE class="dojoTabContainer" border="0" cellpadding="0" cellspacing="0" width="100%" summary="">
                <TR>
                  <TD>
                    <TABLE class="dojoTabPaneWrapper" border="0" cellpadding="0" cellspacing="0" width="100%" summary="">
                      <TR>
                        <TD>
                        
                        <TABLE cellpadding="0" cellspacing="0" id="messageBoxID" class="MessageBox"><TBODY>
                            <TR class="MessageBox_TopMargin"><TD class="MessageBox_LeftMargin"></TD><TD></TD><TD class="MessageBox_RightMargin"></TD></TR>
                            <TR><TD class="MessageBox_LeftMargin"></TD><TD>
                                  <TABLE cellpadding="0" cellspacing="0" class="MessageBox_Container"><TBODY>
                                      <TR>
                                        <TD class="MessageBox_LeftTrans"><TABLE style="width: 100%; height: 100%" cellpadding="0" cellspacing="0"><TBODY><TR><TD class="MessageBox_TopLeft"></TD></TR><TR><TD class="MessageBox_Left"></TD></TR></TBODY></TABLE></TD>
                                        <TD class="MessageBox_bg"><TABLE class="MessageBox_Top"><TBODY><TR><TD><SPAN><TABLE cellpadding="0" cellspacing="0" class="MessageBox_Body_ContentCell"><TBODY><TR><TD class="MessageBox_Icon_ContentCell"><DIV class="MessageBox_Icon"></DIV></TD>
                                        <TD style="vertical-align: top;" id="messageBoxIDContent"><SPAN><DIV class="MessageBox_TextTitle" id="messageBoxIDTitle"></DIV><DIV class="MessageBox_TextDescription" id="messageBoxIDMessage"></DIV><DIV class="MessageBox_TextSeparator"></DIV></SPAN></TD></TR></TBODY></TABLE></SPAN>
                                        <div id="hideMessage" style="float: right; margin-top:-13px; margin-right:10px;"><a style="color: yellow;font-size: 11.5px" href="javascript:void(0);" onClick="document.getElementById('messageBoxID').style.display = 'none';"><%=Resource.getProperty("utility.hide",lang)%></a></div></TD></TR></TBODY></TABLE></TD>
                                        <TD class="MessageBox_RightTrans"><TABLE style="width: 100%; height: 100%" cellpadding="0" cellspacing="0"><TBODY><TR><TD class="MessageBox_TopRight"></TD></TR><TR><TD class="MessageBox_Right"></TD></TR></TBODY></TABLE></TD>
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
                                </TD><TD class="MessageBox_RightMargin"></TD>
                            </TR>
                            <TR class="MessageBox_BottomMargin"><TD class="MessageBox_LeftMargin"></TD><TD></TD><TD class="MessageBox_RightMargin"></TD></TR></TBODY>
                        </TABLE>
                        
<!--  Start Manual Code -->
                      <table width="100%" cellpadding="0" cellspacing="0" border="0">                       
                       <tr>
                        <td align="center">
                         <table width="75%" cellpadding="0" cellspacing="3" border="0">
                          <TR>                          
                            <TD class="TitleCell"><span class="LabelText"><%= Resource.getProperty("utility.fromdate", lang)%></SPAN></TD>
                            <%-- <td class="TextBox_btn_ContentCell" name="inpFromDate_inp_td" id="inpFromDate_inp_td"><div id="newDiscard"><div name="inpFromDate_inp" id="inpFromDate_inp">
                                <table border="0" cellspacing="0" cellpadding="0" summary="" class=""><tbody><tr><td class="TextBox_ContentCell">
                                    <table style="border:0px;border-collapse:collapse;"><tbody><tr><td style="padding-top: 0px;">
                                    <input type="text" id="inpFromDate" class="dojoValidateValid_focus required_focus TextBox_btn_OneCell_width TextBox_focus" required="true" value="<%= request.getAttribute("inpFromDate") %>" maxlength="10" name="inpFromDate" onkeyup="autoCompleteDate(this);onChangeField();" onkeydown="showCalendar('frmMain.inpFromDate', document.frmMain.inpFromDate.value, false);return false;" onkeypress="changeToEditingMode('onkeypress');" oncut="changeToEditingMode('oncut');" onpaste="changeToEditingMode('onpaste');" oncontextmenu="changeToEditingMode('oncontextmenu');" onblur="expandDateYear(this.id);" onchange="validateDateTextBox(this.id);logChanges(this);onChangeField();return true;" displayformat="DD-MM-YYYY" saveformat="DD-MM-YYYY">
                                    </td></tr></tbody></table>
                                    <span class="TextBox_MsgContainer_span" style="display: none;" name="inpFromDateinvalidSpan" id="inpFromDateinvalidSpan"><table class="TextBox_MsgContainer_table"><tbody><tr class="TextBox_MsgContainer_tr"><td class="TextBox_MsgContainer_td"><div class="TextBox_MsgContainer_div"></div></td><td></td></tr><tr><td colspan="2" class="invalid"><div class="TextBox_MsgContainer_div2" id="inpFromDateinvalidSpanText" name="invalidText"><%= Resource.getProperty("utility.invaliddateformat", lang)%></div></td></tr></tbody></table></span>
                                    <span class="TextBox_MsgContainer_span" style="display: none;" name="inpFromDatemissingSpan" id="inpFromDatemissingSpan"><table class="TextBox_MsgContainer_table"><tbody><tr class="TextBox_MsgContainer_tr"><td class="TextBox_MsgContainer_td"><div class="TextBox_MsgContainer_div"></div></td><td></td></tr><tr><td colspan="2" class="missing"><div class="TextBox_MsgContainer_div2" id="inpFromDatemissingSpanText" name="missingText"><%= Resource.getProperty("utility.invaliddateformat", lang)%></div></td></tr></tbody></table></span>
                                    </td><td class="FieldButton_ContentCell"><a class="FieldButtonLink" href="javascript:void(0);" onfocus="setWindowElementFocus(this); window.status='Scheduled Delivery Date'; return true;" onblur="window.status=''; return true;" onkeypress="this.className='FieldButtonLink_active'; return true;" onkeyup="this.className='FieldButtonLink_focus'; return true;" onclick="showCalendar('frmMain.inpFromDate', document.frmMain.inpFromDate.value, false);return false;"><table class="FieldButton" onmousedown="this.className='FieldButton_active'; return true;" onmouseup="this.className='FieldButton'; return true;" onmouseover="this.className='FieldButton_hover'; window.status='From Date'; return true;" onmouseout="this.className='FieldButton'; window.status=''; return true;" id="inpFromDatelinkCalendar" name="inpFromDatelinkCalendar"><tbody><tr><td class="FieldButton_bg"><img alt="From Date" class="FieldButton_Icon FieldButton_Icon_Calendar" title="From Date" src="../web/images/blank.gif" border="0" id="buttonCalendar"></td></tr></tbody></table></a></td></tr>
                                    </tbody>
                                </table>
                                </div></div>
                            </td> --%>
                            <TD class="TextBox_btn_ContentCell">
                                                                <table border="0" cellspacing="0" cellpadding="0" summary=""  style="padding-top: 0px;">
                                                                  <tr>
                                                                      <td class="TextBox_ContentCell">
                                                                       <%  
                                                                        if(request.getAttribute("inpFromDate")!= null ) {%>
                                                                             <input type="text" id="inpFromDate" class="dojoValidateValid TextBox_btn_OneCell_width required" onchange="document.getElementById('messageBoxID').style.display = 'none';onChangeField();" value="<%= request.getAttribute("inpFromDate") %>" maxlength="10" name="inpFromDate"   ></input> 
                                                                         <%}
                                                                          else{ %>
                                                                          <input type="text" id="inpFromDate" class="dojoValidateValid TextBox_btn_OneCell_width required" onchange="document.getElementById('messageBoxID').style.display = 'none';onChangeField(); "  value="<%= request.getAttribute("inpFromDate") %>" maxlength="10" name="inpFromDate" ></input>
                                                                          <%} %> 
                                                                      </td>
                                                                 </tr>
                                                                </table>
                                                               </TD> 
                            
                            <TD class="TitleCell"><span class="LabelText"><%= Resource.getProperty("utility.todate", lang)%></SPAN></TD>
                            <%-- <td class="TextBox_btn_ContentCell" name="inpToDate_inp_td" id="inpToDate_inp_td"><div id="newDiscard"><div name="inpToDate_inp" id="inpToDate_inp">
                                <table border="0" cellspacing="0" cellpadding="0" summary="" class=""><tbody><tr><td class="TextBox_ContentCell">
                                    <table style="border:0px;border-collapse:collapse;"><tbody><tr><td style="padding-top: 0px;">
                                    <input type="text" id="inpToDate" class="dojoValidateValid_focus required_focus TextBox_btn_OneCell_width TextBox_focus" required="true" value="<%= request.getAttribute("inpToDate") %>" maxlength="10" name="inpToDate" onkeyup="autoCompleteDate(this);onChangeField();" onkeydown="showCalendar('frmMain.inpToDate', document.frmMain.inpToDate.value, false);return false;" onkeypress="changeToEditingMode('onkeypress');" oncut="changeToEditingMode('oncut');" onpaste="changeToEditingMode('onpaste');" oncontextmenu="changeToEditingMode('oncontextmenu');" onblur="expandDateYear(this.id);" onchange="validateDateTextBox(this.id);logChanges(this);onChangeField();return true;" displayformat="DD-MM-YYYY" saveformat="DD-MM-YYYY">
                                    </td></tr></tbody></table>
                                    <span class="TextBox_MsgContainer_span" style="display: none;" name="inpToDateinvalidSpan" id="inpToDateinvalidSpan"><table class="TextBox_MsgContainer_table"><tbody><tr class="TextBox_MsgContainer_tr"><td class="TextBox_MsgContainer_td"><div class="TextBox_MsgContainer_div"></div></td><td></td></tr><tr><td colspan="2" class="invalid"><div class="TextBox_MsgContainer_div2" id="inpToDateinvalidSpanText" name="invalidText">Invalid Date Format</div></td></tr></tbody></table></span>
                                    <span class="TextBox_MsgContainer_span" style="display: none;" name="inpToDatemissingSpan" id="inpToDatemissingSpan"><table class="TextBox_MsgContainer_table"><tbody><tr class="TextBox_MsgContainer_tr"><td class="TextBox_MsgContainer_td"><div class="TextBox_MsgContainer_div"></div></td><td></td></tr><tr><td colspan="2" class="missing"><div class="TextBox_MsgContainer_div2" id="inpToDatemissingSpanText" name="missingText">Invalid Date Format</div></td></tr></tbody></table></span>
                                    </td><td class="FieldButton_ContentCell"><a class="FieldButtonLink" href="javascript:void(0);" onfocus="setWindowElementFocus(this); window.status='To Date'; return true;" onblur="window.status=''; return true;" onkeypress="this.className='FieldButtonLink_active'; return true;" onkeyup="this.className='FieldButtonLink_focus'; return true;" onclick="showCalendar('frmMain.inpToDate', document.frmMain.inpToDate.value, false);return false;"><table class="FieldButton" onmousedown="this.className='FieldButton_active'; return true;" onmouseup="this.className='FieldButton'; return true;" onmouseover="this.className='FieldButton_hover'; window.status='To Date'; return true;" onmouseout="this.className='FieldButton'; window.status=''; return true;" id="inpToDatelinkCalendar" name="inpToDatelinkCalendar"><tbody><tr><td class="FieldButton_bg"><img alt="To Date" class="FieldButton_Icon FieldButton_Icon_Calendar" title="To Date" src="../web/images/blank.gif" border="0" id="buttonCalendar"></td></tr></tbody></table></a></td></tr>
                                    </tbody>
                                </table>
                                </div></div>
                            </td> --%>
                            <TD class="TextBox_btn_ContentCell">
                                                            <table border="0" cellspacing="0" cellpadding="0" summary=""  style="padding-top: 0px;">
                                                              <tr>
                                                                    <td class="TextBox_ContentCell">
                                                                    <%  
                                                                    if(request.getAttribute("inpToDate")!= null ) {%>
                                                                         <input type="text" id="inpToDate" class="dojoValidateValid TextBox_btn_OneCell_width required" onchange="document.getElementById('messageBoxID').style.display = 'none';onChangeField();"  value="<%= request.getAttribute("inpToDate") %>" maxlength="10" name="inpToDate" ></input> 
                                                                     <%}
                                                                      else{ %>
                                                                      <input type="text" id="inpToDate" class="dojoValidateValid TextBox_btn_OneCell_width required" onchange="document.getElementById('messageBoxID').style.display = 'none';onChangeField(); "  value="" maxlength="10" name="inpToDate" ></input>
                                                                      <%} %> 
                                                                 </td>
                                                             </tr>
                                                            </table>
                                                           </TD> 
                          </TR>
                          <tr>                                        
                            <TD class="TitleCell" valign="middle" nowrap><span class="LabelText"><%= Resource.getProperty("utility.fromuser", lang)%></SPAN></TD>
                            <TD class="Combo_ContentCell"  valign="middle">                          
                                <SELECT name="inpFromUserId" id="inpFromUserId"> 
                                 <% if(usrLs!=null  && (isAdminUser.equals("true")  || (isAdminUser.equals("false") && userId!=null && userId!="") ))
                                {
                                    if(usrLs.size()>0)
                                    {%>                                         
                                        <%for(JSONObject usrVO:usrLs)
                                        {%>                         
                                            <option value='<%=usrVO.getString("id")%>'><span><%=Utility.escapeQuote(usrVO.getString("name")) %></span></option>
                                        <%}%>
                                    <%}
                                }%> 
                                </SELECT>                                                           
                            </TD>
                            <TD class="TitleCell"><span class="LabelText"><%= Resource.getProperty("utility.fromrole", lang)%></SPAN></TD>
                            <TD class="Combo_ContentCell"><DIV id="newDiscard">
                                <DIV>
                                    <SELECT name="inpFromRoleId" class="ComboKey Combo_TwoCells_width" id="inpFromRoleId" onchange="onChangeField();onChangeFromRole();">
                                    <%if(roleLs!=null && (isAdminUser.equals("true")  || (isAdminUser.equals("false") && userId!=null  && userId!="") ))
                                    {
                                        if(roleLs.size()>0)
                                        {%>                                         
                                            <%for(JSONObject rolVO:roleLs)
                                            {%>                         
                                                <option value='<%=rolVO.getString("id")%>'><span><%=Utility.escapeQuote(rolVO.getString("name")) %></span></option>
                                            <%}%>
                                        <%}
                                    }%>
                                    </SELECT>                                         
                                </div></DIV>
                            </TD>
                         </tr>
                         <tr>
                            <TD class="TitleCell" valign="middle" nowrap><span class="LabelText"><%= Resource.getProperty("utility.touser", lang)%></SPAN></TD>
                            <TD class="Combo_ContentCell"  valign="middle">                             
                                    <SELECT name="inpToUserId" id="inpToUserId">
                                    <option value='0' selected></option>
                                    <%if(toUsrLs!=null)
                                    {
                                        if(toUsrLs.size()>0)
                                        {%>                                         
                                            <%for(JSONObject jsonObject:toUsrLs)
                                            {%>                         
                                                <option value='<%=jsonObject.getString("id")%>'><span><%=Utility.escapeQuote(jsonObject.getString("name"))%></span></option>
                                            <%}%>
                                        <%}
                                    }%>                                 
                                    </SELECT>
                            </TD>
                         </tr>
                         </table>
                         <table>
                         <tr>
                            <td>
                              <div id="jqgrid">
                                <table id="DocList" class="scroll" cellpadding="0" cellspacing="0" width="100%"></table>
                                <div id="pager" class="scroll" style="text-align: center;"></div>
                              </div>
                            </td>
                         </tr>
                     </table>                           
                    </td>
                   </tr>
                  </table>
<!--  End Manual Code -->
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
<script type="text/javascript" src="../web/js/jquery/grid.locale-en.js"></script>
 <script type="text/javascript" src="../web/js/jquery/jquery.jqGrid.min.js"></script>
<script src="../web/js/ui/jquery.ui.widget.js"></script>
<script src="../web/js/ui/jquery.ui.mouse.js"></script>
<script src="../web/js/ui/jquery.ui.button.js"></script>
<script src="../web/js/ui/jquery.ui.draggable.js"></script>
<script src="../web/js/ui/jquery.ui.position.js"></script>
<script src="../web/js/ui/jquery.ui.autocomplete.js"></script>
<script src="../web/js/ui/jquery.ui.resizable.js"></script>
<script src="../web/js/ui/jquery.ui.dialog.js"></script> 
<script type="text/javascript">

function onChangeField(){
    changeFlag=1;
    enableSaveButton('true');
}
function onChangeFromRole(){
    jQuery("#DocList").trigger("reloadGrid");
}
function onProcessChange()
{    
    if($("#inpToDate").val()!=null || $("#inpFromDate").val()!=null )
        {
         var httpurl = "<%=request.getContextPath()%>/sa.elm.ob.utility.ad_forms.delegation.ajax/ApprovalDelegationAjax?inpAction=processedRecord";
           $.ajax({
             type: 'GET',
             url: httpurl,
             data: {
               inpHeaderId:$('#headerId').val()
             },
             dataType: 'json',
             async: false
           }).done(function (response) {
         });
        }
 
   
    }

function onClickGridView(){
    document.getElementById("inpAction").value = "GridView";
    reloadWindow();
}
function onClickRefresh()
{   
    document.getElementById("inpAction").value = "EditView";
    reloadWindow();
}
function onClickNew()
{
    document.getElementById("inpAction").value = "EditView";
    document.getElementById("headerId").value="";
    reloadWindow();
}
function onClickDelete()
{           
    if(document.getElementById("headerId").value!='' && document.getElementById('buttonDelete').className == 'Main_ToolBar_Button' && document.getElementById("headerId").value!=null)
    {
        var processed ="<%=request.getAttribute("Processed")%>";
        if(processed=="N"){
             OBConfirm("<%= Resource.getProperty("utility.approvaldelegation.suretodelete", lang) %>",
                        function(result)
                        {
                            if(result)
                            {
                                document.getElementById("inpAction").value = "GridView";
                                document.getElementById("inpSave").value = "Delete";
                                reloadWindow();
                            }
                        });
        } else{
            OBAlert("<%= Resource.getProperty("utility.approvaldelegation.delete.error", lang) %>");
            return false;
        }
       
    }
}
function reloadWindow()
{
    submitCommandForm('DEFAULT', true, null, '<%=request.getContextPath()%>/sa.elm.ob.utility.ad_forms.delegation.header/ApprovalDelegation', '_self', null,true);
    return false;
}
function reSizeGrid()
{
    var w, h;
    if (window.innerWidth)  { w = window.innerWidth - 56; h = window.innerHeight - 280; }
    else if (document.body)  { w = document.body.clientWidth - 56; h = document.body.clientHeight - 280; }      
        
    jQuery("#DocList").setGridWidth(w,true);
    jQuery("#DocList").setGridHeight(h,true);
} 
    
var gridIds=0;
var gridIdsLength=0;
jqgridcalling();
function jqgridcalling(){
jQuery(function() 
{
    jQuery("#DocList").jqGrid(
    {
        url:"<%=request.getContextPath()%>/sa.elm.ob.utility.ad_forms.delegation.ajax/ApprovalDelegationAjax?inpAction=getDocList",
        mtype: 'POST',          
        colNames:['<%=Resource.getProperty( "utility.accesscontrol.documentrule.docmenttype", lang) %>','UserName','<%=Resource.getProperty( "utility.touser", lang) %>', 'UserId', 'Type', '<%=Resource.getProperty( "utility.torole", lang) %>', 'RoleId', 'Rolename'],              
        colModel:[                    
              {name:'document_type', index:'document_type', sortable:false,search:false},
              {name:'username', index:'username', hidden: true},
              <%-- {name:'touser',index:'touser', width:180, editable:true, formatter: 'select', edittype: 'select', editrules:{required:true},
                  editoptions: 
                  {dataUrl:'<%=request.getContextPath()%>/com.qualiantech.utility.ad_forms.delegation.ajax/ApprovalDelegationAjax?inpAction=getToUsers',
                     buildSelect: function(data) {                                   
                         var response = jQuery.parseJSON(data);                                                                                                      
                          var s = '<select>';
                          if (response && response.length) {                             
                              for (var i = 0, l=response.length; i<l ; i++) {                                                
                                  s += '<option value="'+response[i].UserId+'">'+response[i].UserName+'</option>';
                              }
                          }
                          return s + "</select>"; 
                      }      
                  }}, --%>
              {name:'touser',index:'touser', width:200, formatter:customSelectBox, sortable:false,search:false},
              {name:'userId', index:'userId', hidden: true},
              {name:'type', index:'type', hidden: true},
              {name:'torole',index:'torole', width:200, sortable:false,search:false},
              {name:'roleId', index:'roleId', hidden: true},
              {name:'rolename', index:'rolename', hidden: true}
             ],
             
    pager: '#pager',
    datatype: 'xml',
    rownumbers: true,   
    viewrecords: true,  
    autowidth: true,
    shrinkToFit: true,
    sortname: 'document_type',
    sortorder: "asc",
    height:"90%",
    pgbuttons:false,
    pgtext:'<%=Resource.getProperty( "utility.approvaldelegation.page", lang) %> {0} <%=Resource.getProperty( "utility.of", lang) %> {1}',
    recordtext:'<%=Resource.getProperty( "utility.approvaldelegation.view", lang) %> {0} - {1} <%=Resource.getProperty( "utility.of", lang) %> {2}',
    multiselect: true, 
    
    beforeRequest: function()
    {                       
        jQuery("#DocList").setPostDataItem("headerId", document.getElementById("headerId").value);      
        jQuery("#DocList").setPostDataItem("type", "E");
        var selectRol="<%=request.getAttribute("selectRol")%>";
        if(selectRol!=null && selectRol==$('#inpFromRoleId').val()){
            jQuery("#DocList").setPostDataItem('roleId',selectRol);
        }
       // jQuery("#DocList").setPostDataItem('roleId',$('#inpFromRoleId').val());
         else{
        jQuery("#DocList").setPostDataItem('roleId',$('#inpFromRoleId').val());

        } 
        
        var selectUsr="<%=request.getAttribute("selectUsr")%>";

        if(selectUsr!=null && selectUsr==$('#inpFromUserId').val()){
            jQuery("#DocList").setPostDataItem('userId',selectUsr);
        }
         else{
             jQuery("#DocList").setPostDataItem('userId',$("#inpFromUserId option:selected").val());


        } 
       
    },
    loadComplete: function() 
    {
        ChangeJQGridAllRowColor(jQuery("#DocList"));    
        $('input[role*="checkbox"]').click(function(){jQuery("#DocList").setSelection($(this).parent().parent().attr('id'));});     
        gridIds=""+jQuery("#DocList").getDataIDs();
        gridIdsLength=gridIds.length;
        gridIds=gridIds.split(",");
        var i=0, j=0;
        for(i;i<gridIds.length;i++)
        {
            var rowData = jQuery("#DocList").getRowData(gridIds[i]);            
            var type = rowData['type'];                 
            if(type=="U"){
                var userId = rowData['userId']; 
                var roleId= rowData['roleId']; 
                jQuery("#DocList").setSelection(gridIds[i]);
                $('#'+gridIds[i]+'_touser option[value='+userId+']').attr("selected", "selected");
                $('#'+gridIds[i]+'_torole option[value='+roleId+']').attr("selected", "selected");
                
                $('#'+gridIds[i]+'_touser').next().val($('#'+gridIds[i]+'_touser :selected').text());
                $('#'+gridIds[i]+'_torole').next().val($('#'+gridIds[i]+'_torole :selected').text());
                j++;
            }
            $('#'+gridIds[i]+'_touser').combobox();
        }
        if(gridIds.length==j)
            document.getElementById("cb_DocList").checked = true;
        /* if($('#inpToUserId').val()!= null){
            setToUser($('#inpToUserId').val());
        } */
        
        enableSaveButton('false');
        reSizeGrid();
    },
    onSelectRow: function(id, status)
    {
        var ids = ""+jQuery("#DocList").jqGrid('getGridParam','selarrrow');
        var idList=ids.split(",");
        var idListLength=idList.length;
        if(ids.indexOf(",")==0)
            idListLength--;
        if(status==true)
        {
            ChangeJQGridSelectMultiRowColor(id, "S");
            if(idListLength==gridIdsLength)
            {
                document.getElementById("cb_DocList").checked = true;
            }
        }
        else if (status==false)
        {
            ChangeJQGridSelectMultiRowColor(id, "US");
            jQuery('#DocList').restoreRow(id);
            if(document.getElementById("cb_DocList").checked = true)
                document.getElementById("cb_DocList").checked = false;
        }
        onChangeField();
    },
    onSelectAll:  function(id,status)
    {
        onChangeField();
        var ids = ""+id;
        var idList=ids.split(",");
        if(idList.indexOf(",")==0)
            i=1;
        else
            i=0;
        for(var i=i;i<idList.length;i++)
        {
            if(status==true)
            {
                ChangeJQGridSelectMultiRowColor(idList[i], "S");
                jQuery('#DocList').editRow(idList[i], true);
            }   
            else if (status==false)
            {
                ChangeJQGridSelectMultiRowColor(idList[i], "US");       
                jQuery('#DocList').restoreRow(idList[i]); 
            }
        }
    },
    caption:''
     
});    
jQuery("#DocList").jqGrid('navGrid','#pager',{edit:false,add:false,del:false,search:false,view: false,beforeRefresh:function(){         
        jQuery("#DocList").trigger("reloadGrid");
    }
},{ },{ },{ }, { });
reSizeGrid();   
});

}
function customSelectBox(el, cellval, opts)
{   
    var rowKey = cellval.rowId;
    var i=0;
    var userId="";
    /* var select = '<select name='+rowKey+' id="'+rowKey+'_touser" style="width: 400px" class="editable ui-jqgrid-celledit" onchange="onChangeField();getRoles(this.value, \'l\', \''+rowKey+'\');">'; */
    var select = '<SELECT name='+rowKey+' id="'+rowKey+'_touser" style="width: 400px">';
    select+='<option value="0" role="option"></option>';
    <%if(toUsrLs!=null)
    {
        if(toUsrLs.size()>0)
        {%>                                         
            <%for(JSONObject jsonObject:toUsrLs)
            {%>     
                if(i==0)
                    userId="<%=jsonObject.getString("id")%>";
                select+='<option value="<%=jsonObject.getString("id")%>" role="option"><%=Utility.escapeQuote(jsonObject.getString("name")) %></option>';
                i++;            
            <%}%>
        <%}
    }%>     
    select+='</select>';
    return select;   
}
/* function customRoleSelectBox(el, cellval, opts){
    var rowKey = cellval.rowId;         
    var select = '<select name='+rowKey+' id="'+rowKey+'_torole" style="width: 400px" class="editable ui-jqgrid-celledit" onchange="onChangeField();"><option value="0" role="option"></option></select>';
    return select;  
} */
function getRoles(userId, type, roleKey){   
    if(userId!="" && userId!=null)
    {
        if(type=='h')
            document.getElementById("inpFromRoleId").length=0;
        else if(type=='l'){
            $('#'+userId).val(userId);
            if(document.getElementById(roleKey+'_torole')!=null)
                document.getElementById(roleKey+'_torole').length=0;
        }
        var url="<%=request.getContextPath()%>/sa.elm.ob.utility.ad_forms.delegation.ajax/ApprovalDelegationAjax?inpAction=getRoles&userId="+userId+"&type="+type;
        $.getJSON(url, function(result){            
        }).done(function(data){         
            $.each( data, function( i, item ) 
            {                   
                if(item.RoleId!="")
                {                   
                    var newOption = document.createElement("option");
                    if(type=='h')
                        document.getElementById("inpFromRoleId").options.add(newOption);
                    else 
                        document.getElementById(roleKey+'_torole').options.add(newOption);
                    newOption.text = item.RoleName;
                    newOption.value = item.RoleId;          
                }
            });
            if(type == 'h'){
                jQuery("#DocList").trigger("reloadGrid");               
            }
        });
    }
}
function setToUser(userId)
{
    var gridIds=""+jQuery("#DocList").getDataIDs();
    gridIdsLength=gridIds.length;
    gridIds=gridIds.split(","); 
    for(var i=0;i<gridIds.length;i++)
    {                       
        //$('#'+gridIds[i]+'_touser option[value='+userId+']').attr("selected", "selected");        
        $('#'+gridIds[i]+'_touser').val(userId);
        $('#'+gridIds[i]+'_touser').next().val($('#'+gridIds[i]+'_touser :selected').text());
        if(document.getElementById(gridIds[i]+'_torole')!=null)
            getRoles(userId, 'l', gridIds[i]);
    }   
}
function validateDelegation(processed){
    var fromDate=$("#inpFromDate").val();
    var toDate=$("#inpToDate").val();
    var nowdate = $("#nowdate").val();    
    fromDate = new Date(fromDate.substring(3,5)+"/"+fromDate.substring(0,2)+"/"+fromDate.substring(6,10));
    toDate = new Date(toDate.substring(3,5)+"/"+toDate.substring(0,2)+"/"+toDate.substring(6,10));
    nowdate = new Date(nowdate.substring(3,5)+"/"+nowdate.substring(0,2)+"/"+nowdate.substring(6,10));
   /*  var currentDate = new Date();
    var curDay= new Date((parseInt(currentDate.getMonth()) + 1)+"/"+currentDate.getDate()+"/"+currentDate.getFullYear()); 
    alert(curDay); */
    if(processed=="N"){
        if(fromDate < nowdate)
        {
       OBAlert("<%= Resource.getProperty("utility.approvaldelegation.pastdateshouldnotallow", lang) %>");
        return false;
        }
    }   
    if(toDate < fromDate)
    {
       OBAlert("<%= Resource.getProperty("utility.approvaldelegation.todategreaterthanfromdate", lang) %>");
        return false;
    }
    return true;
}
function saveApprovalDelegation(){
    
    var doctype = {};
    doctype.Doctype = [];  
    if(changeFlag==1 && document.getElementById('linkButtonSave').className == 'Main_ToolBar_Button')
    {        
        var docs = ""+jQuery("#DocList").jqGrid('getGridParam','selarrrow');
        var fromUserId=$("#inpFromUserId").val();
        if(docs=="")
        {           
            OBAlert("<%= Resource.getProperty("utility.approvaldelegation.onedoctypetodelegate", lang) %>");
            return false;
        }
        else
        {      
            var processed ="<%=request.getAttribute("Processed")%>";
             if(processed=='N'){
                 $.ajax({
                     type:'GET',
                     url: '<%=request.getContextPath()%>/sa.elm.ob.utility.ad_forms.delegation.ajax/ApprovalDelegationAjax?inpAction=isProcessed',             
                     data:{inpheaderID:document.getElementById("headerId").value},
                     dataType:'json',
                     async:false
                     }).done(function(response) {
                         processed=response.isProcessed;
                     });
                 }
            if(validateDelegation(processed)){              
                if(processed=="Y"){
                    var canEdit = 0;
                    var fromDate=$("#inpFromDate").val();
                    var toDate=$("#inpToDate").val();
                    var nowdate = $("#nowdate").val();
                    var prevToDate = "<%=request.getAttribute("inpToDate")%>";
                    var prevFromDate = "<%=request.getAttribute("inpFromDate")%>";
                    
                    toDate = new Date(toDate.substring(3,5)+"/"+toDate.substring(0,2)+"/"+toDate.substring(6,10));
                    nowdate = new Date(nowdate.substring(3,5)+"/"+nowdate.substring(0,2)+"/"+nowdate.substring(6,10));
                    prevToDate = new Date(prevToDate.substring(3,5)+"/"+prevToDate.substring(0,2)+"/"+prevToDate.substring(6,10));

                    if(prevFromDate>fromDate || prevFromDate<fromDate){
                         canEdit=1;
                    }
                    else if(prevToDate>toDate || prevToDate<toDate){
                        if(toDate < nowdate){
                            canEdit=1;   
                        }                    
                        else if (prevToDate < nowdate){
                            canEdit=1;                  
                        }                                           
                    } else {
                        canEdit=1;                                  
                    }         
                    if(canEdit==1){
                        OBAlert("<%= Resource.getProperty("utility.approvaldelegation.delete.error", lang) %>");
                        return false;  
                    }
                }
                var idList=docs.split(",");
                for(var i=0;i<idList.length;i++)
                {
                    var rowData = jQuery("#DocList").getRowData(idList[i]); 
                    var docTypeNo=idList[i];                    
                    //var toUser =$("#"+idList[i]+"_touser").val();
                    var toUser =$('#'+idList[i]+'_touser :selected').val();                 
                    var toRole =$("#"+idList[i]+"_torole").val();
                    if(toUser==fromUserId)
                    {
                        OBAlert("<%= Resource.getProperty("utility.approvaldelegation.fromandtousrcannotbesame", lang) %>");
                        return false;
                    }
                    else{
                        if(toUser=="" || toUser=="0"){
                            OBAlert("<%= Resource.getProperty("utility.approvaldelegation.selecttouser", lang) %>");
                            return false;
                        }
                        else if(toRole=="" || toRole=="0"){ 
                            OBAlert("<%= Resource.getProperty("utility.approvaldelegation.selecttorole", lang) %>");
                            return false;
                        }
                    }                       
                    if(doctype.Doctype=="" || doctype.Doctype==null)
                    {               
                        doctype.Doctype[i]={};
                        doctype.Doctype[i]['DocNo']=docTypeNo;
                        doctype.Doctype[i]['ToUserId']=toUser;
                        doctype.Doctype[i]['ToRoleId']=toRole;                  
                    }
                    else
                    {
                        doctype.Doctype[i]={};
                        doctype.Doctype[i]['DocNo']=docTypeNo;
                        doctype.Doctype[i]['ToUserId']=toUser;
                        doctype.Doctype[i]['ToRoleId']=toRole;
                    }
                }
                OBConfirm("<%= Resource.getProperty("utility.approvaldelegation.suretosave", lang) %>",
                        function(result)
                        {
                            if(result)
                            {
                                var checkcount=0;  
                                var uname="",docname="";
                                var docTypeList=JSON.stringify(doctype);
                                var httpurl = "<%=request.getContextPath()%>/sa.elm.ob.utility.ad_forms.delegation.ajax/ApprovalDelegationAjax?inpAction=validateToUserDelegated";
                                                              
                                $.ajax({
                                    type: 'GET',
                                    url: httpurl,
                                    data: {
                                        inpFromDate:$('#inpFromDate').val(),
                                        inpToDate: $('#inpToDate').val(),
                                        inpFromUserId: $('#inpFromUserId').val(),
                                        inpHeaderId:$('#headerId').val(),
                                        inpDocList :docTypeList
                                    },
                                    dataType: 'json',
                                    async: false
                                  }).done(function (response) {
                                        var count=response.count;
                                        var fromdate=response.fromdate;
                                        var todate=response.todate;
                                        uname=response.uname;
                                        docname=response.docname;
                                        if(count ==1){
                                            checkcount=1;
                                        }
                                        else if(count==2){
                                            checkcount=2;
                                        }
                                        else{
                                            checkcount=0;
                                        }
                                  });
                                if(checkcount ==1){
                                    OBAlert("<%= Resource.getProperty("utility.approvaldelegation.selectedtouser", lang) %>"+" <b>"+uname+"</b> <%= Resource.getProperty("utility.approvaldelegation.notavailablefordel", lang) %>"+$('#inpFromDate').val()+" <%= Resource.getProperty("utility.to", lang) %>"+$('#inpToDate').val()+" ");
                                     return false;
                                }
                                else if(checkcount==2){
                                    OBAlert("<%= Resource.getProperty("utility.approvaldelegation.frmuseralreadydelegated", lang) %>"+ "<b>"+uname+"</b>"+" <%= Resource.getProperty("utility.approvaldelegation.forselectedtype", lang) %>"+" <b>"+docname+"</b>"+"<%= Resource.getProperty("utility.from", lang) %>"+$('#inpFromDate').val()+"<%= Resource.getProperty("utility.to", lang) %>"+$('#inpToDate').val()+" ");
                                    return false;
                                }
                                else
                                {
                                    /* $.getJSON(url, function(result){    
                                    }).done(function(data){
                                        var count=data.count;
                                        var fromdate=data.fromdate;
                                        var todate=data.todate;
                                        if (count==0)
                                            {        
                                            document.getElementById("inpDocList").value = JSON.stringify(doctype);          
                                            document.getElementById("inpSave").value="Save";
                                            document.getElementById("inpAction").value = "EditView";
                                            reloadWindow();
                                            }
                                        else 
                                        {
                                           OBAlert("<%= Resource.getProperty("utility.approvaldelegation.frmuseralreadydelegated", lang) %>"+$('#inpFromDate').val()+"<%= Resource.getProperty("utility.to", lang) %>"+$('#inpToDate').val()+" ");
                                        return false;
                                        }   
                                        
                                    }); */
                                
                                var docTypeList=JSON.stringify(doctype);
                                var httpurl = "<%=request.getContextPath()%>/sa.elm.ob.utility.ad_forms.delegation.ajax/ApprovalDelegationAjax?inpAction=validateUser";
                                    $.ajax({
                                            type: 'GET',
                                            url: httpurl,
                                            data: {
                                                inpFromDate:$('#inpFromDate').val(),
                                                inpToDate: $('#inpToDate').val(),
                                                inpFromUserId: $('#inpFromUserId').val(),
                                                inpFromRoleId: $('#inpFromRoleId').val(),
                                                inpHeaderId:$('#headerId').val(),
                                                inpDocList :docTypeList
                                            },
                                            dataType: 'json',
                                            async: false
                                          }).done(function (data) {
                                                var count=data.count;
                                                var fromdate=data.fromdate;
                                                var todate=data.todate;
                                                if (count==0)
                                                {
                                                    <%-- var url = "<%=request.getContextPath()%>/sa.elm.ob.utility.ad_forms.delegation.ajax/ApprovalDelegationAjax?inpAction=validateRecord&inpDelegatId="+$('#headerId').val();
                                                    $.getJSON(url, function(result){    
                                                    }).done(function(data){
                                                        var count=data.count;                                                      
                                                        if (count==1)
                                                            {
                                                            document.getElementById("inpDocList").value = JSON.stringify(doctype);          
                                                            document.getElementById("inpSave").value="Save";
                                                            document.getElementById("inpAction").value = "EditView";
                                                            reloadWindow();
                                                            }  
                                                        
                                                    }); --%>
                                                    document.getElementById("inpDocList").value = JSON.stringify(doctype);          
                                                    document.getElementById("inpSave").value="Save";
                                                    document.getElementById("inpAction").value = "EditView";
                                                    reloadWindow();
                                                } else 
                                                {
                                                    OBAlert("<%= Resource.getProperty("utility.approvaldelegation.notavailableforusr", lang) %>"+$('#inpFromDate').val()+" to:"+$('#inpToDate').val()+" ");
                                                    return false;
                                                   
                                                } 
                                           }); 
                                    }  
                                }
                        }); 
            }
        }
    }
}
$(document).ready(function (){
    var isAdminUser = "<%= (request.getAttribute("isAdminUser")==null?"":request.getAttribute("isAdminUser")) %>";
    if(isAdminUser=="true"){
        $("#inpFromUserId").combobox('enable');   
    }else{
        $("#inpFromUserId").combobox('disable'); 
    }
});

(function( $ ) {
    $.widget( "ui.combobox", {
        _create: function() {
            var self = this,select = this.element.hide(),selected = select.children( ":selected" ),value = selected.val() ? selected.text() : "";
            var input = this.input = $( "<input>" )
                .insertAfter( select )
                .val( value )
                .css('background-color', '#FFC')
                .autocomplete({
                        delay: 0,
                        minLength: 0,
                        source: function( request, response ) {
                            var matcher = new RegExp( $.ui.autocomplete.escapeRegex(request.term), "i" );
                            response( select.children( "option" ).map(function() {
                                var text = $( this ).text();                                
                                if ( this.value && ( !request.term || matcher.test(text) ) )
                                    return {
                                        label: text.replace(
                                                new RegExp("(?![^&;]+;)(?!<[^<>]*)(" +
                                                        $.ui.autocomplete.escapeRegex(request.term) +
                                                        ")(?![^<>]*>)(?![^&;]+;)", ""), 
                                                        "<strong>$1</strong>" ),
                                                        value: text,
                                                        option: this};
                                }) );
                            },
                        select: function( event, ui ) {
                            ui.item.option.selected = true;
                            self._trigger( "selected", event, {
                                item: ui.item.option
                                });                                                                         
                            if($(select).attr("id")=='inpFromUserId')
                            {
                                onChangeField();                                
                                getRoles(document.getElementById("inpFromUserId").value, 'h', '');                              
                            }
                            else if($(select).attr("id")=='inpToUserId'){
                                onChangeField();
                                setToUser(document.getElementById("inpToUserId").value);                                
                            }
                            else{
                                //jQuery("#DocList").setSelection($(select).parent().parent().get(0).id);
                                onChangeField();
                                var id=$(select).attr("id");
                                getRoles($("#"+id).val(), 'l', $(select).attr("name"));                                
                            }
                        }
                });            
            if($(select).attr("id")=='inpFromUserId') {
                var isAdminUser = "<%= (request.getAttribute("isAdminUser")==null?"":request.getAttribute("isAdminUser")) %>";
                if(isAdminUser=="true"){
                    input.addClass( "ui-widget autocomplete-widget-content ui-corner-left" );   
                }else{
                    input.addClass( "ui-widget ui-widget-content autocomplete-widget-content ui-corner-left" ); 
                }                
            }
            else if($(select).attr("id")=='inpToUserId')
                input.addClass( "ui-widget ui-widget-content autocomplete-widget-content ui-corner-left" );
            else
                input.addClass( "ui-widget ui-widget-content autocomplete-widget-content ui-corner-left autocomp_hght autocomp_wdth" );
            input.data( "autocomplete" )._renderItem = function( ul, item ) {
                return $( "<li></li>" )
                .data( "item.autocomplete", item )
                .append( "<a>" + item.label + "</a>" )
                .appendTo( ul );
            };
            input.click(function() {
                this.select();
            });
            this.div = $("<div class='divComboImage'></div>")
                    .insertAfter( input )
                    .append(  $( "<button type='button'>&nbsp;</button>" )                              
                        .css({'padding-top':"0px","height":"21px"}) 
                        .attr( "tabIndex", -1 )
                        .attr( "title", "Show All Items" )                      
                        .button({icons: {primary: "ui-icon-triangle-1-s"},text: false})
                        .removeClass( "ui-corner-all" )
                        .addClass( "ui-corner-right ui-button-icon" )
                        .addClass("ui_flip_image ")
                        .click(function() {
                            if ( input.autocomplete( "widget" ).is( ":visible" ) ) {
                                input.autocomplete( "close" );
                                return;
                            }
                            input.autocomplete( "search", "" );
                            input.focus();  
                            jQuery("#DocList").setSelection($(select).parent().parent().get(0).id);
                        }) 
                    )
                    .css({'float':'left'});
            },
            destroy: function() {
                this.input.remove();
                this.div.remove();
                this.element.show();
                $.Widget.prototype.destroy.call( this );
            },
            disable: function() {
                this.input.prop('disabled',true);
                this.input.autocomplete("disable");
                this.div.remove();
            },
            enable: function() {
                this.input.prop('disabled',false);
                this.input.autocomplete("enable");
                //this.div.button("enable");
                //this.a.button("enable");
            }
    });
})( jQuery );
$( "#inpFromUserId" ).combobox();
$( "#inpToUserId" ).combobox();

</script>
</HTML>