package wasdi.shared.viewmodels.products;

import java.util.Date;

public class ProductInfoViewModel {
	
	Date date;
	String 	originalname,
			name,
			polarization,
			relOrbit,
			provider;
	
	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}
	public String getOriginalname() {
		return originalname;
	}
	public void setOriginalname(String originalname) {
		this.originalname = originalname;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getPolarization() {
		return polarization;
	}
	public void setPolarization(String polarization) {
		this.polarization = polarization;
	}
	public String getRelOrbit() {
		return relOrbit;
	}
	public void setRelOrbit(String relOrbit) {
		this.relOrbit = relOrbit;
	}
	public String getProvider() {
		return provider;
	}
	public void setProvider(String provider) {
		this.provider = provider;
	}
	
}
