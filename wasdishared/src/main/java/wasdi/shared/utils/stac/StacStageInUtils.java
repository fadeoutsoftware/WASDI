package wasdi.shared.utils.stac;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;

/**
 * Stage-in helper for a CWL "Directory" input bound to a STAC Item (OGC BP §9.4.1, Req 18/19).
 * 
 * v1 scope: a single remote STAC Item (GeoJSON Feature with an "assets" map). Downloads every
 * asset locally and writes a local STAC Catalog + Item, in the exact layout used by the
 * OGC BP Annex D examples (https://github.com/eoap/ogc-bp-ap-annex-a-examples), i.e.:
 * 
 * &lt;staging folder&gt;/catalog.json
 * &lt;staging folder&gt;/&lt;item id&gt;/&lt;item id&gt;.json
 * &lt;staging folder&gt;/&lt;item id&gt;/&lt;asset files...&gt;
 * 
 * @author p.campanella
 */
public class StacStageInUtils {

	private StacStageInUtils() {
		// private constructor to hide the public implicit one
	}

	/**
	 * Stages-in a single STAC Item into a local folder: downloads all its assets and writes a
	 * local STAC Catalog + Item referencing the now-local files.
	 * 
	 * @param sStacItemUrl URL of the STAC Item (a GeoJSON Feature) to stage-in
	 * @param sHostStagingFolder Host folder where to stage the data (created if missing)
	 * @return the (host) staging folder containing the local catalog.json, or null in case of problems
	 */
	@SuppressWarnings("unchecked")
	public static File stageInStacItem(String sStacItemUrl, String sHostStagingFolder) {
		try {
			if (Utils.isNullOrEmpty(sStacItemUrl)) {
				WasdiLog.errorLog("StacStageInUtils.stageInStacItem: the STAC Item url is null or empty");
				return null;
			}

			File oStagingFolder = new File(sHostStagingFolder);
			oStagingFolder.mkdirs();

			WasdiLog.debugLog("StacStageInUtils.stageInStacItem: fetching STAC Item " + sStacItemUrl);

			HttpCallResponse oResponse = HttpUtils.httpGet(sStacItemUrl);

			if (oResponse == null || oResponse.getResponseCode() < 200 || oResponse.getResponseCode() > 299) {
				WasdiLog.errorLog("StacStageInUtils.stageInStacItem: impossible to fetch the STAC Item " + sStacItemUrl);
				return null;
			}

			ObjectMapper oJsonMapper = new ObjectMapper();
			Map<String, Object> oItem = oJsonMapper.readValue(oResponse.getResponseBody(), Map.class);

			String sItemId = String.valueOf(oItem.getOrDefault("id", "item"));

			File oItemFolder = new File(oStagingFolder, sItemId);
			oItemFolder.mkdirs();

			Object oAssets = oItem.get("assets");

			if (!(oAssets instanceof Map)) {
				WasdiLog.errorLog("StacStageInUtils.stageInStacItem: the STAC Item has no assets, nothing to stage-in");
				return null;
			}

			Map<String, Object> oLocalAssets = new LinkedHashMap<>();

			for (Map.Entry<String, Object> oAssetEntry : ((Map<String, Object>) oAssets).entrySet()) {

				if (!(oAssetEntry.getValue() instanceof Map)) continue;

				Map<String, Object> oAsset = new LinkedHashMap<>((Map<String, Object>) oAssetEntry.getValue());
				String sHref = String.valueOf(oAsset.get("href"));

				if (Utils.isNullOrEmpty(sHref) || "null".equals(sHref)) {
					WasdiLog.warnLog("StacStageInUtils.stageInStacItem: asset [" + oAssetEntry.getKey() + "] has no href, skipping it");
					continue;
				}

				String sAssetFileName = sHref.substring(Math.max(sHref.lastIndexOf('/'), sHref.lastIndexOf('\\')) + 1);
				String sLocalAssetPath = new File(oItemFolder, sAssetFileName).getAbsolutePath();

				WasdiLog.debugLog("StacStageInUtils.stageInStacItem: downloading asset [" + oAssetEntry.getKey() + "] from " + sHref);

				if (Utils.isNullOrEmpty(HttpUtils.downloadFile(sHref, new LinkedHashMap<>(), sLocalAssetPath))) {
					WasdiLog.errorLog("StacStageInUtils.stageInStacItem: impossible to download asset [" + oAssetEntry.getKey() + "] from " + sHref);
					return null;
				}

				// Rewrite the href to be local and relative to the item json file
				oAsset.put("href", sAssetFileName);
				oLocalAssets.put(oAssetEntry.getKey(), oAsset);
			}

			oItem.put("assets", oLocalAssets);

			File oItemJsonFile = new File(oItemFolder, sItemId + ".json");
			oJsonMapper.writeValue(oItemJsonFile, oItem);

			writeLocalCatalog(oStagingFolder, sItemId);

			WasdiLog.debugLog("StacStageInUtils.stageInStacItem: stage-in done in " + oStagingFolder.getAbsolutePath());

			return oStagingFolder;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("StacStageInUtils.stageInStacItem: exception", oEx);
			return null;
		}
	}

	private static void writeLocalCatalog(File oStagingFolder, String sItemId) throws Exception {

		Map<String, Object> oCatalog = new LinkedHashMap<>();
		oCatalog.put("id", "catalog");
		oCatalog.put("stac_version", "1.0.0");
		oCatalog.put("type", "Catalog");
		oCatalog.put("description", "WASDI stage-in catalog");

		List<Map<String, Object>> aoLinks = new ArrayList<>();
		Map<String, Object> oLink = new LinkedHashMap<>();
		oLink.put("rel", "item");
		oLink.put("type", "application/geo+json");
		oLink.put("href", sItemId + "/" + sItemId + ".json");
		aoLinks.add(oLink);

		oCatalog.put("links", aoLinks);

		File oCatalogFile = new File(oStagingFolder, "catalog.json");
		new ObjectMapper().writeValue(oCatalogFile, oCatalog);
	}
}
