package wasdi.shared.viewmodels.processors;

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
public class PackageViewModel {

	private String managerName;
	private String packageName;
	private String currentVersion;
	private String currentBuild;
	private String latestVersion;
	private String type;
	private String channel;

}
