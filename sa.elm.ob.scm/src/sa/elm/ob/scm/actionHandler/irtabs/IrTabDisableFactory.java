package sa.elm.ob.scm.actionHandler.irtabs;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import sa.elm.ob.finance.actionHandler.irtabs.irtabprocess.RDVSummaryDisableProcess;
import sa.elm.ob.scm.actionHandler.irtabs.irtabprocess.AnnouncementSummary;
import sa.elm.ob.scm.actionHandler.irtabs.irtabprocess.BankGrnteWrkBnch;
import sa.elm.ob.scm.actionHandler.irtabs.irtabprocess.BidManagement;
import sa.elm.ob.scm.actionHandler.irtabs.irtabprocess.CustodyRtnTransaction;
import sa.elm.ob.scm.actionHandler.irtabs.irtabprocess.InsuranceCertWrkBnch;
import sa.elm.ob.scm.actionHandler.irtabs.irtabprocess.InventoryCounting;
import sa.elm.ob.scm.actionHandler.irtabs.irtabprocess.IssueReturnTransac;
import sa.elm.ob.scm.actionHandler.irtabs.irtabprocess.MaterialIssueReqIconProcess;
import sa.elm.ob.scm.actionHandler.irtabs.irtabprocess.MaterialIssueReqLineCopyIconProcess;
import sa.elm.ob.scm.actionHandler.irtabs.irtabprocess.OpenEnvEvent;
import sa.elm.ob.scm.actionHandler.irtabs.irtabprocess.POReceipt;
import sa.elm.ob.scm.actionHandler.irtabs.irtabprocess.ProcurementCommitte;
import sa.elm.ob.scm.actionHandler.irtabs.irtabprocess.ProposalEvalEvents;
import sa.elm.ob.scm.actionHandler.irtabs.irtabprocess.ProposalMgmt;
import sa.elm.ob.scm.actionHandler.irtabs.irtabprocess.PurchaseOrdAndContSumry;
import sa.elm.ob.scm.actionHandler.irtabs.irtabprocess.RFPSalesVoucher;
import sa.elm.ob.scm.actionHandler.irtabs.irtabprocess.ReturnTrnAndCustodyTransac;
import sa.elm.ob.scm.actionHandler.irtabs.irtabprocess.TechnicalEvalEvents;
import sa.elm.ob.utility.util.irtabsutils.IRTabIconVariables;

public class IrTabDisableFactory {
  Logger log4j = Logger.getLogger(IrTabDisableFactory.class);

  public IRTabIconVariables getTab(HttpServletRequest request, JSONObject jsonData) {
    IRTabIconVariables irtabIcon = null;
    try {
      final String tabId = jsonData.getString("tabId") == null ? "" : jsonData.getString("tabId");

      /* Material Issue Request-Material Issue Request */
      if (tabId.equals("CE947EDC9B174248883292F17F03BB32")) {
        irtabIcon = new MaterialIssueReqIconProcess();
      }
      // Process copy icon for material issue request lines
      else if (tabId.equals("4AB913F4E6064ED1833ED08A8B7FA2D5")) {
        irtabIcon = new MaterialIssueReqLineCopyIconProcess();
      }
      /* Return Transaction-Header and Custody Transfer-Custody Transfer */
      else if (tabId.equals("72A6B3CA5BE848ACA976304375A5B7A6")
          || tabId.equals("CB9A2A4C6DB24FD19D542A78B07ED6C1")) {
        irtabIcon = new ReturnTrnAndCustodyTransac();
      }
      /* Return Transaction-Line and Issue Return Transaction- Header, Lines */
      else if (tabId.equals("922927563BFC48098D17E4DC85DD504C")
          || tabId.equals("0C0819F5D78A401A916BDD8ADB30E4EF")
          || tabId.equals("5B16AE5DFDEF47BB9518CDD325F31DFF")) {
        irtabIcon = new IssueReturnTransac();
      }
      /* Inventory Counting-Lines */
      else if (tabId.equals("9A4225DDEFFD40C8BFA386059CA93DEC")) {
        irtabIcon = new InventoryCounting();
      }
      /* Return Transaction-Custody Transaction and Issue Return Transaction-Custody Transaction */
      else if (tabId.equals("DD6AB8A564D5482795B0976F6A68FBC5")
          || tabId.equals("D4E9D5A2F73E4A15AEA52FD9A5A57902")) {
        irtabIcon = new CustodyRtnTransaction();
      }
      /* Procurement Committees-Members */
      else if (tabId.equals("23F1315422F341588CA43363CF21915E")) {
        irtabIcon = new ProcurementCommitte();
      }
      /*
       * Bid Management-Lines, Bid dates, Suppliers, Bid Terms and Conditions, Source Reference,
       * Header
       */
      else if (tabId.equals("D54F30C8AD574A2A84999F327EF0E3A4")
          || tabId.equals("754D4F75D3F54A3EBBC69496D27B9C3B")
          || tabId.equals("0D0A5AFFF5EA480DAB978052AD2198D3")
          || tabId.equals("9165D36805BC4B6E8B7CDE4420D09B4B")
          || tabId.equals("FC8BC787053F4759A9C2129C324834FE")
          || tabId.equals("31960EC365D746A180594FFB7B403ABB")) {
        irtabIcon = new BidManagement();
      }
      /* Announcements Summary-Announcements Summary, Media, Bids */
      else if (tabId.equals("BA8A044E0AC54DB8A51210458C4FADD9")
          || tabId.equals("D04AD680E4DB4A48BB887B031A7E06A2")
          || tabId.equals("1EF34C4055DC47BDAC85371CE8386B54")) {
        irtabIcon = new AnnouncementSummary();
      }
      /* RFP sales Voucher-Header */
      else if (tabId.equals("6F86F1F0E85C4A8F8DF36B5654BA3E3C")) {
        irtabIcon = new RFPSalesVoucher();
      }
      /* Open Envelope Event-Header, Bank Guarantee Detail */
      else if (tabId.equals("8095B818800446D795B8ADFEDE104733")
          || tabId.equals("BC7489A521854DA1B92D40ED7C7A7098")) {
        irtabIcon = new OpenEnvEvent();
      }
      /* Proposal Management-Proposal Management, Bank Guarantee Detail, Lines, Source Reference */
      else if (tabId.equals("D6115C9AF1DD4C4C9811D2A69E42878B")
          || tabId.equals("614665E1FB764B38A0EAA6153B110824")
          || tabId.equals("88E026FD2D0446048C80E9D4749AB608")
          || tabId.equals("8876DC52E0214C1C8A442F88784A9ACD")) {
        irtabIcon = new ProposalMgmt();
      }
      /*
       * Proposals Evaluation Events-Proposals Evaluation Events, Committee Recommendation,
       * Proposals, Committee Comments
       */
      else if (tabId.equals("61D6CF3612134CAF942B811EC74B1F0B")
          || tabId.equals("B95E00033F514207B2915772C2D6D282")
          || tabId.equals("53A3B7C2D094483CBC66DEE4D9715A6E")
          || tabId.equals("6E0596A123994C82BC8F80C0D2554578")) {
        irtabIcon = new ProposalEvalEvents();
      }
      /*
       * Technical Evaluation Events-Technical Evaluation Events, Committee Recommendation,
       * Proposals, Committee Comments
       */
      else if (tabId.equals("7185D00B421A4F62B403E085F00176D6")
          || tabId.equals("4937EA14A9E44775B176F79052F13BFF")
          || tabId.equals("F8DBF5C0C51E4212A331FBA07BCDAC53")
          || tabId.equals("2F500D79A67F4CF5927467A48680B829")) {
        irtabIcon = new TechnicalEvalEvents();
      }
      /*
       * Purchase Order and Contracts Summary-Header, Lines Attributes, Bank Guarantee Detail,
       * Shipment Attributes, Source Reference, Payment Terms, PO Amendment, Payment Schedule
       */
      else if (tabId.equals("62248BBBCF644C18A75B92AD8E50238C")
          || tabId.equals("8F35A05BFBB34C34A80E9DEF769613F7")
          || tabId.equals("07AF133F4E2E45AAA53D7FEA71656DD4")
          || tabId.equals("EFD9C9C596D24068ABEB15062EE2EDBC")
          || tabId.equals("832ED077041D47F49BB8AA9EB70F14EC")
          || tabId.equals("02F79A626AEE4BB4B8B12D345FFB164C")
          || tabId.equals("283293291F49463A905E37366C799426")
          || tabId.equals("1CEC4F8FFBCC41AD86E0A830880CBFF3")) {
        irtabIcon = new PurchaseOrdAndContSumry();
      }
      /* Bank Guarantee Workbench-Release, Confiscation, Extention, Amount Revision, Lines */
      else if (tabId.equals("C1779EE84BE44C30B4385F367742CE7F")
          || tabId.equals("008692D1D80444E78AAB4FDFFFA41476")
          || tabId.equals("E579C036C1C2401FA439F0F858FA8DE3")
          || tabId.equals("4E2C60BDF7894C32BF27E6CAC7684625")
          || tabId.equals("6732339A97874A85BF73542C2B5AFF88")) {
        irtabIcon = new BankGrnteWrkBnch();
      }
      /* Insurance Certificate Workbench-Extention, Release */
      else if (tabId.equals("742DBD83811C4B1FAD5570D5160B30FF")
          || tabId.equals("A76CE84017684DDD94583680C5AF7912")) {
        irtabIcon = new InsuranceCertWrkBnch();
      } else {
        irtabIcon = new POReceipt();
      }

      // RDV Summary Report
      if (tabId.equals("FDBA56F9D57A4F988F4CC6F3577428B9")
          || tabId.equals("A0F3A7D17A834A93B3BD4D2C40E77AFE")) {
        irtabIcon = new RDVSummaryDisableProcess();
      }

      if (irtabIcon != null) {
        irtabIcon.getIconVariables(request, jsonData);
      }
    } catch (Exception e) {
      log4j.error("Excpetion in getTab(): " + e);
      return null;
    }
    return irtabIcon;
  }
}