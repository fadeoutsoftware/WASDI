package wasdi.shared.viewmodels.organizations;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectViewModel {

	private String projectId;
	private String subscriptionId;
	private String name;
	private String description;
	private boolean activeProject;

}
