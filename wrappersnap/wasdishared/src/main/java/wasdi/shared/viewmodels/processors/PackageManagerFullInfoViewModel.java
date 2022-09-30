package wasdi.shared.viewmodels.processors;

import java.util.List;

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
public class PackageManagerFullInfoViewModel {

	private PackageManagerViewModel packageManager;
	private List<PackageViewModel> outdated;
	private List<PackageViewModel> uptodate;
	private List<PackageViewModel> all;

}
