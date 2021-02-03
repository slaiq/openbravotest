package sa.elm.ob.hcm.services.profile;

import java.util.List;

import org.springframework.stereotype.Service;

import sa.elm.ob.hcm.EhcmDependents;
import sa.elm.ob.hcm.EhcmEmpPerInfo;
import sa.elm.ob.hcm.dao.profile.EmployeeProfileDAO;
import sa.elm.ob.hcm.dao.profile.EmployeeProfileUpdateDAO;
import sa.elm.ob.hcm.dto.profile.AddressInformationDTO;
import sa.elm.ob.hcm.dto.profile.DependentInformationDTO;
import sa.elm.ob.hcm.dto.profile.EmployeeAdditionalInformationDTO;
import sa.elm.ob.hcm.dto.profile.PersonalInformationDTO;
import sa.elm.ob.hcm.selfservice.exceptions.BusinessException;
import sa.elm.ob.hcm.selfservice.exceptions.SystemException;
import sa.elm.ob.hcm.util.MessageKeys;

/**
 * 
 * @author gopalakrishnan
 *
 */
@Service
public class EmployeeProfileUpdateServiceImpl implements EmployeeProfileUpdateService {

    private EmployeeProfileUpdateDAO employeeProfileUpdateDAO;
    private EmployeeProfileDAO employeeProfileDAO;

    @Override
    public Boolean updateEmployeeProfile(String userName, PersonalInformationDTO personalInformationDTO,
	    AddressInformationDTO addressInformationDTO, List<DependentInformationDTO> ehcmDependentList)
	    throws BusinessException, SystemException {
	EhcmEmpPerInfo employeeOB = checkEmployee(userName);
	employeeProfileUpdateDAO.updateEmployeeProfileByUser(employeeOB, personalInformationDTO);
	employeeProfileUpdateDAO.updateEmployeeAddress(employeeOB, addressInformationDTO);
	for (DependentInformationDTO empDep : ehcmDependentList) {
	    employeeProfileUpdateDAO.updateEmployeeDependent(employeeOB, empDep);
	}

	// default return true
	return true;
    }

    @Override
    public Boolean updateEmployeeDependent(String userName, DependentInformationDTO ehcmDependent)
	    throws BusinessException, SystemException {
	EhcmEmpPerInfo employeeOB = checkEmployee(userName);
	employeeProfileUpdateDAO.updateEmployeeDependent(employeeOB, ehcmDependent);
	// default return true
	return true;

    }

    @Override
    public Boolean updateEmployeeAddress(String userName, AddressInformationDTO addressInformationDTO)
	    throws BusinessException, SystemException {
	// TODO Auto-generated method stub
	EhcmEmpPerInfo employeeOB = checkEmployee(userName);
	employeeProfileUpdateDAO.updateEmployeeAddress(employeeOB, addressInformationDTO);
	// default return true
	return true;
    }

    @Override
    public Boolean updatePersonalInformation(String userName, PersonalInformationDTO personalInformationDTO)
	    throws BusinessException, SystemException {
	// TODO Auto-generated method stub
	EhcmEmpPerInfo employeeOB = checkEmployee(userName);
	employeeProfileUpdateDAO.updateEmployeeProfileByUser(employeeOB, personalInformationDTO);
	// default return true
	return true;
    }

    @Override
    public EhcmEmpPerInfo checkEmployee(String userName) throws BusinessException {
	// TODO Auto-generated method stub
	EhcmEmpPerInfo employeePersonalInfo = employeeProfileDAO.getEmployeeProfileByUser(userName);
	if (employeePersonalInfo == null) {
	    throw new BusinessException(MessageKeys.EMPLOYEE_NOT_AVAILABLE);
	}
	return employeePersonalInfo;
    }

    @Override
    public Boolean addDependent(String userName, DependentInformationDTO dependentInformationDTO)
	    throws BusinessException, SystemException {
	checkEmployee(userName);
	employeeProfileUpdateDAO.addDependent(userName, dependentInformationDTO);
	// TODO Auto-generated method stub
	// default return true
	return true;
    }

    @Override
    public Boolean removeDependent(String userName, String dependentId) throws BusinessException, SystemException {
	// TODO Auto-generated method stub
	// default return true
	employeeProfileUpdateDAO.removeDependent(userName, dependentId);
	return true;
    }

    @Override
    public Boolean updateContactInformation(String userName, EmployeeAdditionalInformationDTO additionalInformationDTO)
	    throws BusinessException, SystemException {
	// TODO Auto-generated method stub
	checkEmployee(userName);
	employeeProfileUpdateDAO.updateContactInformation(userName, additionalInformationDTO);
	return true;
    }

    @Override
    public EhcmDependents getDependentByNationalId(String userName, String NationalID)
	    throws SystemException, BusinessException {
	EhcmDependents ehcmDependents = employeeProfileUpdateDAO.findDependentByNationalId(userName, NationalID);
	// TODO Auto-generated method stub
	return ehcmDependents;
    }

    @Override
    public Boolean updateProfilePhoto(String userName, String photoBytes) throws SystemException, BusinessException {
	// TODO Auto-generated method stub
	employeeProfileUpdateDAO.updateProfilePhoto(userName, photoBytes);
	// by default true
	return true;
    }
}
