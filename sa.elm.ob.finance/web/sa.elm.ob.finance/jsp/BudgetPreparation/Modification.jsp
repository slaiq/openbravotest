<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List,java.util.ArrayList,sa.elm.ob.finance.properties.Resource" errorPage="/web/jsp/ErrorPage.jsp" %>
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
    <TITLE><%= Resource.getProperty("finance.modification",lang) %></TITLE>   
    <LINK rel="stylesheet" type="text/css" href="<%=style %>" id="paramCSS"></LINK>  
    <link rel="stylesheet" type="text/css" href="../web/js/jquery/jquery-ui-1.8.11.custom.css" />
    <script type="text/javascript" src="../web/js/common/common.js"></script>
    <script type="text/javascript">
    </script>
</HEAD>
<BODY leftmargin="0" topmargin="0" marginwidth="0" marginheight="0" onload="" onresize="" id="paramBody">
<FORM name="frmMain" method="post" action="">
    <INPUT type="hidden" name="Command"></INPUT>
    <INPUT type="hidden" name="act" id="act"  value=""></INPUT>
     <INPUT type="hidden" name="inpBudgetPrepartationId" id="inpBudgetPrepartationId"  value="<%= request.getAttribute("inpBudgetPrepartationId")%>"></INPUT>
    <DIV class="Popup_ContentPane_CircleLogo"><DIV class="Popup_WindowLogo"><IMG class="Popup_WindowLogo_Icon Popup_WindowLogo_Icon_attribute" src="../web/images/blank.gif" border="0/"></IMG></DIV></DIV>
    <TABLE cellspacing="0" cellpadding="0" width="100%" id="table_header">
    <TR>
    <TD>
        <TABLE cellspacing="0" cellpadding="0" class="Popup_ContentPane_NavBar" id="tdToolBar">
        <TR class="Popup_NavBar_bg"><TD></TD>
        <TD class="Popup_NavBar_separator_cell"></TD>
        <TD class="Popup_NavBar_Popup_title_cell"><SPAN><%= Resource.getProperty("finance.modification",lang) %></SPAN></TD>
        <TD class="Popup_NavBar_separator_cell"></TD>
        </TR>
        </TABLE>
    </TD>
    </TR>
    <TR><TD><TABLE cellspacing="0" cellpadding="0" class="Popup_ContentPane_SeparatorBar" id="tdtopTabs"><TR><TD class="Popup_SeparatorBar_bg"></TD></TR></TABLE></TD></TR>
    </TABLE>
    <TABLE cellspacing="0" cellpadding="0" width="100%">
    <TR><TD>
        <DIV class="Popup_ContentPane_Client" style="overflow: auto;" id="client"><br/>
        <div id="LoadingContent" style="position:absolute;top:40%;left:48%;text-align: center;">
             <!--   <div><img alt="Loading" src="../web/images/loading.gif"></div> 
             <div style="color:#000000;font-family: sans-serif;font-size:14px;margin-top: 15px;">Loading...</div> -->
        </div>
        <div align="center" style="margin-top: 15px;">
            <table>
                <tr>
                    <td>&nbsp;&nbsp;&nbsp;</td>
                    <td class="Combo_ContentCell">
                      <select id="inpType" name="inpType" style="width:100%;" class="ComboKey Combo_TwoCells_width" onchange="">
                         <option value='I'><%= Resource.getProperty("finance.increase",lang) %></option>
                          <option value='D'><%= Resource.getProperty("finance.decrease",lang) %></option>
                      </select>
                    </td>
                    <td>  </td>
                    <td style="font-size:90%;"><%= Resource.getProperty("finance.of",lang) %></td>
                    <td> </td>
                    <td><input type="text" id="inpPercentage"  name="inpPercentage"  maxlength="5" value='0' required="true" class="dojoValidateValid required TextBox_OneCell_width" onkeydown="stopRKey();" onkeyup="stopRKey();" onblur="" onchange=""></input></td>
                    <td style="font-size:90%;"><%= Resource.getProperty("finance.on",lang) %></td>
                    <td class="Combo_ContentCell">
                      <select id="inpYears" name="inpYears"   style="width:98%;"  class="ComboKey Combo_TwoCells_width" onchange="">
                         <option value='LB'><%= Resource.getProperty("finance.budget.lastyearbudget",lang) %></option>
                          <option value='Avg2'><%= Resource.getProperty("finance.twoyearverage",lang) %></option>
                            <option value='Avg3'><%= Resource.getProperty("finance.threeyearaverage",lang) %></option>
                             <option value='LYFA'><%= Resource.getProperty("finance.lastyrfa",lang) %></option>
                             <option value='TYFA'><%= Resource.getProperty("finance.twoyrfaavg",lang) %></option>
                              <option value='THEFA'><%= Resource.getProperty("finance.threeyrfaavg",lang) %></option>
                             <option value='LYA'><%= Resource.getProperty("finance.lastyractual",lang) %></option>
                             <option value='TYAA'><%= Resource.getProperty("finance.twoyractualavg",lang) %></option>
                             <option value='THEYAA'><%= Resource.getProperty("finance.threeyractualavg",lang) %></option>
                             
                      </select>
                    </td>
                    
                </tr>
            </table>
        </div>
        <br>
        </br>
        <div align="center">
        <BUTTON type="button" id="SubmitButton" class="ButtonLink" onclick="Process();">
        <TABLE class="Button">
             <TR>
             <TD class="Button_left"><IMG  class="Button_Icon Button_Icon_process" src="../web/images/blank.gif" border="0"></IMG> </TD>
             <TD class="Button_text"><%= Resource.getProperty("finance.ok",lang) %></TD>
             <TD class="Button_right"></TD>
             </TR>
             </TABLE>
         </BUTTON>
         <BUTTON type="button" id="CloseButton" class="ButtonLink" onclick="parent.close();">
         <TABLE class="Button">
           <TR>
           <TD class="Button_left"><IMG class="Button_Icon Button_Icon_process" src="../web/images/blank.gif" border="0"></IMG> </TD>
           <TD class="Button_text"><%= Resource.getProperty("finance.cancel",lang) %></TD>
           <TD class="Button_right"></TD>
           </TR>
           </TABLE>
                    </BUTTON>
        </div>
        </DIV>
    </TD></TR>
    </TABLE>
 </FORM>
</BODY>
<script type="text/javascript">
function Process(){
	if(document.getElementById("inpPercentage").value > 99.99 ){
		OBAlert('<%= Resource.getProperty("finance.budgetpreparation.modification.allow.greater.99",lang) %>');
	        return false;
	}
	if(document.getElementById("inpPercentage").value < 0 ){
        OBAlert('<%= Resource.getProperty("finance.budgetpreparation.modification.allow.less.zero",lang) %>');
           return false;
   }
	var percentage= document.getElementById("inpPercentage").value;
	if(percentage.indexOf('.') == 1){
		var decNum = percentage.substring(percentage.indexOf('.')+1, percentage.length);
		if (decNum.length > 2){
			 OBAlert('<%= Resource.getProperty("finance.budgetpreparation.modification.allow.two.decimals",lang) %>');
	           return false;
		}
	}
	 document.getElementById("act").value = "Modification";
	 document.frmMain.submit();
}


	window.addEventListener('keydown', function(e) {
		if (e.keyIdentifier == 'U+000A' || e.keyIdentifier == 'Enter'
				|| e.keyCode == 13) {
			if (e.target.nodeName == 'INPUT' && e.target.type == 'text') {
				e.preventDefault();
				return false;
			}
		}
	}, true);
</script>
</HTML>