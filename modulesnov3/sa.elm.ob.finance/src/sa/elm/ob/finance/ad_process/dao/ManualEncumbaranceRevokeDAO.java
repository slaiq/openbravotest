package sa.elm.ob.finance.ad_process.dao;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.service.db.CallStoredProcedure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinBudManencumRev;
import sa.elm.ob.finance.EfinBudgetInquiry;
import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.ad_process.EncumbranceCancelProcess;
import sa.elm.ob.finance.ad_process.EncumbranceReactivate;

/**
 * @author Gowtham
 */

// DAO file for Encumbrance Revoke process
public class ManualEncumbaranceRevokeDAO {
  private static final Logger LOG = LoggerFactory.getLogger(ManualEncumbaranceRevokeDAO.class);

  /**
   * Revert the impacts of budget enquiry, which done in after budget control approver.
   * 
   * @param manEncumbarance
   */
  public static void revokeBudgetEnquiryImpact(EfinBudgetManencum manEncumbarance) {
    // revert the changes from budget enquiry
    try {
      for (EfinBudgetManencumlines encumLine : manEncumbarance.getEfinBudgetManencumlinesList()) {

        try {
          OBDal.getInstance().refresh(encumLine);
          final List<Object> parameters = new ArrayList<Object>();
          parameters.add(encumLine.getAccountingCombination().getId());
          parameters.add(encumLine.getRevamount().negate());
          parameters.add(manEncumbarance.getBudgetInitialization().getId());
          final String procedureName = "efin_updateBudgetInq";
          CallStoredProcedure.getInstance().call(procedureName, parameters, null, true, false);
        } catch (Exception e) {
          throw new OBException(e);
        }

        if (!encumLine.getAccountingCombination().isEFINDepartmentFund()) {
          EfinBudgetInquiry budInq = ManualEncumbaranceSubmitDAO.getBudgetInquiry(
              encumLine.getAccountingCombination().getId(),
              encumLine.getManualEncumbrance().getBudgetInitialization().getId());

          if (budInq != null) {
            budInq.setEncumbrance(budInq.getEncumbrance().subtract(encumLine.getRevamount()));
            OBDal.getInstance().save(budInq);
          }
        }
      }

      // set reservedfund as 'N'
      manEncumbarance.setReservedfund(false);
      OBDal.getInstance().save(manEncumbarance);
      OBDal.getInstance().flush();

    } catch (Exception e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Exception while revoke encumbracne:" + e, e);
      }
    }

  }

  /**
   * This method is used to cancelEncumbrance
   * 
   * @param encumbrance
   */
  public static void cancelEncumbrance(EfinBudgetManencum encumbrance) {
    try {

      revokeBudgetEnquiryImpact(encumbrance);
      encumbrance.setDocumentStatus(EncumbranceCancelProcess.CANCEL);

      OBDal.getInstance().save(encumbrance);
      OBDal.getInstance().flush();

    } catch (Exception e) {
      LOG.error("Exception while cancelEncumbrance: " + e);
      e.printStackTrace();
    }
  }

  /**
   * This method is used to reactivate encumbrance
   * 
   * @param encumbrance
   */
  public static void reactivateEncumbrance(EfinBudgetManencum encumbrance) {
    try {

      revokeBudgetEnquiryImpact(encumbrance);

      encumbrance.setDocumentStatus(EncumbranceReactivate.DRAFT);
      encumbrance.setAction(EncumbranceReactivate.APPROVED);

      OBDal.getInstance().save(encumbrance);
      OBDal.getInstance().flush();

    } catch (Exception e) {
      LOG.error("Exception while cancelEncumbrance: " + e);
    }
  }

  /**
   * Method to check whether Encumbrance is Modified or not
   * 
   * @param encumbrance
   * @return
   */
  public static boolean isEncumbranceModified(EfinBudgetManencum encumbrance) {
    Boolean isModified = Boolean.FALSE;
    try {
      for (EfinBudgetManencumlines encumLines : encumbrance.getEfinBudgetManencumlinesList()) {
        if (encumLines.getEfinBudManencumRevList().size() > 0) {
          isModified = Boolean.TRUE;
          break;
        }
      }
    } catch (Exception e) {
      LOG.error("Exception in isEncumbranceModified(): " + e);
    }
    return isModified;
  }

  /**
   * Method to check whether Encumbrance is Modified or not
   * 
   * @param encumbrance
   * @return
   */
  public static boolean isEncumbranceModifiedCheckAmt(EfinBudgetManencum encumbrance) {
    Boolean isModified = Boolean.FALSE;
    try {
      List<EfinBudgetManencumlines> encumLinesList = encumbrance.getEfinBudgetManencumlinesList();
      if (encumLinesList.size() > 0) {
        for (EfinBudgetManencumlines encumLines : encumLinesList) {
          if ((encumLines.getAPPAmt() != null
              && encumLines.getAPPAmt().compareTo(BigDecimal.ZERO) > 0)
              || (encumLines.getUsedAmount() != null
                  && encumLines.getUsedAmount().compareTo(BigDecimal.ZERO) > 0)
              || (encumLines.getSystemIncrease() != null
                  && encumLines.getSystemIncrease().compareTo(BigDecimal.ZERO) > 0)
              || (encumLines.getSystemDecrease() != null
                  && encumLines.getSystemDecrease().compareTo(BigDecimal.ZERO) > 0)) {
            isModified = Boolean.TRUE;
          }
        }
      }

    } catch (Exception e) {
      LOG.error("Exception in isEncumbranceModifiedCheckAmt(): " + e);
    }
    return isModified;
  }

  /**
   * Method to delete records in modification tab
   * 
   * @param encumbrance
   * @return
   */
  public static void deleteModificationRecords(EfinBudgetManencum encumbrance) {

    try {
      List<EfinBudgetManencumlines> encumLinesList = encumbrance.getEfinBudgetManencumlinesList();
      if (encumLinesList.size() > 0) {
        for (EfinBudgetManencumlines encumLines : encumLinesList) {
          OBQuery<EfinBudManencumRev> encumRevQry = OBDal.getInstance().createQuery(
              EfinBudManencumRev.class,
              "e where e.manualEncumbranceLines.id = :encumLineID and issystem = 'N'");
          encumRevQry.setNamedParameter("encumLineID", encumLines.getId());
          List<EfinBudManencumRev> encumRevList = encumRevQry.list();
          if (encumRevList.size() > 0) {
            for (EfinBudManencumRev encumRev : encumRevList) {
              OBDal.getInstance().remove(encumRev);
            }
          }
        }
        OBDal.getInstance().flush();
      }

    } catch (Exception e) {
      LOG.error("Exception in deleteModificationRecords(): " + e);
    }
  }
}
