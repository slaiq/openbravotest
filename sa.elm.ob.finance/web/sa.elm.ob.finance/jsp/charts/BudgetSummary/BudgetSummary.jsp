<!--
 *************************************************************************
 * All Rights Reserved.
 * Contributor(s): Gopalakrishnan
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
<TITLE>Budget Summary</TITLE>
<LINK rel="shortcut icon" href="../web/images/favicon.ico"
	type="image/x-icon"></LINK>
<LINK rel="stylesheet" type="text/css" href="<%=style%>" id="paramCSS"></LINK>
<SCRIPT language="JavaScript" src="../utility/DynamicJS.js"
	type="text/javascript"></SCRIPT>
<script type="text/javascript" id="paramLanguage">var defaultLang="<%=((String) session.getAttribute("#AD_LANGUAGE"))%>";</script>
<SCRIPT language="JavaScript" src="../web/js/utils.js"
	type="text/javascript"></SCRIPT>
<SCRIPT language="JavaScript" src="../web/js/windowKeyboard.js"
	type="text/javascript"></SCRIPT>
<script type="text/javascript" src="../web/js/default/DateTextBox.js"></script>
<script type="text/javascript" src="../web/js/jscalendar/calendar.js"></script>
<script type="text/javascript"
	src="../web/js/jscalendar/lang/calendar-lang.js"></script>
<script type="text/javascript" src="../web/js/common/common.js"></script>
<script type="text/javascript"
	src="http://ajax.googleapis.com/ajax/libs/jquery/1.9.1/jquery.min.js"></script>
<script src="../web/sa.elm.ob.finance/js/highcharts.js"
	type="text/javascript"></script>
<script src="../web/sa.elm.ob.finance/js/exporting.js"
	type="text/javascript"></script>
<script src="../web/sa.elm.ob.finance/js/drilldown.js"
	type="text/javascript"></script>
<link rel="stylesheet"
	href="../web/sa.elm.ob.finance/js/bootstrap/css/bootstrap.min.css">
<link rel="stylesheet"
	href="../web/sa.elm.ob.finance/js/bootstrap/css/bootstrap-theme.min.css">
<script src="../web/sa.elm.ob.finance/js/bootstrap/js/bootstrap.min.js"
	type="text/javascript"></script>

<style type="text/css">
.margin {
	margin-left: auto;
	margin-right: auto;
}

body {
	overflow-y: auto;
	overflow-x: auto;
}

#load {
	width: 100%;
	height: 100%;
	position: fixed;
	z-index: 9999;
	background:
		url("https://www.creditmutuel.fr/cmne/fr/banques/webservices/nswr/images/loading.gif")
		no-repeat center center rgba(0, 0, 0, 0.25)
}

@media (min-width: 768px) {
    .navbar .navbar-nav {
        display: inline-block;
        float: none;
    }

    .navbar .navbar-collapse {
        text-align: center;
    }
}


.btn:focus {
  outline: none;
}
</style>


</head>
<body>

	<!-- <div id="load"></div>
 -->



	<nav class="navbar navbar-default navbar-static-top">
		<div class="container">
			<div class="btn-group" style="margin-left: 45%">
				<button type="button" class="btn btn-default navbar-btn" aria-label="Left Align" data-toggle="modal" data-target="#myModal">
					<span class="glyphicon glyphicon-edit" aria-hidden="true"></span>
				</button>

				<button type="button" class="btn btn-default navbar-btn" aria-label="right Align" onclick="getPreferences();">
					<span class="glyphicon glyphicon-refresh"></span>
				</button>
			</div>
		</div>
	</nav>

	<!-- Modal -->
	<div id="myModal" class="modal fade" role="dialog">
		<div class="modal-dialog">

			<!-- Modal content-->
			<div class="modal-content">
				<div class="modal-header">
					<button type="button" class="close" data-dismiss="modal">&times;</button>
				</div>
				<div class="modal-body">

					<form class="form-horizontal" action="">
						<div class="form-group">
							<label class="control-label col-sm-2" for="inpOrg"> <%=Resource.getProperty("finance.organization", lang)
					.replace("'", "\\\'")%></label>
							<div class="col-sm-10">
								<select id="inpOrg" class="form-control"
									onchange="generateYear(this.value); validateForm();">
								</select>
							</div>
						</div>
						<div class="form-group">
							<label class="control-label col-sm-2" for="inpYear"><%=Resource.getProperty(
					"finance.budgetdetailreport.field.budgetyr", lang).replace(
					"'", "\\\'")%></label>
							<div class="col-sm-10">
								<select id="inpYear" class="form-control"
									onchange="validateForm();">
								</select>
							</div>
						</div>
						<div class="form-group">
							<label class="control-label col-sm-2" for="inpBudgetType"><%=Resource.getProperty(
					"finance.budgetdetailreport.field.budgettyp", lang)
					.replace("'", "\\\'")%></label>
							<div class="col-sm-10">
								<select id="inpBudgetType" class="form-control"
									onchange="validateForm();">
								</select>
							</div>
						</div>

						<div class="form-group">
							<div class="col-sm-offset-2 col-sm-10">
								<button type="button" onclick="getData();"
									class="btn btn-default" style="margin-left: 35%">save</button>
							</div>
						</div>
					</form>
				</div>

			</div>

		</div>
	</div>


	<div id="contents" class="container-fluid center">
		<div class="row">
			<div class="col-sm-6">
				<div class="card card-block yellow">
					<p class="card-text"></p>
					<div id="chartContainer_21"></div>
					<p></p>

				</div>
			</div>
			<div class="col-sm-6">
				<div class="card card-block">
					<p class="card-text"></p>
					<div id="chartContainer_22"></div>
					<p></p>
				</div>
			</div>

			<div class="col-sm-6">
				<div class="card card-block">
					<p class="card-text"></p>
					<div id="chartContainer_31"></div>
					<p></p>

				</div>
			</div>

		</div>
	</div>
</body>
<script type="text/javascript">

// auto refresh every 5 minutes
// setInterval(function(){ getPreferences(); }, 300000);


/* show loadig icon
document.onreadystatechange = function () {
	  var state = document.readyState
	  if (state == 'interactive') {
	       document.getElementById('contents').style.visibility="hidden";
	  } else if (state == 'complete') {
	      setTimeout(function(){
	         document.getElementById('interactive');
	         document.getElementById('load').style.visibility="hidden";
	         document.getElementById('contents').style.visibility="visible";
	      },1000);
	  }
	}
*/
var chart_21 = null;
var chart_22 = null;
var chart_31 = null;



getPreferences();

$("[data-toggle='toggle']").click(function() {
    var selector = $(this).data("target");
    $(selector).toggleClass('in');
});

$('#errorMsg').hide();
//load Organization
var url = "<%=request.getContextPath()%>/BudgetSummaryAjax?action=getOrganization";
$.getJSON(url, function(result){
	
}).done(function(data){

	$("#inpOrg")
		.find("option")
		.remove()
		.end();
	
	var select = "<%=Resource.getProperty("finance.select", lang)%>";
	$("#inpOrg").append("<option value='0' selected>"+select+"</option>");
//	$("#inpOrg").append("<option value='0'>*</option>");
	$.each( data, function( i, item ) {
		$("#inpOrg").append("<option value='"+item.OrgId+"'>"+item.OrgName+"</option>");
      });
});

function validateForm(){
	
	var select = "<%=Resource.getProperty("finance.select", lang)%>";
	var selectionOK = true;
    $('select').each(function(){
        if( $(this).val() == select || $(this).val() == null || $(this).val() == '0'){
            $(this).delay(3000).css("background", "#f1dbdb");
            selectionOK = false;
        }
        else {
            $(this).delay(3000).css("background", "#fff");
        }
     });
   
    if(selectionOK){
    	return true;
    }
    else {
    	return false;
    }

}
function generateYear(orgId){
	//load budget Type
	var url = "<%=request.getContextPath()%>/BudgetSummaryAjax?action=getBudgetType";
	$.getJSON(url, function(result){
		
	}).done(function(data){

		$("#inpBudgetType")
   		.find("option")
   		.remove()
   		.end();
		
		var select = "<%=Resource.getProperty("finance.select", lang)%>";
		$("#inpBudgetType").append("<option value='0' selected>"+select+"</option>");
		$.each( data, function( i, item ) {
			$("#inpBudgetType").append("<option value='"+item.bTypeId+"'>"+item.bTypeName+"</option>");
	      });
	});
    var url = "<%=request.getContextPath()%>/BudgetSummaryAjax?action=getYear&orgId="+orgId;
	$.getJSON(url, function(result){
		
	}).done(function(data){

		$("#inpYear")
   		.find("option")
   		.remove()
   		.end();
		
		var select = "<%=Resource.getProperty("finance.select", lang)%>";
		$("#inpYear").append("<option value='0' selected>"+select+"</option>");
		$.each( data, function( i, item ) {
			$("#inpYear").append("<option value='"+item.YearId+"'>"+item.YearName+"</option>");
	      });
	});
	
}


<%-- function generateAccountElement(campaignId){
	//load account Element
	var url = "<%=request.getContextPath()%>/BudgetSummaryAjax?action=getAccountElement&budgetTypeId="+campaignId;
	$.getJSON(url, function(result){
		
	}).done(function(data){

		$("#inpElement")
   		.find("option")
   		.remove()
   		.end();
		
		var select = "<%=Resource.getProperty("finance.select", lang)%>";
		$("#inpElement").append("<option value='0' selected>"+select+"</option>");
		$.each( data, function( i, item ) {
			$("#inpElement").append("<option value='"+item.ElementId+"'>"+item.ElementName+"</option>");
	      });
	});
}
 --%>
function generateAccounts(elementId){
	//load accounts
	var url = "<%=request.getContextPath()%>/BudgetSummaryAjax?action=getAccounts&elementId="+elementId;
	$.getJSON(url, function(result){
		
	}).done(function(data){

		$("#inpAccount")
   		.find("option")
   		.remove()
   		.end();
		
		var select = "<%=Resource.getProperty("finance.select", lang)%>";
		$("#inpAccount").append("<option value='0' selected>"+select+"</option>");
		$.each( data, function( i, item ) {
			$("#inpAccount").append("<option value='"+item.ElementId+"'>"+item.ElementName+"</option>");
	      });
	});
}

function getPreferences(){
    //load prefs

    var url = "<%=request.getContextPath()%>/BudgetSummaryAjax?action=getPreferences";
    $.getJSON(url, function(data){
        $.each( data, function( i, item ) {
            var budgetLines = new Array();
            var data ="";
            var i=0;
            var path = "<%=request.getContextPath()%>"+ "/BudgetSummaryAjax?action=getBudgetDetails";
                path += "&orgId=" +item.org;
                path += "&budgetTypeId="
                        + item.btype;
                path += "&yearId=" + item.year;

                $.ajax({
                    type : "POST",
                    url : path,
                    dataType : "xml",
                    async : false,
                    success : function(data) {
                        $(data).find("Budget").each(
                                function() {

                                    var line = {
                                        chapter : getChapter($(this).find("code")
                                                .text()),
                                        code : $(this).find("code").text(),
                                        amount : parseFloat($(this).find("amount")
                                                .text()),
                                        currentAmount : parseFloat($(this).find(
                                                "currentAmount").text()),
                                        encAmount : parseFloat($(this)
                                                .find("encAmount").text()),
                                        amountSpent : parseFloat($(this).find(
                                                "amountSpent").text()),
                                        available : parseFloat($(this)
                                                .find("available").text()),
                                        accountName : $(this).find("accountName").text()
                                            
                                    }

                                    budgetLines.push(line);
                                });
                    }
                });
                calculateBudgetPerChapter(budgetLines);
        });
    });
    
}


function savePreferences(preferences){
    //load Projects
    var url = "<%=request.getContextPath()%>/BudgetSummaryAjax?action=saveDefault";
    url +="&preferences="+preferences;
    $.getJSON(url);
}

function getData(){
    
    if(validateForm()){
        
        
        $('#myModal').modal('toggle');
        var budgetLines = new Array();
        var data ="";
        var i=0;
            
        var path  = "<%=request.getContextPath()%>";
			path += "/BudgetSummaryAjax?action=getBudgetDetails";
			path += "&orgId=" + document.getElementById("inpOrg").value;
			path += "&budgetTypeId="
					+ document.getElementById("inpBudgetType").value;
			path += "&yearId=" + document.getElementById("inpYear").value;

			$.ajax({
				type : "POST",
				url : path,
				dataType : "xml",
				async : false,
				success : function(data) {
					$(data).find("Budget").each(
							function() {

								var line = {
									chapter : getChapter($(this).find("code")
											.text()),
									code : $(this).find("code").text(),
									amount : parseFloat($(this).find("amount")
											.text()),
									currentAmount : parseFloat($(this).find(
											"currentAmount").text()),
									encAmount : parseFloat($(this).find(
											"encAmount").text()),
									amountSpent : parseFloat($(this).find(
											"amountSpent").text()),
									available : parseFloat($(this).find(
											"available").text()),
									accountName : $(this).find("accountName")
											.text()
								}
								budgetLines.push(line);
							});
				}
			});
			var preferences = document.getElementById("inpOrg").value + ","
					+ document.getElementById("inpYear").value + ","
					+ document.getElementById("inpBudgetType").value;
			savePreferences(preferences);
			calculateBudgetPerChapter(budgetLines);
		}
	}

	function calculateBudgetPerChapter(budgetLinesArray) {

		var budgetLines21 = new Array();
		var budgetLines22 = new Array();
		var budgetLines31 = new Array();
		var amount21 = 0;
		var currentAmount21 = 0;
		var encAmount21 = 0;
		var amountSpent21 = 0;
		var available21 = 0;
		var amount22 = 0;
		var currentAmount22 = 0;
		var encAmount22 = 0;
		var amountSpent22 = 0;
		var available22 = 0;
		var amount31 = 0;
		var currentAmount31 = 0;
		var encAmount31 = 0;
		var amountSpent31 = 0;
		var available31 = 0;
		var found21 = false;
		var found22 = false;
		var found31 = false;
		for (var i = 0; i < budgetLinesArray.length; i++) {

			if (budgetLinesArray[i].chapter === "21") {

				found21 = true;
				amount21 += budgetLinesArray[i].amount;
				currentAmount21 += budgetLinesArray[i].currentAmount;
				encAmount21 += budgetLinesArray[i].encAmount;
				amountSpent21 += budgetLinesArray[i].amountSpent;
				available21 += budgetLinesArray[i].available;
				budgetLines21.push(budgetLinesArray[i]);

			} else if (budgetLinesArray[i].chapter === "22") {

				found22 = true;
				amount22 += budgetLinesArray[i].amount;
				currentAmount22 += budgetLinesArray[i].currentAmount;
				encAmount22 += budgetLinesArray[i].encAmount;
				amountSpent22 += budgetLinesArray[i].amountSpent;
				available22 += budgetLinesArray[i].available;
				budgetLines22.push(budgetLinesArray[i]);

			} else if (budgetLinesArray[i].chapter === "30"
					|| budgetLinesArray[i].chapter === "40") {

				found31 = true;
				amount31 += budgetLinesArray[i].amount;
				currentAmount31 += budgetLinesArray[i].currentAmount;
				encAmount31 += budgetLinesArray[i].encAmount;
				amountSpent31 += budgetLinesArray[i].amountSpent;
				available31 += budgetLinesArray[i].available;
				budgetLines31.push(budgetLinesArray[i]);
			}
		}

		if (found21) {
			generateChart(parseFloat(currentAmount21), parseFloat(encAmount21),
					parseFloat(amountSpent21), parseFloat(available21),
					'chartContainer_21', budgetLines21);
		} else {
			document.getElementById("chartContainer_21").innerHTML = "";
		}
		if (found22) {
			generateChart(parseFloat(currentAmount22), parseFloat(encAmount22),
					parseFloat(amountSpent22), parseFloat(available22),
					'chartContainer_22', budgetLines22);
		} else {
			document.getElementById("chartContainer_22").innerHTML = "";
		}
		if (found31) {
			generateChart(parseFloat(currentAmount31), parseFloat(encAmount31),
					parseFloat(amountSpent31), parseFloat(available31),
					'chartContainer_31', budgetLines31);
		} else {
			document.getElementById("chartContainer_31").innerHTML = "";
		}

	}

	function generateChart(currentAmount, encAmount, amountSpent, available,
			divId, budgetLines) {
		var chapter;
		var tempChart;
		if (divId === "chartContainer_21") {
			chapter = "21";
		} else if (divId === "chartContainer_22") {
			chapter = "22";
		} else if (divId === "chartContainer_31") {
			chapter = "31";
		}

		var availableAccountsDrillDown = {
			name : 'accounts',
			id : 'availableAccounts',
			showInLegend : true,
			size : '90%',
			data : []
		};
		var utilizedAccountsDrillDown = {
			name : 'accounts',
			id : 'utilizedAccounts',
			showInLegend : true,
			size : '90%',
			data : []
		};

		var utilizedBreakDownDrillDown = {
			name : 'utilized',
			id : 'utilizedBreakDown',
			showInLegend : true,
			size : '90%',
			data : []
		};

		for (line in budgetLines) {

			$
					.merge(
							availableAccountsDrillDown.data,
							[ {
								id : budgetLines[line].accountName,
								name : budgetLines[line].accountName
										+ '<br>'
										+ new Intl.NumberFormat('en')
												.format(parseFloat(budgetLines[line].available)),
								y : (parseFloat(budgetLines[line].available) / parseFloat(available)) * 100

							} ]);
			$
					.merge(
							utilizedAccountsDrillDown.data,
							[ {
								drilldown : 'utilizedBreakDown',
								id : budgetLines[line].accountName,
								name : budgetLines[line].accountName
										+ '<br>'
										+ new Intl.NumberFormat('en')
												.format(parseFloat(budgetLines[line].amountSpent)
														+ parseFloat(budgetLines[line].encAmount)),
								y : ((parseFloat(budgetLines[line].amountSpent) + parseFloat(budgetLines[line].encAmount)) / (parseFloat(encAmount) + parseFloat(amountSpent))) * 100
							} ]);
		}

		Highcharts.theme = {
			colors : [ "#9a908f", "#0f97a6", "#eb7f00" ],
			chart : {
				backgroundColor : null,
				style : {
					fontFamily : "Dosis, sans-serif"
				}
			},
			title : {
				style : {
					fontSize : '16px',
					fontWeight : 'bold',
					textTransform : 'uppercase'
				}
			},
			tooltip : {
				borderWidth : 0,
				backgroundColor : 'rgba(219,219,216,0.8)',
				shadow : false
			},
			legend : {
				itemStyle : {
					fontWeight : 'bold',
					fontSize : '13px'
				}
			},
			xAxis : {
				gridLineWidth : 1,
				labels : {
					style : {
						fontSize : '12px'
					}
				}
			},
			yAxis : {
				minorTickInterval : 'auto',
				title : {
					style : {
						textTransform : 'uppercase'
					}
				},
				labels : {
					style : {
						fontSize : '12px'
					}
				}
			},
			plotOptions : {
				candlestick : {
					lineColor : '#404048'
				}
			},

			// General
			background2 : '#F0F0EA'

		};

		// Apply the theme
		Highcharts.setOptions(Highcharts.theme);
		// Create the chart
		tempChart = $('#' + divId)
				.highcharts(
						{
							chart : {
								type : 'pie',
								events : {
									drilldown : function(e) {
										if (e.seriesOptions.id == 'utilizedBreakDown') {

											for (line in budgetLines) {

												if (e.point.id == budgetLines[line].accountName) {
													utilizedBreakDownDrillDown.data = [
															{

																name : 'spent',
																y : budgetLines[line].amountSpent
																		/ (budgetLines[line].amountSpent + budgetLines[line].encAmount)
																		* 100

															},
															{
																name : 'encumbrance',
																y : budgetLines[line].encAmount
																		/ (budgetLines[line].amountSpent + budgetLines[line].encAmount)
																		* 100
															} ];
												}
											}
										}
									}
								}
							},
							title : {
								text : '( '
										+ chapter
										+ ' )  Total Budget = '
										+ new Intl.NumberFormat('en')
												.format(currentAmount)
							},
							subtitle : {
								text : 'Click the slices to view details.'
							},

							plotOptions : {
								pie : {
									size : '100%',
									dataLabels : {
										enabled : false
									},
									showInLegend : true
								},
								series : {
									dataLabels : {
										enabled : false,
										format : '{point.name}: {point.y:.1f}%'
									}
								}
							},
							legend : {
								enabled : true,
								borderWidth : 1,
								borderColor : 'gray',
								align : 'center',
								verticalAlign : 'top',
								layout : 'horizontal',
								maxHeight : 40,
								x : 0,
								y : 50,
								itemStyle : {
									color : '#000000',
									fontWeight : 'bold',
									fontSize : '14px'
								},
								labelFormatter : function() {
									if (this.name.length > 15) {
										return this.name.split('-')[0];
									} else {
										return this.name;
									}
								}
							},
							tooltip : {
								headerFormat : '<span style="font-size:14px">{series.name}</span><br>',
								pointFormat : '<span style="color:{point.color}; font-size:14px">{point.name}</span>: <b>{point.y:.2f}%</b><br/>'
							},
							series : [ {
								name : 'budget',
								colorByPoint : true,
								data : [
										{
											name : 'Utilized',
											y : ((amountSpent + encAmount) / currentAmount) * 100,
											drilldown : 'utilizedAccounts'
										},
										{
											name : 'Available',
											y : (available / currentAmount) * 100,
											drilldown : 'availableAccounts'
										} ]
							} ],
							credits : {
								enabled : false
							},
							drilldown : {
/* 								drillUpButton : {

									relativeTo : 'spacingBox',
									position : {
										y : 100,
										x : -15
									},
									theme : {
										fill : '#eb7f00',
										states : {
											hover : {
												fill : '#eb7f00'
											}
										}
									}

								}, */
								series : [ availableAccountsDrillDown,
										utilizedAccountsDrillDown,
										utilizedBreakDownDrillDown ]
							}
						});

		if (divId === "chartContainer_21") {
			chart_21 = tempChart;
		} else if (divId === "chartContainer_22") {
			chart_22 = tempChart;
		} else if (divId === "chartContainer_31") {
			chart_31 = tempChart;
		}

	}

	function getPosition(str, m, i) {
		return str.split(m, i).join(m).length;
	}

	function getChapter(code) {
		var thirdSegmentIndex = getPosition(code, '-', 2);
		return code.substring(thirdSegmentIndex + 1, thirdSegmentIndex + 3);
	}

	function getThirdSegment(code) {
		return (code.split('-')[2]);
	}

	
	/*$(window).load(function() {
	    if(chart_21 == null && chart_22 == null && chart_31 == null){
	        $("#myModal").modal("show");
	    }
	});*/
</script>


</HTML>