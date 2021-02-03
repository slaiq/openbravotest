package sa.elm.ob.hcm.dao.profile;

import sa.elm.ob.hcm.EhcmEmpPerInfo;

/**
 * 
 * @author mrahim
 *
 */
public interface EmployeeProfileDAO {
    /**
     * Get the employee personal profile by User
     * 
     * @param username
     * @return
     */
    EhcmEmpPerInfo getEmployeeProfileByUser(String username);
}
