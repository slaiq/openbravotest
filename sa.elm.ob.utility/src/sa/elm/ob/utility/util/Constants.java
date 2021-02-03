package sa.elm.ob.utility.util;

public class Constants {
  public static final String sDRAFT = "جديد";
  public static final String vDRAFT = "DR";
  public static final String sWAITINGFORAPPROVAL = "في انتظار موافقتكم";
  public static final String sWAITINGFOR_S_APPROVAL = "%s";
  public static final String sWAITINGFORCANCELAPPROVAL = "في انتظار الغاء الاعتماد";
  public static final String sWAITINGFOR_S_CANCELAPPROVAL = "في انتظار اعتماد الالغاء من السيد %s";
  public static final String vWAITINGFORAPPROVAL = "WFA";
  public static final String vWAITINGFORCANCELAPPROVAL = "WFCA";
  public static final String sAPPROVED = "معتمد";
  public static final String vAPPROVED = "APP";
  public static final String sREWORKDRAFT = "تعديل";
  public static final String vREWORKDRAFT = "RDR";
  public static final String sREWORK = "تعديل";
  public static final String vREWORK = "RWK";
  public static final String sCANCEL = "تم الغاء الطلب من الجهة الطالبة";
  public static final String vCANCEL = "RC";
  public static final String vBLOCK = "BL";
  public static final String vBLOCK_P = "BL-P";
  public static final String vISSUE = "ISS";
  public static final String vISSUE_P = "ISS-P";
  public static final String vMob = "MOB";
  public static final String vMob_P = "MOB-P";
  public static final String sREJECTED = "مرفوض";
  public static final String sCANCELLED = "ملغى";
  public static final String vCANCELLED = "CAN";
  public static final String ESCMERROR = "Error";
  public static final String ESCMDOESNOTEXIST = "does not exist for ";
  public static final String ESCMORGANIZATIONMANAGER = "Organization manager";
  public static final String ESCMUSERNOTEXIST = "ESCMUSERNOTEXIST";
  public static final String ESCMROLENOTEXIST = "ESCMROLENOTEXIST";
  // Activiti Module Constants
  public static final String ACTVITI_LOGGED_IN_USER = "loggedInUserRoleId";
  public static final String ACTVITI_CANDIDATE_GROUP_EXPRESSIONS = "candidateRole";
  public static final String ACTVITI_DOCUMENT_TYPE = "documentType";

  public static final String TASK_SUBJECT = "taskSubject";
  public static final String TASK_REQUESTER_USERNAME = "taskRequesterUsername";
  public static final String TARGET_DECISION_IDENTIFIER = "targetDecisionIdentifier";

  public static final String TASK_REQUESTER_NAME = "taskRequesterName";
  public static final String TASK_LETTER_NUMBER = "taskLetterNumber";
  public static final String TAKS_REQUEST_DATE = "taskRequestDate";
  public static final String TASK_STATUS = "taskStatus";
  public static final String TASK_TYPE = "taskType";
  public static final String TASK_REQUESTER_EMAIL = "taskRequesterEmail";
  public static final String TASK_ACTION_HISTORY_ID = "taskActionHistoryId";

  public static final String UPDATE_PROFILE_WORKFLOW_KEY = "updateProfileWorkflow";
  public static final String BUSINESS_MISSION_WORKFLOW_KEY = "businessMissionWorkflow";
  public static final String LEAVE_MANAGEMENT_WORKFLOW_KEY = "leaveManagementWorkflow";
  public static final String CANCEL_LEAVE_MANAGEMENT_WORKFLOW_KEY = "cancelLeaveManagementWorkflow";
  public static final String EMPLOYEE_INFORMATIONS_WORKFLOW_KEY = "empInfoWorkflow";
  public static final String CHANGE_BANK_DETAILS_WORKFLOW_KEY = "changeBankDetailsWorkflow";

  // Workflow status
  public static String REQUEST_CREATED = "CREATED";
  public static String REQUEST_PENDING = "PENDING";
  public static String REQUEST_IN_PROGRESS = "INPROGRESS";
  public static String REQUEST_APPROVED = "APPROVED";
  public static String REQUEST_REJECTED = "REJECTED";
  // Email Generic Template
  public static String GENERIC_TEMPLATE = "generic.ftl";

  // DocBaseType In Open/Close Period Control
  public static String PURCHASE_REQUISIION_DOC = "POR";
  public static String PURCHASE_ORDER_DOC = "POO";

  // Forward/Request More Information
  // Transaction Screens in SCM
  public static String BID_MANAGEMENT = "BID";
  public static String PURCHASE_INVOICE = "PI";
  public static String PURCHASE_REQUISITION_DIRECT = "PRD";
  public static String PURCHASE_REQUISITION_LIMITED_TENDER = "PRL";
  public static String PROPOSAL_MANAGEMENT_DIRECT = "PROD";
  public static String PROPOSAL_MANAGEMENT_LIMITED_TENDER = "PROL";
  public static String MATERIAL_ISSUE_REQUEST = "MIR";
  public static String MATERIAL_ISSUE_REQUEST_IT = "MIRIT";
  public static String SITE_MATERIAL_ISSUE_REQUEST = "SMIR";
  public static String PURCHASE_REQUISITION = "PRD";
  public static String PURCHASE_ORDER_AND_CONTRACT_SUMMARY = "PO";
  public static String PROPOSAL_MANAGEMENT = "PROD";
  public static String Custody_Transfer = "CT";
  public static String TECHNICAL_EVALUATION_EVENT = "TEE";
  public static String RETURN_TRANSACTION = "RT";
  public static String RECEIPT_DELIVERY_VERIFICATION = "RDV";
  public static String RECEIPT_DELIVERY_VERIFICATION_ADVANCE = "RDVA";

  // Transaction Screens in Finance
  public static String BUDGET = "BUD";
  public static String BUDGET_REVISION = "BR";
  public static String FundsReqMgmt = "BCU";
  public static String ENCUMBRANCE = "ENCUM";
  public static String FundsReqMgmt_ORG = "BCUORG";
  public static String BUDGETADJUSTMENT = "BUDADJ";
  public static String PROPERTY_COMP = "PC";
  // Funds Req Mgmt
  public static String BCU_FUNDS = "BCUR";
  public static String BCU_ORG = "ORGR";

  // Forward/RMI
  public static String FORWARD = "F";
  public static String REQUEST_MORE_INFORMATION = "RMI";

  // Request/Response
  public static String REQUEST_MORE_INFORMATION_REQUEST = "RMIREQ";
  public static String REQUEST_MORE_INFORMATION_RESPONSE = "RMIRES";
  public static String REQUEST = "REQ";
  public static String RESPONSE = "RES";

  // Revoke
  public static String FORWARD_REVOKE = "FR";
  public static String REQUEST_MORE_INFORMATION_REVOKE = "RMIR";

  // Status
  public static String COMPLETE = "CO";
  public static String DRAFT = "DR";

  // Process Type

  // Purchase Requisition
  public static String PUBLIC_BID = "PB";
  public static String LIMITED_BID = "LB";
  public static String DIRECT_PO = "DP";
  // Proposal Management
  public static String TENDER = "TR";
  public static String LIMITED = "LD";
  public static String DIRECT = "DR";
  // Material Issue Request
  public static String MIR = "EUT_112";
  public static String MIR_IT = "EUT_115";
  // Purchase Order and Contract Summary
  public static String PURCHASE_ORDER = "PO";
  // Custody_Transfer
  public static String CT = "CT";
  // Technical Evaluation Event
  public static String TECHNICAL_EVALUATION = "TEE";
  // Encumbrance
  public static String ENCUMBRANCE_DOCTYPE = "ENC";

  // Purchase Requisition Document Type
  public static String REQUISITION_DOCTYPE = "REQ";

  // purchase invoice
  public static String AP_INVOICE = "API";
  public static String AP_PREPAYMENT_INVOICE = "PPI";
  public static String PREPAYMENT_APPLICATION = "PPA";
  // Window ReferenceId
  public static String PURCHASE_REQUISITION_W = "800092";
  public static String PROPOSAL_MANAGEMENT_W = "CAF2D3EEF3B241018C8F65E8F877B29F";
  public static String MATERIAL_ISSUE_REQUEST_W = "D8BA0A87790B4B67A86A8DF714525736";
  public static String PURCHASE_ORDER_AND_CONTRACT_SUMMARY_W = "2ADDCB0DD2BF4F6DB13B21BBCCC3038C";
  public static String Custody_Transfer_W = "D6F05B3A695E4D6BB357E1B6686E3D4D";
  public static String SITE_MATERIAL_ISSUE_REQUEST_W = "B81CF41736534BA796E4A9D729CF9F65";
  public static String TECHNICAL_EVALUATION_EVENT_W = "006832D5A20E45289F191D08949D252B";
  public static String RETURN_TRANSACTION_W = "E397822E8DAB4FCDACC84F5C27455F8C";
  public static String FUNDS_REQ_MGMT_W = "4824ABD4AE6E49F68F2AAFE976EFFEC2";
  public static String RDV_W = "A9E4930BC3A8499C82E358FADA3CDEC8";
  public static String PURCHASE_INVOICE_W = "183";
  public static String BID_MANAGEMENT_W = "E509200618424FD099BAB1D4B34F96B8";
  public static String ANNOUNCEMENT_SUMMARY_W = "72AF0A6A09494113ABFA815B367EF930";
  public static String PO_Hold_Plan_Details_W = "63275FED6D4C4CFA97AE5FFF661FCB43";
  public static String Budget_HoldPlan_Details_W = "562A0972E03C443FA7657900C336BD5F";
  public static String Budget_Revision_W = "05C3944B54FE4C5DA0E735D1144DCB94";
  public static String OPEN_ENVELOPE_EVENT_W = "62E42B7D4CF74BF08532F18D5AF084FD";
  public static String PO_RECEIPT_W = "184";
  // approve and reject
  public static String APPROVE = "APP";
  public static String REJECT = "REJ";

  // PO Receipt
  public static String RECEIVING = "IR";
  public static String SITE_RECEIVING = "SR";
  public static String INSPECT = "INS";
  public static String DELIVERY = "DEL";
  public static String RETURN = "RET";
  public static String PROJECT_RECEIVING = "PROJ";

  // Purchase Agreement
  public static String PURCHASE_AGREEMENT = "PUR_AG";
  public static String PURCHASE_RELEASE = "PUR_REL";
  public static String PURCHASE_AGREEMENT_DOC_SEQ = "C_Order_Agreement";
  public static String PURCHASE_RELEASE_DOC_SEQ = "C_Order_Release";

  // PO RECEIPT amount and quanity based constanct
  public static String AMOUNT_BASED = "AMT";
  public static String QTY_BASED = "QTY";

  // Document Sequence Name
  public static String PURCHASE_AGREEMENT_DOC_SEQ_NAME = "DocumentNo_C_Order_Agreement";
  public static String PURCHASE_RELEASE_DOC_SEQ_NAME = "DocumentNo_C_Order_Release";
  public static String ENCUMBRANCE_MANUAL_DOC_SEQ_NAME = "DocumentNo_Efin_Budget_Manencum_User";
  public static String RDV_MUS_DOC_SEQ_NAME = "DocumentNo_Efin_RDV_MUS";
  public static String RDV_PO_DOC_SEQ_NAME = "DocumentNo_Efin_RDV_PO";
  public static String RDV_DR_DOC_SEQ_NAME = "DocumentNo_Efin_RDV_DR";
  public static String RDV_ADVANCE_DOC_SEQ_NAME = "DocumentNo_efin_rdv_advance";

  // Email info for validation alerts notification
  public static final String SUBJECT = "Alert Validation - Background Process Check GRP";
  public static final String CONTENT_TYPE = "text/html; charset=utf-8";

  // TableId
  public static String PROPOSAL_MANAGEMENT_T = "BB9A536E94FE4CF3AA815CBE6CC66C3D";
  public static String PURCHASE_ORDER_T = "259";

  // TabId
  public static String PROPOSAL_MANAGEMENT_TAB = "D6115C9AF1DD4C4C9811D2A69E42878B";
  public static String PURCHASE_ORDER_TAB = "62248BBBCF644C18A75B92AD8E50238C";
  public static String PURCHASE_REQUISITION_TAB = "800249";
  public static String PURCHASE_REQUISITION_LINES_TAB = "800251";
  public static String BID_MANAGEMENT_LINES_TAB = "D54F30C8AD574A2A84999F327EF0E3A4";
  public static String PROPOSAL_MANAGEMENT_LINES_TAB = "88E026FD2D0446048C80E9D4749AB608";
  public static String OEE_PROPOSAL_ATTRIBUTES_TAB = "106D3D0F9C6648A9AB3F50FA69AC6BCA";
  public static String OEE_BANK_GUARANTEE_TAB = "BC7489A521854DA1B92D40ED7C7A7098";
  public static String PURCHASE_ORDER_LINES_TAB = "8F35A05BFBB34C34A80E9DEF769613F7";

  // Static reference in Arabic
  public static String INSERT = "أضافة سطور جديدة";

  // FieldId
  public static String PURCHASE_REQUISITION_SECUREDFIELD = "B0A936719BC0466EA0E42BC3EBDF62A0";
  public static String PURCHASE_REQUISITION_ID = "803836";

  // Message tag
  public static String ORDEREDLIST_OPENTAG = "<ol>";
  public static String ORDEREDLIST_CLOSETAG = "</ol>";
  public static String LIST_OPENTAG = "<li>";
  public static String LIST_CLOSETAG = "</li>";

}