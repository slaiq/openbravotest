package sa.elm.ob.utility.tabadul;

import java.io.Serializable;

/**
 * Represents the Tabadul Reason for Error
 * @author mrahim
 *
 */
public class TabadulReason implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -2433281233447138101L;
	
	private String id;
	private String message;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	
	

}
