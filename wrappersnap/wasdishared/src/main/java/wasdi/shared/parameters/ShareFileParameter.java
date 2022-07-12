package wasdi.shared.parameters;

import lombok.Getter;
import lombok.Setter;

/**
 * Parameter of the SHARE operation.
 * @author PetruPetrescu
 *
 */
@Getter
@Setter
public class ShareFileParameter extends BaseParameter {

	private String productName;

	private String originWorkspaceId;
	private String originWorkspaceNode;
	private String originFilePath;

	private String boundingBox;

	private String destinationWorkspaceNode;
	private String destinationFilePath;

//	private String provider;

}
