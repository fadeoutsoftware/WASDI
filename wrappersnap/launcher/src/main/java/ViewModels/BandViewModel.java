package ViewModels;

/**
 * Created by s.adamo on 20/05/2016.
 */
public class BandViewModel {

    public BandViewModel(String sBandName)
    {
        this.name = sBandName;
    }

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
