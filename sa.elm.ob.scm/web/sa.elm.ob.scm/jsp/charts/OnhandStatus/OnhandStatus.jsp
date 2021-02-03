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
<TITLE>Onhand Status</TITLE>
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

<!--  
 <div class="radio col-md-offset-5" dir="rtl" align="center">
  <label>
    <input type="radio" name="optionsRadios" id="optionsRadios1" value="option1" checked>
    1
  </label>
  <label>
    <input type="radio" name="optionsRadios" id="optionsRadios2" value="option2">
    2
  </label>
  <label>
    <input type="radio" name="optionsRadios" id="optionsRadios3" value="option3">
    3
  </label>
</div>-->

<div id="container" style="min-width: 310px; height: 400px; margin: 0 auto"></div>

  <script>

  var chart;

 // drawChart('../web/sa.elm.ob.scm/json/OnhandStatus/dataDrill.json');


  function drawChart(json){
      var options = {
          chart: {
              renderTo: 'container',
              type: 'column'
          },
          plotOptions: {
                column: {
                    pointPadding: 0.05,
                    borderWidth: 0
                }
            },
          title: {
            text: 'حالة المخزون'
          },
          legend: {
              enabled: false
          },
          credit: {
              enabled: false
          },
          xAxis: {
            type: 'category',
            title: {
              text: "الأصناف"
            }
          },
          yAxis: {
            min: 0,
            title: {
              text: "الكمية"
            }
          },
          tooltip: {
            useHTML: true  
          },
          credits: {
              enabled: false
          },
          series: [{}]
         // drilldown:{series:[{}]}
      };


      options.series[0] = json;
      options.series[0].colors = ['#d5d5d5', '#0f97a6']
 /*     for(var i = 0; i < json.length - 1; i++){
          options.drilldown.series[i] = json[i];
      } */
      chart = new Highcharts.Chart(options);

}
  </script>
</body>
<script type="text/javascript">
getOnhandStatus();
function getOnhandStatus(){
    var url = "<%=request.getContextPath()%>/OnhandStatusAjax?action=getOnhandStatus";
    $.getJSON(url, function(result){
    }).done(function(data){
        var jsonFirstLevel = {
                "name": "count",
                "colorByPoint": true,
                "data": []
              };
        var jsonData = [];
         $.each(data, function(i, row){
             var catagoryObject = {
                     "name": "",
                     "y": ""
                   };
             catagoryObject.name = row[0];
             catagoryObject.y = row[1];
             jsonData.push(catagoryObject);
         });
         jsonFirstLevel.data = jsonData;
         console.log(jsonFirstLevel);
         drawChart(jsonFirstLevel);
        /*        console.table(data);
        var jsonData = [];
        var jsonFirstLevel = {
              "name": "Types",
              "colorByPoint": true,
              "data": []
            };
        $.each(data, function(i, row){
            var catagoryExists = false;
            var catagoryObject = {
                    "name": row[6],
                    "y": row[9],
                    "drilldown": row[6] 
                  };
            var itemObject = {
                    "name": "",
                    "id": "",
                    "data": []
                  };
            if(i == 0){
                jsonFirstLevel.data[i] = catagoryObject;
                itemObject.name = row[6];
                itemObject.id = row[6];
                itemObject.data.push([row[4], row[9]]);
                jsonData.push(itemObject);
            } else {
                $.each(jsonFirstLevel.data, function(j, catagory){
                    if(catagory.name == row[6]){
                        jsonFirstLevel.data[j].y += row[9];
                        catagoryExists = true;
                        jsonData[j].data.push([row[4], row[9]]);  
                    }
                });
                if(!catagoryExists){
                    jsonFirstLevel.data.push(catagoryObject);
                    itemObject.name = row[6];
                    itemObject.id = row[6];
                    itemObject.data.push([row[4], row[9]]);
                    jsonData.push(itemObject);
                }
            }       
        });
        jsonData[jsonData.length] = jsonFirstLevel;
        drawChart(jsonData); */
    });
}
</script>
</HTML>