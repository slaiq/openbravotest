function getShortHijriDayNames() {
	var names = [];
	names = ['السبت','الجمعة','الخميس','الأربعاء','الثلاثاء','الإثنين','الأحد'];
	return names;
}

function getHijriDayTitles() {
	var names = [];
	names = ['السبت','الجمعة','الخميس','الأربعاء','الثلاثاء','الإثنين','الأحد'];
	return names.reverse();
}

function getUmmAlQuaraWeekendDays() {
	var days = [];
	days = [ 5, 6 ];
	return days;
}

function getShortHijriDayTitles(_number) {
	var _title = "";
	switch (key) {
	case 0:
		_title = "الأحد";
		break;
	case 1:
		_title = "الإثنين";
		break;
	case 2:
		_title = "الثلاثاء";
		break;
	case 3:
		_title = "الأربعاء";
		break;
	case 4:
		_title = "الخميس";
		break;
	case 5:
		_title = "الجمعة";
		break;
	case 6:
		_title = "السبت";
		break;

	default:
		break;
	}

	return _title;
}

function getCurrentHijriDate() {
	return convertGregorianToHijri(new Date(), 'D', 'D', '+0');
}

function getShortHijriDayName(hijriDate) {
	var shortMonthName = "";
	switch (hijriDate.getUTCMonth()) {
	case 0:
		shortMonthName = "محرم";
		break;
	case 1:
		shortMonthName = "صفر";
		break;
	case 2:
		shortMonthName = "ربيع الأول";
		break;
	case 3:
		shortMonthName = "ربيع الثاني";
		break;
	case 4:
		shortMonthName = "جمادى الأول";
		break;
	case 5:
		shortMonthName = "جمادى الآخرة";
		break;
	case 6:
		shortMonthName = "رجب";
		break;
	case 7:
		shortMonthName = "شعبان";
		break;
	case 8:
		shortMonthName = "رمضان";
		break;
	case 9:
		shortMonthName = "شوال";
		break;
	case 10:
		shortMonthName = "ذو القعدة";
		break;
	case 11:
		shortMonthName = "ذو الحجة";
		break;
	default:
		break;
	}

	return shortMonthName;
}

function setCalendarDays(_number) {
	if (_number == 0)
		_number = 5;
	else if (_number == 1)
		_number = 6;
	else
		_number = _number - 2;
	return _number;
}

function getMonth(_date) {
	var hijriDate = convertGregorianToHijri(_date, 'D', 'D', '+1');
	return hijriDate.split("/")[0] - 1;
}

function getHijriMonthStartDate(_date) {
	var hijriDate = convertGregorianToHijri(_date, 'D', 'D', '+0');
	return (parseInt(hijriDate.split("/")[0]) + 1) + "/1/" + hijriDate.split("/")[2];
}

function getNumberOfMonths(_month) {
	var noOfDays;

	switch (hijriDate.getUTCMonth()) {
	case 0:
		noOfDays = 30;
		break;
	case 1:
		noOfDays = 29;
		break;
	case 2:
		noOfDays = 30;
		break;
	case 3:
		noOfDays = 30;
		break;
	case 4:
		noOfDays = 29;
		break;
	case 5:
		noOfDays = 29;
		break;
	case 6:
		noOfDays = 30;
		break;
	case 7:
		noOfDays = 29;
		break;
	case 8:
		noOfDays = 30;
		break;
	case 9:
		noOfDays = 29;
		break;
	case 10:
		noOfDays = 29;
		break;
	case 11:
		noOfDays = 30;
		break;
	default:
		noOfDays = 30;
		break;
	}
	return noOfDays;
}

function getJSDate(_year, _month, _date) {
	var date = new Date();

	date.setFullYear(_year);
	date.setMonth(_month);
	date.setDate(_date);

	return date;
}

function getShortUmmalQuraMonth(_month){
	var shortMonths = ['محرم', 'صفر', 'ربيع الأول', 'ربيع الثاني', 'جمادى الأول', 'جمادى الآخرة', 'رجب', 'شعبان', 'رمضان', 'شوال', 'ذو القعدة', 'ذو الحجة'];
	return shortMonths[_month-1];
}