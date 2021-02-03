package sa.elm.ob.hcm.selfservice.service.login;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sa.elm.ob.hcm.dto.profile.LoginInformationDTO;
import sa.elm.ob.hcm.selfservice.dao.login.LoginDAO;
import sa.elm.ob.hcm.selfservice.exceptions.BusinessException;
import sa.elm.ob.hcm.util.MessageKeys;

/**
 * 
 * @author mrahim
 *
 */
@Service
public class LoginServiceImpl implements LoginService {

  @Autowired
  private LoginDAO loginDao;

  @Override
  public Boolean validateOldPassword(LoginInformationDTO loginInformationDTO)
      throws BusinessException {

    Boolean isPasswordCorrect = loginDao.validateOldPassword(loginInformationDTO.getLoginId(),
        loginInformationDTO.getOldPassword());
    // If the password is not correct throw exception
    if (!isPasswordCorrect) {
      throw new BusinessException(MessageKeys.INCORRECT_OLD_PASSWORD);
    }

    // return true by default
    return true;
  }

  @Override
  public Boolean changePassword(LoginInformationDTO loginInformationDTO) throws BusinessException {
    validateOldPassword(loginInformationDTO);
    // Update the password
    loginDao.updatePassword(loginInformationDTO.getLoginId(), loginInformationDTO.getNewPassword());

    return true;
  }

}
