package sa.elm.ob.hcm.selfservice.dto.lookup;

import java.io.Serializable;
/**
 * DTO for holding lookupValues
 * @author mrahim
 *
 */
public class LookUpDTO implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 6132713761434061156L;
	private String id;
	private String descriptionEn;
	private String descriptionAr;
	
	public LookUpDTO(String id, String descriptionEn, String descriptionAr) {
		super();
		this.id = id;
		this.descriptionEn = descriptionEn;
		this.descriptionAr = descriptionAr;
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDescriptionEn() {
		return descriptionEn;
	}
	public void setDescriptionEn(String descriptionEn) {
		this.descriptionEn = descriptionEn;
	}
	public String getDescriptionAr() {
		return descriptionAr;
	}
	public void setDescriptionAr(String descriptionAr) {
		this.descriptionAr = descriptionAr;
	}
	
	
	

}
