package sa.elm.ob.scm.actionHandler.dao;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.hibernate.exception.GenericJDBCException;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.financialmgmt.calendar.Year;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DbUtility;

import sa.elm.ob.finance.EfinBudgetIntialization;
import sa.elm.ob.finance.ad_callouts.BudgetAdjustmentCallout;
import sa.elm.ob.finance.ad_callouts.dao.HijiridateDAO;
import sa.elm.ob.scm.ESCMBGWorkbench;
import sa.elm.ob.scm.ESCMCommittee;
import sa.elm.ob.scm.ESCMDefLookupsTypeLn;
import sa.elm.ob.scm.ESCMProposalEvlEvent;
import sa.elm.ob.scm.ESCM_Proposal_CommentAttr;
import sa.elm.ob.scm.EscmBidMgmt;
import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalRegulation;
import sa.elm.ob.scm.EscmProposalmgmtLine;
import sa.elm.ob.scm.EscmProposalmgmtLineVersion;
import sa.elm.ob.scm.EscmTechnicalevlEvent;
import sa.elm.ob.scm.Escmbankguaranteedetail;
import sa.elm.ob.scm.Escmbiddates;
import sa.elm.ob.scm.Escmbidmgmtline;
import sa.elm.ob.scm.Escmbidsuppliers;
import sa.elm.ob.scm.Escmopenenvcommitee;
import sa.elm.ob.scm.Escmsalesvoucher;
import sa.elm.ob.scm.actionHandler.irtabs.PEEOnSaveHandler;
import sa.elm.ob.scm.ad_process.BankGuarantee.BGWorkbenchProcess;
import sa.elm.ob.scm.ad_process.OpenEnvlopCommitee.OpenEnvlopCommiteeAction;
import sa.elm.ob.scm.ad_process.TechnicalEvaluationEvent.TechnicalEvaluationEventProcess;
import sa.elm.ob.scm.ad_process.proposalevaluationevent.ProposalEvaluationEventProcess;

public class BidMgmtActionHandlerDAOImpl implements BidMgmtActionHandlerDAO {

  private static final Logger log = Logger.getLogger(BidMgmtActionHandlerDAOImpl.class);

  @Override
  public JSONObject checkorcreateOEE(String strBidId) {
    JSONObject OEEreusltJson = new JSONObject();
    SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy-MM-dd");
    String openEnvelopDay = null;
    Date openEnvelopDate = null;
    ESCMDefLookupsTypeLn contCategoryObj = null;
    List<EscmProposalMgmt> proposalList = new ArrayList<EscmProposalMgmt>();
    try {
      OBContext.setAdminMode();
      OBQuery<Escmopenenvcommitee> oeeQuery = OBDal.getInstance()
          .createQuery(Escmopenenvcommitee.class, " as e where e.bidNo.id=:bidMgmtId ");
      oeeQuery.setNamedParameter("bidMgmtId", strBidId);
      if (oeeQuery.list().size() > 0) {
        Escmopenenvcommitee openEnvelopObj = oeeQuery.list().get(0);
        OEEreusltJson.put("result", "1");
        OEEreusltJson.put("openId", openEnvelopObj.getId());
      } else {
        EscmBidMgmt bidMgmtObj = OBDal.getInstance().get(EscmBidMgmt.class, strBidId);
        ESCMCommittee openEnvelopCommittee = null;

        // get open envelop committee
        OBQuery<ESCMCommittee> committeeQry = OBDal.getInstance().createQuery(ESCMCommittee.class,
            " as e where e.type='OEC' ");
        committeeQry.setMaxResult(1);
        if (committeeQry.list().size() > 0) {
          openEnvelopCommittee = committeeQry.list().get(0);
        }

        String strQuery = " select to_char(coalesce(max(openenvday), now()) ,'yyyy-MM-dd')  "
            + " from escm_biddates where escm_bidmgmt_id  =? ";
        Query query = OBDal.getInstance().getSession().createSQLQuery(strQuery);
        query.setParameter(0, strBidId);
        @SuppressWarnings("unchecked")
        List<Object> queryList = query.list();
        if (queryList.size() > 0) {
          Object row = queryList.get(0);
          openEnvelopDay = (String) row;
        }
        if (openEnvelopDay != null) {
          openEnvelopDate = yearFormat.parse(openEnvelopDay);
        }

        OBQuery<EscmProposalMgmt> proposalQry = OBDal.getInstance()
            .createQuery(EscmProposalMgmt.class, " as e where e.escmBidmgmt.id=:bidId and "
                + " e.contractType is not null  order by e.creationDate  desc ");
        proposalQry.setNamedParameter("bidId", bidMgmtObj.getId());
        proposalList = proposalQry.list();
        if (proposalList.size() > 0) {
          contCategoryObj = proposalList.get(0).getContractType();
        } else if (bidMgmtObj.getContractType() != null) {
          contCategoryObj = bidMgmtObj.getContractType();
        }
        // check how many proposal is exists for this bid
        Long proposalCount = (long) bidMgmtObj.getEscmProposalManagementList().size();
        Escmopenenvcommitee openEnvelopObj = OBProvider.getInstance()
            .get(Escmopenenvcommitee.class);
        openEnvelopObj.setOrganization(bidMgmtObj.getOrganization());
        openEnvelopObj.setPreparername(OBContext.getOBContext().getUser());
        openEnvelopObj.setBidNo(bidMgmtObj);
        openEnvelopObj.setBidName(bidMgmtObj.getBidname());
        openEnvelopObj.setCommitteeIDName(openEnvelopCommittee);
        openEnvelopObj.setProposalcount(proposalCount);
        openEnvelopObj.setApprovedbud(bidMgmtObj.getApprovedbudget().toString());
        openEnvelopObj.setAlertStatus("DR");
        openEnvelopObj.setEscmDocaction("CO");
        openEnvelopObj.setRefbutton(false);
        openEnvelopObj.setTodaydate(openEnvelopDate);
        openEnvelopObj.setRole(OBContext.getOBContext().getRole());
        openEnvelopObj.setContractType(contCategoryObj);
        OBDal.getInstance().save(openEnvelopObj);
        OBDal.getInstance().flush();
        OEEreusltJson.put("result", "1");
        OEEreusltJson.put("openId", openEnvelopObj.getId());
      }

    } catch (OBException e) {
      log.error("Exception while checkorcreateOEE:" + e);
      try {
        OEEreusltJson.put("result", "0");
        OEEreusltJson.put("errorMsg", e.getMessage());
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
      return OEEreusltJson;

    } catch (ParseException e) {
      log.error("Exception while checkorcreateOEE:" + e);
      try {
        OEEreusltJson.put("result", "0");
        OEEreusltJson.put("errorMsg", e.getMessage());
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
      return OEEreusltJson;
    }

    catch (JSONException e) {
      try {
        OEEreusltJson.put("result", "0");
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
      return OEEreusltJson;
    } finally {
      OBContext.restorePreviousMode();
    }
    return OEEreusltJson;

  }

  public JSONObject createPee(String strBidId) {
    JSONObject peereusltJson = new JSONObject();
    EscmTechnicalevlEvent techeventobj = null;
    String commiteeId = null;
    List<Escmopenenvcommitee> openlist = new ArrayList<Escmopenenvcommitee>();
    Escmopenenvcommitee openEnv = null;
    EscmBidMgmt bidMgmtObj = null;
    String proposalNo_Msg = null;
    try {
      OBContext.setAdminMode();
      if (strBidId != null)
        bidMgmtObj = OBDal.getInstance().get(EscmBidMgmt.class, strBidId);
      if (!bidMgmtObj.getBidtype().equals("DR")) {
        // check the tee is created for type tender and limited

        OBQuery<EscmTechnicalevlEvent> teeQuery = OBDal.getInstance().createQuery(
            EscmTechnicalevlEvent.class, " as e where e.bidNo.id=:bidMgmtId and e.status='CO' ");
        teeQuery.setNamedParameter("bidMgmtId", strBidId);
        if (teeQuery.list().size() == 0) {
          peereusltJson.put("result", "0");
          peereusltJson.put("errorMsg", OBMessageUtils.messageBD("Escm_CreateTee_Bid"));
          return peereusltJson;
        }
        // BG created
        /*
         * OBQuery<ESCMBGWorkbench> bgQuery = OBDal.getInstance().createQuery(ESCMBGWorkbench.class,
         * " as e where e.bidNo.id=:bidMgmtId and e.bghdstatus='CO' ");
         * bgQuery.setNamedParameter("bidMgmtId", strBidId); if (bgQuery.list().size() == 0) {
         * peereusltJson.put("result", "0"); peereusltJson.put("errorMsg",
         * OBMessageUtils.messageBD("Escm_CreateBG_Bid")); return peereusltJson; }
         */

      } else {
        // check the proposal is created or not
        OBQuery<EscmProposalMgmt> teeQuery = OBDal.getInstance().createQuery(EscmProposalMgmt.class,
            " as e where e.escmBidmgmt.id=:bidMgmtId and e.proposalstatus not in ('DR') ");
        teeQuery.setNamedParameter("bidMgmtId", strBidId);
        if (teeQuery.list().size() == 0) {
          peereusltJson.put("result", "0");
          peereusltJson.put("errorMsg", OBMessageUtils.messageBD("Escm_Createproposal_Bid"));
          return peereusltJson;
        }

      }

      // check if the proposal doesn't have BG
      if (!bidMgmtObj.getBidtype().equals("DR")) {
        proposalNo_Msg = PEEOnSaveHandler.integProsalAtttoProsalEvent(bidMgmtObj);
        if (proposalNo_Msg != null) {
          peereusltJson.put("BGDetail", proposalNo_Msg);
        }
      }
      OBQuery<ESCMProposalEvlEvent> peeQuery = OBDal.getInstance()
          .createQuery(ESCMProposalEvlEvent.class, " as e where e.bidNo.id=:bidMgmtId ");
      peeQuery.setNamedParameter("bidMgmtId", strBidId);
      if (peeQuery.list().size() > 0) {
        ESCMProposalEvlEvent openEnvelopObj = peeQuery.list().get(0);
        peereusltJson.put("result", "1");
        peereusltJson.put("peeId", openEnvelopObj.getId());
      } else {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        ESCMProposalEvlEvent proposalEval = OBProvider.getInstance()
            .get(ESCMProposalEvlEvent.class);
        proposalEval.setOrganization(bidMgmtObj.getOrganization());
        proposalEval.setDateHijri(new java.util.Date());
        proposalEval.setDateGregorian(dateFormat.format(new java.util.Date()));
        proposalEval.setTimeEvaluation(timeFormat.format(new java.util.Date()));
        proposalEval.setPreparerIDName(OBContext.getOBContext().getUser());
        commiteeId = getCommiteeIdName(bidMgmtObj.getBidtype(), bidMgmtObj.getClient().getId());
        proposalEval.setEscmCommittee(OBDal.getInstance().get(ESCMCommittee.class, commiteeId));

        proposalEval.setBidNo(bidMgmtObj);
        proposalEval.setBidName(bidMgmtObj.getBidname());

        if (bidMgmtObj.isPartialaward()) {
          proposalEval.setPartialaward(true);
          proposalEval.setAwardfullqty(true);
        }

        if (!bidMgmtObj.getBidtype().equals("DR")) {
          // get tee details
          OBQuery<EscmTechnicalevlEvent> techEvlevent = OBDal.getInstance().createQuery(
              EscmTechnicalevlEvent.class, " as e where e.bidNo.id=:bidId and e.status='CO' ");
          techEvlevent.setNamedParameter("bidId", strBidId);
          techEvlevent.setMaxResult(1);
          if (techEvlevent.list().size() > 0) {
            techeventobj = techEvlevent.list().get(0);
          }
          if (techeventobj != null) {
            proposalEval.setEscmTechnicalevlEvent(techeventobj);
            proposalEval.setTECDateHijri(techeventobj.getDateH());

          }
          // getoee details
          OBQuery<Escmopenenvcommitee> openenvelop = OBDal.getInstance()
              .createQuery(Escmopenenvcommitee.class, " as e where e.bidNo.id=:bidId ");
          openenvelop.setNamedParameter("bidId", strBidId);
          openenvelop.setMaxResult(1);
          openlist = openenvelop.list();
          if (openlist.size() > 0) {
            openEnv = openlist.get(0);
          }
          if (openEnv != null) {
            proposalEval.setEscmOpenenvcommitee(openEnv);
            proposalEval.setEnvelopeDate(openEnv.getTodaydate());
            proposalEval.setProposalCounts(openEnv.getProposalcount());

          }

        }
        proposalEval.setApprovedBudgetSAR(bidMgmtObj.getApprovedbudget());

        OBDal.getInstance().save(proposalEval);
        OBDal.getInstance().flush();
        peereusltJson.put("result", "1");
        peereusltJson.put("peeId", proposalEval.getId());
      }

    } catch (OBException e) {
      log.error("Exception while createPee:" + e);
      try {
        peereusltJson.put("result", "0");
        peereusltJson.put("errorMsg", e.getMessage());
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
      return peereusltJson;

    } catch (JSONException e) {
      try {
        peereusltJson.put("result", "0");
        peereusltJson.put("errorMsg", e.getMessage());
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
      return peereusltJson;
    } catch (GenericJDBCException e) {
      OBDal.getInstance().rollbackAndClose();
      Throwable t = DbUtility.getUnderlyingSQLException(e);
      String errorMessage = OBMessageUtils.translateError(t.getMessage()).getMessage();
      try {
        peereusltJson.put("result", "0");
        peereusltJson.put("errorMsg", errorMessage);
      } catch (Exception e1) {
        e1.printStackTrace();
      }
    }

    finally {
      OBContext.restorePreviousMode();
    }
    return peereusltJson;

  }

  public String getCommiteeIdName(String bidType, String clientId) {
    PreparedStatement st = null;
    ResultSet rs = null;
    String commiteeID = null;
    try {

      st = OBDal.getInstance().getConnection()
          .prepareStatement("select escm_committee_id from escm_committee where type =(case when '"
              + bidType + "'='TR' OR  '" + bidType + "'='LD' then 'TL' " + "     else 'DPC' end)  "
              + "AND " + " status='CO' and ad_client_id = '" + clientId + "'");
      rs = st.executeQuery();
      if (rs.next()) {
        commiteeID = rs.getString("escm_committee_id");
      }
    } catch (Exception e) {
      log.error("Exception in getCommiteeIdName", e);
    }
    return commiteeID;

  }

  public String getCommiteeIdName(String clientId) {
    PreparedStatement st = null;
    ResultSet rs = null;
    String commiteeID = null;
    try {

      st = OBDal.getInstance().getConnection()
          .prepareStatement("select escm_committee_id from escm_committee where type ='TC' "
              + " AND  status='CO' and ad_client_id = '" + clientId + "'");
      rs = st.executeQuery();
      if (rs.next()) {
        commiteeID = rs.getString("escm_committee_id");
      }
    } catch (Exception e) {
      log.error("Exception in getCommiteeIdName", e);
    }
    return commiteeID;
  }

  @Override
  public JSONObject createProposal(String strBidId, String proposalAction, String cBpartnerId,
      String contractCategoryID) {
    JSONObject ProposalListJson = new JSONObject();
    JSONObject ProposalreusltJson = new JSONObject();
    List<Escmbidsuppliers> supplierList = new ArrayList<Escmbidsuppliers>();
    List<Escmsalesvoucher> rfpSupplierList = new ArrayList<Escmsalesvoucher>();
    try {
      OBContext.setAdminMode();
      ProposalreusltJson.put("result", "0");
      EscmBidMgmt bidMgmtObj = OBDal.getInstance().get(EscmBidMgmt.class, strBidId);

      if (bidMgmtObj != null) {

        if (proposalAction.equals("PD")) {
          if (bidMgmtObj.getEscmProposalManagementList().size() > 0) {
            ProposalListJson.put("tabId", "D6115C9AF1DD4C4C9811D2A69E42878B");
            ProposalListJson.put("filterClause", "e.escmBidmgmt.id='" + strBidId + "'");
            ProposalListJson.put("wait", true);
            ProposalreusltJson.put("proposalList", ProposalListJson);
            ProposalreusltJson.put("result", "1");
            return ProposalreusltJson;
          } else {
            ProposalreusltJson.put("result", "4");
            return ProposalreusltJson;
          }
        }
        if (proposalAction.equals("PCFL")) {
          if (bidMgmtObj.getBidtype().equals("DR") || bidMgmtObj.getBidtype().equals("LD")) {
            if (bidMgmtObj.getEscmBidsuppliersList().size() > 0) {
              OBQuery<Escmbidsuppliers> suppliersQry = OBDal.getInstance().createQuery(
                  Escmbidsuppliers.class,
                  " as e where e.suppliernumber.id not in ( select proposal.supplier.id from Escm_Proposal_Management proposal"
                      + " where proposal.escmBidmgmt.id=:bidMgmtId ) and  e.escmBidmgmt.id=:bidMgmtId  ");
              suppliersQry.setNamedParameter("bidMgmtId", bidMgmtObj.getId());
              suppliersQry.setNamedParameter("bidMgmtId", bidMgmtObj.getId());
              supplierList = suppliersQry.list();
            }
            if (supplierList.size() > 0) {
              for (Escmbidsuppliers supplierObj : supplierList) {
                JSONObject proposalJson = insertNewProposal(supplierObj.getSuppliernumber().getId(),
                    bidMgmtObj, supplierObj, null, contractCategoryID);

                if (proposalJson.has("result") && proposalJson.getString("result").equals("0")) {
                  OBDal.getInstance().rollbackAndClose();
                  ProposalreusltJson.put("result", "0");
                  ProposalreusltJson.put("errorMsg", proposalJson.getString("errorMsg"));
                  return ProposalreusltJson;
                }
              }

              if (bidMgmtObj.getEscmProposalManagementList().size() > 0) {
                for (EscmProposalMgmt proposal : bidMgmtObj.getEscmProposalManagementList()) {
                  OBDal.getInstance().refresh(proposal);
                  for (EscmProposalmgmtLine line : proposal.getEscmProposalmgmtLineList()) {
                    line.setProcess(false);
                    OBDal.getInstance().save(line);
                  }
                }
                OBDal.getInstance().flush();
              }

              ProposalreusltJson.put("result", "1");
              ProposalListJson.put("tabId", "D6115C9AF1DD4C4C9811D2A69E42878B");
              ProposalListJson.put("filterClause", "e.escmBidmgmt.id='" + strBidId + "'");
              ProposalListJson.put("wait", true);
              ProposalreusltJson.put("proposalList", ProposalListJson);
              return ProposalreusltJson;
            }
          }
          if (bidMgmtObj.getBidtype().equals("TR")) {
            OBQuery<Escmsalesvoucher> rfpSupplierQry = OBDal.getInstance().createQuery(
                Escmsalesvoucher.class,
                " as e where e.supplierNumber.id not in ( select proposal.supplier.id from Escm_Proposal_Management proposal"
                    + " where proposal.escmBidmgmt.id=:bidMgmtId )  and e.documentStatus='CO' and  e.escmBidmgmt.id=:bidMgmtId  ");
            rfpSupplierQry.setNamedParameter("bidMgmtId", bidMgmtObj.getId());
            rfpSupplierQry.setNamedParameter("bidMgmtId", bidMgmtObj.getId());
            rfpSupplierList = rfpSupplierQry.list();
            if (rfpSupplierList.size() > 0) {
              for (Escmsalesvoucher supplierObj : rfpSupplierList) {
                JSONObject proposalJson = insertNewProposal(supplierObj.getSupplierNumber().getId(),
                    bidMgmtObj, null, supplierObj, contractCategoryID);
                if (proposalJson.has("result") && proposalJson.getString("result").equals("0")) {
                  OBDal.getInstance().rollbackAndClose();
                  ProposalreusltJson.put("result", "0");
                  ProposalreusltJson.put("errorMsg", proposalJson.getString("errorMsg"));
                  return ProposalreusltJson;
                }
              }

              if (bidMgmtObj.getEscmProposalManagementList().size() > 0) {
                for (EscmProposalMgmt proposal : bidMgmtObj.getEscmProposalManagementList()) {
                  OBDal.getInstance().refresh(proposal);
                  for (EscmProposalmgmtLine line : proposal.getEscmProposalmgmtLineList()) {
                    line.setProcess(false);
                    OBDal.getInstance().save(line);
                  }
                }
              }

              ProposalreusltJson.put("result", "1");
              ProposalListJson.put("tabId", "D6115C9AF1DD4C4C9811D2A69E42878B");
              ProposalListJson.put("filterClause", "e.escmBidmgmt.id='" + strBidId + "'");
              ProposalListJson.put("wait", true);
              ProposalreusltJson.put("proposalList", ProposalListJson);
              return ProposalreusltJson;
            }
          }
          if (bidMgmtObj.getEscmProposalManagementList().size() > 0) {
            // if already proposal created then return '2'
            ProposalreusltJson.put("result", "2");
            ProposalListJson.put("tabId", "D6115C9AF1DD4C4C9811D2A69E42878B");
            ProposalListJson.put("filterClause", "e.escmBidmgmt.id='" + strBidId + "'");
            ProposalListJson.put("wait", true);
            ProposalreusltJson.put("proposalList", ProposalListJson);
            return ProposalreusltJson;
          }
          // if no supplier to create the proposal then return as '3'
          else if (bidMgmtObj.getEscmProposalManagementList().size() == 0) {
            ProposalreusltJson.put("result", "3");
            return ProposalreusltJson;
          }
        }

        if (proposalAction.equals("PC")) {
          JSONObject proposalJson = insertNewProposal(cBpartnerId, bidMgmtObj, null, null,
              contractCategoryID);
          if (bidMgmtObj.getEscmProposalManagementList().size() > 0) {
            for (EscmProposalMgmt proposal : bidMgmtObj.getEscmProposalManagementList()) {
              OBDal.getInstance().refresh(proposal);
              for (EscmProposalmgmtLine line : proposal.getEscmProposalmgmtLineList()) {
                line.setProcess(false);
                OBDal.getInstance().save(line);
              }
            }
          }

          if (proposalJson.has("result")) {
            if (proposalJson.getString("result").equals("1")
                && proposalJson.getString("proposal") != null) {
              if (bidMgmtObj.getEscmProposalManagementList().size() > 0) {
                ProposalreusltJson.put("result", "1");
                ProposalListJson.put("tabId", "D6115C9AF1DD4C4C9811D2A69E42878B");
                ProposalListJson.put("filterClause",
                    "e.id='" + proposalJson.getString("proposal") + "'");
                ProposalListJson.put("wait", true);
                ProposalreusltJson.put("proposalList", ProposalListJson);
                return ProposalreusltJson;
              }
            } else {
              OBDal.getInstance().rollbackAndClose();
              ProposalreusltJson.put("result", "0");
              ProposalreusltJson.put("errorMsg", proposalJson.getString("errorMsg"));
              return ProposalreusltJson;
            }
          }

        }
      }
    } catch (OBException e) {
      log.error("Exception while createProposal:" + e);
      try {
        ProposalreusltJson.put("result", "0");
        ProposalreusltJson.put("errorMsg", e.getMessage());
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
      return ProposalreusltJson;

    } catch (JSONException e) {
      try {
        ProposalreusltJson.put("result", "0");
        ProposalreusltJson.put("errorMsg", e.getMessage());
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
      return ProposalreusltJson;
    } finally {
      OBContext.restorePreviousMode();
    }
    return ProposalreusltJson;
  }

  @Override
  public JSONObject insertNewProposal(String cBpartnerId, EscmBidMgmt bidMgmtObj,
      Escmbidsuppliers suppliers, Escmsalesvoucher rfpSuppliers, String contractCategoryID) {
    EscmProposalMgmt propMgmt = null;
    JSONObject ProposalreusltJson = new JSONObject();
    Date today = new Date();
    String dateFormat = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("dateFormat.java");
    SimpleDateFormat dateFormater = new SimpleDateFormat(dateFormat);
    SimpleDateFormat hoursFormat = new SimpleDateFormat("HH:mm");
    String budgInitialId = null;
    String strProposalWindowId = "CAF2D3EEF3B241018C8F65E8F877B29F";
    String yearId = null;
    Location branchName = null;
    try {
      OBContext.setAdminMode();

      if (bidMgmtObj.getOrganization().getCurrency() == null) {
        ProposalreusltJson.put("result", "0");
        ProposalreusltJson.put("errorMsg", OBMessageUtils.messageBD("ESCM_OrgCurrencyNotDef"));
        return ProposalreusltJson;
      }
      BusinessPartner supplier = OBDal.getInstance().get(BusinessPartner.class, cBpartnerId);

      if (suppliers != null) {
        branchName = suppliers.getBranchname();
      } else if (rfpSuppliers != null) {
        branchName = rfpSuppliers.getBranchName();
      } else if (cBpartnerId != null) {
        if (supplier.getBusinessPartnerLocationList().size() > 0) {
          branchName = supplier.getBusinessPartnerLocationList().get(0);
        }
      }

      propMgmt = OBProvider.getInstance().get(EscmProposalMgmt.class);
      propMgmt.setClient(bidMgmtObj.getClient());
      propMgmt.setOrganization(bidMgmtObj.getOrganization());
      propMgmt.setProposalType(bidMgmtObj.getBidtype());
      propMgmt.setBuyername(OBContext.getOBContext().getUser());
      propMgmt.setBranchName(branchName);
      propMgmt.setProposalstatus("SUB");
      propMgmt.setEscmDocaction("RE");
      // propMgmt.setProposalno(
      // UtilityDAO.getTransactionSequence(bidMgmtObj.getOrganization().getId(), "PMG"));
      propMgmt.setBidName(bidMgmtObj.getBidname());
      propMgmt.setBidType(bidMgmtObj.getBidtype());
      if (bidMgmtObj.getOrganization().getCurrency() != null) {
        propMgmt.setCurrency(bidMgmtObj.getOrganization().getCurrency());
      }
      propMgmt.setEscmBidmgmt(bidMgmtObj);
      propMgmt.setSubmissiondate(dateFormater.parse(dateFormater.format(today)));
      propMgmt.setSubmissiontime(hoursFormat.format(today));
      propMgmt.setSupplier(supplier);
      propMgmt.setRole(OBContext.getOBContext().getRole());

      yearId = HijiridateDAO.getYearId(today, bidMgmtObj.getClient().getId());

      propMgmt.setFinancialYear(OBDal.getInstance().get(Year.class, yearId));
      propMgmt.setApprovedBudgetSAR(bidMgmtObj.getApprovedbudget());
      budgInitialId = BudgetAdjustmentCallout.getBudgetDefinitionForStartDate(today,
          bidMgmtObj.getClient().getId(), strProposalWindowId);
      propMgmt.setEfinBudgetinitial(
          OBDal.getInstance().get(EfinBudgetIntialization.class, budgInitialId));
      if (contractCategoryID != null && bidMgmtObj.getContractType() == null) {
        propMgmt.setContractType(
            OBDal.getInstance().get(ESCMDefLookupsTypeLn.class, contractCategoryID));
      } else {
        propMgmt.setContractType(bidMgmtObj.getContractType());
      }
      OBDal.getInstance().save(propMgmt);
      OBDal.getInstance().flush();

      ProposalreusltJson.put("result", "1");
      ProposalreusltJson.put("proposal", propMgmt.getId());

    } catch (OBException e) {
      log.error("Exception while insertNewProposal:" + e);
      try {
        ProposalreusltJson.put("result", "0");
        ProposalreusltJson.put("errorMsg", e.getMessage());
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
      return ProposalreusltJson;
    } catch (ParseException e) {
      log.error("Exception while insertNewProposal:" + e);
      try {
        ProposalreusltJson.put("result", "0");
        ProposalreusltJson.put("errorMsg", e.getMessage());
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
      return ProposalreusltJson;
    } catch (JSONException e) {
      try {
        ProposalreusltJson.put("result", "0");
        ProposalreusltJson.put("errorMsg", e.getMessage());
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
      return ProposalreusltJson;
    } finally {
      OBContext.restorePreviousMode();
    }
    return ProposalreusltJson;
  }

  @Override
  public JSONObject createProposalFromOEE(String openEnvelopId, String proposalAction,
      String cBpartnerId, String contractCategoryID) {
    JSONObject ProposalreusltJson = new JSONObject();
    List<Escmbidsuppliers> supplierList = new ArrayList<Escmbidsuppliers>();
    List<Escmsalesvoucher> rfpSupplierList = new ArrayList<Escmsalesvoucher>();
    JSONObject ProposalListJson = new JSONObject();
    try {
      OBContext.setAdminMode();
      // first set the result as '0'
      ProposalreusltJson.put("result", "0");

      Escmopenenvcommitee openEnvelopObj = OBDal.getInstance().get(Escmopenenvcommitee.class,
          openEnvelopId);
      EscmBidMgmt bidMgmtObj = openEnvelopObj.getBidNo();
      if (bidMgmtObj != null) {
        // proposal create from list
        if (proposalAction.equals("PCFL")) {
          // Direct and limited case
          if (bidMgmtObj.getBidtype().equals("DR") || bidMgmtObj.getBidtype().equals("LD")) {

            // fetch bid suppliers
            OBQuery<Escmbidsuppliers> suppliersQry = OBDal.getInstance().createQuery(
                Escmbidsuppliers.class,
                " as e where e.suppliernumber.id not in ( select proposal.supplier.id from Escm_Proposal_Management proposal"
                    + " where proposal.escmBidmgmt.id=:bidMgmtId ) and  e.escmBidmgmt.id=:bidMgmtId  ");
            suppliersQry.setNamedParameter("bidMgmtId", bidMgmtObj.getId());
            suppliersQry.setNamedParameter("bidMgmtId", bidMgmtObj.getId());
            supplierList = suppliersQry.list();

            if (supplierList.size() > 0) {
              for (Escmbidsuppliers supplierObj : supplierList) {
                JSONObject proposalJson = insertNewProposal(supplierObj.getSuppliernumber().getId(),
                    bidMgmtObj, supplierObj, null, contractCategoryID);
                if (proposalJson.has("result")) {
                  if (proposalJson.getString("result").equals("0")) {
                    OBDal.getInstance().rollbackAndClose();
                    ProposalreusltJson.put("result", "0");
                    ProposalreusltJson.put("errorMsg", proposalJson.getString("errorMsg"));
                    return ProposalreusltJson;
                  }
                  if (proposalJson.getString("result").equals("1")
                      && proposalJson.has("proposal")) {
                    EscmProposalMgmt proposal = OBDal.getInstance().get(EscmProposalMgmt.class,
                        proposalJson.getString("proposal"));
                    JSONObject proposalAttJson = insertProposalAttribute(proposal, openEnvelopObj);
                    if (proposalAttJson.has("result")
                        && proposalAttJson.getString("result").equals("0")) {
                      OBDal.getInstance().rollbackAndClose();
                      ProposalreusltJson.put("result", "0");
                      ProposalreusltJson.put("errorMsg", proposalJson.getString("errorMsg"));
                      return ProposalreusltJson;
                    }
                  }
                }
              }

              if (bidMgmtObj.getEscmProposalManagementList().size() > 0) {
                for (EscmProposalMgmt proposal : bidMgmtObj.getEscmProposalManagementList()) {
                  OBDal.getInstance().refresh(proposal);
                  for (EscmProposalmgmtLine line : proposal.getEscmProposalmgmtLineList()) {
                    line.setProcess(false);
                    OBDal.getInstance().save(line);
                  }
                }
              }

              // if all proposal inserted then return result as '1'
              ProposalreusltJson.put("result", "1");
              ProposalListJson.put("tabId", "D6115C9AF1DD4C4C9811D2A69E42878B");
              ProposalListJson.put("filterClause", "e.escmBidmgmt.id='" + bidMgmtObj.getId() + "'");
              ProposalListJson.put("wait", true);
              ProposalreusltJson.put("proposalList", ProposalListJson);
              return ProposalreusltJson;
            }
          }
          // Tender Case
          else if (bidMgmtObj.getBidtype().equals("TR")) {

            // fetching supplier from the RFP sales voucher
            OBQuery<Escmsalesvoucher> rfpSupplierQry = OBDal.getInstance().createQuery(
                Escmsalesvoucher.class,
                " as e where e.supplierNumber.id not in ( select proposal.supplier.id from Escm_Proposal_Management proposal"
                    + " where proposal.escmBidmgmt.id=:bidMgmtId ) and e.documentStatus='CO'  and  e.escmBidmgmt.id=:bidMgmtId  ");
            rfpSupplierQry.setNamedParameter("bidMgmtId", bidMgmtObj.getId());
            rfpSupplierQry.setNamedParameter("bidMgmtId", bidMgmtObj.getId());
            rfpSupplierList = rfpSupplierQry.list();
            if (rfpSupplierList.size() > 0) {
              for (Escmsalesvoucher supplierObj : rfpSupplierList) {

                JSONObject proposalJson = insertNewProposal(supplierObj.getSupplierNumber().getId(),
                    bidMgmtObj, null, supplierObj, contractCategoryID);
                if (proposalJson.has("result")) {
                  if (proposalJson.getString("result").equals("0")) {
                    OBDal.getInstance().rollbackAndClose();
                    ProposalreusltJson.put("result", "0");
                    ProposalreusltJson.put("errorMsg", proposalJson.getString("errorMsg"));
                    return ProposalreusltJson;
                  }
                  if (proposalJson.getString("result").equals("1")
                      && proposalJson.has("proposal")) {
                    EscmProposalMgmt proposal = OBDal.getInstance().get(EscmProposalMgmt.class,
                        proposalJson.getString("proposal"));
                    JSONObject proposalAttJson = insertProposalAttribute(proposal, openEnvelopObj);
                    if (proposalAttJson.has("result")
                        && proposalAttJson.getString("result").equals("0")) {
                      OBDal.getInstance().rollbackAndClose();
                      ProposalreusltJson.put("result", "0");
                      ProposalreusltJson.put("errorMsg", proposalJson.getString("errorMsg"));
                      return ProposalreusltJson;
                    }
                  }
                }
              }
              if (bidMgmtObj.getEscmProposalManagementList().size() > 0) {
                for (EscmProposalMgmt proposal : bidMgmtObj.getEscmProposalManagementList()) {
                  OBDal.getInstance().refresh(proposal);
                  for (EscmProposalmgmtLine line : proposal.getEscmProposalmgmtLineList()) {
                    line.setProcess(false);
                    OBDal.getInstance().save(line);
                  }
                }
              }

              // if all proposal inserted then return result as '1'
              ProposalreusltJson.put("result", "1");
              ProposalListJson.put("tabId", "D6115C9AF1DD4C4C9811D2A69E42878B");
              ProposalListJson.put("filterClause", "e.escmBidmgmt.id='" + bidMgmtObj.getId() + "'");
              ProposalListJson.put("wait", true);
              ProposalreusltJson.put("proposalList", ProposalListJson);
              return ProposalreusltJson;
            }
          }

          if (bidMgmtObj.getEscmProposalManagementList().size() > 0) {
            // if already proposal created then return '2'
            ProposalreusltJson.put("result", "2");
            ProposalListJson.put("tabId", "D6115C9AF1DD4C4C9811D2A69E42878B");
            ProposalListJson.put("filterClause", "e.escmBidmgmt.id='" + bidMgmtObj.getId() + "'");
            ProposalListJson.put("wait", true);
            ProposalreusltJson.put("proposalList", ProposalListJson);
            return ProposalreusltJson;
          }
          // if no supplier to create the proposal then return as '3'
          else if (bidMgmtObj.getEscmProposalManagementList().size() == 0) {
            ProposalreusltJson.put("result", "3");
            return ProposalreusltJson;
          }
        }
        if (proposalAction.equals("PC")) {
          JSONObject proposalJson = insertNewProposal(cBpartnerId, bidMgmtObj, null, null,
              contractCategoryID);
          if (bidMgmtObj.getEscmProposalManagementList().size() > 0) {
            for (EscmProposalMgmt proposal : bidMgmtObj.getEscmProposalManagementList()) {
              OBDal.getInstance().refresh(proposal);
              for (EscmProposalmgmtLine line : proposal.getEscmProposalmgmtLineList()) {
                line.setProcess(false);
                OBDal.getInstance().save(line);
              }
            }
          }

          if (proposalJson.has("result")) {
            if (proposalJson.getString("result").equals("0")) {
              OBDal.getInstance().rollbackAndClose();
              ProposalreusltJson.put("result", "0");
              ProposalreusltJson.put("errorMsg", proposalJson.getString("errorMsg"));
              return ProposalreusltJson;
            }
            if (proposalJson.getString("result").equals("1") && proposalJson.has("proposal")) {

              EscmProposalMgmt proposal = OBDal.getInstance().get(EscmProposalMgmt.class,
                  proposalJson.getString("proposal"));
              JSONObject proposalAttJson = insertProposalAttribute(proposal, openEnvelopObj);
              if (proposalAttJson.has("result")) {
                if (proposalAttJson.getString("result").equals("0")) {
                  OBDal.getInstance().rollbackAndClose();
                  ProposalreusltJson.put("result", "0");
                  ProposalreusltJson.put("errorMsg", proposalJson.getString("errorMsg"));
                  return ProposalreusltJson;
                } else if (proposalAttJson.getString("result").equals("1")) {
                  ProposalreusltJson.put("result", "1");
                  if (bidMgmtObj.getEscmProposalManagementList().size() > 0) {
                    ProposalListJson.put("tabId", "D6115C9AF1DD4C4C9811D2A69E42878B");
                    ProposalListJson.put("filterClause",
                        "e.id='" + proposalJson.getString("proposal") + "'");
                    ProposalListJson.put("wait", true);
                    ProposalreusltJson.put("proposalList", ProposalListJson);
                    return ProposalreusltJson;
                  }
                }
              }
            }
          }
        }
      }
    } catch (

    OBException e) {
      log.error("Exception while checkorcreateOEE:" + e);
      try {
        ProposalreusltJson.put("result", "0");
        ProposalreusltJson.put("errorMsg", e.getMessage());
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
      return ProposalreusltJson;

    } catch (JSONException e) {
      try {
        ProposalreusltJson.put("result", "0");
        ProposalreusltJson.put("errorMsg", e.getMessage());
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
      return ProposalreusltJson;
    } finally {
      OBContext.restorePreviousMode();
    }
    return ProposalreusltJson;
  }

  public JSONObject insertProposalAttribute(EscmProposalMgmt proposal,
      Escmopenenvcommitee openEnvelopObj) {
    EscmProposalAttribute proposalAttribute = null;
    JSONObject ProposalAttJson = new JSONObject();
    Long line = (long) 0;
    try {
      OBContext.setAdminMode();

      line = (long) (openEnvelopObj.getEscmProposalAttrList().size() * 10 + 10);

      proposalAttribute = OBProvider.getInstance().get(EscmProposalAttribute.class);
      proposalAttribute.setClient(openEnvelopObj.getClient());
      proposalAttribute.setOrganization(openEnvelopObj.getOrganization());
      proposalAttribute.setLineNo(line);
      proposalAttribute.setEscmProposalmgmt(proposal);
      proposalAttribute.setEscmOpenenvcommitee(openEnvelopObj);
      proposalAttribute.setSupplier(proposal.getSupplier());
      proposalAttribute.setBranchName(proposal.getBranchName());
      proposalAttribute.setCurrency(proposal.getCurrency());
      proposalAttribute.setGrossPrice(BigDecimal.ONE);
      proposalAttribute.setNetPrice(BigDecimal.ONE);
      OBDal.getInstance().save(proposalAttribute);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(openEnvelopObj);
      ProposalAttJson.put("result", "1");
      ProposalAttJson.put("proposalAttrId", proposalAttribute.getId());

    } catch (OBException e) {
      log.error("Exception while insertProposalAttribute:" + e);
      try {
        ProposalAttJson.put("result", "0");
        ProposalAttJson.put("errorMsg", e.getMessage());
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
      return ProposalAttJson;
    } catch (JSONException e) {
      try {
        ProposalAttJson.put("result", "0");
        ProposalAttJson.put("errorMsg", e.getMessage());
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
      return ProposalAttJson;
    } finally {
      OBContext.restorePreviousMode();
    }
    return ProposalAttJson;
  }

  @Override
  public JSONObject checkorcreateTEE(String strBidId) {
    JSONObject teereusltJson = new JSONObject();
    String commiteeId = "";
    List<EscmProposalMgmt> proMgmtList = null;
    List<Escmopenenvcommitee> oeeList = null;
    List<EscmTechnicalevlEvent> teeList = null;
    Escmopenenvcommitee oeeObj = null;
    EscmTechnicalevlEvent techEval = null;
    SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");

    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
    try {
      OBContext.setAdminMode();
      // check whether proposal is created or not
      EscmBidMgmt bidMgmt = OBDal.getInstance().get(EscmBidMgmt.class, strBidId);
      if (bidMgmt != null) {
        OBQuery<EscmProposalMgmt> proMgmtQuery = OBDal.getInstance()
            .createQuery(EscmProposalMgmt.class, "as e where e.escmBidmgmt.id= :bidId ");
        proMgmtQuery.setNamedParameter("bidId", strBidId);
        proMgmtList = proMgmtQuery.list();
        if (proMgmtList.size() > 0) {
          if (bidMgmt.getBidtype().equals("TR") || bidMgmt.getBidtype().equals("LD")) {
            // Tender/Limited case
            // check whether OEE is created or not, if not throw error
            OBQuery<Escmopenenvcommitee> oeeQuery = OBDal.getInstance().createQuery(
                Escmopenenvcommitee.class, "as e where e.bidNo.id= :bidId and e.alertStatus='CO' ");
            oeeQuery.setNamedParameter("bidId", strBidId);
            oeeList = oeeQuery.list();
            if (oeeList.size() == 0) {
              teereusltJson.put("result", "0");
              teereusltJson.put("errorMsg",
                  OBMessageUtils.messageBD("ESCM_ABidStep_OEENotCreated"));
              return teereusltJson;
            } else {
              oeeObj = oeeList.get(0);
              // check whether TEE is created or not
              OBQuery<EscmTechnicalevlEvent> teeQuery = OBDal.getInstance()
                  .createQuery(EscmTechnicalevlEvent.class, " as e where e.bidNo.id=:bidId ");
              teeQuery.setNamedParameter("bidId", strBidId);
              teeList = teeQuery.list();
              if (teeList.size() > 0) {
                techEval = teeList.get(0);
                teereusltJson.put("result", "1");
                teereusltJson.put("teeId", techEval.getId());
              } else {
                techEval = OBProvider.getInstance().get(EscmTechnicalevlEvent.class);
                techEval.setOrganization(bidMgmt.getOrganization());
                techEval.setDateH(new java.util.Date());
                techEval.setDateGreg(dateFormat.format(new java.util.Date()));
                techEval.setTime(timeFormat.format(new java.util.Date()));
                techEval.setBidNo(bidMgmt);
                techEval.setBidName(bidMgmt.getBidname());
                techEval.setApprovedBudgetSAR(bidMgmt.getApprovedbudget().longValue());
                techEval.setPreparerIDName(OBContext.getOBContext().getUser());
                commiteeId = getCommiteeIdName(bidMgmt.getClient().getId());
                techEval
                    .setCommitteeIDName(OBDal.getInstance().get(ESCMCommittee.class, commiteeId));
                techEval.setOpenEnvelopID(oeeObj);
                techEval.setOpenEnvelopeDateH(oeeObj.getTodaydate());
                techEval.setProposalsCount(oeeObj.getProposalcount());
                techEval.setRole(OBContext.getOBContext().getRole());
                OBDal.getInstance().save(techEval);
                OBDal.getInstance().flush();
                teereusltJson.put("result", "1");
                teereusltJson.put("teeId", techEval.getId());
              }
            }
          }
        } else {
          teereusltJson.put("result", "0");
          teereusltJson.put("errorMsg", OBMessageUtils.messageBD("ESCM_ABidStep_ProNotCreated"));
          return teereusltJson;
        }
      }
    } catch (OBException e) {
      log.error("Exception in checkorcreateTEE:" + e);
      try {
        teereusltJson.put("result", "0");
        teereusltJson.put("errorMsg", e.getMessage());
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
      return teereusltJson;

    } catch (JSONException e) {
      try {
        teereusltJson.put("result", "0");
        teereusltJson.put("errorMsg", e.getMessage());
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
      return teereusltJson;
    } finally {
      OBContext.restorePreviousMode();
    }
    return teereusltJson;
  }

  public JSONObject checkValidationBeforeRemTran(String bidManagementId, String removeTransaction) {
    JSONObject chkValidJson = new JSONObject();
    try {
      OBContext.setAdminMode();
      if (bidManagementId != null) {
        // po validation
        EscmBidMgmt bidMgmtObj = OBDal.getInstance().get(EscmBidMgmt.class, bidManagementId);
        if (bidMgmtObj.getEscmProposalManagementList().size() > 0) {
          for (EscmProposalMgmt proposalMgmt : bidMgmtObj.getEscmProposalManagementList()) {
            if (proposalMgmt.getOrderEMEscmProposalmgmtIDList().size() > 0) {
              chkValidJson.put("result", "0");
              chkValidJson.put("errorMsg", OBMessageUtils.messageBD("ESCM_POCreatedforProp"));
              return chkValidJson;
            }
          }
        }
        if (bidMgmtObj.getESCMProposalEvlEventList().size() > 0) {
          ESCMProposalEvlEvent evalEvent = bidMgmtObj.getESCMProposalEvlEventList().get(0);
          if (evalEvent.getEscmProposalAttrList().size() > 0) {
            for (EscmProposalAttribute propAttrObj : evalEvent.getEscmProposalAttrList()) {
              EscmProposalMgmt proposalMgmt = propAttrObj.getEscmProposalmgmt();
              if (proposalMgmt != null && (proposalMgmt.getProposalstatus().equals("SHO")
                  || proposalMgmt.getProposalstatus().equals("AWD")
                  || proposalMgmt.getProposalstatus().equals("PAWD")
                  || proposalMgmt.getProposalstatus().equals("DIS")
                  || proposalMgmt.getProposalstatus().equals("WD")
                  || proposalMgmt.getProposalstatus().equals("CL"))) {
                chkValidJson.put("result", "0");
                chkValidJson.put("errorMsg", OBMessageUtils.messageBD("ESCM_PropProcessed"));
                return chkValidJson;
              }
            }
          }
        }

        chkValidJson.put("result", "1");
        return chkValidJson;
      }

    } catch (OBException e) {
      log.error("Exception while checkValidationBefore         .RemTran:" + e);
      try {
        chkValidJson.put("result", "0");
        chkValidJson.put("errorMsg", e.getMessage());
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
      return chkValidJson;
    } catch (JSONException e) {
      try {
        chkValidJson.put("result", "0");
        chkValidJson.put("errorMsg", e.getMessage());
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
      return chkValidJson;
    } finally {
      OBContext.restorePreviousMode();
    }
    return chkValidJson;
  }

  public JSONObject removeTransactions(String bidManagementId, String removeTransaction,
      VariablesSecureApp vars) {
    JSONObject removeTransJSON = new JSONObject();
    try {
      OBContext.setAdminMode();
      if (bidManagementId != null) {
        // po validation
        EscmBidMgmt bidMgmtObj = OBDal.getInstance().get(EscmBidMgmt.class, bidManagementId);

        if (removeTransaction.equals("UBID")) {

          if (bidMgmtObj.getBidtype().equals("DR")) {

            // remove Bank Guarantee
            JSONObject removeBankguaranteeJSON = removeBankguarantee(bidMgmtObj, vars);

            if (removeBankguaranteeJSON.has("result")) {
              if (removeBankguaranteeJSON.getString("result").equals("0")) {
                OBDal.getInstance().rollbackAndClose();
                removeTransJSON.put("result", "0");
                removeTransJSON.put("errorMsg", removeBankguaranteeJSON.getString("errorMsg"));
                return removeTransJSON;
              } else {
                // remove PEE
                JSONObject removePEEJSON = removePEE(bidMgmtObj, vars);
                if (removePEEJSON.has("result")) {
                  if (removePEEJSON.getString("result").equals("0")) {
                    OBDal.getInstance().rollbackAndClose();
                    removeTransJSON.put("result", "0");
                    removeTransJSON.put("errorMsg", removePEEJSON.getString("errorMsg"));
                    return removeTransJSON;
                  } else if (removePEEJSON.getString("result").equals("1")
                      || removePEEJSON.getString("result").equals("2")) {

                    // remove Proposals
                    JSONObject removeProposalJson = removeProposals(bidMgmtObj, vars);
                    if (removeProposalJson.has("result")) {
                      if (removeProposalJson.getString("result").equals("0")) {
                        OBDal.getInstance().rollbackAndClose();
                        removeTransJSON.put("result", "0");
                        removeTransJSON.put("errorMsg", removeProposalJson.getString("errorMsg"));
                        return removeTransJSON;
                      } else if (removeProposalJson.getString("result").equals("1")
                          || removeProposalJson.getString("result").equals("2")) {
                        // remove bid
                        JSONObject removeBidJson = removeBid(bidMgmtObj, vars);
                        if (removeBidJson.has("result")) {
                          if (removeBidJson.getString("result").equals("0")) {
                            OBDal.getInstance().rollbackAndClose();
                            removeTransJSON.put("result", "0");
                            removeTransJSON.put("errorMsg", removeBidJson.getString("errorMsg"));
                            return removeTransJSON;
                          } else {
                            removeTransJSON.put("result", removeBidJson.getString("result"));
                            if (removeBidJson.has("successMsg"))
                              removeTransJSON.put("successMsg",
                                  removeBidJson.getString("successMsg"));
                          }
                        }
                      }
                    }
                  }
                }
              }
            }

          } else {
            // remove PEE
            JSONObject removePEEJSON = removePEE(bidMgmtObj, vars);
            if (removePEEJSON.has("result")) {
              if (removePEEJSON.getString("result").equals("0")) {
                OBDal.getInstance().rollbackAndClose();
                removeTransJSON.put("result", "0");
                removeTransJSON.put("errorMsg", removePEEJSON.getString("errorMsg"));
                return removeTransJSON;
              } else if (removePEEJSON.getString("result").equals("1")
                  || removePEEJSON.getString("result").equals("2")) {
                // remove TEE
                JSONObject removeTEEJSON = removeTEE(bidMgmtObj, vars);
                if (removeTEEJSON.has("result")) {
                  if (removeTEEJSON.getString("result").equals("0")) {
                    OBDal.getInstance().rollbackAndClose();
                    removeTransJSON.put("result", "0");
                    removeTransJSON.put("errorMsg", removeTEEJSON.getString("errorMsg"));
                    return removeTransJSON;
                  } else if (removeTEEJSON.getString("result").equals("1")
                      || removeTEEJSON.getString("result").equals("2")) {
                    // remove Bank Guarantee
                    JSONObject removeBankguaranteeJSON = removeBankguarantee(bidMgmtObj, vars);

                    if (removeBankguaranteeJSON.has("result")) {
                      if (removeBankguaranteeJSON.getString("result").equals("0")) {
                        OBDal.getInstance().rollbackAndClose();
                        removeTransJSON.put("result", "0");
                        removeTransJSON.put("errorMsg",
                            removeBankguaranteeJSON.getString("errorMsg"));
                        return removeTransJSON;
                      } else {
                        // remove OEE
                        JSONObject removeOEEJSON = removeOEE(bidMgmtObj, vars);
                        if (removeOEEJSON.has("result")) {
                          if (removeOEEJSON.getString("result").equals("0")) {
                            OBDal.getInstance().rollbackAndClose();
                            removeTransJSON.put("result", "0");
                            removeTransJSON.put("errorMsg", removeOEEJSON.getString("errorMsg"));
                            return removeTransJSON;
                          } else if (removeOEEJSON.getString("result").equals("1")
                              || removeOEEJSON.getString("result").equals("2")) {

                            // remove Proposals
                            JSONObject removeProposalJson = removeProposals(bidMgmtObj, vars);
                            if (removeProposalJson.has("result")) {
                              if (removeProposalJson.getString("result").equals("0")) {
                                OBDal.getInstance().rollbackAndClose();
                                removeTransJSON.put("result", "0");
                                removeTransJSON.put("errorMsg",
                                    removeProposalJson.getString("errorMsg"));
                                return removeTransJSON;
                              } else if (removeProposalJson.getString("result").equals("1")
                                  || removeProposalJson.getString("result").equals("2")) {

                                // remove sales voucher
                                JSONObject removeSalesVoucherJson = removeRFP(bidMgmtObj, vars);
                                if (removeSalesVoucherJson.has("result")) {
                                  if (removeSalesVoucherJson.getString("result").equals("0")) {
                                    OBDal.getInstance().rollbackAndClose();
                                    removeTransJSON.put("result", "0");
                                    removeTransJSON.put("errorMsg",
                                        removeSalesVoucherJson.getString("errorMsg"));
                                    return removeTransJSON;
                                  } else {
                                    // remove bid
                                    JSONObject removeBidJson = removeBid(bidMgmtObj, vars);
                                    if (removeBidJson.has("result")) {
                                      if (removeBidJson.getString("result").equals("0")) {
                                        OBDal.getInstance().rollbackAndClose();
                                        removeTransJSON.put("result", "0");
                                        removeTransJSON.put("errorMsg",
                                            removeBidJson.getString("errorMsg"));
                                        return removeTransJSON;
                                      } else {
                                        removeTransJSON.put("result",
                                            removeBidJson.getString("result"));
                                        if (removeBidJson.has("successMsg"))
                                          removeTransJSON.put("successMsg",
                                              removeBidJson.getString("successMsg"));
                                      }
                                    }
                                  }
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }

        }

        if (removeTransaction.equals("UPRO")) {

          if (bidMgmtObj.getBidtype().equals("DR")) {

            // remove Bank Guarantee
            JSONObject removeBankguaranteeJSON = removeBankguarantee(bidMgmtObj, vars);

            if (removeBankguaranteeJSON.has("result")) {
              if (removeBankguaranteeJSON.getString("result").equals("0")) {
                OBDal.getInstance().rollbackAndClose();
                removeTransJSON.put("result", "0");
                removeTransJSON.put("errorMsg", removeBankguaranteeJSON.getString("errorMsg"));
                return removeTransJSON;
              } else {
                // remove PEE
                JSONObject removePEEJSON = removePEE(bidMgmtObj, vars);
                if (removePEEJSON.has("result")) {
                  if (removePEEJSON.getString("result").equals("0")) {
                    OBDal.getInstance().rollbackAndClose();
                    removeTransJSON.put("result", "0");
                    removeTransJSON.put("errorMsg", removePEEJSON.getString("errorMsg"));
                    return removeTransJSON;
                  } else if (removePEEJSON.getString("result").equals("1")
                      || removePEEJSON.getString("result").equals("2")) {

                    // remove Proposals
                    JSONObject removeProposalJson = removeProposals(bidMgmtObj, vars);
                    if (removeProposalJson.has("result")) {
                      if (removeProposalJson.getString("result").equals("0")) {
                        OBDal.getInstance().rollbackAndClose();
                        removeTransJSON.put("result", "0");
                        removeTransJSON.put("errorMsg", removeProposalJson.getString("errorMsg"));
                        return removeTransJSON;
                      } else {
                        if (removeProposalJson.has("result"))
                          removeTransJSON.put("result", removeProposalJson.getString("result"));
                        if (removeProposalJson.has("successMsg"))
                          removeTransJSON.put("successMsg",
                              removeProposalJson.getString("successMsg"));
                      }
                    }
                  }
                }
              }
            }

          } else {
            // remove PEE
            JSONObject removePEEJSON = removePEE(bidMgmtObj, vars);
            if (removePEEJSON.has("result")) {
              if (removePEEJSON.getString("result").equals("0")) {
                OBDal.getInstance().rollbackAndClose();
                removeTransJSON.put("result", "0");
                removeTransJSON.put("errorMsg", removePEEJSON.getString("errorMsg"));
                return removeTransJSON;
              } else if (removePEEJSON.getString("result").equals("1")
                  || removePEEJSON.getString("result").equals("2")) {
                // remove TEE
                JSONObject removeTEEJSON = removeTEE(bidMgmtObj, vars);
                if (removeTEEJSON.has("result")) {
                  if (removeTEEJSON.getString("result").equals("0")) {
                    OBDal.getInstance().rollbackAndClose();
                    removeTransJSON.put("result", "0");
                    removeTransJSON.put("errorMsg", removeTEEJSON.getString("errorMsg"));
                    return removeTransJSON;
                  } else if (removeTEEJSON.getString("result").equals("1")
                      || removeTEEJSON.getString("result").equals("2")) {
                    // remove Bank Guarantee
                    JSONObject removeBankguaranteeJSON = removeBankguarantee(bidMgmtObj, vars);

                    if (removeBankguaranteeJSON.has("result")) {
                      if (removeBankguaranteeJSON.getString("result").equals("0")) {
                        OBDal.getInstance().rollbackAndClose();
                        removeTransJSON.put("result", "0");
                        removeTransJSON.put("errorMsg",
                            removeBankguaranteeJSON.getString("errorMsg"));
                        return removeTransJSON;
                      } else {
                        // remove OEE
                        JSONObject removeOEEJSON = removeOEE(bidMgmtObj, vars);
                        if (removeOEEJSON.has("result")) {
                          if (removeOEEJSON.getString("result").equals("0")) {
                            OBDal.getInstance().rollbackAndClose();
                            removeTransJSON.put("result", "0");
                            removeTransJSON.put("errorMsg", removeOEEJSON.getString("errorMsg"));
                            return removeTransJSON;
                          } else if (removeOEEJSON.getString("result").equals("1")
                              || removeOEEJSON.getString("result").equals("2")) {

                            // remove Proposals
                            JSONObject removeProposalJson = removeProposals(bidMgmtObj, vars);
                            if (removeProposalJson.has("result")) {
                              if (removeProposalJson.getString("result").equals("0")) {
                                OBDal.getInstance().rollbackAndClose();
                                removeTransJSON.put("result", "0");
                                removeTransJSON.put("errorMsg",
                                    removeProposalJson.getString("errorMsg"));
                                return removeTransJSON;
                              } else {
                                if (removeProposalJson.has("result"))
                                  removeTransJSON.put("result",
                                      removeProposalJson.getString("result"));
                                if (removeProposalJson.has("successMsg"))
                                  removeTransJSON.put("successMsg",
                                      removeProposalJson.getString("successMsg"));
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }

        }

        if (removeTransaction.equals("UOEE")) {
          // remove PEE
          JSONObject removePEEJSON = removePEE(bidMgmtObj, vars);
          if (removePEEJSON.has("result")) {
            if (removePEEJSON.getString("result").equals("0")) {
              OBDal.getInstance().rollbackAndClose();
              removeTransJSON.put("result", "0");
              removeTransJSON.put("errorMsg", removePEEJSON.getString("errorMsg"));
              return removeTransJSON;
            } else if (removePEEJSON.getString("result").equals("1")
                || removePEEJSON.getString("result").equals("2")) {
              // remove TEE
              JSONObject removeTEEJSON = removeTEE(bidMgmtObj, vars);
              if (removeTEEJSON.has("result")) {
                if (removeTEEJSON.getString("result").equals("0")) {
                  OBDal.getInstance().rollbackAndClose();
                  removeTransJSON.put("result", "0");
                  removeTransJSON.put("errorMsg", removeTEEJSON.getString("errorMsg"));
                  return removeTransJSON;
                } else if (removeTEEJSON.getString("result").equals("1")
                    || removeTEEJSON.getString("result").equals("2")) {

                  JSONObject removeBankguaranteeJSON = removeBankguarantee(bidMgmtObj, vars);
                  if (removeBankguaranteeJSON.has("result")) {
                    if (removeBankguaranteeJSON.getString("result").equals("0")) {
                      OBDal.getInstance().rollbackAndClose();
                      removeTransJSON.put("result", "0");
                      removeTransJSON.put("errorMsg",
                          removeBankguaranteeJSON.getString("errorMsg"));
                      return removeTransJSON;
                    } else if (removeBankguaranteeJSON.getString("result").equals("1")
                        || removeBankguaranteeJSON.getString("result").equals("2")) {
                      // remove OEE
                      JSONObject removeOEEJSON = removeOEE(bidMgmtObj, vars);
                      if (removeOEEJSON.has("result")) {
                        if (removeOEEJSON.getString("result").equals("0")) {
                          OBDal.getInstance().rollbackAndClose();
                          removeTransJSON.put("result", "0");
                          removeTransJSON.put("errorMsg", removeOEEJSON.getString("errorMsg"));
                          return removeTransJSON;
                        } else {
                          if (removeOEEJSON.has("result"))
                            removeTransJSON.put("result", removeOEEJSON.getString("result"));
                          if (removeOEEJSON.has("successMsg"))
                            removeTransJSON.put("successMsg",
                                removeOEEJSON.getString("successMsg"));
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }

        if (removeTransaction.equals("UTEE")) {
          // remove PEE
          JSONObject removePEEJSON = removePEE(bidMgmtObj, vars);
          if (removePEEJSON.has("result")) {
            if (removePEEJSON.getString("result").equals("0")) {
              OBDal.getInstance().rollbackAndClose();
              removeTransJSON.put("result", "0");
              removeTransJSON.put("errorMsg", removePEEJSON.getString("errorMsg"));
              return removeTransJSON;
            } else if (removePEEJSON.getString("result").equals("1")
                || removePEEJSON.getString("result").equals("2")) {
              // remove TEE
              JSONObject removeTEEJSON = removeTEE(bidMgmtObj, vars);
              if (removeTEEJSON.has("result")) {
                if (removeTEEJSON.getString("result").equals("0")) {
                  OBDal.getInstance().rollbackAndClose();
                  removeTransJSON.put("result", "0");
                  removeTransJSON.put("errorMsg", removeTEEJSON.getString("errorMsg"));
                  return removeTransJSON;
                } else {
                  if (removeTEEJSON.has("result"))
                    removeTransJSON.put("result", removeTEEJSON.getString("result"));
                  if (removeTEEJSON.has("successMsg"))
                    removeTransJSON.put("successMsg", removeTEEJSON.getString("successMsg"));
                }
              }
            }

          }
        }

        if (removeTransaction.equals("UPEE")) {
          // remove PEE
          JSONObject removePEEJSON = removePEE(bidMgmtObj, vars);
          if (removePEEJSON.has("result")) {
            if (removePEEJSON.getString("result").equals("0")) {
              OBDal.getInstance().rollbackAndClose();
              removeTransJSON.put("result", "0");
              removeTransJSON.put("errorMsg", removePEEJSON.getString("errorMsg"));
              return removeTransJSON;
            } else {
              if (removePEEJSON.has("result"))
                removeTransJSON.put("result", removePEEJSON.getString("result"));
              if (removePEEJSON.has("successMsg"))
                removeTransJSON.put("successMsg", removePEEJSON.getString("successMsg"));
              return removeTransJSON;
            }
          }

        }
      }
      return removeTransJSON;
    } catch (

    OBException e) {
      log.error("Exception while removeTransactions:" + e);
      try {
        removeTransJSON.put("result", "0");
        removeTransJSON.put("errorMsg", e.getMessage());
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
      return removeTransJSON;
    } catch (JSONException e) {
      try {
        removeTransJSON.put("result", "0");
        removeTransJSON.put("errorMsg", e.getMessage());
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
      return removeTransJSON;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public JSONObject removePEE(EscmBidMgmt bidMgmtObj, VariablesSecureApp vars) {
    JSONObject removePEEJSON = new JSONObject();
    try {
      OBContext.setAdminMode();
      if (bidMgmtObj != null) {
        if (bidMgmtObj.getESCMProposalEvlEventList().size() > 0) {
          ESCMProposalEvlEvent proEvalEventObj = bidMgmtObj.getESCMProposalEvlEventList().get(0);
          if (proEvalEventObj.getStatus().equals("CO")) {
            ProcessBundle pb = new ProcessBundle("C839E66D63B34D67A65FE14A8BA24850", vars);
            HashMap<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("Escm_Proposalevl_Event_ID", proEvalEventObj.getId());
            pb.setParams(parameters);
            OBError myMessage = null;
            new ProposalEvaluationEventProcess().doExecute(pb);
            myMessage = (OBError) pb.getResult();
            if (myMessage.getType().equals("Error")) {
              OBDal.getInstance().rollbackAndClose();
              removePEEJSON.put("result", "0");
              removePEEJSON.put("errorMsg", myMessage.getMessage());
            } else {
              removePEEJSON.put("result", "1");
            }
          } else {
            removePEEJSON.put("result", "1");
          }
          if (removePEEJSON.has("result") && removePEEJSON.getString("result").equals("1")) {
            // remove proposal attribute Obj
            List<EscmProposalAttribute> attrList = proEvalEventObj.getEscmProposalAttrList();
            List<EscmProposalAttribute> attrDelList = new ArrayList<EscmProposalAttribute>();
            if (attrList.size() > 0) {
              for (EscmProposalAttribute attributeObj : attrList) {
                if (bidMgmtObj.getBidtype().equals("DR")) {
                  attributeObj.getEscmProposalmgmt().setProposalstatus("SUB");
                  attrDelList.add(attributeObj);
                  OBDal.getInstance().remove(attributeObj);

                } else {
                  attributeObj.getEscmProposalmgmt().setProposalstatus("TER");
                  attributeObj.setEscmProposalevlEvent(null);
                  OBDal.getInstance().save(attributeObj);
                }
              }
            }

            if (attrDelList.size() > 0) {
              proEvalEventObj.getEscmProposalAttrList().removeAll(attrDelList);
            }

            proEvalEventObj.setBidNo(null);
            proEvalEventObj.setBidName(null);
            proEvalEventObj.setApprovedBudgetSAR(null);
            proEvalEventObj.setEscmAnnoucements(null);
            proEvalEventObj.setEscmOpenenvcommitee(null);
            proEvalEventObj.setEscmCommittee(null);
            proEvalEventObj.setEnvelopeDate(null);
            proEvalEventObj.setEscmTechnicalevlEvent(null);
            proEvalEventObj.setTECDateHijri(null);
            proEvalEventObj.setProposalCounts(null);
            proEvalEventObj.setDeletelines(false);
            proEvalEventObj.setProposalCounts((long) 0);
            // spec no as null
            // proEvalEventObj.setSpecNo(null);
            OBDal.getInstance().save(proEvalEventObj);
            OBDal.getInstance().flush();
            OBDal.getInstance().refresh(proEvalEventObj);

          }
          return removePEEJSON;
        } else {
          removePEEJSON.put("result", "2");
          removePEEJSON.put("successMsg", OBMessageUtils.messageBD("ESCM_AlreadyPEE_Deleted"));
        }
      }

      return removePEEJSON;
    } catch (

    OBException e) {
      log.error("Exception while removePEE:" + e);
      try {
        removePEEJSON.put("result", "0");
        removePEEJSON.put("errorMsg", e.getMessage());
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
      return removePEEJSON;
    } catch (Exception e) {
      log.error("Exception while removePEE:" + e);
      try {
        removePEEJSON.put("result", "0");
        removePEEJSON.put("errorMsg", e.getMessage());
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
      return removePEEJSON;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public JSONObject removeTEE(EscmBidMgmt bidMgmtObj, VariablesSecureApp vars) {
    JSONObject removeTEEJSON = new JSONObject();
    List<ESCM_Proposal_CommentAttr> proCommAttList = new ArrayList<ESCM_Proposal_CommentAttr>();
    List<EscmProposalmgmtLine> proposallineList = new ArrayList<EscmProposalmgmtLine>();
    List<EscmProposalAttribute> attrToDeleteList = new ArrayList<EscmProposalAttribute>();
    boolean isdeleteFlag = false;
    try {
      OBContext.setAdminMode();
      if (bidMgmtObj != null) {
        if (bidMgmtObj.getEscmTechnicalevlEventList().size() > 0) {
          EscmTechnicalevlEvent techEvalEventObj = bidMgmtObj.getEscmTechnicalevlEventList().get(0);
          if (techEvalEventObj.getStatus().equals("CO")) {
            ProcessBundle pb = new ProcessBundle("D1D99C7B68E14C58B5218FA2F18F1880", vars);
            HashMap<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("Escm_Technicalevl_Event_ID", techEvalEventObj.getId());
            parameters.put("comments", "");
            pb.setParams(parameters);
            OBError myMessage = null;
            new TechnicalEvaluationEventProcess().doExecute(pb);
            myMessage = (OBError) pb.getResult();
            if (myMessage.getType().equals("Error")) {
              OBDal.getInstance().rollbackAndClose();
              removeTEEJSON.put("result", "0");
              removeTEEJSON.put("errorMsg", myMessage.getMessage());
            } else {
              removeTEEJSON.put("result", "1");
            }
          } else {
            removeTEEJSON.put("result", "1");
          }
          if (removeTEEJSON.has("result") && removeTEEJSON.getString("result").equals("1")) {
            // remove proposal attribute Obj
            if (techEvalEventObj.getEscmProposalAttrList().size() > 0) {
              isdeleteFlag = true;
              for (EscmProposalAttribute proatt : techEvalEventObj.getEscmProposalAttrList()) {
                proatt.setEscmTechnicalevlEvent(null);
                proatt.setTechevalDecision(null);
                proatt.setTechVariation(BigDecimal.ZERO);
                proatt.setTechnicalDiscount(BigDecimal.ZERO);
                OBDal.getInstance().save(proatt);
                attrToDeleteList.add(proatt);

                // update the proposal line values before delete the TEE
                OBQuery<EscmProposalmgmtLine> proposallineQry = OBDal.getInstance().createQuery(
                    EscmProposalmgmtLine.class, " as e where e.escmProposalmgmt.id=:proposalID ");
                proposallineQry.setNamedParameter("proposalID",
                    proatt.getEscmProposalmgmt().getId());
                proposallineList = proposallineQry.list();
                if (proposallineList.size() > 0) {
                  for (EscmProposalmgmtLine proposalline : proposallineList) {
                    proposalline.setTechDiscount(BigDecimal.ZERO);
                    proposalline.setTechDiscountamt(BigDecimal.ZERO);
                    proposalline.setTechLineTotal(BigDecimal.ZERO);
                    proposalline.setTechLineQty(BigDecimal.ZERO);
                    proposalline.setTechUnitPrice(BigDecimal.ZERO);
                    proposalline.setPEELineTotal(BigDecimal.ZERO);
                    proposalline.setPEEQty(BigDecimal.ZERO);
                    proposalline.setPEENegotUnitPrice(BigDecimal.ZERO);
                    proposalline.setTEELineTaxamt(BigDecimal.ZERO);
                    proposalline.setPEELineTaxamt(BigDecimal.ZERO);
                    OBDal.getInstance().save(proposalline);
                  }
                }

                // update the proposal comments attribute values before delete the TEE
                OBQuery<ESCM_Proposal_CommentAttr> proposalcommattQry = OBDal.getInstance()
                    .createQuery(ESCM_Proposal_CommentAttr.class,
                        " as e where e.istechevent='Y' and  e.escmProposalAttr.id=:proattID");
                proposalcommattQry.setNamedParameter("proattID", proatt.getId());
                proCommAttList = proposalcommattQry.list();
                if (proCommAttList.size() > 0) {
                  for (ESCM_Proposal_CommentAttr procomatt : proCommAttList) {
                    OBDal.getInstance().remove(procomatt);
                  }
                }

              }
              // techEvalEventObj.getEscmProposalAttrList().removeAll(attrToDeleteList);
              OBDal.getInstance().flush();
              OBDal.getInstance().refresh(techEvalEventObj);
            }
            if (isdeleteFlag)
              OBDal.getInstance().remove(techEvalEventObj);
            OBDal.getInstance().flush();
          }
          return removeTEEJSON;
        } else {
          removeTEEJSON.put("result", "2");
          removeTEEJSON.put("successMsg", OBMessageUtils.messageBD("ESCM_AlreadyTEE_Deleted"));
        }
      }
      return removeTEEJSON;
    } catch (OBException e) {
      log.error("Exception while removeTEE:" + e);
      try {
        removeTEEJSON.put("result", "0");
        removeTEEJSON.put("errorMsg", e.getMessage());
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
      return removeTEEJSON;
    } catch (Exception e) {
      log.error("Exception while removeTEE:" + e);
      try {
        removeTEEJSON.put("result", "0");
        removeTEEJSON.put("errorMsg", e.getMessage());
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
      return removeTEEJSON;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public JSONObject removeOEE(EscmBidMgmt bidMgmtObj, VariablesSecureApp vars) {
    JSONObject removeOEEJSON = new JSONObject();
    try {
      OBContext.setAdminMode();
      if (bidMgmtObj != null) {
        if (bidMgmtObj.getEscmOpenenvcommiteeList().size() > 0) {
          Escmopenenvcommitee openEnvlop = bidMgmtObj.getEscmOpenenvcommiteeList().get(0);
          if (openEnvlop.getAlertStatus().equals("CO")) {
            ProcessBundle pb = new ProcessBundle("4753925FCB4A4831BB3EFC9CCDA75FDE", vars);
            HashMap<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("Escm_Openenvcommitee_ID", openEnvlop.getId());
            pb.setParams(parameters);
            OBError myMessage = null;
            new OpenEnvlopCommiteeAction().doExecute(pb);
            myMessage = (OBError) pb.getResult();
            if (myMessage.getType().equals("Error")) {
              OBDal.getInstance().rollbackAndClose();
              removeOEEJSON.put("result", "0");
              removeOEEJSON.put("errorMsg", myMessage.getMessage());
            } else {
              removeOEEJSON.put("result", "1");
            }
          } else {
            removeOEEJSON.put("result", "1");
          }
          if (removeOEEJSON.has("result") && removeOEEJSON.getString("result").equals("1")) {
            // remove proposal attribute Obj
            List<EscmProposalAttribute> attrlist = openEnvlop.getEscmProposalAttrList();
            // List<EscmProposalAttribute> attrDelList = new ArrayList<EscmProposalAttribute>();
            if (attrlist.size() > 0) {
              for (EscmProposalAttribute attributeObj : attrlist) {
                // attrDelList.add(attributeObj);
                OBDal.getInstance().remove(attributeObj);
              }
              // openEnvlop.getEscmProposalAttrList().removeAll(attrDelList);
              // OBDal.getInstance().flush();
              // --- OBDal.getInstance().refresh(openEnvlop);
            }
            OBDal.getInstance().remove(openEnvlop);
          }

          return removeOEEJSON;
        } else {
          removeOEEJSON.put("result", "2");
          removeOEEJSON.put("successMsg", OBMessageUtils.messageBD("ESCM_AlreadyOEE_Deleted"));
        }
      }
      return removeOEEJSON;
    } catch (

    OBException e) {
      log.error("Exception while removeOEE:" + e);
      try {
        removeOEEJSON.put("result", "0");
        removeOEEJSON.put("errorMsg", e.getMessage());
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
      return removeOEEJSON;
    } catch (Exception e) {
      log.error("Exception while removeOEE1QQQ1:" + e);
      try {
        removeOEEJSON.put("result", "0");
        removeOEEJSON.put("errorMsg", e.getMessage());
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
      return removeOEEJSON;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public JSONObject removeBankguarantee(EscmBidMgmt bidMgmtObj, VariablesSecureApp vars) {
    JSONObject removeBankguaranteeJSON = new JSONObject();
    List<ESCMBGWorkbench> bgList = new ArrayList<ESCMBGWorkbench>();
    boolean isBGLinesdeleted = false;
    try {
      OBContext.setAdminMode();
      if (bidMgmtObj != null) {
        removeBankguaranteeJSON.put("result", "0");

        OBQuery<ESCMBGWorkbench> bgWorkBenchQry = OBDal.getInstance()
            .createQuery(ESCMBGWorkbench.class, " as e where (( e.documentNo in "
                + " ( select e.id from Escm_Proposal_Management e where e.escmBidmgmt.id=:bidID)) or(e.bidNo.id=:bidID)) and e.documentType='P' ");
        bgWorkBenchQry.setNamedParameter("bidID", bidMgmtObj.getId());
        bgList = bgWorkBenchQry.list();
        if (bgList.size() > 0) {
          for (ESCMBGWorkbench bgWorkbenchObj : bgList) {

            if (bgWorkbenchObj.getEscmBankguaranteeDetailList().size() > 0) {
              for (Escmbankguaranteedetail bankGuaranteeObj : bgWorkbenchObj
                  .getEscmBankguaranteeDetailList()) {
                if (!bankGuaranteeObj.getBgstatus().equals("ACT")
                    && !bankGuaranteeObj.getBgstatus().equals("DR")) {
                  ProcessBundle pb = new ProcessBundle("423DFE9BDAC14FF7A15109DC5EDCB57C", vars);
                  HashMap<String, Object> parameters = new HashMap<String, Object>();
                  parameters.put("Escm_Bgworkbench_ID", bgWorkbenchObj.getId());
                  pb.setParams(parameters);
                  OBError myMessage = null;
                  new BGWorkbenchProcess().doExecute(pb);
                  myMessage = (OBError) pb.getResult();
                  if (myMessage.getType().equals("Error")) {
                    OBDal.getInstance().rollbackAndClose();
                    removeBankguaranteeJSON.put("result", "0");
                    removeBankguaranteeJSON.put("errorMsg", myMessage.getMessage());
                  } else {
                    removeBankguaranteeJSON.put("result", "1");
                  }
                } else {
                  removeBankguaranteeJSON.put("result", "1");
                }
                if (removeBankguaranteeJSON.has("result")
                    && removeBankguaranteeJSON.getString("result").equals("1")) {
                  // if (bankGuaranteeObj.getESCMBGExtensionList().size() > 0) {
                  // for (ESCMBGExtension bgExtensionObj :
                  // bankGuaranteeObj.getESCMBGExtensionList()) {
                  // OBDal.getInstance().remove(bgExtensionObj);
                  // }
                  // }
                  // if (bankGuaranteeObj.getESCMBGReleaseList().size() > 0) {
                  // for (ESCMBGRelease bgReleaseObj : bankGuaranteeObj.getESCMBGReleaseList()) {
                  // OBDal.getInstance().remove(bgReleaseObj);
                  // }
                  // }
                  // if (bankGuaranteeObj.getESCMBGAmtRevisionList().size() > 0) {
                  // for (ESCMBGAmtRevision bgAmtRevisionObj : bankGuaranteeObj
                  // .getESCMBGAmtRevisionList()) {
                  // OBDal.getInstance().remove(bgAmtRevisionObj);
                  // }
                  // }
                  // if (bankGuaranteeObj.getESCMBGConfiscationList().size() > 0) {
                  // for (ESCMBGConfiscation bgConfiscationObj : bankGuaranteeObj
                  // .getESCMBGConfiscationList()) {
                  // OBDal.getInstance().remove(bgConfiscationObj);
                  // }
                  // }
                  isBGLinesdeleted = true;
                  bgWorkbenchObj.setBghdstatus("DR");
                  OBDal.getInstance().save(bgWorkbenchObj);
                  OBDal.getInstance().flush();
                  OBDal.getInstance().remove(bankGuaranteeObj);
                }
              }
            }
            // if (isBGLinesdeleted)
            OBDal.getInstance().remove(bgWorkbenchObj);
            OBDal.getInstance().flush();
            removeBankguaranteeJSON.put("result", "1");
          }

        } else {
          removeBankguaranteeJSON.put("result", "2");
        }
      }
      return removeBankguaranteeJSON;
    } catch (OBException e) {
      log.error("Exception while removePEE:" + e);
      try {
        removeBankguaranteeJSON.put("result", "0");
        removeBankguaranteeJSON.put("errorMsg", e.getMessage());
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
      return removeBankguaranteeJSON;
    } catch (Exception e) {
      log.error("Exception while removeTEE:" + e);
      try {
        removeBankguaranteeJSON.put("result", "0");
        removeBankguaranteeJSON.put("errorMsg", e.getMessage());
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
      return removeBankguaranteeJSON;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public JSONObject removeProposals(EscmBidMgmt bidMgmtObj, VariablesSecureApp vars) {
    JSONObject removeProposalJson = new JSONObject();
    List<EscmProposalMgmt> proposalList = new ArrayList<EscmProposalMgmt>();
    try {
      OBContext.setAdminMode();
      if (bidMgmtObj != null) {

        proposalList = bidMgmtObj.getEscmProposalManagementList();
        OBDal.getInstance().flush();
        bidMgmtObj.setEscmProposalManagementList(null);
        OBDal.getInstance().refresh(bidMgmtObj);
        if (proposalList.size() > 0) {
          for (EscmProposalMgmt proposal : proposalList) {
            List<EscmProposalmgmtLine> proposalLine = proposal.getEscmProposalmgmtLineList();
            for (EscmProposalmgmtLine lines : proposalLine) {
              lines.setParentLineNo(null);
              lines.setManual(true);
              OBDal.getInstance().save(lines);

              if (lines.getEscmProposalmgmtLnVerList().size() > 0) {
                for (EscmProposalmgmtLineVersion verlnObj : proposal
                    .getEscmProposalmgmtLnVerList()) {
                  OBDal.getInstance().remove(verlnObj);
                }
              }
            }

            OBDal.getInstance().flush();
            List<EscmProposalRegulation> regulationLine = proposal.getEscmProposalRegulationList();
            if (regulationLine.size() > 0) {
              for (EscmProposalRegulation reglineObj : regulationLine) {
                OBDal.getInstance().remove(reglineObj);
              }
              proposal.getEscmProposalRegulationList().removeAll(regulationLine);
              OBDal.getInstance().flush();
            }

            OBDal.getInstance().refresh(proposal);
            proposal.setProposalstatus("DR");
            OBDal.getInstance().remove(proposal);
            OBDal.getInstance().flush();
          }
          removeProposalJson.put("result", "1");
          OBDal.getInstance().refresh(bidMgmtObj);
        } else {
          removeProposalJson.put("result", "2");
          removeProposalJson.put("successMsg", OBMessageUtils.messageBD("ESCM_AlreadyPRO_Deleted"));
        }
      }

    } catch (

    OBException e) {
      log.error("Exception while removeProposals:" + e);
      try {
        removeProposalJson.put("result", "0");
        removeProposalJson.put("errorMsg", e.getMessage());
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
      return removeProposalJson;
    } catch (Exception e) {
      log.error("Exception while removeProposals:" + e);
      try {
        removeProposalJson.put("result", "0");
        removeProposalJson.put("errorMsg", e.getMessage());
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
      return removeProposalJson;
    } finally {
      OBContext.restorePreviousMode();
    }
    return removeProposalJson;
  }

  public JSONObject removeBid(EscmBidMgmt bidMgmtObj, VariablesSecureApp vars) {
    JSONObject removeBidJson = new JSONObject();
    List<Escmbiddates> bidDateList = bidMgmtObj.getEscmBiddatesList();
    List<Escmbidmgmtline> bidMgmtLineList = bidMgmtObj.getEscmBidmgmtLineList();
    try {
      OBContext.setAdminMode();
      if (bidMgmtObj != null) {

        if (bidDateList.size() > 0) {
          for (Escmbiddates dates : bidDateList) {
            dates.setApproved(false);
            OBDal.getInstance().save(dates);
          }
        }

        if (bidMgmtLineList.size() > 0) {
          for (Escmbidmgmtline line : bidMgmtLineList) {
            line.setParentline(null);
            line.setManual(true);
            OBDal.getInstance().save(line);
          }
        }
        OBDal.getInstance().flush();
        removeBidJson.put("result", "1");
        OBDal.getInstance().remove(bidMgmtObj);
        removeBidJson.put("successMsg", OBMessageUtils.messageBD("ESCM_BidDeletedSuccess"));
        return removeBidJson;
      }

    } catch (

    OBException e) {
      log.error("Exception while removeBid:" + e);
      try {
        removeBidJson.put("result", "0");
        removeBidJson.put("errorMsg", e.getMessage());
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
      return removeBidJson;
    } catch (Exception e) {
      log.error("Exception while removeBid:" + e);
      try {
        removeBidJson.put("result", "0");
        removeBidJson.put("errorMsg", e.getMessage());
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
      return removeBidJson;
    } finally {
      OBContext.restorePreviousMode();
    }
    return removeBidJson;
  }

  public JSONObject removeRFP(EscmBidMgmt bidMgmtObj, VariablesSecureApp vars) {
    JSONObject removeRFPJSON = new JSONObject();

    try {
      OBContext.setAdminMode();
      List<Escmsalesvoucher> salesVoucherList = bidMgmtObj.getEscmSalesvoucherList();
      if (salesVoucherList.size() > 0) {
        for (Escmsalesvoucher rfpObj : salesVoucherList) {
          OBDal.getInstance().remove(rfpObj);
        }
        removeRFPJSON.put("result", "1");
        return removeRFPJSON;
      } else {
        removeRFPJSON.put("result", "1");
        return removeRFPJSON;
      }
    } catch (

    OBException e) {
      log.error("Exception while removeRFP:" + e);
      try {
        removeRFPJSON.put("result", "0");
        removeRFPJSON.put("errorMsg", e.getMessage());
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
      return removeRFPJSON;
    } catch (Exception e) {
      log.error("Exception while removeRFP:" + e);
      try {
        removeRFPJSON.put("result", "0");
        removeRFPJSON.put("errorMsg", e.getMessage());
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
      return removeRFPJSON;
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
