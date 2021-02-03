<%@page import="org.openbravo.base.secureApp.VariablesSecureApp"%>
<%@page import="org.codehaus.jettison.json.JSONObject, org.codehaus.jettison.json.JSONArray"%>
<%@page import="sa.elm.ob.utility.util.Utility"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
    <%@page import="sa.elm.ob.scm.properties.Resource" errorPage="/web/jsp/ErrorPage.jsp" %>
    <%@ page import="java.util.List,java.util.ArrayList" %>
    <%
    String lang = ((String)session.getAttribute("#AD_LANGUAGE"));
    JSONObject orgList = (JSONObject) request.getAttribute("OrgList");
    JSONObject org = (JSONObject) request.getAttribute("Organization");
    JSONObject prefix = (JSONObject) request.getAttribute("Prefix");
    
    JSONArray orgArray=orgList.getJSONArray("data");
    
    String style="../web/skins/ltr/org.openbravo.userinterface.skin.250to300Comp/250to300Comp/Openbravo_ERP.css";
    String toolBarStyle="../web/js/common/CommonFormLtr.css";
    if(lang.equals("ar_SA")){
     style="../web/skins/rtl/org.openbravo.userinterface.skin.250to300Comp/250to300Comp/Openbravo_ERP_250.css";
    toolBarStyle="../web/js/common/CommonFormRtl.css";
    }
    %>
<fmt:setLocale value="<%=new VariablesSecureApp(request).getLanguage()%>"/> 
<fmt:setBundle basename="sa.elm.ob.utility.properties.applicationresources"/>

<HTML xmlns="http://www.w3.org/1999/xhtml">
<HEAD>
<TITLE><%= Resource.getProperty("scm.updatesequence.title",lang) %></TITLE>
<META http-equiv="Content-Type" content="text/html; charset=utf-8"></META> 
<LINK rel="stylesheet" type="text/css" href="<%=style %>" id="paramCSS"></LINK>
<link rel="stylesheet" type="text/css" href="../web/js/jquery/jquery-ui-1.8.11.custom.css"></link>
<link rel="stylesheet" type="text/css" href="<%=toolBarStyle %>"></link>     
 <link rel="stylesheet" type="text/css" href="../web/js/common/select2.min.css"></link>
   
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
    /*<<<<<OB Code>>>>>*/
    var jsonObj = <%= request.getAttribute("result")%>;
    var status = (jsonObj!=null ? jsonObj.severity : "");
    var statusMessage = (jsonObj!=null ? jsonObj.text : "");
    if(status!="" && status!=null){
        if("success"==status)
            displayMessage("S", "Success",statusMessage);
        else if("error"==status){            
            displayMessage("E", "Error",statusMessage);
        }
    } 
}
function onResizeDo() {
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
         <INPUT type="hidden" name="inpOrgid" id="inpOrgid" value=""></INPUT>         
        
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
                              
                           <td id="Button">
								<div style="text-align: right;">
									<div style="position: relative; top: 10px; right: 20px;">
									<button onclick="updateSequence();" class="ButtonLink" id="Submit_linkBTN" type="button" >
						  			<table class="Button">
					      			<tbody><tr>
					        			<td class="Button_left"><img border="0" src="../web/images/blank.gif" title="Done" alt="Done" class="Button_Icon Button_Icon_process"></td>
					        			<td id="Submit_BTNname" class="Button_text"><%= Resource.getProperty("scm.updatesequence.done",lang) %></td>
					        			<td class="Button_right"><img border="0" src="../web/images/blank.gif" title="Done" alt="Done" class="Button_Icon Button_Icon_process"></td>
					      			</tr></tbody></table>
					  				</button>
									&nbsp;&nbsp;&nbsp;			  				
									</div>
				  				</div>
							</td>
                           <td class="Main_ToolBar_Space" style="display: none;"></td>
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
                                       <%= Resource.getProperty("scm.updatesequence.title",lang) %>
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
                                          <div style="color:#000000;font-family: sans-serif;font-size:14px;margin-top:10px;text-align: center;"><%= Resource.getProperty("scm.loading",lang).replace("'", "\\\'") %> </div>
                                       </div>
                                       <TABLE  align="center" width="100%">
                                          <TR>
                                             <TD>&nbsp;</TD>
                                          </TR>
                                          <tr>
                                             <td class="TitleCell"><span class="LabelText" id="inpOrgIdLabel"> <b> <%=Resource.getProperty("scm.organization", lang).replace("'", "\\\'")%>
                                                </b>
                                                </span>
                                             </td>
                                             <td class="TextBox_ContentCell">
                                                <select id="inpOrgId" class="ComboKey Combo_TwoCells_width">
                                                  <option value="0"><span>--select--</span></option>
                                                   <%   if(orgArray.length() > 0){
                                                	   	for (int i = 0, size = orgArray.length(); i < size; i++){
                                                		   JSONObject objectInArray = orgArray.getJSONObject(i);%>
                                                   	<option value='<%= objectInArray.get("OrgId")%>' ><span><%= objectInArray.get("OrgName")%></span></option>
                                                   <%}}%>
                                                   
                                                </select>
                                             </td>
                                          </tr>  
                                          <tr>
                                           <td class="TitleCell"><span class="LabelText" id="inpPrefixLabel"> <b> <%=Resource.getProperty("scm.updatesequence.prefix", lang).replace("'", "\\\'")%></b></span> </td>																			
											<td><INPUT type="text" style="margin-left: 2px;margin-top: 5px;" id="inpPrefix" name="inpPrefix" class="dojoValidateValid required TextBox_TwoCells_width" required="true" onkeypress="return charactersOnKeyPress(this, event);" value="<%=prefix.get("Prefix")%>"></INPUT></td>
                                          </tr>                               
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
 <script type="text/javascript" src="../web/js/common/select2.min.js"></script> 
 
<script type="text/javascript">

function reloadWindow() {
    submitCommandForm('DEFAULT', false, null, 'UpdateSequence', '_self', null, false);
    return false;
}
function onClickRefresh() {
    document.getElementById("inpAction").value = "";
    reloadWindow();
}
function updateSequence(){
	if($("#inpOrgId").val() == 0 || $("#inpPrefix").val()==''){
		OBAlert('<%=Resource.getProperty("scm.updatesequence.organdprefixcannotbeempty",lang).replace("'","\\\'") %>');
        return false;
	}
	
	document.getElementById("inpOrgid").value = $("#inpOrgId").val();
	document.getElementById("inpAction").value = "Submit"; 
	reloadWindow();    
}
function charactersOnKeyPress(index, evt)
{
	document.getElementById("messageBoxID").style.display = "none";		
	var charCode = (evt.which) ? evt.which : evt.keyCode;			
	if(charCode == 8 || charCode == 9 || charCode == 35 || charCode == 36 || charCode == 37 || charCode == 39 || charCode == 46 || charCode == 127 || ( charCode >= 48 && charCode <= 57 ))
   	{			
		var character = String.fromCharCode(charCode);			
		if(evt.which!="0" && (character=="#" || character=="$" || character=="%" || character=="'" || character=="."))
   			return false;			
		if(evt.which=="0" && charCode == 46)
   			return true;
		if(charCode == 46 && index.value.indexOf(".")!=-1)
			return false;
		return true;
   	}
   	return false;
}
(function( $ ) {
    $.widget( "ui.combobox", {
        _create: function() {
            var self = this,select = this.element.hide(),selected = select.children( ":selected" ),value = selected.val() ? selected.text() : "";
            var input = this.input = $( "<input  placeholder=\"<%= Resource.getProperty("scm.select", lang) %>\">" )
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
                        	
                            if($(select).attr("id")=='inpOrgId')
                            {       
                                // Selected an item, nothing to do
                                if ( ui.item ) {
                                    return;
                                }

                                var typedValue = $('#inpOrgId').next().val();
                                // Search for a match (case-insensitive)
                                var value = typedValue,
                                    valueLowerCase = value.toLowerCase(),
                                    valid = false;
                                
                                $("#inpOrgId > option").each(function() {
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
                                $('#inpOrgId').next().val( "" );
                            }                     
                        },
                         search: function( event, ui ) { 
                            if($(select).attr("id")=='inpOrgId'  )
                            {           
                                if($("#inpOrgId").next().val()=="")
                                    $("#inpOrgId").val("");
                            } 
                      	}                     
                });
            if($(select).attr("id")=='inpOrgId')                
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
$( "#inpOrgId").select2(selectBoxAjaxPaging({size: "small", placeholder: true}));
</script>
</HTML>