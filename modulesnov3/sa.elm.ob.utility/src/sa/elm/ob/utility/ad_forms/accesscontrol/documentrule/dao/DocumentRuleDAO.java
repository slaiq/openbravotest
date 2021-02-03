package sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.ad.access.Role;

import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.vo.DocumentRuleVO;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.UtilityDAO;

public class DocumentRuleDAO {

  private Connection conn = null;
  VariablesSecureApp vars = null;
  private static Logger log4j = Logger.getLogger(DocumentRuleDAO.class);

  public DocumentRuleDAO(Connection con, VariablesSecureApp vars) {
    this.conn = con;
    this.vars = vars;
  }

  /**
   * This method is used to get Role List
   * 
   * @param vars
   * @return list
   */
  public static List<DocumentRuleVO> getRoleList(VariablesSecureApp vars) {
    List<DocumentRuleVO> list = new ArrayList<DocumentRuleVO>();
    try {
      OBCriteria<Role> roleCri = OBDal.getInstance().createCriteria(Role.class);
      roleCri.add(Restrictions.eq(Role.PROPERTY_CLIENT + ".id", vars.getClient()));
      roleCri.add(Restrictions.not(Restrictions.eq(Role.PROPERTY_CREATEDBY + ".id", "0")));
      roleCri.add(Restrictions.eq(Role.PROPERTY_EUTINCLUDEINRULE, true));
      roleCri.addOrderBy(Role.PROPERTY_NAME, true);
      if (roleCri.list().size() > 0) {
        for (Role role : roleCri.list()) {
          DocumentRuleVO ruleVO = new DocumentRuleVO();
          ruleVO.setId(role.getId());
          ruleVO.setName(role.getName());
          if (role.getEfinOrgbcumanger() != null && (role.getEfinOrgbcumanger().equals("ORGM")
              || role.getEfinOrgbcumanger().equals("ORGBM"))) {
            ruleVO.setIsDummyRole(true);
          } else if (role.isEscmIshrlinemanager() != null && role.isEscmIshrlinemanager()) {
            ruleVO.setIsDummyRole(role.isEscmIshrlinemanager());
          } else if (role.isEscmIsspecializeddept() != null && role.isEscmIsspecializeddept()) {
            ruleVO.setIsDummyRole(role.isEscmIsspecializeddept());
          } else {
            ruleVO.setIsDummyRole(false);
          }
          list.add(ruleVO);
        }
      }
    } catch (final Exception e) {
      log4j.error("Exception in getRoleList() :", e);
      return list;
    }
    return list;
  }

  /**
   * This method is used to get Organization List
   * 
   * @return List
   */
  public List<DocumentRuleVO> getOrganisationList() {
    PreparedStatement st = null;
    ResultSet rs = null;
    List<DocumentRuleVO> list = new ArrayList<DocumentRuleVO>();
    try {
      st = conn.prepareStatement(
          "select acc.ad_org_id,org.name from ad_role_orgaccess  acc left join ad_org org  on  "
              + "org.ad_org_id=acc.ad_org_id left join ad_orgtype ot on org.ad_orgtype_id=ot.ad_orgtype_id "
              + " where acc.ad_client_id = ? and ad_role_id  =? and ot.istransactionsallowed='Y' and org.isready='Y' and org.em_ehcm_orgtyp in ( select ehcm_org_type_id from ehcm_org_type where value='ORG') or org.ad_org_id='0' "
              + " group by acc.ad_org_id,org.name order by org.name;");
      st.setString(1, vars.getClient());
      st.setString(2, vars.getRole());
      rs = st.executeQuery();
      while (rs.next()) {
        DocumentRuleVO documentRuleVO = new DocumentRuleVO();
        documentRuleVO.setId(rs.getString("ad_org_id"));
        documentRuleVO.setName(rs.getString("name"));
        list.add(documentRuleVO);
      }

    } catch (final Exception e) {
      log4j.error("Exception in getOrganisationList() :", e);
      return list;
    } finally {
      // close connection
      try {
        if (rs != null) {
          rs.close();
        }
        if (st != null) {
          st.close();
        }
      } catch (Exception e) {
        log4j.error("Exception in closing connection :", e);
      }
    }

    return list;
  }

  /**
   * This method is used to get the document type list from DocumentType Reference
   * 
   * @param var
   * @return List
   */
  public List<DocumentRuleVO> getDocumentTypeList(VariablesSecureApp var) {
    PreparedStatement st = null;
    ResultSet rs = null;
    String sql = "";
    List<DocumentRuleVO> list = new ArrayList<DocumentRuleVO>();
    try {
      log4j.debug("Language>>" + var.getLanguage());
      if (var.getLanguage().equals("ar_SA")) {
        sql = "select coalesce(tr.name,list.name) as name ,list.value ";
      } else {
        sql = "select list.name ,list.value ";
      }
      sql += " from ad_ref_list list left join ad_ref_list_trl tr on list.ad_ref_list_id = tr.ad_ref_list_id "
          + " where ad_reference_id ='295CB9ADDA424ADD92F7BF5B21D12F21' order by name asc";
      st = conn.prepareStatement(sql);
      log4j.debug("doctype qry>>" + st.toString());
      rs = st.executeQuery();
      while (rs.next()) {
        DocumentRuleVO documentRuleVO = new DocumentRuleVO();
        documentRuleVO.setNo(rs.getString("value"));
        documentRuleVO.setName(rs.getString("name"));
        list.add(documentRuleVO);
      }
    } catch (final Exception e) {
      log4j.error("Exception in getDocumentTypeList() :", e);
      return list;
    } finally {
      // close connection
      try {
        if (rs != null) {
          rs.close();
        }
        if (st != null) {
          st.close();
        }
      } catch (Exception e) {
        log4j.error("Exception in closing connection :", e);
      }
    }
    return list;
  }

  /**
   * This method is used to get Document rule list
   * 
   * @param documentType
   * @param organization
   * @return list
   */
  public List<DocumentRuleVO> getDocumentRuleList(String documentType, String organization) {
    PreparedStatement st = null;
    ResultSet rs = null;
    List<DocumentRuleVO> list = new ArrayList<DocumentRuleVO>();
    try {
      st = conn.prepareStatement("select qdrh.rulevalue, qdrh.rulesequenceno, qdrh.isused, "
          + " qdrl.ad_role_id, qdrl.rolesequenceno, qdrl.roleorderno, coalesce(allowreservation,'N') as allowreservation,"
          + " (select 'Role'||rolesequenceno from  eut_documentrule_header join eut_documentrule_lines using (eut_documentrule_header_id) where eut_documentrule_header_id= qdrh.eut_documentrule_header_id and allowreservation = 'Y' limit 1 )as reservationrole,  ismultirule, iscontractcategoryrole as contractcategory, (select 'Role'||rolesequenceno from  eut_documentrule_header join eut_documentrule_lines using (eut_documentrule_header_id) where eut_documentrule_header_id= qdrh.eut_documentrule_header_id and iscontractcategoryrole = 'Y' limit 1 )as contractcategoryrole"
          + " from eut_documentrule_header qdrh "
          + " join eut_documentrule_lines qdrl on qdrh.eut_documentrule_header_id = qdrl.eut_documentrule_header_id "
          + " where qdrh.ad_client_id = ? and qdrh.ad_org_id =?  and qdrh.document_type = ?  "
          + " group by qdrh.rulevalue, qdrh.rulesequenceno, qdrh.isused, qdrl.ad_role_id, qdrl.rolesequenceno, qdrl.roleorderno, qdrl.allowreservation,qdrh.eut_documentrule_header_id,qdrl.iscontractcategoryrole"
          + " order by qdrh.rulesequenceno, qdrl.rolesequenceno, qdrl.roleorderno,qdrl.iscontractcategoryrole");
      st.setString(1, vars.getClient());
      st.setString(2, organization);
      st.setString(3, documentType);
      rs = st.executeQuery();
      log4j.debug(" Query: " + st.toString());

      while (rs.next()) {
        DocumentRuleVO documentRuleVO = new DocumentRuleVO();
        documentRuleVO.setRuleValue(rs.getBigDecimal("rulevalue"));
        documentRuleVO.setRuleSequenceNo(rs.getInt("rulesequenceno"));
        documentRuleVO.setRuleUsed(rs.getString("isused"));
        documentRuleVO.setId(rs.getString("ad_role_id"));
        documentRuleVO.setRoleSequenceNo(rs.getInt("rolesequenceno"));
        documentRuleVO.setRoleOrderNo(rs.getInt("roleorderno"));
        documentRuleVO.setAllowReservation(rs.getString("allowreservation"));
        documentRuleVO.setReservation_role(rs.getString("reservationrole"));
        documentRuleVO.setIsMultiRule(rs.getString("ismultirule"));
        documentRuleVO.setContractcategory_role(rs.getString("contractcategoryrole"));
        documentRuleVO.setIscontractcategory(rs.getString("contractcategory"));
        list.add(documentRuleVO);
      }
    } catch (final Exception e) {
      log4j.error("Exception in getDocumentRuleList() :", e);
      return list;
    } finally {
      // close connection
      try {
        if (rs != null) {
          rs.close();
        }
        if (st != null) {
          st.close();
        }
      } catch (Exception e) {
        log4j.error("Exception in closing connection :", e);
      }
    }
    return list;
  }

  /**
   * This method is used to insert document rule
   * 
   * @param documentType
   * @param list
   * @param organization
   * @param inpIsMultiRule
   * @return
   */
  @SuppressWarnings("resource")
  public int insertDocumentRule(String documentType, List<DocumentRuleVO> list, String organization,
      String inpIsMultiRule) {
    PreparedStatement headerSt = null, lineSt = null, st = null, st1 = null;
    ResultSet rs = null, rs1 = null, rs2 = null, rs3 = null;
    boolean headerCnt = false, lineCnt = false, chkDocRulSeqInPrg = false, changesexists = false;
    String[] orgIds = null;
    String[] childOrgArr = null;
    String orgChdList = "", childOrg = "", currentroleId = "";
    Boolean WFA = false;
    int loopcount = 0;

    try {
      // Delete Unused Roles
      // DocumentRuleDAO.deleteUnusedNextRoles(conn, documentType);

      // Check Current Roles exits in Next Role Approval List
      StringBuffer roleList = new StringBuffer("");
      st = conn.prepareStatement(
          "select ad_role_id, rolesequenceno from eut_documentrule_lines WHERE eut_documentrule_header_id in (select eut_documentrule_header_id from eut_documentrule_header where ad_client_id = ? and ad_org_id = ? and document_type = ? );");
      st.setString(1, vars.getClient());
      st.setString(2, organization);
      st.setString(3, documentType);
      rs = st.executeQuery();
      while (rs.next()) {
        boolean haveRole = false;
        for (DocumentRuleVO ruleVO : list) {
          if (ruleVO.getId().equals(rs.getString("ad_role_id"))) {
            haveRole = true;
            if (ruleVO.getRoleSequenceNo() != rs.getInt("rolesequenceno")) {
              roleList.append(",'" + rs.getString("ad_role_id") + "' ");
            }
            break;
          }
        }
        if (!haveRole) {
          roleList.append(",'" + rs.getString("ad_role_id") + "' ");
        }
      }

      // check record is in waiting for approval, if yes we should not allow to change.
      st = conn.prepareStatement("select eut_getchildorglist(?,?)");
      st.setString(1, vars.getClient());
      st.setString(2, organization);
      rs = st.executeQuery();
      if (rs.next()) {
        orgChdList = rs.getString("eut_getchildorglist");
      }
      orgIds = (orgChdList).split(",");
      List<String> orglist = new ArrayList<String>(Arrays.asList(orgIds));

      for (int i = 1; i < orglist.size(); i++) {
        st = conn.prepareStatement(
            "select count (eut_documentrule_header_id) as count from eut_documentrule_header  where ad_client_id = ? and ad_org_id = "
                + orglist.get(i) + " and document_type = ?");
        st.setString(1, vars.getClient());
        st.setString(2, documentType);
        rs1 = st.executeQuery();
        if (rs1.next() && rs1.getInt("count") > 0) {
          st = conn.prepareStatement("select eut_getchildorglist(?," + orglist.get(i) + ")");
          st.setString(1, vars.getClient());
          rs2 = st.executeQuery();
          if (rs2.next()) {
            childOrg = rs2.getString("eut_getchildorglist");
          }
          childOrgArr = (childOrg).split(",");
          List<String> childList = new ArrayList<String>(Arrays.asList(childOrgArr));
          for (int j = 0; j < childList.size(); j++) {
            orglist.remove(childList.get(j));
          }
        }
      }
      String orgfinal = orglist.toString().replaceAll("[\\[\\]]", "");
      // if its not multirule and delete all the rule (size==0) then directly we can check any
      // record is in WFA by using document type .
      log4j.debug("inpIsMultiRule:" + inpIsMultiRule);
      if (!inpIsMultiRule.equals("Y") || list.size() == 0) {
        st = conn.prepareStatement(
            "select count(*) as count from eut_next_role_line qnrl join eut_next_role qnrh on qnrh.eut_next_role_id = qnrl.eut_next_role_id where qnrl.ad_client_id = ? and qnrl.ad_org_id in("
                + orgfinal + ") and qnrh.document_type = ? ");
        st.setString(1, vars.getClient());
        st.setString(2, documentType);
        log4j.debug("st:" + st.toString());
        rs3 = st.executeQuery();
        if (rs3.next() && rs3.getInt("count") > 0) {
          WFA = true;
          // return -1;
        }
      } else {
        int lastRuleSeqNos = 0, lastRuleSeqNos2 = 0;
        String lastreqRole = null;
        String newRoleList = "";

        // chk duplicate requester role , if duplicate requester role not allow to add the new
        // document rule.
        ArrayList<String> requesterrolelist = new ArrayList<String>();
        log4j.debug("size:" + list.size());
        for (DocumentRuleVO ruleVO : list) {
          log4j.debug("getRequester:" + ruleVO.getRequester());
          if (lastRuleSeqNos2 == 0 || lastRuleSeqNos2 != ruleVO.getRuleSequenceNo()) {
            requesterrolelist.add(ruleVO.getRequester());
            lastRuleSeqNos2 = ruleVO.getRuleSequenceNo();
          }
        }
        log4j.debug("requesterrolelist:" + requesterrolelist.size());
        HashSet<String> removeduplicaterole = new HashSet<String>(requesterrolelist);
        log4j.debug("removeduplicaterole:" + removeduplicaterole.size());

        if (requesterrolelist.size() != removeduplicaterole.size()) {
          return 4;
        }
        // end check duplicate requester role

        /*
         * check if delete full rule sequence line (for ex: deleting total line of rule1 seq) then
         * chk delete requester role having an pending transaction
         */
        // get latest request Role
        if (removeduplicaterole.size() > 0) {
          Iterator<String> iterator = removeduplicaterole.iterator();
          while (iterator.hasNext()) {
            if (currentroleId.equals("")) {
              currentroleId = "'" + iterator.next() + "'";
            } else {
              currentroleId = currentroleId + ",'" + iterator.next() + "'";
            }
          }
        }
        // get deleting requester role by using latest requester role
        st1 = conn.prepareStatement(
            " select requester_role from eut_documentrule_header where  requester_role not in ("
                + currentroleId + ") and ad_client_id = ? and ad_org_id = ? and document_type = ?");
        st1.setString(1, vars.getClient());
        st1.setString(2, organization);
        st1.setString(3, documentType);
        log4j.debug("st:" + st1.toString());
        rs = st1.executeQuery();
        if (rs.next()) {
          // chk any pending transaction for deleting requester role
          chkDocRulSeqInPrg = UtilityDAO.chkDocRulSeqInPrg(conn, vars.getClient(), orgfinal,
              documentType, rs.getString("requester_role"));
          log4j.debug("chkDocRulSeqInPrg:" + chkDocRulSeqInPrg);
          if (chkDocRulSeqInPrg)
            return -1;
        }
        // end chk if deleting full line of rule sequence

        // checking any edit/delete process happening in any of the document rule sequence
        for (DocumentRuleVO ruleVO : list) {
          loopcount += 1;
          log4j.debug("loopcount:" + loopcount);
          // grouping roleId based on Rule seq no.
          if (lastRuleSeqNos == 0 || lastRuleSeqNos == ruleVO.getRuleSequenceNo()) {
            if (newRoleList.equals(""))
              newRoleList = newRoleList + "'" + ruleVO.getId() + "'";
            else
              newRoleList = newRoleList + ",'" + ruleVO.getId() + "'";

            lastRuleSeqNos = ruleVO.getRuleSequenceNo();
            lastreqRole = ruleVO.getRequester();
            /*
             * if list reached final record need to chk lastreqRole having any pending transaction .
             * if they have throw error
             */
            if (loopcount == list.size()) {
              changesexists = UtilityDAO.chkUpdateExistsDR(conn, vars.getClient(), organization,
                  documentType, newRoleList, lastRuleSeqNos);
              if (changesexists) {
                chkDocRulSeqInPrg = UtilityDAO.chkDocRulSeqInPrg(conn, vars.getClient(), orgfinal,
                    documentType, lastreqRole);
                if (chkDocRulSeqInPrg) {
                  WFA = true;
                  break;
                }
              } else {
                WFA = false;
              }
            }
            log4j.debug("newRoleList:" + newRoleList);
          } else {
            /*
             * if rule seq no change like rule 1 to rule 2 that time , have to validate any pending
             * transaction there for rule 1 requester role
             */
            changesexists = UtilityDAO.chkUpdateExistsDR(conn, vars.getClient(), organization,
                documentType, newRoleList, lastRuleSeqNos);
            log4j.debug("changesexists:" + changesexists);
            if (changesexists) {
              chkDocRulSeqInPrg = UtilityDAO.chkDocRulSeqInPrg(conn, vars.getClient(), orgfinal,
                  documentType, lastreqRole);
              log4j.debug("chkDocRulSeqInPrg:" + chkDocRulSeqInPrg);
              if (chkDocRulSeqInPrg) {
                WFA = true;
                break;
              }
            } else {
              WFA = false;
            }
            newRoleList = null;
            newRoleList = "'" + ruleVO.getId() + "'";
            lastRuleSeqNos = ruleVO.getRuleSequenceNo();
            lastreqRole = ruleVO.getRequester();
            /*
             * if list reached final record need to chk lastreqRole having any pending transaction .
             * if they have throw error
             */
            if (loopcount == list.size()) {
              changesexists = UtilityDAO.chkUpdateExistsDR(conn, vars.getClient(), organization,
                  documentType, newRoleList, lastRuleSeqNos);
              if (changesexists) {
                chkDocRulSeqInPrg = UtilityDAO.chkDocRulSeqInPrg(conn, vars.getClient(), orgfinal,
                    documentType, lastreqRole);
                if (chkDocRulSeqInPrg) {
                  WFA = true;
                  break;
                }
              } else {
                WFA = false;
              }
            }
          }
        }

      }
      log4j.debug(" WFA: " + WFA);
      if (WFA)
        return -1;

      // Delete existing records
      deleteDocumentRule(documentType, organization);

      headerSt = conn.prepareStatement(
          "INSERT INTO eut_documentrule_header(eut_documentrule_header_id, ad_client_id, ad_org_id, createdby, updatedby, document_type, rulevalue, rulesequenceno, ismultirule, requester_role) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
      lineSt = conn.prepareStatement(
          "INSERT INTO eut_documentrule_lines(eut_documentrule_lines_id, ad_client_id, ad_org_id, createdby, updatedby, eut_documentrule_header_id, ad_role_id, rolesequenceno, roleorderno, allowreservation, iscontractcategoryrole) VALUES (get_uuid(), ?, ?, ?, ?, ?, ?, ?, ?, ?,?);");

      String uniqueId = "";
      int lastRuleSeqNo = 0;
      for (DocumentRuleVO ruleVO : list) {
        if (lastRuleSeqNo == 0 || lastRuleSeqNo != ruleVO.getRuleSequenceNo()) {
          uniqueId = SequenceIdData.getUUID();
          headerSt.setString(1, uniqueId);
          headerSt.setString(2, vars.getClient());
          headerSt.setString(3, organization);
          headerSt.setString(4, vars.getUser());
          headerSt.setString(5, vars.getUser());
          headerSt.setString(6, documentType);
          headerSt.setBigDecimal(7, ruleVO.getRuleValue());
          headerSt.setInt(8, ruleVO.getRuleSequenceNo());
          headerSt.setString(9, inpIsMultiRule);
          headerSt.setString(10, ruleVO.getRequester());

          headerSt.addBatch();
          headerCnt = true;
          lastRuleSeqNo = ruleVO.getRuleSequenceNo();
        }

        lineSt.setString(1, vars.getClient());
        lineSt.setString(2, organization);
        lineSt.setString(3, vars.getUser());
        lineSt.setString(4, vars.getUser());
        lineSt.setString(5, uniqueId);
        lineSt.setString(6, ruleVO.getId());
        lineSt.setInt(7, ruleVO.getRoleSequenceNo());
        lineSt.setInt(8, ruleVO.getRoleOrderNo());
        lineSt.setString(9, ruleVO.getAllowReservation());
        lineSt.setString(10, ruleVO.getIscontractcategory());

        lineSt.addBatch();
        lineCnt = true;
      }
      if (headerCnt)
        headerSt.executeBatch();
      if (lineCnt)
        lineSt.executeBatch();

      // Update Rule Seq No
      lineSt.clearBatch();
      lineCnt = false;
      int i = 0;
      lineSt = conn.prepareStatement(
          "update eut_documentrule_header set rulesequenceno = ? where eut_documentrule_header_id = ?;");
      st = conn.prepareStatement(
          "select eut_documentrule_header_id from eut_documentrule_header where ad_client_id = ? and ad_org_id = ? and document_type = ? order by rulevalue desc;");
      st.setString(1, vars.getClient());
      st.setString(2, organization);
      st.setString(3, documentType);
      rs = st.executeQuery();
      while (rs.next()) {
        lineSt.setInt(1, ++i);
        lineSt.setString(2, rs.getString("eut_documentrule_header_id"));
        lineSt.addBatch();
        lineCnt = true;
      }
      if (lineCnt)
        lineSt.executeBatch();
    } catch (final Exception e) {
      log4j.error("Exception in insertDocumentRule() :", e);
      return 0;
    } finally {
      // close connection
      try {
        if (rs != null) {
          rs.close();
        }
        if (rs1 != null) {
          rs1.close();
        }
        if (rs2 != null) {
          rs2.close();
        }
        if (rs3 != null) {
          rs3.close();
        }
        if (headerSt != null) {
          headerSt.close();
        }
        if (lineSt != null) {
          lineSt.close();
        }
        if (st != null) {
          st.close();
        }
        if (st1 != null) {
          st1.close();
        }

      } catch (Exception e) {
        log4j.error("Exception in closing connection :", e);
      }

    }
    return 1;
  }

  /**
   * This method is used to delete document rule
   * 
   * @param documentType
   * @param organization
   * @return
   */
  public boolean deleteDocumentRule(String documentType, String organization) {
    PreparedStatement st = null;
    try {
      st = conn.prepareStatement(
          "DELETE FROM eut_documentrule_lines WHERE eut_documentrule_header_id in (select eut_documentrule_header_id from eut_documentrule_header where ad_client_id = ? and ad_org_id = ? and document_type = ? );");
      st.setString(1, vars.getClient());
      st.setString(2, organization);
      st.setString(3, documentType);
      st.executeUpdate();
      if (st != null) {
        st.close();
      }
      st = conn.prepareStatement(
          "DELETE FROM eut_documentrule_header WHERE ad_client_id = ? and ad_org_id = ? and document_type = ?;");
      st.setString(1, vars.getClient());
      st.setString(2, organization);
      st.setString(3, documentType);
      st.executeUpdate();
    } catch (final Exception e) {
      log4j.error("Exception in deleteDocumentRule() :", e);
      return false;
    } finally {
      // close connection
      try {
        if (st != null) {
          st.close();
        }
      } catch (Exception e) {
        log4j.error("Exception in closing connection :", e);
      }
    }
    return true;
  }

  /**
   * This method is used to delete unused next roles
   * 
   * @param con
   * @param documentType
   * @return
   */
  public static boolean deleteUnusedNextRoles(Connection con, String documentType) {
    String sqlCondition = "";
    PreparedStatement st = null;
    try {
      if (documentType.equals(Resource.Bid_Management)) {
        sqlCondition += "(select eut_next_role_id from escm_bidmgmt where eut_next_role_id is not null)";
      } else if (documentType.equals(Resource.PAYMENT_OUT_RULE)) {
        sqlCondition += "(select eut_next_role_id from fin_payment where eut_next_role_id is not null)";
      } else if (documentType.equals(Resource.PURCHASE_ORDER_RULE)) {
        sqlCondition += "(select em_eut_next_role_id from c_order where em_eut_next_role_id is not null)";
      } else if (documentType.equals(Resource.GLJOURNAL_RULE)) {
        sqlCondition += "(select em_eut_next_role_id from gl_journal where em_eut_next_role_id is not null)";
      } else if (documentType.equals(Resource.BUDGET_PREPARATION_RULE)) {
        sqlCondition += "(select eut_next_role_id from efin_budget_preparation where eut_next_role_id is not null)";
      } else if (documentType.equals(Resource.BUDGET_ENTRY_RULE)) {
        sqlCondition += "((select eut_next_role_id from efin_budget where eut_next_role_id is not null)";
        sqlCondition += "union (select eut_next_role_id from  efin_budgetadd where eut_next_role_id is not null))";
      } else if (documentType.equals(Resource.MANUAL_ENCUMBRANCE_RULE)) {
        sqlCondition += "(select eut_next_role_id from efin_budget_manencum where eut_next_role_id is not null)";
      } else if (documentType.equals(Resource.AP_INVOICE_RULE)
          || documentType.equals(Resource.AP_Prepayment_App_RULE)
          || documentType.equals(Resource.AP_Prepayment_Inv_RULE)) {
        sqlCondition += "(select em_eut_next_role_id from c_invoice where em_eut_next_role_id is not null)";
      } else if (documentType.equals(Resource.BUDGET_REVISION_RULE)) {
        sqlCondition += "(select eut_next_role_id from efin_budget_transfertrx where eut_next_role_id is not null)";
      } else if (documentType.equals(Resource.PURCHASE_REQUISITION)
          || documentType.equals(Resource.PURCHASE_REQUISITION_LIMITED)) {
        sqlCondition += "(select em_eut_next_role_id from m_requisition where em_eut_next_role_id is not null)";
      } else if (documentType.equals(Resource.MATERIAL_ISSUE_REQUEST)
          || documentType.equals(Resource.MATERIAL_ISSUE_REQUEST_IT)) {
        sqlCondition += "(select eut_next_role_id from escm_material_request where eut_next_role_id is not null)";
      } else if (documentType.equals(Resource.Return_Transaction)) {
        sqlCondition += "(select em_eut_next_role_id from m_inout where em_eut_next_role_id is not null)";
      } else if (documentType.equals(Resource.CUSTODY_TRANSFER)) {
        sqlCondition += "(select em_eut_next_role_id from m_inout where em_escm_iscustody_transfer='Y' and  em_eut_next_role_id is not null)";
      } else if (documentType.equals(Resource.PROPOSAL_MANAGEMENT)
          || documentType.equals(Resource.PROPOSAL_MANAGEMENT_DIRECT)) {
        sqlCondition += "(select eut_next_role_id from escm_proposalmgmt where eut_next_role_id is not null)";
      } else if (documentType.equals(Resource.BUDGET_ADJUSTMENT_RULE)) {
        sqlCondition += "(select eut_next_role_id from efin_budgetadj where eut_next_role_id is not null)";
      } else if (documentType.equals(Resource.BCU_BUDGET_DISTRIBUTION)
          || documentType.equals(Resource.ORG_BUDGET_DISTRIBUTION)) {
        sqlCondition += "(select eut_next_role_id from efin_fundsreq where eut_next_role_id is not null)";
      } else if (documentType.equals(Resource.GLJOURNAL_RULE)) {
        sqlCondition += "(select em_eut_next_role_id from gl_journal where em_eut_next_role_id is not null)";
      } else if (documentType.equals(Resource.TECHNICAL_EVALUATION_EVENT)) {
        sqlCondition += "(select eut_next_role_id from escm_technicalevl_event where eut_next_role_id is not null)";
      } else if (documentType.equals(Resource.RDV_Transaction)) {
        sqlCondition += "((select eut_next_role_id from efin_rdvtxn where eut_next_role_id is not null) union all (select eut_next_role_id from efin_rdvtxnline where eut_next_role_id is not null)) ";
      } else {
        sqlCondition += "('0') and 1 = 0";
      }

      // Delete Next Role Lines
      st = con.prepareStatement(
          "delete from eut_next_role_line where eut_next_role_id in (select eut_next_role_id from eut_next_role where document_type = ? and eut_next_role_id not in "
              + sqlCondition + ");");
      st.setString(1, documentType);
      st.executeUpdate();
      if (st != null) {
        st.close();
      }
      // Delete Next Role
      st = con.prepareStatement(
          "delete from eut_next_role where document_type = ? and eut_next_role_id not in "
              + sqlCondition + ";");
      st.setString(1, documentType);
      st.executeUpdate();
    } catch (final Exception e) {
      log4j.error("Exception in deleteUnusedNextRoles() Method", e);
    } finally {
      // close connection
      try {
        if (st != null) {
          st.close();
        }
      } catch (Exception e) {
        log4j.error("Exception in closing connection :", e);
      }
    }
    return true;
  }
}