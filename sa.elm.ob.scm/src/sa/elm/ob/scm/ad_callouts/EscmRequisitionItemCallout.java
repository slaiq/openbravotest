package sa.elm.ob.scm.ad_callouts;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;

import sa.elm.ob.hcm.util.Utility;
import sa.elm.ob.scm.ad_process.Requisition.RequisitionDao;
import sa.elm.ob.utility.util.UtilityDAO;

public class EscmRequisitionItemCallout extends SimpleCallout {
  private static Logger log = Logger.getLogger(EscmRequisitionItemCallout.class);
  /**
   * Callout to update the Description and UOM.
   */
  private static final long serialVersionUID = 1L;

  protected void execute(CalloutInfo info) throws ServletException {

    VariablesSecureApp vars = info.vars;
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String strMProductID = vars.getStringParameter("inpmProductId");
    String strQty = vars.getNumericParameter("inpqty");
    String strQuantity = vars.getNumericParameter("inpquantity");
    String strUnitPrice = vars.getNumericParameter("inpunitprice");
    String strManual = vars.getStringParameter("inpismanual");
    String strPriceActual = vars.getNumericParameter("inppriceactual");
    String receivingType = vars.getStringParameter("inpemEscmReceivingtype");
    String strOrderLineId = vars.getStringParameter("inpcOrderlineId");
    String strmInoutId = vars.getStringParameter("inpmInoutId");
    String uniqueCode = vars.getStringParameter("inpemEfinCValidcombinationId");
    String budgetInit = vars.getStringParameter("inpemEfinBudgetintId");
    String exeStartDateH = vars.getStringParameter("inpexeStartDateH");
    String exeStartDateG = vars.getStringParameter("inpexeStartDateG");
    String exeEndDateH = vars.getStringParameter("inpexeEndDateH");
    String exeEndDateG = vars.getStringParameter("inpexeEndDateG");
    String receivedAmount = vars.getStringParameter("inpreceivedAmount");
    String receiveType = vars.getStringParameter("inpemEscmReceivetype");
    String totalLineAmount = vars.getStringParameter("inptotLineAmt");

    Boolean isProductItem = true, errorFlag = false;
    BigDecimal lineNetAmount = BigDecimal.ZERO, fundsAvailable = BigDecimal.ZERO;
    try {
      if (inpLastFieldChanged.equals("inpmProductId")) {
        if (StringUtils.isNotBlank(strMProductID) && StringUtils.isNotEmpty(strMProductID)) {
          Product product = OBDal.getInstance().get(Product.class, strMProductID);
          OrderLine objOrderLine = OBDal.getInstance().get(OrderLine.class, strOrderLineId);
          if (objOrderLine != null) {
            if (objOrderLine.getProduct() == null) {
              isProductItem = false;
            }
          }
          info.addResult("inpcUomId", product.getUOM().getId());
          if (StringUtils.isNotBlank(strmInoutId) && StringUtils.isNotEmpty(strmInoutId)) {
            if (strManual.equals("Y") && isProductItem) {
              info.addResult("inpdescription", product.getName());
            } else if (isProductItem) {
              info.addResult("inpdescription", "");
            }
          } else {
            info.addResult("inpdescription", product.getName());
          }
          if (product.getImage() != null)
            info.addResult("inpadImageId", product.getImage().getId());
          else
            info.addResult("inpadImageId", "");
          if (product.getEscmStockType().getSearchKey().equals("CUS")) {
            info.addResult("inpcustodyItem", true);
          }
          // while changing the product in site receiving getting unit price as 1 because of that in
          // rdv we are getting negative net match amount
          /*
           * if (receivingType.equals("SR")) { info.addResult("inpunitprice", BigDecimal.ONE); }
           */

          // set product category based on selected item
          info.addResult("inpemEscmProdcate", product.getProductCategory().getId());

        } else {
          info.addResult("inpcUomId", "");
          info.addResult("inpdescription", "");
          info.addResult("inpadImageId", "");
        }
      }
      if (inpLastFieldChanged.equals("inpqty")) {
        if (strPriceActual != null && strQty != null && StringUtils.isNotEmpty(strPriceActual)
            && StringUtils.isNotEmpty(strQty)) {
          lineNetAmount = new BigDecimal(strPriceActual).multiply(new BigDecimal(strQty));
        }

        info.addResult("inplinenetamt", lineNetAmount);
      }
      if (inpLastFieldChanged.equals("inpreceivedAmount") && (receiveType.equals("AMT"))) {
        info.addResult("inpunitprice", receivedAmount);
        info.addResult("inptotLineAmt", receivedAmount);
      }
      if (inpLastFieldChanged.equals("inpunitprice") && (receiveType.equals("AMT"))) {
        info.addResult("inpreceivedAmount", strUnitPrice);
      }

      if (inpLastFieldChanged.equals("inptotLineAmt")
          && ((receiveType.equals("AMT")) || (receivingType.equals("RET")))) {
        info.addResult("inpreceivedAmount", totalLineAmount);
        info.addResult("inpunitprice", totalLineAmount);
      }

      if (inpLastFieldChanged.equals("inppriceactual")) {
        if (strPriceActual != null && strQty != null && StringUtils.isNotEmpty(strPriceActual)
            && StringUtils.isNotEmpty(strQty)) {
          lineNetAmount = new BigDecimal(strPriceActual).multiply(new BigDecimal(strQty));
        }
        info.addResult("inplinenetamt", lineNetAmount);
      }
      if (inpLastFieldChanged.equals("inpemEfinCValidcombinationId")) {
        if (uniqueCode.equals("")) {
          info.addResult("inpemEfinFundsAvailable", BigDecimal.ZERO);
          info.addResult("inpemEfinUniquecodename", "");
        } else {
          AccountingCombination dimention = OBDal.getInstance().get(AccountingCombination.class,
              uniqueCode);
          fundsAvailable = RequisitionDao.getAutoEncumFundsAvailable(uniqueCode, budgetInit);
          info.addResult("inpemEfinFundsAvailable", fundsAvailable);
          info.addResult("inpemEfinUniquecodename", dimention.getEfinUniquecodename());
        }
      }

      String exeStartGreg = null, exeEndGreg = null;
      if (inpLastFieldChanged.equals("inpexeStartDateH")
          || inpLastFieldChanged.equals("inpexeEndDateH")) {

        // LastField changes is Execution Start date (H)
        if (inpLastFieldChanged.equals("inpexeStartDateH")) {
          if (!StringUtils.isNotEmpty(exeStartDateH)) {
            info.addResult("inpexeStartDateG", "");
          }
          // To check hijiri date format or not
          if (exeStartDateH.matches("([0-9]{2})-([0-9]{2})-([0-9]{4})")
              && UtilityDAO.Checkhijridate(exeStartDateH)) {
            exeStartGreg = UtilityDAO.convertToGregorian_tochar(exeStartDateH);
            info.addResult("inpexeStartDateG", exeStartGreg);
          } else {
            info.addResult("inpexeStartDateG", "");
            info.addResult("inpexeStartDateH", "");
          }

        }
        // LastField changes is Execution End date (H)
        else {
          if (!StringUtils.isNotEmpty(exeEndDateH)) {
            info.addResult("inpexeEndDateG", "");
          }
          // To check Hijiri date format or not
          if (exeEndDateH.matches("([0-9]{2})-([0-9]{2})-([0-9]{4})")
              && UtilityDAO.Checkhijridate(exeEndDateH)) {
            exeEndGreg = UtilityDAO.convertToGregorian_tochar(exeEndDateH);
            info.addResult("inpexeEndDateG", exeEndGreg);
          } else {
            info.addResult("inpexeEndDateG", "");
            info.addResult("inpexeEndDateH", "");

          }
          if (StringUtils.isNotEmpty(exeEndDateH) && !errorFlag) {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

            Date needByDateG = new java.util.Date(); // today
            String strNeedByDateG = df.format(needByDateG);
            String strNeedByDateH = UtilityDAO.convertToHijriDate(strNeedByDateG);
            Date needByDateH = sdf.parse(strNeedByDateH);
            String strNeedByDate = sdf.format(needByDateH);

            Date exeEndH = sdf.parse(exeEndDateH);
            String strExeEnd = sdf.format(exeEndH);
            int noofday = Utility.calculatetheDays(strNeedByDate, strExeEnd);
            if (noofday > 0) {
              info.addResult("inpcontractDelayDays", noofday);
            } else {
              info.addResult("inpcontractDelayDays", '0');
            }
          } else {
            info.addResult("inpcontractDelayDays", '0');
          }

        }
        // To calculate number of Contract Execution days based on hijiri date
        if (StringUtils.isNotEmpty(exeStartDateH) && StringUtils.isNotEmpty(exeEndDateH)) {
          if (UtilityDAO.Checkhijridate(exeEndDateH) && UtilityDAO.Checkhijridate(exeStartDateH)) {
            // java.util.Date exeStart = UtilityDAO.convertToGregorianDate(exeStartDateH);
            // java.util.Date exeEnd = UtilityDAO.convertToGregorianDate(exeEndDateH);
            // LocalDate exeStart1 =
            // exeStart.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            // LocalDate exeEnd1 = exeEnd.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            // long noOfDaysBetween = ChronoUnit.DAYS.between(exeStart1, exeEnd1);
            // info.addResult("inpcontractExeDays", noOfDaysBetween);

            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

            Date exeStartH = sdf.parse(exeStartDateH);
            String strExeStart = sdf.format(exeStartH);

            Date exeEndH = sdf.parse(exeEndDateH);
            String strExeEnd = sdf.format(exeEndH);

            int noofday = Utility.calculatetheDays(strExeStart, strExeEnd);
            info.addResult("inpcontractExeDays", noofday);
          }

        } else {
          info.addResult("inpcontractExeDays", '0');
        }

      }
      if (inpLastFieldChanged.equals("inpexeStartDateG")
          || inpLastFieldChanged.equals("inpexeEndDateG")) {
        String day, month, year;
        // LastField changes is Execution Start date (G)
        if (inpLastFieldChanged.equals("inpexeStartDateG")) {

          if (!StringUtils.isNotEmpty(exeStartDateG)) {
            info.addResult("inpexeStartDateH", "");
          }
          // To check greg date format or not
          if (exeStartDateG.matches("([0-9]{2})-([0-9]{2})-([0-9]{4})")) {
            String[] hijiriDate = exeStartDateG.split("-");// splits the string based on "-"
            day = hijiriDate[0];
            month = hijiriDate[1];
            year = hijiriDate[2];
            if (UtilityDAO.isGregorianDateValid(day, month, year)) {
              exeStartDateG = year + "-" + month + "-" + day;
              String exeStartHijiri = UtilityDAO.convertTohijriDate(exeStartDateG);
              info.addResult("inpexeStartDateH", exeStartHijiri);
            } else {
              errorFlag = true;
              info.addResult("inpexeStartDateH", "");
              info.addResult("inpexeStartDateG", "");
              info.addResult("ERROR", OBMessageUtils.messageBD("Escm_GregDate_Invalid"));
            }

          } else {
            errorFlag = true;
            info.addResult("inpexeStartDateH", "");
            info.addResult("inpexeStartDateG", "");
            if (!exeStartDateG.equals(""))
              info.addResult("ERROR", OBMessageUtils.messageBD("Escm_GregDate_Invalid"));

          }
        }
        // LastField changes is Execution End date (G)
        else {
          if (!StringUtils.isNotEmpty(exeEndDateG)) {
            info.addResult("inpexeEndDateH", "");
          }
          // To check Greg is valid date format or not
          if (exeEndDateG.matches("([0-9]{2})-([0-9]{2})-([0-9]{4})")) {
            String[] hijiriDate = exeEndDateG.split("-");// splits the string based on "-"
            day = hijiriDate[0];
            month = hijiriDate[1];
            year = hijiriDate[2];
            if (UtilityDAO.isGregorianDateValid(day, month, year)) {
              exeEndDateG = year + "-" + month + "-" + day;
              String exeEndHijiri = UtilityDAO.convertTohijriDate(exeEndDateG);
              info.addResult("inpexeEndDateH", exeEndHijiri);
            } else {
              errorFlag = true;
              info.addResult("inpexeEndDateH", "");
              info.addResult("inpexeEndDateG", "");
              info.addResult("ERROR", OBMessageUtils.messageBD("Escm_GregDate_Invalid"));
            }

          } else {
            errorFlag = true;
            info.addResult("inpexeEndDateH", "");
            info.addResult("inpexeEndDateG", "");
            if (!exeEndDateG.equals(""))
              info.addResult("ERROR", OBMessageUtils.messageBD("Escm_GregDate_Invalid"));

          }
          if (StringUtils.isNotEmpty(exeEndDateG) && !errorFlag) {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

            Date needByDateG = new java.util.Date(); // today
            String strNeedByDateG = df.format(needByDateG);
            String strNeedByDateH = UtilityDAO.convertToHijriDate(strNeedByDateG);
            Date needByDateH = sdf.parse(strNeedByDateH);
            String strNeedByDate = sdf.format(needByDateH);

            String strExeEndH = UtilityDAO.convertToHijriDate(exeEndDateG);
            Date strExeEndDate = sdf.parse(strExeEndH);
            String strExeEnd = sdf.format(strExeEndDate);

            int noofday = Utility.calculatetheDays(strNeedByDate, strExeEnd);
            if (noofday > 0) {
              info.addResult("inpcontractDelayDays", noofday);
            } else {
              info.addResult("inpcontractDelayDays", '0');
            }
          } else {
            info.addResult("inpcontractDelayDays", '0');
          }

        }

        if (inpLastFieldChanged.equals("inpexeStartDateG") && StringUtils.isNotEmpty(exeStartDateG)
            && !errorFlag) {
          SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
          SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

          java.util.Date exeStartDateGreg = df.parse(exeStartDateG);
          exeStartDateG = sdf.format(exeStartDateGreg);
        }

        // To calculate number of Contract Execution days based on Greg datee
        if (StringUtils.isNotEmpty(exeStartDateG)
            && StringUtils.isNotEmpty(vars.getStringParameter("inpexeEndDateG"))) {
          if (vars.getStringParameter("inpexeEndDateG").matches("([0-9]{2})-([0-9]{2})-([0-9]{4})")
              && exeStartDateG.matches("([0-9]{2})-([0-9]{2})-([0-9]{4})")) {
            String[] exeStartDate = exeStartDateG.split("-");// splits the string based on "-"
            String[] exeEndDate = vars.getStringParameter("inpexeEndDateG").split("-");// splits the
                                                                                       // string
                                                                                       // based on
                                                                                       // "-"
            if (UtilityDAO.isGregorianDateValid(exeEndDate[0], exeEndDate[1], exeEndDate[2])
                && UtilityDAO.isGregorianDateValid(exeStartDate[0], exeStartDate[1],
                    exeStartDate[2])) {
              // java.util.Date exeStartG = new SimpleDateFormat("dd-MM-yyyy").parse(exeStartDateG);
              // java.util.Date exeEndG = new SimpleDateFormat("dd-MM-yyyy")
              // .parse(vars.getStringParameter("inpexeEndDateG"));
              // LocalDate exeStartG1 = exeStartG.toInstant().atZone(ZoneId.systemDefault())
              // .toLocalDate();
              // LocalDate exeEndG1 =
              // exeEndG.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
              // long noOfDaysBetween = ChronoUnit.DAYS.between(exeStartG1, exeEndG1);
              // info.addResult("inpcontractExeDays", noOfDaysBetween);

              SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
              SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

              java.util.Date exeStartG = sdf.parse(exeStartDateG);
              java.util.Date exeEndG = sdf.parse(vars.getStringParameter("inpexeEndDateG"));

              String strExeStartG = df.format(exeStartG);
              String strExeStartH = UtilityDAO.convertToHijriDate(strExeStartG);
              Date exeStartH = sdf.parse(strExeStartH);
              String strExeStart = sdf.format(exeStartH);

              String strExeEndG = df.format(exeEndG);
              String strExeEndH = UtilityDAO.convertToHijriDate(strExeEndG);
              Date exeEndH = sdf.parse(strExeEndH);
              String strExeEnd = sdf.format(exeEndH);

              int noofday = Utility.calculatetheDays(strExeStart, strExeEnd);
              info.addResult("inpcontractExeDays", noofday);

            } else {
              info.addResult("inpcontractExeDays", '0');
            }

          } else {
            info.addResult("inpcontractExeDays", '0');
          }
        } else {
          info.addResult("inpcontractExeDays", '0');
        }
      }

      if (inpLastFieldChanged.equals("inpquantity") || inpLastFieldChanged.equals("inpunitprice")) {
        if (StringUtils.isNotEmpty(strQuantity) && StringUtils.isNotEmpty(strUnitPrice)) {
          BigDecimal quantity = new BigDecimal(strQuantity);
          BigDecimal unitPrice = new BigDecimal(strUnitPrice);

          info.addResult("inptotLineAmt", (quantity.multiply(unitPrice)));
        } else {
          info.addResult("inptotLineAmt", '0');
        }

      }
    } catch (Exception e) {
      log.debug("Exception in Requisition item callout:", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }

  }
}
