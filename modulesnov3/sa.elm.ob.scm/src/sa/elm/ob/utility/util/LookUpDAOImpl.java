package sa.elm.ob.utility.util;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.service.OBDal;

import sa.elm.ob.scm.ESCMDefLookupsType;
import sa.elm.ob.scm.ESCMDefLookupsTypeLn;

public class LookUpDAOImpl implements LookUpDAO {

	@SuppressWarnings("unchecked")
	@Override
	public List<ESCMDefLookupsType> getAllLookUpTypes() {
		Query query = OBDal.getInstance().getSession().createQuery(" from ESCM_DefLookups_Type ");
		return query.list();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<ESCMDefLookupsType> getLookUpByProperty(String propertyName, Object value) {
		Session session = OBDal.getInstance().getSession();
		Criteria criteria = session.createCriteria(ESCMDefLookupsType.class);
		
		criteria.add(Restrictions.eq(propertyName, value));
		
		List <ESCMDefLookupsType> list = (List<ESCMDefLookupsType>)criteria.list();
		
		return list;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<ESCMDefLookupsTypeLn> getLookUpLinesByProperty(String lookUpTypeId, String propertyName, Object value) {
		
		Session session = OBDal.getInstance().getSession();
		Criteria criteria = session.createCriteria(ESCMDefLookupsTypeLn.class);
		
		criteria.add(Restrictions.eq(propertyName, value));
		
		List <ESCMDefLookupsTypeLn> list = (List<ESCMDefLookupsTypeLn>)criteria.list();
		return list;

	}

}
