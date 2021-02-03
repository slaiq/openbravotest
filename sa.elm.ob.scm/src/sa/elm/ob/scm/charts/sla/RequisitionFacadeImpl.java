package sa.elm.ob.scm.charts.sla;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import org.apache.log4j.Logger;
import org.openbravo.model.procurement.Requisition;

import sa.elm.ob.utility.util.LookUpDAOImpl;
import sa.elm.ob.utility.util.LookUpLineVO;
import sa.elm.ob.utility.util.LookUpServiceImpl;
import sa.elm.ob.utility.util.LookUpVO;

public class RequisitionFacadeImpl implements RequisitionFacade {

	private static final String REFERENCE_PROPERTY_VALUE = "ISLA";

	private static final String REFERENCE_PROPERTY_NAME = "reference";

	private static final int DEFAULT_SLA_VALUE = 3;

	private static final Logger logger = Logger.getLogger(RequisitionFacadeImpl.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see sa.elm.ob.finance.widget.RequisitionFacadeInterface#getSlaViolation()
	 */
	@Override
	public Hashtable<String, Integer> getSlaViolation() {

		Hashtable<String, Integer> violationMap = new Hashtable<String, Integer>();

		RequisitionRepositoryImpl repository = new RequisitionRepositoryImpl();

		List<Requisition> activeRequisition = repository.getActiveRequisition();

		for (Requisition requisition : activeRequisition) {
			Date stageDate = requisition.getEscmStageDate();
			if (violateSla(stageDate, requisition.getEscmStage())) {
				incrementSlaViolation(violationMap, requisition.getEscmStage());
			}
		}
		return violationMap;
	}

	private void incrementSlaViolation(Hashtable<String, Integer> violationMap, String prStag) {

		if (violationMap.containsKey(prStag)) {
			violationMap.put(prStag, violationMap.get(prStag) + 1);
		} else {
			violationMap.put(prStag, 1);
		}

	}

	private boolean violateSla(Date stagDate, String prStag) {

		int slaValue = getSLAValue(prStag);

		LocalDate stagLocalDate = toLocalDate(stagDate);

		LocalDate dueDate = stagLocalDate.plusDays(slaValue).plusDays(countWeekendDays(stagLocalDate, slaValue));

		if (LocalDate.now().isAfter(dueDate)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Convert from Date to LocalDate
	 * 
	 * @param stagDate
	 *            : date
	 * @return LocalDate
	 */
	private LocalDate toLocalDate(Date stagDate) {
		ZoneId defaultZoneId = ZoneId.systemDefault();
		return stagDate.toInstant().atZone(defaultZoneId).toLocalDate();
	}

	/**
	 * Retrieve SLA value from reference lookup table.
	 * 
	 * @param prStag
	 * @return
	 */
	private int getSLAValue(String prStag) {

		int slaValue = DEFAULT_SLA_VALUE;
		LookUpServiceImpl lookUpService = new LookUpServiceImpl();
		try {
			lookUpService.setLookUpDAO(new LookUpDAOImpl());
			List<LookUpVO> refernceLookuplist = lookUpService.getLookUpByProperty(REFERENCE_PROPERTY_NAME,
					REFERENCE_PROPERTY_VALUE);
			if (refernceLookuplist.size() > 0) {
				LookUpVO referenceLookup = refernceLookuplist.get(0);
				List<LookUpLineVO> lookuplines = referenceLookup.getLines();
				for (LookUpLineVO lookUpLine : lookuplines) {
					if (lookUpLine.getSearchKey() != null && lookUpLine.getSearchKey().equalsIgnoreCase(prStag)) {
						slaValue = Integer.parseInt(lookUpLine.getCommercialName());
					}
				}
			}
		} catch (Exception e) {
			logger.error("Error while getting SLA value, default value will be used.");
			e.printStackTrace();
		}
		logger.error("SLA Stag|Value :" + prStag + "|" + slaValue);
		return slaValue;
	}


	private long countWeekendDays(LocalDate stagDate, int slaValue) {
		long count = 0L;
		for (int i = 0; i < slaValue; i++) {
			LocalDate date = stagDate.plusDays(i);
			if (isWeekendDay(date)) {
				count++;
			}
		}
		return count;
	}

	private boolean isWeekendDay(LocalDate date) {
		DayOfWeek dayOfWeek = date.getDayOfWeek();
		return dayOfWeek.equals(DayOfWeek.FRIDAY) || dayOfWeek.equals(DayOfWeek.SATURDAY);
	}

}
