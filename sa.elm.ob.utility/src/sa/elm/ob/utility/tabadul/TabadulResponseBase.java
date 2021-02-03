package sa.elm.ob.utility.tabadul;

import java.io.Serializable;

public class TabadulResponseBase implements Serializable{
	
	private static final long serialVersionUID = 1L;
	private String status;
	private String tid;
	private String fid;
	private String aid;
	private TabadulReason reason;
	
	public TabadulReason getReason() {
		return reason;
	}
	public void setReason(TabadulReason reason) {
		this.reason = reason;
	}
	public String getFid() {
		return fid;
	}
	public void setFid(String fid) {
		this.fid = fid;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getTid() {
		return tid;
	}
	public void setTid(String tid) {
		this.tid = tid;
	}
	public String getAid() {
		return aid;
	}
	public void setAid(String aid) {
		this.aid = aid;
	}

}
