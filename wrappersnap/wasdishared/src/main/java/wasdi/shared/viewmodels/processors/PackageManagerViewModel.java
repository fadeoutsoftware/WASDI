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
public class PackageManagerViewModel {

	private String name;
	private String version;
	private int major;
	private int minor;
	private int patch;

}