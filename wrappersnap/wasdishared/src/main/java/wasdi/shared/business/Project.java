package wasdi.shared.business;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Project {

	private String projectId;
	private String subscriptionId;
	private String name;
	private String description;

}
