package wasdi.shared.business.modis11a2;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class ModisItemForReading {
	
	private String sFileName;
	private long lFileSize;
	private String sDayNightFlag;
	
	private Double dStartDate;
	private Double dEndDate;

	private ModisLocation oBoundingBox;

	private String sInstrument; 
	private String sSensor;
	private String sLatitude;
	private String sPlatform;
	private String sUrl;

}
