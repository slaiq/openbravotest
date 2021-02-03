package sa.elm.ob.utility.tabadul;

import java.io.Serializable;

/**
 * Represents the success / failure Message
 * @author mrahim
 *
 */
public class MessageVO implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 309153169826906353L;
	private Integer tenderId;
	private Integer totalFiles;
	private Integer totalFilesUploaded;
	private Boolean isError;
	private String errorMessageKey;
	
	public MessageVO () {
		isError = false ;
	}
	
	public String getErrorMessageKey() {
		return errorMessageKey;
	}
	public void setErrorMessageKey(String errorMessageKey) {
		this.errorMessageKey = errorMessageKey;
	}
	public Boolean getIsError() {
		return isError;
	}
	public void setIsError(Boolean isError) {
		this.isError = isError;
	}
	public Integer getTenderId() {
		return tenderId;
	}
	public void setTenderId(Integer tenderId) {
		this.tenderId = tenderId;
	}
	public Integer getTotalFiles() {
		return totalFiles;
	}
	public void setTotalFiles(Integer totalFiles) {
		this.totalFiles = totalFiles;
	}
	public Integer getTotalFilesUploaded() {
		return totalFilesUploaded;
	}
	public void setTotalFilesUploaded(Integer totalFilesUploaded) {
		this.totalFilesUploaded = totalFilesUploaded;
	}
}
