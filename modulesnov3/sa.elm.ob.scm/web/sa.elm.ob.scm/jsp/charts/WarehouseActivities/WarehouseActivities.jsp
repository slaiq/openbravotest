<!--
 *************************************************************************
 * All Rights Reserved.
 * Contributor(s): halgadaibi
 ************************************************************************
-->
<%@page import="org.openbravo.client.myob.WidgetURL"%>
<%@page import="org.openbravo.client.myob.WidgetInstance"%>
<%@page import="org.openbravo.dal.core.OBContext"%>
<%@page import="sa.elm.ob.utility.util.Utility"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@page import="sa.elm.ob.finance.properties.Resource"
    errorPage="/web/jsp/ErrorPage.jsp"%>
<%
  String textDir = "LTR";
            String right = "right";
            String left = "left";
            String lang = ((String) session.getAttribute("#AD_LANGUAGE"));
            String style = "../web/skins/ltr/org.openbravo.userinterface.skin.250to300Comp/250to300Comp/Openbravo_ERP.css";
            if (lang.equals("ar_SA") || lang.equals("ar")) {
                // style = "../web/skins/rtl/org.openbravo.userinterface.skin.250to300Comp/250to300Comp/Openbravo_ERP_250.css";
                textDir = "RTL";
                right = "left";
                left = "right";
            }
%>

<HTML>
<HEAD>
<META http-equiv="Content-Type" content="text/html; charset=utf-8"></META>
<TITLE>Warehouse Activites</TITLE>
<LINK rel="shortcut icon" href="../web/images/favicon.ico"
    type="image/x-icon"></LINK>
<LINK rel="stylesheet" type="text/css" href="<%=style%>" id="paramCSS"></LINK>
<script type="text/javascript" id="paramLanguage">var defaultLang="<%=((String) session.getAttribute("#AD_LANGUAGE"))%>";</script>

<style type="text/css">
</style>

  <title>title</title>
  <script src="../web/sa.elm.ob.scm/js/highcharts.js"></script>
  <script src="../web/sa.elm.ob.scm/js/exporting.js"></script>
  <script src="../web/sa.elm.ob.scm/js/drilldown.js"></script>
  <script src="../web/sa.elm.ob.scm/js/jquery-3.2.0.min.js"></script>
  
</head>
<body>

 <div class="radio col-md-offset-5" dir="rtl" align="center">
  <label>
    <input type="radio" name="optionsRadios" id="optionsRadios1" value="option1" checked>
    طلب صرف مواد
  </label>
  <label>
    <input type="radio" name="optionsRadios" id="optionsRadios2" value="option2">
    محضر استلام
  </label>
  <label>
    <input type="radio" name="optionsRadios" id="optionsRadios3" value="option3">
    مستند رجيع
  </label>
  <label>
    <input type="radio" name="optionsRadios" id="optionsRadios4" value="option4">
     صرف أصناف رجيع
  </label>
  <label>
    <input type="radio" name="optionsRadios" id="optionsRadios5" value="option4">
     بيانات عشوائية
  </label>
</div>

<div id="container" style="min-width: 310px; height: 370px; margin: 0 auto"></div>

  <script>

  var chart;



  $('input:radio').on('change',function () {

         if ($("#optionsRadios1").is(":checked")) {
             getMaterialIssueCount();
         }
         else if ($("#optionsRadios2").is(":checked")) {
             getPOReceiptCount();
         }
         else if ($("#optionsRadios3").is(":checked")) {
             getReturnTransactionCount();
         } else if ($("#optionsRadios4").is(":checked")) {
             getIssueReturnTransactionCount();
         } else {
             var thisYear = [];
             var lastYear = [];
             for(var i = 0; i<12; i++){
                 thisYear.push(Math.floor(Math.random() * 20)); 
                 lastYear.push(Math.floor(Math.random() * 20)); 
             }
             drawChart('../web/sa.elm.ob.scm/json/WarehouseActivities/data.json', thisYear, lastYear);
         }
  });






  function drawChart(file, thisYear, lastYear){
        var options = {
            chart: {
                renderTo: 'container'
            },
            plotOptions: {
                  column: {
                      pointPadding: 0.05,
                      borderWidth: 0
                  }
              },
            title: {
              text: ''
            },
            xAxis: {
              categories: [],
              title: {
                text: "الشهر"
              }
            },
            yAxis: {
              min: 0,
              title: {
                text: "العدد"
              }
            },
            tooltip: {
                useHTML: true  
              },
            credits: {
                enabled: false
            },
            series: [{}]
        };

        $.getJSON(file, function(json) {
            var index = 0;
            options.xAxis.categories = json[0]['data'].reverse();
            json[1]['data'] = thisYear.reverse();
            json[2]['data'] = lastYear.reverse();
            options.series[0] = json[1];
            options.series[0].color = '#0f97a6';
            options.series[1] = json[2];
            options.series[1].color = '#d5d5d5';
            for (index = 0; index < json[0]['data'].length; ++index) {
                json[3]['data'][index] = (json[1]['data'][index] + json[2]['data'][index]) / 2;
            }
            options.series[2] = json[3];
            options.series[2].color = '#eb7f00';
            chart = new Highcharts.Chart(options);

        });
  }
  </script>
</body>
<script type="text/javascript">
getMaterialIssueCount();
function getMaterialIssueCount(){
    var url = "<%=request.getContextPath()%>/WarehouseActivitiesAjax?action=getWarehouseActivities";
    $.getJSON(url, function(result){
    }).done(function(data){
            drawChart('../web/sa.elm.ob.scm/json/WarehouseActivities/data.json', data.thisYear, data.lastYear);
    });
}

function getReturnTransactionCount(){
    var url = "<%=request.getContextPath()%>/WarehouseActivitiesAjax?action=getReturnTransaction";
    $.getJSON(url, function(result){
    }).done(function(data){
            drawChart('../web/sa.elm.ob.scm/json/WarehouseActivities/data.json', data.thisYear, data.lastYear);
    });
}

function getIssueReturnTransactionCount(){
    var url = "<%=request.getContextPath()%>/WarehouseActivitiesAjax?action=getIssueReturnTransaction";
    $.getJSON(url, function(result){
    }).done(function(data){
            drawChart('../web/sa.elm.ob.scm/json/WarehouseActivities/data.json', data.thisYear, data.lastYear);
    });
}

function getPOReceiptCount(){
    var url = "<%=request.getContextPath()%>/WarehouseActivitiesAjax?action=getPOReceipt";
    $.getJSON(url, function(result){
    }).done(function(data){
            drawChart('../web/sa.elm.ob.scm/json/WarehouseActivities/data.json', data.thisYear, data.lastYear);
    });
}



</script>
</HTML>