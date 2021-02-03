package sa.elm.ob.scm.webservice.approvedpo;

import java.util.List;

import org.springframework.stereotype.Service;

import sa.elm.ob.scm.webservice.dao.GetApprovedPoDAO;
import sa.elm.ob.scm.webservice.dto.POHeaderDTO;

/**
 * This class is implementation class for GetApprovedPoService
 * 
 * @author Sathishkumar.P
 *
 */
@Service
public class GetApprovedPoServiceImpl implements GetApprovedPoService {

  @Override
  public List<POHeaderDTO> getApprovedPO(int start, int end) {

    List<POHeaderDTO> headerDTO = GetApprovedPoDAO.getApprovedPO(start, end);
    return headerDTO;
  }

  @Override
  public List<POHeaderDTO> getApprovedPOByContractNo(String contractno) {
    List<POHeaderDTO> headerDTO = GetApprovedPoDAO.getApprovedPOByContractNo(contractno);
    return headerDTO;
  }

  @Override
  public List<POHeaderDTO> getApprovedPOByContractType(String contracttype) {
    List<POHeaderDTO> headerDTO = GetApprovedPoDAO.getApprovedPOByContractType(contracttype);
    return headerDTO;
  }

  @Override
  public List<POHeaderDTO> getApprovedPOByContractDate(String contracttype) {
    List<POHeaderDTO> headerDTO = GetApprovedPoDAO.getApprovedPOByContractDate(contracttype);
    return headerDTO;
  }

}
