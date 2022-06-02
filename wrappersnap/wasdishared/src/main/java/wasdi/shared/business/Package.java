package wasdi.shared.business;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Package {

	private String sManagerName;
	private String sPackageName;
	private String sCurrentVersion;
	private String sLatestVersion;
	private String sType;

}
