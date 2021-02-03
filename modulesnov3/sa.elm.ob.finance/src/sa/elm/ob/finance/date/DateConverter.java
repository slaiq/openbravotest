package sa.elm.ob.finance.date;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.Logger;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;

import sa.elm.ob.utility.EutAdjustHijriDate;
import sa.elm.ob.utility.util.Utility;

public class DateConverter {

	double EPOCH = 2451545.0;
	long g = 0;
	String bora = "AD";
	String DAY = "";
	String NOTE = ";";
	String resultDt = "";
	double day, month, year, ayear, myear, cdays, hijri, Gday, MONTH, HMONTH, HDAY, cyear, cday, mday, cmonth, thistory;

	VariablesSecureApp vars = null;
	Logger log4j = Logger.getLogger(DateConverter.class);

	public DateConverter() {

	}

	public DateConverter(VariablesSecureApp vars) {
		this.vars = vars;
	}
	
	@Deprecated
	public String convertGregoriantoHijri(String date) throws ParseException {

		String[] datArr = date.split("-");
		String sign = "P";
		BigDecimal adjustdays = BigDecimal.ZERO;

		//Date Format
		log4j.debug("varsclie:" + vars.getClient());
		log4j.debug("vars:" + vars.getSessionValue("#AD_JavaDateFormat"));
		SimpleDateFormat dateFmt = new SimpleDateFormat(vars.getSessionValue("#AD_JavaDateFormat"));

		// Get Date Adjustment Details..
		OBQuery<EutAdjustHijriDate> qry = OBDal.getInstance().createQuery(EutAdjustHijriDate.class, "fiscalYear='" + Integer.parseInt(datArr[2]) + "' order by updated desc limit 1");
		log4j.debug(qry.toString());
		log4j.debug(qry.list().size());
		if(qry.list().size() > 0) {
			EutAdjustHijriDate list = qry.list().get(0);
			sign = list.getSign();
			adjustdays = list.getWaitingPeriodDays();
		}
		log4j.debug("adjustdays:" + adjustdays);
		log4j.debug("sign:" + sign);
		// Add or Subtract days to adjust
		Date fromDate = dateFmt.parse(date);
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(fromDate);

		if(sign.equalsIgnoreCase("P"))
			calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + adjustdays.intValue());
		else if(sign.equalsIgnoreCase("M"))
			calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - adjustdays.intValue());

		String gregorianDate = Utility.dateFormat.format(calendar.getTime());
		log4j.debug("gregorianDate:" + gregorianDate);
		String[] adjustDate = gregorianDate.split("-");
		int d = Integer.parseInt(adjustDate[0]);
		int m = Integer.parseInt(adjustDate[1]);
		int y = Integer.parseInt(adjustDate[2]);
		int delta = 0, j, l, n, jd = 0; // added delta=1 on jd to comply isna rulling 2007
		if((y > 1582) || ((y == 1582) && (m > 10)) || ((y == 1582) && (m == 10) && (d > 14))) {
			jd = intPart((1461 * (y + 4800 + intPart((m - 14) / 12))) / 4) + intPart((367 * (m - 2 - 12 * (intPart((m - 14) / 12)))) / 12) - intPart((3 * (intPart((y + 4900 + intPart((m - 14) / 12)) / 100))) / 4) + d - 32075 + delta;
		}
		else {
			jd = 367 * y - intPart((7 * (y + 5001 + intPart((m - 9) / 7))) / 4) + intPart((275 * m) / 9) + d + 1729777 + delta;
		}
		l = jd - 1948440 + 10632;
		n = intPart((l - 1) / 10631);
		l = l - 10631 * n + 354;
		j = (intPart((10985 - l) / 5316)) * (intPart((50 * l) / 17719)) + (intPart(l / 5670)) * (intPart((43 * l) / 15238));
		l = l - (intPart((30 - j) / 15)) * (intPart((17719 * j) / 50)) - (intPart(j / 16)) * (intPart((15238 * j) / 43)) + 29;
		m = intPart((24 * l) / 709);
		d = l - intPart((709 * m) / 24);
		y = 30 * n + j - 30;

		String day = null;
		String month = null;
		if(d < 10)
			day = "0".concat(String.valueOf(d));
		else
			day = String.valueOf(d);
		if(m < 10)
			month = "0".concat(String.valueOf(m));
		else
			month = String.valueOf(m);
		log4j.debug("month:" + month);
		log4j.debug("year:" + y);
		log4j.debug("day:" + day);
		return day + "-" + month + "-" + y;
	}
	
	@Deprecated
	public String convertHijritoGregorian(String date) throws ParseException {
		// Date Format

		SimpleDateFormat dateFmt = new SimpleDateFormat(vars.getSessionValue("#AD_JavaDateFormat"));
		String sign = "P";
		BigDecimal adjustdays = BigDecimal.ZERO;
		String[] datArr = date.split("-");

		OBQuery<EutAdjustHijriDate> qry = OBDal.getInstance().createQuery(EutAdjustHijriDate.class, "fiscalYear='" + Integer.parseInt(datArr[2]) + "' order by updated desc limit 1");
		log4j.debug(qry.toString());
		log4j.debug(qry.list().size());
		if(qry.list().size() > 0) {
			EutAdjustHijriDate list = qry.list().get(0);
			sign = list.getSign();
			adjustdays = list.getWaitingPeriodDays();
			log4j.debug("adjustdays:" + adjustdays);
			log4j.debug("sign:" + sign);
		}
		log4j.debug("adjustdays:" + adjustdays);
		log4j.debug("sign:" + sign);
		// Add or Subtract days to adjust
		Date fromDate = dateFmt.parse(date);
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(fromDate);

		if(sign.equalsIgnoreCase("P"))
			calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + adjustdays.intValue());
		else if(sign.equalsIgnoreCase("M"))
			calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - adjustdays.intValue());

		String gregorianDate = Utility.dateFormat.format(calendar.getTime());
		log4j.debug("gregorianDate:" + gregorianDate);
		String[] adjustDate = gregorianDate.split("-");
		int d = Integer.parseInt(adjustDate[0]);
		int m = Integer.parseInt(adjustDate[1]);
		int y = Integer.parseInt(adjustDate[2]);
		int delta = 0, i, j, k, l, n, jd = 0; // added delta=1 on jd to comply isna rulling 2007
		jd = intPart((11 * y + 3) / 30) + 354 * y + 30 * m - intPart((m - 1) / 2) + d + 1948440 - 385 - delta;
		if(jd > 2299160) {
			l = jd + 68569;
			n = intPart((4 * l) / 146097);
			l = l - intPart((146097 * n + 3) / 4);
			i = intPart((4000 * (l + 1)) / 1461001);
			l = l - intPart((1461 * i) / 4) + 31;
			j = intPart((80 * l) / 2447);
			d = l - intPart((2447 * j) / 80);
			l = intPart(j / 11);
			m = j + 2 - 12 * l;
			y = 100 * (n - 49) + i + l;
		}
		else {
			j = jd + 1402;
			k = intPart((j - 1) / 1461);
			l = j - 1461 * k;
			n = intPart((l - 1) / 365) - intPart(l / 1461);
			i = l - 365 * n + 30;
			j = intPart((80 * i) / 2447);
			d = i - intPart((2447 * j) / 80);
			i = intPart(j / 11);
			m = j + 2 - 12 * i;
			y = 4 * k + n + i - 4716;
		}

		String day = null;
		String month = null;
		if(d < 10)
			day = "0".concat(String.valueOf(d));
		else
			day = String.valueOf(d);
		if(m < 10)
			month = "0".concat(String.valueOf(m));
		else
			month = String.valueOf(m);
		log4j.debug("month:" + month);
		log4j.debug("year:" + y);
		log4j.debug("day:" + day);
		return y + "-" + month + "-" + d;
	}

	private static int intPart(float value) {
		if(value < -0.0000001) {
			return (int) Math.ceil(value - 0.0000001);
		}
		return (int) Math.floor(value + 0.0000001);
	}

	public double calculateDuration(String inpStartDate, String inpEndDate, String fromsession, String tosession) throws ParseException {
		// Date Format

		SimpleDateFormat dateFmt = new SimpleDateFormat(vars.getSessionValue("#AD_JavaDateFormat"));

		double totalDays = 0;
		Date fromDate = dateFmt.parse(inpStartDate);
		Date endDate = dateFmt.parse(inpEndDate);
		Calendar calstartDate = Calendar.getInstance();
		Calendar calendDate = Calendar.getInstance();

		calstartDate.setTime(fromDate);
		calendDate.setTime(endDate);

		calstartDate.set(calstartDate.get(Calendar.YEAR), calstartDate.get(Calendar.MONTH), calstartDate.get(Calendar.DATE));
		calendDate.set(calendDate.get(Calendar.YEAR), calendDate.get(Calendar.MONTH), calendDate.get(Calendar.DATE));
		long milliseconds1 = calstartDate.getTimeInMillis();
		long milliseconds2 = calendDate.getTimeInMillis();
		long diff = milliseconds2 - milliseconds1;
		long diffDays = diff / (24 * 60 * 60 * 1000);
		totalDays = diffDays + 1;

		return totalDays;
	}

	public String calculateEndDate(String inpEndDate, String inpDuration) {
		String endDate = "";

		try {
			// Date Format

			SimpleDateFormat dateFmt = new SimpleDateFormat(vars.getSessionValue("#AD_JavaDateFormat"));

			Date fromDate = dateFmt.parse(inpEndDate);
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(fromDate);

			calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + Integer.parseInt(inpDuration) - 1);

			endDate = Utility.dateFormat.format(calendar.getTime());
		}
		catch (Exception e) {
			log4j.error("Exception in calculateEndDate :", e);
		}
		return endDate;
	}
}
