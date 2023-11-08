package wasdi.shared.utils.docker.containersViewModels;

import wasdi.shared.utils.docker.containersViewModels.constants.ConsistencyTypes;
import wasdi.shared.utils.log.WasdiLog;

/**
 * Object Representation of the Mount Volume sub payload to send to the Docker API 
 * to create a new container.
 * 
 * Given some "strange" features it is serialized in JSON by a custom object.
 * 
 * Refer to this documentation:
 * https://docs.docker.com/engine/api/v1.43/#tag/Container/operation/ContainerCreate
 */
public class MountVolumeParam {
	public String Target;
	public String Source;
	public String Type;
	public String Consistency = ConsistencyTypes.DEFAULT;
	public boolean ReadOnly=false;
	
	public String toJson() {
		String sReturn = "{\n";
		
		try {
			
			boolean bAdded = false;
			
			if (Target != null) {
				sReturn += "\"Target\": \"" + Target.replace("\\", "\\\\") + "\"";
				bAdded = true;
			}
			
			if (Source != null) {
				if (bAdded) sReturn += ",\n";
				sReturn += "\"Source\": \"" + Source.replace("\\", "\\\\") + "\"";
				bAdded = true;
			}
			
			if (Type != null) {
				if (bAdded) sReturn += ",\n";
				sReturn += "\"Type\": \"" + Type + "\"";
				bAdded = true;
			}
			
			if (Consistency != null) {
				if (bAdded) sReturn += ",\n";
				sReturn += "\"Consistency\": \"" + Consistency + "\"";
				bAdded = true;
			}			
			
			if (bAdded) sReturn += ",\n";
			
			sReturn += "\"ReadOnly\": " + ReadOnly + "\n";
			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ContainerMountVolumeParam.toJson: Exception Converting ContainerCreateParams to JSON" ,  oEx);
			return null;
		}
		
		sReturn += "}";
		
		return sReturn;		
	}
}
