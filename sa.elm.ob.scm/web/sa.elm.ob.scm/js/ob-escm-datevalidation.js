OB.ESCMDate = {};
OB.ESCMDate.dateValidation	 = {};

OB.ESCMDate.dateValidation.compareto = function (item, view, form, grid) { 
	var needbyDate = form.getItem('needByDate').getValue();
	var calendar = $.calendars.instance("ummalqura");
	var needbyGreg=	 calendar.newDate(needbyDate.getFullYear(), (needbyDate.getMonth()+1), needbyDate.getDate());
	needbyGreg= calendar.toJSDate(needbyGreg);
	var currentDate=new Date();
	currentDate.setHours(0, 0, 0, 0);
	if(needbyGreg.getTime() < currentDate.getTime()){
		grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, OB.I18N.getLabel('ESCM_MatReqLine_NeedbyDate'));
		form.setItemValue('needByDate', null);
		    return false;
		    }
};
