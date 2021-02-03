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
     List<InvitationLetterReport> supplierList = (request.getAttribute("inpSupplierList")==null?null:(List<InvitationLetterReport>)request.getAttribute("inpSupplierList"));

 %>
<HTML>
<HEAD>
 <META http-equiv="Content-Type" content="text/html; charset=utf-8"></META>
    <TITLE><%=Resource.getProperty("scm.printreport.title",lang)%></TITLE>
    <LINK rel="stylesheet" type="text/css" href="<%=style %>" id="paramCSS"></LINK>  
      <link rel="stylesheet" type="text/css" href="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.picker.css"></link>
    <link rel="stylesheet" type="text/css" href="../web/js/jquery/jquery-ui-1.8.11.custom.css"></link>
    
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
    function onLoadDo()
    {
        /* this.windowTables = new Array(
          new windowTableId('client', 'buttonOK')
        );
        setWindowTableParentElement();
        enableShortcuts('popup');
        setBrowserAutoComplete(false); */  
                
        var inpWindowID = "<%= request.getAttribute("inpWindowID")%>";
        if(inpWindowID=="184"){
            var inpReceivingType = "<%= request.getAttribute("inpReceivingType")%>";
           /*  if(inpReceivingType=='IR'){
                $('#delivery_params').hide();
            }
            else */ if(inpReceivingType=='SR' || inpReceivingType=='DEL'){     
                $('#delivery_params').show();
            }
            else if(inpReceivingType=='INS')
            {
                $('#inspe_params').show();
                $('#delivery_params').hide();
            }            
        }else if(inpWindowID=="D8BA0A87790B4B67A86A8DF714525736"){
            $('#delivery_params').show();
        }
        else if(inpWindowID=="E509200618424FD099BAB1D4B34F96B8"){
            $('#supplier_params').show();
            $('#supplier_params_check').show();
        }else if(inpWindowID=="62E42B7D4CF74BF08532F18D5AF084FD"){
            $('#openenvcmt_params').show();
        }else if(inpWindowID=="CAF2D3EEF3B241018C8F65E8F877B29F"){
            $('#fundreservreq_report').show();
        }else{
            $('#delivery_params').hide();
            $('#inspe_params').hide();
        }
        $('#inpParamCost1').calendarsPicker({calendar:  
            $.calendars.instance('ummalqura'),dateFormat: 'dd-mm-yyyy' ,showTrigger:  
        '<img src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/calendar.gif" alt="Popup" class="trigger">'});
    }
    function onResizeDo()
    {
        //resizePopup();
    }
    
    </script>
</HEAD>
<BODY leftmargin="0" topmargin="0" marginwidth="0" marginheight="0" onload="onLoadDo();" onresize="onResizeDo();" onfocus="window.close();" id="paramBody">
<FORM name="frmMain" method="post" action="">
  <INPUT type="hidden" name="Command"></INPUT>  
  <INPUT type="hidden" name="action" id="action" value=""></INPUT>
   
  <INPUT type="hidden" name="inpTabId" id=inpTabId value="<%= request.getAttribute("inpTabId")%>"></INPUT>
  <INPUT type="hidden" name="inpWindowID" id="inpWindowID" value="<%=request.getAttribute("inpWindowID")%>"></INPUT>
  <INPUT type="hidden" name="inpRecordId" id=inpRecordId value="<%= request.getAttribute("inpRecordId")%>"></INPUT>
  <INPUT type="hidden" name="inpReceivingType" id=inpReceivingType value="<%= request.getAttribute("inpReceivingType")%>"></INPUT>
  <INPUT type="hidden" name="inpDocNo" id=inpDocNo value="<%= request.getAttribute("inpDocNo")%>"></INPUT>
     
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
          <TABLE cellspacing="0" cellpadding="0" class="Popup_Client_TablePopup">           
            <TR>
            <td>    
            <table>
            <%-- <tr>
                <TD class="TitleCell" style="width: 200px; padding-top: 5px !important"><span class="LabelText"><%=Resource.getProperty("scm.printreport.initrecevreprt",lang)%></span></TD>
                <TD class="Radio_Check_ContentCell"><input type="radio" name="poreceipt_report" id="initreceiv_en" value=""/></TD>
                
                <TD class="TitleCell" style="width: 200px; padding-top: 5px !important;"><span class="LabelText">تقرير الاستلام الأولي</span></TD>
                <TD class="Radio_Check_ContentCell"><input type="radio" name="poreceipt_report" id="initreceiv_ar" value=""/></TD>
            </tr>
            <tr>
                <TD class="TitleCell" style="width: 200px; padding-top: 5px !important"><span class="LabelText"><%=Resource.getProperty("scm.printreport.invendelivnote",lang)%></span></TD>
                <TD class="Radio_Check_ContentCell"><input type="radio" name="poreceipt_report" id="invendeliv_en" value=""/></TD>
                
                <TD class="TitleCell" style="width: 200px; padding-top: 5px !important"><span class="LabelText">جرد مذكرة التسليم</span></TD>
                <TD class="Radio_Check_ContentCell"><input type="radio" name="poreceipt_report" id="invendeliv_ar" value=""/></TD>
            </tr>
            <tr>
                <TD class="TitleCell" style="width: 200px; padding-top: 5px !important"><span class="LabelText"><%=Resource.getProperty("scm.printreport.sitedelivnote",lang)%></span></TD>
                <TD class="Radio_Check_ContentCell"><input type="radio" name="poreceipt_report" id="sitedeliv_en" value=""/></TD>
                
                <TD class="TitleCell" style="width: 200px; padding-top: 5px !important"><span class="LabelText">موقع تسليم ملاحظة</span></TD>
                <TD class="Radio_Check_ContentCell"><input type="radio" name="poreceipt_report" id="sitedeliv_ar" value=""/></TD>
            </tr>
            <tr><td></td></tr> --%>
            <%-- <tr id="initreceiv_params" style="display: none;" > 
                <TD class="TitleCell"><span class="LabelText"><span> <%=Resource.getProperty("scm.printreport.suppdeliverer",lang)%></SPAN> </SPAN></TD>
                <td><INPUT type="text" maxlength="50" id="inpSupplierDeliverer" name="inpSupplierDeliverer" class="dojoValidateValid required TextBox_TwoCells_width" value="" ></INPUT></td>
            </tr> --%>
            <tr><td></td></tr>
            <tr id="delivery_params" style="display: none;">
                <TD class="TitleCell"><SPAN class="LabelText"> <%=Resource.getProperty("scm.printreport.outputtype",lang)%></SPAN></TD>
                <TD class="TextBox_btn_ContentCell TextBox_ReadOnly_ContentCell">
                    <TABLE border="0" cellpadding="0" cellspacing="0"><TR><TD class="TextBox_ReadOnly_ContentCell">
                    <SELECT name="inpOutputType" class="ComboKey Combo_OneCell_width" id="inpOutputType" >
                        <option value="A4"><%= Resource.getProperty("scm.printreport.a4", lang) %></option>
                        <option value="DM"><%= Resource.getProperty("scm.printreport.dotmatrix",lang)%></option>                        
                    </SELECT>
                    </TD></TR></TABLE>
                </TD>
            </tr>     
             <tr id="inspe_params" style="display: none;">
                <TD class="TitleCell"><SPAN class="LabelText"> <%=Resource.getProperty("scm.printreport.outputtype",lang)%></SPAN></TD>
                <TD class="TextBox_btn_ContentCell TextBox_ReadOnly_ContentCell">
                    <TABLE border="0" cellpadding="0" cellspacing="0"><TR><TD class="TextBox_ReadOnly_ContentCell">
                    <SELECT name="inpOutputType" class="ComboKey Combo_OneCell_width" id="inpOutputType" >
                        <option value="A4"><%= Resource.getProperty("scm.printreport.a4", lang) %></option>
                    </SELECT>
                    </TD></TR></TABLE>
                </TD>
            </tr>
            <tr id="supplier_params" style="display: none;">
                <TD class="TitleCell"><SPAN class="LabelText"><%=Resource.getProperty("scm.printreport.supplierName",lang)%></SPAN></TD>
                <TD class="TextBox_btn_ContentCell TextBox_ReadOnly_ContentCell">
                    <TABLE border="0" cellpadding="0" cellspacing="0"><TR><TD class="TextBox_ReadOnly_ContentCell">
                         <SELECT name="inpSupList" class="ComboKey Combo_TwoCells_width"  id="inpSupList" >
                        <option value="0"><%=Resource.getProperty("scm.printreport.selectall",lang)%></option>
                           <% 
                           if (supplierList!=null && supplierList.size() > 0) 
                               for(InvitationLetterReport sup : supplierList) {%>    
                                <option value="<%= sup.getSuppliersId() %>"><%= Utility.escapeHTML(sup.getBpName()+" - "+sup.getBpAddress())%></option>
                           <% } %>                          
                          </SELECT>
            </TD></TR></TABLE> 
            </tr> 
            <tr id="openenvcmt_params" style="display: none;">           
             <TD class="Radio_Check_ContentCell"><input type="radio" checked="checked" name="openenvevt_report" id="openenvprop" value="PROP"/></TD>
             <TD class="TitleCell"  style="padding-top: 4px !important"><span class="LabelText"><%=Resource.getProperty("scm.openenvcomtie.openenvproposal",lang)%></span></TD>
             
             <td width="100px">&nbsp;</td>
             
             <TD class="Radio_Check_ContentCell"><input type="radio" name="openenvevt_report" id="openenvcommite" value="COMT"/></TD>
             <TD class="TitleCell" style="padding-top: 4px !important;"><span class="LabelText"><%=Resource.getProperty("scm.openenvcomtie.openenvcommittee",lang)%></span></TD>
           </tr> 
            <%-- <tr id="fundreservreq_report" style="display: none;">
            <td><table>
                <tr id="fundresrvreport_head">
                    <TD class="TitleCell" style="width: 250px; padding-top: 5px !important"><span class="LabelText"><%=Resource.getProperty("scm.pm.fundrsrvinfo",lang)%></span></TD>
                    <TD class="Radio_Check_ContentCell"><input type="radio" name="fundreserv_report" id="fndresrvinfo" value="FRIR"/></TD>
                    
                    <TD class="TitleCell" style="width: 250px; padding-top: 5px !important;"><span class="LabelText"><%=Resource.getProperty("scm.pm.fundrsrvcost",lang)%></span></TD>
                    <TD class="Radio_Check_ContentCell"><input type="radio" name="fundreserv_report" id="fndresrvcost" value="FRCR"/></TD>
                </tr>
                <tr><td>&nbsp;</td></tr>
                <tr id="fundrsrvinfo_params" style="display: none;">
                    <td>
                    <table>
                        <tr>
                            <TD class="TitleCell"><span class="LabelText" ><%= Resource.getProperty("scm.pm.param1", lang) %></span></TD>
                            <TD class="TextBox_ContentCell"><input type="text" name="inpParam1" id="inpParam1" maxlength="80" value='' required="true" class="dojoValidateValid required TextBox_TwoCells_width"/></TD>
                        </tr>
                        <tr>
                            <TD class="TitleCell"><span class="LabelText" ><%= Resource.getProperty("scm.pm.param2", lang) %></span></TD>
                            <TD class="TextBox_ContentCell"><input type="text" name="inpParam2" id="inpParam2" maxlength="80" value='' required="true" class="dojoValidateValid required TextBox_TwoCells_width"/></TD>
                        </tr>
                        <tr>
                            <TD class="TitleCell"><span class="LabelText" ><%= Resource.getProperty("scm.pm.param3", lang) %></span></TD>
                            <TD class="TextBox_ContentCell"><input type="text" name="inpParam3" id="inpParam3" maxlength="80" value='' required="true" class="dojoValidateValid required TextBox_TwoCells_width"/></TD>
                        </tr>
                        <tr>
                            <TD class="TitleCell"><span class="LabelText" ><%= Resource.getProperty("scm.pm.param4", lang) %></span></TD>
                            <TD class="TextBox_ContentCell"><input type="text" name="inpParam4" id="inpParam4" maxlength="80" value='' required="true" class="dojoValidateValid required TextBox_TwoCells_width"/></TD>
                        </tr>
                        <tr>
                            <TD class="TitleCell"><span class="LabelText" ><%= Resource.getProperty("scm.pm.param5", lang) %></span></TD>
                            <TD class="TextBox_ContentCell"><input type="text" name="inpParam5" id="inpParam5" maxlength="80" value='' required="true" class="dojoValidateValid required TextBox_TwoCells_width"/></TD>
                        </tr>
                        <tr>
                            <TD class="TitleCell"><span class="LabelText" ><%= Resource.getProperty("scm.pm.param6", lang) %></span></TD>
                            <TD class="TextBox_ContentCell"><input type="text" name="inpParam6" id="inpParam6" maxlength="80" value='' required="true" class="dojoValidateValid required TextBox_TwoCells_width"/></TD>
                        </tr>
                    </table>
                    </td>                   
                </tr>
                <tr id="fundrsrvcost_params" style="display: none;">
                    <td>
                    <table>
                        <tr>
                            <TD class="TitleCell"><span class="LabelText" ><%= Resource.getProperty("scm.pm.param1", lang) %></span></TD>
                            <TD class="TextBox_ContentCell"><input type="text" name="inpParamCost1" id="inpParamCost1" maxlength="80" value='<%= Utility.nullToEmpty(request.getSession().getAttribute("inpParamCost1")) %>' required="true" class="dojoValidateValid TextBox_btn_OneCell_width required"/></TD>
                        </tr>
                        <tr>
                            <TD class="TitleCell"><span class="LabelText" ><%= Resource.getProperty("scm.pm.param2", lang) %></span></TD>
                            <TD class="TextBox_ContentCell"><input type="text" name="inpParamCost2" id="inpParamCost2" maxlength="80" value='<%= Utility.nullToEmpty(request.getSession().getAttribute("inpParamCost2")) %>' required="true" class="dojoValidateValid required TextBox_TwoCells_width"/></TD>
                        </tr>
                        <tr>
                            <TD class="TitleCell"><span class="LabelText" ><%= Resource.getProperty("scm.pm.param3", lang) %></span></TD>
                            <TD class="TextBox_ContentCell"><input type="text" name="inpParamCost3" id="inpParamCost3" maxlength="80" value='<%= Utility.nullToEmpty(request.getSession().getAttribute("inpParamCost3")) %>' required="true" class="dojoValidateValid required TextBox_TwoCells_width"/></TD>
                        </tr>
                        <tr>
                            <TD class="TitleCell"><span class="LabelText" ><%= Resource.getProperty("scm.pm.param4", lang) %></span></TD>
                            <TD class="TextBox_ContentCell"><input type="text" name="inpParamCost4" id="inpParamCost4" maxlength="80" value='<%= Utility.nullToEmpty(request.getSession().getAttribute("inpParamCost4")) %>' required="true" class="dojoValidateValid required TextBox_TwoCells_width"/></TD>
                        </tr>
                        <tr>
                            <TD class="TitleCell"><span class="LabelText" ><%= Resource.getProperty("scm.pm.param5", lang) %></span></TD>
                            <TD class="TextBox_ContentCell"><input type="text" name="inpParamCost5" id="inpParamCost5" maxlength="80" value='<%= Utility.nullToEmpty(request.getSession().getAttribute("inpParamCost5")) %>' required="true" class="dojoValidateValid required TextBox_TwoCells_width"/></TD>
                        </tr>                       
                    </table>
                    </td>                   
                </tr> 
            </table></td>               
            </tr> --%>               
            <TR> 
              <TD class="Button_CenterAlign_ContentCell"  id="Submit_inp_td" colspan="5">
              <DIV id="newDiscard">
              <DIV id="Submit_inp">           
                 <BUTTON type="button" id="Submit_linkBTN" class="ButtonLink" onclick="GenerateReport();">
                 <TABLE class="Button">
                   <TR>
                     <TD class="Button_left"><IMG class="Button_Icon Button_Icon_process" alt="Submit" title="Submit" src="../web/images/blank.gif" border="0"></IMG></TD>
                     <TD class="Button_text" id="Submit_BTNname"><%= Resource.getProperty("scm.printreport.submit",lang)%></TD>
                     <TD class="Button_right"></TD>          
                   </TR>
                 </TABLE>
               </BUTTON>
             </DIV>
             </DIV>
             </TD>
           </TR> 
            </table>                    
              </td></TR>
            </TABLE>        
          </DIV>
        </TD>
       </TR>
      </TABLE>
 </FORM>
</BODY>
<script type="text/javascript">onLoadDo();</script>
<script type="text/javascript">
    /* $(document).ready(function(){
        var fundResrv = $( "input:radio[name=fundreserv_report]" );
        fundResrv.on( "change", function() {            
             //$("input[name='fundreserv_report']:checked").val();
             if($(this).val()==='FRIR'){                 
                 $('#fundrsrvinfo_params').show();
                 $('#fundrsrvcost_params').hide();
             }else if($(this).val()==='FRCR'){               
                 $('#fundrsrvcost_params').show();
                 $('#fundrsrvinfo_params').hide();
             } 
        });     
    }); */ 
    function closePopUp() {
        document.frmMain.action="<%=request.getContextPath()%>/sa.elm.ob.scm.ad_process.printreport/PrintReport?action=Close&pageType="+"<%= request.getAttribute("pageType") %>";
        document.frmMain.submit();
    }
    function GenerateReport(){
        $("#action").val("DownloadReport");       
        <%-- if($('#inpReceivingType').val()=='IR') {
            if($("#inpSupplierDeliverer").val()==null || $("#inpSupplierDeliverer").val()==''){
                OBAlert("<%= Resource.getProperty("scm.printreport.suppdelivnotempty",lang)%>");
                return false;
            }
            submitCommandForm('DEFAULT', false, null, '<%=request.getContextPath()%>/sa.elm.ob.scm.ad_process.printreport/PrintReport?report=InitialReceive', '_self', null, false);
        }
        else --%> 
        if($('#inpWindowID').val()=='184')  { //PO Receipt
             if($('#inpReceivingType').val()=='DEL') { 
                 submitCommandForm('DEFAULT', false, null, '<%=request.getContextPath()%>/sa.elm.ob.scm.ad_process.printreport/PrintReport?report=InventoryDelivery', 'background_target', null, false);
             }
             else if($('#inpReceivingType').val()=='SR') {
                 submitCommandForm('DEFAULT', false, null, '<%=request.getContextPath()%>/sa.elm.ob.scm.ad_process.printreport/PrintReport?report=SiteDelivery', 'background_target', null, false);
             }
             else if($('#inpReceivingType').val()=='INS') {
                 submitCommandForm('DEFAULT', false, null, '<%=request.getContextPath()%>/sa.elm.ob.scm.ad_process.printreport/PrintReport?report=Inspection', 'background_target', null, false);
             }
        }else if($('#inpWindowID').val()=='D8BA0A87790B4B67A86A8DF714525736')  { //Material Issue Req
            if ($('#inpOutputType').val()=='DM') {
            	$.post('<%=request.getContextPath()%>/MIRAjax', {
                    recordId : "<%= request.getAttribute("inpRecordId")%>"
                }, function(data) {
                    var result = JSON.parse(data);
                    if (result.username !== "" && result.printedDate !== "") {
                        OBConfirm(result.username + " "+"<%= Resource.getProperty("scm.mir.report.printedin",lang)%>" + " "+ result.printedDate + ", "+"<%= Resource.getProperty("scm.mir.report.printagain",lang)%>", function(result){
                            if (result) {
                                submitCommandForm('DEFAULT', false, null, '<%=request.getContextPath()%>/sa.elm.ob.scm.ad_process.printreport/PrintReport?report=MaterialReq', 'background_target', null, false);           
                            } else
                                return false;
                        });
                    } else {
                    	submitCommandForm('DEFAULT', false, null, '<%=request.getContextPath()%>/sa.elm.ob.scm.ad_process.printreport/PrintReport?report=MaterialReq', 'background_target', null, false);
                    }                   
                });
            } else {
            	submitCommandForm('DEFAULT', false, null, '<%=request.getContextPath()%>/sa.elm.ob.scm.ad_process.printreport/PrintReport?report=MaterialReq', 'background_target', null, false);
            }
            
            return false;
        }else if($('#inpWindowID').val()=='E509200618424FD099BAB1D4B34F96B8')  { //Bid management
            submitCommandForm('DEFAULT', false, null, '<%=request.getContextPath()%>/sa.elm.ob.scm.ad_process.printreport/PrintReport?report=BidManagement', 'background_target', null, false);           
        }else if($('#inpWindowID').val()=='62E42B7D4CF74BF08532F18D5AF084FD')  { //Open Envelope Committee
            submitCommandForm('DEFAULT', false, null, '<%=request.getContextPath()%>/sa.elm.ob.scm.ad_process.printreport/PrintReport?report=OpenEnvCmt', 'background_target', null, false);           
        }else if($('#inpWindowID').val()=='CAF2D3EEF3B241018C8F65E8F877B29F')  { //Proposal mgmt            
            if($("input[name='fundreserv_report']:checked").val()==undefined){
                OBAlert("<%= Resource.getProperty("scm.pm.pleaseselectonereport",lang)%>");
                return false;
            }else if($("input[name='fundreserv_report']:checked").val()=='FRIR'){
                if($('#inpParam1').val() =='' || $('#inpParam2').val()=='' || $('#inpParam3').val()=='' || $('#inpParam4').val()=='' 
                        || $('#inpParam5').val()=='' || $('#inpParam6').val()==''){
                    OBAlert("<%= Resource.getProperty("scm.pm.mandatoryfieldsnotempty",lang)%>");
                    return false;
                }
            }else if($("input[name='fundreserv_report']:checked").val()=='FRCR'){
                if($('#inpParamCost1').val()=='' || $('#inpParamCost2').val()=='' || $('#inpParamCost3').val()=='' || $('#inpParamCost4').val()=='' 
                        || $('#inpParamCost5').val()==''){
                    OBAlert("<%= Resource.getProperty("scm.pm.mandatoryfieldsnotempty",lang)%>");
                    return false;
                }
            }
            submitCommandForm('DEFAULT', false, null, '<%=request.getContextPath()%>/sa.elm.ob.scm.ad_process.printreport/PrintReport?report=ProposalMgmt', 'background_target', null, false);        
        }    
        closePage();
        return true;
    }
</script>
</HTML>
