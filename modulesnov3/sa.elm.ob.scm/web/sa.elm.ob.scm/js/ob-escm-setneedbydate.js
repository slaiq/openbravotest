OB.ESCMNeedDate = {};
OB.ESCMNeedDate.ExcludeWeekEnd = {};

OB.ESCMNeedDate.ExcludeWeekEnd.Set_NeedByDate = function (item, view, form, grid) {			
	var calendar = $.calendars.instance("ummalqura");
	var today = calendar.today();	
	var nextday = today.add(1, 'd');
	var isWeekDay = nextday.weekDay();
	//console.log("isWeekDay>"+nextday+" , "+isWeekDay);
	if(isWeekDay){
		var needDate = nextday.add(1, 'd');		
	}else{
		var needDate = nextday;		
	}
	var formatNeedDate = calendar.formatDate("dd-mm-yyyy", needDate);
	//console.log("formdate>"+calendar.formatDate("dd-mm-yyyy", needDate));	
	form.setItemValue('escmNeedbydate', formatNeedDate);
};
