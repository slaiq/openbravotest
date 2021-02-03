package sa.elm.ob.scm.webservice.epm.dto;

import java.math.BigDecimal;

public class AddExtractProjectRequestDto {

	private Integer extractValue;
	private String extractValueDate;
	private BigDecimal exchangeValue;
	private String exchangeValueDate;	
	private String gRPNumber;

	public AddExtractProjectRequestDto() {

	}

	public Integer getExtractValue() {
		return extractValue;
	}

	public void setExtractValue(Integer extractValue) {
		this.extractValue = extractValue;
	}

	public String getExtractValueDate() {
		return extractValueDate;
	}

	public void setExtractValueDate(String extractValueDate) {
		this.extractValueDate = extractValueDate;
	}

	public BigDecimal getExchangeValue() {
		return exchangeValue;
	}

	public void setExchangeValue(BigDecimal exchangeValue) {
		this.exchangeValue = exchangeValue;
	}

	public String getExchangeValueDate() {
		return exchangeValueDate;
	}

	public void setExchangeValueDate(String exchangeValueDate) {
		this.exchangeValueDate = exchangeValueDate;
	}

	public String getgRPNumber() {
		return gRPNumber;
	}

	public void setgRPNumber(String gRPNumber) {
		this.gRPNumber = gRPNumber;
	}

	
}
