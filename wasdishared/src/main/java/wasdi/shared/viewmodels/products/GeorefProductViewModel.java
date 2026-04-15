package wasdi.shared.viewmodels.products;

/**
 * Extension of the product View Model with also the bbox.
 * @author p.campanella
 *
 */
public class GeorefProductViewModel extends ProductViewModel {

	private String bbox;

	public GeorefProductViewModel() {
		super();
	}
	
	public GeorefProductViewModel(ProductViewModel base) {
		super(base);
	}	
	
	public String getBbox() {
		return bbox;
	}

	public void setBbox(String bbox) {
		this.bbox = bbox;
	}
	
}
