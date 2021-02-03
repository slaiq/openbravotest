package sa.elm.ob.utility.ad_reports;

import java.math.BigDecimal;
import java.text.DecimalFormat;

import org.apache.commons.lang.StringUtils;

public class NumberToWords {
  static int flag = 0;
  @SuppressWarnings("unused")
  private static final String[] tensNames = { "", " ten", " twenty", " thirty", " forty", " fifty",
      " sixty", " seventy", " eighty", " ninety" };

  private static final String[] tensNamesCaps = { "", " Ten", " Twenty", " Thirty", " Forty",
      " Fifty", " Sixty", " Seventy", " Eighty", " Ninety" };

  @SuppressWarnings("unused")
  private static final String[] numNames = { "", " one", " two", " three", " four", " five", " six",
      " seven", " eight", " nine", " ten", " eleven", " twelve", " thirteen", " fourteen",
      " fifteen", " sixteen", " seventeen", " eighteen", " nineteen" };

  private static final String[] numNamesCaps = { "", " One", " Two", " Three", " Four", " Five",
      " Six", " Seven", " Eight", " Nine", " Ten", " Eleven", " Twelve", " Thirteen", " Fourteen",
      " Fifteen", " Sixteen", " Seventeen", " Eighteen", " Nineteen" };

  private static String convertLessThanOneThousand(int number) {
    String soFar;
    String result = "";
    String result1 = "";
    String result2 = "";

    if (number % 100 < 20) {
      if (flag == 1)
        result1 = numNamesCaps[number % 100];
      else if (flag == 0)
        result1 = numNamesCaps[number % 100];
      soFar = result1;
      number /= 100;
    } else {
      if (flag == 1)
        result2 = numNamesCaps[number % 10];
      else if (flag == 0)
        result2 = numNamesCaps[number % 10];
      soFar = result2;
      number /= 10;
      // log4j.debug(("convertLessThanOneThousand tensNamesCaps:"+flag);
      if (flag == 1)
        soFar = tensNamesCaps[number % 10] + soFar;
      else if (flag == 0)
        soFar = tensNamesCaps[number % 10] + soFar;
      number /= 10;
    }
    if (number == 0)
      return soFar;
    // log4j.debug(("convertLessThanOneThousand numNamesCaps:"+flag);

    if (flag == 1) {
      result = numNamesCaps[number] + " hundred " + soFar;
    } else if (flag == 0) {
      if (soFar.equals(""))
        result = numNamesCaps[number] + " hundred" + soFar;
      else
        result = numNamesCaps[number] + " hundred and " + soFar;
    }

    return result;
  }

  private static String convert(long number) {
    // 0 to 999 999 999 999

    if (number == 0) {
      return "Zero";
    }

    String snumber = Long.toString(number);

    // pad with "0"
    String mask = "000000000000";
    DecimalFormat df = new DecimalFormat(mask);
    snumber = df.format(number);
    // log4j.debug(("snumber:"+snumber);
    // XXXnnnnnnnnn
    int billions = Integer.parseInt(snumber.substring(0, 3));
    // log4j.debug(("billions:"+billions);
    // nnnXXXnnnnnn
    int millions = Integer.parseInt(snumber.substring(3, 6));
    // log4j.debug(("millions:"+millions);
    // nnnnnnXXXnnn
    int hundredThousands = Integer.parseInt(snumber.substring(6, 9));
    // log4j.debug(("hundredThousands:"+hundredThousands);
    // nnnnnnnnnXXX
    int thousands = Integer.parseInt(snumber.substring(9, 12));

    String tradBillions;
    switch (billions) {
    case 0:
      tradBillions = "";
      break;
    case 1:
      flag = 1;
      // log4j.debug(("tradBillions:"+flag);
      tradBillions = convertLessThanOneThousand(billions) + " Billion ";
      break;
    default:
      flag = 1;
      // log4j.debug(("tradBillions:"+flag);
      tradBillions = convertLessThanOneThousand(billions) + " Billion ";
    }
    String result = tradBillions;
    String tradMillions;
    switch (millions) {
    case 0:
      tradMillions = "";
      break;
    case 1:
      if (flag == 1)
        flag = 0;
      else if (billions == 0)
        flag = 1;
      // log4j.debug(("tradMillions:"+flag);
      tradMillions = convertLessThanOneThousand(millions) + " Million ";
      break;
    default:
      if (flag == 1)
        flag = 0;
      else if (billions == 0)
        flag = 1;
      // log4j.debug(("tradMillions:"+flag);
      tradMillions = convertLessThanOneThousand(millions) + " Million ";
    }
    result = result + tradMillions;
    String tradHundredThousands;
    switch (hundredThousands) {
    case 0:
      tradHundredThousands = "";
      break;
    case 1:
      if (flag == 1)
        flag = 0;
      else if (millions == 0 && billions == 0)
        flag = 1;
      // log4j.debug(("tradHundredThousands:"+flag);
      tradHundredThousands = "One Thousand ";
      break;
    default:
      if (flag == 1)
        flag = 0;
      else if (millions == 0 && billions == 0)
        flag = 1;
      // log4j.debug(("tradHundredThousands:"+flag);
      tradHundredThousands = convertLessThanOneThousand(hundredThousands) + " Thousand ";
    }

    result = result + tradHundredThousands;

    String tradThousand;
    if (flag == 1)
      flag = 0;
    else if (millions == 0 && billions == 0 && hundredThousands == 0)
      flag = 1;
    // log4j.debug(("tradThousand:"+flag);
    tradThousand = convertLessThanOneThousand(thousands);
    result = result + tradThousand;

    // remove extra spaces!
    return result.replaceAll("^\\s+", "").replaceAll("\\b\\s{2,}\\b", " ");
  }

  public static String ConvertAmountToWords(double amount) {
    String result = "";
    DecimalFormat df = new DecimalFormat("###.##");

    long number = 0;
    long pisa = 0;
    String strAmount = df.format(amount);
    int index = strAmount.indexOf(".");

    if (index != -1) {
      number = Long.parseLong(strAmount.substring(0, index));
      pisa = Long.parseLong(strAmount.substring(index + 1));
    } else {
      number = Long.parseLong(strAmount);
    }

    if (pisa == 0) {
      result += "" + convert(number) + " Riyals only";
    } else {
      result += "" + convert(number) + " Riyals and " + convert(pisa) + " Halala(s) only";
    }
    return result;
  }

  // convert the numbers into arabic numbers
  public static String ConvertAmountToArabicAmount(String amount) {
    char[] arabicChars = { '٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩' };
    StringBuilder builder = new StringBuilder();
    String str = amount;
    for (int i = 0; i < str.length(); i++) {
      if (Character.isDigit(str.charAt(i))) {
        builder.append(arabicChars[(int) (str.charAt(i)) - 48]);
      } else {
        builder.append(str.charAt(i));
      }
    }
    return builder.toString();
  }

  // convert the numbers into arabic numbers
  public static String ConvertAmountToArabicAmountWithCommaSeparator(String amount) {
    String str = "";
    Double Amount = (double) 0;
    DecimalFormat df = new DecimalFormat("#,##0.00");

    char[] arabicChars = { '٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩' };
    StringBuilder builder = new StringBuilder();
    if ((StringUtils.isNotEmpty(amount) && !amount.contains("-")) || (StringUtils.isNotEmpty(amount)
        && new BigDecimal(amount).compareTo(new BigDecimal(0)) < 0)) {
      Amount = new Double(amount);
      str = df.format(Amount);
    } else {
      str = amount;
    }
    for (int i = 0; i < str.length(); i++) {
      if (Character.isDigit(str.charAt(i))) {
        builder.append(arabicChars[(int) (str.charAt(i)) - 48]);
      } else {
        builder.append(str.charAt(i));
      }
    }
    return builder.toString();
  }

  // get hijri weekdays
  public static String getWeekdaysInArabic(int weekdays) {
    String hijridays = "";
    switch (weekdays) {
    case 0:
      hijridays = "الأحد";
      break;
    case 1:
      hijridays = "الإثنين";
      break;
    case 2:
      hijridays = "الثلاثاء";
      break;
    case 3:
      hijridays = "الأربعاء";
      break;
    case 4:
      hijridays = "الخميس";
      break;
    case 5:
      hijridays = "الجمعة";
      break;
    case 6:
      hijridays = "السبت";
      break;

    }
    return hijridays;

  }

  // convert the numbers into arabic numbers and weekdays to arabic weekdays
  public static String ConvertNumbertoArabicwithdays(String amount) {
    char[] arabicChars = { '٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩' };
    StringBuilder builder = new StringBuilder();
    String str = amount.split("-")[1];
    int weekdays = Integer.parseInt(amount.split("-")[0]);
    String hijirday = getWeekdaysInArabic(weekdays);
    builder.append(hijirday);
    builder.append('-');
    for (int i = 0; i < str.length(); i++) {
      if (Character.isDigit(str.charAt(i))) {
        builder.append(arabicChars[(int) (str.charAt(i)) - 48]);
      } else {
        builder.append(str.charAt(i));
      }
    }
    return builder.toString();
  }

  // get hijri Months
  public static String getMonthsInArabic(int months) {
    String hijrimonths = "";
    switch (months) {
    case 1:
      hijrimonths = "محرم";
      break;
    case 2:
      hijrimonths = "صفر";
      break;
    case 3:
      hijrimonths = "ربيع الأول";
      break;
    case 4:
      hijrimonths = "ربيع الثاني";
      break;
    case 5:
      hijrimonths = "جمادى الأول";
      break;
    case 6:
      hijrimonths = "جمادى الآخرة";
      break;
    case 7:
      hijrimonths = "رجب";
      break;
    case 8:
      hijrimonths = "شعبان";
      break;
    case 9:
      hijrimonths = "رمضان";
      break;
    case 10:
      hijrimonths = "شوال";
      break;
    case 11:
      hijrimonths = "ذو القعدة";
      break;
    case 12:
      hijrimonths = "ذو الحجة";
      break;

    }
    return hijrimonths;

  }

  // convert the numbers into arabic numbers and months to arabic months and weekdays to arabic
  // weekdays
  public static String ConvertNumbertoArabicwithmonthsandweekdays(String amount) {
    char[] arabicChars = { '٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩' };
    StringBuilder builder = new StringBuilder();
    String str = amount;
    // Split date part for month
    String strdate = amount.split("-")[1];
    int months = Integer.parseInt(strdate.split("/")[1]);
    String hijirmonths = getMonthsInArabic(months);
    // replace month to hijri month
    str = str.split("-")[0] + "-" + strdate.substring(0, 2) + "/"
        + strdate.substring(3, 5).replace(strdate.substring(3, 5), hijirmonths) + "/"
        + strdate.substring(6) + "-" + str.split("-")[2];
    // split weekdays
    int weekdays = Integer.parseInt(str.split("-")[0]);
    String hijriweekdays = getWeekdaysInArabic(weekdays);
    // replace weekdays to hijri weekdays
    str = str.substring(0, 1).replace(str.substring(0, 1), hijriweekdays) + '-' + str.substring(2);

    for (int i = 0; i < str.length(); i++) {
      if (Character.isDigit(str.charAt(i))) {
        builder.append(arabicChars[(int) (str.charAt(i)) - 48]);
      } else {
        builder.append(str.charAt(i));
      }
    }

    return builder.toString();
  }
}
