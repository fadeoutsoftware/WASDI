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

import org.joda.time.DateTimeUtils;

import it.fadeout.Wasdi;
import wasdi.shared.business.labelling.Attribute;
import wasdi.shared.business.labelling.Template;
import wasdi.shared.business.users.User;
import wasdi.shared.data.labelling.LabellingTemplateRepository;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.ClientMessageCodes;
import wasdi.shared.viewmodels.ErrorResponse;
import wasdi.shared.viewmodels.labelling.attributes.AttributeViewModel;
import wasdi.shared.viewmodels.labelling.templates.TemplateListViewModel;
import wasdi.shared.viewmodels.labelling.templates.TemplateViewModel;

@Path("/labelling/templates")
public class TemplateResource {
	
	@GET
	@Path("/list")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response getListByUser(@HeaderParam("x-session-token") String sSessionId) {
		
		WasdiLog.debugLog("TemplateResource.getListByUser");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		List<TemplateListViewModel> aoTemplatesList = new ArrayList<>();

		// Domain Check
		if (oUser == null) {
			WasdiLog.warnLog("TemplateResource.getListByUser: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}

		try {
			
			WasdiLog.debugLog("TemplateResource.getListByUser: templates for " + oUser.getUserId());

			// Create repo
			LabellingTemplateRepository oTemplateRepository = new LabellingTemplateRepository();
			
			List<Template> aoTemplates = oTemplateRepository.getAll();
			
			if (aoTemplates==null) {
				WasdiLog.debugLog("TemplateResource.getListByUser: aoTemplates is null");
				return Response.ok(aoTemplatesList).build();
			}			
			
			// For each
			for (Template oTemplate : aoTemplates) {
				// Create View Model
				TemplateListViewModel oTemplateListViewModel = new TemplateListViewModel();
				
				oTemplateListViewModel.name = oTemplate.getName();
				oTemplateListViewModel.id = oTemplate.getId();
				if (oTemplate.getCreator().equals(oUser.getUserId())) {
					oTemplateListViewModel.canEdit = true;	
				}
				else {
					oTemplateListViewModel.canEdit = false;
				}
				oTemplateListViewModel.created = oTemplate.getCreationDate();
				oTemplateListViewModel.creator = oTemplate.getCreator();
				
				aoTemplatesList.add(oTemplateListViewModel);
			}
			
			return Response.ok(aoTemplatesList).build();
		} catch (Exception oEx) {
			WasdiLog.errorLog("TemplateResource.getListByUser error: " + oEx);
			return Response.serverError().build();
		}
	}
	
	@GET
	@Path("/")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response getById(@HeaderParam("x-session-token") String sSessionId, @QueryParam("templateId") String sTemplateId) {
		
		WasdiLog.debugLog("TemplateResource.getById");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		// Domain Check
		if (oUser == null) {
			WasdiLog.warnLog("TemplateResource.getById: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}

		try {
			
			WasdiLog.debugLog("TemplateResource.getById: template " + sTemplateId);

			// Create repo
			LabellingTemplateRepository oTemplateRepository = new LabellingTemplateRepository();
			
			Template oTemplate = oTemplateRepository.getTemplate(sTemplateId);
			
			if (oTemplate==null) {
				WasdiLog.debugLog("TemplateResource.getById: oTemplate is null");
				return Response.status(Status.BAD_REQUEST).build();
			}			
			
			TemplateViewModel oTemplateViewModel = new TemplateViewModel();
			
			
			oTemplateViewModel.name = oTemplate.getName();
			oTemplateViewModel.id = oTemplate.getId();
			if (oTemplate.getCreator().equals(oUser.getUserId())) {
				oTemplateViewModel.canEdit = true;	
			}
			else {
				oTemplateViewModel.canEdit = false;
			}
			oTemplateViewModel.created = oTemplate.getCreationDate();
			oTemplateViewModel.creator = oTemplate.getCreator();
			
			for (Attribute oAttribute : oTemplate.getAttributes()) {
				AttributeViewModel oVM = AttributeViewModel.getFromEntity(oAttribute);
				oTemplateViewModel.attributes.add(oVM);	
			}
			
			oTemplateViewModel.colourAttributeName = oTemplate.getColourAttributeName();
			oTemplateViewModel.description = oTemplate.getDescription();
			oTemplateViewModel.fixedColour = oTemplate.getFixedColour();
			oTemplateViewModel.hasLines = oTemplate.isHasLines();
			oTemplateViewModel.hasPoints = oTemplate.isHasPoints();
			oTemplateViewModel.hasPolygons = oTemplate.isHasPolygons();
			oTemplateViewModel.isFixedColour = oTemplate.isFixedColour();
			
			return Response.ok(oTemplateViewModel).build();
		} catch (Exception oEx) {
			WasdiLog.errorLog("TemplateResource.getById error: " + oEx);
			return Response.serverError().build();
		}
	}

	@POST
	@Path("/")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response create(@HeaderParam("x-session-token") String sSessionId, TemplateViewModel oTemplateViewModel) {
		WasdiLog.debugLog("TemplateResource.create");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		// Domain Check
		if (oUser == null) {
			WasdiLog.warnLog("TemplateResource.create: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}
		
		if (oTemplateViewModel == null) {
			WasdiLog.warnLog("TemplateResource.create: invalid oTemplateViewModel");
			return Response.status(Status.BAD_REQUEST).build();
		}

		try {
			
			WasdiLog.debugLog("TemplateResource.create:");

			// Create repo
			LabellingTemplateRepository oTemplateRepository = new LabellingTemplateRepository();
			
			Template oTemplate = new Template();
			
			
			oTemplate.setName(oTemplateViewModel.name);
			oTemplate.setId(Utils.getRandomName());
			oTemplate.setCreator(oUser.getUserId());			
			oTemplate.setCreationDate(DateTimeUtils.currentTimeMillis());

			oTemplate.setColourAttributeName(oTemplateViewModel.colourAttributeName) ;
			oTemplate.setDescription(oTemplateViewModel.description) ;
			oTemplate.setFixedColour(oTemplateViewModel.fixedColour) ;
			oTemplate.setHasLines(oTemplateViewModel.hasLines) ;
			oTemplate.setHasPoints(oTemplateViewModel.hasPoints);
			oTemplate.setHasPolygons(oTemplateViewModel.hasPolygons) ;
			oTemplate.setFixedColour(oTemplateViewModel.isFixedColour) ;
			
			
			for (AttributeViewModel oVM : oTemplateViewModel.attributes) {
				Attribute oEntity = AttributeViewModel.convertToEntity(oVM);
				oTemplate.getAttributes().add(oEntity);	
			}
			
			oTemplateRepository.insertTemplate(oTemplate);
			
			return Response.ok(oTemplate.getId()).build();
		} catch (Exception oEx) {
			WasdiLog.errorLog("TemplateResource.create error: " + oEx);
			return Response.serverError().build();
		}		
	}
	
	@PUT
	@Path("/")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response update(@HeaderParam("x-session-token") String sSessionId, TemplateViewModel oTemplateViewModel) {
		WasdiLog.debugLog("TemplateResource.update");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		// Domain Check
		if (oUser == null) {
			WasdiLog.warnLog("TemplateResource.update: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}
		
		if (oTemplateViewModel == null) {
			WasdiLog.warnLog("TemplateResource.update: invalid oTemplateViewModel");
			return Response.status(Status.BAD_REQUEST).build();
		}
		

		try {
			
			WasdiLog.debugLog("TemplateResource.update: " + oTemplateViewModel.id);

			// Create repo
			LabellingTemplateRepository oTemplateRepository = new LabellingTemplateRepository();
			
			Template oTemplate = oTemplateRepository.getTemplate(oTemplateViewModel.id);
			
			if (oTemplate == null) {
				WasdiLog.warnLog("TemplateResource.update: template not found");
				return Response.status(Status.BAD_REQUEST).build();
			}
			
			if (!oTemplate.getCreator().equals(oUser.getUserId())) {
				WasdiLog.warnLog("TemplateResource.update: the user is not the creator for the template");
				return Response.status(Status.UNAUTHORIZED).build();				
			}
			
			oTemplate.setName(oTemplateViewModel.name);
			oTemplate.setColourAttributeName(oTemplateViewModel.colourAttributeName) ;
			oTemplate.setDescription(oTemplateViewModel.description) ;
			oTemplate.setFixedColour(oTemplateViewModel.fixedColour) ;
			oTemplate.setHasLines(oTemplateViewModel.hasLines) ;
			oTemplate.setHasPoints(oTemplateViewModel.hasPoints);
			oTemplate.setHasPolygons(oTemplateViewModel.hasPolygons) ;
			oTemplate.setFixedColour(oTemplateViewModel.isFixedColour) ;
			
			oTemplate.getAttributes().clear();
			
			for (AttributeViewModel oVM : oTemplateViewModel.attributes) {
				Attribute oEntity = AttributeViewModel.convertToEntity(oVM);
				oTemplate.getAttributes().add(oEntity);	
			}
			
			oTemplateRepository.updateTemplate(oTemplate);
			
			return Response.ok().build();
		} catch (Exception oEx) {
			WasdiLog.errorLog("TemplateResource.update error: " + oEx);
			return Response.serverError().build();
		}		
	}	
	
	@DELETE
	@Path("/")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response delete(@HeaderParam("x-session-token") String sSessionId, @QueryParam("templateId") String sTemplateId) {
		WasdiLog.debugLog("TemplateResource.delete");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		// Domain Check
		if (oUser == null) {
			WasdiLog.warnLog("TemplateResource.delete: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}

		try {
			
			WasdiLog.debugLog("TemplateResource.delete: " + sTemplateId);

			// Create repo
			LabellingTemplateRepository oTemplateRepository = new LabellingTemplateRepository();
			
			Template oTemplate = oTemplateRepository.getTemplate(sTemplateId);
			
			if (oTemplate == null) {
				WasdiLog.warnLog("TemplateResource.delete: template not found");
				return Response.status(Status.BAD_REQUEST).build();
			}
			
			if (!oTemplate.getCreator().equals(oUser.getUserId())) {
				WasdiLog.warnLog("TemplateResource.delete: the user is not the creator for the template");
				return Response.status(Status.UNAUTHORIZED).build();				
			}
			
			oTemplateRepository.deleteTemplate(sTemplateId);
			
			return Response.ok().build();
		} catch (Exception oEx) {
			WasdiLog.errorLog("TemplateResource.delete error: " + oEx);
			return Response.serverError().build();
		}		
	}		

}
