package wasdi.shared.viewmodels;

/**
 * Created by s.adamo on 20/05/2016.
 */
public class BandViewModel {

    public BandViewModel() {

    }
    public BandViewModel(String sBandName)
    {
        this.name = sBandName;
    }

    private String name;
    
    private Boolean published = false;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
	public Boolean getPublished() {
		return published;
	}
	public void setPublished(Boolean published) {
		this.published = published;
	}
}
