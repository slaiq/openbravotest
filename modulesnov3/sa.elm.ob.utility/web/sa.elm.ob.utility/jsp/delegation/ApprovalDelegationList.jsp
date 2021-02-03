<%@page import="sa.elm.ob.utility.properties.Resource, sa.elm.ob.utility.ad_forms.delegation.vo.ApprovalDelegationVO"%>
<%@ page import="java.util.List,java.util.ArrayList" errorPage="/web/jsp/ErrorPage.jsp" %>
<%
    String lang = ((String)session.getAttribute("#AD_LANGUAGE"));
    String style="../web/skins/ltr/org.openbravo.userinterface.skin.250to300Comp/250to300Comp/Openbravo_ERP.css";
    if(lang.equals("ar_SA")){
     style="../web/skins/rtl/org.openbravo.userinterface.skin.250to300Comp/250to300Comp/Openbravo_ERP_250.css";
    }
%>
<HTML xmlns:="http://www.w3.org/1999/xhtml">
  <HEAD>
    <META http-equiv="Content-Type" content="text/html; charset=utf-8">
    <TITLE><%= Resource.getProperty("utility.approvaldelegation.requestapprovaldelegation", lang)%></TITLE>
    <LINK rel="stylesheet" type="text/css" href="<%=style %>" id="paramCSS"></LINK>
    <link rel="stylesheet" type="text/css" href="../web/js/jquery/jquery-ui-1.8.11.custom.css"></link>
    <link rel="stylesheet" type="text/css" href="../web/js/jquery/ui.jqgrid.css"></link>
        <link rel="stylesheet" type="text/css" href="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.picker.css"></link>
    <link rel="stylesheet" type="text/css" href="../web/js/common/htabs.css"></link>
    <script type="text/javascript" src="../web/js/common/common.js"></script>
    <script type="text/javascript" src="../web/js/utils.js"></script>
    <script type="text/javascript" src="../web/js/windowKeyboard.js"></script>
   
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
        
        var deleteDel = "<%= (request.getAttribute("Delete")==null?"":request.getAttribute("Delete")) %>";
        if(deleteDel!="")
        {
            if(deleteDel=="1")
                SuccessMessage("Request approval delegation successfully deleted");         
            else if(deleteDel=="0"){
                var message= "<%=Resource.getProperty("utility.approvaldelegation.delete.error",lang)%>";
                ErrorMessage(message);
            }
                
        }
    }
    function SuccessMessage(message)
    {
         document.getElementById("messageBoxID").style.display = "";
         document.getElementById("messageBoxID").className="MessageBoxSUCCESS";
         document.getElementById("messageBoxIDTitle").innerHTML="<%=Resource.getProperty("construction.success",lang).replaceAll("\"","\"")%>";
         document.getElementById("messageBoxIDMessage").innerHTML=message;
    }

    function ErrorMessage(message)
    {
        document.getElementById("messageBoxID").style.display = "";
         var ID=document.getElementById("messageBoxID");
         ID.className="MessageBoxERROR";
         ID2=document.getElementById("messageBoxIDTitle");
         ID3=document.getElementById("messageBoxIDMessage");
         ID2.innerHTML="<%=Resource.getProperty("construction.error",lang).replaceAll("\"","\"")%>";
         ID3.innerHTML=message;
    }
    </script>

</HEAD>
<BODY onload="onLoadDo();" onresize="onResizeDo();" leftmargin="0" topmargin="0" marginwidth="0" marginheight="0" id="paramBody" >
<FORM id="form" method="post" action="" name="frmMain">
  <INPUT type="hidden" name="Command"></INPUT>
  <INPUT type="hidden" name="IsPopUpCall" value="1"></INPUT>
  <INPUT type="hidden" name="inpLastFieldChanged"></INPUT>
  <INPUT type="hidden" name="inpAction" id="inpAction" value=""></INPUT>  
  <INPUT type="hidden" name="inpSave" id="inpSave" value=""></INPUT>
  <INPUT type="hidden" name="headerId" id="headerId" value="<%=request.getAttribute("headerId")%>"></INPUT>
        
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
              <td width="2%"><a id="linkButtonNew" onblur="this.hideFocus=false" onclick="onClickNew()" onmouseout="window.status='';return true;" onmouseover="window.status='<%=Resource.getProperty("construction.new",lang)%>';return true;" class="Main_ToolBar_Button" href="javascript:void(0);"><img border="0" id="buttonNew" title='<%=Resource.getProperty("construction.new",lang)%>' src="../web/images/blank.gif" class="Main_ToolBar_Button_Icon Main_ToolBar_Button_Icon_New"></a></td>
              <td class="Main_ToolBar_Separator_cell" ><img src="../web/images/blank.gif" class="Main_ToolBar_Separator"></td> 
              <td width="2%" ><a href="javascript:void(0);" onClick="onClickEditView()" class="Main_ToolBar_Button_disabled" onMouseOver="window.status='Form View';return true;" onMouseOut="window.status='';return true;" onclick="this.hideFocus=true" onblur="this.hideFocus=false" id="buttonEdition"><img class="Main_ToolBar_Button_Icon Main_ToolBar_Button_Icon_Edition_disabled" src="../web/images/blank.gif" title="<%= Resource.getProperty("construction.formview",lang) %>" border="0" id="linkButtonEdition"></a></td>
              <%-- <td width="2%" ><a href="javascript:void(0);" onClick="onClickEditView()" class="Main_ToolBar_Button" onMouseOver="window.status='<%= Resource.getProperty("construction.formview",lang).replaceAll("\"", "\\\\\\\"")%>';return true;" onMouseOut="window.status='';return true;" onclick="this.hideFocus=true" onblur="this.hideFocus=false" id="buttonEdition"><img class="Main_ToolBar_Button_Icon Main_ToolBar_Button_Icon_Edition" src="../web/images/blank.gif" title="<%= Resource.getProperty("construction.formview",lang).replaceAll("\"", "\\\\\\\"")%>" border="0" id="linkButtonEdition"></a></td> --%>
              <td class="Main_ToolBar_Separator_cell" ><img src="../web/images/blank.gif" class="Main_ToolBar_Separator"></td>            
              <td width="2%" ><a href="javascript:void(0);" onClick="onClickDelete()" class="Main_ToolBar_Button_disabled" onMouseOver="window.status='Delete Record';return true;" onMouseOut="window.status='';return true;" id="buttonDelete"><img class="Main_ToolBar_Button_Icon Main_ToolBar_Button_Icon_Delete_disabled" src="../web/images/blank.gif" title="<%= Resource.getProperty("construction.delete", lang) %>" border="0" id="linkButtonDelete"></a></td>
              <td class="Main_ToolBar_Separator_cell" ><img src="../web/images/blank.gif" class="Main_ToolBar_Separator"></td>
              <td width="2%" ><a href="javascript:void(0);" onClick="onClickRefresh()" class="Main_ToolBar_Button" onMouseOver="window.status='<%= Resource.getProperty("construction.reload",lang).replaceAll("\"", "\\\\\\\"")%>';return true;" onMouseOut="window.status='';return true;" onclick="this.hideFocus=true" onblur="this.hideFocus=false" id="buttonRefresh"><img class="Main_ToolBar_Button_Icon Main_ToolBar_Button_Icon_Refresh" src="../web/images/blank.gif" title="<%= Resource.getProperty("construction.reload",lang).replaceAll("\"", "\\\\\\\"")%>" border="0" id="linkButtonRefresh"></a></td>
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
                    <div id="GridView" style="width:100%;">
                        <div align="center" id="Head_Div" >
                            <table id="headerList" class="scroll" cellpadding="0" cellspacing="0" width="100%"></table>
                            <div id="pager" class="scroll" style="text-align: center;"></div>
                        </div>                  
                        <table id="Doc_FG" align="center" style="margin-top: 20px;margin-left: 17px;margin-right: 0px;display: none;" width="97.5%" cellspacing="0" cellpadding="0">
                            <TR>
                              <TD colspan="">
                                <TABLE class="FieldGroup" cellspacing="0" cellpadding="0" border="0">
                                <TBODY>
                                  <TR class="FieldGroup_TopMargin"></TR>
                                  <TR>
                                    <TD class="FieldGroupTitle_Left"><IMG class="FieldGroupTitle_Left_bg" border="0" src="../web/images/blank.gif"></IMG></TD>
                                    <TD class="FieldGroupTitle"><%= Resource.getProperty("utility.delagateddocuments", lang)%></TD>
                                    <TD class="FieldGroupTitle_Right"><IMG class="FieldGroupTitle_Right_bg" border="0" src="../web/images/blank.gif"></IMG></TD>
                                    <TD class="FieldGroupContent"></TD>
                                  </TR>
                                  <TR class="FieldGroup_BottomMargin"></TR>
                                </TBODY>
                                </TABLE>
                              </TD>
                            </TR>
                        </table>
                        <div align="center" id="Doc_Div" style="display: none;">
                            <table id="DocList" class="scroll" cellpadding="0" cellspacing="0" width="100%"></table>
                            <div id="pager1" class="scroll" style="text-align: center;"></div>
                        </div>                      
                    </div>
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

<script type="text/javascript" src="../web/js/jquery-1.7.2.min.js"></script>
<script type="text/javascript" src="../web/js/ui/jquery.ui.core.js"></script>
<script type="text/javascript" src="../web/js/jquery/grid.locale-en.js"></script>
<script type="text/javascript" src="../web/js/jquery/jquery.jqGrid.min.js"></script>
<script type="text/javascript" src="../web/js/ui/jquery.ui.datepicker.js"></script>

<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.plugin.js"></script>
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.ummalqura.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.ummalqura-ar.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.plus.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.picker.js"></script> 
<script type="text/javascript" src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.picker.ext.js"></script>
<script type="text/javascript">

function enableEditButton(flag)
{
    if(flag=="true")
    {           
        document.getElementById('buttonEdition').className = 'Main_ToolBar_Button';
        document.getElementById('linkButtonEdition').className = 'Main_ToolBar_Button_Icon Main_ToolBar_Button_Icon_Edition';           
    }
    else if(flag=='false')
    {           
        document.getElementById('buttonEdition').className = 'Main_ToolBar_Button_disabled';
        document.getElementById('linkButtonEdition').className = 'Main_ToolBar_Button_Icon Main_ToolBar_Button_Icon_Edition_disabled';          
    }
}
function enableDeleteButton(flag)
{	
    if(flag=="true")
    {  
    	document.getElementById('buttonDelete').className = 'Main_ToolBar_Button';
        document.getElementById('linkButtonDelete').className = 'Main_ToolBar_Button_Icon Main_ToolBar_Button_Icon_Delete';	    	        
    }
    else if(flag=='false')
    {               
        document.getElementById('buttonDelete').className = 'Main_ToolBar_Button_disabled';
        document.getElementById('linkButtonDelete').className = 'Main_ToolBar_Button_Icon Main_ToolBar_Button_Icon_Delete_disabled';
    }
}
function onClickDelete()
{           
    if(document.getElementById("headerId").value!='' && document.getElementById('buttonDelete').className == 'Main_ToolBar_Button' && document.getElementById("headerId").value!=null)
    {   	
        
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
        
        
    }
}
function onClickEditView(){
    if(document.getElementById("headerId").value!='' && document.getElementById('buttonEdition').className == 'Main_ToolBar_Button' && document.getElementById("headerId").value!=null && document.getElementById("headerId").value!='null')
    {
        document.getElementById("inpAction").value = "EditView";
        reloadWindow();
    }   
} 
function onClickRefresh()
{
    document.getElementById("inpAction").value = "GridView";    
    reloadWindow();
}
function onClickNew()
{
    document.getElementById("inpAction").value = "EditView";
    document.getElementById("headerId").value = ""; 
    reloadWindow();
}
function reloadWindow()
{
    submitCommandForm('DEFAULT', true, null, '<%=request.getContextPath()%>/sa.elm.ob.utility.ad_forms.delegation.header/ApprovalDelegation', '_self', null,true);
    return false;
}

function reSizeGrid()
{
    var w, h;
    if (window.innerWidth)  { w = window.innerWidth - 56; h = window.innerHeight - 390; }
    else if (document.body)  { w = document.body.clientWidth - 56; h = document.body.clientHeight - 390; }      
    
    jQuery("#headerList").setGridWidth(w,true);
    jQuery("#headerList").setGridHeight(h,true);
    
    jQuery("#DocList").setGridWidth(w,true);
    jQuery("#DocList").setGridHeight(120,true);
}  
var gridIds=0;
var gridIdsLength=0;
var lastSelectedId = "";

jQuery(function() 
{
    /* Header Grid */
    jQuery("#headerList").jqGrid(
    {
        url:"<%=request.getContextPath()%>/sa.elm.ob.utility.ad_forms.delegation.ajax/ApprovalDelegationAjax?inpAction=getHeaderList",
        mtype: 'POST',          
        colNames:['<%=Resource.getProperty( "utility.fromdate", lang) %>','<%=Resource.getProperty( "utility.todate", lang) %>', '<%=Resource.getProperty( "utility.fromuser", lang) %>','<%=Resource.getProperty( "utility.fromrole", lang) %>', 'Processed'],              
        colModel:[                    
              {name:'from_date', index:'from_date',search:true, searchoptions: {
                dataInit: function (e) {
                $(e).calendarsPicker({
                    calendar: $.calendars.instance('ummalqura'),
                    dateFormat: 'dd-mm-yyyy',
                    showTrigger:  
                        '<img src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/calendar.gif" alt="Popup" class="trigger">',
                    changeMonth : true,
                    changeYear : true,
                    onClose : function(dateText, inst) {
                        if (dateText != "")
                            jQuery("#headerList")[0].triggerToolbar();
                    }
                });
                e.style.width="55%"; 
                setTimeout("$('#gs_from_date').before('<select onchange=\"searchDate(\\\'FD\\\');\" style=\"width: 40%; padding: 0px; margin: 2px 0px 0px; height: 17px;\" class=\"Combo\" id=\"gs_from_date_s\"><option value=\"=\"><%=Resource.getProperty( "utility.approvaldelegation.equalto", lang) %></option><option value=\">=\"><%=Resource.getProperty( "utility.approvaldelegation.GreaterthanorEqualto", lang) %></option><option value=\"<=\"><%=Resource.getProperty( "utility.approvaldelegation.LesserthanorEqualto", lang) %></option></select>');", 10);
            }              
              }},
              {name:'to_date', index:'to_date',search:true, searchoptions: {
                    dataInit: function (e) {
                        $(e).calendarsPicker({
                            calendar: $.calendars.instance('ummalqura'),
                            dateFormat: 'dd-mm-yyyy',
                            showTrigger:  
                                '<img src="../web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/calendar.gif" alt="Popup" class="trigger">',
                            changeMonth : true,
                            changeYear : true,
                            onClose : function(dateText, inst) {
                                if (dateText != "")
                                    jQuery("#headerList")[0].triggerToolbar();
                            }
                        });
                    e.style.width="55%";    
                    setTimeout("$('#gs_to_date').before('<select onchange=\"searchDate(\\\'TD\\\');\" style=\"width: 40%; padding: 0px; margin: 2px 0px 0px; height: 17px;\" class=\"Combo\" id=\"gs_to_date_s\"><option value=\"=\"><%=Resource.getProperty( "utility.approvaldelegation.equalto", lang) %></option><option value=\">=\"><%=Resource.getProperty( "utility.approvaldelegation.GreaterthanorEqualto", lang) %></option><option value=\"<=\"><%=Resource.getProperty( "utility.approvaldelegation.LesserthanorEqualto", lang) %></option></select>');", 10);
                }}},
              {name:'name', index:'name',search:true},
              {name:'rolename', index:'rolename',search:true},
              {name:'processed', index:'processed',hidden:true}
             ],
  
        rowNum:50,
        rowList:[50,100],
        pager: '#pager',
        datatype: 'xml',
        rownumbers: true,
        viewrecords: true,
        autowidth: true,
        shrinkToFit: true,    
        sortname: 'from_date',
        sortorder: "desc",
        height:"100%",
        pgtext:'<%=Resource.getProperty( "utility.approvaldelegation.page", lang) %> {0} <%=Resource.getProperty( "utility.of", lang) %> {1}',
        recordtext:'<%=Resource.getProperty( "utility.approvaldelegation.view", lang) %> {0} - {1} <%=Resource.getProperty( "utility.of", lang) %> {2}',
        beforeRequest: function()
        { 
            document.getElementById("Doc_Div").style.display="none";
            document.getElementById("Doc_FG").style.display="none";     
            document.getElementById("headerId").value="";
            enableEditButton("false");
            enableDeleteButton("false");
            if ("" + jQuery("#headerList").getPostDataItem("_search") == "true") {
                if ("" + jQuery("#headerList").getPostDataItem("from_date") != "") {
                    var date = "" + jQuery("#headerList").getPostDataItem("from_date");
                    var validformat = OBValidateDate(date);
                    if (!validformat)
                        jQuery("#headerList").setPostDataItem("from_date", "");
                }
                if ("" + jQuery("#headerList").getPostDataItem("to_date") != "") {
                    var date = "" + jQuery("#headerList").getPostDataItem("to_date");
                    var validformat = OBValidateDate(date);
                    if (!validformat)
                        jQuery("#headerList").setPostDataItem("to_date", "");
                }
                jQuery("#headerList").setPostDataItem("from_date_s", document.getElementById("gs_from_date_s").value);
                jQuery("#headerList").setPostDataItem("to_date_s", document.getElementById("gs_to_date_s").value);
            }
        },
        loadComplete: function() 
        {           
            lastSelectedId = "";
            var ids = jQuery("#headerList").getDataIDs();
            var idCount = ids.length;
            if(idCount==0)      
                document.getElementById("headerId").value = ""; 
            else if(document.getElementById("headerId").value != null && document.getElementById("headerId").value != "null")
            {
                jQuery("#headerList").setSelection(document.getElementById("headerId").value);
                getDocumentTypes(document.getElementById("headerId").value);        
            }   
            ChangeJQGridAllRowColor(jQuery("#headerList"));         
            reSizeGrid();
        },
        onSelectRow: function(id, status)
        {
            if(id && id!=lastSelectedId)
            { 
                document.getElementById("headerId").value=id;  
                var rowData = jQuery("#headerList").getRowData(id);    
                var processed=rowData['processed'];
                
                getDocumentTypes(id);
                lastSelectedId=id; 
                if(status==true){
                    enableEditButton("true");
                    if(processed=="N")
                        enableDeleteButton("true");
                }
                else{
                    enableEditButton("false");
                    enableDeleteButton("false");
                }
            }
        },
        ondblClickRow:function(id)
        {
            onClickEditView();
        },      
        caption:''
     
    }); 
    jQuery("#headerList").jqGrid('navGrid','#pager',{edit:false,add:false,del:false,search:false,view: false,beforeRefresh:function(){          
        document.getElementById("Doc_Div").style.display="none";
        document.getElementById("Doc_FG").style.display="none";     
        document.getElementById("headerId").value="";
        enableEditButton("false");
        enableDeleteButton("false");
            
        jQuery('#headerList')[0].clearToolbar();
        jQuery('#headerList')[0].triggerToolbar();
        jQuery("#headerList").trigger("reloadGrid");    
        
        },
    },{ },{ },{ }, { });
    jQuery("#headerList").jqGrid('filterToolbar',{searchOnEnter: false});
    jQuery('#headerList')[0].clearToolbar();
    jQuery('#headerList')[0].triggerToolbar();
    reSizeGrid();
    /* Header Grid completed */
    
    /* Doc Type Grid */
    jQuery("#DocList").jqGrid(
    {       
        mtype: 'POST',          
        colNames:['<%=Resource.getProperty( "utility.accesscontrol.documentrule.docmenttype", lang) %>','<%=Resource.getProperty( "utility.touser", lang) %>','ToUsrSel','UsrId','Type','ToRolSel','RolId','<%=Resource.getProperty( "utility.torole", lang) %>'],              
        colModel:[                    
              {name:'document_type', index:'document_type',search:false},
              {name:'name', index:'name',search:false},
              {name:'usrsel', index:'usrId',search:false, hidden:true},
              {name:'usrId', index:'usrId',search:false, hidden:true},
              {name:'type', index:'type',search:false, hidden:true},
              {name:'rolsel', index:'rolsel',search:false, hidden:true},              
              {name:'roleId', index:'roleId',search:false, hidden:true},
              {name:'rolename', index:'rolename',search:false}
             ],
  
        rowNum:50,
        rowList:[50,100],
        pager: '#pager1',
        datatype: 'xml',
        rownumbers: true,       
        viewrecords: true,
        autowidth: true,
        shrinkToFit: true,
        sortname: 'document_type', 
        sortorder: "asc",
        height:"100%",          
        pgtext:'<%=Resource.getProperty( "utility.approvaldelegation.page", lang) %> {0} <%=Resource.getProperty( "utility.of", lang) %> {1}',
        recordtext: "<%=Resource.getProperty( "utility.approvaldelegation.view", lang) %> {0} - {1} <%=Resource.getProperty( "utility.of", lang) %> {2}",
        loadComplete:function()
        {
            reSizeGrid();
        },
        caption:''
    });    
    jQuery("#DocList").jqGrid('navGrid','#pager1',{edit:false,add:false,del:false,search:false,view: false,beforeRefresh:function(){            
            jQuery("#DocList").trigger("reloadGrid");
            reSizeGrid();
        }
    },{ },{ },{ }, { });
    reSizeGrid();   
    });
/* Doc Type Grid */
function searchDate(type) {
    if (type == "FD" && document.getElementById("gs_from_date").value != "")
        jQuery("#headerList")[0].triggerToolbar();
    else if (type == "TD" && document.getElementById("gs_to_date").value != "")
        jQuery("#headerList")[0].triggerToolbar();
}
function getDocumentTypes(headerId){
    if(headerId!=""){
        document.getElementById("Doc_Div").style.display="";
        document.getElementById("Doc_FG").style.display="";
    
        jQuery("#DocList").setGridParam({url:"<%=request.getContextPath()%>/sa.elm.ob.utility.ad_forms.delegation.ajax/ApprovalDelegationAjax?inpAction=getDocList&headerId="+headerId+"&type=G"});
        jQuery("#DocList").trigger("reloadGrid");
    }
}
</script>
</HTML>