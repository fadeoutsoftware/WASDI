package wasdi.shared.utils.cwl;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import wasdi.shared.utils.log.WasdiLog;

/**
 * Utility methods to read (and lightly rewrite) an OGC Best Practice for Earth Observation
 * Application Package (see https://docs.ogc.org/bp/20-089r1.html), i.e. a CWL document with a
 * "Workflow" class and one or more "CommandLineTool" classes.
 * 
 * The actual CWL execution semantics (steps wiring, scatter, expressions, subworkflows, ...) are
 * NOT reimplemented here: they are delegated to the real cwltool reference runner. This class only
 * covers what WASDI itself needs around that: locating/parsing the document, deriving the WASDI
 * parameter sample from the Workflow's own inputs, building the CWL job order for a run, and
 * rewriting a CommandLineTool's DockerRequirement when WASDI built the image itself.
 * 
 * @author p.campanella
 */
public class CwlApplicationPackageUtils {

	private CwlApplicationPackageUtils() {
		// private constructor to hide the public implicit one
	}

	/**
	 * Finds the (first) *.cwl file in a processor folder
	 * @param sProcessorFolder Folder of the processor
	 * @return File found or null
	 */
	public static File findCwlFile(String sProcessorFolder) {
		try {
			File oFolder = new File(sProcessorFolder);

			if (!oFolder.exists() || !oFolder.isDirectory()) {
				WasdiLog.errorLog("CwlApplicationPackageUtils.findCwlFile: folder does not exist " + sProcessorFolder);
				return null;
			}

			File[] aoFiles = oFolder.listFiles((oDir, sName) -> sName.toLowerCase().endsWith(".cwl"));

			if (aoFiles == null || aoFiles.length == 0) {
				WasdiLog.errorLog("CwlApplicationPackageUtils.findCwlFile: no cwl file found in " + sProcessorFolder);
				return null;
			}

			return aoFiles[0];
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("CwlApplicationPackageUtils.findCwlFile: exception", oEx);
			return null;
		}
	}

	/**
	 * Loads and parses a CWL document ($graph based, cwlVersion v1.x)
	 * @param oCwlFile CWL file
	 * @return Map representation of the document, or null in case of problems
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Object> loadCwlDocument(File oCwlFile) {
		try {
			if (oCwlFile == null || !oCwlFile.exists()) {
				WasdiLog.errorLog("CwlApplicationPackageUtils.loadCwlDocument: cwl file does not exist");
				return null;
			}

			ObjectMapper oYamlMapper = new ObjectMapper(new YAMLFactory());
			return oYamlMapper.readValue(oCwlFile, Map.class);
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("CwlApplicationPackageUtils.loadCwlDocument: exception parsing " + oCwlFile, oEx);
			return null;
		}
	}

	/**
	 * Writes a parsed (and possibly rewritten) YAML document - a CWL document or a CWL job order - to a file
	 * @param oDocument Map representation of the document
	 * @param oOutputFile Destination file
	 * @return true if written correctly
	 */
	public static boolean writeYamlDocument(Map<String, Object> oDocument, File oOutputFile) {
		try {
			ObjectMapper oYamlMapper = new ObjectMapper(new YAMLFactory());
			oYamlMapper.writeValue(oOutputFile, oDocument);
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("CwlApplicationPackageUtils.writeYamlDocument: exception writing " + oOutputFile, oEx);
			return false;
		}
	}

	/**
	 * Gets the "$graph" entry with class "Workflow" from a parsed CWL document
	 * @param oCwlDocument Parsed CWL document
	 * @return Workflow node as a Map, or null if not found
	 */
	public static Map<String, Object> getWorkflowNode(Map<String, Object> oCwlDocument) {
		return getGraphNodeByClass(oCwlDocument, "Workflow");
	}

	/**
	 * Gets the "$graph" entry with class "CommandLineTool" from a parsed CWL document
	 * @param oCwlDocument Parsed CWL document
	 * @return CommandLineTool node as a Map, or null if not found
	 */
	public static Map<String, Object> getCommandLineToolNode(Map<String, Object> oCwlDocument) {
		return getGraphNodeByClass(oCwlDocument, "CommandLineTool");
	}

	/**
	 * Gets every "$graph" entry with class "CommandLineTool" from a parsed CWL document (a
	 * multi-step Workflow can reference more than one CommandLineTool, each with its own image)
	 * @param oCwlDocument Parsed CWL document
	 * @return list of CommandLineTool nodes, possibly empty
	 */
	@SuppressWarnings("unchecked")
	public static List<Map<String, Object>> getAllCommandLineToolNodes(Map<String, Object> oCwlDocument) {

		List<Map<String, Object>> aoNodes = new ArrayList<>();

		if (oCwlDocument == null) return aoNodes;

		Object oGraph = oCwlDocument.get("$graph");

		if (!(oGraph instanceof List)) return aoNodes;

		for (Object oNode : (List<Object>) oGraph) {
			if (!(oNode instanceof Map)) continue;

			Map<String, Object> oNodeMap = (Map<String, Object>) oNode;

			if ("CommandLineTool".equals(oNodeMap.get("class"))) {
				aoNodes.add(oNodeMap);
			}
		}

		return aoNodes;
	}

	/**
	 * Gets the Workflow node's own "id", needed to invoke cwltool as "&lt;file&gt;#&lt;id&gt;"
	 * @param oWorkflow Workflow node
	 * @return the id, or an empty string if not found
	 */
	public static String getWorkflowId(Map<String, Object> oWorkflow) {
		if (oWorkflow == null) return "";
		Object oId = oWorkflow.get("id");
		return oId == null ? "" : oId.toString();
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> getGraphNodeByClass(Map<String, Object> oCwlDocument, String sClass) {
		try {
			if (oCwlDocument == null) return null;

			Object oGraph = oCwlDocument.get("$graph");

			if (!(oGraph instanceof List)) {
				WasdiLog.errorLog("CwlApplicationPackageUtils.getGraphNodeByClass: $graph entry not found or not a list");
				return null;
			}

			for (Object oNode : (List<Object>) oGraph) {
				if (!(oNode instanceof Map)) continue;

				Map<String, Object> oNodeMap = (Map<String, Object>) oNode;

				if (sClass.equals(oNodeMap.get("class"))) {
					return oNodeMap;
				}
			}

			return null;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("CwlApplicationPackageUtils.getGraphNodeByClass: exception", oEx);
			return null;
		}
	}

	/**
	 * Extracts the DockerRequirement.dockerPull image reference from a CommandLineTool node.
	 * Supports both the map form (requirements: DockerRequirement: dockerPull: ...) and the
	 * list form (requirements: - class: DockerRequirement, dockerPull: ...), and falls back
	 * to "hints" if the DockerRequirement is not declared as a mandatory requirement.
	 * 
	 * @param oCommandLineTool CommandLineTool node
	 * @return the image reference, or an empty string if not found
	 */
	@SuppressWarnings("unchecked")
	public static String getDockerPull(Map<String, Object> oCommandLineTool) {
		try {
			if (oCommandLineTool == null) return "";

			String sDockerPull = getDockerPullFromRequirementsLike(oCommandLineTool.get("requirements"));

			if (!sDockerPull.isEmpty()) return sDockerPull;

			// Not mandatory in the CWL spec, but some packages declare DockerRequirement as a hint
			return getDockerPullFromRequirementsLike(oCommandLineTool.get("hints"));
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("CwlApplicationPackageUtils.getDockerPull: exception", oEx);
			return "";
		}
	}

	@SuppressWarnings("unchecked")
	private static String getDockerPullFromRequirementsLike(Object oRequirements) {

		if (oRequirements instanceof Map) {
			Map<String, Object> oRequirementsMap = (Map<String, Object>) oRequirements;
			Object oDockerRequirement = oRequirementsMap.get("DockerRequirement");

			if (oDockerRequirement instanceof Map) {
				Object oDockerPull = ((Map<String, Object>) oDockerRequirement).get("dockerPull");
				if (oDockerPull != null) return oDockerPull.toString();
			}
		}
		else if (oRequirements instanceof List) {
			for (Object oRequirement : (List<Object>) oRequirements) {
				if (!(oRequirement instanceof Map)) continue;

				Map<String, Object> oRequirementMap = (Map<String, Object>) oRequirement;

				if ("DockerRequirement".equals(oRequirementMap.get("class"))) {
					Object oDockerPull = oRequirementMap.get("dockerPull");
					if (oDockerPull != null) return oDockerPull.toString();
				}
			}
		}

		return "";
	}

	/**
	 * Rewrites (in place) a CommandLineTool's DockerRequirement.dockerPull, e.g. to point at the
	 * image WASDI itself built and pushed for a self-contained Application Package. Normalizes
	 * to whichever of "requirements"/"hints" list-or-map form the node already used, preserving
	 * any other declared requirement/hint.
	 * 
	 * @param oCommandLineTool CommandLineTool node to rewrite
	 * @param sDockerPull the new image reference
	 */
	@SuppressWarnings("unchecked")
	public static void setDockerPull(Map<String, Object> oCommandLineTool, String sDockerPull) {

		if (oCommandLineTool == null) return;

		Object oRequirements = oCommandLineTool.get("requirements");

		if (oRequirements instanceof List) {
			List<Object> aoRequirements = (List<Object>) oRequirements;
			aoRequirements.removeIf(oReq -> (oReq instanceof Map) && "DockerRequirement".equals(((Map<String, Object>) oReq).get("class")));

			Map<String, Object> oDockerRequirement = new LinkedHashMap<>();
			oDockerRequirement.put("class", "DockerRequirement");
			oDockerRequirement.put("dockerPull", sDockerPull);
			aoRequirements.add(oDockerRequirement);
		}
		else {
			Map<String, Object> oRequirementsMap = (oRequirements instanceof Map) ? (Map<String, Object>) oRequirements : new LinkedHashMap<>();

			Map<String, Object> oDockerRequirement = new LinkedHashMap<>();
			oDockerRequirement.put("dockerPull", sDockerPull);
			oRequirementsMap.put("DockerRequirement", oDockerRequirement);

			oCommandLineTool.put("requirements", oRequirementsMap);
		}
	}

	/**
	 * Builds a WASDI JSON parameter sample from the "inputs" section of the CWL Workflow node.
	 * Values are taken from the CWL "default", or a type-consistent empty value otherwise.
	 * 
	 * @param oWorkflow Workflow node
	 * @return JSON string with the parameter sample
	 */
	@SuppressWarnings("unchecked")
	public static String buildParameterSampleJson(Map<String, Object> oWorkflow) {
		try {
			Map<String, Object> oSample = new LinkedHashMap<>();

			if (oWorkflow == null) return "{}";

			Object oInputs = oWorkflow.get("inputs");

			if (!(oInputs instanceof Map)) return "{}";

			Map<String, Object> oInputsMap = (Map<String, Object>) oInputs;

			for (Map.Entry<String, Object> oEntry : oInputsMap.entrySet()) {
				String sKey = oEntry.getKey();
				Object oValue = oEntry.getValue();

				if (!(oValue instanceof Map)) {
					// Shorthand notation: "key: type" -> we only have the type
					oSample.put(sKey, defaultValueForType(oValue == null ? "string" : oValue.toString()));
					continue;
				}

				Map<String, Object> oInputDefinition = (Map<String, Object>) oValue;

				if (oInputDefinition.containsKey("default")) {
					oSample.put(sKey, oInputDefinition.get("default"));
				}
				else {
					String sType = String.valueOf(oInputDefinition.getOrDefault("type", "string"));
					oSample.put(sKey, defaultValueForType(sType));
				}
			}

			ObjectMapper oJsonMapper = new ObjectMapper();
			return oJsonMapper.writeValueAsString(oSample);
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("CwlApplicationPackageUtils.buildParameterSampleJson: exception", oEx);
			return "{}";
		}
	}

	/**
	 * Builds the CWL job order (the document cwltool expects as its second argument) from the
	 * WASDI job input values, keyed by the Workflow's own input ids. Directory-typed inputs are
	 * wrapped as CWL Directory objects ({class: Directory, path: ...}); every other value (incl.
	 * arrays, for a scattered input) is passed through as-is, since cwltool resolves scatter,
	 * steps wiring and expressions itself.
	 * 
	 * @param oWorkflow Workflow node
	 * @param oJobInputValues WASDI job input values, keyed by Workflow input ids
	 * @return the job order, ready to be written to YAML
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Object> buildJobOrder(Map<String, Object> oWorkflow, Map<String, Object> oJobInputValues) {

		Map<String, Object> oJobOrder = new LinkedHashMap<>();

		if (oWorkflow == null || oJobInputValues == null) return oJobOrder;

		Object oInputs = oWorkflow.get("inputs");

		if (!(oInputs instanceof Map)) return oJobOrder;

		for (Map.Entry<String, Object> oEntry : ((Map<String, Object>) oInputs).entrySet()) {

			String sInputId = oEntry.getKey();

			if (!oJobInputValues.containsKey(sInputId)) continue;

			Object oValue = oJobInputValues.get(sInputId);

			String sType = (oEntry.getValue() instanceof Map)
					? String.valueOf(((Map<String, Object>) oEntry.getValue()).getOrDefault("type", "string"))
					: String.valueOf(oEntry.getValue());

			String sCleanType = sType.replace("?", "").replace("[]", "").trim();

			if ("Directory".equalsIgnoreCase(sCleanType) && oValue != null) {
				Map<String, Object> oDirectoryValue = new LinkedHashMap<>();
				oDirectoryValue.put("class", "Directory");
				oDirectoryValue.put("path", oValue.toString());
				oJobOrder.put(sInputId, oDirectoryValue);
			}
			else {
				oJobOrder.put(sInputId, oValue);
			}
		}

		return oJobOrder;
	}

	/**
	 * Lists the ids of the Workflow's (or a CommandLineTool's) "inputs" that are declared with
	 * the given CWL type (e.g. "Directory"), used to find which job values need STAC stage-in.
	 * 
	 * @param oToolOrWorkflow CommandLineTool or Workflow node
	 * @param sType CWL type to match (e.g. "Directory")
	 * @return list of input ids matching the type
	 */
	@SuppressWarnings("unchecked")
	public static List<String> getInputIdsOfType(Map<String, Object> oToolOrWorkflow, String sType) {

		List<String> asMatchingIds = new ArrayList<>();

		if (oToolOrWorkflow == null) return asMatchingIds;

		Object oInputs = oToolOrWorkflow.get("inputs");

		if (!(oInputs instanceof Map)) return asMatchingIds;

		for (Map.Entry<String, Object> oEntry : ((Map<String, Object>) oInputs).entrySet()) {

			if (!(oEntry.getValue() instanceof Map)) continue;

			String sInputType = String.valueOf(((Map<String, Object>) oEntry.getValue()).getOrDefault("type", "string"));

			if (sInputType.replace("?", "").replace("[]", "").trim().equalsIgnoreCase(sType)) {
				asMatchingIds.add(oEntry.getKey());
			}
		}

		return asMatchingIds;
	}

	private static Object defaultValueForType(String sType) {
		String sCleanType = sType.replace("?", "").replace("[]", "").trim().toLowerCase();

		switch (sCleanType) {
			case "int":
			case "long":
				return 0;
			case "float":
			case "double":
				return 0.0;
			case "boolean":
				return false;
			default:
				return "";
		}
	}
}
