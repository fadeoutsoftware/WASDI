package it.fadeout.rest.resources;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import it.fadeout.Wasdi;
import wasdi.shared.business.DownloadedFile;
import wasdi.shared.business.ProductWorkspace;
import wasdi.shared.business.Workspace;
import wasdi.shared.business.users.User;
import wasdi.shared.business.users.UserResourcePermission;
import wasdi.shared.config.PathsConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.DownloadedFilesRepository;
import wasdi.shared.data.ProductWorkspaceRepository;
import wasdi.shared.data.UserResourcePermissionRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.utils.PermissionsUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.gis.BoundingBoxUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.stac.StacAsset;
import wasdi.shared.viewmodels.stac.StacCatalog;
import wasdi.shared.viewmodels.stac.StacCollection;
import wasdi.shared.viewmodels.stac.StacCollectionsResponse;
import wasdi.shared.viewmodels.stac.StacExtent;
import wasdi.shared.viewmodels.stac.StacGeometry;
import wasdi.shared.viewmodels.stac.StacItem;
import wasdi.shared.viewmodels.stac.StacItemCollection;
import wasdi.shared.viewmodels.stac.StacLink;
import wasdi.shared.viewmodels.stac.StacSpatialExtent;
import wasdi.shared.viewmodels.stac.StacTemporalExtent;

/**
 * STAC API (https://github.com/radiantearth/stac-api-spec) resource, exposing WASDI Workspaces as
 * STAC Collections and their files as STAC Items.
 * 
 * v1 scope: a Workspace's Collection extent is derived only from what is already stored in the db
 * ({@link ProductWorkspace#getBbox()}/{@link DownloadedFile#getBoundingBox()}/{@link DownloadedFile#getRefDate()}),
 * no on-the-fly geospatial computation. Asset hrefs reuse {@link CatalogResources#downloadEntryByName}
 * with the caller's own session token embedded in the link (so links are only valid as long as the
 * session is): a known, accepted, v1 limitation.
 * 
 * Access: like the rest of the site a session is required to see private Workspaces, but every
 * endpoint here also works without a session (or with an invalid/expired one), in which case only
 * public Workspaces ({@link Workspace#isPublic()}) are visible - to support anonymous STAC clients.
 * 
 * @author p.campanella
 */
@Path("/stac")
public class StacResource {

	private static final String STAC_VERSION = "1.0.0";

	private static final List<String> CONFORMANCE_CLASSES = Arrays.asList(
			"https://api.stacspec.org/v1.0.0/core",
			"https://api.stacspec.org/v1.0.0/ogcapi-features",
			"https://api.stacspec.org/v1.0.0/collections",
			"http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/core",
			"http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/oas30",
			"http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/geojson");

	private static final int DEFAULT_ITEMS_LIMIT = 20;
	private static final int MAX_ITEMS_LIMIT = 500;

	/**
	 * Landing page: links to the other endpoints/capabilities.
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getLandingPage() {
		try {
			String sSelfUrl = getBaseUrl() + "stac";

			StacCatalog oCatalog = new StacCatalog();
			oCatalog.setId("wasdi");
			oCatalog.setTitle("WASDI STAC Catalog");
			oCatalog.setDescription("STAC entry point exposing WASDI Workspaces as Collections and their files as Items");
			oCatalog.setConformsTo(CONFORMANCE_CLASSES);

			List<StacLink> aoLinks = new ArrayList<>();
			aoLinks.add(new StacLink(sSelfUrl, "self", MediaType.APPLICATION_JSON, "This document"));
			aoLinks.add(new StacLink(sSelfUrl, "root", MediaType.APPLICATION_JSON, "This document"));
			aoLinks.add(new StacLink(sSelfUrl + "/conformance", "conformance", MediaType.APPLICATION_JSON, "Conformance classes"));
			aoLinks.add(new StacLink(sSelfUrl + "/collections", "data", MediaType.APPLICATION_JSON, "List of Collections"));
			oCatalog.setLinks(aoLinks);

			return Response.ok(oCatalog).build();
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("StacResource.getLandingPage: exception " + oEx.toString());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * Conformance classes this API implements.
	 */
	@GET
	@Path("/conformance")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getConformance() {
		Map<String, Object> oBody = new HashMap<>();
		oBody.put("conformsTo", CONFORMANCE_CLASSES);
		return Response.ok(oBody).build();
	}

	/**
	 * Lists the Workspaces accessible to the caller (owned + shared + public if a session is
	 * provided, public only otherwise) as STAC Collections.
	 */
	@GET
	@Path("/collections")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getCollections(@HeaderParam("x-session-token") String sSessionId, @QueryParam("token") String sTokenSessionId) {
		try {
			User oUser = resolveUser(sSessionId, sTokenSessionId);

			List<Workspace> aoWorkspaces = getAccessibleWorkspaces(oUser);

			List<StacCollection> aoCollections = new ArrayList<>();
			for (Workspace oWorkspace : aoWorkspaces) {
				aoCollections.add(buildCollection(oWorkspace));
			}

			StacCollectionsResponse oResponse = new StacCollectionsResponse();
			oResponse.setCollections(aoCollections);

			List<StacLink> aoLinks = new ArrayList<>();
			String sSelfUrl = getBaseUrl() + "stac/collections";
			aoLinks.add(new StacLink(sSelfUrl, "self", MediaType.APPLICATION_JSON, "This document"));
			aoLinks.add(new StacLink(getBaseUrl() + "stac", "root", MediaType.APPLICATION_JSON, "Landing Page"));
			oResponse.setLinks(aoLinks);

			return Response.ok(oResponse).build();
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("StacResource.getCollections: exception " + oEx.toString());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * Returns metadata, and the spatial/temporal extent, of a single Workspace as a STAC Collection.
	 */
	@GET
	@Path("/collections/{workspaceId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getCollection(@HeaderParam("x-session-token") String sSessionId, @QueryParam("token") String sTokenSessionId, @PathParam("workspaceId") String sWorkspaceId) {
		try {
			User oUser = resolveUser(sSessionId, sTokenSessionId);

			WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
			Workspace oWorkspace = oWorkspaceRepository.getWorkspace(sWorkspaceId);

			if (oWorkspace == null) {
				return Response.status(Status.NOT_FOUND).build();
			}

			if (!canAccessWorkspace(oUser, oWorkspace)) {
				return Response.status(oUser == null ? Status.UNAUTHORIZED : Status.FORBIDDEN).build();
			}

			return Response.ok(buildCollection(oWorkspace)).build();
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("StacResource.getCollection: exception " + oEx.toString());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * Returns the files of a Workspace as a GeoJSON FeatureCollection of STAC Items, paginated via
	 * "limit"/"next", and optionally filtered by "bbox" and/or "datetime".
	 */
	@GET
	@Path("/collections/{workspaceId}/items")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getItems(@HeaderParam("x-session-token") String sSessionId, @QueryParam("token") String sTokenSessionId,
			@PathParam("workspaceId") String sWorkspaceId, @QueryParam("limit") Integer iLimit, @QueryParam("next") Integer iOffset,
			@QueryParam("bbox") String sBboxFilter, @QueryParam("datetime") String sDatetimeFilter) {
		try {
			User oUser = resolveUser(sSessionId, sTokenSessionId);

			WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
			Workspace oWorkspace = oWorkspaceRepository.getWorkspace(sWorkspaceId);

			if (oWorkspace == null) {
				return Response.status(Status.NOT_FOUND).build();
			}

			if (!canAccessWorkspace(oUser, oWorkspace)) {
				return Response.status(oUser == null ? Status.UNAUTHORIZED : Status.FORBIDDEN).build();
			}

			if (iLimit == null || iLimit <= 0) iLimit = DEFAULT_ITEMS_LIMIT;
			if (iLimit > MAX_ITEMS_LIMIT) iLimit = MAX_ITEMS_LIMIT;
			if (iOffset == null || iOffset < 0) iOffset = 0;

			String sWorkspaceOwnerId = oWorkspace.getUserId();
			String sWorkspacePath = PathsConfig.getWorkspacePath(sWorkspaceOwnerId, sWorkspaceId);

			ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();
			List<ProductWorkspace> aoAllProducts = oProductWorkspaceRepository.getProductsByWorkspace(sWorkspaceId);

			double[] adBboxFilter = Utils.isNullOrEmpty(sBboxFilter) ? null : parseStacBboxParam(sBboxFilter);
			String[] asDatetimeFilter = Utils.isNullOrEmpty(sDatetimeFilter) ? null : sDatetimeFilter.split("/", -1);

			List<StacItem> aoMatchingItems = new ArrayList<>();

			for (ProductWorkspace oProductWorkspace : aoAllProducts) {

				DownloadedFile oDownloadedFile = getDownloadedFile(sWorkspacePath, oProductWorkspace.getProductName());

				double[] adItemBbox = toStacBbox(!Utils.isNullOrEmpty(oProductWorkspace.getBbox()) ? oProductWorkspace.getBbox() : (oDownloadedFile != null ? oDownloadedFile.getBoundingBox() : null));

				if (adBboxFilter != null && !bboxIntersects(adBboxFilter, adItemBbox)) continue;
				if (asDatetimeFilter != null && !matchesDatetimeFilter(asDatetimeFilter, oDownloadedFile)) continue;

				aoMatchingItems.add(buildItem(oProductWorkspace, oDownloadedFile, sWorkspaceId, sTokenSessionId != null ? sTokenSessionId : sSessionId));
			}

			int iNumberMatched = aoMatchingItems.size();
			int iFromIndex = Math.min(iOffset, iNumberMatched);
			int iToIndex = Math.min(iOffset + iLimit, iNumberMatched);
			List<StacItem> aoPageItems = aoMatchingItems.subList(iFromIndex, iToIndex);

			StacItemCollection oItemCollection = new StacItemCollection();
			oItemCollection.setFeatures(aoPageItems);
			oItemCollection.setNumberMatched(iNumberMatched);
			oItemCollection.setNumberReturned(aoPageItems.size());

			String sItemsSelfUrl = getBaseUrl() + "stac/collections/" + sWorkspaceId + "/items";

			List<StacLink> aoLinks = new ArrayList<>();
			aoLinks.add(new StacLink(sItemsSelfUrl, "self", MediaType.APPLICATION_JSON, "This document"));

			if (iToIndex < iNumberMatched) {
				aoLinks.add(new StacLink(sItemsSelfUrl + "?limit=" + iLimit + "&next=" + iToIndex, "next", MediaType.APPLICATION_JSON, "Next page"));
			}

			oItemCollection.setLinks(aoLinks);

			return Response.ok(oItemCollection).build();
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("StacResource.getItems: exception " + oEx.toString());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * Returns a single file of a Workspace as a STAC Item.
	 */
	@GET
	@Path("/collections/{workspaceId}/items/{fileId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getItem(@HeaderParam("x-session-token") String sSessionId, @QueryParam("token") String sTokenSessionId,
			@PathParam("workspaceId") String sWorkspaceId, @PathParam("fileId") String sFileId) {
		try {
			User oUser = resolveUser(sSessionId, sTokenSessionId);

			WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
			Workspace oWorkspace = oWorkspaceRepository.getWorkspace(sWorkspaceId);

			if (oWorkspace == null) {
				return Response.status(Status.NOT_FOUND).build();
			}

			if (!canAccessWorkspace(oUser, oWorkspace)) {
				return Response.status(oUser == null ? Status.UNAUTHORIZED : Status.FORBIDDEN).build();
			}

			ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();
			ProductWorkspace oProductWorkspace = oProductWorkspaceRepository.getProductWorkspace(sFileId, sWorkspaceId);

			if (oProductWorkspace == null) {
				return Response.status(Status.NOT_FOUND).build();
			}

			String sWorkspacePath = PathsConfig.getWorkspacePath(oWorkspace.getUserId(), sWorkspaceId);
			DownloadedFile oDownloadedFile = getDownloadedFile(sWorkspacePath, sFileId);

			StacItem oItem = buildItem(oProductWorkspace, oDownloadedFile, sWorkspaceId, sTokenSessionId != null ? sTokenSessionId : sSessionId);

			return Response.ok(oItem).build();
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("StacResource.getItem: exception " + oEx.toString());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * Resolves the caller's session to a User, returning null (anonymous) instead of failing when
	 * no valid session is provided: STAC endpoints must stay browsable without authentication.
	 */
	private User resolveUser(String sSessionId, String sTokenSessionId) {
		String sEffectiveSessionId = !Utils.isNullOrEmpty(sSessionId) ? sSessionId : sTokenSessionId;

		if (Utils.isNullOrEmpty(sEffectiveSessionId)) return null;

		return Wasdi.getUserFromSession(sEffectiveSessionId);
	}

	/**
	 * True if the given (possibly null/anonymous) user can see the Workspace: owner/shared/public
	 * when logged in, public only when anonymous.
	 */
	private boolean canAccessWorkspace(User oUser, Workspace oWorkspace) {
		if (oWorkspace == null) return false;
		if (oUser != null) return PermissionsUtils.canUserAccessWorkspace(oUser.getUserId(), oWorkspace.getWorkspaceId());
		return oWorkspace.isPublic();
	}

	/**
	 * Owned + shared + public Workspaces for a logged in user, or just the public ones for an
	 * anonymous caller, deduplicated by workspaceId.
	 */
	private List<Workspace> getAccessibleWorkspaces(User oUser) {

		Map<String, Workspace> oWorkspacesById = new HashMap<>();

		WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();

		if (oUser != null) {
			for (Workspace oWorkspace : oWorkspaceRepository.getWorkspaceByUser(oUser.getUserId())) {
				oWorkspacesById.put(oWorkspace.getWorkspaceId(), oWorkspace);
			}

			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
			for (UserResourcePermission oSharing : oUserResourcePermissionRepository.getWorkspaceSharingsByUserId(oUser.getUserId())) {
				if (oWorkspacesById.containsKey(oSharing.getResourceId())) continue;
				Workspace oSharedWorkspace = oWorkspaceRepository.getWorkspace(oSharing.getResourceId());
				if (oSharedWorkspace != null) oWorkspacesById.put(oSharedWorkspace.getWorkspaceId(), oSharedWorkspace);
			}
		}

		for (Workspace oWorkspace : oWorkspaceRepository.getWorkspacesList()) {
			if (oWorkspace.isPublic() && !oWorkspacesById.containsKey(oWorkspace.getWorkspaceId())) {
				oWorkspacesById.put(oWorkspace.getWorkspaceId(), oWorkspace);
			}
		}

		return new ArrayList<>(oWorkspacesById.values());
	}

	/**
	 * Builds a STAC Collection from a Workspace: extent is derived only from the bbox/refDate
	 * already stored in the db for its files - no on-the-fly geospatial computation.
	 */
	private StacCollection buildCollection(Workspace oWorkspace) {

		StacCollection oCollection = new StacCollection();
		oCollection.setId(oWorkspace.getWorkspaceId());
		oCollection.setTitle(oWorkspace.getName());
		oCollection.setDescription("WASDI Workspace \"" + oWorkspace.getName() + "\"");
		oCollection.setExtent(computeExtent(oWorkspace));

		String sSelfUrl = getBaseUrl() + "stac/collections/" + oWorkspace.getWorkspaceId();

		List<StacLink> aoLinks = new ArrayList<>();
		aoLinks.add(new StacLink(sSelfUrl, "self", MediaType.APPLICATION_JSON, "This document"));
		aoLinks.add(new StacLink(getBaseUrl() + "stac", "root", MediaType.APPLICATION_JSON, "Landing Page"));
		aoLinks.add(new StacLink(sSelfUrl + "/items", "items", MediaType.APPLICATION_JSON, "Items of this Collection"));
		oCollection.setLinks(aoLinks);

		return oCollection;
	}

	/**
	 * Aggregates the bbox/refDate already stored for a Workspace's files into a Collection extent.
	 * Falls back to the whole world / an open interval when no data is available, rather than
	 * running any geospatial computation.
	 */
	private StacExtent computeExtent(Workspace oWorkspace) {

		double dMinWest = Double.NaN, dMinSouth = Double.NaN, dMaxEast = Double.NaN, dMaxNorth = Double.NaN;
		String sMinDate = null, sMaxDate = null;

		try {
			String sWorkspacePath = PathsConfig.getWorkspacePath(oWorkspace.getUserId(), oWorkspace.getWorkspaceId());

			ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();
			List<ProductWorkspace> aoProducts = oProductWorkspaceRepository.getProductsByWorkspace(oWorkspace.getWorkspaceId());

			for (ProductWorkspace oProductWorkspace : aoProducts) {

				DownloadedFile oDownloadedFile = getDownloadedFile(sWorkspacePath, oProductWorkspace.getProductName());

				double[] adBbox = toStacBbox(!Utils.isNullOrEmpty(oProductWorkspace.getBbox()) ? oProductWorkspace.getBbox() : (oDownloadedFile != null ? oDownloadedFile.getBoundingBox() : null));

				if (adBbox != null) {
					dMinWest = Double.isNaN(dMinWest) ? adBbox[0] : Math.min(dMinWest, adBbox[0]);
					dMinSouth = Double.isNaN(dMinSouth) ? adBbox[1] : Math.min(dMinSouth, adBbox[1]);
					dMaxEast = Double.isNaN(dMaxEast) ? adBbox[2] : Math.max(dMaxEast, adBbox[2]);
					dMaxNorth = Double.isNaN(dMaxNorth) ? adBbox[3] : Math.max(dMaxNorth, adBbox[3]);
				}

				if (oDownloadedFile != null && !Utils.isNullOrEmpty(oDownloadedFile.getRefDate())) {
					String sRefDate = oDownloadedFile.getRefDate();
					if (sMinDate == null || sRefDate.compareTo(sMinDate) < 0) sMinDate = sRefDate;
					if (sMaxDate == null || sRefDate.compareTo(sMaxDate) > 0) sMaxDate = sRefDate;
				}
			}
		}
		catch (Exception oEx) {
			WasdiLog.warnLog("StacResource.computeExtent: exception aggregating extent for workspace " + oWorkspace.getWorkspaceId() + ": " + oEx.toString());
		}

		StacSpatialExtent oSpatialExtent = new StacSpatialExtent();
		List<List<Double>> aoBboxes = new ArrayList<>();
		if (!Double.isNaN(dMinWest)) {
			aoBboxes.add(Arrays.asList(dMinWest, dMinSouth, dMaxEast, dMaxNorth));
		}
		else {
			// No bbox data available in the db: declare the whole world rather than computing anything
			aoBboxes.add(Arrays.asList(-180.0, -90.0, 180.0, 90.0));
		}
		oSpatialExtent.setBbox(aoBboxes);

		StacTemporalExtent oTemporalExtent = new StacTemporalExtent();
		List<List<String>> aoIntervals = new ArrayList<>();
		aoIntervals.add(Arrays.asList(sMinDate, sMaxDate));
		oTemporalExtent.setInterval(aoIntervals);

		StacExtent oExtent = new StacExtent();
		oExtent.setSpatial(oSpatialExtent);
		oExtent.setTemporal(oTemporalExtent);

		return oExtent;
	}

	/**
	 * Builds a STAC Item from a ProductWorkspace (+ its DownloadedFile entry, if any, for extra metadata).
	 */
	private StacItem buildItem(ProductWorkspace oProductWorkspace, DownloadedFile oDownloadedFile, String sWorkspaceId, String sSessionId) {

		StacItem oItem = new StacItem();
		oItem.setId(oProductWorkspace.getProductName());
		oItem.setCollection(sWorkspaceId);

		double[] adBbox = toStacBbox(!Utils.isNullOrEmpty(oProductWorkspace.getBbox()) ? oProductWorkspace.getBbox() : (oDownloadedFile != null ? oDownloadedFile.getBoundingBox() : null));

		if (adBbox != null) {
			oItem.setBbox(Arrays.asList(adBbox[0], adBbox[1], adBbox[2], adBbox[3]));
			oItem.setGeometry(buildBboxGeometry(adBbox));
		}

		Map<String, Object> oProperties = new HashMap<>();
		if (oDownloadedFile != null && !Utils.isNullOrEmpty(oDownloadedFile.getRefDate())) {
			oProperties.put("datetime", oDownloadedFile.getRefDate());
		}
		else {
			oProperties.put("datetime", null);
		}
		if (oDownloadedFile != null && !Utils.isNullOrEmpty(oDownloadedFile.getPlatform())) {
			oProperties.put("platform", oDownloadedFile.getPlatform());
		}
		oItem.setProperties(oProperties);

		Map<String, StacAsset> oAssets = new HashMap<>();
		StacAsset oDataAsset = new StacAsset();
		oDataAsset.setHref(buildDownloadHref(sWorkspaceId, oProductWorkspace.getProductName(), sSessionId));
		oDataAsset.setTitle(oProductWorkspace.getProductName());
		oDataAsset.setRoles(Arrays.asList("data"));
		oAssets.put("data", oDataAsset);
		oItem.setAssets(oAssets);

		String sSelfUrl = getBaseUrl() + "stac/collections/" + sWorkspaceId + "/items/" + encodePathSegment(oProductWorkspace.getProductName());

		List<StacLink> aoLinks = new ArrayList<>();
		aoLinks.add(new StacLink(sSelfUrl, "self", MediaType.APPLICATION_JSON, "This document"));
		aoLinks.add(new StacLink(getBaseUrl() + "stac/collections/" + sWorkspaceId, "collection", MediaType.APPLICATION_JSON, "Parent Collection"));
		aoLinks.add(new StacLink(getBaseUrl() + "stac", "root", MediaType.APPLICATION_JSON, "Landing Page"));
		oItem.setLinks(aoLinks);

		return oItem;
	}

	/**
	 * Looks up the DownloadedFile entry for a product, if any: not every ProductWorkspace has one
	 * (e.g. entries created outside the ingest flow), so this can legitimately return null.
	 */
	private DownloadedFile getDownloadedFile(String sWorkspacePath, String sProductName) {
		try {
			DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
			return oDownloadedFilesRepository.getDownloadedFileByPath(sWorkspacePath + sProductName);
		}
		catch (Exception oEx) {
			WasdiLog.warnLog("StacResource.getDownloadedFile: exception " + oEx.toString());
			return null;
		}
	}

	/**
	 * Converts a WASDI "north,west,south,east" bbox string to a STAC/GeoJSON [west,south,east,north] array.
	 */
	private double[] toStacBbox(String sWasdiBbox) {
		if (Utils.isNullOrEmpty(sWasdiBbox)) return null;

		List<Double> adWasdiBbox = BoundingBoxUtils.parseBoundingBox(sWasdiBbox);
		if (adWasdiBbox == null) return null;

		double dNorth = adWasdiBbox.get(0), dWest = adWasdiBbox.get(1), dSouth = adWasdiBbox.get(2), dEast = adWasdiBbox.get(3);
		return new double[] { dWest, dSouth, dEast, dNorth };
	}

	/**
	 * Parses a STAC API "bbox" query param ("west,south,east,north") into a [west,south,east,north] array.
	 */
	private double[] parseStacBboxParam(String sBboxParam) {
		try {
			String[] asParts = sBboxParam.split(",");
			if (asParts.length != 4) return null;
			return new double[] { Double.parseDouble(asParts[0].trim()), Double.parseDouble(asParts[1].trim()), Double.parseDouble(asParts[2].trim()), Double.parseDouble(asParts[3].trim()) };
		}
		catch (Exception oEx) {
			WasdiLog.warnLog("StacResource.parseStacBboxParam: invalid bbox param " + sBboxParam);
			return null;
		}
	}

	private boolean bboxIntersects(double[] adFilterBbox, double[] adItemBbox) {
		if (adItemBbox == null) return false;
		return adFilterBbox[0] <= adItemBbox[2] && adFilterBbox[2] >= adItemBbox[0] && adFilterBbox[1] <= adItemBbox[3] && adFilterBbox[3] >= adItemBbox[1];
	}

	/**
	 * Matches a STAC API "datetime" filter (single instant, or "start/.."/"../end"/"start/end" interval)
	 * against a DownloadedFile's refDate. If we have no refDate to compare with, the Item is excluded.
	 */
	private boolean matchesDatetimeFilter(String[] asDatetimeFilter, DownloadedFile oDownloadedFile) {
		if (oDownloadedFile == null || Utils.isNullOrEmpty(oDownloadedFile.getRefDate())) return false;

		String sRefDate = oDownloadedFile.getRefDate();

		if (asDatetimeFilter.length == 1) return sRefDate.equals(asDatetimeFilter[0]);

		String sStart = asDatetimeFilter[0];
		String sEnd = asDatetimeFilter[1];

		if (!Utils.isNullOrEmpty(sStart) && !"..".equals(sStart) && sRefDate.compareTo(sStart) < 0) return false;
		if (!Utils.isNullOrEmpty(sEnd) && !"..".equals(sEnd) && sRefDate.compareTo(sEnd) > 0) return false;

		return true;
	}

	/**
	 * Builds a rectangular GeoJSON Polygon geometry out of a [west,south,east,north] bbox.
	 */
	private StacGeometry buildBboxGeometry(double[] adBbox) {
		double dWest = adBbox[0], dSouth = adBbox[1], dEast = adBbox[2], dNorth = adBbox[3];

		List<List<Double>> aoRing = Arrays.asList(
				Arrays.asList(dWest, dSouth),
				Arrays.asList(dEast, dSouth),
				Arrays.asList(dEast, dNorth),
				Arrays.asList(dWest, dNorth),
				Arrays.asList(dWest, dSouth));

		return new StacGeometry("Polygon", Arrays.asList(aoRing));
	}

	/**
	 * Builds a download href reusing the existing catalog download endpoint, embedding the caller's
	 * own session token (v1 limitation: the link is only valid as long as that session is).
	 */
	private String buildDownloadHref(String sWorkspaceId, String sFileName, String sSessionId) {
		try {
			String sHref = getBaseUrl() + "catalog/downloadbyname?filename=" + URLEncoder.encode(sFileName, StandardCharsets.UTF_8.toString())
					+ "&workspace=" + URLEncoder.encode(sWorkspaceId, StandardCharsets.UTF_8.toString());

			if (!Utils.isNullOrEmpty(sSessionId)) {
				sHref += "&token=" + URLEncoder.encode(sSessionId, StandardCharsets.UTF_8.toString());
			}

			return sHref;
		}
		catch (Exception oEx) {
			WasdiLog.warnLog("StacResource.buildDownloadHref: exception " + oEx.toString());
			return null;
		}
	}

	private String encodePathSegment(String sSegment) {
		try {
			return URLEncoder.encode(sSegment, StandardCharsets.UTF_8.toString());
		}
		catch (Exception oEx) {
			return sSegment;
		}
	}

	private String getBaseUrl() {
		String sUrl = WasdiConfig.Current.baseUrl;
		if (!sUrl.endsWith("/")) sUrl += "/";
		return sUrl;
	}
}
