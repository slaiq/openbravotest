package sa.elm.ob.utility.tabadul.scheduler;

import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.actionHandler.SupplierController;
import sa.elm.ob.utility.tabadul.TabadulAuthenticationServiceImpl;
import sa.elm.ob.utility.tabadul.TabadulIntegrationDAOImpl;
import sa.elm.ob.utility.tabadul.TabadulIntegrationServiceImpl;
import sa.elm.ob.utility.tabadul.services.BidPurchasesUpdateService;
import sa.elm.ob.utility.tabadul.services.BidPurchasesUpdateServiceImpl;

/**
 * This scheduler updates the list of purchases made to tender/bid from tabadul.
 * 
 * @author mrahim
 *
 */
public class BidPurchasesUpdateScheduler extends DalBaseProcess {

  private BidPurchasesUpdateService bidpurchaseUpdateService;
  private static final Logger log = LoggerFactory.getLogger(BidPurchasesUpdateScheduler.class);

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    OBError obError = new OBError();

    // Can be injected if dependency injection is setup
    bidpurchaseUpdateService = new BidPurchasesUpdateServiceImpl();
    bidpurchaseUpdateService.setTabadulIntegrationDAO(new TabadulIntegrationDAOImpl());
    bidpurchaseUpdateService.setTabadulIntegrationService(new TabadulIntegrationServiceImpl());
    bidpurchaseUpdateService
        .setTabadulAuthenticationService(new TabadulAuthenticationServiceImpl());
    bidpurchaseUpdateService.setSupplierController(new SupplierController());
    // Call the service to update purchases from tabadul
    try {
      bidpurchaseUpdateService.updateBidPurchasesFromTabadul();
      obError.setType("Success");
      obError.setTitle("Success");
      obError.setMessage(OBMessageUtils.messageBD("ProcessOK"));

    } catch (Exception e) {
      log.error("Exception in BidPurchasesUpdateScheduler ", e.getMessage());
      obError.setType("Error");
      obError.setTitle("Error");
      obError.setMessage(OBMessageUtils.messageBD("EUT_TABADUL.ERROR.INTERNAL_ERROR"));
    }

    bundle.setResult(obError);
  }

}
