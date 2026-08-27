package wasdi.shared.utils.cwl;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * Utility methods to read an OGC Best Practice for Earth Observation Application Package
 * (see https://docs.ogc.org/bp/20-089r1.html), i.e. a CWL document with a single "Workflow"
 * class and a single "CommandLineTool" class.
 * 
 * This is the "consumer" side counterpart of the CWL generation done, for the EOEPCA
 * processor engine, by the wasdi-processor.cwl.j2 template.
 * 
 * NOTE: v1 scope only, no multi-step Workflow, no Directory/File (STAC) inputs.
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
	 * Translates WASDI job input values, keyed by Workflow input ids, into values keyed by the
	 * single step's CommandLineTool input ids, resolving the Workflow "steps.&lt;step&gt;.in" mapping
	 * (e.g. a Workflow input "proj" bound to a CommandLineTool input "epsg" via "in: { epsg: proj }").
	 * 
	 * v1 scope: single step workflow. If the steps/in mapping cannot be read, falls back to the
	 * values as-is (same behaviour as before, i.e. assumes matching ids).
	 * 
	 * @param oWorkflow Workflow node
	 * @param oWorkflowInputValues WASDI job input values, keyed by Workflow input ids
	 * @return values keyed by the CommandLineTool input ids
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Object> mapWorkflowValuesToToolInputs(Map<String, Object> oWorkflow, Map<String, Object> oWorkflowInputValues) {

		if (oWorkflowInputValues == null) oWorkflowInputValues = new LinkedHashMap<>();

		try {
			if (oWorkflow == null) return oWorkflowInputValues;

			Object oSteps = oWorkflow.get("steps");

			if (!(oSteps instanceof Map) || ((Map<String, Object>) oSteps).isEmpty()) return oWorkflowInputValues;

			// v1: single step workflow, take the first (and only) one
			Object oFirstStep = ((Map<String, Object>) oSteps).values().iterator().next();

			if (!(oFirstStep instanceof Map)) return oWorkflowInputValues;

			Object oIn = ((Map<String, Object>) oFirstStep).get("in");

			if (!(oIn instanceof Map)) return oWorkflowInputValues;

			Map<String, Object> oToolInputValues = new LinkedHashMap<>();

			for (Map.Entry<String, Object> oInEntry : ((Map<String, Object>) oIn).entrySet()) {

				String sToolInputId = oInEntry.getKey();
				Object oSource = oInEntry.getValue();

				// "in" entries are usually the plain Workflow input id, but can also be an object with a "source" field
				String sWorkflowInputId = (oSource instanceof Map) ? String.valueOf(((Map<String, Object>) oSource).get("source")) : String.valueOf(oSource);

				if (oWorkflowInputValues.containsKey(sWorkflowInputId)) {
					oToolInputValues.put(sToolInputId, oWorkflowInputValues.get(sWorkflowInputId));
				}
			}

			return oToolInputValues;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("CwlApplicationPackageUtils.mapWorkflowValuesToToolInputs: exception, falling back to the values as-is", oEx);
			return oWorkflowInputValues;
		}
	}

	/**
	 * Builds the command line (as a list of args, baseCommand included) to run the
	 * CommandLineTool container, mapping the CWL "inputs" (sorted by inputBinding.position)
	 * on the values found in the WASDI job parameters.
	 * 
	 * @param oCommandLineTool CommandLineTool node
	 * @param oJobInputValues WASDI job input values, keyed by CommandLineTool input ids (see {@link #mapWorkflowValuesToToolInputs})
	 * @return ordered list of command line arguments
	 */
	@SuppressWarnings("unchecked")
	public static List<String> buildCommandLine(Map<String, Object> oCommandLineTool, Map<String, Object> oJobInputValues) {

		List<String> asCommand = new ArrayList<>();

		if (oCommandLineTool == null) return asCommand;

		if (oJobInputValues == null) oJobInputValues = new LinkedHashMap<>();

		// Base Command: can be a single string or a list of strings
		Object oBaseCommand = oCommandLineTool.get("baseCommand");

		if (oBaseCommand instanceof List) {
			for (Object oCommandPart : (List<Object>) oBaseCommand) {
				asCommand.add(String.valueOf(oCommandPart));
			}
		}
		else if (oBaseCommand != null) {
			asCommand.add(oBaseCommand.toString());
		}

		// Positional args: merge fixed "arguments" entries and the "inputs" bound to job values
		List<PositionalArg> aoPositionalArgs = new ArrayList<>();

		Object oArguments = oCommandLineTool.get("arguments");

		if (oArguments instanceof List) {
			int iPosition = 0;
			for (Object oArgument : (List<Object>) oArguments) {
				aoPositionalArgs.add(new PositionalArg(iPosition++, String.valueOf(oArgument)));
			}
		}

		Object oInputs = oCommandLineTool.get("inputs");

		if (oInputs instanceof Map) {
			for (Map.Entry<String, Object> oEntry : ((Map<String, Object>) oInputs).entrySet()) {

				if (!(oEntry.getValue() instanceof Map)) continue;

				Map<String, Object> oInputDefinition = (Map<String, Object>) oEntry.getValue();
				Object oInputBinding = oInputDefinition.get("inputBinding");

				if (!(oInputBinding instanceof Map)) continue;

				Map<String, Object> oInputBindingMap = (Map<String, Object>) oInputBinding;

				int iPosition = toInt(oInputBindingMap.get("position"), Integer.MAX_VALUE);
				String sPrefix = oInputBindingMap.get("prefix") == null ? null : oInputBindingMap.get("prefix").toString();

				String sType = String.valueOf(oInputDefinition.getOrDefault("type", "string"));

				if (isUnsupportedType(sType)) {
					WasdiLog.warnLog("CwlApplicationPackageUtils.buildCommandLine: input [" + oEntry.getKey() + "] has type [" + sType + "], File inputs are not supported yet, skipping it");
					continue;
				}

				Object oValue = oJobInputValues.containsKey(oEntry.getKey()) ? oJobInputValues.get(oEntry.getKey()) : oInputDefinition.get("default");

				if (oValue == null) {
					WasdiLog.warnLog("CwlApplicationPackageUtils.buildCommandLine: no value found for input [" + oEntry.getKey() + "], skipping it");
					continue;
				}

				if (sType.replace("?", "").equalsIgnoreCase("boolean")) {
					if (Boolean.parseBoolean(oValue.toString()) && !Utils.isNullOrEmpty(sPrefix)) {
						aoPositionalArgs.add(new PositionalArg(iPosition, sPrefix));
					}
					continue;
				}

				if (!Utils.isNullOrEmpty(sPrefix)) {
					aoPositionalArgs.add(new PositionalArg(iPosition, sPrefix));
					// Give the value the same position + a fraction so it stays right after its prefix
					aoPositionalArgs.add(new PositionalArg(iPosition, oValue.toString()));
				}
				else {
					aoPositionalArgs.add(new PositionalArg(iPosition, oValue.toString()));
				}
			}
		}

		aoPositionalArgs.sort((oFirst, oSecond) -> Integer.compare(oFirst.position, oSecond.position));

		for (PositionalArg oPositionalArg : aoPositionalArgs) {
			asCommand.add(oPositionalArg.value);
		}

		return asCommand;
	}

	private static boolean isUnsupportedType(String sType) {
		String sCleanType = sType.replace("?", "").replace("[]", "").trim();
		return sCleanType.equalsIgnoreCase("File");
	}

	/**
	 * Lists the ids of the CommandLineTool (or Workflow) "inputs" that are declared with the
	 * given CWL type (e.g. "Directory"), used to find which job values need STAC stage-in.
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

	private static int toInt(Object oValue, int iDefault) {
		if (oValue == null) return iDefault;
		try {
			return Integer.parseInt(oValue.toString());
		}
		catch (Exception oEx) {
			return iDefault;
		}
	}

	/**
	 * Small helper class to keep a command line argument together with its CWL position
	 */
	private static class PositionalArg {
		int position;
		String value;

		PositionalArg(int position, String value) {
			this.position = position;
			this.value = value;
		}
	}
}
