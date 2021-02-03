var OBDateFormat = function() {
	var token = /d{1,4}|M{1,4}|yy(?:yy)?|([HhmsSTta])\1?|[LloZ]|"[^"]*"|'[^']*'/g, timezone = /\b(?:[PMCEA][DP]T|(?:Pacific|Mountain|Central|Eastern|Atlantic) (?:Standard|Daylight|Prevailing) Time|(?:GMT|UTC)(?:[-+]\d{4})?)\b/g, timezoneClip = /[^-+\dA-Z]/g, pad = function(val, len) {
		val = String(val);
		len = len || 2;
		while (val.length < len)
			val = "0" + val;
		return val;
	};
	// Regexes and supporting functions are cached through closure
	return function(date, mask, utc) {
		var dF = OBDateFormat;
		// You can't provide utc if you skip other args (use the "UTC:" mask
		// prefix)
		if (arguments.length == 1 && Object.prototype.toString.call(date) == "[object String]" && !/\d/.test(date)) {
			mask = date;
			date = undefined;
		}
		// Passing date through Date applies Date.parse, if necessary
		date = date ? new Date(date) : new Date;
		if (isNaN(date))
			throw SyntaxError(errReturnDateFormat);
		mask = String(dF.masks[mask] || mask || dF.masks["default"]);
		// Allow setting the utc argument via the mask
		if (mask.slice(0, 4) == "UTC:") {
			mask = mask.slice(4);
			utc = true;
		}
		var _ = utc ? "getUTC" : "get", d = date[_ + "Date"](), D = date[_ + "Day"](), M = date[_ + "Month"](), y = date[_ + "FullYear"](), H = date[_ + "Hours"](), m = date[_ + "Minutes"](), s = date[_ + "Seconds"](), L = date[_ + "Milliseconds"](), o = utc ? 0 : date.getTimezoneOffset(), flags = {
			d : d,
			dd : pad(d),
			ddd : dF.i18n.dayNames[D],
			dddd : dF.i18n.dayNames[D + 7],
			M : M + 1,
			MM : pad(M + 1),
			MMM : dF.i18n.monthNames[M],
			MMMM : dF.i18n.monthNames[M + 12],
			yy : String(y).slice(2),
			yyyy : y,
			h : H % 12 || 12,
			hh : pad(H % 12 || 12),
			H : H,
			HH : pad(H),
			m : m,
			mm : pad(m),
			s : s,
			ss : pad(s),
			S : pad(L, 3),
			l : pad(L, 3),
			L : pad(L > 99 ? Math.round(L / 10) : L),
			t : H < 12 ? "a" : "p",
			tt : H < 12 ? "am" : "pm",
			T : H < 12 ? "A" : "P",
			TT : H < 12 ? "AM" : "PM",
			a : H < 12 ? "AM" : "PM",
			Z : utc ? "UTC" : (String(date).match(timezone) || [ "" ]).pop().replace(timezoneClip, ""),
			o : (o > 0 ? "-" : "+") + pad(Math.floor(Math.abs(o) / 60) * 100 + Math.abs(o) % 60, 4)
		};
		return mask.replace(token, function($0) {
			return $0 in flags ? flags[$0] : $0.slice(1, $0.length - 1);
		});
	};
}();
// Some common format strings
OBDateFormat.masks = {
	"default" : "ddd MMM dd yyyy HH:mm:ss",
	shortDate : "M/d/yy",
	mediumDate : "MMM d, yyyy",
	longDate : "MMMM d, yyyy",
	fullDate : "dddd, MMMM d, yyyy",
	shortTime : "h:mm TT",
	mediumTime : "h:mm:ss TT",
	longTime : "h:mm:ss TT Z",
	isoDate : "yyyy-MM-dd",
	isoTime : "HH:mm:ss",
	isoDateTime : "yyyy-MM-dd'T'HH:mm:ss",
	isoUtcDateTime : "UTC:yyyy-MM-dd'T'HH:mm:ss'Z'"
};
// Internationalization strings
OBDateFormat.i18n = {
	dayNames : [ "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday" ],
	monthNames : [ "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec", "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December" ]
};
// For date format
Date.prototype.format = function(mask, utc) {
	return OBDateFormat(this, mask, utc);
};
// For date validate
Date.prototype.validate = function() {
	return OBValidateDate(this);
};
// For date compare
Date.prototype.compareTo = function(date) {
	var date1 = this, date2 = date;
	if ((!(date1 instanceof Date) && !(date1 instanceof Date)) && (date1 == 'Invalid Date' && date2 == 'Invalid Date'))
		return false;
	else if (date1.getTime() < date2.getTime())
		return -1;
	else if (date1.getTime() == date2.getTime())
		return 0;
	else if (date1.getTime() > date2.getTime())
		return 1;
};