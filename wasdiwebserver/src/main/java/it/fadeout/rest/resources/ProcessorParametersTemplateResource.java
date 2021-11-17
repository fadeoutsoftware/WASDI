package it.fadeout.rest.resources;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import it.fadeout.Wasdi;
import wasdi.shared.business.Processor;
import wasdi.shared.business.ProcessorParametersTemplate;
import wasdi.shared.business.User;
import wasdi.shared.data.ProcessorParametersTemplateRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.utils.PermissionsUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.processors.ListProcessorParametersTemplatesViewModel;
import wasdi.shared.viewmodels.processors.ProcessorParametersTemplateViewModel;

/**
 * ProcessorParametersTemplate Resource.
 * 
 * Hosts the API for: .handle ProcessorParametersTemplate
 * 
 * @author PetruPetrescu
 *
 */
@Path("processorParametersTemplates")
public class ProcessorParametersTemplateResource {

	/**
	 * Deletes a processor parameters template.
	 * 
	 * @param sSessionId  the User Session Id
	 * @param sTemplateId the template Id
	 * @param sName       the name of the template
	 * @return std http response
	 */
	@DELETE
	@Path("/delete")
	public Response deleteProcessorParametersTemplate(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("templateId") String sTemplateId) {

		try {
			sTemplateId = java.net.URLDecoder.decode(sTemplateId, StandardCharsets.UTF_8.name());
		} catch (UnsupportedEncodingException e) {
			Utils.debugLog(
					"ProcessorParametersTemplateResource.deleteProcessorParametersTemplate excepion decoding the template Id");
		}

		Utils.debugLog("ProcessorParametersTemplateResource.deleteProcessorParametersTemplate(sProcessorId: "
				+ sTemplateId + ")");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		// Check the user session
		if (oUser == null) {
			return Response.status(Status.UNAUTHORIZED).build();
		}

		String sUserId = oUser.getUserId();

		ProcessorParametersTemplateRepository oProcessorParametersTemplateRepository = new ProcessorParametersTemplateRepository();
		ProcessorParametersTemplate oProcessorParametersTemplate = oProcessorParametersTemplateRepository
				.getProcessorParametersTemplateByTemplateId(sTemplateId);

		if (oProcessorParametersTemplate == null) {
			return Response.status(Status.BAD_REQUEST).build();
		}

		// CHEK USER ID TOKEN AND USER ID IN VIEW MODEL ARE ==
		if (!oProcessorParametersTemplateRepository.isTheOwnerOfTheTemplate(sTemplateId, sUserId)) {
			return Response.status(Status.UNAUTHORIZED).build();
		}

		int iDeletedCount = oProcessorParametersTemplateRepository.deleteByTemplateId(sTemplateId);

		if (iDeletedCount == 0) {
			return Response.status(Status.BAD_REQUEST).build();
		}

		return Response.status(Status.OK).build();
	}

	/**
	 * Updates a template
	 * 
	 * @param sSessionId                            User Session Id
	 * @param oProcessorParametersTemplateViewModel ProcessorParametersTemplate View
	 *                                              Model
	 * @return std http response
	 */
	@POST
	@Path("/update")
	public Response updateProcessorParametersTemplate(@HeaderParam("x-session-token") String sSessionId,
			ProcessorParametersTemplateViewModel oProcessorParametersTemplateViewModel) {
		Utils.debugLog("ProcessorParametersTemplateResource.updateProcessorParametersTemplate");

		// Check the user session
		User oUser = Wasdi.getUserFromSession(sSessionId);
		if (oUser == null) {
			return Response.status(Status.UNAUTHORIZED).build();
		}

		String sUserId = oUser.getUserId();

		if (oProcessorParametersTemplateViewModel == null) {
			return Response.status(Status.BAD_REQUEST).build();
		}

		ProcessorParametersTemplateRepository oProcessorParametersTemplateRepository = new ProcessorParametersTemplateRepository();

		ProcessorParametersTemplate oProcessorParametersTemplate = getProcessorParametersTemplateFromViewModel(
				oProcessorParametersTemplateViewModel, sUserId, oProcessorParametersTemplateViewModel.getTemplateId());

		boolean isUpdated = oProcessorParametersTemplateRepository
				.updateProcessorParametersTemplate(oProcessorParametersTemplate);
		if (isUpdated) {
			return Response.status(Status.OK).build();
		} else {
			return Response.status(Status.BAD_REQUEST).build();
		}
	}

	/**
	 * Add a template
	 * 
	 * @param sSessionId                            User Session Id
	 * @param oProcessorParametersTemplateViewModel ProcessorParametersTemplate View
	 *                                              Model
	 * @return std http reponse
	 */
	@POST
	@Path("/add")
	public Response addProcessorParametersTemplate(@HeaderParam("x-session-token") String sSessionId,
			ProcessorParametersTemplateViewModel oProcessorParametersTemplateViewModel) {
		Utils.debugLog("ProcessorParametersTemplateResource.addProcessorParametersTemplate");

		// Check the user session
		User oUser = Wasdi.getUserFromSession(sSessionId);
		if (oUser == null) {
			Utils.debugLog("ProcessorParametersTemplateResource.addProcessorParametersTemplate: invalid user");
			return Response.status(Status.UNAUTHORIZED).build();
		}

		if (oProcessorParametersTemplateViewModel == null) {
			Utils.debugLog("ProcessorParametersTemplateResource.addProcessorParametersTemplate: invalid view model");
			return Response.status(Status.BAD_REQUEST).build();
		}

		String sUserId = oUser.getUserId();

		String sProcessorId = oProcessorParametersTemplateViewModel.getProcessorId();

		if (Utils.isNullOrEmpty(sProcessorId)) {
			Utils.debugLog("ProcessorParametersTemplateResource.addProcessorParametersTemplate: invalid processorId");
			return Response.status(Status.BAD_REQUEST).build();
		}

		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);

		if (oProcessor == null) {
			Utils.debugLog("ProcessorParametersTemplateResource.addProcessorParametersTemplate: processor null "
					+ sProcessorId);
			return Response.status(Status.BAD_REQUEST).build();
		}

		ProcessorParametersTemplateRepository oProcessorParametersTemplateRepository = new ProcessorParametersTemplateRepository();

		ProcessorParametersTemplate oProcessorParametersTemplate = getProcessorParametersTemplateFromViewModel(
				oProcessorParametersTemplateViewModel, sUserId, Utils.getRandomName());

		oProcessorParametersTemplateRepository.insertProcessorParametersTemplate(oProcessorParametersTemplate);

		return Response.status(Status.OK).build();
	}

	/**
	 * Get the ProcessorParametersTemplate by Id.
	 * 
	 * @param sSessionId  User Session Id
	 * @param sTemplateId template Id
	 * @return List ProcessorParametersTemplate View Model
	 */
	@GET
	@Path("/get")
	public Response getProcessorParameterTemplateById(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("templateId") String sTemplateId) {
		Utils.debugLog("ProcessorParametersTemplateResource.getProcessorParametersTemplateById");

		// Check the user session
		User oUser = Wasdi.getUserFromSession(sSessionId);
		if (oUser == null) {
			return Response.status(Status.UNAUTHORIZED).build();
		}

		String sUserId = oUser.getUserId();

		if (!PermissionsUtils.canUserAccessProcessorParametersTemplate(sUserId, sTemplateId)) {
			Utils.debugLog("ProcessorParametersTemplateResource.getProcessorParameterTemplateById: user " + sUserId
					+ " is not allowed to access template " + sTemplateId + ", aborting");
			return Response.status(Status.UNAUTHORIZED).build();
		}

		// Get all the ProcessorParametersTemplates
		ProcessorParametersTemplateRepository oProcessorParametersTemplateRepository = new ProcessorParametersTemplateRepository();
		ProcessorParametersTemplate oProcessorParametersTemplate = oProcessorParametersTemplateRepository
				.getProcessorParametersTemplateByTemplateId(sTemplateId);

		ProcessorParametersTemplateViewModel oProcessorParametersTemplateViewModel = getProcessorParametersTemplateViewModel(
				oProcessorParametersTemplate);

		return Response.ok(oProcessorParametersTemplateViewModel).build();
	}

	/**
	 * Get the list of ProcessorParametersTemplate associated to a processor.
	 * 
	 * @param sSessionId   User Session Id
	 * @param sProcessorId Processor Id
	 * @return List ProcessorParametersTemplate View Model
	 */
	@GET
	@Path("/getlist")
	public Response getProcessorParametersTemplatesListByProcessor(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("processorId") String sProcessorId) {
		Utils.debugLog("ProcessorParametersTemplateResource.getProcessorParametersTemplatesListByProcessor");

		// Check the user session
		User oUser = Wasdi.getUserFromSession(sSessionId);
		if (oUser == null) {
			return Response.status(Status.UNAUTHORIZED).build();
		}

		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);

		if (oProcessor == null) {
			return Response.status(Status.BAD_REQUEST).build();
		}

		String sUserId = oUser.getUserId();

		// Get all the ProcessorParametersTemplates
		ProcessorParametersTemplateRepository oProcessorParametersTemplateRepository = new ProcessorParametersTemplateRepository();
		List<ProcessorParametersTemplate> aoProcessorParametersTemplates = oProcessorParametersTemplateRepository
				.getProcessorParametersTemplatesByUserAndProcessor(sUserId, sProcessorId);

		if (aoProcessorParametersTemplates == null || aoProcessorParametersTemplates.size() == 0) {
			return Response.ok(new ListProcessorParametersTemplatesViewModel()).build();
		}

		// Cast in a list, computing all the statistics
		ListProcessorParametersTemplatesViewModel oListProcessorParametersTemplatesViewModel = getListProcessorParametersTemplatesViewModel(
				aoProcessorParametersTemplates);

		return Response.ok(oListProcessorParametersTemplatesViewModel).build();
	}

	/**
	 * Fill the ProcessorParametersTemplate Wrapper View Model result from a list of
	 * templates.
	 * 
	 * @param aoProcessorParametersTemplateList the list of templates
	 * @return the ListViewModel
	 */
	private ListProcessorParametersTemplatesViewModel getListProcessorParametersTemplatesViewModel(
			List<ProcessorParametersTemplate> aoProcessorParametersTemplateList) {
		ListProcessorParametersTemplatesViewModel oListProcessorParametersTemplates = new ListProcessorParametersTemplatesViewModel();
		List<ProcessorParametersTemplateViewModel> aoProcessorParametersTemplates = new ArrayList<>();
		if (aoProcessorParametersTemplateList == null) {
			return null;
		}

		for (ProcessorParametersTemplate oProcessorParametersTemplate : aoProcessorParametersTemplateList) {
			ProcessorParametersTemplateViewModel oProcessorParametersTemplateViewModel = new ProcessorParametersTemplateViewModel();
			oProcessorParametersTemplateViewModel.setTemplateId(oProcessorParametersTemplate.getTemplateId());
			oProcessorParametersTemplateViewModel.setUserId(oProcessorParametersTemplate.getUserId());
			oProcessorParametersTemplateViewModel.setProcessorId(oProcessorParametersTemplate.getProcessorId());
			oProcessorParametersTemplateViewModel.setName(oProcessorParametersTemplate.getName());
			oProcessorParametersTemplateViewModel.setDescription(oProcessorParametersTemplate.getDescription());
			oProcessorParametersTemplateViewModel.setJsonParameters(oProcessorParametersTemplate.getJsonParameters());

			aoProcessorParametersTemplates.add(oProcessorParametersTemplateViewModel);
		}

		oListProcessorParametersTemplates.setProcessorParametersTemplates(aoProcessorParametersTemplates);

		return oListProcessorParametersTemplates;
	}

	/**
	 * Fill the ProcessorParametersTemplate Wrapper View Model result from a
	 * template object.
	 * 
	 * @param oProcessorParametersTemplate the template object
	 * @return the ViewModel
	 */
	private ProcessorParametersTemplateViewModel getProcessorParametersTemplateViewModel(
			ProcessorParametersTemplate oProcessorParametersTemplate) {
		ProcessorParametersTemplateViewModel oProcessorParametersTemplateViewModel = new ProcessorParametersTemplateViewModel();
		oProcessorParametersTemplateViewModel.setTemplateId(oProcessorParametersTemplate.getTemplateId());
		oProcessorParametersTemplateViewModel.setUserId(oProcessorParametersTemplate.getUserId());
		oProcessorParametersTemplateViewModel.setProcessorId(oProcessorParametersTemplate.getProcessorId());
		oProcessorParametersTemplateViewModel.setName(oProcessorParametersTemplate.getName());
		oProcessorParametersTemplateViewModel.setDescription(oProcessorParametersTemplate.getDescription());
		oProcessorParametersTemplateViewModel.setJsonParameters(oProcessorParametersTemplate.getJsonParameters());

		return oProcessorParametersTemplateViewModel;
	}

	/**
	 * Converts a ProcessorParametersTemplate View Model in a
	 * ProcessorParametersTemplate Entity
	 * 
	 * @param oProcessorParametersTemplateViewModel the view model
	 * @param sUserId                               the user Id
	 * @param sId                                   the id of the template object
	 * @return a new template object
	 */
	private ProcessorParametersTemplate getProcessorParametersTemplateFromViewModel(
			ProcessorParametersTemplateViewModel oProcessorParametersTemplateViewModel, String sUserId, String sId) {
		if (oProcessorParametersTemplateViewModel != null) {
			ProcessorParametersTemplate oProcessorParametersTemplate = new ProcessorParametersTemplate();
			oProcessorParametersTemplate.setTemplateId(sId);
			oProcessorParametersTemplate.setUserId(sUserId);
			oProcessorParametersTemplate.setProcessorId(oProcessorParametersTemplateViewModel.getProcessorId());
			oProcessorParametersTemplate.setName(oProcessorParametersTemplateViewModel.getName());
			oProcessorParametersTemplate.setDescription(oProcessorParametersTemplateViewModel.getDescription());
			oProcessorParametersTemplate.setJsonParameters(oProcessorParametersTemplateViewModel.getJsonParameters());

			return oProcessorParametersTemplate;
		}

		return null;
	}

}
