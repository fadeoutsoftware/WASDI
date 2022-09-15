package wasdi.shared.business;

import lombok.Getter;
import lombok.Setter;

/**
 * JupyterNotebook Entity
 * Represents a JupyterNotebook associated to a specific user and to a specific workspace
 * 
 * @author PetruPetrescu
 *
 */
@Getter
@Setter
public class JupyterNotebook {

	private String code;
	private String userId;
	private String workspaceId;
	private String url;

}
