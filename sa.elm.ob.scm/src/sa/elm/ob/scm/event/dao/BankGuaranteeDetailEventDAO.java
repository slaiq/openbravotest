package sa.elm.ob.scm.event.dao;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinRDV;
import sa.elm.ob.scm.ESCMBGAmtRevision;
import sa.elm.ob.scm.ESCMBGDocumentnoV;
import sa.elm.ob.scm.ESCMBGExtension;
import sa.elm.ob.scm.ESCMBGWorkbench;
import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.Escmbankguaranteedetail;
import sa.elm.ob.scm.ad_callouts.dao.BGWorkbenchDAO;
import sa.elm.ob.scm.ad_callouts.dao.OpenEnvelopeDAO;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

//BankGuaranteeDetail Event DAO file
public class BankGuaranteeDetailEventDAO {
  private static final Logger log = LoggerFactory.getLogger(BankGuaranteeDetailEventDAO.class);

  /**
   * check unique bg no based on business partner , bid , bank
   * 
   * @param bankguarantee
   * @return true if same bg no exists else return false
   */
  public static Boolean checkUniqueBGNo(Escmbankguaranteedetail bankguarantee) {
    try {
      OBContext.setAdminMode();
      OBQuery<Escmbankguaranteedetail> bgdetail = OBDal.getInstance().createQuery(
          Escmbankguaranteedetail.class,
          " as e where e.client.id=:clientID  and e.bankbgno=:bankNo "
              + " and e.escmBgworkbench.id =:bgWorkBenchID and e.bankName.id=:bankID ");
      bgdetail.setNamedParameter("clientID", bankguarantee.getClient().getId());
      bgdetail.setNamedParameter("bankNo", bankguarantee.getBankbgno());
      bgdetail.setNamedParameter("bgWorkBenchID", bankguarantee.getEscmBgworkbench().getId());
      bgdetail.setNamedParameter("bankID", bankguarantee.getBankName().getId());

      if (bgdetail.list().size() > 0) {
        return true;
      } else
        return false;

    } catch (OBException e) {
      log.error("Exception while checkUniqueBGNo:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * 
   * @param bankguarantee
   * @return
   */
  public static Boolean checksameDocNowithSameBank(Escmbankguaranteedetail bankguarantee) {
    try {
      OBContext.setAdminMode();
      OBQuery<Escmbankguaranteedetail> bankguaranteedet = OBDal.getInstance().createQuery(
          Escmbankguaranteedetail.class,
          " as e where e.client.id=:clientId and e.escmBgworkbench.id=:wrkbenchId "
              + " and e.bankbgno=:bankbgNo  and e.bankName.id=:bankName");
      bankguaranteedet.setNamedParameter("clientId", bankguarantee.getClient().getId());
      bankguaranteedet.setNamedParameter("wrkbenchId", bankguarantee.getEscmBgworkbench().getId());
      bankguaranteedet.setNamedParameter("bankbgNo", bankguarantee.getBankbgno());
      bankguaranteedet.setNamedParameter("bankName", bankguarantee.getBankName().getId());

      log.debug("checksameDocNowithSameBank:" + bankguaranteedet.list().size());
      if (bankguaranteedet.list().size() > 0) {
        return true;
      } else
        return false;

    } catch (OBException e) {
      log.error("Exception while checksameDocNowithSameBank:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * check entered intital bg is valid or not
   * 
   * @param bankguarantee
   */
  public static void validateBGValue(Escmbankguaranteedetail bankguarantee) {
    if (bankguarantee.getInitialbg() != null) {
      if (UtilityDAO.isNumber(bankguarantee.getInitialbg())) {
        if (!bankguarantee.getInitialbg().matches(Utility.zeroToTwoDecimalCheck)) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_Initialbg"));
        }
      }
    }
  }

  public static boolean validBGDatewithOpenEnvel(Escmbankguaranteedetail bankguarantee,
      EscmProposalMgmt proposal) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    try {
      OBContext.setAdminMode();
      String maxbidopenday = null, AftersixtyoneDays = null;
      String days = "60";
      Date AftersixtyoneDay = null;
      maxbidopenday = OpenEnvelopeDAO.getBidOpenenvelopday(proposal.getEscmBidmgmt().getId());

      AftersixtyoneDays = BGWorkbenchDAO.getRequestedExpirydate(maxbidopenday, days,
          bankguarantee.getClient().getId(), null);
      AftersixtyoneDays = UtilityDAO.eventConvertToGregorian(AftersixtyoneDays.split("-")[2]
          + AftersixtyoneDays.split("-")[1] + AftersixtyoneDays.split("-")[0]);
      AftersixtyoneDay = sdf.parse(AftersixtyoneDays);
      if (bankguarantee.getExpirydateh().compareTo(AftersixtyoneDay) < 0)
        return true;
      else
        return false;

    } catch (OBException e) {
      log.error("Exception while validBGDatewithOpenEnvel:" + e);
      throw new OBException(e.getMessage());
    } catch (ParseException e) {
      log.error("Exception while validBGDatewithOpenEnvel:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * check expiry date is conflict with some other record
   * 
   * @param bankguarantee
   * @param ReqExpiryDate
   * @return
   */
  public static Boolean chkexpirydateconflics(Escmbankguaranteedetail bankguarantee,
      String bgExtensionId, Date ReqExpiryDate) {
    try {
      OBContext.setAdminMode();
      SimpleDateFormat dateYearFormat = new SimpleDateFormat("yyyy-MM-dd");
      OBQuery<ESCMBGExtension> bgExtensionlist = OBDal.getInstance().createQuery(
          ESCMBGExtension.class, " as e where e.escmBankguaranteeDetail.id=:bgID and id <>:bgextID "
              + " and to_date(to_char(e.reqexpiryDate,'dd-MM-yyyy'),'dd-MM-yyyy') >=:expDate");
      bgExtensionlist.setNamedParameter("bgID", bankguarantee.getId());
      bgExtensionlist.setNamedParameter("bgextID", bgExtensionId);
      bgExtensionlist.setNamedParameter("expDate", ReqExpiryDate);

      if (bgExtensionlist.list().size() > 0) {
        return true;
      } else
        return false;
    } catch (OBException e) {
      log.error("Exception while chkexpirydateconflics:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public static EscmProposalAttribute getProposalAttribute(String ProposalId) {
    List<EscmProposalAttribute> attlist = new ArrayList<EscmProposalAttribute>();
    EscmProposalAttribute att = null;
    try {
      OBContext.setAdminMode();
      OBQuery<EscmProposalAttribute> proposalatt = OBDal.getInstance().createQuery(
          EscmProposalAttribute.class, " as e where e.escmProposalmgmt.id=:proposalID ");
      proposalatt.setNamedParameter("proposalID", ProposalId);
      proposalatt.setMaxResult(1);
      attlist = proposalatt.list();
      if (attlist.size() > 0) {
        att = attlist.get(0);
        return att;
      }
    } catch (OBException e) {
      log.error("Exception while getProposalAttribute:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return att;
  }

  public static Boolean chkBgExtProcessornot(Escmbankguaranteedetail bgdetail) {
    try {
      OBQuery<ESCMBGExtension> bgextension = OBDal.getInstance().createQuery(ESCMBGExtension.class,
          " as e where e.escmBankguaranteeDetail.id=:bgdetailID"
              + " and ( e.bankLetterRef is null or e.letterRefDate is null ) order by creationDate desc  ");
      bgextension.setNamedParameter("bgdetailID", bgdetail.getId());
      bgextension.setMaxResult(1);
      if (bgextension.list().size() > 0) {
        return true;
      } else
        return false;
    }

    catch (final Exception e) {
      log.error("Exception in chkBgExtProcessornot() Method : ", e);
    }
    return false;
  }

  public static Boolean chkBgAmtProcessornot(Escmbankguaranteedetail bgdetail) {
    try {
      OBQuery<ESCMBGAmtRevision> bgextension = OBDal.getInstance().createQuery(
          ESCMBGAmtRevision.class, " as e where e.escmBankguaranteeDetail.id=:bgdetailID "
              + " and (e.bankLetterReference is null or e.letterReferenceDateH is null )  order by creationDate desc  ");
      bgextension.setNamedParameter("bgdetailID", bgdetail.getId());
      bgextension.setMaxResult(1);
      if (bgextension.list().size() > 0) {
        return true;
      } else
        return false;
    }

    catch (final Exception e) {
      log.error("Exception in chkBgAmtProcessornot() Method : ", e);
    }
    return false;
  }

  public static Boolean chkBgExtProcessornot(Escmbankguaranteedetail bgdetail, long lineNo) {
    try {
      OBQuery<ESCMBGExtension> bgextension = OBDal.getInstance().createQuery(ESCMBGExtension.class,
          " as e where e.escmBankguaranteeDetail.id=:bgdetailID order by reqexpiryDate desc ");
      bgextension.setNamedParameter("bgdetailID", bgdetail.getId());
      bgextension.setMaxResult(1);
      if (bgextension.list().size() > 0) {
        if (lineNo != bgextension.list().get(0).getLineNo()) {
          return true;
        } else {
          return false;
        }
      } else {
        return false;
      }
    } catch (final Exception e) {
      log.error("Exception in chkBgAmtProcessornot() Method : ", e);
    }
    return false;
  }

  /**
   * Checks whether Amount revision can be deleted.
   * 
   * @param bgdetail
   * @param lineNo
   * @return boolean
   * 
   */
  public static Boolean chkBgAmtRevProcessornot(Escmbankguaranteedetail bgdetail, long lineNo) {
    try {
      OBQuery<ESCMBGAmtRevision> bgAmtRevQry = OBDal.getInstance().createQuery(
          ESCMBGAmtRevision.class,
          " as e where e.escmBankguaranteeDetail.id=:bgdetailID order by creationDate desc ");
      bgAmtRevQry.setNamedParameter("bgdetailID", bgdetail.getId());
      bgAmtRevQry.setMaxResult(1);
      List<ESCMBGAmtRevision> bgAmtRevLs = bgAmtRevQry.list();
      if (bgAmtRevLs.size() > 0) {
        if (lineNo != bgAmtRevLs.get(0).getLineNo()) {
          return true;
        } else {
          return false;
        }
      } else {
        return false;
      }
    } catch (final Exception e) {
      log.error("Exception in chkBgAmtRevProcessornot() Method : ", e);
    }
    return false;
  }

  /**
   * This method is to create header (escm_bgworkbench).
   * 
   * 
   * @param bankguarantee
   */

  public static ESCMBGWorkbench createWorkBench(Escmbankguaranteedetail bankguarantee) {

    ESCMBGWorkbench bgWorkbench = null;
    EscmProposalAttribute proposalAttr = bankguarantee.getEscmProposalAttr();
    EscmProposalMgmt proposal = null;
    String bgRate = "";
    List<ESCMBGWorkbench> detailList = null;
    if (proposalAttr != null) {
      detailList = proposalAttr.getESCMBGWorkbenchList();
    }

    if (proposalAttr != null) {
      proposal = proposalAttr.getEscmProposalmgmt();
    }

    if (detailList != null && detailList.size() > 0) {
      bgWorkbench = detailList.get(0);
    } else {

      bgWorkbench = OBProvider.getInstance().get(ESCMBGWorkbench.class);
      bgWorkbench.setClient(bankguarantee.getClient());
      bgWorkbench.setOrganization(bankguarantee.getOrganization());
      if (proposalAttr != null && proposal != null) {
        bgWorkbench.setFinancialYear(proposal.getFinancialYear());
        bgWorkbench.setVendorName(proposal.getSupplier());
        bgWorkbench.setPartnerAddress(proposal.getBranchName());
        ESCMBGDocumentnoV documentNo = OBDal.getInstance().get(ESCMBGDocumentnoV.class,
            proposal.getId());
        bgWorkbench.setDocumentNo(documentNo);
        bgWorkbench.setBidNo(proposal.getEscmBidmgmt());
        bgWorkbench.setDocumentAmount(proposalAttr.getNetPrice());
        bgWorkbench.setEscmProposalmgmt(proposal);
        bgWorkbench.setBidName(proposal.getEscmBidmgmt().getBidname());
        bgWorkbench.setEscmProposalAttr(proposalAttr);

        bgRate = BGWorkbenchDAO.getbidTermsValue(proposal.getEscmBidmgmt().getId(), "IBG",
            proposal.getClient().getId());
        bgWorkbench.setInitialBG(bgRate);

      }

      bgWorkbench.setCurrency(
          OBDal.getInstance().get(org.openbravo.model.common.currency.Currency.class, "317"));
      bgWorkbench.setDocumentType("P");
      bgWorkbench.setType("IBG");

      String sequence = Utility.getTransactionSequencewithclient("0",
          bankguarantee.getClient().getId(), "BGD");
      Boolean sequenceexists = Utility.chkTransactionSequencewithclient(
          bankguarantee.getOrganization().getId(), bankguarantee.getClient().getId(), "BGD",
          sequence);

      if (!sequenceexists) {
        throw new OBException(OBMessageUtils.messageBD("Escm_Duplicate_Sequence"));
      }

      // ** thorw the error if same sequence exists *//*
      if (sequence.equals("false") || StringUtils.isEmpty(sequence)) {
        throw new OBException(OBMessageUtils.messageBD("Escm_NoSequence"));
      } else {
        bgWorkbench.setInternalNo(Long.parseLong(sequence));
      }
      OBDal.getInstance().save(bankguarantee);

      OBQuery<Preference> bgSpecialist = OBDal.getInstance().createQuery(Preference.class,
          "as e where e.property='ESCM_BGSpecialist_Role' and e.searchKey='Y' "
              + " and e.client.id=:clientID and active='Y'");
      bgSpecialist.setNamedParameter("clientID", bankguarantee.getClient().getId());
      List<Preference> preference = bgSpecialist.list();
      if (preference != null && preference.size() > 0) {
        Preference contactName = preference.get(0);
        User user = contactName.getUserContact();
        Role role = contactName.getVisibleAtRole();
        if (user != null && user.getBusinessPartner() != null) {
          bgWorkbench.setContactName(user.getBusinessPartner());
        } else if (role != null) {
          List<UserRoles> userRoles = role.getADUserRolesList();
          if (userRoles != null && userRoles.size() > 0) {
            for (UserRoles usr : userRoles) {
              if (usr.getUserContact() != null) {
                if (usr.getUserContact().getBusinessPartner() != null)
                  bgWorkbench.setContactName(usr.getUserContact().getBusinessPartner());
                break;
              }
            }
          }
        }
      } else {
        throw new OBException(OBMessageUtils.messageBD("ESCM_NOBGSpecialistrole"));
      }
    }
    OBDal.getInstance().save(bankguarantee);
    return bgWorkbench;
  }

  public static int restrictReactivateBG(ESCMBGWorkbench bgworkbenchObj) {
    int count = 0;
    Order ord = null;
    try {
      if (bgworkbenchObj.getDocumentNo() != null)
        ord = OBDal.getInstance().get(Order.class, bgworkbenchObj.getDocumentNo().getId());
      // if order is linked with po receipt then should not allow to reactivate
      OBQuery<ShipmentInOut> poreceiptHeader = OBDal.getInstance().createQuery(ShipmentInOut.class,
          " as e where e.salesOrder.id=:SalesOrder");
      poreceiptHeader.setNamedParameter("SalesOrder", ord.getId());
      if (poreceiptHeader != null && poreceiptHeader.list().size() > 0) {
        count = 1;
      }
      // if order is linked with po match then should not allow to reactivate
      OBQuery<Invoice> poMatch = OBDal.getInstance().createQuery(Invoice.class,
          " as e where e.efinCOrder.id=:SalesOrder");
      poMatch.setNamedParameter("SalesOrder", ord.getId());
      if (poMatch != null && poMatch.list().size() > 0) {
        count = 2;
      }
      // if order is linked with rdv then should not allow to reactivate
      OBQuery<EfinRDV> rdvHeader = OBDal.getInstance().createQuery(EfinRDV.class,
          " as e where e.salesOrder.id=:SalesOrder");
      rdvHeader.setNamedParameter("SalesOrder", ord.getId());
      if (rdvHeader != null && rdvHeader.list().size() > 0) {
        count = 3;
      }
    } catch (final Exception e) {
      log.error("Exception in restrictReactivateBG() Method : ", e);
    }
    return count;
  }
}