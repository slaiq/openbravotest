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
    <script type="text/javascript">
    function onLoadDo()
    {   
        var inpWindowID = "<%= request.getAttribute("inpWindowID")%>";
        var Docstatus = "<%= request.getAttribute("Docstatus")%>";
        var Ordertype = "<%= request.getAttribute("Ordertype")%>";
<%--          if(inpWindowID=="2ADDCB0DD2BF4F6DB13B21BBCCC3038C"){
              if((Docstatus=='ESCM_AP' || Docstatus=='ESCM_IP' || Docstatus=='DR'|| Docstatus=='ESCM_REJ' || Docstatus=='ESCM_CA') && Ordertype == 'CR')
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
             
        } --%>
        
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
                    <TD class="TitleCell" align="left"><SPAN class="LabelText"> <%=Resource.getProperty("scm.technical.event.report",lang)%></SPAN></TD>
                    <TD class="TextBox_btn_ContentCell TextBox_ReadOnly_ContentCell">
                        <TABLE border="0" cellpadding="0" cellspacing="0"><TR><TD class="TextBox_ReadOnly_ContentCell">
                        <SELECT name="inpTechnicalReport" class="ComboKey Combo_TwoCells_width" id="inpTechnicalReport" >
                            <option value="0"><%= Resource.getProperty("scm.--select--", lang) %></option>                            
                            <option value="TSR"><%= Resource.getProperty("scm.tech.study", lang) %></option>
                            <option value="TSRR"><%= Resource.getProperty("scm.tech.study.reply",lang)%></option>                        
                                             
                        </SELECT>
                        </TD></TR></TABLE>
                    </TD>
                  
                </tr>
                </table>
                </td>
                </tr>
               
                <tr id="TechnicalReport_Params" style="display: none;">
                <td colspan="2"><table>
                 <tr>
                    <TD class="TitleCell"><SPAN class="LabelText"> <%=Resource.getProperty("scm.tech.requester",lang)%></SPAN></TD>
                    <TD class="TextBox_btn_ContentCell TextBox_ReadOnly_ContentCell">
                         <TABLE border="0" cellpadding="0" cellspacing="0"><TR><TD class="TextBox_ReadOnly_ContentCell">
                          <input type="text" name="inpParamOne" id="inpParamOne" maxlength="100" value=""   class="dojoValidateValid TextBox_TwoCells_width required " autocomplete="off" />
                        </TD></TR></TABLE>
                    </TD>
                </tr>
                <tr>
                    <TD class="TitleCell"><SPAN class="LabelText"> <%=Resource.getProperty("scm.tech.study.param2",lang)%></SPAN></TD>
                    <TD class="TextBox_btn_ContentCell TextBox_ReadOnly_ContentCell">
                         <TABLE border="0" cellpadding="0" cellspacing="0"><TR><TD class="TextBox_ReadOnly_ContentCell">
                          <input type="text" name="inpParamTwo" id="inpParamTwo" maxlength="100" value="مدير عام الإدارة العامة للمنافسات والعقود"   class="dojoValidateValid TextBox_TwoCells_width required " autocomplete="off" />
                        </TD></TR></TABLE>
                    </TD>
                </tr>
                <tr>
                    <TD class="TitleCell"><SPAN class="LabelText"> <%=Resource.getProperty("scm.tech.study.param3",lang)%></SPAN></TD>
                    <TD class="TextBox_btn_ContentCell TextBox_ReadOnly_ContentCell">
                         <TABLE border="0" cellpadding="0" cellspacing="0"><TR><TD class="TextBox_ReadOnly_ContentCell">
                          <input type="text" name="inpParamThree" id="inpParamThree" maxlength="100" value="حامد بن محمد الشمري"   class="dojoValidateValid TextBox_TwoCells_width required " autocomplete="off" />
                        </TD></TR></TABLE>
                    </TD>
                </tr>
                <tr><td>&nbsp;</td></tr>
                </table>
                </td>
                </tr>
                
                <tr id="TechnicalReplyReport_Params" style="display: none;">
                <td colspan="2"><table>
                 <tr>
                    <TD class="TitleCell"><SPAN class="LabelText"> <%=Resource.getProperty("scm.tech.requester",lang)%></SPAN></TD>
                    <TD class="TextBox_btn_ContentCell TextBox_ReadOnly_ContentCell">
                         <TABLE border="0" cellpadding="0" cellspacing="0"><TR><TD class="TextBox_ReadOnly_ContentCell">
                          <input type="text" name="replyParamOne" id="replyParamOne" maxlength="100" value=""   class="dojoValidateValid TextBox_TwoCells_width required " autocomplete="off" />
                        </TD></TR></TABLE>
                    </TD>
                </tr>
                                 <tr>
                    <TD class="TitleCell"><SPAN class="LabelText"> <%=Resource.getProperty("scm.tech.param2",lang)%></SPAN></TD>
                    <TD class="TextBox_btn_ContentCell TextBox_ReadOnly_ContentCell">
                         <TABLE border="0" cellpadding="0" cellspacing="0"><TR><TD class="TextBox_ReadOnly_ContentCell">
                          <input type="text" name="replyParamTwo" id="replyParamTwo" maxlength="100" value="مدير عام الإدارة العامة للمنافسات والعقود"   class="dojoValidateValid TextBox_TwoCells_width required " autocomplete="off" />
                        </TD></TR></TABLE>
                    </TD>
                </tr>
                                 <tr>
                    <TD class="TitleCell"><SPAN class="LabelText"> <%=Resource.getProperty("scm.tech.param3",lang)%></SPAN></TD>
                    <TD class="TextBox_btn_ContentCell TextBox_ReadOnly_ContentCell">
                         <TABLE border="0" cellpadding="0" cellspacing="0"><TR><TD class="TextBox_ReadOnly_ContentCell">
                          <input type="text" name="replyParamThree" id="replyParamThree" maxlength="100" value="حامد بن محمد الشمري"   class="dojoValidateValid TextBox_TwoCells_width required " autocomplete="off" />
                        </TD></TR></TABLE>
                    </TD>
                </tr>
                                 <tr>
                    <TD class="TitleCell"><SPAN class="LabelText"> <%=Resource.getProperty("scm.tech.param4",lang)%></SPAN></TD>
                    <TD class="TextBox_btn_ContentCell TextBox_ReadOnly_ContentCell">
                         <TABLE border="0" cellpadding="0" cellspacing="0"><TR><TD class="TextBox_ReadOnly_ContentCell">
                          <input type="text" name="replyParamFour" id="replyParamFour" maxlength="100" value="مدير عام الإدارة العامة للمنافسات والعقود"   class="dojoValidateValid TextBox_TwoCells_width required " autocomplete="off" />
                        </TD></TR></TABLE>
                    </TD>
                </tr>
                                 <tr>
                    <TD class="TitleCell"><SPAN class="LabelText"> <%=Resource.getProperty("scm.tech.param5",lang)%></SPAN></TD>
                    <TD class="TextBox_btn_ContentCell TextBox_ReadOnly_ContentCell">
                         <TABLE border="0" cellpadding="0" cellspacing="0"><TR><TD class="TextBox_ReadOnly_ContentCell">
                          <input type="text" name="replyParamFive" id="replyParamFive" maxlength="100" value="حامد بن محمد الشمري"   class="dojoValidateValid TextBox_TwoCells_width required " autocomplete="off" />
                        </TD></TR></TABLE>
                    </TD>
                </tr>
                                 <tr>
                    <TD class="TitleCell"><SPAN class="LabelText"> <%=Resource.getProperty("scm.tech.param6",lang)%></SPAN></TD>
                    <TD class="TextBox_btn_ContentCell TextBox_ReadOnly_ContentCell">
                         <TABLE border="0" cellpadding="0" cellspacing="0"><TR><TD class="TextBox_ReadOnly_ContentCell">
                          <input type="text" name="replyParamSix" id="replyParamSix" maxlength="100" value="مدير عام الإدارة العامة للمنافسات والعقود"   class="dojoValidateValid TextBox_TwoCells_width required " autocomplete="off" />
                        </TD></TR></TABLE>
                    </TD>
                </tr>
                                 <tr>
                    <TD class="TitleCell"><SPAN class="LabelText"> <%=Resource.getProperty("scm.tech.param7",lang)%></SPAN></TD>
                    <TD class="TextBox_btn_ContentCell TextBox_ReadOnly_ContentCell">
                         <TABLE border="0" cellpadding="0" cellspacing="0"><TR><TD class="TextBox_ReadOnly_ContentCell">
                          <input type="text" name="replyParamSeven" id="replyParamSeven" maxlength="100" value="حامد بن محمد الشمري"   class="dojoValidateValid TextBox_TwoCells_width required " autocomplete="off" />
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
   
    $("#inpTechnicalReport").change(function() {
            if($("#inpTechnicalReport").val()=='TSR'){
              $('#TechnicalReport_Params').show();
              $('#TechnicalReplyReport_Params').hide(); 
          }else if($("#inpTechnicalReport").val()=='TSRR'){
              $('#TechnicalReport_Params').hide();
              $('#TechnicalReplyReport_Params').show();
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
       
            
            submitCommandForm('DEFAULT', false, null, '<%=request.getContextPath()%>/sa.elm.ob.scm.ad_process.printreport/PrintReport?report=TechnicalStudy', 'background_target', null, false);        
            closePage();
            return true;
        
    }
 
    
</script>
</HTML>
