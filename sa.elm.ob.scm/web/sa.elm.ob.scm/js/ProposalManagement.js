 

  var url = contextPath + "/sa.elm.ob.scm.ad_process.printreport/PrintReportAjax?action=getReminderSubjectlist";
$.getJSON(url, function (result) {

  }).done(function (data) {

    $("#inpParamReminder2").find("option").remove().end();
    $("#inpParamReminder2").append("<option value='0' selected>select</option>");
    //	$("#inpOrg").append("<option value='0'>*</option>");
    $.each(data, function (i, item) {
      $("#inpParamReminder2").append("<option value='" + item.referencelookuplineid + "'>" + item.reflookuplnvaluename + "</option>");
    });
  });

