// Add Date Format
var script = document.createElement('script');
script.src = "../web/js/common/DateUtilFn.js";
script.type = 'text/javascript';
document.getElementsByTagName('head')[0].appendChild(script);
var defaultJSDateFormat = 'dd-MM-yyyy', defaultJSDateTimeFormat = 'dd-MM-yyyy HH:mm:ss';
var errReturnDateFormat = 'Invalid Date', jsDateFormatRE = new Array();
// 23-10-2015 23:59:59
jsDateFormatRE[0] = "^([\\d]{1,2})\\-([\\d]{1,2})\\-([\\d]{4})([\\s]{1}([\\d]{1,2})\\:([\\d]{1,2})\\:([\\d]{1,2})){0,1}$";
// 23-10-15 23:59:59
jsDateFormatRE[1] = "^([\\d]{1,2})\\-([\\d]{1,2})\\-([\\d]{2})([\\s]{1}([\\d]{1,2})\\:([\\d]{1,2})\\:([\\d]{1,2})){0,1}$";
// 10/23/2015 23:59:59
jsDateFormatRE[2] = "^([\\d]{1,2})\\/([\\d]{1,2})\\/([\\d]{4})([\\s]{1}([\\d]{1,2})\\:([\\d]{1,2})\\:([\\d]{1,2})){0,1}$";
// 10/23/15 23:59:59
jsDateFormatRE[3] = "^([\\d]{1,2})\\/([\\d]{1,2})\\/([\\d]{2})([\\s]{1}([\\d]{1,2})\\:([\\d]{1,2})\\:([\\d]{1,2})){0,1}$";
// 2015-10-23 23:59:59
jsDateFormatRE[4] = "^([\\d]{4})\\-([\\d]{1,2})\\-([\\d]{1,2})([\\s]{1}([\\d]{1,2})\\:([\\d]{1,2})\\:([\\d]{1,2})){0,1}$";
if (parent && parent.parent && parent.parent.parent.parent.OB) {
	defaultJSDateFormat = parent.parent.parent.parent.OB.EUT.defaultJSDateFormat;
	defaultJSDateTimeFormat = parent.parent.parent.parent.OB.EUT.defaultJSDateTimeFormat;
}
else if (window.opener && window.opener.parent && window.opener.parent.parent && window.opener.parent.parent.parent.parent.OB) {
	defaultJSDateFormat = window.opener.parent.parent.parent.parent.OB.EUT.defaultJSDateFormat;
	defaultJSDateTimeFormat = window.opener.parent.parent.parent.parent.OB.EUT.defaultJSDateTimeFormat;
}
else if (parent && parent.parent && parent.parent.parent && parent.parent.parent.parent.parent.OB) {
	defaultJSDateFormat = parent.parent.parent.parent.parent.OB.EUT.defaultJSDateFormat;
	defaultJSDateTimeFormat = parent.parent.parent.parent.parent.OB.EUT.defaultJSDateTimeFormat;
}
function OBValidateDate(date) {
	try {
		if (date instanceof Date) {
			if (date == 'Invalid Date')
				return false;
			else
				return date;
		}
		else {
			var reg = null;
			for ( var i = 0; i < jsDateFormatRE.length; i++) {
				reg = new RegExp(jsDateFormatRE[i]);
				if (reg.test(date)) {
					var cDate = null, yr = 0, mon = 0, day = 0, hr = 0, min = 0, sec = 0;
					var dateArray = date.match(jsDateFormatRE[i]);
					if (i == 0) {
						yr = parseInt(dateArray[3]);
						mon = parseInt(dateArray[2]);
						day = parseInt(dateArray[1]);
						if (dateArray[4]) {
							hr = parseInt(dateArray[5]);
							min = parseInt(dateArray[6]);
							sec = parseInt(dateArray[7]);
						}
					}
					else if (i == 1) {
						yr = parseInt(('' + new Date().getFullYear()).substring(0, 2) + parseInt(dateArray[3]));
						mon = parseInt(dateArray[2]);
						day = parseInt(dateArray[1]);
						if (dateArray[4]) {
							hr = parseInt(dateArray[5]);
							min = parseInt(dateArray[6]);
							sec = parseInt(dateArray[7]);
						}
					}
					else if (i == 2) {
						yr = parseInt(dateArray[3]);
						mon = parseInt(dateArray[1]);
						day = parseInt(dateArray[2]);
						if (dateArray[4]) {
							hr = parseInt(dateArray[5]);
							min = parseInt(dateArray[6]);
							sec = parseInt(dateArray[7]);
						}
					}
					else if (i == 3) {
						yr = parseInt(('' + new Date().getFullYear()).substring(0, 2) + parseInt(dateArray[3]));
						mon = parseInt(dateArray[1]);
						day = parseInt(dateArray[2]);
						if (dateArray[4]) {
							hr = parseInt(dateArray[5]);
							min = parseInt(dateArray[6]);
							sec = parseInt(dateArray[7]);
						}
					}
					else if (i == 4) {
						yr = parseInt(dateArray[1]);
						mon = parseInt(dateArray[2]);
						day = parseInt(dateArray[3]);
						if (dateArray[4]) {
							hr = parseInt(dateArray[5]);
							min = parseInt(dateArray[6]);
							sec = parseInt(dateArray[7]);
						}
					}
					if (mon > 12 || day >= 32 || hr >= 24 || min >= 60 || sec >= 60)
						return false;
					cDate = new Date(yr, (mon - 1), day, hr, min, sec, 0);
					if (cDate == 'Invalid Date' || (cDate.getFullYear() != yr) || (cDate.getMonth() + 1 != mon) || (cDate.getDate() != day) || (cDate.getHours() != hr) || (cDate.getMinutes() != min) || (cDate.getSeconds() != sec))
						return false;
					else
						return cDate;
					break;
				}
			}
			return false;
		}
	} catch (e) {
		console.log(e);
		return false;
	}
}
function OBCompareDate(date1, date2) {
	var fDate1 = OBValidateDate(date1), fDate2 = OBValidateDate(date2);
	if (fDate1 != false && fDate2 != false && fDate1 instanceof Date && fDate2 instanceof Date)
		return fDate1.compareTo(fDate2);
	return false;
}
function OBFormatDate(date, formatter) {
	try {
		var cDate = null;
		if (date instanceof Date) {
			if (date == 'Invalid Date')
				return errReturnDateFormat;
			else
				cDate = date;
		}
		else {
			cDate = OBValidateDate(date);
			if (cDate == false)
				return errReturnDateFormat;
		}
		if (typeof formatter != "undefined")
			return cDate.format(formatter);
		else
			return cDate.format(defaultJSDateFormat);
	} catch (e) {
		console.error(e);
		return date;
	}
}
