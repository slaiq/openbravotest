<%@page import="sa.elm.ob.utility.util.Utility"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@page import="sa.elm.ob.finance.properties.Resource"
    errorPage="/web/jsp/ErrorPage.jsp"%>
<%@ page import="java.util.List,java.util.ArrayList"%>
<%
    String lang = ((String) session.getAttribute("#AD_LANGUAGE"))
            .toString();
    String style = "../web/skins/ltr/org.openbravo.userinterface.skin.250to300Comp/250to300Comp/Openbravo_ERP.css";
    String toolBarStyle = "../web/js/common/CommonFormLtr.css";
    if (lang.equals("ar_SA")) {
        style = "../web/skins/rtl/org.openbravo.userinterface.skin.250to300Comp/250to300Comp/Openbravo_ERP_250.css";
        toolBarStyle = "../web/js/common/CommonFormRtl.css";
    }
%>
<HTML xmlns="http://www.w3.org/1999/xhtml">
<HEAD>

<TITLE><%=Resource.getProperty("finance.glreport.title", lang)%></TITLE>
<META http-equiv="Content-Type" content="text/html; charset=utf-8"></META>
<LINK rel="stylesheet" type="text/css" href="<%=style%>" id="paramCSS"></LINK>
<link rel="stylesheet" type="text/css"
    href="../web/js/jquery/jquery-ui-1.8.11.custom.css"></link>
    <link rel="stylesheet"  href="../web/js/jquery/css/demos.css"></link>
    <link rel="stylesheet" type="text/css" href="../web/js/themes/base/jquery.ui.autocomplete.css"></link>
<link rel="stylesheet" type="text/css" href="<%=toolBarStyle%>"></link>

<link rel="stylesheet"  href="../web/js/jquery/css/demos.css">
    <link rel="stylesheet" type="text/css" href="../web/js/themes/base/jquery.ui.autocomplete.css"></link>
    <script type="text/javascript" src="../web/js/common/common.js"></script>
    <script type="text/javascript" src="../web/js/utils.js"></script>
    <script type="text/javascript" src="../web/js/windowKeyboard.js"></script>
    <SCRIPT language="JavaScript" src="../web/../org.openbravo.client.kernel/OBCLKER_Kernel/StaticResources" type="text/javascript"></SCRIPT>
    <script type="text/javascript" id="paramDirectory">var baseDirectory = "../web/";</script>
    <script type="text/javascript" id="paramLanguage">var defaultLang="en_US";</script>
    <script type="text/javascript" src="../web/js/default/DateTextBox.js"></script>
    <script type="text/javascript" src="../web/js/jscalendar/calendar.js"></script>
    <script type="text/javascript" src="../web/js/jscalendar/lang/calendar-lang.js"></script>
<style type="text/css">

.ui-button-icon-primary {
    left: .9em !important;
}

ul {
    border: 1px solid #FF9C30 !important;
}
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

<BODY onresize="onResizeDo();" leftmargin="0" topmargin="0"
    marginwidth="0" marginheight="0" id="paramBody">
    <FORM id="form" method="post" action="" name="frmMain">
        <INPUT type="hidden" name="Command"></INPUT> <INPUT type="hidden"
            name="IsPopUpCall" value="1"></INPUT> <INPUT type="hidden"
            name="inpLastFieldChanged"></INPUT> <INPUT type="hidden"
            name="inpAction" id="inpAction" value=""></INPUT> <INPUT
            type="hidden" name="inphDoctypelist" id="inphDoctypelist" value=""></INPUT>

        <TABLE height="100%" border="0" cellpadding="0" cellspacing="0"
            id="main">
            <TR>

                <TD valign="top">
                    <DIV class="Main_ContentPane_NavBar" id="tdtopButtons">
                        <TABLE class="Main_ContentPane_NavBar" id="tdtopNavButtons"></TABLE>
                    </DIV>
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
                    <TABLE width="100%" border="0" cellspacing="0" cellpadding="0"
                        class="Main_ContentPane_TabPane" id="tdtopTabs">
                        <TR id="paramParentTabContainer">
                            <td class="tabBackGroundInit"></td>
                        </TR>
                        <TR id="paramMainTabContainer">
                            <td class="tabBackGroundInit"><span
                                style="background: none;" class="tabTitle"></span></td>
                        </TR>
                        <tr>
                            <td class="tabBackGround">
                                <div class="marginLeft">
                                    <div>
                                        <span class="dojoTabcurrentfirst">
                                            <div>
                                                <span><a class="dojoTabLink"
                                                    href="javascript:void(0);" onclick="return false;"
                                                    onMouseOver="return true;" onMouseOut="return true;"> <%=Resource.getProperty("finance.manualreport.title", lang)
                    .replace("'", "\\\'")%>
                                                </a></span>
                                            </div>
                                        </span>
                                    </div>
                                </div>
                            </td>
                        </tr>
                    </TABLE>
                    <DIV class="Main_ContentPane_Client" style="overflow: auto;"
                        id="client">
                        <TABLE class="dojoTabContainer" border="0" cellpadding="0"
                            cellspacing="0" width="100%">
                            <TR>
                                <TD>
                                    <TABLE class="dojoTabPaneWrapper" align="right" border="0"
                                        cellpadding="0" cellspacing="0" width="100%" summary="">
                                        <TR>
                                            <TD>
                                                <TABLE cellpadding="0" cellspacing="0" id="messageBoxID"
                                                    class="MessageBox">
                                                    <TBODY>
                                                        <TR class="MessageBox_TopMargin" align="right">
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
                                                                                            <TD><SPAN>
                                                                                                    <TABLE cellpadding="0" cellspacing="0"
                                                                                                        class="MessageBox_Body_ContentCell">
                                                                                                        <TBODY>
                                                                                                            <TR>
                                                                                                                <TD class="MessageBox_Icon_ContentCell">
                                                                                                                    <DIV class="MessageBox_Icon"></DIV>
                                                                                                                </TD>
                                                                                                                <TD style="vertical-align: top;"
                                                                                                                    id="messageBoxIDContent"><SPAN>
                                                                                                                        <DIV class="MessageBox_TextTitle"
                                                                                                                            id="messageBoxIDTitle"></DIV>
                                                                                                                        <DIV class="MessageBox_TextDescription"
                                                                                                                            id="messageBoxIDMessage"></DIV>
                                                                                                                        <DIV class="MessageBox_TextSeparator"></DIV>
                                                                                                                </SPAN></TD>
                                                                                                            </TR>
                                                                                                        </TBODY>
                                                                                                    </TABLE>
                                                                                            </SPAN>
                                                                                                <div id="hideMessage"
                                                                                                    style="float: right; margin-top: -13px; margin-right: 10px;">
                                                                                                    <a style="color: yellow; font-size: 11.5px"
                                                                                                        href="javascript:void(0);"
                                                                                                        onClick="document.getElementById('messageBoxID').style.display = 'none';"><%=Resource.getProperty("finance.hide", lang)%></a>
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
                                                <div id="LoadingContent" style="display: none;">
                                                    <div style="text-align: center; margin-top: 17%;">
                                                        <img alt="Loading" src="../web/images/loading.gif">
                                                    </div>
                                                    <div
                                                        style="color: #000000; font-family: sans-serif; font-size: 14px; margin-top: 10px; text-align: center;"><%=Resource.getProperty("finance.loading", lang).replace(
                    "'", "\\\'")%>
                                                    </div>
                                                </div>
                                                <TABLE align="center" width="100%">



                                                    <TR>
                                                        <TD>&nbsp;</TD>
                                                    </TR>
                                                    
                                                    
                                                    <tr>
                                                      <td class="TitleCell"><span name="inpOrgLabel"
                                                          class="LabelText" id="inpOrgLabel"> <b> <%=Resource.getProperty("finance.organization", lang)
                                                         .replace("'", "\\\'")%>
                                                         </b>
                                                        </span></td>
                                                     <td class="TextBox_ContentCell"><select id="inpOrg" class="ComboKey Combo_TwoCells_width" onchange="setDocNoBlank();generateDocumentNo();setDocTypeBlank();">
                                                     </select></td>
                                                    </tr>
                                                    
                                                    
                                                    
                                                    <TR>
                                                        <TD class="TitleCell"><span name="formtlabel"
                                                            class="LabelText" id="formtlabel"> <%=Resource.getProperty("finance.manual.field.label", lang)
                    .replace("'", "\\\'")%>

                                                        </span></TD>
                                                        <TD class="TextBox_ContentCell"><SELECT
                                                            name="Doctypelist" class="ComboKey Combo_TwoCells_width"
                                                            id="inpDoctypelist" onchange="generateDocumentNo(this.value);setDocNoBlank();">
                                                                 <option value="0">--Select--</option>
                                                                <option value="gi">G/L Journal</option>
                                                                <option value="apa">AP Prepayment Application</option>
                                                                <option value="recon">Reconciliation</option>
                                                                <option value="prje">Payment Reverse Journal Entries</option>
                                                                <option value="urje">Un Reconcile Journal Entries</option>
                                                                <option value="AR">AR Receipt</option>
                                                                    
                                                        </SELECT></TD>
                                                    </TR>
                                                    
                                                    <TR>
                                                        <TD class="TitleCell" valign="middle" nowrap><span name="formtlabel"
                                                            class="LabelText" id="formtlabel"> <%=Resource
                    .getProperty("finance.manualno.field.label", lang).replace(
                            "'", "\\\'")%>

                                                        </span></TD>
                                                 <TD class="Combo_ContentCell" valign="middle">
                                                        <SELECT name="inpRequestNo" id="inpRequestNo">
                                                             <option value="0" selected>--Select--</option>
                                                        </SELECT>
                                                        </TD> 
                                                    </TR>


                                                    <TR>
                                                        <TD class="TitleCell"><span name="formtlabel"
                                                            class="LabelText" id="formtlabel"> <%=Resource
                    .getProperty("finance.glreport.field.label", lang).replace(
                            "'", "\\\'")%>

                                                        </span></TD>


                                                        <TD colspan=2 class="TextBox_ContentCell"><SELECT
                                                            name="formattype" class="ComboKey Combo_TwoCells_width"
                                                            id="formattype">


                                                                <option value="xls"><%=Resource.getProperty(
                    "finance.budgetdetailreport.button.generateexcel", lang)%></option>
                                                                <option value="pdf"><%=Resource.getProperty(
                    "finance.budgetdetailreport.button.generatepdf", lang)%></option>

                                                        </SELECT></TD>
                                                    </TR>


                                                    <TR align="center">
                                                        <td></td>
                                                        <TD id="submitButton">


                                                            <BUTTON type="button" id="Submit_linkBTN"
                                                                class="ButtonLink" onclick="generateReport();"
                                                                style="margin: 10px 0;">
                                                                <TABLE class="Button">
                                                                    <TR>
                                                                        <TD class="Button_left"><IMG
                                                                            class="Button_Icon Button_Icon_process" alt="Submit"
                                                                            title="Submit" src="../web/images/blank.gif"
                                                                            border="0"></IMG></TD>
                                                                        <TD class="Button_text" id="Submit_BTNname"><%=Resource.getProperty(
                    "finance.glreport.button.generatereport", lang).replace(
                    "'", "\\\'")%></TD>
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
            </TR>
        </TABLE>
    </FORM>
</BODY>
<script type="text/javascript">onLoadDo();</script>
<script type="text/javascript" src="../web/js/jquery-1.7.2.min.js"></script>
<script type="text/javascript" src="../web/js/ui/jquery.ui.core.js"></script>
<script type="text/javascript" src="../web/js/jquery/grid.locale-en.js"></script>
<script type="text/javascript" src="../web/js/jquery/jquery.jqGrid.min.js"></script>
<script type="text/javascript" src="../web/js/ui/jquery.ui.datepicker.js"></script>
<script type="text/javascript" src="../web/js/jquery/datejs/jquery-ui.min.js"></script>
<script type="text/javascript" src="../web/js/jquery/datejs/jquery-dateplustimepicker.min.js"></script>
<script src="../web/js/ui/jquery.ui.widget.js"></script>
<script src="../web/js/ui/jquery.ui.mouse.js"></script>
<script src="../web/js/ui/jquery.ui.button.js"></script>
<script src="../web/js/ui/jquery.ui.draggable.js"></script>
<script src="../web/js/ui/jquery.ui.position.js"></script>
<script src="../web/js/ui/jquery.ui.autocomplete.js"></script>
<script src="../web/js/ui/jquery.ui.resizable.js"></script>
<script src="../web/js/ui/jquery.ui.dialog.js"></script> 
<script type="text/javascript">
var allowPrint = false;
//load Organizations based on client and orgtype( legal with accounting ) 
var url = "<%=request.getContextPath()%>/GLAdjustmentReportAjax?action=getOrganization";
$.getJSON(url, function(result){
    
}).done(function(data){

    $("#inpOrg")
        .find("option")
        .remove()
        .end();
    
    var select = "<%=Resource.getProperty("finance.select", lang)%>";
    $("#inpOrg").append("<option value='0' selected>"+"--"+select+"--"+"</option>");
    $.each( data, function( i, item ) {
        $("#inpOrg").append("<option value='"+item.id+"'>"+item.orgname+"</option>");
      });
});


    function reloadWindow() 
    {
        submitCommandForm('DEFAULT', false, null, 'ManualAdjustmentReport', '_self', null, false); 
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
        document.getElementById("inpAction").value=$("#formattype").val();
        var typedVal = $("#inpRequestNo").next().val();
        var selectedVal = document.getElementById("inpRequestNo").value;
            
           if(document.getElementById("inpOrg").value=="0")
                {
                 /*alert("Please select Organization");*/
                 OBConfirm('<%=Resource.getProperty(
                    "finance.gladjustmentreport.alert.select.organization", lang)
                    .replace("'", "\\\'")%>'); 
                    //  OBAlert("Please select Organization");
                      return false;
                }
           else if(document.getElementById("inpDoctypelist").value=="0")
                {
                /* alert("Please select Document Type"); */
                  OBConfirm('<%=Resource.getProperty(
                    "finance.gladjustmentreport.alert.select.documenttype", lang)
                    .replace("'", "\\\'")%>');
                       /*  OBAlert("Please select Document Type"); */
                        return false;
                    
               }
           else if(typedVal==='')
                {
                /* alert ("Please select valid Document Number"); */
                OBConfirm('<%=Resource.getProperty(
                        "finance.gladjustmentreport.alert.select.validdocumentno", lang)
                        .replace("'", "\\\'")%>'); 
                         return false;
                }
           else if(selectedVal==='')
              {
                /* alert ("Please select valid Document Number"); */
                OBConfirm('<%=Resource.getProperty(
                        "finance.gladjustmentreport.alert.select.validdocumentno", lang)
                        .replace("'", "\\\'")%>'); 
                         return false;              
              }
          else if(typedVal!=selectedVal)
             {
                /* alert ("Please select valid Document Number"); */
                OBConfirm('<%=Resource.getProperty(
                        "finance.gladjustmentreport.alert.select.validdocumentno", lang)
                        .replace("'", "\\\'")%>'); 
                         return false;          
            }
           else
              {
               <%--  document.forms['frmMain'].action= "<%=request.getContextPath() %>/sa.elm.ob.finance.ad_reports.Manualadjustment.header/ManualAdjustmentReport?inpAction="+inpAction+"&inphDoctypelist="+doctype+"&inpRequestNo="+requestNo;
                document.forms['frmMain'].submit();  --%>
                /*  submitCommandForm('DEFAULT', false, null, 'ManualAdjustmentReport', '_self', null, false);  */ 
                openServletNewWindow('DEFAULT', true,null, 'ManualAdjustmentReport', null, false, '700', '1000', true);
                return false;
              }
    }
     // set Document No. field null if change the Organization or Document Type 
     function setDocNoBlank(){
    $("#inpRequestNo")
            .find("option")
            .remove()
            .end();
      $("#inpRequestNo").val("0"); 
      $("#inpRequestNo").next().val($("#inpRequestNo :selected").text());
     }
     
    // set Document Type field null if change the Organization 
    function setDocTypeBlank(){
        $("#inpDoctypelist").val("0"); 
        $("#inpDoctypelist").next().val($("#inpDoctypelist :selected").text());
    }
    
    function generateDocumentNo(inphDoctypelist){
        //load DocumentNos based on DocType,org,client,and posted 
        var url = "<%=request.getContextPath()%>/GLAdjustmentReportAjax?action=getDocumentNo&inphDoctypelist="+inphDoctypelist;
        url +="&orgId="+document.getElementById("inpOrg").value;
        $.getJSON(url, function(result){
            
        }).done(function(data){
            $("#inpRequestNo")
            .find("option")
            .remove()
            .end();
            var select = "<%=Resource.getProperty("finance.select", lang)%>";
            $.each( data, function( i, item ) {
                $("#inpRequestNo").append("<option value='"+item.DocumentNo+"'>"+item.DocumentNo+"</option>");
                
              });
        });
    }
 
    // hide/prevent  enter key from keyboard (while generate report)
    $("#Submit_BTNname").keypress(function(e){
           if(e.keyCode === 13){         // if user is hitting enter 
               e.preventDefault();
               return false;
           }
        });
    
    (function( $ ) {
        $.widget( "ui.combobox", {
            _create: function() {
                var self = this,select = this.element.hide(),selected = select.children( ":selected" ),value = selected.val() ? selected.text() : "";
                var input = this.input = $( "<input  placeholder=\"<%= Resource.getProperty("finance.select", lang) %>\">" )
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
                            },
                            change: function( event, ui ) {
                                if($(select).attr("id")=='inpRequestNo')
                                {       
                                    // Selected an item, nothing to do
                                    if ( ui.item ) {
                                        return;
                                    }

                                    var typedValue = $('#inpRequestNo').next().val();
                                    // Search for a match (case-insensitive)
                                    var value = typedValue,
                                        valueLowerCase = value.toLowerCase(),
                                        valid = false;
                                    
                                    $("#inpRequestNo > option").each(function() {
                                        if ( this.text.toLowerCase() === valueLowerCase ) {
                                            valid = true;
                                            allowPrint = true;
                                            return false;
                                        }
                                        else 
                                            allowPrint = false;
                                    });

                                    // Found a match, nothing to do
                                    if ( valid ) {
                                        return;
                                    }
                                    $('#inpRequestNo').next().val( "" );
                                }   
                                
                            },
                             search: function( event, ui ) {
                                    if($(select).attr("id")=='inpRequestNo')
                                    {           
                                        if($("#inpRequestNo").next().val()=="")
                                            $("#inpRequestNo").val("");
                                    }   
                        
                          }  
                 
                            
                    });
                if($(select).attr("id")=='inpRequestNo')                
                    input.addClass( "ui-widget autocomplete-widget-content ui-corner-left" );       
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
                }
            
        });
        
        
    })( jQuery );
                $( "#inpRequestNo" ).combobox();

    </script>
