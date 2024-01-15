package wasdi.shared.utils.jinja;

import java.util.Map;

import com.hubspot.jinjava.Jinjava;

import wasdi.shared.config.WasdiConfig;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.log.WasdiLog;

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
			
			WasdiLog.debugLog("JinjaTemplateRenderer.translate: sTemplate = " + sTemplateFile);
			WasdiLog.debugLog("JinjaTemplateRenderer.translate: JsonInput = " + sJsonInputs);
			
			Jinjava oJinjava = new Jinjava();
			String sTemplate = WasdiFileUtils.fileToText(sTemplateFile);
			Map<String, Object> aoVariables = JsonUtils.jsonToMapOfObjects(sJsonInputs);
			
			WasdiLog.infoLog("JinjaTemplateRenderer.translate: calling render");
			String sRendered = oJinjava.render(sTemplate, aoVariables);
			
			WasdiLog.infoLog("JinjaTemplateRenderer.translate: calling write file");
			
			WasdiFileUtils.writeFile(sRendered, sOutputFile);
			
			WasdiLog.infoLog("JinjaTemplateRenderer.translate: template rendered in = " + sOutputFile);
			
			// Bye bye
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("JinjaTemplateRenderer.translate: exception " + oEx.toString());
			return false;
		}
	}
}
