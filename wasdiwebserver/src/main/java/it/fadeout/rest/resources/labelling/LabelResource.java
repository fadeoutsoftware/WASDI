package it.fadeout.rest.resources.labelling;

import java.util.ArrayList;
import java.util.List;

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
import wasdi.shared.business.labelling.Attribute;
import wasdi.shared.business.labelling.DatasetProject;
import wasdi.shared.business.labelling.Label;
import wasdi.shared.business.users.User;
import wasdi.shared.data.labelling.DatasetProjectRepository;
import wasdi.shared.data.labelling.LabelRepository;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.ClientMessageCodes;
import wasdi.shared.viewmodels.ErrorResponse;
import wasdi.shared.viewmodels.labelling.attributes.AttributeViewModel;
import wasdi.shared.viewmodels.labelling.labels.LabelViewModel;

@Path("/labelling/labels")
public class LabelResource {

	@GET
	@Path("/byimage")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response getByImage(@HeaderParam("x-session-token") String sSessionId, @QueryParam("imageName") String sImageName) {

		WasdiLog.debugLog("LabelResource.getByImage");

		User oUser = Wasdi.getUserFromSession(sSessionId);
		List<LabelViewModel> aoLabelsViewModels = new ArrayList<>();

		if (oUser == null) {
			WasdiLog.warnLog("LabelResource.getByImage: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}

		if (Utils.isNullOrEmpty(sImageName)) {
			WasdiLog.warnLog("LabelResource.getByImage: invalid image name");
			return Response.status(Status.BAD_REQUEST).build();
		}

		try {
			DatasetProject oDataset = getReadableDatasetForImage(oUser, sImageName);
			if (oDataset == null) {
				WasdiLog.warnLog("LabelResource.getByImage: user cannot access the label dataset");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			LabelRepository oLabelRepository = new LabelRepository();
			List<Label> aoLabels = oLabelRepository.getLabelsByImage(sImageName);

			if (aoLabels == null) {
				return Response.ok(aoLabelsViewModels).build();
			}

			for (Label oLabel : aoLabels) {
				aoLabelsViewModels.add(toViewModel(oLabel));
			}

			return Response.ok(aoLabelsViewModels).build();
		} catch (Exception oEx) {
			WasdiLog.errorLog("LabelResource.getByImage error: " + oEx);
			return Response.serverError().build();
		}
	}

	@GET
	@Path("/")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response getById(@HeaderParam("x-session-token") String sSessionId, @QueryParam("labelId") String sLabelId) {

		WasdiLog.debugLog("LabelResource.getById");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.warnLog("LabelResource.getById: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}

		if (Utils.isNullOrEmpty(sLabelId)) {
			WasdiLog.warnLog("LabelResource.getById: invalid label id");
			return Response.status(Status.BAD_REQUEST).build();
		}

		try {
			LabelRepository oLabelRepository = new LabelRepository();
			Label oLabel = oLabelRepository.getLabel(sLabelId);

			if (oLabel == null) {
				WasdiLog.warnLog("LabelResource.getById: label not found");
				return Response.status(Status.BAD_REQUEST).build();
			}

			DatasetProject oDataset = getReadableDatasetForImage(oUser, oLabel.getImage());
			if (oDataset == null) {
				WasdiLog.warnLog("LabelResource.getById: user cannot access the label dataset");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			return Response.ok(toViewModel(oLabel)).build();
		} catch (Exception oEx) {
			WasdiLog.errorLog("LabelResource.getById error: " + oEx);
			return Response.serverError().build();
		}
	}

	@POST
	@Path("/")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response create(@HeaderParam("x-session-token") String sSessionId, LabelViewModel oLabelViewModel) {

		WasdiLog.debugLog("LabelResource.create");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.warnLog("LabelResource.create: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}

		if (oLabelViewModel == null || Utils.isNullOrEmpty(oLabelViewModel.image)) {
			WasdiLog.warnLog("LabelResource.create: invalid view model");
			return Response.status(Status.BAD_REQUEST).build();
		}

		try {
			DatasetProject oDataset = getWritableDatasetForImage(oUser, oLabelViewModel.image);
			if (oDataset == null) {
				WasdiLog.warnLog("LabelResource.create: user cannot edit the label dataset");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			Label oLabel = toEntity(oLabelViewModel);
			oLabel.setId(Utils.getRandomName());
			oLabel.setAnnotator(oUser.getUserId());

			LabelRepository oLabelRepository = new LabelRepository();
			oLabelRepository.insertLabel(oLabel);

			return Response.ok(oLabel.getId()).build();
		} catch (Exception oEx) {
			WasdiLog.errorLog("LabelResource.create error: " + oEx);
			return Response.serverError().build();
		}
	}

	@PUT
	@Path("/")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response update(@HeaderParam("x-session-token") String sSessionId, LabelViewModel oLabelViewModel) {

		WasdiLog.debugLog("LabelResource.update");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.warnLog("LabelResource.update: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}

		if (oLabelViewModel == null || Utils.isNullOrEmpty(oLabelViewModel.id)) {
			WasdiLog.warnLog("LabelResource.update: invalid view model");
			return Response.status(Status.BAD_REQUEST).build();
		}

		try {
			LabelRepository oLabelRepository = new LabelRepository();
			Label oStoredLabel = oLabelRepository.getLabel(oLabelViewModel.id);

			if (oStoredLabel == null) {
				WasdiLog.warnLog("LabelResource.update: label not found");
				return Response.status(Status.BAD_REQUEST).build();
			}

			String sTargetImageId = Utils.isNullOrEmpty(oLabelViewModel.image) ? oStoredLabel.getImage() : oLabelViewModel.image;
			DatasetProject oDataset = getWritableDatasetForImage(oUser, sTargetImageId);
			if (oDataset == null) {
				WasdiLog.warnLog("LabelResource.update: user cannot edit the label dataset");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			updateEntity(oStoredLabel, oLabelViewModel);
			oLabelRepository.updateLabel(oStoredLabel);

			return Response.ok().build();
		} catch (Exception oEx) {
			WasdiLog.errorLog("LabelResource.update error: " + oEx);
			return Response.serverError().build();
		}
	}

	@DELETE
	@Path("/")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response delete(@HeaderParam("x-session-token") String sSessionId, @QueryParam("labelId") String sLabelId) {

		WasdiLog.debugLog("LabelResource.delete");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.warnLog("LabelResource.delete: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}

		if (Utils.isNullOrEmpty(sLabelId)) {
			WasdiLog.warnLog("LabelResource.delete: invalid label id");
			return Response.status(Status.BAD_REQUEST).build();
		}

		try {
			LabelRepository oLabelRepository = new LabelRepository();
			Label oLabel = oLabelRepository.getLabel(sLabelId);

			if (oLabel == null) {
				WasdiLog.warnLog("LabelResource.delete: label not found");
				return Response.status(Status.BAD_REQUEST).build();
			}

			DatasetProject oDataset = getWritableDatasetForImage(oUser, oLabel.getImage());
			if (oDataset == null) {
				WasdiLog.warnLog("LabelResource.delete: user cannot edit the label dataset");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			oLabelRepository.deleteLabel(sLabelId);

			return Response.ok().build();
		} catch (Exception oEx) {
			WasdiLog.errorLog("LabelResource.delete error: " + oEx);
			return Response.serverError().build();
		}
	}

	private DatasetProject getReadableDatasetForImage(User oUser, String sImageId) {
		DatasetProject oDataset = getDatasetByImageId(sImageId);

		if (oDataset == null) {
			return null;
		}

		if (oDataset.getOwner().equals(oUser.getUserId()) || oDataset.isPublic() || isCollaborator(oDataset, oUser.getUserId())) {
			return oDataset;
		}

		return null;
	}

	private DatasetProject getWritableDatasetForImage(User oUser, String sImageId) {
		DatasetProject oDataset = getDatasetByImageId(sImageId);

		if (oDataset == null) {
			return null;
		}

		if (oDataset.getOwner().equals(oUser.getUserId()) || isCollaborator(oDataset, oUser.getUserId())) {
			return oDataset;
		}

		return null;
	}

	private DatasetProject getDatasetByImageId(String sImageId) {
		if (Utils.isNullOrEmpty(sImageId)) {
			return null;
		}

		DatasetProjectRepository oDatasetRepository = new DatasetProjectRepository();
		for (DatasetProject oDataset : oDatasetRepository.getAll()) {
			if (oDataset != null && oDataset.getImagesIds() != null && oDataset.getImagesIds().contains(sImageId)) {
				return oDataset;
			}
		}

		return null;
	}

	private boolean isCollaborator(DatasetProject oDataset, String sUserId) {
		return oDataset.getAnnotators().contains(sUserId) || oDataset.getReviewers().contains(sUserId);
	}

	private LabelViewModel toViewModel(Label oLabel) {
		LabelViewModel oLabelViewModel = new LabelViewModel();
		oLabelViewModel.id = oLabel.getId();
		oLabelViewModel.geometry = oLabel.getGeometry();
		oLabelViewModel.isPoint = oLabel.isPoint();
		oLabelViewModel.isLine = oLabel.isLine();
		oLabelViewModel.isPolygon = oLabel.isPolygon();
		oLabelViewModel.isMultiPolygon = oLabel.isMultiPolygon();
		oLabelViewModel.annotator = oLabel.getAnnotator();
		oLabelViewModel.image = oLabel.getImage();
		oLabelViewModel.reviewers.addAll(oLabel.getReviewers());
		oLabelViewModel.reviewNotes.addAll(oLabel.getReviewNotes());
		for (Attribute oAttribute : oLabel.getAttributes()) {
			oLabelViewModel.attributes.add(AttributeViewModel.getFromEntity(oAttribute));
		}
		return oLabelViewModel;
	}

	private Label toEntity(LabelViewModel oLabelViewModel) {
		Label oLabel = new Label();
		updateEntity(oLabel, oLabelViewModel);
		return oLabel;
	}

	private void updateEntity(Label oLabel, LabelViewModel oLabelViewModel) {
		oLabel.setGeometry(oLabelViewModel.geometry);
		oLabel.setPoint(oLabelViewModel.isPoint);
		oLabel.setLine(oLabelViewModel.isLine);
		oLabel.setPolygon(oLabelViewModel.isPolygon);
		oLabel.setMultiPolygon(oLabelViewModel.isMultiPolygon);
		oLabel.setImage(oLabelViewModel.image);
		oLabel.getReviewers().clear();
		if (oLabelViewModel.reviewers != null) {
			oLabel.getReviewers().addAll(oLabelViewModel.reviewers);
		}
		oLabel.getReviewNotes().clear();
		if (oLabelViewModel.reviewNotes != null) {
			oLabel.getReviewNotes().addAll(oLabelViewModel.reviewNotes);
		}
		oLabel.getAttributes().clear();
		if (oLabelViewModel.attributes != null) {
			for (AttributeViewModel oAttributeViewModel : oLabelViewModel.attributes) {
				oLabel.getAttributes().add(AttributeViewModel.convertToEntity(oAttributeViewModel));
			}
		}
	}
}
