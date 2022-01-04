package it.fadeout.rest.resources;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

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
import wasdi.shared.viewmodels.processors.ProcessorParametersTemplateListViewModel;
import wasdi.shared.viewmodels.processors.ProcessorParametersTemplateDetailViewModel;

/**
 * ProcessorParametersTemplate Resource.
 * 
 * Hosts the API for: .handle ProcessorParametersTemplate
 * 
 * @author PetruPetrescu
 *
 */
@Path("processorParamTempl")
public class ProcessorParametersTemplateResource {

	/**
	 * Deletes a processor parameters template.
	 * 
	 * @param sSessionId the User Session Id
	 * @param sTemplateId the template Id
	 * @param sName the name of the template
	 * @return std http response
	 */
	@DELETE
	@Path("/delete")
	public Response deleteProcessorParametersTemplate(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("templateId") String sTemplateId) {

		try {
			sTemplateId = java.net.URLDecoder.decode(sTemplateId, StandardCharsets.UTF_8.name());
		} catch (UnsupportedEncodingException e) {
			Utils.debugLog("ProcessorParametersTemplateResource.deleteProcessorParametersTemplate excepion decoding the template Id");
		}

		Utils.debugLog("ProcessorParametersTemplateResource.deleteProcessorParametersTemplate(sProcessorId: " + sTemplateId + ")");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		// Check the user session
		if (oUser == null) {
			return Response.status(Status.UNAUTHORIZED).build();
		}

		String sUserId = oUser.getUserId();

		ProcessorParametersTemplateRepository oProcessorParametersTemplateRepository = new ProcessorParametersTemplateRepository();
		ProcessorParametersTemplate oTemplate = oProcessorParametersTemplateRepository.getProcessorParametersTemplateByTemplateId(sTemplateId);

		if (oTemplate == null) {
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
	 * @param sSessionId the User Session Id
	 * @param oDetailViewModel the detail view model
	 * @return std http response
	 */
	@POST
	@Path("/update")
	public Response updateProcessorParametersTemplate(@HeaderParam("x-session-token") String sSessionId,
			ProcessorParametersTemplateDetailViewModel oDetailViewModel) {
		Utils.debugLog("ProcessorParametersTemplateResource.updateProcessorParametersTemplate");

		// Check the user session
		User oUser = Wasdi.getUserFromSession(sSessionId);
		if (oUser == null) {
			return Response.status(Status.UNAUTHORIZED).build();
		}

		String sUserId = oUser.getUserId();

		if (oDetailViewModel == null) {
			return Response.status(Status.BAD_REQUEST).build();
		}

		ProcessorParametersTemplateRepository oProcessorParametersTemplateRepository = new ProcessorParametersTemplateRepository();

		ProcessorParametersTemplate oTemplate = getTemplateFromDetailViewModel(oDetailViewModel, sUserId, oDetailViewModel.getTemplateId());

		Date oDate = new Date();
		oTemplate.setUpdateDate((double) oDate.getTime());

		boolean isUpdated = oProcessorParametersTemplateRepository.updateProcessorParametersTemplate(oTemplate);
		if (isUpdated) {
			return Response.status(Status.OK).build();
		} else {
			return Response.status(Status.BAD_REQUEST).build();
		}
	}

	/**
	 * Add a template
	 * 
	 * @param sSessionId the User Session Id
	 * @param oDetailViewModel the detail view model
	 * @return std http reponse
	 */
	@POST
	@Path("/add")
	public Response addProcessorParametersTemplate(@HeaderParam("x-session-token") String sSessionId,
			ProcessorParametersTemplateDetailViewModel oDetailViewModel) {
		Utils.debugLog("ProcessorParametersTemplateResource.addProcessorParametersTemplate");

		// Check the user session
		User oUser = Wasdi.getUserFromSession(sSessionId);
		if (oUser == null) {
			Utils.debugLog("ProcessorParametersTemplateResource.addProcessorParametersTemplate: invalid user");
			return Response.status(Status.UNAUTHORIZED).build();
		}

		if (oDetailViewModel == null) {
			Utils.debugLog("ProcessorParametersTemplateResource.addProcessorParametersTemplate: invalid view model");
			return Response.status(Status.BAD_REQUEST).build();
		}

		String sUserId = oUser.getUserId();

		String sProcessorId = oDetailViewModel.getProcessorId();

		if (Utils.isNullOrEmpty(sProcessorId)) {
			Utils.debugLog("ProcessorParametersTemplateResource.addProcessorParametersTemplate: invalid processorId");
			return Response.status(Status.BAD_REQUEST).build();
		}

		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);

		if (oProcessor == null) {
			Utils.debugLog("ProcessorParametersTemplateResource.addProcessorParametersTemplate: processor null " + sProcessorId);
			return Response.status(Status.BAD_REQUEST).build();
		}

		ProcessorParametersTemplateRepository oProcessorParametersTemplateRepository = new ProcessorParametersTemplateRepository();

		ProcessorParametersTemplate oTemplate = getTemplateFromDetailViewModel(oDetailViewModel, sUserId, Utils.getRandomName());

		Date oDate = new Date();
		oTemplate.setCreationDate((double) oDate.getTime());
		oTemplate.setUpdateDate((double) oDate.getTime());

		oProcessorParametersTemplateRepository.insertProcessorParametersTemplate(oTemplate);

		return Response.status(Status.OK).build();
	}

	/**
	 * Get the ProcessorParametersTemplate by Id.
	 * 
	 * @param sSessionId the User Session Id
	 * @param sTemplateId the template Id
	 * @return the detail view model
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
		ProcessorParametersTemplate oTemplate = oProcessorParametersTemplateRepository.getProcessorParametersTemplateByTemplateId(sTemplateId);

		ProcessorParametersTemplateDetailViewModel oDetailViewModel = getDetailViewModel(oTemplate);

		return Response.ok(oDetailViewModel).build();
	}

	/**
	 * Get the list of ProcessorParametersTemplate associated to a processor.
	 * 
	 * @param sSessionId   User Session Id
	 * @param sProcessorId Processor Id
	 * @return a list of list view model
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
		List<ProcessorParametersTemplate> aoTemplates = oProcessorParametersTemplateRepository
				.getProcessorParametersTemplatesByUserAndProcessor(sUserId, sProcessorId);

		// Cast in a list
		List<ProcessorParametersTemplateListViewModel> aoListViewModel = getListViewModels(aoTemplates);

		return Response.ok(aoListViewModel).build();
	}

	/**
	 * Fill the list of ProcessorParametersTemplateListViewModel result from a list of templates.
	 * 
	 * @param aoTemplates the list of templates
	 * @return the list of ListViewModel
	 */
	private static List<ProcessorParametersTemplateListViewModel> getListViewModels(List<ProcessorParametersTemplate> aoTemplates) {
		if (aoTemplates == null) {
			return null;
		}

		return aoTemplates.stream()
				.map(ProcessorParametersTemplateResource::getListViewModel)
				.collect(Collectors.toList());
	}

	/**
	 * Fill the ProcessorParametersTemplateListViewModel result from a {@link ProcessorParametersTemplate} object.
	 * 
	 * @param oTemplate the template object
	 * @return a new list view model object
	 */
	private static ProcessorParametersTemplateListViewModel getListViewModel(ProcessorParametersTemplate oTemplate) {
		if (oTemplate == null) {
			return null;
		}

		ProcessorParametersTemplateListViewModel oListViewModel = new ProcessorParametersTemplateListViewModel();
		oListViewModel.setTemplateId(oTemplate.getTemplateId());
		oListViewModel.setUserId(oTemplate.getUserId());
		oListViewModel.setProcessorId(oTemplate.getProcessorId());
		oListViewModel.setName(oTemplate.getName());

		if (oTemplate.getUpdateDate() != null) {
			oListViewModel.setUpdateDate(Utils.getFormatDate(new Date(oTemplate.getUpdateDate().longValue())));
		}

		return oListViewModel;
	}

	/**
	 * Fill the {@link ProcessorParametersTemplateDetailViewModel} result from a {@link ProcessorParametersTemplate} object.
	 * 
	 * @param oTemplate the template object
	 * @return a new detail view model object
	 */
	private static ProcessorParametersTemplateDetailViewModel getDetailViewModel(ProcessorParametersTemplate oTemplate) {
		if (oTemplate == null) {
			return null;
		}

		ProcessorParametersTemplateDetailViewModel oDetailViewModel = new ProcessorParametersTemplateDetailViewModel();
		oDetailViewModel.setTemplateId(oTemplate.getTemplateId());
		oDetailViewModel.setUserId(oTemplate.getUserId());
		oDetailViewModel.setProcessorId(oTemplate.getProcessorId());
		oDetailViewModel.setName(oTemplate.getName());
		oDetailViewModel.setDescription(oTemplate.getDescription());
		oDetailViewModel.setJsonParameters(oTemplate.getJsonParameters());

		if (oTemplate.getCreationDate() != null) {
			oDetailViewModel.setCreationDate(Utils.getFormatDate(new Date(oTemplate.getCreationDate().longValue())));
		}

		if (oTemplate.getUpdateDate() != null) {
			oDetailViewModel.setUpdateDate(Utils.getFormatDate(new Date(oTemplate.getUpdateDate().longValue())));
		}

		return oDetailViewModel;
	}

	/**
	 * Converts a {@link ProcessorParametersTemplateDetailViewModel} in a {@link ProcessorParametersTemplate} object.
	 * 
	 * @param oDetailViewModel the view model object
	 * @param sUserId the user Id
	 * @param sId the id of the template object
	 * @return a new template object
	 */
	private static ProcessorParametersTemplate getTemplateFromDetailViewModel(ProcessorParametersTemplateDetailViewModel oDetailViewModel, String sUserId, String sId) {
		if (oDetailViewModel == null) {
			return null;
		}

		ProcessorParametersTemplate oTemplate = new ProcessorParametersTemplate();
		oTemplate.setTemplateId(sId);
		oTemplate.setUserId(sUserId);
		oTemplate.setProcessorId(oDetailViewModel.getProcessorId());
		oTemplate.setName(oDetailViewModel.getName());
		oTemplate.setDescription(oDetailViewModel.getDescription());
		oTemplate.setJsonParameters(oDetailViewModel.getJsonParameters());
		oTemplate.setCreationDate(Utils.getWasdiDateAsDouble(oDetailViewModel.getCreationDate()));

		return oTemplate;
	}

}
