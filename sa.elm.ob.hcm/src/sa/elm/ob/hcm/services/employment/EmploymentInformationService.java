package sa.elm.ob.hcm.services.employment;

import java.util.List;

import sa.elm.ob.hcm.dto.employment.CertificationsDTO;
import sa.elm.ob.hcm.dto.employment.CustodiesDTO;
import sa.elm.ob.hcm.dto.employment.QualificationsDTO;
import sa.elm.ob.hcm.dto.employment.ViewEmplInfoDTO;

/**
 * @authorg Gopalakrishnan
 * @author oalbader
 *
 */
public interface EmploymentInformationService {

  /**
   * @param username
   * @return
   */
  List<ViewEmplInfoDTO> getAllEmpInfo(String username);

  /**
   * Retrieve All Employment Qualifications
   * 
   * @param username
   * @return
   */
  List<QualificationsDTO> getQualifications(String username);

  /**
   * Add/Update Employment Qualifications
   * 
   * @param username
   * @param qualificationsDTO
   * @return
   */
  QualificationsDTO submitQualification(String username, QualificationsDTO qualificationsDTO);

  /**
   * Retrieve All Employment Skills/Certifications
   * 
   * @param username
   * @return
   */
  List<CertificationsDTO> getCertifications(String username);

  /**
   * Add/Update Employment Skills/Certifications
   * 
   * @param username
   * @param certificationsDTO
   * @return
   */
  CertificationsDTO submitCertificate(String username, CertificationsDTO certificationsDTO);

  /**
   * @param username
   * @return
   */
  List<CustodiesDTO> getAllCustodies(String username);

}
