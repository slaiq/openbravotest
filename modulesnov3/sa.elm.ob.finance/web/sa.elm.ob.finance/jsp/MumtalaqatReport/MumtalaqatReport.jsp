<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="sa.elm.ob.scm.ESCMDefLookupsTypeLn"%>
<%@page import="org.openbravo.dal.service.OBCriteria"%>
<%@page import="org.openbravo.dal.service.OBDal"%>
<%@page import="sa.elm.ob.scm.ESCMDefLookupsType"%>
<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
 <%@page import="sa.elm.ob.utility.util.Utility"%>
 <%@ page import="sa.elm.ob.finance.properties.Resource" errorPage="/web/jsp/ErrorPage.jsp"%>
 <%@ page import="java.util.List,java.util.ArrayList" errorPage="/web/jsp/ErrorPage.jsp" %>
 
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
     <link rel="stylesheet" type="text/css" href="../web/js/common/select2.min.css"></link>
    
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
       
        
    }
    function onResizeDo()
    {
        reSizeGrid();
    }
    
    </script>
    
    <script type="text/javascript">
    var contextPath = "<%=request.getContextPath()%>";
    </script>
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
  <INPUT type="hidden" name="inpBeneficiaryId_1" id=inpBeneficiaryId_1 value=""></INPUT>
  <INPUT type="hidden" name="inpRegionId_1" id=inpRegionId_1 value=""></INPUT>
  <INPUT type="hidden" name="inpOutput_1" id=inpOutput_1 value=""></INPUT>
  <INPUT type="hidden" name="inpYearId_1" id="inpYearId_1" value=""></INPUT>
    <INPUT type="hidden" name="inpInvoiceId_1" id="inpInvoiceId_1" value=""></INPUT>
  
  

       
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
                <td class="Popup_InfoBar_text" id="processHelp"><%=Resource.getProperty("finance.MumtalaqatReport.title",lang)%> </td>
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
            <tr id="inpYear">
           <td colspan="2">
                <tr >
                    <TD class="TitleCell" align="right"><SPAN class="LabelText" width=112px> <%=Resource.getProperty("finance.MumtalaqatReport.finyear",lang)%></SPAN></TD>
                    <TD class="TextBox_btn_ContentCell TextBox_ReadOnly_ContentCell">
                        <TABLE border="0" cellpadding="0" cellspacing="0"><TR><TD class="TextBox_ReadOnly_ContentCell">
                        <select id="inpYearId" class="ComboKey Combo_TwoCells_width"   onkeydown="return true;" onchange="document.getElementById('messageBoxID').style.display = 'none';">
                           <option value="0">--select--</option>
                             </select>
                        </TD></TR></TABLE>
                    </TD>
                  
                </tr>
               
                </td>
                </tr>
                <tr id="inpBeneficiary">
           <td colspan="2">
                <tr id="contract_report_select">
                    <TD class="TitleCell" align="right"><SPAN class="LabelText"> <%=Resource.getProperty("finance.MumtalaqatReport.paymentben",lang)%></SPAN></TD>
                    <TD class="TextBox_btn_ContentCell TextBox_ReadOnly_ContentCell">
                        <TABLE border="0" cellpadding="0" cellspacing="0"><TR><TD class="TextBox_ReadOnly_ContentCell">
                         <select id="inpBeneficiaryId" class="ComboKey Combo_TwoCells_width"  onkeydown="return true;" onchange="document.getElementById('messageBoxID').style.display = 'none';getinvoice();">
                         </select>
                       
                        </TD></TR>
                    </TD>                  
                </tr>
            </table>
                </td>
                </tr>
                
                      <tr id="inpinvoice">
           <td colspan="2">
                <tr id="contract_report_select">
                    <TD class="TitleCell" align="right"><SPAN class="LabelText"> <%=Resource.getProperty("finance.MumtalaqatReport.invoice",lang)%></SPAN></TD>
                    <TD class="TextBox_btn_ContentCell TextBox_ReadOnly_ContentCell">
                        <TABLE border="0" cellpadding="0" cellspacing="0"><TR><TD class="TextBox_ReadOnly_ContentCell">
                         <select id="inpinvoiceId" class="ComboKey Combo_TwoCells_width"  onkeydown="return true;" onchange="document.getElementById('messageBoxID').style.display = 'none';"">
                         </select>
                       
                        </TD></TR>
                    </TD>                  
                </tr>
            </table>
                </td>
                </tr>
                
                  <tr id="inpRegion">
           <td colspan="2">
                <tr >
                    <TD class="TitleCell" align="right"><SPAN class="LabelText"> <%=Resource.getProperty("finance.MumtalaqatReport.Region",lang)%></SPAN></TD>
                    <TD class="TextBox_btn_ContentCell TextBox_ReadOnly_ContentCell">
                        <TABLE border="0" cellpadding="0" cellspacing="0"><TR><TD class="TextBox_ReadOnly_ContentCell">
                         <select id="inpRegionId" class="ComboKey Combo_TwoCells_width" onchange="document.getElementById('messageBoxID').style.display = 'none';">
                         </select>
                       
                        </TD></TR>
                    </TD>                  
                </tr>
                </table>
                </td>
                </tr>
                
                
                <tr id="inpRegion">
                   <td colspan="2">
                <tr >
                    <TD class="TitleCell" align="right"><SPAN class="LabelText"> <%=Resource.getProperty("finance.MumtalaqatReport.output",lang)%></SPAN></TD>
                    <TD class="TextBox_btn_ContentCell TextBox_ReadOnly_ContentCell">
                        <TABLE border="0" cellpadding="0" cellspacing="0"><TR><TD class="TextBox_ReadOnly_ContentCell">
                       <SELECT name="inpOutput" class="ComboKey Combo_OneCell_width" id="inpOutput" >
                            <option value="pdf"><%= Resource.getProperty("finance.CEL.PDF", lang) %></option>
                            <option value="xls"><%= Resource.getProperty("finance.CEL.EXCEL",lang)%></option>                        
                        </SELECT>
                       
                        </TD></TR>
                    </TD>                  
                </tr>
                 
                </table>
                </td>
                </tr>
                 
           
                
                <td colspan="2"><table>
   
                <tr><td>&nbsp;</td></tr>
          
                </table>
                </td>
                </tr>

                <tr><td>&nbsp;</td></tr>
                </table>
                </td>
                </tr>
            </table> 
               <div id="GridView" style="width:100%; display: none;">
                   <div align="center"><table id="LookUpsList" class="scroll" cellpadding="0" cellspacing="0" width="100%"></table>
                       <div id="pager" class="scroll" style="text-align: center; height:30px;"></div>
                   </div>
               </div>               
              <div id="Submit_inp" style="width:100%; text-align: center; margin: 10px 0px;">        
                 <BUTTON type="button" id="Submit_linkBTN" class="ButtonLink" onclick="GenerateReport();">
                 <TABLE class="Button">
                   <TR>
                     <TD class="Button_left"><IMG class="Button_Icon Button_Icon_process" alt="Submit" title="Submit" src="../web/images/blank.gif" border="0"></IMG></TD>
                     <TD class="Button_text" id="Submit_BTNname"> <%=Resource.getProperty("finance.printreport.submit",lang)%></TD>
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
<script type="text/javascript" src="../web/js/common/select2.min.js"></script>
<script type="text/javascript" src="../web/js/jquery/jquery.jqGrid.min.js"></script>
<script type="text/javascript" src="../web/js/common/select2.min.js"></script>


<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.plugin.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.ummalqura.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.ummalqura-ar.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.plus.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.picker.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.picker.ext.js"></script>
<script type="text/javascript">
var type="", lkups = "";
var lkpGrid = jQuery("#LookUpsList");
var lookup = {};
lookup.Lookup = [];
var gridIds, gridIdsLength;
$(document).ready(function(){
   lkpGrid.trigger("reloadGrid");
   $('#GridView').show();
   $('#Submit_linkBTN').show();
   var setData = new Option("--select--", "0", true, true);
   $('#inpYearId').append(setData).trigger('change');   
   $('#inpBeneficiaryId').append(setData).trigger('change');
   $('#inpinvoiceId').append(setData).trigger('change');
  }); 
  
$( "#inpBeneficiaryId").select2(selectBoxAjaxPaging({size: "small", placeholder: true}));
$( "#inpYearId").select2(selectBoxAjaxPaging({size: "small", placeholder: true}));
$( "#inpRegionId").select2(selectBoxAjaxPaging({size: "small", placeholder: true}));
$( "#inpinvoiceId").select2(selectBoxAjaxPaging({size: "small", placeholder: true}));



function getbeneficiary(){
    document.getElementById("inpBeneficiaryId").value = "0";
    document.getElementById("inpinvoiceId").value = "0";

        $("#inpBeneficiaryId").select2(selectBoxAjaxPaging({
            url : function() {
                return '<%=request.getContextPath()%>/MumtalaqatReportAjax?action=getbeneficiary'
            },
            size : "small"
        }));
    return true;
} 


function getinvoice(){
    document.getElementById("inpinvoiceId").value = "0";
        $("#inpinvoiceId").select2(selectBoxAjaxPaging({
            url : function() {
                return '<%=request.getContextPath()%>/MumtalaqatReportAjax?action=getinvoice&&bpId='+document.getElementById("inpBeneficiaryId").value
            },
            size : "small"
        }));
    return true;
} 


function getfinancialyear(){
    document.getElementById("inpYearId").value = "0";
        $("#inpYearId").select2(selectBoxAjaxPaging({
            url : function() {
                return '<%=request.getContextPath()%>/MumtalaqatReportAjax?action=getyear'
            },
            size : "small"
        }));
    return true;
} 

function getRegion(){
    document.getElementById("inpRegionId").value = "0";
        $("#inpRegionId").select2(selectBoxAjaxPaging({
            url : function() {
                return '<%=request.getContextPath()%>/MumtalaqatReportAjax?action=getregion'
            },
            size : "small"
        }));
    return true;
} 

        
    function reSizeGrid()
    {
        var w, h;
        if (window.innerWidth)  { w = window.innerWidth; h = window.innerHeight; }
        else if (document.body)  { w = document.body.clientWidth; h = document.body.clientHeight; }
        document.getElementById('client').style.height = ((h - 20) + 'px');
        w = w - 50;
        h = h - 500;
        if (w <= 600)
            w = 600;
        if (h <= 200)
            h = 150;
        lkpGrid.setGridWidth(w, true);
        lkpGrid.setGridHeight(h, true);
        
    }
    jQuery(function() 
    {
        lkpGrid.jqGrid(
        {
                url:'<%=request.getContextPath()%>/MumtalaqatReportAjax?action=getLookUpsList',
                colNames:['<%= Resource.getProperty("finance.pm.seqno",lang)%>', '<%= Resource.getProperty("finance.pm.LookUp",lang)%>'],
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

        if(lkups!="")
        {
            var idList=lkups.split(",");
            lookup.Lookup=[];
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
        document.frmMain.action="<%=request.getContextPath()%>/sa.elm.ob.scm.ad_process.printreport/MumtalaqatReport?action=Close&pageType="+"<%= request.getAttribute("pageType") %>";
        document.frmMain.submit();
    }
    function GenerateReport(){
        $("#action").val("DownloadReport");  
        document.getElementById("action").value = "DownloadReport";
        document.getElementById("inpBeneficiaryId_1").value = document.getElementById("inpBeneficiaryId").value;
        document.getElementById("inpRegionId_1").value = document.getElementById("inpRegionId").value;
        document.getElementById("inpYearId_1").value = document.getElementById("inpYearId").value;
        document.getElementById("inpOutput_1").value = document.getElementById("inpOutput").value
        document.getElementById("inpInvoiceId_1").value = document.getElementById("inpinvoiceId").value;

       
        if( $("#inpYearId").val()=='0' || $("#inpBeneficiaryId").val()=='0'){
              OBAlert("<%= Resource.getProperty("finance.mumtalaqat.mandatoryfields",lang)%>");
              return false;  
        }      
        
        if(addLookups()){  
            if(lkups=="")
            {     
                OBAlert("<%= Resource.getProperty("finance.CEL.selectatleastonerow",lang)%>");
                return false;  
            }
            else if(validateDuplicate(lookup.Lookup)==1){
                OBAlert("<%= Resource.getProperty("finance.pm.validateduplicateseqno",lang)%>");
                return false;
            }                       
        }
     
            submitCommandForm('DEFAULT', false, null, '<%=request.getContextPath()%>/sa.elm.ob.finance.ad_reports.Mumtalaqat/MumtalaqatReport', 'background_target', null, false);        
            closePage();
            return true;
        
    }
    

getbeneficiary(); 
getinvoice();
getfinancialyear();
getRegion();
</script>
</HTML>