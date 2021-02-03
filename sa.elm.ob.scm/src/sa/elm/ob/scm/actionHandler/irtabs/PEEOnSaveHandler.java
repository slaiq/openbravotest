package sa.elm.ob.scm.actionHandler.irtabs;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.jfree.util.Log;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.scm.EscmBidMgmt;
import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.Escmopenenvcommitee;

/**
 * 
 * @author poongodi on 06/08/2020
 *
 */

public class PEEOnSaveHandler extends BaseActionHandler {
  Logger log4j = Logger.getLogger(PEEOnSaveHandler.class);
  JSONObject json;

  protected JSONObject execute(Map<String, Object> parameters, String data) {
    try {
      /* Get the data as json */
      final JSONObject jsonData = new JSONObject(data);

      json = new JSONObject();

      final String recordId = jsonData.getString("recordId");
      EscmBidMgmt bidmgmt = OBDal.getInstance().get(EscmBidMgmt.class, recordId);
      if (!bidmgmt.getBidtype().equals("DR")) {
        String message = integProsalAtttoProsalEvent(bidmgmt);
        if (message != null) {
          json.put("showAction", "true");
          json.put("showproposal", message);
        }

      }

      log4j.debug("json:" + json);
    } catch (Exception e) {
      throw new OBException(e);
    }
    return json;
  }

  public static String integProsalAtttoProsalEvent(EscmBidMgmt bidHeader) {
    String strQuery = null;
    Query query = null;
    Integer count = 0;
    String proposalNo = "";
    String message = null;

    try {

      if (!bidHeader.getBidtype().equals("DR")) {
        OBQuery<EscmProposalMgmt> proposal = OBDal.getInstance().createQuery(EscmProposalMgmt.class,
            " as e where e.escmBidmgmt.id=:bidId and e.proposalstatus='TER'");
        proposal.setNamedParameter("bidId", bidHeader.getId());
        if (proposal.list().size() > 0) {
          for (EscmProposalMgmt pro : proposal.list()) {
            count = 0;

            // take the count of bg to conside that proposal while
            strQuery = " select case when bgdetcount= actcount then 1 else 0 end  from escm_bgworkbench bg "
                + " join ( select count(escm_bankguarantee_detail_id)  as bgdetcount,escm_bgworkbench_id  from escm_bankguarantee_detail group by escm_bgworkbench_id ) "
                + " bgdet on bgdet.escm_bgworkbench_id= bg.escm_bgworkbench_id "
                + " join ( select count(escm_bankguarantee_detail_id) as actcount,escm_bgworkbench_id  from escm_bankguarantee_detail where  bgstatus not in ('REL','CON','EXP') "
                + "  group by  escm_bgworkbench_id )  actdet on actdet.escm_bgworkbench_id= bg.escm_bgworkbench_id "
                + " where bg.document_no  ='" + pro.getId() + "'"
                + " and bg.document_type='P' and bg.bghdstatus='CO' ";
            query = OBDal.getInstance().getSession().createSQLQuery(strQuery);

            if (query != null && query.list().size() > 0) {
              count = (Integer) query.list().get(0);
            }
            if (count == 0) {
              if (proposalNo != "") {
                message = OBMessageUtils.messageBD("Escm_BG_NotIncluded").replace("@",
                    proposalNo.concat(",").concat(pro.getProposalno()));
                proposalNo = proposalNo.concat(",").concat(pro.getProposalno());
              } else {
                message = OBMessageUtils.messageBD("Escm_BG_NotIncluded").replace("@",
                    pro.getProposalno());
                proposalNo = pro.getProposalno();
              }

            }

          }
        }

      }
      return message;
    } catch (OBException e) {
      Log.debug("Exception while integProsalAtttoProsalEvent:", e);
      throw new OBException(e.getMessage());
    }

  }

  public static String integProsalAtttoProsalEventWithCustomErrorMsg(Escmopenenvcommitee oee) {
    String strQuery = null;
    Query query = null;
    Integer count = 0;
    String message = null;
    try {
      if (!oee.getBidNo().getBidtype().equals("DR")) {
        List<EscmProposalAttribute> proposalAttr = oee.getEscmProposalAttrList().stream()
            .filter(a -> a.getEscmProposalmgmt() != null).collect(Collectors.toList());

        for (EscmProposalAttribute proposalAtt : proposalAttr) {
          count = 0;
          EscmProposalMgmt pro = proposalAtt.getEscmProposalmgmt();

          // take the count of bg to conside that proposal while
          strQuery = " select case when bgdetcount= actcount then 1 else 0 end  from escm_bgworkbench bg "
              + " join ( select count(escm_bankguarantee_detail_id)  as bgdetcount,escm_bgworkbench_id  from escm_bankguarantee_detail group by escm_bgworkbench_id ) "
              + " bgdet on bgdet.escm_bgworkbench_id= bg.escm_bgworkbench_id "
              + " join ( select count(escm_bankguarantee_detail_id) as actcount,escm_bgworkbench_id  from escm_bankguarantee_detail where  bgstatus not in ('REL','CON','EXP') "
              + "  group by  escm_bgworkbench_id )  actdet on actdet.escm_bgworkbench_id= bg.escm_bgworkbench_id "
              + " where bg.document_no  ='" + pro.getId() + "'" + " and bg.document_type='P'";
          query = OBDal.getInstance().getSession().createSQLQuery(strQuery);

          if (query != null && query.list().size() > 0) {
            count = (Integer) query.list().get(0);
          }
          if (count == 0) {
            if (message != null) {
              message = message
                  .concat(sa.elm.ob.utility.util.Constants.LIST_OPENTAG.concat(pro.getProposalno())
                      .concat(sa.elm.ob.utility.util.Constants.LIST_CLOSETAG));
            } else {
              message = sa.elm.ob.utility.util.Constants.ORDEREDLIST_OPENTAG;
              message = message
                  .concat(sa.elm.ob.utility.util.Constants.LIST_OPENTAG.concat(pro.getProposalno())
                      .concat(sa.elm.ob.utility.util.Constants.LIST_CLOSETAG));
            }
          }
        }

        if (message != null) {
          message = message.concat(sa.elm.ob.utility.util.Constants.ORDEREDLIST_CLOSETAG);
        }
      }
      return message;
    } catch (OBException e) {
      Log.debug("Exception while integProsalAtttoProsalEvent:", e);
      throw new OBException(e.getMessage());
    }

  }
}