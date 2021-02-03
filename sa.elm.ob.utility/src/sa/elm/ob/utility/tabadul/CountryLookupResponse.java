package sa.elm.ob.utility.tabadul;

public class CountryLookupResponse extends TabadulResponseBase {
	
	private static final long serialVersionUID = 1L;
	
	private CountriesVO countries;

	public CountriesVO getCountries() {
		return countries;
	}

	public void setCountries(CountriesVO countries) {
		this.countries = countries;
	}

}
