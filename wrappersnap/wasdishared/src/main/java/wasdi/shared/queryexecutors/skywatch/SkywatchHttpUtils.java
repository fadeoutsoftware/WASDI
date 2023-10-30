package wasdi.shared.queryexecutors.skywatch;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

import wasdi.shared.config.DataProviderConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.queryexecutors.skywatch.models.CreatePipelineRequest;
import wasdi.shared.queryexecutors.skywatch.models.CreatePipelineResponse;
import wasdi.shared.queryexecutors.skywatch.models.DeletePipelineResponse;
import wasdi.shared.queryexecutors.skywatch.models.GetListPipelinesResponse;
import wasdi.shared.queryexecutors.skywatch.models.GetPipelineResponse;
import wasdi.shared.queryexecutors.skywatch.models.PipelineResultsResponse;
import wasdi.shared.queryexecutors.skywatch.models.SearchRequest;
import wasdi.shared.queryexecutors.skywatch.models.SearchResponse;
import wasdi.shared.queryexecutors.skywatch.models.SearchResultsResponse;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.LoggerWrapper;
import wasdi.shared.viewmodels.HttpCallResponse;

public class SkywatchHttpUtils {

	/**
	 * Static logger reference
	 */
	public static LoggerWrapper s_oLogger = new LoggerWrapper(LogManager.getLogger(SkywatchHttpUtils.class));

	private static ObjectMapper s_oMapper = new ObjectMapper();
	private static DataProviderConfig s_oDataProviderConfig = WasdiConfig.Current.getDataProviderConfig("SKYWATCH");


	private SkywatchHttpUtils() {
		throw new java.lang.UnsupportedOperationException("This is a utility class and cannot be instantiated");
	}

	public static SearchResultsResponse makeAttemptsToPerformSearchOperation(SearchRequest oSearchRequest, int iMaxAttempts) {
		return makeAttemptsToPerformSearchOperation(oSearchRequest, null, null, iMaxAttempts);
	}

	public static SearchResultsResponse makeAttemptsToPerformSearchOperation(SearchRequest oSearchRequest, String sOffset, String sLimit, int iMaxAttempts) {
		SearchResponse oSearchResponse = search(oSearchRequest);

		if (oSearchResponse == null) {
			return null;
		} else if (oSearchResponse.getData() == null || oSearchResponse.getData().getId() == null) {
			if (oSearchResponse != null && oSearchResponse.getErrors() != null) {
				SearchResultsResponse oSearchResultsResponse = new SearchResultsResponse();

				List<SearchResultsResponse.Error> aoErrors = oSearchResponse.getErrors().stream()
					.map((SearchResponse.Error t) -> new SearchResultsResponse.Error(t.getMessage()))
					.collect(Collectors.toList());

				oSearchResultsResponse.setErrors(aoErrors);

				return oSearchResultsResponse;
			} else {
				return null;
			}
		}

		String sSearchId = oSearchResponse.getData().getId();

		int iAttempts = 0;

		while (iAttempts < iMaxAttempts) {
			SearchResultsResponse oSearchResultsResponse = fetchSearchResults(sSearchId, sOffset, sLimit);

			if (oSearchResultsResponse != null && oSearchResultsResponse.getData() != null) {

				oSearchResultsResponse.getData().forEach(t -> {
					t.setSearchId(sSearchId);
				});

				return oSearchResultsResponse;
			}

			iAttempts++;

			try {
				Thread.sleep(iAttempts * 1_000);
			} catch (InterruptedException oInterruptedException) {
			}
		}

		return null;
	}

	public static SearchResponse search(SearchRequest oSearchRequest) {
		String sUrl = s_oDataProviderConfig.link + "/archive/search";
		String sApiKey = s_oDataProviderConfig.apiKey;

		Map<String, String> asHeaders = new HashMap<>();
		asHeaders.put("Accept", "application/json");
		asHeaders.put("x-api-key", sApiKey);

		try {
			String sPayload = s_oMapper.writeValueAsString(oSearchRequest);

			HttpCallResponse oHttpCallResponse = HttpUtils.httpPost(sUrl, sPayload, asHeaders);
			Integer iResult = oHttpCallResponse.getResponseCode();

			if (iResult != null && (iResult.intValue() == 200 || iResult.intValue() == 400)) {
				String sJsonResponse = oHttpCallResponse.getResponseBody();

				SearchResponse oSearchResponse = s_oMapper.readValue(sJsonResponse, SearchResponse.class);

				return oSearchResponse;
			}
		} catch (Exception oE) {
			s_oLogger.debug("SkywatchHttpUtils.search: could not serialize payload due to " + oE + ".");
		}

		return null;
	}

	public static SearchResultsResponse fetchSearchResults(String requestId, String sOffset, String sLimit) {
		String sUrl = s_oDataProviderConfig.link + "/archive/search/" + requestId + "/search_results";

		if (!Utils.isNullOrEmpty(sOffset) && !Utils.isNullOrEmpty(sLimit)) {
			sUrl += "?per_page=" + sLimit + "&cursor=" + sOffset;
		}

		String sApiKey = s_oDataProviderConfig.apiKey;

		Map<String, String> asHeaders = new HashMap<>();
		asHeaders.put("Accept", "application/json");
		asHeaders.put("x-api-key", sApiKey);

		try {
			HttpCallResponse oHttpCallResponse = HttpUtils.httpGet(sUrl, asHeaders);
			Integer iResult = oHttpCallResponse.getResponseCode();

			if (iResult != null && iResult.intValue() == 200) {
				String sJsonResponse = oHttpCallResponse.getResponseBody();

				SearchResultsResponse oSearchResultsResponse = s_oMapper.readValue(sJsonResponse, SearchResultsResponse.class);

				return oSearchResultsResponse;
			}
		} catch (Exception oE) {
			s_oLogger.debug("SkywatchHttpUtils.fetchSearchResults: could not serialize payload due to " + oE + ".");
		}

		return null;
	}

	public static long getFileSize(CreatePipelineRequest oPipelineRequest, int iMaxRetry) {
		PipelineResultsResponse oPipelineResultsResponse = makeAttemptsToPerformPipelineOperation(oPipelineRequest, iMaxRetry * 10);

		if (oPipelineResultsResponse != null) {
			if (oPipelineResultsResponse.getData() != null) {
				if (!oPipelineResultsResponse.getData().isEmpty()) {
					PipelineResultsResponse.Datum oDatum = oPipelineResultsResponse.getData().get(0);

					List<PipelineResultsResponse.Result> aoResults = oDatum.getResults();
					if (aoResults != null && !aoResults.isEmpty()) {
						PipelineResultsResponse.Result oResult = aoResults.get(0);

						PipelineResultsResponse.Metadata oMetadata = oResult.getMetadata();

						if (oMetadata != null) {
							int iSizeInMb = oMetadata.getSize_in_mb();
							long lSizeInBytes = iSizeInMb * 1024 * 1024;

							return lSizeInBytes;
						}
					}
				}
			} else {
				List<PipelineResultsResponse.Error> aoError = oPipelineResultsResponse.getErrors();

				if (aoError != null && !aoError.isEmpty()) {
					aoError.forEach(t -> {
						s_oLogger.debug("SkywatchHttpUtils.getFileSize: the PipelineResultsResponse encountered a problem: " + t.getMessage() + ".");
					});
				}
			}
		}

		return 0L;
	}

	public static String downloadProduct(CreatePipelineRequest oPipelineRequest, String sSaveDirOnServer, int iMaxRetry) {
		PipelineResultsResponse oPipelineResultsResponse = makeAttemptsToPerformPipelineOperation(oPipelineRequest, iMaxRetry * 10);

		if (oPipelineResultsResponse != null) {
			if (oPipelineResultsResponse.getData() != null && !oPipelineResultsResponse.getData().isEmpty()) {
				PipelineResultsResponse.Datum oDatum = oPipelineResultsResponse.getData().get(0);

				List<PipelineResultsResponse.Result> aoResults = oDatum.getResults();
				if (aoResults != null && !aoResults.isEmpty()) {
					PipelineResultsResponse.Result oResult = aoResults.get(0);

					String sAnalyticsUrl = oResult.getAnalytics_url();

					if (!Utils.isNullOrEmpty(sAnalyticsUrl)) {
						String sRemoteUri = sAnalyticsUrl;

						String sFileName = sRemoteUri.substring(sAnalyticsUrl.lastIndexOf("/") + 1);

						String sDownloadPath = sSaveDirOnServer;
						if (! (sDownloadPath.endsWith("\\") || sDownloadPath.endsWith("/") || sDownloadPath.endsWith(File.separator)) ) sDownloadPath += File.separator;

						String sOutputFilePath = sDownloadPath + sFileName;

						Map<String, String> asHeaders = new HashMap<>();

						String sActualOutputFilePath = HttpUtils.downloadFile(sRemoteUri, asHeaders, sOutputFilePath);

						if (!sOutputFilePath.equalsIgnoreCase(sActualOutputFilePath)) return null;

						DeletePipelineResponse oDeletePipelineResponse = deletePipeline(oDatum.getPipeline_id());

						if (oDeletePipelineResponse != null) {
							if (oDeletePipelineResponse.getErrors() != null) {
								List<DeletePipelineResponse.Error> aoError = oDeletePipelineResponse.getErrors();

								if (aoError != null && !aoError.isEmpty()) {
									aoError.forEach(t -> {
										s_oLogger.debug("SkywatchHttpUtils.downloadProduct: the deleting the pipeline encountered a problem: " + t.getMessage() + ".");
									});
								}
							}
						}

						return sActualOutputFilePath;
					}
				}
			} else {
				List<PipelineResultsResponse.Error> aoErrors = oPipelineResultsResponse.getErrors();

				if (aoErrors != null && !aoErrors.isEmpty()) {
					aoErrors.forEach(t -> {
						s_oLogger.debug("SkywatchHttpUtils.downloadProduct: the PipelineResultsResponse encountered a problem: " + t.getMessage() + ".");
					});
				}
			}
		}

		return null;
	}

	public static PipelineResultsResponse makeAttemptsToPerformPipelineOperation(CreatePipelineRequest oPipelineRequest, int iMaxAttempts) {
		String sPipelineId = null;

		GetListPipelinesResponse oGetListPipelinesResponse = getPipelines();

		List<GetListPipelinesResponse.Datum> oData = oGetListPipelinesResponse.getData();

		if (oData != null) {
			for (GetListPipelinesResponse.Datum oDatum : oData) {
				if (oPipelineRequest.getName().equalsIgnoreCase(oDatum.getName())
					|| oPipelineRequest.getSearch_id().equalsIgnoreCase(oDatum.getSearch_id())
					|| (oDatum.getSearch_results() != null
						&& !oDatum.getSearch_results().isEmpty()
						&& oPipelineRequest.getSearch_results().get(0).equalsIgnoreCase(oDatum.getSearch_results().get(0)))) {
					sPipelineId = oDatum.getId();
					break;
				}
			}
		}

		if (sPipelineId == null) {
			CreatePipelineResponse oPipelineResponse = createPipeline(oPipelineRequest);

			if (oPipelineResponse == null) {
				return null;
			} else if (oPipelineResponse.getData() == null || oPipelineResponse.getData().getId() == null) {
				if (oPipelineResponse != null && oPipelineResponse.getErrors() != null) {

					List<PipelineResultsResponse.Error> aoPipelineResultsResponseError = oPipelineResponse.getErrors().stream()
							.map(t -> new PipelineResultsResponse.Error(t.getMessage())).collect(Collectors.toList());

					PipelineResultsResponse oPipelineResultsResponse = new PipelineResultsResponse();
					oPipelineResultsResponse.setErrors(aoPipelineResultsResponseError);

					return oPipelineResultsResponse;
				} else {
					return null;
				}
			} else {
				sPipelineId = oPipelineResponse.getData().getId();
			}
		}
		
		

		int iAttempts = 0;

		while (iAttempts < iMaxAttempts) {
			PipelineResultsResponse oPipelineResultsResponse = fetchPipelineResults(sPipelineId);

			if (oPipelineResultsResponse != null && oPipelineResultsResponse.getData() != null && !oPipelineResultsResponse.getData().isEmpty()) {
				String sStatus = oPipelineResultsResponse.getData().get(0).getStatus();

				if ("complete".equalsIgnoreCase(sStatus)) {
					return oPipelineResultsResponse;
				} else if ("cancelled".equalsIgnoreCase(sStatus)
						|| "failed".equalsIgnoreCase(sStatus)) {
					s_oLogger.debug("SkywatchHttpUtils.downloadProduct: something went wrong on Skywatch side. status: " + sStatus + ".");
					return null;
				}
				
			}

			// give it more time to process

			iAttempts++;

			try {
				Thread.sleep(10_000);
			} catch (InterruptedException oInterruptedException) {
			}
		}

		return null;
	}

	public static CreatePipelineResponse createPipeline(CreatePipelineRequest oPipelineRequest) {
		String sUrl = s_oDataProviderConfig.link + "/pipelines";
		String sApiKey = s_oDataProviderConfig.apiKey;

		Map<String, String> asHeaders = new HashMap<>();
		asHeaders.put("Accept", "application/json");
		asHeaders.put("x-api-key", sApiKey);

		try {
			s_oMapper.setSerializationInclusion(Include.NON_NULL);
			String sPayload = s_oMapper.writeValueAsString(oPipelineRequest);

			HttpCallResponse oHttpCallResponse = HttpUtils.httpPost(sUrl, sPayload, asHeaders);
			Integer iResult = oHttpCallResponse.getResponseCode();

			if (iResult != null && (iResult.intValue() == 201 || iResult.intValue() == 400)) {
				String sJsonResponse = oHttpCallResponse.getResponseBody();

				CreatePipelineResponse oPipelineResponse = s_oMapper.readValue(sJsonResponse, CreatePipelineResponse.class);

				return oPipelineResponse;
			}
		} catch (Exception oE) {
			s_oLogger.debug("SkywatchHttpUtils.createPipeline: could not serialize payload due to " + oE + ".");
		}

		return null;
	}

	public static PipelineResultsResponse fetchPipelineResults(String pipelineId) {
		String sUrl = s_oDataProviderConfig.link + "/interval_results?pipeline_id=" + pipelineId;

		String sApiKey = s_oDataProviderConfig.apiKey;

		Map<String, String> asHeaders = new HashMap<>();
		asHeaders.put("Accept", "application/json");
		asHeaders.put("x-api-key", sApiKey);

		try {
			HttpCallResponse oHttpCallResponse = HttpUtils.httpGet(sUrl, asHeaders);
			Integer iResult = oHttpCallResponse.getResponseCode();

			if (iResult != null && iResult.intValue() == 200) {
				String sJsonResponse = oHttpCallResponse.getResponseBody();

				PipelineResultsResponse oPipelineResultsResponse = s_oMapper.readValue(sJsonResponse, PipelineResultsResponse.class);

				return oPipelineResultsResponse;
			}
		} catch (Exception oE) {
			s_oLogger.debug("SkywatchHttpUtils.fetchCreatePipelineResults: could not serialize payload due to " + oE + ".");
		}

		return null;
	}

	public static GetPipelineResponse getPipeline(String pipelineId) {
		String sUrl = s_oDataProviderConfig.link + "/pipelines/" + pipelineId;

		String sApiKey = s_oDataProviderConfig.apiKey;

		Map<String, String> asHeaders = new HashMap<>();
		asHeaders.put("Accept", "application/json");
		asHeaders.put("x-api-key", sApiKey);

		try {
			HttpCallResponse oHttpCallResponse = HttpUtils.httpGet(sUrl, asHeaders);
			Integer iResult = oHttpCallResponse.getResponseCode();

			if (iResult != null && iResult.intValue() == 200) {
				String sJsonResponse = oHttpCallResponse.getResponseBody();

				GetPipelineResponse oGetPipelineResponse = s_oMapper.readValue(sJsonResponse, GetPipelineResponse.class);

				return oGetPipelineResponse;
			}
		} catch (Exception oE) {
			s_oLogger.debug("SkywatchHttpUtils.getPipeline: could not serialize payload due to " + oE + ".");
		}

		return null;
	}

	public static GetListPipelinesResponse getPipelines() {
		String sUrl = s_oDataProviderConfig.link + "/pipelines";

		String sApiKey = s_oDataProviderConfig.apiKey;

		Map<String, String> asHeaders = new HashMap<>();
		asHeaders.put("Accept", "application/json");
		asHeaders.put("x-api-key", sApiKey);

		try {
			HttpCallResponse oHttpCallResponse = HttpUtils.httpGet(sUrl, asHeaders);
			Integer iResult = oHttpCallResponse.getResponseCode();

			if (iResult != null && iResult.intValue() == 200) {
				String sJsonResponse = oHttpCallResponse.getResponseBody();

				GetListPipelinesResponse oGetListPipelinesResponse = s_oMapper.readValue(sJsonResponse, GetListPipelinesResponse.class);

				return oGetListPipelinesResponse;
			}
		} catch (Exception oE) {
			s_oLogger.debug("SkywatchHttpUtils.getPipelines: could not serialize payload due to " + oE + ".");
		}

		return null;
	}

	public static DeletePipelineResponse deletePipeline(String pipelineId) {
		String sUrl = s_oDataProviderConfig.link + "/pipelines/" + pipelineId;

		String sApiKey = s_oDataProviderConfig.apiKey;

		Map<String, String> asHeaders = new HashMap<>();
		asHeaders.put("Accept", "application/json");
		asHeaders.put("x-api-key", sApiKey);

		try {
			HttpCallResponse oHttpCallResponse = HttpUtils.httpDelete(sUrl, asHeaders);
			Integer iResult = oHttpCallResponse.getResponseCode();

			if (iResult != null && iResult.intValue() == 204) {
				return new DeletePipelineResponse();
			} else {
				String sJsonResponse = oHttpCallResponse.getResponseBody();

				DeletePipelineResponse oDeletePipelineResponse = s_oMapper.readValue(sJsonResponse, DeletePipelineResponse.class);

				return oDeletePipelineResponse;
			}
		} catch (Exception oE) {
			s_oLogger.debug("SkywatchHttpUtils.deletePipeline: could not serialize payload due to " + oE + ".");
		}

		return null;
	}

}
