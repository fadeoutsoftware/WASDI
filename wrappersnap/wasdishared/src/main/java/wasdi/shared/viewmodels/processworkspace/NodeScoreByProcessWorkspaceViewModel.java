package wasdi.shared.viewmodels.processworkspace;

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
public class NodeScoreByProcessWorkspaceViewModel {

	private String nodeCode;
	private Integer numberOfProcesses;

	private Double diskPercentageUsed;
	private Double diskPercentageAvailable;

	private Long diskAbsoluteTotal;
	private Long diskAbsoluteUsed;
	private Long diskAbsoluteAvailable;

	private String licenses;

	private String timestampAsString;

}