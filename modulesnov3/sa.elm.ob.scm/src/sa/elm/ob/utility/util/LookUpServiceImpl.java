package sa.elm.ob.utility.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.beanutils.BeanUtils;

import sa.elm.ob.scm.ESCMDefLookupsType;
import sa.elm.ob.scm.ESCMDefLookupsTypeLn;

public class LookUpServiceImpl implements LookUpService {

	private LookUpDAO lookUpDAO;
	
	@Override
	public List<LookUpVO> getAllLookUpTypes() throws Exception {
		List <LookUpVO> lookUpVOsList = new ArrayList<LookUpVO>();
		List <ESCMDefLookupsType> lookupsTypes = getLookUpDAO().getAllLookUpTypes();
		// Loop and convert all to VO's
		for (ESCMDefLookupsType lookupsType : lookupsTypes){
				lookUpVOsList.add(convertDomainToVO(lookupsType));		
		}
		
		return lookUpVOsList;
	}

	@Override
	public List<LookUpVO> getLookUpByProperty(String propertyName, Object value) throws Exception {
		List <LookUpVO> lookUpVOsList = new ArrayList<LookUpVO>();
		List <ESCMDefLookupsType> lookupsTypes = getLookUpDAO().getLookUpByProperty(propertyName, value);
		
		// Loop and convert all to VO's
		for (ESCMDefLookupsType lookupsType : lookupsTypes) {
			lookUpVOsList.add(convertDomainToVO(lookupsType));
		}

		return lookUpVOsList;
	}

	@Override
	public List<LookUpLineVO> getLookUpLinesByProperty(String lookUpType,String propertyName, Object value) throws Exception {
		List <LookUpLineVO> lineList = new ArrayList<LookUpLineVO>();
		LookUpLineVO lookUpLineVO = null;
		List <ESCMDefLookupsTypeLn> lookupsTypesLnList = getLookUpDAO().getLookUpLinesByProperty(lookUpType, propertyName, value);
		// Domain Object to Vo Conversion
		for (ESCMDefLookupsTypeLn lookupsTypeLn : lookupsTypesLnList){
			lookUpLineVO = new LookUpLineVO();
			BeanUtils.copyProperties(lookUpLineVO, lookupsTypeLn);
			lineList.add(lookUpLineVO);
		}
		return lineList;
	}
	
	/**
	 * Converts the Domain to VO's
	 * @param esDefLookupsType
	 * @return
	 * @throws Exception
	 */
	private LookUpVO convertDomainToVO (ESCMDefLookupsType esDefLookupsType) throws Exception {
		LookUpLineVO lookUpLineVO = null;
		List <LookUpLineVO> linesVOList = new ArrayList<LookUpLineVO>();
		LookUpVO lookUpVO = new LookUpVO();
		BeanUtils.copyProperties(lookUpVO, esDefLookupsType);
		List <ESCMDefLookupsTypeLn> lines = esDefLookupsType.getESCMDefLookupsTypeLnList();
		
		// Now get the lines and convert the Domain to VO
		for (ESCMDefLookupsTypeLn escmLn : lines){
			lookUpLineVO = new LookUpLineVO();
			BeanUtils.copyProperties(lookUpLineVO, escmLn);
			linesVOList.add(lookUpLineVO);
		}
		
		lookUpVO.setLines(linesVOList);
		
		return lookUpVO;
	}

	public LookUpDAO getLookUpDAO() {
		return lookUpDAO;
	}

	public void setLookUpDAO(LookUpDAO lookUpDAO) {
		this.lookUpDAO = lookUpDAO;
	}
}
