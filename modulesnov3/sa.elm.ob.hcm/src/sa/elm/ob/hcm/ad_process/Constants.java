package sa.elm.ob.hcm.ad_process;

/**
 * 
 * @author divya
 *
 */
public class Constants {

  // Decision Overlap
  public static String SECONDMENT_OVERLAP = "SEC";
  public static String ABSENCE_OVERLAP = "AB";
  public static String SCHOLARSHIP_OVERLAP = "SCTR";
  public static String BUSINESSMISSION_OVERLAP = "BUMS";
  public static String DISCIPLINARY_ACTION = "DIS";
  public static String OVERTIME_OVERLAP = "OT";
  public static String DELEGATION_OVERLAP = "DEL";
  public static String SUSPENSION_OVERLAP = "SUS";
  public static String TERMINATION_OVERLAP = "TER";
  public static String DISCLIPLINE_OVERLAP = "DIS";

  // suspension decision type
  public static String SUSPENSION_START = "SUS";
  public static String SUSPENSION_END = "SUE";

  // employee status
  public static String EMPSTATUS_INACTIVE = "INACT";
  public static String EMPSTATUS_ACTIVE = "ACT";

  // promotion decision type
  public static String PROMOTION = "PR";
  public static String PROMOTIONTRANSFER = "PRT";

  // leave type variables
  public static String EMPLEAVE_ABSENCE = "AB";
  public static String EMPLEAVE_CREDITON_ONETIME = "OT";

  // accrual reset date
  public static String ACCRUALRESETDATE_HIJIRI = "BHY";
  public static String ACCRUALRESETDATE_GREGORIAN = "BGY";
  public static String ACCRUALRESETDATE_HIREANNIVERSARY = "HAD";
  public static String ACCRUALRESETDATE_FULLSERVICE = "FS";
  public static String ACCRUALRESETDATE_LEAVEOCCUR = "LO";

  // accrual Frequency
  public static String ACCRUALFREQUENCY_HIJIRI = "HY";
  public static String ACCRUALFREQUENCY_GREGORIAN = "GY";

  // leave line type
  public static String EMPLEAVE_ACCRUAL = "AC";
  public static String EMPLEAVE_OPENINGBALANCE = "OB";

  // employment status
  public static String EMPLOYMENT_STATUS_ACTIVE = "AC";
  public static String EMPLOYMENT_STATUS_SECONDMENT = "SE";

  // days In Year
  public static int NoOfDaysInYear = 355;
  public static int SecondmentBlockYear = 3;
  public static int SecondmentGapYear = 3;
  public static int SecondmentMaxYear = 6;
  public static int YearForSecondmentCreateAfterPromotion = 1;

  // days In Month
  public static int NoOfDaysInMonths = 30;

  // Initial Balance
  public static String ANNUALLEAVEBALANCE = "ALB";
  public static String ALLPAIDLEAVEBALANCE = "APLB";
  public static String BUSINESSMISSIONBALANCE = "BM";
  public static String TOTALPERIODOFSERVICE = "TPS";

  public static String PROMOTIONBALANCE = "LPD";
}
