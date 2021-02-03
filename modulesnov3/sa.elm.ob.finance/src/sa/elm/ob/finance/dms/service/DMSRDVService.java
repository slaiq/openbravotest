
package sa.elm.ob.finance.dms.service;

import sa.elm.ob.finance.EfinRDVTransaction;

public interface DMSRDVService {

  /**
   * This method is used to do DMS operation during reject, revoke and reactivate
   * 
   * @param RDV
   */
  void rejectAndReactivateOperations(EfinRDVTransaction rdv) throws Exception;

}
