package it.fadeout.rest.resources.labelling;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
import wasdi.shared.business.labelling.ReviewNote;
import wasdi.shared.business.users.User;
import wasdi.shared.data.labelling.DatasetProjectRepository;
import wasdi.shared.data.labelling.LabelRepository;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.ClientMessageCodes;
import wasdi.shared.viewmodels.ErrorResponse;
import wasdi.shared.viewmodels.labelling.attributes.AttributeViewModel;
import wasdi.shared.viewmodels.labelling.labels.LabelViewModel;
import wasdi.shared.viewmodels.labelling.labels.NoteRequestViewModel;
import wasdi.shared.viewmodels.labelling.labels.ResolveNoteRequestViewModel;
import wasdi.shared.viewmodels.labelling.labels.ReviewNoteViewModel;

@Path("/labelling/labels")
public class LabelResource {

	@GET
	@Path("/byimage")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response getByImage(@HeaderParam("x-session-token") String sSessionId, @QueryParam("datasetId") String sDatasetId, @QueryParam("imageName") String sImageName) {

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
			DatasetProject oDataset = getReadableDataset(oUser, sDatasetId);
			if (oDataset == null) {
				WasdiLog.warnLog("LabelResource.getByImage: user cannot access the label dataset");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			LabelRepository oLabelRepository = new LabelRepository();
			List<Label> aoLabels = oLabelRepository.getLabelsByImage(sDatasetId, sImageName);

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

			DatasetProject oDataset = getReadableDataset(oUser, oLabel.getImage());
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
			DatasetProject oDataset = getWritableDatasetForImage(oUser, oLabelViewModel.datasetId);
			if (oDataset == null) {
				WasdiLog.warnLog("LabelResource.create: user cannot edit the label dataset");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			Label oLabel = toEntity(oLabelViewModel);
			oLabel.setId(Utils.getRandomName());
			oLabel.setAnnotator(oUser.getUserId());
			oLabel.setCreatorId(oUser.getUserId());

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

			DatasetProject oDataset = getWritableDatasetForImage(oUser, oLabelViewModel.datasetId);
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

			DatasetProject oDataset = getWritableDatasetForImage(oUser, oLabel.getDatasetId());
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

	@GET
	@Path("/approve")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response approve(@HeaderParam("x-session-token") String sSessionId, @QueryParam("labelId") String sLabelId) {

		WasdiLog.debugLog("LabelResource.approve");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.warnLog("LabelResource.approve: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}

		if (Utils.isNullOrEmpty(sLabelId)) {
			WasdiLog.warnLog("LabelResource.approve: invalid label id");
			return Response.status(Status.BAD_REQUEST).build();
		}

		try {
			LabelRepository oLabelRepository = new LabelRepository();
			Label oLabel = oLabelRepository.getLabel(sLabelId);

			if (oLabel == null) {
				WasdiLog.warnLog("LabelResource.approve: label not found");
				return Response.status(Status.NOT_FOUND).build();
			}

			// Check authorization: user must be owner or reviewer of the dataset
			DatasetProject oDataset = getWritableDatasetForImage(oUser, oLabel.getDatasetId());
			if (oDataset == null) {
				WasdiLog.warnLog("LabelResource.approve: dataset not found");
				return Response.status(Status.NOT_FOUND).build();
			}
			
			if (oDataset.isReviewRequired() == false) {				
				WasdiLog.warnLog("LabelResource.approve: dataset does not requires review");
				return Response.status(Status.BAD_REQUEST).build();
			}

			boolean bIsOwnerOrReviewer = oDataset.getOwner().equals(oUser.getUserId()) || oDataset.getReviewers().contains(oUser.getUserId());
			if (!bIsOwnerOrReviewer) {
				WasdiLog.warnLog("LabelResource.approve: user is not owner or reviewer");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			// Add reviewer if not already present
			ArrayList<String> aoCurrentReviewers = oLabel.getReviewers();
			if (aoCurrentReviewers == null) {
				aoCurrentReviewers = new ArrayList<>();
			}

			if (!aoCurrentReviewers.contains(oUser.getUserId())) {
				aoCurrentReviewers.add(oUser.getUserId());
				oLabel.setReviewers(aoCurrentReviewers);
				oLabel.setReviewCount(oLabel.getReviewCount() + 1);

				// Check if validation threshold is met
				int iMinReviewCount = oDataset.getMinReviewCount();
				if (oLabel.getReviewCount() >= iMinReviewCount) {
					oLabel.setValidated(true);
				}

				oLabelRepository.updateLabel(oLabel);
			}

			LabelViewModel oViewModel = new LabelViewModel();
			oViewModel.id = sLabelId;
			oViewModel.isValidated = oLabel.isValidated();
			oViewModel.reviewCount = oLabel.getReviewCount();

			return Response.ok(oViewModel).build();
		} catch (Exception oEx) {
			WasdiLog.errorLog("LabelResource.approve error: " + oEx);
			return Response.serverError().build();
		}
	}

	@GET
	@Path("/reject")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response reject(@HeaderParam("x-session-token") String sSessionId, @QueryParam("labelId") String sLabelId) {

		WasdiLog.debugLog("LabelResource.reject");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.warnLog("LabelResource.reject: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}

		if (Utils.isNullOrEmpty(sLabelId)) {
			WasdiLog.warnLog("LabelResource.reject: invalid label id");
			return Response.status(Status.BAD_REQUEST).build();
		}

		try {
			LabelRepository oLabelRepository = new LabelRepository();
			Label oLabel = oLabelRepository.getLabel(sLabelId);

			if (oLabel == null) {
				WasdiLog.warnLog("LabelResource.reject: label not found");
				return Response.status(Status.NOT_FOUND).build();
			}

			// Check authorization: user must be owner or reviewer of the dataset
			DatasetProject oDataset = getWritableDatasetForImage(oUser, oLabel.getDatasetId());
			if (oDataset == null) {
				WasdiLog.warnLog("LabelResource.reject: dataset not found");
				return Response.status(Status.NOT_FOUND).build();
			}
			
			if (oDataset.isReviewRequired() == false) {				
				WasdiLog.warnLog("LabelResource.approve: dataset does not requires review");
				return Response.status(Status.BAD_REQUEST).build();
			}			

			boolean bIsOwnerOrReviewer = oDataset.getOwner().equals(oUser.getUserId()) || oDataset.getReviewers().contains(oUser.getUserId());
			if (!bIsOwnerOrReviewer) {
				WasdiLog.warnLog("LabelResource.reject: user is not owner or reviewer");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			oLabel.setValidated(false);
			oLabelRepository.updateLabel(oLabel);

			LabelViewModel oViewModel = new LabelViewModel();
			oViewModel.id = sLabelId;

			return Response.ok(oViewModel).build();
		} catch (Exception oEx) {
			WasdiLog.errorLog("LabelResource.reject error: " + oEx);
			return Response.serverError().build();
		}
	}

	@POST
	@Path("/addNote")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response addNote(@HeaderParam("x-session-token") String sSessionId, NoteRequestViewModel oRequest) {

		WasdiLog.debugLog("LabelResource.addNote");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.warnLog("LabelResource.addNote: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}

		if (oRequest == null || Utils.isNullOrEmpty(oRequest.labelId) || Utils.isNullOrEmpty(oRequest.note)) {
			WasdiLog.warnLog("LabelResource.addNote: invalid request");
			return Response.status(Status.BAD_REQUEST).build();
		}

		try {
			LabelRepository oLabelRepository = new LabelRepository();
			Label oLabel = oLabelRepository.getLabel(oRequest.labelId);

			if (oLabel == null) {
				WasdiLog.warnLog("LabelResource.addNote: label not found");
				return Response.status(Status.NOT_FOUND).build();
			}

			// Check authorization: user must be owner or reviewer of the dataset
			DatasetProject oDataset = getWritableDatasetForImage(oUser, oLabel.getDatasetId());
			if (oDataset == null) {
				WasdiLog.warnLog("LabelResource.addNote: dataset not found");
				return Response.status(Status.NOT_FOUND).build();
			}

			boolean bIsOwnerOrReviewer = oDataset.getOwner().equals(oUser.getUserId()) || oDataset.getReviewers().contains(oUser.getUserId());
			if (!bIsOwnerOrReviewer) {
				WasdiLog.warnLog("LabelResource.addNote: user is not owner or reviewer");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			ArrayList<ReviewNote> aoCurrentNotes = oLabel.getReviewNotes();
			if (aoCurrentNotes == null) {
				aoCurrentNotes = new ArrayList<>();
			}

			ReviewNote oNewNote = new ReviewNote();
			oNewNote.setId(UUID.randomUUID().toString());
			oNewNote.setSender(oUser.getUserId());
			oNewNote.setNote(oRequest.note);
			oNewNote.setResolved(false);

			aoCurrentNotes.add(oNewNote);
			oLabel.setReviewNotes(aoCurrentNotes);
			oLabelRepository.updateLabel(oLabel);

			ReviewNoteViewModel oNoteViewModel = new ReviewNoteViewModel();
			oNoteViewModel.id = oNewNote.getId();
			oNoteViewModel.sender = oNewNote.getSender();
			oNoteViewModel.note = oNewNote.getNote();
			oNoteViewModel.resolved = oNewNote.isResolved();

			return Response.ok(oNoteViewModel).build();
		} catch (Exception oEx) {
			WasdiLog.errorLog("LabelResource.addNote error: " + oEx);
			return Response.serverError().build();
		}
	}

	@POST
	@Path("/resolveNote")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response resolveNote(@HeaderParam("x-session-token") String sSessionId, ResolveNoteRequestViewModel oRequest) {

		WasdiLog.debugLog("LabelResource.resolveNote");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.warnLog("LabelResource.resolveNote: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}

		if (oRequest == null || Utils.isNullOrEmpty(oRequest.labelId) || Utils.isNullOrEmpty(oRequest.noteId)) {
			WasdiLog.warnLog("LabelResource.resolveNote: invalid request");
			return Response.status(Status.BAD_REQUEST).build();
		}

		try {
			LabelRepository oLabelRepository = new LabelRepository();
			Label oLabel = oLabelRepository.getLabel(oRequest.labelId);

			if (oLabel == null) {
				WasdiLog.warnLog("LabelResource.resolveNote: label not found");
				return Response.status(Status.NOT_FOUND).build();
			}

			// Check authorization: user must be owner or reviewer of the dataset
			DatasetProject oDataset = getWritableDatasetForImage(oUser, oLabel.getDatasetId());
			if (oDataset == null) {
				WasdiLog.warnLog("LabelResource.resolveNote: dataset not found");
				return Response.status(Status.NOT_FOUND).build();
			}

			boolean bIsOwnerOrReviewer = oDataset.getOwner().equals(oUser.getUserId()) || oDataset.getReviewers().contains(oUser.getUserId());
			if (!bIsOwnerOrReviewer) {
				WasdiLog.warnLog("LabelResource.resolveNote: user is not owner or reviewer");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			ArrayList<ReviewNote> aoCurrentNotes = oLabel.getReviewNotes();
			if (aoCurrentNotes == null) {
				WasdiLog.warnLog("LabelResource.resolveNote: no notes found");
				return Response.status(Status.NOT_FOUND).build();
			}

			// Find and mark the note as resolved
			boolean bFound = false;
			for (ReviewNote oNote : aoCurrentNotes) {
				if (oNote.getId().equals(oRequest.noteId)) {
					oNote.setResolved(true);
					bFound = true;
					break;
				}
			}

			if (!bFound) {
				WasdiLog.warnLog("LabelResource.resolveNote: note not found");
				return Response.status(Status.NOT_FOUND).build();
			}

			oLabel.setReviewNotes(aoCurrentNotes);
			oLabelRepository.updateLabel(oLabel);

			return Response.ok().build();
		} catch (Exception oEx) {
			WasdiLog.errorLog("LabelResource.resolveNote error: " + oEx);
			return Response.serverError().build();
		}
	}

	private DatasetProject getReadableDataset(User oUser, String sDatasetId) {
		DatasetProjectRepository oDatasetRepository = new DatasetProjectRepository();
		DatasetProject oDataset = oDatasetRepository.getDataset(sDatasetId);

		if (oDataset == null) {
			return null;
		}

		if (oDataset.getOwner().equals(oUser.getUserId()) || oDataset.isPublic() || isCollaborator(oDataset, oUser.getUserId())) {
			return oDataset;
		}

		return null;
	}

	private DatasetProject getWritableDatasetForImage(User oUser, String sDatasetId) {
		DatasetProjectRepository oDatasetRepository = new DatasetProjectRepository();
		DatasetProject oDataset = oDatasetRepository.getDataset(sDatasetId);

		if (oDataset == null) {
			return null;
		}

		if (oDataset.getOwner().equals(oUser.getUserId()) || isCollaborator(oDataset, oUser.getUserId())) {
			return oDataset;
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
		oLabelViewModel.reviewCount = oLabel.getReviewCount();
		oLabelViewModel.isValidated = oLabel.isValidated();
		oLabelViewModel.creatorId = oLabel.getCreatorId();
		oLabelViewModel.reviewers.addAll(oLabel.getReviewers());
		oLabelViewModel.datasetId = oLabel.getDatasetId();
		
		for (ReviewNote oReviewNote : oLabel.getReviewNotes()) {
			ReviewNoteViewModel oNoteViewModel = new ReviewNoteViewModel();
			oNoteViewModel.id = oReviewNote.getId();
			oNoteViewModel.sender = oReviewNote.getSender();
			oNoteViewModel.note = oReviewNote.getNote();
			oNoteViewModel.resolved = oReviewNote.isResolved();
			oLabelViewModel.reviewNotes.add(oNoteViewModel);
		}
		
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
		oLabel.setReviewCount(oLabelViewModel.reviewCount);
		oLabel.setValidated(oLabelViewModel.isValidated);
		oLabel.setCreatorId(oLabelViewModel.creatorId);
		oLabel.setDatasetId(oLabelViewModel.datasetId);
		
		oLabel.getReviewers().clear();
		if (oLabelViewModel.reviewers != null) {
			oLabel.getReviewers().addAll(oLabelViewModel.reviewers);
		}
		
		oLabel.getReviewNotes().clear();
		if (oLabelViewModel.reviewNotes != null) {
			for (ReviewNoteViewModel oNoteViewModel : oLabelViewModel.reviewNotes) {
				ReviewNote oReviewNote = new ReviewNote();
				oReviewNote.setId(oNoteViewModel.id);
				oReviewNote.setSender(oNoteViewModel.sender);
				oReviewNote.setNote(oNoteViewModel.note);
				oReviewNote.setResolved(oNoteViewModel.resolved);
				oLabel.getReviewNotes().add(oReviewNote);
			}
		}
		
		oLabel.getAttributes().clear();
		if (oLabelViewModel.attributes != null) {
			for (AttributeViewModel oAttributeViewModel : oLabelViewModel.attributes) {
				oLabel.getAttributes().add(AttributeViewModel.convertToEntity(oAttributeViewModel));
			}
		}
	}
}
