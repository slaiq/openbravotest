package sa.elm.ob.utility.util;

import java.util.Date;

/**
 * 
 * @author mrahim
 *
 */
public class LookUpLineVO extends LookUpVO{

	/**
	 * 
	 */
	private static final long serialVersionUID = -451044436346893122L;
	
	private Date creationDate;
	private String lookUpType;
	private String searchKey;
	private String lineNo;
	private String commercialName;
	private String datatype;
	private String minvalue;
	private String maxvalue;
	
	
	public Date getCreationDate() {
		return creationDate;
	}
	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}
	public String getLookUpType() {
		return lookUpType;
	}
	public void setLookUpType(String lookUpType) {
		this.lookUpType = lookUpType;
	}
	public String getSearchKey() {
		return searchKey;
	}
	public void setSearchKey(String searchKey) {
		this.searchKey = searchKey;
	}
	public String getLineNo() {
		return lineNo;
	}
	public void setLineNo(String lineNo) {
		this.lineNo = lineNo;
	}
	public String getCommercialName() {
		return commercialName;
	}
	public void setCommercialName(String commercialName) {
		this.commercialName = commercialName;
	}
	public String getDatatype() {
		return datatype;
	}
	public void setDatatype(String datatype) {
		this.datatype = datatype;
	}
	public String getMinvalue() {
		return minvalue;
	}
	public void setMinvalue(String minvalue) {
		this.minvalue = minvalue;
	}
	public String getMaxvalue() {
		return maxvalue;
	}
	public void setMaxvalue(String maxvalue) {
		this.maxvalue = maxvalue;
	}
	
	

}
