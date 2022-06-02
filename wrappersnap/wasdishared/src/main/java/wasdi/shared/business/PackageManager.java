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
public class PackageManager {

	private String sName;
	private String sVersion;
	private int iMajor;
	private int iMinor;
	private int iPatch;

}
