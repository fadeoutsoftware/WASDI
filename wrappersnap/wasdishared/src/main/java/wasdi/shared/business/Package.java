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

	private String managerName;
	private String packageName;
	private String currentVersion;
	private String latestVersion;
	private String type;

}
