 <%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
 <%@page import="sa.elm.ob.utility.util.Utility"%>
 <%@page import="sa.elm.ob.scm.ad_reports.InvitationLetterReport.InvitationLetterReport"%>
 <%@ page import="sa.elm.ob.utility.properties.Resource" errorPage="/web/jsp/ErrorPage.jsp"%>
 <%@ page import="java.util.List,java.util.ArrayList" errorPage="/web/jsp/ErrorPage.jsp" %>
 <%     
    String lang = ((String)session.getAttribute("#AD_LANGUAGE"));
     String style="../web/skins/ltr/org.openbravo.userinterface.skin.250to300Comp/250to300Comp/Openbravo_ERP.css";
     String toolBarStyle="../web/js/common/CommonFormLtr.css";
     if(lang.equals("ar_SA")){
         style="../web/skins/rtl/org.openbravo.userinterface.skin.250to300Comp/250to300Comp/Openbravo_ERP_250.css";
     }

 %>
<HTML>
<HEAD>
 <META http-equiv="Content-Type" content="text/html; charset=utf-8"></META>
    <TITLE><%=Resource.getProperty("utility.req.response.moreinfo",lang)%></TITLE>
    <LINK rel="stylesheet" type="text/css" href="<%=style %>" id="paramCSS"></LINK> 
    <link rel="stylesheet" type="text/css" href="<%=toolBarStyle %>"></link>  
      <link rel="stylesheet" type="text/css" href="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.picker.css"></link>
    <link rel="stylesheet" type="text/css" href="../web/js/jquery/jquery-ui-1.8.11.custom.css"></link>
    <link rel="stylesheet" type="text/css" href="../web/js/jquery/ui.jqgrid.css"></link>
    <link rel="stylesheet" type="text/css" href="../web/js/common/select2.min.css"></link>
    
    <script type="text/javascript" id="paramDirectory">var baseDirectory = "../web/";</script> 
    <script type="text/javascript" id="paramLanguage">var defaultLang="en_US";</script>
        <script type="text/javascript" src="../web/js/common/common.js"></script>
    <script type="text/javascript" src="../web/js/utils.js"></script>
    <script type="text/javascript" src="../web/js/windowKeyboard.js"></script>
    <script type="text/javascript" src="../web/js/searchs.js"></script>
    <script type="text/javascript" id="paramDirectory">var baseDirectory = "../web/";</script>
    <script type="text/javascript" id="paramLanguage">var defaultLang="en_US";</script>
    <script type="text/javascript" src="../web/js/jquery-1.7.2.min.js"></script>
    <script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.min_1.11.0.js"></script> 
    <script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.plugin.js"></script> 
    <script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.js"></script> 
    <script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.ummalqura.js"></script> 
    <script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.ummalqura-ar.js"></script> 
    <script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.plus.js"></script> 
    <script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.picker.js"></script> 
    <script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.picker.ext.js"></script>


     
    <style type="text/css">
    #client
    {
        height:auto !important;
    }
    </style>
 <script type="text/javascript">
 var contextpath= '<%=request.getContextPath()%>';

function onLoadDo() {
    /*<<<<<OB Code>>>>>*/
    this.windowTables = new Array(new windowTableId('client', 'buttonOK'));
    setWindowTableParentElement();
    this.tabsTables = new Array(new tabTableId('tdtopTabs'));
    setTabTableParentElement();
    setBrowserAutoComplete(false);
    setFocusFirstControl(null, 'inpMailTemplate');
    updateMenuIcon('buttonMenu');
    /*<<<<<OB Code>>>>>*/
    
    var reqval = document.getElementById("inprequest").value;
    if(reqval.length == 0){
        document.getElementById("nestedRMICheck").style.visibility = 'hidden';
        document.getElementById('isNestedRMILabel').style.display = 'none';
            }
    
    isNestedRMI();
}
</script>
  
</HEAD>
<BODY leftmargin="0" topmargin="0" marginwidth="0" marginheight="0" onload="onLoadDo();" onresize="onResizeDo();" onfocus="window.close();" id="paramBody" >
<FORM name="frmMain" method="post" action="">
  <INPUT type="hidden" name="Command"></INPUT>  
  <INPUT type="hidden" name="action" id="action" value=""></INPUT>
   
  <INPUT type="hidden" name="inpTabId" id=inpTabId value="<%= request.getAttribute("inpTabId")%>"></INPUT>
  <INPUT type="hidden" name="inpwindowId" id="inpwindowId" value="<%=request.getAttribute("inpwindowId")%>"></INPUT>
  <INPUT type="hidden" name="inpRecordId" id=inpRecordId value="<%= request.getAttribute("inpRecordId")%>"></INPUT>
  <INPUT type="hidden" name="inpAwardLookUps" id=inpAwardLookUps value=""></INPUT>
  <INPUT type="hidden" name="inpLetterCount" id=inpLetterCount value="<%= request.getAttribute("Count")%>"></INPUT>
  <INPUT type="hidden" name="selectUserId" id="selectUserId" value="<%= request.getAttribute("inpUserId") %>"></INPUT>
      <INPUT type="hidden" name="selectRoleId" id="selectRoleId" value="<%= request.getAttribute("inpRoleId") %>"></INPUT>
        <INPUT type="hidden" name="selectUserName" id="selectUserName" value="<%= request.getAttribute("inpUserName") %>"></INPUT>
         <INPUT type="hidden" name="selectRolName" id="selectRolName" value="<%= request.getAttribute("inpRoleName") %>"></INPUT>
               <%-- <INPUT type="hidden" name="selectDeptId" id="selectDeptId" value="<%= request.getAttribute("inpDeptId") %>"></INPUT> 
               <INPUT type="hidden" name="selectDeptName" id="selectDeptName" value="<%= request.getAttribute("inpDeptName") %>"></INPUT> --%>
      <INPUT type="hidden" name="selectRmiRequestId" id="selectRmiRequestId" value="<%= Utility.escapeQuote(request.getAttribute("inpRmiRequest")) %>"></INPUT>
        <INPUT type="hidden" name="inpIsnestedRMI" id="inpIsnestedRMI" value="<%= request.getAttribute("inpIsnestedRMI") %>"></INPUT>
      
  
       
 <TABLE cellspacing="0" cellpadding="0" width="100%" id="table_header">
  <TR>
    <TD>
      <TABLE cellspacing="0" cellpadding="0" class="Popup_ContentPane_NavBar" id="tdToolBar">
        <TR class="Popup_NavBar_bg"><TD></TD>
          <TD class="Popup_NavBar_separator_cell"></TD>
          <TD class="Popup_NavBar_Popup_title_cell"><SPAN><%=Resource.getProperty("utility.req.response.moreinfo",lang)%></SPAN></TD>
          <TD class="Popup_NavBar_separator_cell"></TD>
        </TR>
      </TABLE>
    </TD>
    <div class="Popup_ContentPane_CircleLogo">
      <div class="Popup_WindowLogo">
        <img class="Popup_WindowLogo_Icon Popup_WindowLogo_Icon_process" src="../web/images/blank.gif" border="0/">
      </div>
    </div>
  </TR>
  <TR>
    <TD>
      <TABLE cellspacing="0" cellpadding="0" class="Popup_ContentPane_SeparatorBar" id="tdtopTabs">
        <TR>
          <TD class="Popup_SeparatorBar_bg"></TD>
        </TR>
      </TABLE>
    </TD>
  </TR>
  <tr>
  <td>
     <table cellspacing="0" cellpadding="0" class="Popup_ContentPane_InfoBar">
        <tbody><tr>
          <td class="Popup_InfoBar_Icon_cell"><img src="../web/images/blank.gif" border="0" class="Popup_InfoBar_Icon_info"></td>
          <td class="Popup_InfoBar_text_table">
            <table>
              <tbody><tr>
                <td class="Popup_InfoBar_text" id="processHelp"><%=Resource.getProperty("utility.req.response.moreinfo",lang)%> </td>
              </tr>
            </tbody></table>
          </td>
        </tr>
      </tbody></table>
    </td>
</tr>
</TABLE>


<div id="Request_info" style="width:100%;" align="center">
           
                    <table>
                        <tbody>
                        
                         <tr>              
                                             
                                             <td class="TitleCell" Style ="align:Center"><span class="LabelText" id="isNestedRMILabel">
                                             
                                              <b> 
                                             <%= Resource.getProperty("utility.isnestedrmi",lang)%>
                                                </b>
                                                </span>
                                             </td>
                                            <TD class="TextBox_ContentCell">
                                                             <input type="checkbox"  id ="nestedRMICheck" onchange="isNestedRMI()"></input>
                                                         </TD>
                                                         
                                           </tr>
                                             
                   
                        
                        
                                <tr>
                            <td class="TitleCell" align="center"><span class="LabelText"><%= Resource.getProperty("utility.emp",lang)%></span></td>
                            <td class="TextBox_ContentCell">
                                                <select id="inpUser" class="ComboKey Combo_TwoCells_width" onchange="getRoleList(this.value)">
                                                  <%-- <option><%= Resource.getProperty("utility.comboselect",lang).replace("'", "\\\'") %></option> --%>
                                                
                                                </select>
                                             </td>
                        </tr>  
                               <tr>
                                             <td class="TitleCell"><span class="LabelText"><%=Resource.getProperty("utility.torole", lang).replace("'", "\\\'")%>
                                               
                                                </span>
                                             </td>
                                             <td class="TextBox_ContentCell">
                                                <select id="inpRole" class="ComboKey Combo_TwoCells_width">
                                                    <%-- <option value='0'><%= Resource.getProperty("utility.comboselect",lang).replace("'", "\\\'") %></option> --%>
                                     
                                                </select>
                                             </td>
                                          </tr>  
                        
                        
                        <tr>
                            <td class="TitleCell" align="center"><span class="LabelText"><%= Resource.getProperty("utility.emp.department",lang)%></span></td>
                            <td class="TextBox_btn_ContentCell TextBox_ReadOnly_ContentCell" maxlength="80" value="" required="false" align="center">
                            <table border="0" cellpadding="0" cellspacing="0"><tbody><tr><td class="TextBox_ReadOnly_ContentCell">
                            <!-- <select name="inpdepartment" class="Combo_OneCell_width" id="inpdepartment">
                           </select> -->
                           <input type="text" name="inpdepartment" id="inpdepartment"  maxlength="250"  class="TextBox_TwoCells_width"></input>
                           
                        </td></tr></tbody></table>
                        </td>
                        </tr>
                         <tr>
                               <td class="TitleCell"><span class="LabelText"><%=Resource.getProperty("utility.emp.designation", lang).replace("'", "\\\'")%>
                                </span>
                                </td>
                               <td class="TextBox_ContentCell">
                                <input type="text" name="inpposition" id="inpposition"  maxlength="250"  class="TextBox_TwoCells_width"></input>
                                </td>
                               </tr> 
                        
                        <tr>
                            <td class="TitleCell"><span class="LabelText"><%= Resource.getProperty("utility.req.message",lang)%></span></td>
                            <td class="TextBox_ContentCell"><textarea rows="4" width="50" cols="50" id="inprequest" maxlength="2000" name="inprequest" class="TextBox_btn_TwoCells_width"   ><%= Utility.escapeQuote(request.getAttribute("inpRmiRequest")) %></textarea></td>
                                                     
                </tr> 
                     <tr>
                            <td class="TitleCell"><span class="LabelText"><%= Resource.getProperty("utility.resp.message",lang)%></span></td>
                            <td class="TextBox_ContentCell"><textarea rows="4" width="50" cols="50" id="inpresponse" maxlength="2000" name="inpresponse" class="TextBox_btn_TwoCells_width" ></textarea></td>
                                                     
                </tr> 
                       <TR  align = "center">
                                             <td></td>
                                             <TD  id="submitButton">
                                                <BUTTON type="button" id="Submit_linkBTN" class="ButtonLink" onclick="sendRequestMoreinfo();" style="margin: 10px 0;">
                                                   <TABLE class="Button">
                                                      <TR>
                                                         <TD class="Button_left"><IMG class="Button_Icon Button_Icon_process" alt="Submit" title="Submit" src="../web/images/blank.gif" border="0"></IMG></TD>
                                                         <TD class="Button_text" id="Submit_ReqRes">
                                                            <%= Resource.getProperty("utility.send.request",lang).replace("'", "\\\'") %>
                                                         </TD>
                                                         <TD class="Button_right"></TD>
                                                      </TR>
                                                   </TABLE>
                                                </BUTTON>
                                             </TD>
                                          </TR>
                        <tr><td>&nbsp;</td></tr>
    
  
            </tbody></table>               
            

</div>





</FORM>
</BODY>
<script type="text/javascript">
<%-- <% List<PersonalPaymentMethodVO> inpbank = (ArrayList<PersonalPaymentMethodVO>)  request.getAttribute("inpbank");
  //strbanklist = "{";
  for (PersonalPaymentMethodVO vo : inpbank){
    strbanklist += vo.getbankdetailId() + ":" +  vo.getbankname() + ";";
  }
  // strbanklist += "}";
    List<EfinBankBranch> inpbranch = (ArrayList<EfinBankBranch>)  request.getAttribute("inpbranch");
  for (EfinBankBranch vo : inpbranch){
    strbranchList += vo.getId() + ":" +  vo.getBranchCode() + " "+  vo.getBranchName() +";";
  }
  %>
  document.getElementById("strbank").value="<%=strbanklist.substring(0,strbanklist.lastIndexOf(";"))%>";
   document.getElementById("strbranch").value="<%=strbranchList.substring(0,strbranchList.lastIndexOf(";"))%>";  --%>
  
</script>





 <!-- <script type="text/javascript">onLoadDo();</script> -->
<script type="text/javascript" src="../web/js/ui/jquery.ui.widget.js"></script>
<script type="text/javascript" src="../web/js/ui/jquery.ui.position.js"></script>
<script type="text/javascript" src="../web/js/ui/jquery.ui.button.js"></script>
<script type="text/javascript" src="../web/js/jquery/grid.locale-en.js"></script>
<script type="text/javascript" src="../web/js/jquery/datejs/jquery-ui.min.js"></script>
<script type="text/javascript" src="../web/js/common/select2.min.js"></script>
 <script type="text/javascript">


document.getElementById("inpdepartment").readOnly  = true;
document.getElementById("inpposition").readOnly  = true;
var responseUserId="",responseRoleId="";

 var checkRmiId =  checkRmiIdIsNull();
 function checkRmiIdIsNull() {
       var checkRmiId = "N";
       $.ajax({
         type: 'GET', 
         url: contextpath + '/sa.elm.ob.utility.ad_process.RequestMoreInformation/RequestMoreInformation?inpAction=checkRmiIdIsNull&inpRecordId='+ document.getElementById("inpRecordId").value +'&inpwindowId='+ document.getElementById("inpwindowId").value,


         data: {  
         },
         dataType: 'json',
         async: false
       }).done(function (response) {
           checkRmiId = response.checkRmiId;
       });
       return checkRmiId;
     } 

var isNested ; 
 function isNestedRMI() {
      var checkBox = document.getElementById("nestedRMICheck");
      var olduser;
      var oldrole;
      if (checkBox.checked == true){
          isNested = true;
          document.getElementById("inpresponse").value = "";
          $("#inpUser").empty().trigger('change');
         // document.getElementById("inpUser").value = "";
          //getRoleList(document.getElementById("inpUser").value);
          
          document.getElementById("inpdepartment").value = "";
          document.getElementById("inpposition").value = "";

          document.getElementById("inpUser").disabled  = false;
          document.getElementById("inpRole").disabled  = false;
          document.getElementById("inprequest").readOnly = false ;
          document.getElementById("inpresponse").readOnly = true ;
          document.getElementById("Submit_ReqRes").innerHTML="<%= Resource.getProperty("utility.send.request",lang).replace("'", "\\\'") %>";
          document.getElementById("inprequest").value = "<%= Utility.escapeQuote(request.getAttribute("inpRmiRequest")) %>" ;
          
          $("#inpUser").select2(selectBoxAjaxPaging({
              url: function() {
                return contextpath + '/sa.elm.ob.utility.ad_process.RequestMoreInformation/RequestMoreInformation?inpAction=getUserList&inpRecordId=' + document.getElementById("inpRecordId").value + '&inpwindowId=' + document.getElementById("inpwindowId").value
              },
              size: "small"
            }));
             $("#inpUser").on("select2:unselecting", function(e) {
              document.getElementById("inpUser").options.length = 0;
              document.getElementById("inpRole").options.length = 0; 
            }); 
             
             


      }else{
          isNested = false;

          document.getElementById("inpresponse").readOnly = false;
          document.getElementById("Submit_ReqRes").innerHTML="<%= Resource.getProperty("utility.send.response",lang).replace("'", "\\\'") %>";
          document.getElementById("inprequest").value = "<%= Utility.escapeQuote(request.getAttribute("inpRmiRequest")) %>";
          document.getElementById("inpresponse").value = "<%= Utility.escapeQuote(request.getAttribute("inpRmiReponse")) %>";
          //=====
          if(!isNested){
          if(!checkRmiId){
              $("#inpUser").empty().trigger('change');
              document.getElementById("inpUser").value=document.getElementById("selectUserId").value;
                getRoleList(document.getElementById("selectUserId").value);
             /*  document.getElementById("inpRole").value=document.getElementById("selectRoleId").value; */
               document.getElementById("inpUser").disable = false;
              document.getElementById("inpRole").disable = false;
              document.getElementById("inprequest").readOnly = true;
              document.getElementById("inpdepartment").value = "<%= request.getAttribute("inpDeptName") %>";
              document.getElementById("inpposition").value = "<%= request.getAttribute("inpPositionName") %>";
              document.getElementById("Submit_ReqRes").innerHTML="<%= Resource.getProperty("utility.send.response",lang).replace("'", "\\\'") %>";
              $("#inpUser").click(function() { 
                  $("#inpUser").select2("readonly", true); 
                  });
              //document.getElementById("inpUser")= document.getElementById("inpUserId").value;
              
               $.ajax({
                  type: 'POST', 
                  url: contextpath + '/sa.elm.ob.utility.ad_process.RequestMoreInformation/RequestMoreInformation?inpAction=getResponseUser',
                  data: {  
                      inpUser:document.getElementById("inpUser").value ,
                      inpRecordId:document.getElementById("inpRecordId").value,
                      inpwindowId:document.getElementById("inpwindowId").value
                  },
                  dataType: 'json',
                  async: false
              }).done(function (response) {
                  var data= response.List;
                  if(data.length>0){
                      $("#inpUser").select2({
                          data: data
                        });
                      for(obj in data){
                          responseUserId= data[0].id;
                      }
                  }
                  
              }); 
               $.ajax({
                   type: 'POST', 
                   url: contextpath + '/sa.elm.ob.utility.ad_process.RequestMoreInformation/RequestMoreInformation?inpAction=getResponseUserRoles',
                   data: {  
                       inpUser:responseUserId,
                       inpRecordId:document.getElementById("inpRecordId").value,
                       inpwindowId:document.getElementById("inpwindowId").value
                   },
                   dataType: 'json',
                   async: false
               }).done(function (response) {
                   var data= response.List;
                   if(data.length>0){
                       $("#inpRole").select2({
                           data: data
                         });
                   }
                   
               }); 
           /* 
               if(document.getElementById("selectUserId").value!=null && document.getElementById("selectUserId").value!=""){
                  var data = [{
                                  id: document.getElementById("selectUserId").value,
                                  text: document.getElementById("selectUserName").value
                                }];
                                $("#inpUser").select2({
                                  data: data
                                });
                       }
              
              if(document.getElementById("selectRoleId").value!=null && document.getElementById("selectRoleId").value!=""){
                  var data = [{
                                  id: document.getElementById("selectRoleId").value,
                                  text: document.getElementById("selectRolName").value
                                }];
                                $("#inpRole").select2({
                                  data: data
                                });
                       } */
                
             // $("#inpUser").prop("disabled", true);
             // $("#inpRole").prop("disabled", true); 
               
          }else{
              document.getElementById("inpresponse").readOnly = true;
              document.getElementById("Submit_ReqRes").innerHTML="<%= Resource.getProperty("utility.send.request",lang).replace("'", "\\\'") %>";
          }
          }
              
              
              //========
            
      }

    
    }

</script>
<script type="text/javascript">
var empStatus = "<%=Resource.getProperty("utility.fill.employee", lang)%>";
var empRole = "<%=Resource.getProperty("utility.fill.role", lang)%>";
var disablePreference = "<%=Resource.getProperty("utility.disableRMI", lang)%>";
var validUser = "<%=Resource.getProperty("utility.validUserList", lang)%>";
var checkPreValidations = "<%=Resource.getProperty("utility.checkValidation", lang)%>";
var RequestFieldIsMandatory = "<%=Resource.getProperty("utility.requestMandatory", lang)%>";
var ResponseFieldIsMandatory = "<%=Resource.getProperty("utility.responseMandatory", lang)%>";
var noOrgAccess= "<%=Resource.getProperty("utility.rmi.noorgaccess", lang)%>";
</script>
<script type="text/javascript" src="../web/js/ui/jquery.ui.autocomplete.js"></script> 
<script type="text/javascript" src="../web/js/jquery-1.7.2.min.js"></script>
<script type="text/javascript" src="../web/js/ui/jquery.ui.core.js"></script>
<script type="text/javascript" src="../web/js/jquery/jquery.jqGrid.min.js"></script>
<script type="text/javascript" src="../web/js/ui/jquery.ui.datepicker.js"></script>
<script type="text/javascript" src="../web/js/common/select2.min.js"></script>
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/eut_requestmoreinformation.js"></script>
<script type="text/javascript" src="../web/js/inlineNavGrid.js"></script>  
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.plugin.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.ummalqura.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.ummalqura-ar.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.plus.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.picker.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.picker.ext.js"></script>
</HTML>