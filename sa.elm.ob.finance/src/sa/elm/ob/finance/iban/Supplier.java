package sa.elm.ob.finance.iban;

public class Supplier {
	private String name;
	private int crNumber;
	private String iban;
	private int bankId;
	private String bankKey;
	private String id;
	
	public Supplier() {}
	

	public Supplier(String name, int crNumber, String iban, int bankId) {
		super();
		this.name = name;
		this.crNumber = crNumber;
		this.iban = iban;
		this.bankId = bankId;
	}



	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getCrNumber() {
		return crNumber;
	}

	public void setCrNumber(int crNumber) {
		this.crNumber = crNumber;
	}

	public String getIban() {
		return iban;
	}

	public void setIban(String iban) {
		this.iban = iban;
	}
	
	public String getBankKey() {
		return bankKey;
	}

	public void setBankKey(String bankKey) {
		this.bankKey = bankKey;
	}


	public int getBankId() {
		return bankId;
	}

	public void setBankId(int bankId) {
		this.bankId = bankId;
	}


	public String getId() {
		return id;
	}


	public void setId(String id) {
		this.id = id;
	}
	
	
	
	
}
