package sa.elm.ob.utility.util.datamigration;

public class ImportPrevMaintGuarantee extends ImportBankGuarantee {

	private static final String DATA_SHEET_NAME = "Data-PrevMain";
	private static final String BG_TYPE = "PMG";
	private static final String DOCUMENT_TYPE = "POC";
	private static final String BG_RATE = "5";

	@Override
	protected String getRate() {
		return BG_RATE;
	}

	@Override
	protected String getDocumentType() {
		return DOCUMENT_TYPE;
	}

	@Override
	protected String getType() {
		return BG_TYPE;
	}

	@Override
	protected String getSheetName() {
		return DATA_SHEET_NAME;
	}
}
