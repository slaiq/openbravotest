package sa.elm.ob.hcm.services.businessTrips;

import java.util.List;

import org.springframework.stereotype.Service;

import sa.elm.ob.hcm.dto.businessTrips.BusinessTripRequestDTO;

/**
 * @author oalbader
 *
 */

@Service
public class BusinessTripsServiceImpl implements BusinessTripsService {

  @Override
  public BusinessTripRequestDTO submitBusinessTripRequest(String username,
      BusinessTripRequestDTO businessTripRequestDTO) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<String> getAllOriginalDecisionNoByUsername(String username) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public BusinessTripRequestDTO getBusinessTripRequestByOrginalDecNo(String username,
      String originalDecNo) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public BusinessTripRequestDTO submitCancelBusinessTripRequest(String username,
      String originalDecNo) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public BusinessTripRequestDTO submitPaymentBusinessTripRequest(String username,
      String originalDecNo) {
    // TODO Auto-generated method stub
    return null;
  }

}
