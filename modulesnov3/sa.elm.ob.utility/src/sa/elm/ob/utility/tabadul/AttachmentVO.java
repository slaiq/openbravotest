package sa.elm.ob.utility.tabadul;

import java.io.Serializable;

/**
 * 
 * @author mrahim
 *
 */
public class AttachmentVO implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -6291448347576734136L;
	
	private String fileName;
	private String filePath;
	private String fid;
	private String tabadulAttachmentId;
	private String cFileId;
	
	public String getcFileId() {
		return cFileId;
	}
	public void setcFileId(String cFileId) {
		this.cFileId = cFileId;
	}
	public String getTabadulAttachmentId() {
		return tabadulAttachmentId;
	}
	public void setTabadulAttachmentId(String tabadulAttachmentId) {
		this.tabadulAttachmentId = tabadulAttachmentId;
	}
	public String getFid() {
		return fid;
	}
	public void setFid(String fid) {
		this.fid = fid;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getFilePath() {
		return filePath;
	}
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	
	

}
