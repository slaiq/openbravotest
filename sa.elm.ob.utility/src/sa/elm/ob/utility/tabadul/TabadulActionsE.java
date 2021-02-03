package sa.elm.ob.utility.tabadul;
/**
 * Represents the tabadul api actions
 * @author mrahim
 *
 */
public enum TabadulActionsE {
	
	CREATE_TENDER ("411C5485D15745B999E8E072A5D22FE0") ,
	UPDATE_TENDER ("6E02626679AA4BC59533342874A07305"),
	PUBLISH_TENDER ("A261CEDC4746450992113E166C8068C6"),
	DELETE_TENDER ("E9DED1D465084D2BAA63D1623B563445"),
    CANCEL_TENDER ("A002C3F2E7D34DF29A8ADC5139A5390A") , 
    UPLOAD_TENDER_FILE ("6B308041FD82446393D63C0320DB2D6A"), 
    CANCEL_TENDER_FILE ("799165F25DB64731B91F8AC11AB66BE9")
    ,PUBLISH_TENDER_FILE ("E0572D9C2DDB4243A4714386BEA76127"),
    DELETE_TENDER_FILE ("9536EFFF39E54A8688985AC5E53A3007"); 
	
	private String actionKey;
	
	
	private TabadulActionsE(String actionKey) {
		this.actionKey = actionKey;

	}

	public String getActionKey() {
		return actionKey;
	}

	public void setActionKey(String actionKey) {
		this.actionKey = actionKey;
	}

}
