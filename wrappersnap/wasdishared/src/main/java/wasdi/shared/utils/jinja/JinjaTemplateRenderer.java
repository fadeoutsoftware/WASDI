package wasdi.shared.utils.jinja;

import java.util.Map;

import wasdi.shared.config.WasdiConfig;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.runtime.RunTimeUtils;

/**
 * Utility class to translate a Jinja Template in a valorized file
 * @author p.campanella
 *
 */
public class JinjaTemplateRenderer {

	
	/**
	 * Apply the values of a JSON file to a Jinja template.
	 * @param sTemplateFile Full Path of the template file
	 * @param sOutputFile Full Path of the output file
	 * @param aoParams Map<String, Object> with the parameters to use to render the template
	 * @return
	 */
	public boolean translate(String sTemplateFile, String sOutputFile, Map<String, Object> aoParams) {
		String sJsonInputs = JsonUtils.stringify(aoParams);
		return translate(sTemplateFile,sOutputFile, sJsonInputs, false);
	}
	
	/**
	 * Apply the values of a JSON file to a Jinja template.
	 * @param sTemplateFile Full Path of the template file
	 * @param sOutputFile Full Path of the output file
	 * @param sJsonInputs Strin representing the json used to valorize parameters
	 * @return true if the operation finish without exceptions, false otherwise. Indeed true does not guarantee that the template had been valorized in the right way
	 */
	public boolean translate(String sTemplateFile, String sOutputFile, String sJsonInputs) {
		return translate(sTemplateFile,sOutputFile, sJsonInputs, false);
	}
	
	/**
	 * Apply the values of a JSON file to a Jinja template.
	 * @param sTemplateFile Full Path of the template file
	 * @param sOutputFile Full Path of the output file
	 * @param sJsonInputs Strin representing the json used to valorize parameters
	 * @param bStrict true to force to have all the variables in the json. False if "tolerant"
	 * @return true if the operation finish without exceptions, false otherwise. Indeed true does not guarantee that the template had been valorized in the right way
	 */
	public boolean translate(String sTemplateFile, String sOutputFile, String sJsonInputs, boolean bStrict) {
		
		try {
			// Create the command
			String sRenderCommand = buildRenderCommand(sTemplateFile, sOutputFile, sJsonInputs, bStrict);
			
			// Utility to execute the script
			boolean bRet = RunTimeUtils.runCommand(WasdiConfig.Current.paths.wasdiTempFolder, sRenderCommand);
			
			// Bye bye
			return bRet;			
		}
		catch (Exception oEx) {
			Utils.debugLog("JinjaTemplateRenderer.translate: exception " + oEx.toString());
		}
		
		return false;
	}
	
	/**
	 * Creates the render command
	 * 
	 * @param sTemplateFile Full template file path
	 * @param sOutputFile Full output file path
	 * @param sJsonInputs String representing a json that represents the paramters
	 * @param bStrict True to apply strict flag
	 * @return the full command line to execute
	 */
	protected String buildRenderCommand(String sTemplateFile, String sOutputFile, String sJsonInputs, boolean bStrict) {
		
		String sLineSeparator = System.getProperty("line.separator");
		
		StringBuilder oSB = new StringBuilder();
		
		oSB.append(WasdiConfig.Current.paths.pythonExecPath + "  " +  WasdiConfig.Current.paths.jinjaTemplateRenderTool + " \\");
		oSB.append(sLineSeparator);
		oSB.append("  --template " + sTemplateFile + " \\");
		oSB.append(sLineSeparator);
		oSB.append("  --rendered-file " + sOutputFile + " \\");
		oSB.append(sLineSeparator);
		oSB.append("  --json-inline '" + sJsonInputs + "'");
		
		if (bStrict) {
			oSB.append(" \\");
			oSB.append(sLineSeparator);
			oSB.append("  --strict");			
		}

		return oSB.toString();
	}
	
	
}
