package it.fadeout.rest.resources.labelling;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import it.fadeout.Wasdi;
import wasdi.shared.business.labelling.DatasetProject;
import wasdi.shared.business.labelling.ImageStyle;
import wasdi.shared.business.users.User;
import wasdi.shared.data.labelling.DatasetProjectRepository;
import wasdi.shared.data.labelling.ImageStyleRepository;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.ClientMessageCodes;
import wasdi.shared.viewmodels.ErrorResponse;
import wasdi.shared.viewmodels.labelling.imagestyles.ImageStyleViewModel;

@Path("/labelling/sytles")
public class ImageStyleResource {

	@GET
	@Path("/")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response getById(@HeaderParam("x-session-token") String sSessionId, @QueryParam("styleId") String sStyleId) {

		WasdiLog.debugLog("ImageStyleResource.getById");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.warnLog("ImageStyleResource.getById: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}

		if (Utils.isNullOrEmpty(sStyleId)) {
			WasdiLog.warnLog("ImageStyleResource.getById: invalid image style id");
			return Response.status(Status.BAD_REQUEST).build();
		}

		try {
			ImageStyleRepository oImageStyleRepository = new ImageStyleRepository();
			ImageStyle oImageStyle = oImageStyleRepository.getImageStyle(sStyleId);

			if (oImageStyle == null) {
				WasdiLog.warnLog("ImageStyleResource.getById: image style not found");
				return Response.status(Status.BAD_REQUEST).build();
			}

			DatasetProject oDataset = getReadableDataset(oUser, oImageStyle.getDatasetId());
			if (oDataset == null) {
				WasdiLog.warnLog("ImageStyleResource.getById: user cannot access the dataset");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			return Response.ok(toViewModel(oImageStyle)).build();
		} catch (Exception oEx) {
			WasdiLog.errorLog("ImageStyleResource.getById error: " + oEx);
			return Response.serverError().build();
		}
	}

	@POST
	@Path("/")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response create(@HeaderParam("x-session-token") String sSessionId, ImageStyleViewModel oImageStyleViewModel) {

		WasdiLog.debugLog("ImageStyleResource.create");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.warnLog("ImageStyleResource.create: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}

		if (oImageStyleViewModel == null || Utils.isNullOrEmpty(oImageStyleViewModel.datasetId)) {
			WasdiLog.warnLog("ImageStyleResource.create: invalid view model");
			return Response.status(Status.BAD_REQUEST).build();
		}

		try {
			DatasetProject oDataset = getOwnedDataset(oUser, oImageStyleViewModel.datasetId);
			if (oDataset == null) {
				WasdiLog.warnLog("ImageStyleResource.create: user cannot edit the dataset");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			ImageStyle oImageStyle = toEntity(oImageStyleViewModel);
			oImageStyle.setId(Utils.getRandomName());

			ImageStyleRepository oImageStyleRepository = new ImageStyleRepository();
			oImageStyleRepository.insertImageStyle(oImageStyle);

			return Response.ok(oImageStyle.getId()).build();
		} catch (Exception oEx) {
			WasdiLog.errorLog("ImageStyleResource.create error: " + oEx);
			return Response.serverError().build();
		}
	}

	@PUT
	@Path("/")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response update(@HeaderParam("x-session-token") String sSessionId, ImageStyleViewModel oImageStyleViewModel) {

		WasdiLog.debugLog("ImageStyleResource.update");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.warnLog("ImageStyleResource.update: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}

		if (oImageStyleViewModel == null || Utils.isNullOrEmpty(oImageStyleViewModel.id)) {
			WasdiLog.warnLog("ImageStyleResource.update: invalid view model");
			return Response.status(Status.BAD_REQUEST).build();
		}

		try {
			ImageStyleRepository oImageStyleRepository = new ImageStyleRepository();
			ImageStyle oStoredImageStyle = oImageStyleRepository.getImageStyle(oImageStyleViewModel.id);

			if (oStoredImageStyle == null) {
				WasdiLog.warnLog("ImageStyleResource.update: image style not found");
				return Response.status(Status.BAD_REQUEST).build();
			}

			DatasetProject oDataset = getOwnedDataset(oUser, oStoredImageStyle.getDatasetId());
			if (oDataset == null) {
				WasdiLog.warnLog("ImageStyleResource.update: user cannot edit the dataset");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			updateEntity(oStoredImageStyle, oImageStyleViewModel);
			oImageStyleRepository.updateImageStyle(oStoredImageStyle);

			return Response.ok().build();
		} catch (Exception oEx) {
			WasdiLog.errorLog("ImageStyleResource.update error: " + oEx);
			return Response.serverError().build();
		}
	}

	@DELETE
	@Path("/")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response delete(@HeaderParam("x-session-token") String sSessionId, @QueryParam("imageStyleId") String sImageStyleId) {

		WasdiLog.debugLog("ImageStyleResource.delete");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.warnLog("ImageStyleResource.delete: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}

		if (Utils.isNullOrEmpty(sImageStyleId)) {
			WasdiLog.warnLog("ImageStyleResource.delete: invalid image style id");
			return Response.status(Status.BAD_REQUEST).build();
		}

		try {
			ImageStyleRepository oImageStyleRepository = new ImageStyleRepository();
			ImageStyle oImageStyle = oImageStyleRepository.getImageStyle(sImageStyleId);

			if (oImageStyle == null) {
				WasdiLog.warnLog("ImageStyleResource.delete: image style not found");
				return Response.status(Status.BAD_REQUEST).build();
			}

			DatasetProject oDataset = getOwnedDataset(oUser, oImageStyle.getDatasetId());
			if (oDataset == null) {
				WasdiLog.warnLog("ImageStyleResource.delete: user cannot edit the dataset");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			oImageStyleRepository.deleteImageStyle(sImageStyleId);

			return Response.ok().build();
		} catch (Exception oEx) {
			WasdiLog.errorLog("ImageStyleResource.delete error: " + oEx);
			return Response.serverError().build();
		}
	}

	private DatasetProject getReadableDataset(User oUser, String sDatasetId) {
		DatasetProjectRepository oDatasetRepository = new DatasetProjectRepository();
		DatasetProject oDataset = oDatasetRepository.getDataset(sDatasetId);

		if (oDataset == null) {
			return null;
		}

		if (oDataset.getOwner().equals(oUser.getUserId()) || oDataset.isPublic()) {
			return oDataset;
		}

		return null;
	}

	private DatasetProject getOwnedDataset(User oUser, String sDatasetId) {
		DatasetProjectRepository oDatasetRepository = new DatasetProjectRepository();
		DatasetProject oDataset = oDatasetRepository.getDataset(sDatasetId);

		if (oDataset == null) {
			return null;
		}

		if (oDataset.getOwner().equals(oUser.getUserId())) {
			return oDataset;
		}

		return null;
	}

	private ImageStyleViewModel toViewModel(ImageStyle oImageStyle) {
		ImageStyleViewModel oImageStyleViewModel = new ImageStyleViewModel();
		oImageStyleViewModel.id = oImageStyle.getId();
		oImageStyleViewModel.datasetId = oImageStyle.getDatasetId();
		oImageStyleViewModel.singleBand = oImageStyle.isSingleBand();
		oImageStyleViewModel.band1 = oImageStyle.getBand1();
		oImageStyleViewModel.band2 = oImageStyle.getBand2();
		oImageStyleViewModel.band3 = oImageStyle.getBand3();
		oImageStyleViewModel.brightness = oImageStyle.getBrightness();
		oImageStyleViewModel.contrast = oImageStyle.getContrast();
		oImageStyleViewModel.hue = oImageStyle.getHue();
		oImageStyleViewModel.saturation = oImageStyle.getSaturation();
		oImageStyleViewModel.lightness = oImageStyle.getLightness();
		oImageStyleViewModel.autoLevel = oImageStyle.isAutoLevel();
		oImageStyleViewModel.saturateLevel = oImageStyle.isSaturateLevel();
		oImageStyleViewModel.saturationValue = oImageStyle.getSaturationValue();
		return oImageStyleViewModel;
	}

	private ImageStyle toEntity(ImageStyleViewModel oImageStyleViewModel) {
		ImageStyle oImageStyle = new ImageStyle();
		oImageStyle.setDatasetId(oImageStyleViewModel.datasetId);
		updateEntity(oImageStyle, oImageStyleViewModel);
		return oImageStyle;
	}

	private void updateEntity(ImageStyle oImageStyle, ImageStyleViewModel oImageStyleViewModel) {
		oImageStyle.setSingleBand(oImageStyleViewModel.singleBand);
		oImageStyle.setBand1(oImageStyleViewModel.band1);
		oImageStyle.setBand2(oImageStyleViewModel.band2);
		oImageStyle.setBand3(oImageStyleViewModel.band3);
		oImageStyle.setBrightness(oImageStyleViewModel.brightness);
		oImageStyle.setContrast(oImageStyleViewModel.contrast);
		oImageStyle.setHue(oImageStyleViewModel.hue);
		oImageStyle.setSaturation(oImageStyleViewModel.saturation);
		oImageStyle.setLightness(oImageStyleViewModel.lightness);
		oImageStyle.setAutoLevel(oImageStyleViewModel.autoLevel);
		oImageStyle.setSaturateLevel(oImageStyleViewModel.saturateLevel);
		oImageStyle.setSaturationValue(oImageStyleViewModel.saturationValue);
	}
}
