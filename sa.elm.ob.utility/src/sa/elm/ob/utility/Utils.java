package sa.elm.ob.utility;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

  private static final long SAUDI_MOBILE_NUMBER_MASK = 966000000000L;

  /**
   * adds Saudi Arabia country code 966 to the passed mobile number
   * 
   * @param mobileNumber
   *          mobile number in form 5XXXXXXXX
   * @return the mobile number in format 9665XXXXXXXX
   */
  public static long formatSaudiMobileNumber(long mobileNumber) {
    return mobileNumber + SAUDI_MOBILE_NUMBER_MASK;
  }

  /**
   * validate iqama number(non saudi ID)
   * 
   * @param nationalNumber
   *          nationalNumber in form 2XXXXXXXXX
   * @return true or false.
   */
  public static boolean isIqamaNumber(String nationalNumber) {
    return isNicNumber(nationalNumber, "2");
  }

  /**
   * validate National ID number
   * 
   * @param nationalNumber
   *          nationalNumber in form 1XXXXXXXXX
   * @return true or false.
   */
  public static boolean isNINNumber(String nationalNumber) {
    return isNicNumber(nationalNumber, "1");
  }

  /**
   * validate mobile number with Saudi Arabia Extension
   * 
   * @param mobileNumber
   *          mobile number in form 9665XXXXXXXX
   * @return true or false.
   */
  public static boolean isMobile(String mobile) {
    if (!isNumeric(mobile))
      return false;
    if ((mobile.startsWith("9665")) && (mobile.trim().length() == 12)) {
      return true;
    }
    return false;
  }

  private static boolean isNicNumber(String nicNumber, String prefix) {
    if ((!isBegin(nicNumber, prefix)) || (!isNumeric(nicNumber)) || (!isLength(nicNumber, 10)))
      return false;

    char[] charArray = nicNumber.toCharArray();
    int[] numArray = new int[10];
    for (int i = 0; i < charArray.length; i++) {
      numArray[i] = Character.getNumericValue(charArray[i]);
    }
    int sum = 0;
    for (int i = 0; i < numArray.length - 1; i++) {
      if (i % 2 != 0) {
        sum += numArray[i];
      } else {
        int oddByTwo = numArray[i] * 2;
        String oddByTwoString = String.valueOf(oddByTwo);
        int[] oddByTwoArray = new int[oddByTwoString.length()];
        int oddByTwoSum = 0;
        for (int j = 0; j < oddByTwoArray.length; j++) {
          oddByTwoArray[j] = Character.getNumericValue(oddByTwoString.charAt(j));
          oddByTwoSum += oddByTwoArray[j];
        }
        sum += oddByTwoSum;
      }
    }

    String sumString = String.valueOf(sum);
    int unit = Character.getNumericValue(sumString.charAt(sumString.length() - 1));
    if ((unit == 0) && (numArray[9] == 0))
      return true;
    if (10 - unit == numArray[9]) {
      return true;
    }
    return false;
  }

  public static boolean isBegin(String text, String c) {
    return text.matches("^[" + c + "](.*)");
  }

  public static boolean isLength(String text, int length) {
    if (text.length() == length)
      return true;
    return false;
  }

  public static boolean isNumeric(String text) {
    return text.matches("\\d{1,}");
  }

  /**
   * Checking Phone Number is valid or not
   * 
   * 
   * @param phonenumber
   * @return true or false.
   */
  public static boolean phoneNumberFormat(String phonenumber) {
    Boolean isValid = false;
    String regex = "[0-9+ -]*";
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(phonenumber);

    if (matcher.matches()) {
      isValid = true;
    }
    return isValid;

  }

  /**
   * Checking Fax Number is valid or not
   * 
   * 
   * @param fax
   * @return true or false.
   */
  public static boolean faxNumberFormat(String fax) {
    Boolean isValid = false;
    String regex = "[0-9+() -]*";
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(fax);

    if (matcher.matches()) {
      isValid = true;
    }
    return isValid;

  }
}
