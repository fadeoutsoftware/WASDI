package wasdi.shared.utils.docker.containersViewModels;

import java.util.ArrayList;
import java.util.HashMap;

import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * Object Representation of the payload to send to the Docker API 
 * to create a new container.
 * 
 * Given some "strange" features it is serialized in JSON by a custom method.
 * 
 * Refer to this documentation:
 * https://docs.docker.com/engine/api/v1.43/#tag/Container/operation/ContainerCreate
 * 
 * @author p.campanella
 *
 */
public class CreateParams {
	public String Hostname = "";
	public String Domainname = "";
	public String User = "";
	public boolean AttachStdin = false;
	public boolean AttachStdout = true;
	public boolean AttachStderr = true;
	public boolean Tty = true;
	public boolean OpenStdin = false;
	public boolean StdinOnce = false;
	public ArrayList<String> Env = new ArrayList<>();
	public ArrayList<String> Cmd = new ArrayList<>();
	public String Entrypoint;
	public String OnBuild;
	public String Image;
	public String WorkingDir="";
	public boolean NetworkDisabled = false;
	public String MacAddress;	
	public HashMap<String, String> Labels = new HashMap<>(); 
	public ArrayList<String> Volumes = new ArrayList<>();
	public ArrayList<String> ExposedPorts = new ArrayList<>();
	public HostConfigParam HostConfig = new HostConfigParam();
	
	public String toJson() {
		String sReturn = "{\n";
		
		try {
			
			boolean bAdded = false;
			
			if (Hostname != null) {
				sReturn += "\"Hostname\": \"" + Hostname + "\"";
				bAdded = true;
			}
			
			if (Domainname != null) {
				if (bAdded) sReturn += ",\n";
				sReturn += "\"Domainname\": \"" + Domainname + "\"";
				bAdded = true;
			}
			
			if (User != null) {
				if (bAdded) sReturn += ",\n";
				sReturn += "\"User\": \"" + User + "\"";
				bAdded = true;
			}
			
			if (bAdded) sReturn += ",\n";
			
			sReturn += "\"AttachStdin\": " + AttachStdin + ",\n";
			sReturn += "\"AttachStdout\": " + AttachStdout + ",\n";
			sReturn += "\"AttachStderr\": " + AttachStderr + ",\n";
			sReturn += "\"Tty\": " + Tty + ",\n";
			sReturn += "\"OpenStdin\": " + OpenStdin + ",\n";
			sReturn += "\"StdinOnce\": " + StdinOnce + ",\n";
			bAdded = false;
			
			if (Entrypoint != null) {
				if (bAdded) sReturn += ",\n";
				sReturn += "\"Entrypoint\": \"" + Entrypoint + "\"";
				bAdded = true;
			}
			else {
				if (bAdded) sReturn += ",\n";
				sReturn += "\"Entrypoint\": null";
				bAdded = true;				
			}
			
			if (OnBuild != null) {
				if (bAdded) sReturn += ",\n";
				sReturn += "\"OnBuild\": \"" + OnBuild + "\"";
				bAdded = true;
			}
			else {
				if (bAdded) sReturn += ",\n";
				sReturn += "\"OnBuild\": null";
				bAdded = true;				
			}			

			if (Image != null) {
				if (bAdded) sReturn += ",\n";
				sReturn += "\"Image\": \"" + Image + "\"";
				bAdded = true;
			}

			if (WorkingDir != null) {
				if (bAdded) sReturn += ",\n";
				sReturn += "\"WorkingDir\": \"" + WorkingDir + "\"";
				bAdded = true;
			}
						
			if (MacAddress != null) {
				if (bAdded) sReturn += ",\n";
				sReturn += "\"MacAddress\": \"" + MacAddress + "\"";
				bAdded = true;
			}
			
			if (bAdded) sReturn += ",\n";
			
			if (Env.size()>0) {
				sReturn += "\"Env\": [ ";
				
				for (int iEnvs = 0; iEnvs<Env.size(); iEnvs++) {
					String sEnv = Env.get(iEnvs);
					sReturn += "\"" + sEnv + "\"";
					if (iEnvs<Env.size()-1) sReturn +=",\n";
				}
				sReturn += "],\n";				
			}
			else {
				sReturn += "\"Env\": null,\n";
			}
			
			if (Cmd.size()>0) {
				sReturn += "\"Cmd\": [ ";
				
				for (int iCmds = 0; iCmds<Cmd.size(); iCmds++) {
					String sCmd = Cmd.get(iCmds);
					sReturn += "\"" + sCmd.replace("\"", "\\\"") + "\"";
					if (iCmds<Cmd.size()-1) sReturn +=",\n";
				}
				sReturn += "],\n";				
			}
			else {
				sReturn += "\"Cmd\": null,\n";
			}
			
			sReturn += "\"Labels\": {";
			
			if (Labels.size()>0) {
				for (String sKey : Labels.keySet()) {
					sReturn += "\"" +sKey +"\": \"" + Labels.get(sKey) + "\",";
				}				
				
				sReturn = sReturn.substring(0, sReturn.length()-1);
			}
			
			sReturn += "\n},\n";
			
			sReturn += "\"Volumes\": {\n";
			
			if (Volumes.size()>0) {
				for (String sVolume : Volumes) {
					sReturn += "\"" +sVolume +"\": {},";
				}
				sReturn = sReturn.substring(0, sReturn.length()-1);
			}
			
			sReturn += "\n},\n";
			
			sReturn += "\"ExposedPorts\": {\n";
			
			if (ExposedPorts.size()>0) {
				for (String sExposedPort : ExposedPorts) {
					sReturn += "\"" +sExposedPort +"\": {},";
				}
				sReturn = sReturn.substring(0, sReturn.length()-1);
			}
			
			sReturn += "\n},\n";
			
			String sHostConfig = HostConfig.toJson();
			
			if (Utils.isNullOrEmpty(sHostConfig) == false) {
				sReturn += "\"HostConfig\": " + sHostConfig;
			}
			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ContainerCreateParams.toJson: Exception Converting ContainerCreateParams to JSON" ,  oEx);
			return null;
		}
		
		sReturn += "}";
		
		return sReturn;
	}
}
