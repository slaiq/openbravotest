package sa.elm.ob.scm.webservice.approvedpo;

import java.util.List;

import sa.elm.ob.scm.webservice.dto.POHeaderDTO;

public interface GetApprovedPoService {

  /**
   * This method is used to get approved PO
   * 
   * @return POHeaderDTO
   */

  List<POHeaderDTO> getApprovedPO(int page, int limit);

  /**
   * This method is used to get approved PO filtered by contract no
   * 
   * @return POHeaderDTO
   */

  List<POHeaderDTO> getApprovedPOByContractNo(String contractno);

  /**
   * This method is used to get approved PO filtered by contract type
   * 
   * @return POHeaderDTO
   */

  List<POHeaderDTO> getApprovedPOByContractType(String contracttype);

  /**
   * This method is used to get approved PO filtered by contract date
   * 
   * @return POHeaderDTO
   */

  List<POHeaderDTO> getApprovedPOByContractDate(String contractdate);

}
