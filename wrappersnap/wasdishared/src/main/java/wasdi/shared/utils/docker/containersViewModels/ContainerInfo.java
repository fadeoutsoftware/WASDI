package wasdi.shared.utils.docker.containersViewModels;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents some info of a Container as a subset of the info returned by Docker API
 * @author p.campanella
 *
 */
public class ContainerInfo {
	public String Id;
	public String Image;
	public String ImageId;
	public String State;
	public String Status;
	public List<String> Names = new ArrayList<>(); 
}
