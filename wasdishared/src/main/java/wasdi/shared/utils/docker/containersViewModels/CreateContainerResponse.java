package wasdi.shared.utils.docker.containersViewModels;

import java.util.ArrayList;

/**
 * Represents the JSON returned by Docker API after a successful 
 * call to the API to create a container
 * 
 * @author p.campanella
 *
 */
public class CreateContainerResponse {
	public String Id = "";
	public ArrayList<String> Warnings = new ArrayList<>();
}
