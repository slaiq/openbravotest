package sa.elm.ob.utility.util;

/**
 * 
 * @author Gopinagh.R
 */
public enum DocumentTypeE {
  AP_INVOICE("EUT_101"), BUDGET_PREPARATION("EUT_102"), BUDGET_ENTRY("EUT_103"), BUDGET_REVISION(
      "EUT_104"), ENCUMBRANCE("EUT_105"), JOURNAL_ENTRIES("EUT_106"), PURCHASE_ORDER(
          "EUT_108"), PREPAYMENT_APPLICATION("EUT_109"), PREPAYMENT("EUT_110"), PR_DIRECT(
              "EUT_111"), MIR("EUT_112"), RETURN_TRANSACTION("EUT_113"), CUSTODY_TRANSFER(
                  "EUT_114"), MIR_IT("EUT_115"), BID("EUT_116"), PROPOSAL_LIMITED_TENDER(
                      "EUT_117"), PR_LIMITED_TENDER("EUT_118"), FUNDS_AND_COST_ADJUSTMENT(
                          "EUT_119"), BCU_FUNDS_REQUEST_MANAGEMENT(
                              "EUT_120"), ORG_FUNDS_REQUEST_MANAGEMENT("EUT_121"), DIRECT_PROPOSAL(
                                  "EUT_122"), TEE("EUT_123"), RDV("EUT_124"), RDVBH(
                                      "EUT_126"), RDV_LV("EUT_127"), PROPERTY_COMP("EUT_131");

  private String strDocumentType;

  private DocumentTypeE(String strDocumentType) {
    this.strDocumentType = strDocumentType;
  }

  public String getDocumentTypeCode() {
    return strDocumentType;
  }
}
