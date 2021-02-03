package sa.elm.ob.scm.ad_process.BidManagement;

/**
 * This represents the Bid Statuses
 * @author mrahim
 *
 */
public enum BidStatusE {

   INACTIVE ("IA"), DRAFT ("DR") , INPROGRESS ("IP") , CANCELLED ("CL"), ACTIVE ("ACT") , POSTPONED ("PP") ,
   WITHDRAWN ("WD") , CLOSED ("CD") , RESUBMITTED ("RES"), EXTEND ("EXT"); 
	
	private String status;
	
	private BidStatusE(String status) {
		this.status = status;
	}

	public String getStatus() {
		return status;
	}


	public void setStatus(String status) {
		this.status = status;
	}

}
