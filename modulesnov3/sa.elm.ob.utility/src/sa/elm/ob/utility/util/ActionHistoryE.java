package sa.elm.ob.utility.util;

import org.apache.log4j.Logger;

/**
 * 
 * @author Rashika.V.S 12-11-2018
 */

public enum ActionHistoryE {

  BID("escm_bidmgmt_hist", "escm_bidmgmt_id", "requestreqaction", "BID"), PRD(
      "escm_purchasereq_app_hist", "m_requisition_id", "purchasereqaction",
      "PRD"), PRL("escm_purchasereq_app_hist", "m_requisition_id", "purchasereqaction",
          "PRL"), PROL("escm_proposalmgmt_hist", "escm_proposalmgmt_id", "requestreqaction",
              "PROL"), PROD("escm_proposalmgmt_hist", "escm_proposalmgmt_id", "requestreqaction",
                  "PROD"), PO("escm_purorderacthist", "C_Order_ID", "requestreqaction", "PO"), MIR(
                      "escm_materialrequest_hist", "escm_material_request_id", "requestreqaction",
                      "MIR"), MIRIT("escm_materialrequest_hist", "escm_material_request_id",
                          "requestreqaction", "MIRIT"), SMIR("escm_materialrequest_hist",
                              "escm_material_request_id", "requestreqaction",
                              "SMIR"), CT("escm_custodytransfer_hist", "M_InOut_ID",
                                  "requestreqaction", "CT"), TEE("escm_techevlevent_apphist",
                                      "Escm_Technicalevl_Event_ID", "techevlaction",
                                      "TEE"), RT("escm_custodytransfer_hist", "M_InOut_ID",
                                          "requestreqaction", "RT"), BUD("efin_budget_app_hist",
                                              "efin_budget_id", "action",
                                              "BUD"), BCU("EFIN_FundReq_AppHist",
                                                  "Efin_Fundsreq_ID", "Fundsreqaction",
                                                  "BCU"), BCUORG("EFIN_FundReq_AppHist",
                                                      "Efin_Fundsreq_ID", "Fundsreqaction",
                                                      "BCUORG"), BR("Efin_BudTransTrx_App_Hist",
                                                          "efin_budget_transfertrx_id",
                                                          "REV_Action",
                                                          "BR"), ENCUM("efin_budget_encum_app_hist",
                                                              "efin_budget_manencum_id",
                                                              "Encum_Action", "ENCUM"), BUDADJ(
                                                                  "Efin_Budgetadj_Hist",
                                                                  "Efin_Budgetadj_ID",
                                                                  "requestreqaction",
                                                                  "BUDADJ"), RDV("efin_rdvacthist",
                                                                      "Efin_Rdvtxn_ID",
                                                                      "Requestreqaction",
                                                                      "RDV"), RDVA(
                                                                          "efin_rdvacthist",
                                                                          "Efin_Rdvtxn_ID",
                                                                          "Requestreqaction",
                                                                          "RDVA"), API(
                                                                              "efin_purchasein_app_hist",
                                                                              "c_invoice_id",
                                                                              "purchaseaction",
                                                                              "API"), PPI(
                                                                                  "efin_purchasein_app_hist",
                                                                                  "c_invoice_id",
                                                                                  "purchaseaction",
                                                                                  "PPI"), PPA(
                                                                                      "efin_purchasein_app_hist",
                                                                                      "c_invoice_id",
                                                                                      "purchaseaction",
                                                                                      "PPA"), PC(
                                                                                          "Efin_Prop_Compensation_Hist",
                                                                                          "Efin_property_compensation_id",
                                                                                          "requestreqaction",
                                                                                          "PC");
  ;

  private String historyTable;
  private String headerColumn;
  private String actionColumn;
  private String windowReference;
  private static Logger log4j = Logger.getLogger(ActionHistoryE.class);

  private ActionHistoryE(String historyTable, String headerColumn, String actionColumn,
      String windowReference) {
    this.historyTable = historyTable;
    this.headerColumn = headerColumn;
    this.actionColumn = actionColumn;
    this.windowReference = windowReference;
  }

  public String getHistoryTable() {
    return historyTable;
  }

  public String getHeaderColumn() {
    return headerColumn;
  }

  public String getActionColumn() {
    return actionColumn;
  }

  public String getWindowReference() {
    return windowReference;
  }

  public static ActionHistoryE getColumnNames(String doctype) throws IllegalArgumentException {
    try {
      for (ActionHistoryE e : ActionHistoryE.values()) {
        if (e.getWindowReference().equals(doctype))
          return e;
      }
    } catch (Exception e) {
      log4j.error("Exception in ActionHistoryE:", e);
    }
    return null;
  }
}
