package sa.elm.ob.utility.util;

import java.util.List;

/**
 * Generic Interface for all lookups
 * @author mrahim
 *
 */
public interface LookUpService {
	
	/**
	 * Get all LookUpTypes
	 * @return
	 */
	List <LookUpVO> getAllLookUpTypes () throws Exception;
	/**
	 * Get the LookUp By ID
	 * @param id
	 * @return
	 */
	List<LookUpVO> getLookUpByProperty (String propertyName, Object value) throws Exception;
	
	/**
	 * Get the LookUpLines By property
	 * @param lookUpType
	 * @param propertyName
	 * @param value
	 * @return
	 */
	List <LookUpLineVO> getLookUpLinesByProperty (String lookUpType, String propertyName, Object value) throws Exception;
	
	void setLookUpDAO (LookUpDAO lookUpDAO);

}
