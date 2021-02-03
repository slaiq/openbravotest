//Web Reference : http://www.al-habib.info/islamic-calendar/hijricalendartext.htm
//type = InputType(String[S]/Date[D])  returnType = OutputType(String[S]/Date[D])
function convertGregorianToHijri(date, type, returnType, adjust) {
	if (type == "S")
		var date = new Date(Date.parse(date.substring(3, 5) + "/" + date.substring(0, 2) + "/" + date.substring(6, 10)));
	date.setDate(date.getDate() - 1);
	// Adjust Dates
	if (typeof adjust != "undefined") {
		if (adjust[0] == "+")
			date.setDate(date.getDate() + parseInt(adjust.substring(1, adjust.length)));
		else
			date.setDate(date.getDate() - parseInt(adjust.substring(1, adjust.length)));
	}
	day = date.getDate();
	month = date.getMonth();
	year = date.getFullYear();
	m = month + 1;
	y = year;
	if (m < 3) {
		y -= 1;
		m += 12;
	}
	a = Math.floor(y / 100.);
	b = 2 - a + Math.floor(a / 4.);
	if (y < 1583)
		b = 0;
	if (y == 1582) {
		if (m > 10)
			b = -10;
		if (m == 10) {
			b = 0;
			if (day > 4)
				b = -10;
		}
	}
	jd = Math.floor(365.25 * (y + 4716)) + Math.floor(30.6001 * (m + 1)) + day + b - 1524;
	b = 0;
	if (jd > 2299160) {
		a = Math.floor((jd - 1867216.25) / 36524.25);
		b = 1 + a - Math.floor(a / 4.);
	}
	bb = jd + b + 1524;
	cc = Math.floor((bb - 122.1) / 365.25);
	dd = Math.floor(365.25 * cc);
	ee = Math.floor((bb - dd) / 30.6001);
	day = (bb - dd) - Math.floor(30.6001 * ee);
	month = ee - 1;
	if (ee > 13) {
		cc += 1;
		month = ee - 13;
	}
	year = cc - 4716;

	wd = ((((jd + 1) % 7) + 7) % 7) + 1;
	// wd = gmod(jd+1,7)+1;
	iyear = 10631. / 30.;
	epochastro = 1948084;
	epochcivil = 1948085;
	shift1 = 8.01 / 60.;
	z = jd - epochastro;
	cyc = Math.floor(z / 10631.);
	z = z - 10631 * cyc;
	j = Math.floor((z - shift1) / iyear);
	iy = 30 * cyc + j;
	z = z - Math.floor(j * iyear + shift1);
	im = Math.floor((z + 28.5001) / 29.5);
	if (im == 13)
		im = 12;
	id = z - Math.floor(29.5001 * im - 29);
	/*var calDate = new Array(8);
	calDate[0] = day; //calculated day (CE)
	calDate[1] = month-1; //calculated month (CE)
	calDate[2] = year; //calculated year (CE)
	calDate[3] = jd-1; //julian day number
	calDate[4] = wd-1; //weekday number
	calDate[5] = id; //islamic date
	calDate[6] = im-1; //islamic month
	calDate[7] = iy; //islamic year*/
	if (im <= 9)
		im = "0" + im;
	if (id <= 9)
		id = "0" + id;
	if (returnType == "S")
		return id + "-" + im + "-" + iy;
	else if (returnType == "D")
		return im + "/" + id + "/" + iy;
}
function  convertHijriToGregorian(date, type, returnType, adjust) {
//	alert("convertHijriToGregorian");

	var date1 = date.split("-"); //16-07-1437
	var d = parseInt(date1[0]);//16
	var m = parseInt(date1[1]);//07
	var y = parseInt(date1[2]);//1437
//	alert(d);
//	alert(m);
//	alert(y);
	var delta = 0, i, j, k, l, n, jd = 0; //added delta=1 on jd to comply isna rulling 2007
	jd = Math.floor((11 * y + 3) / 30) + 354 * y + 30 * m - Math.floor((m - 1) / 2) + d + 1948440 - 385 - delta; // Math.floor((11 * 1437 + 3) / 30) + 354 * 1437 + 30 * 7 - Math.floor((7 - 1) / 2) + 16 + 1948440 - 385 - 0;2457503
	if(jd > 2299160) {  //2457503 > 2299160 cond true
		l = jd + 68569;										// 2457503 + 68569; 2526072
		n = Math.floor((4 * l) / 146097);					// Math.floor((4 * 2526072) / 146097) ; 69
		l = l - Math.floor((146097 * n + 3) / 4);			// 2526072 - Math.floor((146097 * 69 + 3) / 4);	5898
		i = Math.floor((4000 * (l + 1)) / 1461001);			// Math.floor((4000 * (5898 + 1)) / 1461001);  // 16
		l = l - Math.floor((1461 * i) / 4) + 31;			// 5898 - Math.floor((1461 * 16) / 4) + 31; 85
		j = Math.floor((80 * l) / 2447);					//  Math.floor((80 * 85) / 2447)  ; 2
		d = l - Math.floor((2447 * j) / 80);  				// 85- Math.floor((2447 * 2) / 80);  24
		l = Math.floor(j / 11);								//   Math.floor(2 / 11); 0
		m = j + 2 - 12 * l;									// 2 + 2 - 12 * 0  //  4
		y = 100 * (n - 49) + i + l;							// 100 * (69- 49) + 16 + 0;  2016
	}
	else {
		j = jd + 1402;
		k = Math.floor((j - 1) / 1461);
		l = j - 1461 * k;
		n = Math.floor((l - 1) / 365) - Math.floor(l / 1461);
		i = l - 365 * n + 30;
		j = Math.floor((80 * i) / 2447);
		d = i - Math.floor((2447 * j) / 80);
		i = Math.floor(j / 11);
		m = j + 2 - 12 * i;
		y = 4 * k + n + i - 4716;
	}
	if (m <= 9)
		m = "0" + m;
	if (d <= 9)
		d = "0" + d;
	


	return d + "-" + m + "-" + y;    // 24-04-2016
}