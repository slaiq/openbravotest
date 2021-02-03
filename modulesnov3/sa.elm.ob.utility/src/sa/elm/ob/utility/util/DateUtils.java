package sa.elm.ob.utility.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

@SuppressWarnings("deprecation")
public class DateUtils {
  private static Logger log4j = Logger.getLogger(DateUtils.class);

  private static SimpleDateFormat ddMMyyyyDateFormat = new SimpleDateFormat("dd-MM-yyyy");

  /**
   * Format Date
   * 
   * @param date
   * @param formmater
   * @return Formatted Date in String
   */
  public static String formatDate(Object date, Object formatter) {
    Calendar calDate = null;
    try {
      calDate = parseDate(date);
      if (calDate == null)
        return "";
      SimpleDateFormat dateFormat = null;
      if (formatter instanceof SimpleDateFormat) {
        dateFormat = (SimpleDateFormat) formatter;
        return dateFormat.format(calDate.getTime());
      } else if (formatter instanceof String) {
        try {
          dateFormat = new SimpleDateFormat(formatter.toString());
          return dateFormat.format(calDate.getTime());
        } catch (Exception e) {
          return Utility.dateFormat.format(calDate.getTime());
        }
      } else
        return Utility.dateFormat.format(calDate.getTime());
    } catch (Exception e) {
      log4j.error("Exception in formatDate : ", e);
      return Utility.dateFormat.format(calDate.getTime());
    }
  }

  /**
   * Calculates duration between two date
   * 
   * @param startDate
   * @param endDate
   * @return Duration between dates in String
   */
  public static String calculateDuration(Object startDate, Object endDate) {
    long diffInDays = 0;
    try {
      Calendar calStartDate = parseDate(startDate), calEndDate = parseDate(endDate);
      if (calStartDate == null || calEndDate == null)
        return "0";
      calStartDate = removeTimeFromDate(calStartDate);
      calEndDate = removeTimeFromDate(calEndDate);

      diffInDays = (calEndDate.getTimeInMillis() - calStartDate.getTimeInMillis())
          / (24 * 60 * 60 * 1000);
      diffInDays = diffInDays + 1;
    } catch (Exception e) {
      log4j.error("Exception in calculateDuration : ", e);
      return "0";
    }
    return Long.toString(diffInDays);
  }

  /**
   * Add duration with date
   * 
   * @param date
   * @param duration
   * @return Date in String
   */
  public static Calendar addDate(Object date, int duration) {
    Calendar calDate = null;
    try {
      calDate = parseDate(date);
      if (calDate == null)
        return null;
      calDate = removeTimeFromDate(calDate);
      calDate.add(Calendar.DATE, duration);
    } catch (Exception e) {
      log4j.error("Exception in addDate : ", e);
      return null;
    }
    return calDate;
  }

  /**
   * Convert Gregorian Date to Hijri Date
   * 
   * @param vars
   * @param date
   * @return Date in String
   */
  public static Calendar convertGregoriantoHijri(Object gregorianDate) {
    Calendar calDate = null;
    try {
      calDate = parseDate(gregorianDate);
      if (calDate == null)
        return null;
      calDate = removeTimeFromDate(calDate);

      int d = calDate.get(Calendar.DATE);
      int m = calDate.get(Calendar.MONTH) + 1;
      int y = calDate.get(Calendar.YEAR);
      int delta = 0, j, l, n, jd = 0; // added delta=1 on jd to comply
                                      // isna rulling 2007
      if ((y > 1582) || ((y == 1582) && (m > 10)) || ((y == 1582) && (m == 10) && (d > 14))) {
        jd = intPart((1461 * (y + 4800 + intPart((m - 14) / 12))) / 4)
            + intPart((367 * (m - 2 - 12 * (intPart((m - 14) / 12)))) / 12)
            - intPart((3 * (intPart((y + 4900 + intPart((m - 14) / 12)) / 100))) / 4) + d - 32075
            + delta;
      } else {
        jd = 367 * y - intPart((7 * (y + 5001 + intPart((m - 9) / 7))) / 4) + intPart((275 * m) / 9)
            + d + 1729777 + delta;
      }
      l = jd - 1948440 + 10632;
      n = intPart((l - 1) / 10631);
      l = l - 10631 * n + 354;
      j = (intPart((10985 - l) / 5316)) * (intPart((50 * l) / 17719))
          + (intPart(l / 5670)) * (intPart((43 * l) / 15238));
      l = l - (intPart((30 - j) / 15)) * (intPart((17719 * j) / 50))
          - (intPart(j / 16)) * (intPart((15238 * j) / 43)) + 29;
      m = intPart((24 * l) / 709);
      d = l - intPart((709 * m) / 24);
      y = 30 * n + j - 30;

      // Add Hijri Date Adjustment
      calDate.setTime(ddMMyyyyDateFormat.parse(String.format("%02d", d) + "-"
          + String.format("%02d", m) + "-" + String.format("%04d", y)));
      calDate.set(calDate.get(Calendar.YEAR), calDate.get(Calendar.MONTH),
          calDate.get(Calendar.DATE), 0, 0, 0);
      // calDate.add(Calendar.DATE, UtilityDAO.getHijriDateAdjustment());
    } catch (Exception e) {
      log4j.error("Exception in convertGregoriantoHijri : ", e);
      return null;
    }
    return calDate;
  }

  /**
   * Convert Hijri Date to Gregorian Date
   * 
   * @param vars
   * @param date
   * @return Date in String
   */
  public static Calendar convertHijritoGregorian(Object hijriDate) {
    Calendar calDate = null;
    try {
      calDate = parseDate(hijriDate);
      if (calDate == null)
        return null;
      calDate = removeTimeFromDate(calDate);

      int d = calDate.get(Calendar.DATE);
      int m = calDate.get(Calendar.MONTH) + 1;
      int y = calDate.get(Calendar.YEAR);
      int delta = 0, i, j, k, l, n, jd = 0; // added delta=1 on jd to
                                            // comply isna rulling 2007
      jd = intPart((11 * y + 3) / 30) + 354 * y + 30 * m - intPart((m - 1) / 2) + d + 1948440 - 385
          - delta;
      if (jd > 2299160) {
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
      } else {
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
      calDate.setTime(ddMMyyyyDateFormat.parse(String.format("%02d", d) + "-"
          + String.format("%02d", m) + "-" + String.format("%04d", y)));
    } catch (Exception e) {
      log4j.error("Exception in convertHijritoGregorian : ", e);
      return null;
    }
    return calDate;
  }

  private static int intPart(float value) {
    if (value < -0.0000001) {
      return (int) Math.ceil(value - 0.0000001);
    }
    return (int) Math.floor(value + 0.0000001);
  }

  /**
   * Getting Calculatin End Date
   * 
   * @param vars
   * @param endDate
   * @param duration
   * @return Date in String
   */
  public static String calculateEndDate(Object startDate, String duration) {
    String endDate = "";
    try {
      Calendar calendar = parseDate(startDate);
      calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + Integer.parseInt(duration) - 1);
      endDate = Utility.dateFormat.format(calendar.getTime());
    } catch (Exception e) {
      log4j.error("Exception in calculateEndDate : ", e);
      return "";
    }
    return endDate;
  }

  /**
   * Validate 24 Hours Format with minute and second
   * 
   * @param time
   * @return boolean
   */
  public static boolean validate24Hours(final String time) {
    try {
      Matcher matcher = Utility.pattern24HOURSMIN.matcher(time);
      if (matcher.matches())
        return true;
      else {
        matcher = Utility.pattern24HOURS.matcher(time);
        if (matcher.matches())
          return true;
      }
    } catch (Exception e) {
      log4j.error("Exception in validate24Hours : ", e);
      return false;
    }
    return false;
  }

  /**
   * Remove Time from Util Date
   * 
   * @param date
   * @return Date
   */
  public static Calendar removeTimeFromDate(Object date) {
    Calendar calendar = null;
    try {
      calendar = parseDate(date);
      if (calendar != null) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
      } else
        return null;
    } catch (Exception e) {
      log4j.error("Exception in removeTimeFromDate : ", e);
      return null;
    }
  }

  /**
   * Convert Time to Minutes
   * 
   * @param value
   * @return Minutes
   */
  public static int convertTimetoMinutes(Object obj, boolean format24Hrs) {
    int len = 0;
    Date utilDate = null;
    java.sql.Timestamp sqlDateTime = null;
    try {
      if (obj == null)
        return 0;
      if (obj instanceof Date) {
        utilDate = (Date) obj;
        len = utilDate.getHours() * 60 + utilDate.getMinutes();
      } else if (obj instanceof java.sql.Date) {
        sqlDateTime = (java.sql.Timestamp) obj;
        len = sqlDateTime.getHours() * 60 + sqlDateTime.getMinutes();
      } else if (obj instanceof java.util.Calendar) {
        sqlDateTime = new java.sql.Timestamp(((java.util.Calendar) obj).getTimeInMillis());
        len = sqlDateTime.getHours() * 60 + sqlDateTime.getMinutes();
      } else if (obj instanceof java.lang.String) {
        if (StringUtils.isEmpty(obj.toString()))
          return 0;
        if (format24Hrs && validate24Hours(obj.toString())) {
          utilDate = new Date("2000/01/01 " + obj.toString());
          len = utilDate.getHours() * 60 + utilDate.getMinutes();
        } else {
          String val[] = obj.toString().split(":");
          len = (Integer.parseInt(val[0]) * 60 + Integer.parseInt(val[1]));
        }
      }
    } catch (final Exception e) {
      log4j.error("Exception in convertTimetoMinutes() Method : ", e);
      return 0;
    }
    return len;
  }

  /**
   * Convert Minutes to Time String
   * 
   * @param date
   * @return String
   */
  public static String convertMinutetoTimeString(Object obj) {
    int i = 0;
    try {
      if (obj == null)
        return "00:00";
      else if (obj instanceof Integer) {
        i = (Integer) obj;
        if (i < 1)
          return "00:00";
        return Utility.timeDecimalFormat.format((i / 60)) + ":"
            + Utility.timeDecimalFormat.format((i % 60));
      } else if (obj instanceof String) {
        if (StringUtils.isEmpty(obj.toString()))
          return "00:00";
        i = Integer.parseInt(obj.toString().replaceAll("\\.0*$", ""));
        if (i < 1)
          return "00:00";
        return Utility.timeDecimalFormat.format((i / 60)) + ":"
            + Utility.timeDecimalFormat.format((i % 60));
      } else
        return "00:00";
    } catch (Exception e) {
      log4j.error("Exception in convertMinutetoTimeString : ", e);
      return "00:00";
    }
  }

  /**
   * Parse String Date
   * 
   * @param value
   * @return java.util.Calendar object
   */
  public static Calendar parseDate(Object date) {
    Calendar calendar = Calendar.getInstance();
    try {
      if (date == null)
        return null;
      else if (date instanceof Date)
        calendar.setTimeInMillis(((Date) date).getTime());
      else if (date instanceof java.sql.Date)
        calendar.setTimeInMillis(((java.sql.Date) date).getTime());
      else if (date instanceof java.sql.Timestamp)
        calendar.setTimeInMillis(((java.sql.Timestamp) date).getTime());
      else if (date instanceof Calendar)
        calendar = (Calendar) date;
      else if (date instanceof String) {
        date = StringUtils.substring((String) date, 0, 19);
        String[] pattern = new String[5];
        // 23-10-2015 23:59:59
        pattern[0] = "^([\\d]{1,2})\\-([\\d]{1,2})\\-([\\d]{4})([\\s]{1}([\\d]{1,2})\\:([\\d]{1,2})\\:([\\d]{1,2})){0,1}$";
        // 23-10-15 23:59:59
        pattern[1] = "^([\\d]{1,2})\\-([\\d]{1,2})\\-([\\d]{2})([\\s]{1}([\\d]{1,2})\\:([\\d]{1,2})\\:([\\d]{1,2})){0,1}$";
        // 10/23/2015 23:59:59
        pattern[2] = "^([\\d]{1,2})\\/([\\d]{1,2})\\/([\\d]{4})([\\s]{1}([\\d]{1,2})\\:([\\d]{1,2})\\:([\\d]{1,2})){0,1}$";
        // 10/23/15 23:59:59
        pattern[3] = "^([\\d]{1,2})\\/([\\d]{1,2})\\/([\\d]{2})([\\s]{1}([\\d]{1,2})\\:([\\d]{1,2})\\:([\\d]{1,2})){0,1}$";
        // 2015-10-23 23:59:59
        pattern[4] = "^([\\d]{4})\\-([\\d]{1,2})\\-([\\d]{1,2})([\\s]{1}([\\d]{1,2})\\:([\\d]{1,2})\\:([\\d]{1,2})){0,1}$";
        for (int i = 0; i < 5; i++) {
          Pattern r = Pattern.compile(pattern[i]);
          Matcher m = r.matcher(date.toString().trim());
          if (m.find()) {
            int yr = 0, mon = 0, day = 0, hr = 0, min = 0, sec = 0;
            if (i == 0) {
              yr = Integer.parseInt(m.group(3));
              mon = Integer.parseInt(m.group(2));
              day = Integer.parseInt(m.group(1));
              if (m.group(4) != null) {
                hr = Integer.parseInt(m.group(5));
                min = Integer.parseInt(m.group(6));
                sec = Integer.parseInt(m.group(7));
              }
            } else if (i == 1) {
              yr = Integer.parseInt(
                  ("" + new Date().getYear()).substring(0, 2) + Integer.parseInt(m.group(3)));
              mon = Integer.parseInt(m.group(2));
              day = Integer.parseInt(m.group(1));
              if (m.group(4) != null) {
                hr = Integer.parseInt(m.group(5));
                min = Integer.parseInt(m.group(6));
                sec = Integer.parseInt(m.group(7));
              }
            } else if (i == 2) {
              yr = Integer.parseInt(m.group(3));
              mon = Integer.parseInt(m.group(1));
              day = Integer.parseInt(m.group(2));
              if (m.group(4) != null) {
                hr = Integer.parseInt(m.group(5));
                min = Integer.parseInt(m.group(6));
                sec = Integer.parseInt(m.group(7));
              }
            } else if (i == 3) {
              yr = Integer.parseInt(
                  ("" + new Date().getYear()).substring(0, 2) + Integer.parseInt(m.group(3)));
              mon = Integer.parseInt(m.group(1));
              day = Integer.parseInt(m.group(2));
              if (m.group(4) != null) {
                hr = Integer.parseInt(m.group(5));
                min = Integer.parseInt(m.group(6));
                sec = Integer.parseInt(m.group(7));
              }
            } else if (i == 4) {
              yr = Integer.parseInt(m.group(1));
              mon = Integer.parseInt(m.group(2));
              day = Integer.parseInt(m.group(3));
              if (m.group(4) != null) {
                hr = Integer.parseInt(m.group(5));
                min = Integer.parseInt(m.group(6));
                sec = Integer.parseInt(m.group(7));
              }
            }
            if (mon > 12 || day >= 32 || hr >= 24 || min >= 60 || sec >= 60)
              return null;
            calendar.set(yr, (mon - 1), day, hr, min, sec);
            return calendar;
          }
        }
        return null;
      } else
        return null;
    } catch (Exception e) {
      log4j.error("Exception in parseDate : ", e);
      return null;
    }
    return calendar;
  }

  /**
   * Converts Java date to string in given date format
   * 
   * @param dateFormat
   * @param date
   * @return
   */
  public static String convertDateToString(String dateFormat, Date date) {

    DateFormat df = new SimpleDateFormat(dateFormat);

    return df.format(date);

  }

  /**
   * Converts String to given date format
   * 
   * @param dateFormat
   * @param date
   * @return
   * @throws ParseException
   */
  public static Date convertStringToDate(String dateFormat, String date) throws ParseException {

    DateFormat df = new SimpleDateFormat(dateFormat);

    return df.parse(date);

  }
}