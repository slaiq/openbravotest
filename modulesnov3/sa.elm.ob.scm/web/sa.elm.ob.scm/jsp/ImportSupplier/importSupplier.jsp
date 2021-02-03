 <%@ page import="sa.elm.ob.scm.properties.Resource" errorPage="/web/jsp/ErrorPage.jsp"%>
  <%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
  <%     
    String lang = ((String)session.getAttribute("#AD_LANGUAGE"));

 %>
<HTML>
<head>
    <script type="text/javascript" src="../web/js/utils.js"></script>
    <script type="text/javascript" src="../web/js/common/common.js"></script>
    

    
    <style>
        .buttonStyle {
            margin-top: 10px;
            background-color: #0f97a6;
            color: white;
            display: inline-block;
            padding: 6px 12px;
            margin-bottom: 0;
            font-size: 14px;
            font-weight: 400;
            line-height: 1.42857143;
            text-align: center;
            white-space: nowrap;
            vertical-align: middle;
            -ms-touch-action: manipulation;
            touch-action: manipulation;
            cursor: pointer;
            -webkit-user-select: none;
            -moz-user-select: none;
            -ms-user-select: none;
            user-select: none;
            background-image: none;
            border: 1px solid transparent;
        }
        
        .supplierSection {
            margin-left: 35%;
            margin-top: 35px;
        }
        
        .buttonsDiv {
            margin-left: 15px;
            margin-top: 30px;
        }
        
        #supplierCRN {
            margin-top: 10px;
            display: block;
            width: 100%;
            height: 34px;
            padding: 6px 12px;
            font-size: 14px;
            line-height: 1.42857143;
            color: #555;
            background-color: #fff;
            background-image: none;
            border: 1px solid #ccc;
            border-radius: 4px;
            -webkit-box-shadow: inset 0 1px 1px rgba(0,0,0,.075);
            box-shadow: inset 0 1px 1px rgba(0,0,0,.075);
            -webkit-transition: border-color ease-in-out .15s,-webkit-box-shadow ease-in-out .15s;
            -o-transition: border-color ease-in-out .15s,box-shadow ease-in-out .15s;
            transition: border-color ease-in-out .15s,box-shadow ease-in-out .15s;
        }
        
        .titleAR {
            margin-right: 40px;
        }
        
        .inputDivAR {
            margin-right: 20px;
        }
        
        .buttonsDivAR {
            margin-top: 30px;
            margin-right: 115px;
        }
        
        .supplierSectionAR {
            margin-left: 20%;
            margin-top: 35px;
            margin-right: 15%;
        }
       
    </style>
</head>
<body>
<FORM name="frmMain" method="post" action="">
  <INPUT type="hidden" name="Command"></INPUT>  
  <INPUT type="hidden" name="action" id="action" value=""></INPUT>
  
  <INPUT type="hidden" name="inpTabId" id=inpTabId value="<%= request.getAttribute("inpTabId")%>"></INPUT>
  <INPUT type="hidden" name="inpWindowID" id="inpWindowID" value="<%=request.getAttribute("inpWindowID")%>"></INPUT>
  <INPUT type="hidden" name="inpRecordId" id=inpRecordId value="<%= request.getAttribute("inpRecordId")%>"></INPUT>
  <INPUT type="hidden" name="inpReceivingType" id=inpReceivingType value="<%= request.getAttribute("inpReceivingType")%>"></INPUT>
  <INPUT type="hidden" name="inpDocNo" id=inpDocNo value="<%= request.getAttribute("inpDocNo")%>"></INPUT>
  <div class="supplierSection" id="supplierSec">
        <div>
            <h2 class="title"><%=Resource.getProperty("scm.import.supplier.title",lang)%></h2>
        </div>
        <%--  <div><%=Resource.getProperty("scm.import.supplier.input.label",lang)%></div>  --%>
        <div>
            <div class="inputDiv">
                <label for="supplierCRN"><%=Resource.getProperty("scm.import.supplier.input.label",lang)%></label>
                <input name="supplierCRN" id="supplierCRN" type="text">
            
                <div class="buttonsDiv">
                    <button type="submit" class="buttonStyle"><%=Resource.getProperty("scm.import.supplier.input.submit",lang)%></button>
                    <button onclick="closePage();" class="buttonStyle"><%=Resource.getProperty("scm.import.supplier.input.cancel",lang)%>
                    </button>
                </div>
            </div>
        </div>
        </div>
    </form>
</body>
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

    $("form").submit(function(e) {
        e.preventDefault();
    
        var self = this,
            tray = $('select[name=tray_id]').val();
    
        $.ajax({
            type:'GET',
            url:"<%=request.getContextPath()%>/sa.elm.ob.scm.ad_process.importsupplier/ImportSupplier",            
            data:{action:"importSupplier",supplierCRN :document.getElementById('supplierCRN').value},
            dataType:'json',
            async:false,
            success:function(data) {
                    if (data.result == "Success") {
                        var successMessage= data.message;
                        var tabId = window.parent.parent.OB.MainView.TabSet.getSelectedTab().ID
                        window.parent.parent.OB.MainView.TabSet.getTab(tabId).pane.activeView.dataSource.view.messageBar.setMessage('success', successMessage);
                        window.parent.parent.OB.MainView.TabSet.getTab(tabId).pane.activeView.dataSource.view.refresh();
                        closePage();
                    } else if (data.result == "Warning" ){
                        var warningMessage=data.message;
                        var tabId = window.parent.parent.OB.MainView.TabSet.getSelectedTab().ID
                        window.parent.parent.OB.MainView.TabSet.getTab(tabId).pane.activeView.dataSource.view.messageBar.setMessage('warning', warningMessage);
                        window.parent.parent.OB.MainView.TabSet.getTab(tabId).pane.activeView.dataSource.view.refresh();
                        closePage();                        
                    }                 
                    else if (data.result == "Error") {
                        OBAlert(data.message);
                    }
                }
         });
    });
</script>

    <script type="text/javascript">
       if ("<%=lang %>" == "ar_SA") {
           document.getElementById("supplierSec").dir = "rtl";
           addArabicClasses("title");
           addArabicClasses("inputDiv");
           addArabicClasses("buttonsDiv");
           addArabicClasses("supplierSection");
       }
       
       function addArabicClasses(className) {
           $("."+className).addClass(className + "AR");
           $("."+className).removeClass(className);
       }
    </script>
</HTML>