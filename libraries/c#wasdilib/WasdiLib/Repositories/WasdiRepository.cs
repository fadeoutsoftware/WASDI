using System.Text;
using System.IO.Compression;

using Microsoft.Extensions.Logging;

using Newtonsoft.Json;
using Newtonsoft.Json.Serialization;

using WasdiLib.Extensions;
using WasdiLib.Models;

namespace WasdiLib.Repositories
{
    internal class WasdiRepository : IWasdiRepository
    {

        private const string HELLO_WASDI_PATH = "wasdi/hello";

        private const string LOGIN_PATH = "auth/login";
        private const string CHECK_SESSION_PATH = "auth/checksession";

        private const string CATALOG_CHECK_FILE_EXISTS_ON_WASDI_PATH = "/catalog/checkdownloadavaialibitybyname";
        private const string CATALOG_CHECK_FILE_EXISTS_ON_NODE_PATH = "/catalog/fileOnNode";
        private const string CATALOG_DOWNLOAD_PATH = "catalog/downloadbyname";
        private const string CATALOG_UPLOAD_INGEST_PATH = "/catalog/upload/ingestinws";

        private const string PROCESSORS_LOGS_ADD_PATH = "processors/logs/add";
        private const string PROCESSORS_RUN_PATH = "processors/run";

        private const string PROCESING_SUBSET_PATH = "processing/subset";

        private const string FILEBUFFER_DOWNLOAD_PATH = "filebuffer/download";


        private readonly ILogger<WasdiRepository> _logger;

        private readonly HttpClient _wasdiHttpClient;

        public WasdiRepository(ILogger<WasdiRepository> logger, IHttpClientFactory httpClientFactory)
        {
            _logger = logger;

            _wasdiHttpClient = httpClientFactory.CreateClient("WasdiApi");
        }

        public async Task<PrimitiveResult> HelloWasdi(string sBaseUrl)
        {
            _logger.LogDebug("HelloWasdi()");

            _wasdiHttpClient.DefaultRequestHeaders.Clear();

            var response = await _wasdiHttpClient.GetAsync(sBaseUrl + HELLO_WASDI_PATH);
            response.EnsureSuccessStatusCode();

            return await response.ConvertResponse<PrimitiveResult>();
        }

        public async Task<LoginResponse> Login(string sBaseUrl, string sUser, string sPassword)
        {
            _logger.LogDebug("Login()");

            _wasdiHttpClient.DefaultRequestHeaders.Clear();

            var loginRequest = new LoginRequest
            {
                UserId = sUser,
                UserPassword = sPassword
            };

            var json = JsonConvert.SerializeObject(loginRequest,
                new JsonSerializerSettings
                {
                    ContractResolver = new CamelCasePropertyNamesContractResolver()
                }
            );

            var requestPayload = new StringContent(json, Encoding.UTF8, "application/json");

            var response = await _wasdiHttpClient.PostAsync(sBaseUrl + LOGIN_PATH, requestPayload);
            response.EnsureSuccessStatusCode();

            return await response.ConvertResponse<LoginResponse>();
        }

        public async Task<LoginResponse> CheckSession(string sBaseUrl, string sSessionId)
        {
            _logger.LogDebug("CheckSession()");

            _wasdiHttpClient.DefaultRequestHeaders.Clear();
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("Content-Type", "application/json");
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("x-session-token", sSessionId);

            var response = await _wasdiHttpClient.GetAsync(sBaseUrl + CHECK_SESSION_PATH);
            response.EnsureSuccessStatusCode();

            return await response.ConvertResponse<LoginResponse>();
        }

        public async Task<bool> FileExistsOnServer(string sWorkspaceBaseUrl, string sSessionId, string sWorkspaceId,
            bool bIsMainNode, string sFileName)
        {
            _logger.LogDebug("FileExistsOnServer()");

            _wasdiHttpClient.DefaultRequestHeaders.Clear();
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("Content-Type", "application/json");
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("x-session-token", sSessionId);


            Dictionary<string, string> parameters = new Dictionary<string, string>();
            parameters.Add("token", sSessionId);
            parameters.Add("filename", sFileName);
            parameters.Add("workspace", sWorkspaceId);

            var formUrlEncodedContent = new FormUrlEncodedContent(parameters);
            string query = formUrlEncodedContent.ReadAsStringAsync().Result;
            if (!String.IsNullOrEmpty(query))
                query = "?" + query;


            string sUrl = sWorkspaceBaseUrl;
            if (bIsMainNode)
                sUrl += CATALOG_CHECK_FILE_EXISTS_ON_WASDI_PATH;
            else
                sUrl += CATALOG_CHECK_FILE_EXISTS_ON_NODE_PATH;

            sUrl += query;


            var response = await _wasdiHttpClient.GetAsync(sUrl);

            var content = response.Content;
            if (content != null)
                _logger.LogDebug(content.ToString());

            if (!response.IsSuccessStatusCode)
                return false;

            PrimitiveResult wasdiResponse = response.ConvertResponse<PrimitiveResult>().GetAwaiter().GetResult();

            return wasdiResponse.BoolValue;
        }

        public void Download(string sUrl, string sSessionId, string sFileName)
        {
            _logger.LogDebug("Download()");
            _logger.LogError("DownloadFile: not yet fully implemented");
        }

        public async Task<PrimitiveResult> CatalogUploadIngest(string sWorkspaceBaseUrl, string sSessionId, string sWorkspaceId, string sFileName, string sStyle)
        {
            _logger.LogDebug("CatalogUploadIngest()");

            _wasdiHttpClient.DefaultRequestHeaders.Clear();
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("Content-Type", "application/json");
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("x-session-token", sSessionId);


            Dictionary<string, string> parameters = new Dictionary<string, string>();
            parameters.Add("token", sSessionId);
            parameters.Add("file", sFileName);
            parameters.Add("workspace", sWorkspaceId);

            if (String.IsNullOrEmpty(sStyle))
                parameters.Add("style", sStyle);

            var formUrlEncodedContent = new FormUrlEncodedContent(parameters);
            string query = formUrlEncodedContent.ReadAsStringAsync().Result;
            if (!String.IsNullOrEmpty(query))
                query = "?" + query;


            string sUrl = sWorkspaceBaseUrl;
            sUrl += CATALOG_UPLOAD_INGEST_PATH;
            sUrl += query;

            var response = await _wasdiHttpClient.GetAsync(sUrl);

            var content = response.Content;
            if (content != null)
                _logger.LogDebug(content.ToString());

            return await response.ConvertResponse<PrimitiveResult>();
        }

        public async Task<string> CatalogDownload(string sWorkspaceBaseUrl, string sSessionId, string sWorkspaceId, string sSavePath, string sFileName)
        {
            _logger.LogDebug("CatalogUploadIngest()");

            _wasdiHttpClient.DefaultRequestHeaders.Clear();
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("Content-Type", "application/json");
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("x-session-token", sSessionId);


            Dictionary<string, string> parameters = new Dictionary<string, string>();
            parameters.Add("filename", sFileName);
            parameters.Add("workspace", sWorkspaceId);

            var formUrlEncodedContent = new FormUrlEncodedContent(parameters);
            string query = formUrlEncodedContent.ReadAsStringAsync().Result;
            if (!String.IsNullOrEmpty(query))
                query = "?" + query;


            string sUrl = sWorkspaceBaseUrl;
            sUrl += CATALOG_DOWNLOAD_PATH;
            sUrl += query;

            var response = await _wasdiHttpClient.GetAsync(sUrl);

            if (response.StatusCode == System.Net.HttpStatusCode.OK)
            {
                var aoHeaders = response.Content.Headers;
                _logger.LogDebug("aoHeaders:\n" + aoHeaders);

                if (aoHeaders != null)
                {
                    var contentDisposition = aoHeaders.ContentDisposition;
                    _logger.LogDebug("contentDisposition:\n" + contentDisposition);

                    string sAttachmentName = null;
                    if (contentDisposition != null)
                    {
                        sAttachmentName = contentDisposition.FileName;
                        _logger.LogDebug("sAttachmentName:\n" + sAttachmentName);



                        string sOutputFilePath = "";
                        if (!String.IsNullOrEmpty(sAttachmentName))
                            sOutputFilePath = sSavePath + sAttachmentName;
                        else
                            sOutputFilePath = sSavePath + sFileName;


                        string parentDirectoryName = Path.GetDirectoryName(sOutputFilePath);

                        //create the directory
                        if (!Directory.Exists(parentDirectoryName))
                        {
                            Directory.CreateDirectory(parentDirectoryName);
                        }

                        try
                        {
                            var oInputStream = await response.Content.ReadAsStreamAsync();
                            CopyStream(oInputStream, sOutputFilePath);
                        }
                        catch (Exception ex)
                        {
                            _logger.LogDebug("The file could not be downloaded.");

                            return string.Empty;
                        }

                        if (null != sAttachmentName && !sFileName.Equals(sAttachmentName) && sAttachmentName.ToLower().EndsWith(".zip"))
                        {
                            try
                            {
                                Unzip(sAttachmentName, sSavePath);
                            }
                            catch (Exception ex)
                            {
                                _logger.LogDebug("The zip file could not be unzipped.");

                                return string.Empty;
                            }
                        }

                        return sOutputFilePath;
                    }
                }

                _logger.LogDebug("No file Name was provided.");
            }
            else
            {
                _logger.LogDebug("response.StatusCode: " + response.StatusCode);
            }

            return string.Empty;
        }

        private void CopyStream(Stream stream, string destPath)
        {
            try
            {
                using (var fileStream = new FileStream(destPath, FileMode.Create, FileAccess.Write))
                {
                    stream.CopyTo(fileStream);
                }
            }
            catch (Exception ex)
            {
                _logger.LogError(ex.StackTrace);

                throw ex;
            }
        }

        private void Unzip(string sAttachmentName, string sPath)
        {
            if (!sPath.EndsWith("/") && !sPath.EndsWith("\\") && !sPath.EndsWith(Path.DirectorySeparatorChar))
            {
                sPath += Path.DirectorySeparatorChar;
            }

            string sZipFilePath = sPath + sAttachmentName;

            try
            {
                ZipFile.ExtractToDirectory(sZipFilePath, sPath);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex.StackTrace);

                throw ex;
            }
        }

        public async Task<PrimitiveResult> ProcessingMosaic(string sUrl, string sSessionId, MosaicSetting oMosaicSetting)
        {
            _logger.LogDebug("ProcessingMosaic()");

            _wasdiHttpClient.DefaultRequestHeaders.Clear();
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("Content-Type", "application/json");
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("x-session-token", sSessionId);


            var json = JsonConvert.SerializeObject(oMosaicSetting,
                new JsonSerializerSettings
                {
                    ContractResolver = new CamelCasePropertyNamesContractResolver()
                }
            );

            var requestPayload = new StringContent(json, Encoding.UTF8, "application/json");

            var response = await _wasdiHttpClient.PostAsync(sUrl, requestPayload);
            response.EnsureSuccessStatusCode();

            return await response.ConvertResponse<PrimitiveResult>();
        }

        public async Task<List<QueryResultViewModel>> SearchQueryList(string sUrl, string sSessionId, string sQueryBody)
        {
            _logger.LogDebug("SearchQueryList()");

            _wasdiHttpClient.DefaultRequestHeaders.Clear();
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("Content-Type", "application/json");
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("x-session-token", sSessionId);

            var requestPayload = new StringContent(sQueryBody, Encoding.UTF8, "application/json");

            var response = await _wasdiHttpClient.PostAsync(sUrl, requestPayload);
            response.EnsureSuccessStatusCode();

            return await response.ConvertResponse<List<QueryResultViewModel>>();
        }

        public async Task<PrimitiveResult> FilebufferDownload(string sBaseUrl, string sSessionId, string sWorkspaceId, string sProvider, string sFileUrl, string sFileName, string sBoundingBox)
        {
            _logger.LogDebug("FilebufferDownload()");

            _wasdiHttpClient.DefaultRequestHeaders.Clear();
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("Content-Type", "application/json");
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("x-session-token", sSessionId);


            Dictionary<string, string> parameters = new Dictionary<string, string>();
            parameters.Add("fileUrl", sFileUrl);
            parameters.Add("provider", sProvider);
            parameters.Add("workspace", sWorkspaceId);
            parameters.Add("bbox", sBoundingBox);

            if (sFileName != null)
                parameters.Add("name", sFileName);

            var formUrlEncodedContent = new FormUrlEncodedContent(parameters);
            string query = formUrlEncodedContent.ReadAsStringAsync().Result;
            if (!String.IsNullOrEmpty(query))
                query = "?" + query;

            string url = sBaseUrl + FILEBUFFER_DOWNLOAD_PATH + query;

            var response = await _wasdiHttpClient.GetAsync(url);
            response.EnsureSuccessStatusCode();

            return await response.ConvertResponse<PrimitiveResult>();
        }

        public async Task<string> AddProcessorsLog(string sWorkspaceBaseUrl, string sSessionId, string sProcessId, string sLogRow)
        {
            _logger.LogDebug("AddProcessorsLog()");

            _wasdiHttpClient.DefaultRequestHeaders.Clear();
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("Content-Type", "application/json");
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("x-session-token", sSessionId);

            var requestPayload = new StringContent(sLogRow, Encoding.UTF8, "application/json");


            Dictionary<string, string> parameters = new Dictionary<string, string>();
            parameters.Add("processworkspace", sProcessId);

            var formUrlEncodedContent = new FormUrlEncodedContent(parameters);
            string query = formUrlEncodedContent.ReadAsStringAsync().Result;
            if (!String.IsNullOrEmpty(query))
                query = "?" + query;

            string url = sWorkspaceBaseUrl + PROCESSORS_LOGS_ADD_PATH + query;

            var response = await _wasdiHttpClient.PostAsync(url, requestPayload);

            var data = string.Empty;

            if (response.IsSuccessStatusCode)
                data = await response.ConvertResponse<string>();

            return data;
        }

        public async Task<PrimitiveResult> ProcessingSubset(string sBaseUrl, string sSessionId, string sWorkspaceId, string sInputFile, string sOutputFile, string sSubsetSetting)
        {
            _logger.LogDebug("ProcessingSubset()");

            var requestPayload = new StringContent(sSubsetSetting, Encoding.UTF8, "application/json");

            _wasdiHttpClient.DefaultRequestHeaders.Clear();
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("Content-Type", "application/json");
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("x-session-token", sSessionId);


            Dictionary<string, string> parameters = new Dictionary<string, string>();
            parameters.Add("source", sInputFile);
            parameters.Add("name", sOutputFile);
            parameters.Add("workspace", sWorkspaceId);

            var formUrlEncodedContent = new FormUrlEncodedContent(parameters);
            string query = formUrlEncodedContent.ReadAsStringAsync().Result;
            if (!String.IsNullOrEmpty(query))
                query = "?" + query;

            string url = sBaseUrl + PROCESING_SUBSET_PATH + query;

            var response = await _wasdiHttpClient.PostAsync(url, requestPayload);
            response.EnsureSuccessStatusCode();

            return await response.ConvertResponse<PrimitiveResult>();
        }

        public async Task<RunningProcessorViewModel> ProcessorsRun(string sBaseUrl, string sSessionId, string sWorkspaceId, string sProcessorName, string sEncodedParams)
        {
            _logger.LogDebug("ProcessorsRun()");

            _wasdiHttpClient.DefaultRequestHeaders.Clear();
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("Content-Type", "application/json");
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("x-session-token", sSessionId);


            Dictionary<string, string> parameters = new Dictionary<string, string>();
            parameters.Add("workspace", sWorkspaceId);
            parameters.Add("name", sProcessorName);
            parameters.Add("encodedJson", sEncodedParams);

            var formUrlEncodedContent = new FormUrlEncodedContent(parameters);
            string query = formUrlEncodedContent.ReadAsStringAsync().Result;
            if (!String.IsNullOrEmpty(query))
                query = "?" + query;



            string sUrl = sBaseUrl;
            sUrl += PROCESSORS_RUN_PATH;
            sUrl += query;

            var response = await _wasdiHttpClient.GetAsync(sUrl);

            return await response.ConvertResponse<RunningProcessorViewModel>();
        }

    }
}
