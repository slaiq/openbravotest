 <%@page import="sa.elm.ob.scm.ad_reports.contractexecutionorder.ContractExecutionOrder"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="sa.elm.ob.scm.ESCMDefLookupsTypeLn"%>
<%@page import="org.openbravo.dal.service.OBCriteria"%>
<%@page import="org.openbravo.dal.service.OBDal"%>
<%@page import="sa.elm.ob.scm.ESCMDefLookupsType"%>
<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
 <%@page import="sa.elm.ob.utility.util.Utility"%>
 <%@page import="sa.elm.ob.scm.ad_reports.InvitationLetterReport.InvitationLetterReport"%>
 <%@ page import="sa.elm.ob.scm.properties.Resource" errorPage="/web/jsp/ErrorPage.jsp"%>
 <%@ page import="java.util.List,java.util.ArrayList" errorPage="/web/jsp/ErrorPage.jsp" %>
 <%     
    String lang = ((String)session.getAttribute("#AD_LANGUAGE"));
     String style="../web/skins/ltr/org.openbravo.userinterface.skin.250to300Comp/250to300Comp/Openbravo_ERP.css";
     if(lang.equals("ar_SA")){
         style="../web/skins/rtl/org.openbravo.userinterface.skin.250to300Comp/250to300Comp/Openbravo_ERP_250.css";
     }
     List<ContractExecutionOrder> subList = (request.getAttribute("inpSubject")==null?null:(List<ContractExecutionOrder>)request.getAttribute("inpSubject"));
 %>
<HTML>
<HEAD>
 <META http-equiv="Content-Type" content="text/html; charset=utf-8"></META>
    <TITLE><%=Resource.getProperty("scm.printreport.title",lang)%></TITLE>
    <LINK rel="stylesheet" type="text/css" href="<%=style %>" id="paramCSS"></LINK>  
      <link rel="stylesheet" type="text/css" href="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.picker.css"></link>
    <link rel="stylesheet" type="text/css" href="../web/js/jquery/jquery-ui-1.8.11.custom.css"></link>
    <link rel="stylesheet" type="text/css" href="../web/js/jquery/ui.jqgrid.css"></link>
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
    
    .reportTit{
        width: 680;

    }
    </style>
    <script type="text/javascript">
    function onLoadDo()
    {   
        var inpWindowID = "<%= request.getAttribute("inpWindowID")%>";
        var Docstatus = "<%= request.getAttribute("Docstatus")%>";
        var Ordertype = "<%= request.getAttribute("Ordertype")%>";
          if(inpWindowID=="2ADDCB0DD2BF4F6DB13B21BBCCC3038C"){
              if((Docstatus=='ESCM_AP' || Docstatus=='ESCM_IP' || Docstatus=='DR'|| Docstatus=='ESCM_REJ' || Docstatus=='ESCM_CA' || Docstatus=='ESCM_RA') && Ordertype == 'CR')
               {
                   $('#contract_report').show();
                   $('#propmgmt_report').hide();
                   $('#awardltr_params').hide();
                   $('#directPO_params').hide();
                    $('#GridView').hide();
                    $('#Submit_linkBTN').hide();
               }
            
           else if(Ordertype == 'PUR' && Docstatus !='ESCM_AP' )
               {
               $('#propmgmt_report').hide();
               $('#awardltr_params').hide();
               $('#directPO_params').show();
               $('#GridView').show();
               $('#Submit_linkBTN').show();
               $('#contract_report').hide();
               }
           else if(OrderType = 'PUR' && Docstatus == 'ESCM_AP')
               {
               $('#propmgmt_report').show();
               $('#awardltr_params').hide();
               $('#directPO_params').hide();
               $('#GridView').hide();
               $('#Submit_linkBTN').hide();
               $('#contract_report').hide();
               }
             
        } 
        
    }
    function onResizeDo()
    {
        reSizeGrid();
    }
    
    </script>
    
    <script type="text/javascript">
    var contextPath = "<%=request.getContextPath()%>";
    </script>
    <script type="text/javascript" src="../web/sa.elm.ob.scm/js/ProposalManagement.js"></script>
    <script type="text/javascript">
    var selecttext = "<%= Resource.getProperty("scm.--select--", lang) %>";
    </script>
    
</HEAD>
<BODY leftmargin="0" topmargin="0" marginwidth="0" marginheight="0" onload="onLoadDo();" onresize="onResizeDo();" onfocus="window.close();" id="paramBody">
<FORM name="frmMain" method="post" action="">
  <INPUT type="hidden" name="Command"></INPUT>  
  <INPUT type="hidden" name="action" id="action" value=""></INPUT>
   
  <INPUT type="hidden" name="inpTabId" id=inpTabId value="<%= request.getAttribute("inpTabId")%>"></INPUT>
  <INPUT type="hidden" name="inpWindowID" id="inpWindowID" value="<%=request.getAttribute("inpWindowID")%>"></INPUT>
  <INPUT type="hidden" name="inpRecordId" id=inpRecordId value="<%= request.getAttribute("inpRecordId")%>"></INPUT>
  <INPUT type="hidden" name="inpContractLookUps" id=inpContractLookUps value=""></INPUT>
  <INPUT type="hidden" name="inpLetterCount" id=inpLetterCount value="<%= request.getAttribute("Count")%>"></INPUT>
  <INPUT type="hidden" name="Docstatus" id=Docstatus value="<%= request.getAttribute("Docstatus")%>"></INPUT>
  <INPUT type="hidden" name="Ordertype" id=Ordertype value="<%= request.getAttribute("Ordertype")%>"></INPUT>
       
 <TABLE cellspacing="0" cellpadding="0" width="100%" id="table_header">
  <TR>
    <TD>
      <TABLE cellspacing="0" cellpadding="0" class="Popup_ContentPane_NavBar" id="tdToolBar">
        <TR class="Popup_NavBar_bg"><TD></TD>
          <TD class="Popup_NavBar_separator_cell"></TD>
          <TD class="Popup_NavBar_Popup_title_cell"><SPAN><%=Resource.getProperty("scm.printreport.title",lang)%></SPAN></TD>
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
                <td class="Popup_InfoBar_text" id="processHelp"><%=Resource.getProperty("scm.printreport.title",lang)%> </td>
              </tr>
            </tbody></table>
          </td>
        </tr>
      </tbody></table>
    </td>
</tr>
</TABLE>

    <TABLE cellspacing="0" cellpadding="0" width="100%">
      <TR>
        <TD>
          <DIV class="Popup_ContentPane_Client" style="overflow: auto;" id="client">  
            <TABLE cellpadding="0" cellspacing="0" id="messageBoxID" class="MessageBox" style="width: 100%;"><TBODY>
                            <TR class="MessageBox_TopMargin"><TD class="MessageBox_LeftMargin"></TD><TD></TD><TD class="MessageBox_RightMargin"></TD></TR>
                            <TR><TD class="MessageBox_LeftMargin"></TD><TD>
                                  <TABLE cellpadding="0" cellspacing="0" class="MessageBox_Container"><TBODY>
                                      <TR>
                                        <TD class="MessageBox_LeftTrans"><TABLE style="width: 100%; height: 100%" cellpadding="0" cellspacing="0"><TBODY><TR><TD class="MessageBox_TopLeft"></TD></TR><TR><TD class="MessageBox_Left"></TD></TR></TBODY></TABLE></TD>
                                        <TD class="MessageBox_bg"><TABLE class="MessageBox_Top"><TBODY><TR><TD><SPAN><TABLE cellpadding="0" cellspacing="0" class="MessageBox_Body_ContentCell"><TBODY><TR><TD class="MessageBox_Icon_ContentCell"><DIV class="MessageBox_Icon"></DIV></TD>
                                        <TD style="vertical-align: top;" id="messageBoxIDContent"><SPAN><DIV class="MessageBox_TextTitle" id="messageBoxIDTitle"></DIV><DIV class="MessageBox_TextDescription" id="messageBoxIDMessage"></DIV><DIV class="MessageBox_TextSeparator"></DIV></SPAN></TD></TR></TBODY></TABLE></SPAN></TD></TR></TBODY></TABLE></TD>
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
          <TABLE cellspacing="0" cellpadding="0" class="Popup_Client_TablePopup">           
            <TR>
            <td>
            <table>   
            <tr><td></td></tr>
            <tr id="propmgmt_report">
           <td colspan="2"><table>
                <tr id="propmgmt_report_select">
                    <TD class="TitleCell" align="left"><SPAN class="LabelText"> <%=Resource.getProperty("scm.POContract.report",lang)%></SPAN></TD>
                    <TD class="TextBox_btn_ContentCell TextBox_ReadOnly_ContentCell">
                        <TABLE border="0" cellpadding="0" cellspacing="0"><TR><TD class="TextBox_ReadOnly_ContentCell">
                        <SELECT name="inpOrderReport" class="ComboKey Combo_TwoCells_width" id="inpOrderReport" >
                            <option value="0"><%= Resource.getProperty("scm.--select--", lang) %></option>
                            <option value="PCER"><%= Resource.getProperty("scm.pocontract.pocontractexecution", lang) %></option>
                            <option value="DPOR"><%= Resource.getProperty("scm.pocontract.directpoletter",lang)%></option>                            
                                              
                        </SELECT>
                        </TD></TR></TABLE>
                    </TD>
                  
                </tr>
                </table>
                </td>
                </tr>
                <tr id="contract_report">
           <td colspan="2"><table>
                <tr id="contract_report_select">
                    <TD class="TitleCell" align="left"><SPAN class="LabelText"> <%=Resource.getProperty("scm.POContract.report",lang)%></SPAN></TD>
                    <TD class="TextBox_btn_ContentCell TextBox_ReadOnly_ContentCell">
                        <TABLE border="0" cellpadding="0" cellspacing="0"><TR><TD class="TextBox_ReadOnly_ContentCell">
                        <SELECT name="inpContractreport" class="ComboKey Combo_TwoCells_width" id="inpContractreport" >
                            <option value="0"><%= Resource.getProperty("scm.--select--", lang) %></option>
                            <option value="PCER"><%= Resource.getProperty("scm.pocontract.pocontractexecution", lang) %></option>
                            <option value="CONT"><%= Resource.getProperty("scm.pocontract.pocontract",lang)%></option>
                            <option value="CLFD"><%= Resource.getProperty("scm.pocontract.contractltrtofinancedewan",lang)%></option> 
                             <option value="TSR"><%= Resource.getProperty("scm.pocontract.techstudyreport",lang)%></option>     
                            <option value="PCCR"><%= Resource.getProperty("scm.pocontract.printcomputerreport",lang)%></option> 
                             <option value="PSC"><%= Resource.getProperty("scm.pocontract.printsupervisioncontract",lang)%></option>               
                            <option value="PER"><%= Resource.getProperty("scm.pocontract.printelectrcityReport",lang)%></option>                   
                              <option value="CCR"><%= Resource.getProperty("scm.pocontract.printcleaningreport",lang)%></option>
                              <option value="DCR"><%= Resource.getProperty("scm.pocontract.designcontractreport",lang)%></option>
                              <option value="MOFAC"><%= Resource.getProperty("scm.pocontract.mofapprovalreport",lang)%></option>                    
                        </SELECT>
                        </TD></TR></TABLE>
                    </TD>                  
                </tr>
                </table>
                </td>
                </tr>
                <tr><td>&nbsp;</td></tr>
                
                <tr id="awardltr_params" style="display: none;"> 
                <td colspan="2"><table>
                <tr>
                    <TD class="TitleCell"><SPAN class="LabelText"> <%=Resource.getProperty("scm.printreport.output",lang)%></SPAN></TD>
                    <TD class="TextBox_btn_ContentCell TextBox_ReadOnly_ContentCell">
                        <TABLE border="0" cellpadding="0" cellspacing="0"><TR><TD class="TextBox_ReadOnly_ContentCell">
                        <SELECT name="inpOutput" class="ComboKey Combo_OneCell_width" id="inpOutput" >
                            <option value="DR"><%= Resource.getProperty("scm.CEL.draft", lang) %></option>
                            <option value="FNL"><%= Resource.getProperty("scm.CEL.final",lang)%></option>                        
                        </SELECT>
                        </TD></TR></TABLE>
                    </TD>
                </tr>
                 <tr>
                    <TD class="TitleCell"><SPAN class="LabelText"> <%=Resource.getProperty("scm.CEL.subject",lang)%></SPAN></TD>
                    <TD class="TextBox_btn_ContentCell TextBox_ReadOnly_ContentCell">
                        <TABLE border="0" cellpadding="0" cellspacing="0"><TR><TD class="TextBox_ReadOnly_ContentCell">
                        <SELECT name="inpSubject" class="ComboKey Combo_OneCell_width" id="inpSubject" >
                       <% 
                           if (subList!=null && subList.size() > 0) 
                               for(ContractExecutionOrder sup : subList) { %>    
                                <option value="<%= sup.getId() %>"><%= Utility.escapeHTML(sup.getSequenceNo()+"-"+sup.getSubject())%></option>
                           <% } %>                                         
                        </SELECT>
                        </TD></TR></TABLE>
                    </TD>
                </tr>
                <tr><td>&nbsp;</td></tr>
          
                </table>
                </td>
                </tr>
               <tr id="directPO_params" style="display: none;">
                <td colspan="2"><table>
                 <tr>
                    <TD class="TitleCell"><SPAN class="LabelText"> <%=Resource.getProperty("scm.CEL.subject",lang)%></SPAN></TD>
                    <TD class="TextBox_btn_ContentCell TextBox_ReadOnly_ContentCell">
                        <TABLE border="0" cellpadding="0" cellspacing="0"><TR><TD class="TextBox_ReadOnly_ContentCell">
                     <textarea rows="5" cols="100" id="inpSubjectdirect"  maxlength="2000" name="inpSubjectdirect" class="TextBox_btn_TwoCells_width required reportTit">بلغكم بالموافقة على قيامكم بتأمين المطلوب وفق ماورد بعرضكم المشار إليه بالقيمة سالفة الذكر, على أن يكون التنسيق في ذلك مع  
    
وعليكم تقديم أصل هذا التعميد مع كافة المستندات المؤيدة للصرف علماً بأنه في حالة فقد أي من هذه المستندات قد تؤدي إلى تأخير صرف مستحقاتكم، وفي حالة تأخركم عن تأمين المطلوب خلال المدة المشار إليها فإنه سيطبق عليكم غرامة تأخير وفق للنظام. </textarea></TD></TR></TABLE>
                      
                    </TD>
                </tr>
                <tr><td>&nbsp;</td></tr>
                </table>
                </td>
                </tr>
                <tr id="ContLtrToFinDewan_Params" style="display: none;">
                <td colspan="2"><table>
                 <tr>
                    <TD class="TitleCell"><SPAN class="LabelText"> <%=Resource.getProperty("scm.pocontract.deptgeneralmanager",lang)%></SPAN></TD>
                    <TD class="TextBox_btn_ContentCell TextBox_ReadOnly_ContentCell">
                         <TABLE border="0" cellpadding="0" cellspacing="0"><TR><TD class="TextBox_ReadOnly_ContentCell">
                          <input type="text" name="inpDptGnrlMgr" id="inpDptGnrlMgr" maxlength="100" value=""   class="dojoValidateValid TextBox_TwoCells_width required " autocomplete="off" />
                        </TD></TR></TABLE>
                    </TD>
                </tr>
                <tr><td>&nbsp;</td></tr>
                </table>
                </td>
                </tr>
                <tr id="TechnicalReport_Params" style="display: none;">
                <td colspan="2"><table>
                 <tr>
                    <TD class="TitleCell"><SPAN class="LabelText"> <%=Resource.getProperty("scm.pocontract.deptowner",lang)%></SPAN></TD>
                    <TD class="TextBox_btn_ContentCell TextBox_ReadOnly_ContentCell">
                         <TABLE border="0" cellpadding="0" cellspacing="0"><TR><TD class="TextBox_ReadOnly_ContentCell">
                          <input type="text" name="inpDptOwner" id="inpDptOwner" maxlength="100" value=""   class="dojoValidateValid TextBox_TwoCells_width required " autocomplete="off" />
                        </TD></TR></TABLE>
                    </TD>
                </tr>
                <tr><td>&nbsp;</td></tr>
                </table>
                </td>
                </tr>
            </table> 
               <div id="GridView" style="width:100%; display: none;">
                   <div align="center"><table id="LookUpsList" class="scroll" cellpadding="0" cellspacing="0" width="100%"></table>
                       <div id="pager" class="scroll" style="text-align: center;"></div>
                   </div>
               </div>               
              <div id="Submit_inp" style="width:100%; text-align: center; margin: 10px 0px;">        
                 <BUTTON type="button" id="Submit_linkBTN" class="ButtonLink" onclick="GenerateReport();">
                 <TABLE class="Button">
                   <TR>
                     <TD class="Button_left"><IMG class="Button_Icon Button_Icon_process" alt="Submit" title="Submit" src="../web/images/blank.gif" border="0"></IMG></TD>
                     <TD class="Button_text" id="Submit_BTNname"><%= Resource.getProperty("scm.printreport.submit",lang)%></TD>
                     <TD class="Button_right"></TD>          
                   </TR>
                 </TABLE>
               </BUTTON>
             </div>
              </td></TR>
            </TABLE>        
          </DIV>
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
<script type="text/javascript" src="../web/js/ui/jquery.ui.datepicker.js"></script>
<script type="text/javascript">
var type="", lkups = "";
var lkpGrid = jQuery("#LookUpsList");
var lookup = {};
lookup.Lookup = [];
var gridIds, gridIdsLength;
$(document).ready(function(){
   var ordertype = "<%= request.getAttribute("Ordertype")%>";
 
  if(ordertype=='PUR'){
      type="DLC";
      $('#directPO_params').show();
      lkpGrid.trigger("reloadGrid");
      $('#GridView').show();
      $('#Submit_linkBTN').show();
  }
  <%--else if(ordertype='CR'){
      type="ELP";
      $('#awardltr_params').show();
      $('#directPO_params').hide();
      lkpGrid.trigger("reloadGrid");
      $('#GridView').show();
      $('#Submit_linkBTN').show();
  } --%>
  
    $("#inpOrderReport").change(function() {
            if($("#inpOrderReport").val()=='PCER'){
              type="ELP";
              $('#awardltr_params').show();
              $('#directPO_params').hide();
              lkpGrid.trigger("reloadGrid");
              $('#GridView').show();
              $('#Submit_linkBTN').show();
              $('#ContLtrToFinDewan_Params').hide(); 
          }else if($("#inpOrderReport").val()=='DPOR'){
              type="DLC";
              $('#awardltr_params').hide();
              $('#directPO_params').show();
              lkpGrid.trigger("reloadGrid");
              $('#GridView').show();
              $('#Submit_linkBTN').show();  
              $('#ContLtrToFinDewan_Params').hide();
          }           
          else if($("#inpOrderReport").val()=='0'){
              $('#propmgmt_report').show();
              $('#awardltr_params').hide();
              $('#directPO_params').hide();
              $('#GridView').hide();
              $('#Submit_linkBTN').hide(); 
              $('#ContLtrToFinDewan_Params').hide();
          }
         
    });
    $("#inpContractreport").change(function() {
        if($("#inpContractreport").val()=='PCER'){
            type="ELP";
            $('#awardltr_params').show();
            $('#directPO_params').hide();
            lkpGrid.trigger("reloadGrid");
            $('#GridView').show();
            $('#Submit_linkBTN').show();
            $('#ContLtrToFinDewan_Params').hide();
            $('#TechnicalReport_Params').hide();
        } 
        if($("#inpContractreport").val()=='TSR'){
            $('#awardltr_params').hide();
            $('#directPO_params').hide();              
            $('#GridView').hide();
            $('#Submit_linkBTN').show();  
            $('#ContLtrToFinDewan_Params').hide(); 
            $('#TechnicalReport_Params').show();
        } 
        if( $("#inpContractreport").val()=='CLFD'){
            $('#awardltr_params').hide();
            $('#directPO_params').hide();              
            $('#GridView').hide();
            $('#Submit_linkBTN').show();  
            $('#ContLtrToFinDewan_Params').show(); 
            $('#TechnicalReport_Params').hide();
        } 
        if( $("#inpContractreport").val()=='CONT'){
            $('#awardltr_params').hide();
            $('#directPO_params').hide();              
            $('#GridView').hide();
            $('#Submit_linkBTN').show();  
            $('#ContLtrToFinDewan_Params').hide(); 
            $('#TechnicalReport_Params').hide();
        }
        if( $("#inpContractreport").val()=='PCCR' ||$("#inpContractreport").val()=='PER' ||  $("#inpContractreport").val()=='CCR' || $("#inpContractreport").val()=='PSC' || $("#inpContractreport").val()=='DCR'){
             $('#awardltr_params').hide();
             $('#directPO_params').hide();              
             $('#GridView').hide();
             $('#Submit_linkBTN').show();  
             $('#ContLtrToFinDewan_Params').hide(); 
             $('#TechnicalReport_Params').hide();
        }
         if($("#inpContractreport").val()=='0'){
            $('#contract_report').show();
            $('#awardltr_params').hide();
            $('#directPO_params').hide();
            $('#GridView').hide();
            $('#Submit_linkBTN').hide();
            $('#ContLtrToFinDewan_Params').hide(); 
            $('#TechnicalReport_Params').hide();
        }if($("#inpContractreport").val()=='MOFAC'){
            type="MOFAC";
            $('#awardltr_params').hide();
            $('#directPO_params').hide();
            lkpGrid.trigger("reloadGrid");
            $('#GridView').show();
            $('#Submit_linkBTN').show();
            $('#ContLtrToFinDewan_Params').hide();
            $('#TechnicalReport_Params').hide();
        } 
    });

  }); 
        
    function reSizeGrid()
    {
        var w, h;
        if (window.innerWidth)  { w = window.innerWidth; h = window.innerHeight; }
        else if (document.body)  { w = document.body.clientWidth; h = document.body.clientHeight; }
        document.getElementById('client').style.height = ((h - 20) + 'px');
        w = w - 50;
        h = h - 310;
        if (w <= 600)
            w = 600;
        if (h <= 200)
            h = 200;
        lkpGrid.setGridWidth(w, true);
        lkpGrid.setGridHeight(h, true);
        
    }
    jQuery(function() 
    {
        lkpGrid.jqGrid(
        {
                url:'<%=request.getContextPath()%>/PrintReportAjax?action=getLookUpsList',
                colNames:['<%= Resource.getProperty("scm.pm.seqno",lang)%>', '<%= Resource.getProperty("scm.pm.LookUp",lang)%>'],
                colModel:[                                                        
                          {name:'seqno',index:'seqno', sortable:false, align:'right', editable:true, edittype:'text', 
                              formatter:'integer', editrules:{number:true, integer:true},
                              editoptions: {  
                                                dataEvents: 
                                                [
                                                     { type: 'keypress', 
                                                         fn: function(e) 
                                                         {                                                  
                                                            var charCode = (e.which) ? e.which : e.keyCode;         
                                                            if(charCode == 8 || charCode == 9 || charCode == 35 || charCode == 36 || charCode == 37 || charCode == 39 || charCode == 46 || charCode == 127 || ( charCode >= 48 && charCode <= 57 ))
                                                            {           
                                                                var character = String.fromCharCode(charCode);          
                                                                if(e.which!="0" && (character=="#" || character=="$" || character=="%" || character=="'" || character=="."))
                                                                    return false;           
                                                                if(e.which=="0" && charCode == 46)
                                                                    return true;
                                                                if(charCode == 46 && index.value.indexOf(".")!=-1)
                                                                    return false;
                                                                return true;
                                                            }
                                                            return false;                                                               
                                                         } 
                                                     }
                                                ]
                                           }
                                                           
                          },              
                          {name:'name',index:'name', sortable:false, align:'left', editable:true, edittype:'text'}
                          ],
                
                editurl:'clientArray',
                pager: '#pager',
                datatype: 'xml',
                rownumbers: true,
                viewrecords: true,
                autowidth: true,
                shrinkToFit: true,
                sortname: 'seqno',
                sortorder: "asc",
                height:"90%",
                pgbuttons:false,
                pgtext:'',
                multiselect: true,  
                  
                loadComplete:function(data)
                {       
                    ChangeJQGridAllRowColor(lkpGrid);   
                    $('input[role*="checkbox"]').click(function(){jQuery("#LookUpsList").setSelection($(this).parent().parent().attr('id'));});
                    reSizeGrid();
                    gridIds=jQuery("#LookUpsList").getDataIDs();
                    gridIdsLength=gridIds.length;                                                   
                            
                },
                beforeSelectRow: function(id, e) { 
                   jQuery(this).find('#'+id+'input[type=checkbox]').prop('checked',true);
                },
                beforeRequest : function(){
                    jQuery('#LookUpsList').setPostDataItem("type",type);
                },
                onSelectRow: function(id, status)
                {               
                    var ids = ""+jQuery("#LookUpsList").jqGrid('getGridParam','selarrrow');
                    var idList=ids.split(",");
                    var idListLength=idList.length;
                    if(ids.indexOf(",")==0)
                        idListLength--;
                    
                    if(status==true)
                    {                   
                        ChangeJQGridSelectMultiRowColor(id, "S");
                        jQuery('#LookUpsList').editRow(id, true);           
                        if(idListLength==gridIdsLength)
                        {
                            document.getElementById("cb_LookUpsList").checked = true;
                        }
                    }   
                    else if (status==false)
                    {                   
                        ChangeJQGridSelectMultiRowColor(id, "US");
                        jQuery('#LookUpsList').restoreRow(id); 
                                        
                        if(document.getElementById("cb_LookUpsList").checked = true)
                            document.getElementById("cb_LookUpsList").checked = false;
                    }                   
                },
                onSelectAll:  function(id,status)
                {                   
                    var ids = ""+id
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
                            jQuery('#LookUpsList').editRow(idList[i], true);                            
                        }   
                        else if (status==false)
                        {
                            ChangeJQGridSelectMultiRowColor(idList[i], "US");
                            jQuery('#LookUpsList').restoreRow(idList[i]);                                               
                        }
                    }
                },
                caption: ''
        }); 
        lkpGrid.jqGrid('navGrid','#pager',{edit:false,add:false,del:false,search:false,view: false,beforeRefresh:function(){                
            lkpGrid.trigger("reloadGrid");
        }
    },{ },{ },{ }, { });
        reSizeGrid();   
    });
    function addLookups(){        
        lkups = ""+jQuery("#LookUpsList").jqGrid('getGridParam','selarrrow');
        /* if(lkups=="")
        {     
            OBAlert("Please select atleast one lookups to print");
            return false;
        } */
        if(lkups!="")
        {
            var idList=lkups.split(",");
            for(var i=0;i<idList.length;i++)
            {
                var rowData = jQuery("#LookUpsList").getRowData(idList[i]); 
                var lkupLnId=idList[i];                             
                var seqNo = ""+$('input[id="'+idList[i]+'_seqno"]').val();
                var awardLkUps = ""+$('input[id="'+idList[i]+'_name"]').val();
                                
                if(lookup.Lookup=="" || lookup.Lookup==null)
                {               
                    lookup.Lookup[i]={};
                    lookup.Lookup[i]['SeqNo']=seqNo;
                    lookup.Lookup[i]['ContractLookup']=awardLkUps;                 
                }
                else
                {
                    lookup.Lookup[i]={};
                    lookup.Lookup[i]['SeqNo']=seqNo;
                    lookup.Lookup[i]['ContractLookup']=awardLkUps;                 
                }
            }   
            document.getElementById("inpContractLookUps").value = JSON.stringify(lookup);
            console.log(JSON.stringify(lookup));
        }
        return true;
    }
    function validateDuplicate(lookUps){
        var temp = [];
        var duplicate = 0;
        $.each(lookUps, function (key, value) {
           if($.inArray(value.SeqNo, temp) === -1) {
                temp.push(value.SeqNo);
            }else{
               //console.log(value.SeqNo+" is a duplicate value");
               duplicate = 1;              
            }
        });
        return duplicate;
    }
    function closePopUp() {
        document.frmMain.action="<%=request.getContextPath()%>/sa.elm.ob.scm.ad_process.printreport/PrintReport?action=Close&pageType="+"<%= request.getAttribute("pageType") %>";
        document.frmMain.submit();
    }
    function GenerateReport(){
        $("#action").val("DownloadReport");  
       
        if($("#inpOrderReport").val()=='PCER' || $("#inpContractreport").val()=='PCER' || $("#inpContractreport").val()=='MOFAC'){

            if(addLookups()){  
                if(lkups=="")
                {     
                    OBAlert("<%= Resource.getProperty("scm.CEL.selectatleastonerow",lang)%>");
                    return false;  
                }
                else if(validateDuplicate(lookup.Lookup)==1){
                    OBAlert("<%= Resource.getProperty("scm.pm.validateduplicateseqno",lang)%>");
                    return false;
                }                       
            }
        }      
        else if($("#inpContractreport").val()=='CLFD')
        {
             if($('#inpDptGnrlMgr').val()=="")
               {
                OBAlert("<%= Resource.getProperty("scm.pocontract.deptgeneralmanagercantbeempty",lang)%>");
                return false;  
               }
         }
        else if($("#inpContractreport").val()=='TSR')
        {
             if($('#inpDptOwner').val()=="")
               {
                OBAlert("<%= Resource.getProperty("scm.pocontract.deptownercantbeempty",lang)%>");
                return false;  
               }
         }
        else if($("#inpContractreport").val()!='CONT' && $("#inpContractreport").val()!='CLFD' && $("#inpContractreport").val()!='TSR' && $("#inpContractreport").val()!='PCCR' && $("#inpContractreport").val()!='PER' && $("#inpContractreport").val()!='CCR' && $("#inpContractreport").val()!='PSC' && $("#inpContractreport").val()!='DCR')
            {
                 if($('#inpSubjectdirect').val()=="")
                   {
                    OBAlert("<%= Resource.getProperty("scm.DPO.inpsubjectvalidate",lang)%>");
                    return false;  
                   }
                else if(addLookups())
                    {
                     if(lkups=="")
                     {     
                         OBAlert("<%= Resource.getProperty("scm.DPO.selectsubmitaction",lang)%>");
                         return false;  
                     }
                    }
             }
            
            submitCommandForm('DEFAULT', false, null, '<%=request.getContextPath()%>/sa.elm.ob.scm.ad_process.printreport/PrintReport?report=ContractExecution', 'background_target', null, false);        
            closePage();
            return true;
        
    }
    
    
    $("#inpOutput").change(function() {
        if($("#inpOutput").val()=='FNL'){
            checkSequenceDefined("ExecutionOrder");                                  
        }
    });
    
 
function checkSequenceDefined(letterName){         
    var url="<%=request.getContextPath()%>/PrintReportAjax?action=checkSequencepo";
    $.getJSON(url, function(result){            
    }).done(function(data){
        if(data.Sequence=="false"){
                $("#inpOutput").val("DR");
            OBAlert("<%= Resource.getProperty("scm.pm.transactionsequencenotdefined",lang)%>");
            return false;
        }            
    });
} 
    
</script>
</HTML>
