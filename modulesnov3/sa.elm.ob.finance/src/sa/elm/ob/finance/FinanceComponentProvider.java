package sa.elm.ob.finance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.openbravo.client.kernel.BaseComponentProvider;
import org.openbravo.client.kernel.Component;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.client.kernel.KernelConstants;

@ApplicationScoped
@ComponentProvider.Qualifier(FinanceComponentProvider.PROVIDER)
public class FinanceComponentProvider extends BaseComponentProvider {

  public static final String PROVIDER = "EFIN_Provider";

  @Override
  public Component getComponent(String componentId, Map<String, Object> parameters) {
    return null;
  }

  @Override
  public List<ComponentResource> getGlobalComponentResources() {

    List<ComponentResource> globalResources = new ArrayList<ComponentResource>();
    globalResources.add(createStaticResource("web/sa.elm.ob.finance/js/Receipt_type.js", true));
    globalResources
        .add(createStaticResource("web/sa.elm.ob.finance/js/budgetRevisionNewDisable.js", false));
    globalResources.add(createStaticResource("web/sa.elm.ob.finance/js/HijriDate.js", false));
    globalResources.add(createStaticResource("web/sa.elm.ob.finance/js/autoCopy.js", false));
    globalResources.add(createStaticResource("web/sa.elm.ob.finance/js/distributeAll.js", false));
    globalResources.add(
        createStaticResource("web/sa.elm.ob.finance/js/budgetAdjustmentDistributeAll.js", false));
    globalResources.add(createStaticResource("web/sa.elm.ob.finance/js/copyRecord.js", false));
    globalResources
        .add(createStaticResource("web/sa.elm.ob.finance/js/EncumbrancecopyRecord.js", false));
    globalResources.add(
        createStaticResource("web/sa.elm.ob.finance/js/budgetRevisionDistributeAll.js", false));
    /*
     * globalResources .add(createStaticResource("web/sa.elm.ob.finance/js/Efin-ob-addpayment.js",
     * true));
     */
    globalResources.add(
        createStaticResource("web/sa.elm.ob.finance/js/BudgetPreparationFields.js", true, true));
    globalResources
        .add(createStaticResource("web/sa.elm.ob.finance/js/ob-efin-budgetrevision.js", true));
    globalResources
        .add(createStaticResource("web/sa.elm.ob.finance/js/budgetHistoryInquiry.js", true));
    globalResources
        .add(createStaticResource("web/sa.elm.ob.finance/js/ob-efinacc-budgetinquiry.js", true));
    globalResources
        .add(createStaticResource("web/sa.elm.ob.finance/js/ob-efinba-validation.js", true));
    /*
     * globalResources.add(createStaticResource(
     * "web/sa.elm.ob.finance/js/budgetRevisionNewDisable.js", true));
     */
    globalResources.add(createStaticResource("web/sa.elm.ob.finance/js/UploadExcel.js", false));
    globalResources.add(createStaticResource("web/sa.elm.ob.finance/js/ProjectUploader.js", false));
    globalResources.add(
        createStaticResource("web/org.openbravo.advpaymentmngt/js/ob-aprm-addPayment.js", false));
    globalResources.add(createStaticResource("web/sa.elm.ob.finance/js/efin-ob-grid.js", false));
    globalResources
        .add(createStaticResource("web/sa.elm.ob.finance/js/efin-ob-formitem-text.js", false));
    /*
     * globalResources
     * .add(createStaticResource("web/sa.elm.ob.finance/js/FinancialAccDisableNew.js", true));
     */
    globalResources
        .add(createStaticResource("web/sa.elm.ob.finance/js/ob-efin-batchpost.js", false));
    globalResources
        .add(createStyleSheetResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + KernelConstants.SKIN_DEFAULT + "/sa.elm.ob.finance/auto_copy.css", false));
    globalResources
        .add(createStyleSheetResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + KernelConstants.SKIN_DEFAULT + "/sa.elm.ob.finance/distribute_all.css", false));
    globalResources.add(createStyleSheetResource(
        "web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + KernelConstants.SKIN_DEFAULT + "/sa.elm.ob.finance/efin_ba_distribute_all.css",
        false));
    globalResources.add(createStyleSheetResource(
        "web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + KernelConstants.SKIN_DEFAULT + "/sa.elm.ob.finance/efin_br_distribute_all.css",
        false));
    globalResources
        .add(createStyleSheetResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + KernelConstants.SKIN_DEFAULT + "/sa.elm.ob.finance/copy_Record.css", false));
    globalResources
        .add(createStyleSheetResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + KernelConstants.SKIN_DEFAULT + "/sa.elm.ob.finance/upload_excel.css", false));
    globalResources.add(createStaticResource("web/sa.elm.ob.finance/js/BudRevRules.js", false));
    globalResources
        .add(createStaticResource("web/sa.elm.ob.finance/js/ob-efinpenalty-release.js", false));
    globalResources.add(createStaticResource(
        "web/sa.elm.ob.finance/js/RdvHoldProcess/ob-efinrdvhold-release.js", false));
    globalResources.add(createStaticResource(
        "web/sa.elm.ob.finance/js/RdvHoldProcess/ob-efinrdvbudget-hold.js", false));

    globalResources
        .add(createStaticResource("web/sa.elm.ob.finance/js/Efin-Ob-Aprm-addpayment.js", false));
    globalResources.add(
        createStaticResource("web/sa.elm.ob.finance/js/PenaltyAction/bulkpenaltyrdv.js", false));
    globalResources
        .add(createStaticResource("web/sa.elm.ob.finance/js/purchaseInvoicePrint.js", false));

    globalResources
        .add(createStaticResource("web/sa.elm.ob.finance/js/ob-efinpoholdplan-release.js", false));

    return globalResources;
  }
}
