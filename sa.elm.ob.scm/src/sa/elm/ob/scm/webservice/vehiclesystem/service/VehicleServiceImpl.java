package sa.elm.ob.scm.webservice.vehiclesystem.service;

import java.util.List;

import org.springframework.stereotype.Service;

import sa.elm.ob.scm.webservice.vehiclesystem.dao.VehicleSystemIntDAO;
import sa.elm.ob.scm.webservice.vehiclesystem.dto.CustodyTagDetailsIntResponse;
import sa.elm.ob.scm.webservice.vehiclesystem.dto.ProductDetailsIntRequest;

@Service
public class VehicleServiceImpl implements VehicleService {

  @Override
  public List<CustodyTagDetailsIntResponse> getDetailsByEmployeeCode(String employeeCode)
      throws Exception {
    List<CustodyTagDetailsIntResponse> list = VehicleSystemIntDAO
        .detailsByEmployeeCode(employeeCode);
    return list;
  }

  @Override
  public CustodyTagDetailsIntResponse getDetailsByEmployeeCodeAndTagCode(String employeeId,
      String tagCode) throws Exception {
    CustodyTagDetailsIntResponse list = VehicleSystemIntDAO.detailsByEmployeeAndTagCode(employeeId,
        tagCode);
    return list;
  }

  @Override
  public boolean updateCustodyData(ProductDetailsIntRequest request) throws Exception {
    return VehicleSystemIntDAO.updateCustody(request);
  }

}
