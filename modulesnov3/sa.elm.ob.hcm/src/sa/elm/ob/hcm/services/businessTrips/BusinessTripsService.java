package sa.elm.ob.hcm.services.businessTrips;

import java.util.List;

import sa.elm.ob.hcm.dto.businessTrips.BusinessTripRequestDTO;

/**
 * Business Trip/Mission/Training Service Interface
 * 
 * @author Gopalakrishnan
 * @author oalbader
 *
 */

public interface BusinessTripsService {

  /**
   * @param username
   * @param businessTripRequestDTO
   * @return
   */
  BusinessTripRequestDTO submitBusinessTripRequest(String username,
      BusinessTripRequestDTO businessTripRequestDTO);

  /**
   * @param username
   * @return
   */
  List<String> getAllOriginalDecisionNoByUsername(String username);

  /**
   * @param originalDecNo
   * @return
   */
  BusinessTripRequestDTO getBusinessTripRequestByOrginalDecNo(String username,
      String originalDecNo);

  /**
   * @param username
   * @param originalDecNo
   */
  BusinessTripRequestDTO submitCancelBusinessTripRequest(String username, String originalDecNo);

  /**
   * @param username
   * @param originalDecNo
   */
  BusinessTripRequestDTO submitPaymentBusinessTripRequest(String username, String originalDecNo);

}
