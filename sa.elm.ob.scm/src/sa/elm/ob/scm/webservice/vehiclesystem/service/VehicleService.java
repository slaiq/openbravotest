package sa.elm.ob.scm.webservice.vehiclesystem.service;

import java.util.List;

import sa.elm.ob.scm.webservice.vehiclesystem.dto.CustodyTagDetailsIntResponse;
import sa.elm.ob.scm.webservice.vehiclesystem.dto.ProductDetailsIntRequest;

/**
 * 
 * 
 * @author Kazim
 *
 */

public interface VehicleService {

  List<CustodyTagDetailsIntResponse> getDetailsByEmployeeCode(String employeeCode) throws Exception;

  CustodyTagDetailsIntResponse getDetailsByEmployeeCodeAndTagCode(String employeeCode,
      String tagCode) throws Exception;

  boolean updateCustodyData(ProductDetailsIntRequest request) throws Exception;

}