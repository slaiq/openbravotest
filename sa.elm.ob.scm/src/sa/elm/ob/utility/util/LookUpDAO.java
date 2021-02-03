package sa.elm.ob.utility.util;

import java.util.List;

import sa.elm.ob.scm.ESCMDefLookupsType;
import sa.elm.ob.scm.ESCMDefLookupsTypeLn;

/**
 * DAO for Look Ups
 * @author mrahim
 *
 */
/**
 * DAO for Look Ups
 * @author mrahim
 *
 */
public interface LookUpDAO {
	/**
	 * Get all lookup types
	 * @return
	 */
	List <ESCMDefLookupsType> getAllLookUpTypes ();
	
	/**
	 * Get the lookup by property
	 * @param propertyName
	 * @param value
	 * @return
	 */
	List<ESCMDefLookupsType> getLookUpByProperty (String propertyName, Object value);
	
	/**
	 * Get Lookup lines by property
	 * @param lookUpTypeId
	 * @param propertyName
	 * @param value
	 * @return
	 */
	List <ESCMDefLookupsTypeLn> getLookUpLinesByProperty (String lookUpTypeId, String propertyName, Object value);
}
