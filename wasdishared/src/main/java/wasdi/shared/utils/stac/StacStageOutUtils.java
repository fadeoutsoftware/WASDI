package wasdi.shared.utils.stac;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
}
