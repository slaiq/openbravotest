package sa.elm.ob.scm.ad_process.printreport;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import sa.elm.ob.finance.ad_process.printreport.RDVSummaryReport;
import sa.elm.ob.utility.util.printUtils.GenerateJasperPrint;

public class GetPrintFactory {
  Logger log4j = Logger.getLogger(GetPrintFactory.class);

  public GenerateJasperPrint getPrint(HttpServletRequest request, JSONObject paramObject) {
    String paramType = "", inpWindowID = "", inpTabID = "", receiveType = "", warehouseType = "";
    GenerateJasperPrint generateJasperPrint = null;
    try {

      String action = (request.getParameter("action") == null ? ""
          : request.getParameter("action"));
      inpWindowID = (request.getParameter("inpWindowID") == null ? ""
          : request.getParameter("inpWindowID"));
      inpTabID = (request.getParameter("inpTabId") == null ? "" : request.getParameter("inpTabId"));
      receiveType = paramObject.optString("receiveType", "");
      warehouseType = paramObject.optString("warehouseType", "");

      if (action.equals("DownloadReport")) {
        paramType = paramObject.getString("paramType");
        if (inpWindowID.equals("184")) {
          generateJasperPrint = PODeliveryPrint.getInstance();
        } else if (paramType.equals("ProposalMgmt")) {
          generateJasperPrint = ProposalMgmtPrint.getInstance();
        }
        // insurance certificate
        else if (paramType.equals("ICWorkbench")) {
          generateJasperPrint = InsuranceCertificateWorkbenchPrint.getInstance();
        }
        // open envelop
        else if (paramType.equals("OpenEnvCmt")) {
          generateJasperPrint = OpenEnvelopCommitteePrint.getInstance();
        } else if (paramType.equals("BidManagement")) {
          generateJasperPrint = InvitationLetterPrint.getInstance();
        } else if (paramType.equals("MaterialReq")) {
          generateJasperPrint = MIRPrint.getInstance();
        } else if (paramType.equals("ContractExecution")) {
          generateJasperPrint = POOrderContractSummaryPrint.getInstance();
        }
        // bg workbench
        else if (paramType.equals("BankGuaranteeworkbench")) {
          generateJasperPrint = BankGuaranteeworkbenchPrint.getInstance();
        } else if (paramType.equals("ProposalEvalExecution")) {
          generateJasperPrint = ProposalEvaluationEventPrint.getInstance();
        } else if (paramType.equals("TechnicalStudy")) {
          generateJasperPrint = TechnicalEvaluationEventPrint.getInstance();
        }
      } else if (action.equals("")) {

        // Announcement Summary Report
        if (inpWindowID.equals("72AF0A6A09494113ABFA815B367EF930")) {
          generateJasperPrint = AnnouncementsummaryPrint.getInstance();
        }
        // Proposal Management
        else if (inpWindowID.equals("CAF2D3EEF3B241018C8F65E8F877B29F")) {
          generateJasperPrint = ProposalMgmtPrint.getInstance();
        }
        // po receipt
        else if (inpWindowID.equals("184")
            && (receiveType.equals("IR") || receiveType.equals("INS"))) {
          generateJasperPrint = PODeliveryPrint.getInstance();
        }
        // insurance certificate
        else if (inpWindowID.equals("13B58C9F5DA14EEC9CAD9FAF9234457D")) {
          generateJasperPrint = InsuranceCertificateWorkbenchPrint.getInstance();
        }
        // open envelop
        else if (inpWindowID.equals("62E42B7D4CF74BF08532F18D5AF084FD")) {
          generateJasperPrint = OpenEnvelopCommitteePrint.getInstance();
        }
        // rfp document delievery
        else if (inpWindowID.equals("F7521058B095442698011735E9A5AC80")) {
          generateJasperPrint = RFPDocumentDeliveryPrint.getInstance();
        }
        // Invitation letter-Bid
        else if (inpWindowID.equals("E509200618424FD099BAB1D4B34F96B8")) {
          generateJasperPrint = InvitationLetterPrint.getInstance();
        }
        // BG workbench
        else if (inpTabID.equals("6732339A97874A85BF73542C2B5AFF88")) {
          generateJasperPrint = BankGuaranteeworkbenchPrint.getInstance();
        }
        // proposal evaluation
        else if (inpWindowID.equals("9B284558C7E149B0AC245D610F8BC2F6")) {
          generateJasperPrint = ProposalEvaluationEventPrint.getInstance();
        }
        // Technical Study Report
        else if (inpWindowID.equals("006832D5A20E45289F191D08949D252B")) {
          generateJasperPrint = TechnicalEvaluationEventPrint.getInstance();
        }
        // Inventory Count Report
        else if (inpWindowID.equals("8FC04D21ED7540F2B6A4ADCE9BDD58A6")) {
          generateJasperPrint = InventoryCountPrint.getInstance();
        }
        // Po order and contract Summary Report
        else if (inpWindowID.equals("2ADDCB0DD2BF4F6DB13B21BBCCC3038C")) {
          generateJasperPrint = POOrderContractSummaryPrint.getInstance();
        } else if (inpWindowID.equals("E397822E8DAB4FCDACC84F5C27455F8C")) {
          generateJasperPrint = ReturnTransactionPrint.getInstance();
        } else if (inpWindowID.equals("D6F05B3A695E4D6BB357E1B6686E3D4D")) {
          generateJasperPrint = CustodyTransferPrint.getInstance();
        } else if (inpWindowID.equals("26209E1C023B4879BF58993F9BF9AAC9")) {
          generateJasperPrint = IssueReturnTransactionPrint.getInstance();
        }
        // MIR-Ret Warehouse
        else if (inpWindowID.equals("D8BA0A87790B4B67A86A8DF714525736")
            && (warehouseType.equals("RTW"))) {
          generateJasperPrint = MIRPrint.getInstance();
        } else if (inpTabID.equals("FDBA56F9D57A4F988F4CC6F3577428B9")
            || inpTabID.equals("A0F3A7D17A834A93B3BD4D2C40E77AFE")) {
          generateJasperPrint = RDVSummaryReport.getInstance();
        } else {
          generateJasperPrint = PrintReportPopup.getInstance();
        }
      }

      if (generateJasperPrint != null) {
        generateJasperPrint.getReportVariables(request, paramObject);
      }

    } catch (Exception e) {
      log4j.error("Excpetion while getPrint(): " + e);
      return null;
    }
    return generateJasperPrint;
  }
}
