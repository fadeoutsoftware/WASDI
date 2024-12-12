package wasdi.shared.viewmodels.search;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import wasdi.shared.utils.Utils;

/**
 * Result of a search to a data provider.
 * 
 * Basic info are:
 * 	.Title -> Name of the file
 * 	.Summary -> Description. Supports a sort of std like: "Date: 2021-12-25T18:25:03.242Z, Instrument: SAR, Mode: IW, Satellite: S1A, Size: 0.95 GB" but is not mandatory
 * 	.Id -> Provider unique id
 * 	.Link -> Link to download the file
 * 	.Footprint -> Bounding box in WKT ie POLYGON ((-7.087445 31.109682, -4.389633 31.524973, -4.062707 29.77639, -6.712266 29.357685, -7.087445 31.109682))
 * 				   Note: for POLYGON the convention is LON LAT, LON LAT...
 * 	.Provider -> Provider used to get this info.
 * 
 * Properties is a dictionary filled with all the properties supported by the data provider.
 * Can be seen with the "info" button in the client.
 * 		Some Commonly used, and shown in the client, are:
 * 			."date": reference Date
 * 			."instrument": used instrument 
 * 			."sensoroperationalmode": sensing mode
 * 			."size": image size as string
 * 			."relativeOrbit": relative orbit of the acquisition
 * 			."relativeorbitnumber": same of above, used by the client
 * 			."platformname": Platform Name
 * 
 * The libs searchs for a property called relativeOrbit
 * 
 * @author p.campanella
 *
 */
@XmlRootElement
public class QueryResultViewModel {
	
	/**
	 * Encoded Image Preview
	 */
	protected String preview="";
	/**
	 * File Name
	 */
	protected String title="";
	/**
	 * Description. Supports a sort of std like: "Date: 2021-12-25T18:25:03.242Z, Instrument: SAR, Mode: IW, Satellite: S1A, Size: 0.95 GB" but is not mandatory
	 */
	protected String summary="";
	/**
	 * Provider Id
	 */
	protected String id="";
	/**
	 * Link (or equivalent) to access the file
	 */
	protected String link="";
	/**
	 * WKT Footprint
	 */
	protected String footprint="";
	/**
	 * Data Provider that found this item
	 */
	protected String provider="";
	/**
	 * Dictionary of additional properties	
	 */
	protected Map<String, String> properties = new HashMap<String, String>();
	/**
	 * If this is accessible in a Volume, here we have the name
	 */
	protected String volumeName="";
	/**
	 * If this is accessible in a Volume, here we have the path in the volume
	 */
	protected String volumePath="";
	
	
	@Override
	public boolean equals(Object arg0) {
		
		if (arg0 ==null) return false;
		
		if (arg0.getClass() == QueryResultViewModel.class) {
			QueryResultViewModel oCompare = (QueryResultViewModel) arg0;
			
			return (this.provider.equals(oCompare.getProvider()) && this.link.equals(oCompare.getLink()) && this.title.equals(oCompare.getTitle()));
		}
		
		return super.equals(arg0);
	}
	
	@Override
	public int hashCode() {
		
		String sProvider = "";
		String sLink = "";
		String sTitle = "";
		
		if (!Utils.isNullOrEmpty(provider)) sProvider = provider;
		if (!Utils.isNullOrEmpty(link)) sLink = link;
		if (!Utils.isNullOrEmpty(title)) sTitle = title;
		
		String sHashCode = sProvider+sLink+sTitle;
		return sHashCode.hashCode();
	}
	
	public String getPreview() {
		return preview;
	}
	public void setPreview(String preview) {
		this.preview = preview;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getSummary() {
		return summary;
	}
	public void setSummary(String summary) {
		this.summary = summary;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getLink() {
		return link;
	}
	public void setLink(String link) {
		this.link = link;
	}
	public String getFootprint() {
		return footprint;
	}
	public void setFootprint(String footprint) {
		this.footprint = footprint;
	}
	public Map<String, String> getProperties() {
		return properties;
	}
	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}
	public String getProvider() {
		return provider;
	}
	public void setProvider(String provider) {
		this.provider = provider;
	}

	public String getVolumeName() {
		return volumeName;
	}

	public void setVolumeName(String volumeName) {
		this.volumeName = volumeName;
	}

	public String getVolumePath() {
		return volumePath;
	}

	public void setVolumePath(String volumePath) {
		this.volumePath = volumePath;
	}	
}
