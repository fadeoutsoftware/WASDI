package wasdi.shared.business.ecostress;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class EcoStressItemForWriting {

	private String fileName;

	private int startOrbitNumber;
	private int stopOrbitNumber;

	private String dayNightFlag;

	private Double beginningDate;
	private Double endingDate;

	private String location;

	private String platform;
	private String instrument;
	private String sensor;
	private String parameterName;

	private String s3Path;
	private String url;
	
}
