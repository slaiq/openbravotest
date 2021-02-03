package sa.elm.ob.hcm.services.employment;

import java.util.List;

import org.springframework.stereotype.Service;

import sa.elm.ob.hcm.dto.employment.CertificationsDTO;
import sa.elm.ob.hcm.dto.employment.CustodiesDTO;
import sa.elm.ob.hcm.dto.employment.QualificationsDTO;
import sa.elm.ob.hcm.dto.employment.ViewEmplInfoDTO;

@Service
public class EmploymentInformationServiceImpl implements EmploymentInformationService {

  @Override
  public List<ViewEmplInfoDTO> getAllEmpInfo(String username) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<QualificationsDTO> getQualifications(String username) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public QualificationsDTO submitQualification(String username,
      QualificationsDTO qualificationsDTO) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<CertificationsDTO> getCertifications(String username) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public CertificationsDTO submitCertificate(String username, CertificationsDTO certificationsDTO) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<CustodiesDTO> getAllCustodies(String username) {
    // TODO Auto-generated method stub
    return null;
  }

}
