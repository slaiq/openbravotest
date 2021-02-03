package sa.elm.ob.scm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.openbravo.client.kernel.BaseComponentProvider;
import org.openbravo.client.kernel.Component;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.client.kernel.KernelConstants;

@ApplicationScoped
@ComponentProvider.Qualifier(SCMComponentProvider.PROVIDER)
public class SCMComponentProvider extends BaseComponentProvider {

  public static final String PROVIDER = "ESCM_Provider";

  @Override
  public Component getComponent(String componentId, Map<String, Object> parameters) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<ComponentResource> getGlobalComponentResources() {

    List<ComponentResource> globalResources = new ArrayList<ComponentResource>();
    globalResources.add(createStaticResource("web/sa.elm.ob.scm/js/copyMIRLine.js", false));
    globalResources.add(createStaticResource("web/sa.elm.ob.scm/js/copyRecord.js", false));
    globalResources.add(createStaticResource("web/sa.elm.ob.scm/js/PrintReport.js", false));
    globalResources.add(createStaticResource("web/sa.elm.ob.scm/js/UpdateRecord.js", false));
    globalResources.add(createStaticResource("web/sa.elm.ob.scm/js/ApplyChanges.js", false));
    globalResources.add(createStaticResource("web/sa.elm.ob.scm/js/UnApplyChanges.js", false));
    globalResources
        .add(createStyleSheetResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + KernelConstants.SKIN_DEFAULT + "/sa.elm.ob.scm/copy_Record.css", false));
    globalResources
        .add(createStyleSheetResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + KernelConstants.SKIN_DEFAULT + "/sa.elm.ob.scm/copy_MIR_Line.css", false));
    globalResources
        .add(createStyleSheetResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + KernelConstants.SKIN_DEFAULT + "/sa.elm.ob.scm/print_pdf.css", false));
    globalResources
        .add(createStyleSheetResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + KernelConstants.SKIN_DEFAULT + "/sa.elm.ob.scm/update.css", false));
    globalResources
        .add(createStyleSheetResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + KernelConstants.SKIN_DEFAULT + "/sa.elm.ob.scm/apply.css", false));
    globalResources
        .add(createStyleSheetResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + KernelConstants.SKIN_DEFAULT + "/sa.elm.ob.scm/unapply.css", false));
    globalResources.add(createStaticResource("web/sa.elm.ob.scm/js/IrtabDisable.js", false));

    globalResources
        .add(createStaticResource("web/sa.elm.ob.scm/js/ob-escm-porecqtyvalidation.js", true));
    globalResources
        .add(createStaticResource("web/sa.elm.ob.scm/js/ob-escm-datevalidation.js", true));
    globalResources
        .add(createStaticResource("web/sa.elm.ob.scm/js/ob-escm-podatedurationcalc.js", true));
    globalResources
        .add(createStaticResource("web/sa.elm.ob.scm/js/ob-escm-createposubmit.js", true));
    globalResources
        .add(createStaticResource("web/sa.elm.ob.scm/js/ob-escm-requisitionlinecancel.js", true));
    globalResources.add(createStaticResource("web/sa.elm.ob.scm/js/ProposalType.js", false));
    globalResources.add(createStaticResource("web/sa.elm.ob.scm/js/ob-escm-updateIC.js", false));
    globalResources
        .add(createStaticResource("web/sa.elm.ob.scm/js/ob-escm-setneedbydate.js", false));

    // Import Supplier Resources
    globalResources.add(createStaticResource("web/sa.elm.ob.scm/js/ImportSupplier.js", false));
    globalResources
        .add(createStyleSheetResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + KernelConstants.SKIN_DEFAULT + "/sa.elm.ob.scm/import_supplier.css", false));

    // Purchase requistion
    globalResources.add(createStaticResource("web/sa.elm.ob.scm/js/PRDistribute.js", false));
    globalResources
        .add(createStyleSheetResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + KernelConstants.SKIN_DEFAULT + "/sa.elm.ob.scm/Copy_UniqueCode.css", false));
    // BID Management
    globalResources.add(createStaticResource("web/sa.elm.ob.scm/js/BidApplyUniqueCode.js", false));
    // Purchase order and contract summary Delete new version.
    globalResources.add(createStaticResource("web/sa.elm.ob.scm/js/PODeleteNewVersion.js", false));
    // Proposal Mgmt
    globalResources
        .add(createStaticResource("web/sa.elm.ob.scm/js/ProposalApplyUniquecode.js", false));
    // Proposal Attribute tab
    globalResources
        .add(createStaticResource("web/sa.elm.ob.scm/js/ProposalAttrApplyUniqueCode.js", false));
    // Purchase Order and Contracts Summary
    globalResources.add(createStaticResource("web/sa.elm.ob.scm/js/POApplyUniqueCode.js", false));
    // Purchase requistion
    globalResources.add(createStaticResource("web/sa.elm.ob.scm/js/DownloadReqTemplate.js", false));
    globalResources
        .add(createStyleSheetResource(
            "web/org.openbravo.userinterface.smartclient/openbravo/skins/"
                + KernelConstants.SKIN_DEFAULT + "/sa.elm.ob.scm/escm_req_download_temp.css",
            false));
    // purchase order Import and export
    globalResources
        .add(createStyleSheetResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + KernelConstants.SKIN_DEFAULT + "/sa.elm.ob.scm/PO_Export.css", false));
    globalResources.add(createStaticResource("web/sa.elm.ob.scm/js/POExport.js", false));
    globalResources.add(createStaticResource("web/sa.elm.ob.scm/js/ImportPO.js", false));
    // Open Envelop Event Import and Export
    globalResources
        .add(createStyleSheetResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + KernelConstants.SKIN_DEFAULT + "/sa.elm.ob.scm/OEE_Export.css", false));
    globalResources
        .add(createStaticResource("web/sa.elm.ob.scm/js/OpenEnvelopeEventExport.js", false));
    globalResources
        .add(createStaticResource("web/sa.elm.ob.scm/js/OpenEnvelopeEventImport.js", false));

    globalResources.add(createStaticResource("web/sa.elm.ob.scm/js/RequisitionImport.js", false));
    globalResources.add(createStaticResource("web/sa.elm.ob.scm/js/RequisitionExport.js", false));

    // Bid Mangement Import and export
    globalResources
        .add(createStyleSheetResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + KernelConstants.SKIN_DEFAULT + "/sa.elm.ob.scm/Bid_Export.css", false));
    globalResources.add(createStaticResource("web/sa.elm.ob.scm/js/BidExport.js", false));
    globalResources.add(createStaticResource("web/sa.elm.ob.scm/js/ImportBid.js", false));

    // Proposal Import and export
    globalResources
        .add(createStyleSheetResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + KernelConstants.SKIN_DEFAULT + "/sa.elm.ob.scm/ProposalExport.css", false));
    globalResources.add(createStaticResource("web/sa.elm.ob.scm/js/ProposalExport.js", false));
    globalResources.add(createStaticResource("web/sa.elm.ob.scm/js/ProposalImport.js", false));

    globalResources
        .add(createStyleSheetResource(
            "web/org.openbravo.userinterface.smartclient/openbravo/skins/"
                + KernelConstants.SKIN_DEFAULT + "/sa.elm.ob.scm/PO_Receipt_ImportExport.css",
            false));
    globalResources.add(createStaticResource("web/sa.elm.ob.scm/js/POReceiptImport.js", false));
    globalResources.add(createStaticResource("web/sa.elm.ob.scm/js/POReceiptExport.js", false));
    return globalResources;
  }
}
