package sa.elm.ob.utility.util;

import org.apache.log4j.Logger;

/**
 * 
 * @author Priyanka Ranjan 31-10-2018
 */

public enum PrimaryKeyDocumentTypeE {

  BID("E509200618424FD099BAB1D4B34F96B8", "Escm_Bidmgmt_ID", "EUT_116", "BID", "BMF_BMR",
      "31960EC365D746A180594FFB7B403ABB"), PI("183", "C_Invoice_ID", "EUT_101", "PI", "PIF_PIR",
          "290"), PRD("800092", "M_Requisition_ID", "EUT_111", "PRD", "PRF_PRR", "800249"), PRL(
              "800092", "M_Requisition_ID", "EUT_118", "PRL", "PRF_PRR",
              "800249"), PROL("CAF2D3EEF3B241018C8F65E8F877B29F", "Escm_Proposalmgmt_ID", "EUT_117",
                  "PROL", "PROF_PROR", "D6115C9AF1DD4C4C9811D2A69E42878B"), PROD(
                      "CAF2D3EEF3B241018C8F65E8F877B29F", "Escm_Proposalmgmt_ID", "EUT_122", "PROD",
                      "PROF_PROR", "D6115C9AF1DD4C4C9811D2A69E42878B"), MIR(
                          "D8BA0A87790B4B67A86A8DF714525736", "Escm_Material_Request_ID", "EUT_112",
                          "MIR", "MIRF_MIRR", "CE947EDC9B174248883292F17F03BB32"), MIRIT(
                              "D8BA0A87790B4B67A86A8DF714525736", "Escm_Material_Request_ID",
                              "EUT_115", "MIRIT", "MIRF_MIRR",
                              "CE947EDC9B174248883292F17F03BB32"), PO(
                                  "2ADDCB0DD2BF4F6DB13B21BBCCC3038C", "C_Order_ID", "EUT_108", "PO",
                                  "POF_POR", "62248BBBCF644C18A75B92AD8E50238C"), SMIR(
                                      "B81CF41736534BA796E4A9D729CF9F65",
                                      "Escm_Material_Request_ID", "EUT_112", "MIR", "SMIRF_SMIRR",
                                      "4D11B5907F654B50B48212A7141F9C0D"), CT(
                                          "D6F05B3A695E4D6BB357E1B6686E3D4D", "M_InOut_ID",
                                          "EUT_114", "CT", "CTF_CTR",
                                          "CB9A2A4C6DB24FD19D542A78B07ED6C1"), TEE(
                                              "006832D5A20E45289F191D08949D252B",
                                              "Escm_Technicalevl_Event_ID", "EUT_123", "TEE",
                                              "TEEF_TEER", "7185D00B421A4F62B403E085F00176D6"), RT(
                                                  "E397822E8DAB4FCDACC84F5C27455F8C", "M_InOut_ID",
                                                  "EUT_113", "RT", "RTF_RTR",
                                                  "72A6B3CA5BE848ACA976304375A5B7A6"), BUD(
                                                      "0D8568D5973442B6ABA7EEF7D044CF78",
                                                      "Efin_Budget_ID", "EUT_103", "BUD",
                                                      "BUDF_BUDR",
                                                      "D1F0FD0F4B3D4CA7AA9BFB81BB819C62"), BCU(
                                                          "4824ABD4AE6E49F68F2AAFE976EFFEC2",
                                                          "Efin_Fundsreq_ID", "EUT_120", "BCU",
                                                          "BCUF_BCUR",
                                                          "53C8735351FC4B4A810DD5A781BC929B"), BCUORG(
                                                              "4824ABD4AE6E49F68F2AAFE976EFFEC2",
                                                              "Efin_Fundsreq_ID", "EUT_121",
                                                              "BCUORG", "BCUF_BCUR",
                                                              "53C8735351FC4B4A810DD5A781BC929B"), BR(
                                                                  "05C3944B54FE4C5DA0E735D1144DCB94",
                                                                  "Efin_Budget_Transfertrx_ID",
                                                                  "EUT_104", "BR", "BRF_BRR",
                                                                  "B50C35C1DB7B4E30A6324FBB4D9CCA5D"), ENCUM(
                                                                      "87CD157057C64D66A4A4BE4CD248116B",
                                                                      "Efin_Budget_Manencum_ID",
                                                                      "EUT_105", "ENCUM",
                                                                      "ENCUMF_ENCUMR",
                                                                      "9CBD55F879EA4DCAA4E944C0B7DC03D4"), BUDADJ(
                                                                          "609AC728E0234C57A2A4EDF13DF0BF1F",
                                                                          "Efin_Budgetadj_ID",
                                                                          "EUT_119", "BUDADJ",
                                                                          "BUDADJF_BUDADJR",
                                                                          "A9D394A5BE374ADC815DABBAF3D6D591"), RDV(
                                                                              "A9E4930BC3A8499C82E358FADA3CDEC8",
                                                                              "Efin_Rdvtxn_ID",
                                                                              "EUT_124", "RDV",
                                                                              "RDVF_RDVR",
                                                                              "A0F3A7D17A834A93B3BD4D2C40E77AFE"), RDVA(
                                                                                  "A9E4930BC3A8499C82E358FADA3CDEC8",
                                                                                  "Efin_Rdvtxn_ID",
                                                                                  "EUT_124", "RDV",
                                                                                  "RDAF_RDAR",
                                                                                  "FDBA56F9D57A4F988F4CC6F3577428B9"), API(
                                                                                      "183",
                                                                                      "C_Invoice_ID",
                                                                                      "EUT_101",
                                                                                      "API",
                                                                                      "PIF_PIR",
                                                                                      "290"), PPA(
                                                                                          "183",
                                                                                          "C_Invoice_ID",
                                                                                          "EUT_109",
                                                                                          "PPA",
                                                                                          "PIF_PIR",
                                                                                          "290"), PPI(
                                                                                              "183",
                                                                                              "C_Invoice_ID",
                                                                                              "EUT_110",
                                                                                              "PPI",
                                                                                              "PIF_PIR",
                                                                                              "290"), PC(
                                                                                                  "3D1AFC46CE1F418293C58B858C432EDB",
                                                                                                  "Efin_Property_Compensation_ID",
                                                                                                  "EUT_131",
                                                                                                  "PC",
                                                                                                  "PCF_PCR",
                                                                                                  "4FF6B76AAE424D6EB5D5F29129EBC1F6");

  private String windowId;
  private String primaryColumnKeyName;
  private String documentType;
  private String windowReference;
  private String alertWindowType;
  private String tabId;
  private static Logger log4j = Logger.getLogger(PrimaryKeyDocumentTypeE.class);

  private PrimaryKeyDocumentTypeE(String windowId, String primaryColumnKeyName, String documentType,
      String windowReference, String alertWindowType, String tabId) {
    this.windowId = windowId;
    this.primaryColumnKeyName = primaryColumnKeyName;
    this.documentType = documentType;
    this.windowReference = windowReference;
    this.alertWindowType = alertWindowType;
    this.tabId = tabId;
  }

  public String getWindowId() {
    return windowId;
  }

  public String getPrimaryKeyColumnName() {
    return primaryColumnKeyName;
  }

  public String getStrDocumentType() {
    return documentType;
  }

  public String getwindowReference() {
    return windowReference;
  }

  public String getAlertWindowType() {
    return alertWindowType;
  }

  public String getTabId() {
    return tabId;
  }

  public static PrimaryKeyDocumentTypeE getWindowType(String windowId)
      throws IllegalArgumentException {
    try {
      for (PrimaryKeyDocumentTypeE e : PrimaryKeyDocumentTypeE.values()) {
        if (e.getWindowId().equals(windowId)) {
          return e;
        }
      }
      return null;

    } catch (Exception e) {
      log4j.error("Exception in PrimaryKeyDocumentTypeE :", e);
    }
    return null;
  }

  public static PrimaryKeyDocumentTypeE getWindowReference(String docType)
      throws IllegalArgumentException {
    try {
      for (PrimaryKeyDocumentTypeE e : PrimaryKeyDocumentTypeE.values()) {
        if (e.getStrDocumentType().equals(docType)) {
          return e;
        }
      }
      return null;

    } catch (Exception e) {
      log4j.error("Exception in getWindowReference :", e);
    }
    return null;
  }

  public static PrimaryKeyDocumentTypeE getWindowTypeSpecialCase(String type) {
    try {
      if (type.equals(Constants.PUBLIC_BID) || type.equals(Constants.LIMITED_BID))
        return PRL;
      else if (type.equals(Constants.DIRECT_PO))
        return PRD;
      else if (type.equals(Constants.TENDER) || type.equals(Constants.LIMITED))
        return PROL;
      else if (type.equals(Constants.DIRECT))
        return PROD;
      else if (type.equals(Constants.MIR))
        return MIR;
      else if (type.equals(Constants.MIR_IT))
        return MIRIT;
      else if (type.equals(Constants.BCU_FUNDS))
        return BCU;
      else if (type.equals(Constants.BCU_ORG))
        return BCUORG;
      else if (type.equals(Constants.AP_INVOICE))
        return API;
      else if (type.equals(Constants.AP_PREPAYMENT_INVOICE))
        return PPI;
      else if (type.equals(Constants.PREPAYMENT_APPLICATION))
        return PPA;
    } catch (Exception e) {
      log4j.error("Exception in PrimaryKeyDocumentTypeE :", e);
    }
    return null;
  }

  public static PrimaryKeyDocumentTypeE getRdvSpecialCase(Boolean advance) {
    try {
      if (advance)
        return RDVA;
      else
        return RDV;
    } catch (Exception e) {
      log4j.error("Exception in PrimaryKeyDocumentTypeE :", e);
    }
    return null;
  }
}
