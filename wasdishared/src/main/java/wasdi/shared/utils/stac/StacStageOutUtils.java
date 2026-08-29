package wasdi.shared.utils.stac;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;

import wasdi.shared.utils.ProcessWorkspaceLogger;
import wasdi.shared.utils.log.WasdiLog;

/**
 * Stage-out helper (OGC BP §9.4.2, Req 20): reads the local STAC Catalog an Application is
 * required to produce in its output/working directory (Req 5), and resolves it to the actual
 * result files (the STAC Item(s) and their Assets) that the Platform must stage-out.
 * 
 * v1 scope: a single-level catalog.json with "item" links (no nested sub-catalogs).
 * 
 * @author p.campanella
 */
public class StacStageOutUtils {

	private StacStageOutUtils() {
		// private constructor to hide the public implicit one
	}

	/**
	 * A file to stage-out, together with the bounding box (WASDI "north,west,south,east" format)
	 * taken from the STAC Item it belongs to, if any
	 */
	public static class StagedOutFile {
		public File file;
		public String bbox;

		public StagedOutFile(File file, String bbox) {
			this.file = file;
			this.bbox = bbox;
		}
	}

	/**
	 * Parses the local STAC Catalog (catalog.json) an Application is expected to produce in its
	 * output folder, and resolves every Item's Assets (and the Item file itself) to stage-out.
	 * 
	 * @param oOutputFolder Application output/working folder
	 * @param oProcessWorkspaceLogger Optional logger to also surface broken/missing links to the user
	 * @return the files to stage-out, or null if no local catalog.json is present (caller should fallback)
	 */
	@SuppressWarnings("unchecked")
	public static List<StagedOutFile> parseLocalOutputCatalog(File oOutputFolder, ProcessWorkspaceLogger oProcessWorkspaceLogger) {
		try {
			File oCatalogFile = new File(oOutputFolder, "catalog.json");

			if (!oCatalogFile.exists()) return null;

			ObjectMapper oJsonMapper = new ObjectMapper();
			Map<String, Object> oCatalog = oJsonMapper.readValue(oCatalogFile, Map.class);

			Object oLinks = oCatalog.get("links");

			List<StagedOutFile> aoStagedOutFiles = new ArrayList<>();

			if (!(oLinks instanceof List)) return aoStagedOutFiles;

			for (Object oLinkObj : (List<Object>) oLinks) {

				if (!(oLinkObj instanceof Map)) continue;

				Map<String, Object> oLink = (Map<String, Object>) oLinkObj;

				if (!"item".equals(oLink.get("rel"))) continue;

				Object oHref = oLink.get("href");

				if (oHref == null) continue;

				File oItemFile = new File(oOutputFolder, oHref.toString());

				if (!oItemFile.exists()) {
					String sWarning = "StacStageOutUtils.parseLocalOutputCatalog: item file not found " + oItemFile.getAbsolutePath();
					WasdiLog.warnLog(sWarning);
					if (oProcessWorkspaceLogger != null) oProcessWorkspaceLogger.log(sWarning);
					continue;
				}

				Map<String, Object> oItem = oJsonMapper.readValue(oItemFile, Map.class);
				String sBbox = extractBboxString(oItem);

				// The Item file itself is staged-out too (kept visible in WASDI for provenance)
				aoStagedOutFiles.add(new StagedOutFile(oItemFile, sBbox));

				Object oAssets = oItem.get("assets");

				if (!(oAssets instanceof Map)) continue;

				for (Object oAssetObj : ((Map<String, Object>) oAssets).values()) {

					if (!(oAssetObj instanceof Map)) continue;

					Object oAssetHref = ((Map<String, Object>) oAssetObj).get("href");

					if (oAssetHref == null) continue;

					File oAssetFile = new File(oItemFile.getParentFile(), oAssetHref.toString());

					if (oAssetFile.exists()) {
						aoStagedOutFiles.add(new StagedOutFile(oAssetFile, sBbox));
					}
					else {
						String sWarning = "StacStageOutUtils.parseLocalOutputCatalog: asset file not found " + oAssetFile.getAbsolutePath();
						WasdiLog.warnLog(sWarning);
						if (oProcessWorkspaceLogger != null) oProcessWorkspaceLogger.log(sWarning);
					}
				}
			}

			return aoStagedOutFiles;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("StacStageOutUtils.parseLocalOutputCatalog: exception", oEx);
			return null;
		}
	}

	/**
	 * Converts a STAC Feature "bbox" ([minLon, minLat, maxLon, maxLat]) into WASDI's own
	 * "north,west,south,east" bbox string convention
	 */
	@SuppressWarnings("unchecked")
	private static String extractBboxString(Map<String, Object> oItem) {
		try {
			Object oBbox = oItem.get("bbox");

			if (!(oBbox instanceof List)) return "";

			List<Object> aoBbox = (List<Object>) oBbox;

			if (aoBbox.size() < 4) return "";

			double dMinLon = Double.parseDouble(aoBbox.get(0).toString());
			double dMinLat = Double.parseDouble(aoBbox.get(1).toString());
			double dMaxLon = Double.parseDouble(aoBbox.get(2).toString());
			double dMaxLat = Double.parseDouble(aoBbox.get(3).toString());

			return dMaxLat + "," + dMinLon + "," + dMinLat + "," + dMaxLon;
		}
		catch (Exception oEx) {
			WasdiLog.warnLog("StacStageOutUtils.extractBboxString: impossible to parse the item bbox");
			return "";
		}
	}

	/**
	 * Parses cwltool's own final result document (the JSON it prints to stdout, describing which
	 * File/Directory correspond to which declared Workflow output - this is the authoritative
	 * source of "what did this run actually produce", covering scatter/multi-step/subworkflow
	 * outputs that a simple CWL outputBinding.glob re-implementation could never resolve correctly.
	 * 
	 * @param oResultJsonFile the file the cwltool invocation's stdout was redirected to
	 * @return the parsed result document, or null if it does not exist / cannot be parsed
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Object> parseCwltoolResult(File oResultJsonFile) {
		try {
			if (oResultJsonFile == null || !oResultJsonFile.exists()) {
				WasdiLog.warnLog("StacStageOutUtils.parseCwltoolResult: result file not found");
				return null;
			}

			return new ObjectMapper().readValue(oResultJsonFile, Map.class);
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("StacStageOutUtils.parseCwltoolResult: exception parsing " + oResultJsonFile, oEx);
			return null;
		}
	}

	/**
	 * Recursively collects every local file referenced by cwltool's result document: walks every
	 * declared Workflow output value, following File "path"/"location" entries, Directory entries
	 * (recursing into their own local folder content) and arrays (for scattered/array outputs).
	 * 
	 * @param oCwltoolResult parsed cwltool result document (see {@link #parseCwltoolResult(File)})
	 * @return the set of local files referenced, in no particular order
	 */
	public static Set<File> collectCwltoolOutputFiles(Map<String, Object> oCwltoolResult) {

		Set<File> aoFiles = new LinkedHashSet<>();

		if (oCwltoolResult == null) return aoFiles;

		for (Object oOutputValue : oCwltoolResult.values()) {
			collectFilesFromValue(oOutputValue, aoFiles);
		}

		return aoFiles;
	}

	@SuppressWarnings("unchecked")
	private static void collectFilesFromValue(Object oValue, Set<File> aoFiles) {

		if (oValue instanceof List) {
			for (Object oElement : (List<Object>) oValue) {
				collectFilesFromValue(oElement, aoFiles);
			}
			return;
		}

		if (!(oValue instanceof Map)) return;

		Map<String, Object> oValueMap = (Map<String, Object>) oValue;
		String sClass = String.valueOf(oValueMap.get("class"));
		String sPath = resolveLocalPath(oValueMap);

		if ("File".equals(sClass)) {
			if (sPath != null) {
				File oFile = new File(sPath);
				if (oFile.exists()) aoFiles.add(oFile);
			}
		}
		else if ("Directory".equals(sClass)) {
			if (sPath != null) {
				File oDirectory = new File(sPath);
				collectFilesRecursively(oDirectory, aoFiles);
			}
		}

		// Some cwltool outputs (e.g. Directory "listing") nest further File/Directory objects
		Object oListing = oValueMap.get("listing");

		if (oListing instanceof List) {
			for (Object oListedValue : (List<Object>) oListing) {
				collectFilesFromValue(oListedValue, aoFiles);
			}
		}
	}

	private static void collectFilesRecursively(File oFolder, Set<File> aoFiles) {
		if (oFolder == null || !oFolder.exists()) return;

		File[] aoContent = oFolder.listFiles();

		if (aoContent == null) return;

		for (File oEntry : aoContent) {
			if (oEntry.isDirectory()) collectFilesRecursively(oEntry, aoFiles);
			else aoFiles.add(oEntry);
		}
	}

	private static String resolveLocalPath(Map<String, Object> oValueMap) {
		Object oPath = oValueMap.get("path");

		if (oPath != null) return oPath.toString();

		Object oLocation = oValueMap.get("location");

		if (oLocation == null) return null;

		String sLocation = oLocation.toString();
		return sLocation.startsWith("file://") ? sLocation.substring("file://".length()) : sLocation;
	}
}
