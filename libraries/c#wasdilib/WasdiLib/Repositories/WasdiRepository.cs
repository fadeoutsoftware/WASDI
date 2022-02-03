using System.Text;
using System.Web;

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
        private const string CATALOG_UPLOAD_INGETS_PATH = "/catalog/upload/ingestinws";


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

            _wasdiHttpClient.DefaultRequestHeaders.Clear();
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

            string sUrl = sWorkspaceBaseUrl;
            if (bIsMainNode)
                sUrl += CATALOG_CHECK_FILE_EXISTS_ON_WASDI_PATH;
            else
                sUrl += CATALOG_CHECK_FILE_EXISTS_ON_NODE_PATH;


            Dictionary<string, string> parameters = new Dictionary<string, string>();
            parameters.Add("token", sSessionId);
            parameters.Add("filename", sFileName);
            parameters.Add("workspace", sWorkspaceId);

            var formUrlEncodedContent = new FormUrlEncodedContent(parameters);
            string query = formUrlEncodedContent.ReadAsStringAsync().Result;
            if (!String.IsNullOrEmpty(query))
            {
                query = "?" + query;
            }

            sUrl += query;


            var response = await _wasdiHttpClient.GetAsync(sUrl);

            var content = response.Content;
            if (content != null)
                _logger.LogDebug(content.ToString());

            if (!response.IsSuccessStatusCode)
                return false;

            _wasdiHttpClient.DefaultRequestHeaders.Clear();

            PrimitiveResult wasdiResponse = response.ConvertResponse<PrimitiveResult>().GetAwaiter().GetResult();

            return wasdiResponse.BoolValue;
        }

        public async Task<PrimitiveResult> CatalogUploadIngest(string sWorkspaceBaseUrl, string sSessionId, string sWorkspaceId,
            string sFileName, string sStyle)
        {
            _logger.LogDebug("CatalogUploadIngest()");

            string sUrl = sWorkspaceBaseUrl;
            sUrl += CATALOG_UPLOAD_INGETS_PATH;


            Dictionary<string, string> parameters = new Dictionary<string, string>();
            parameters.Add("token", sSessionId);
            parameters.Add("file", sFileName);
            parameters.Add("workspace", sWorkspaceId);

            if (String.IsNullOrEmpty(sStyle))
                parameters.Add("style", sStyle);

            var formUrlEncodedContent = new FormUrlEncodedContent(parameters);
            string query = formUrlEncodedContent.ReadAsStringAsync().Result;
            if (!String.IsNullOrEmpty(query))
            {
                query = "?" + query;
            }

            sUrl += query;

            var response = await _wasdiHttpClient.GetAsync(sUrl);

            var content = response.Content;
            if (content != null)
                _logger.LogDebug(content.ToString());

            //if (!response.IsSuccessStatusCode)
            //    return false;

            _wasdiHttpClient.DefaultRequestHeaders.Clear();

            return await response.ConvertResponse<PrimitiveResult>();
        }

        public async Task<PrimitiveResult> ProcessingMosaic(string sUrl, string sSessionId, MosaicSetting oMosaicSetting)
        {
            _logger.LogDebug("ProcessingMosaic()");


            var json = JsonConvert.SerializeObject(oMosaicSetting,
                new JsonSerializerSettings
                {
                    ContractResolver = new CamelCasePropertyNamesContractResolver()
                }
            );

            var requestPayload = new StringContent(json, Encoding.UTF8, "application/json");

            _wasdiHttpClient.DefaultRequestHeaders.Clear();
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("Content-Type", "application/json");
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("x-session-token", sSessionId);

            var response = await _wasdiHttpClient.PostAsync(sUrl, requestPayload);
            response.EnsureSuccessStatusCode();

            return await response.ConvertResponse<PrimitiveResult>();
        }

        public async Task<List<QueryResultViewModel>> SearchQueryList(string sUrl, string sSessionId, string sQueryBody)
        {
            _logger.LogDebug("SearchQueryList()");

            var requestPayload = new StringContent(sQueryBody, Encoding.UTF8, "application/json");

            _wasdiHttpClient.DefaultRequestHeaders.Clear();
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("Content-Type", "application/json");
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("x-session-token", sSessionId);

            var response = await _wasdiHttpClient.PostAsync(sUrl, requestPayload);
            response.EnsureSuccessStatusCode();

            return await response.ConvertResponse<List<QueryResultViewModel>>();
        }

        public async Task<PrimitiveResult> FilebufferDownload(string sBaseUrl, string sSessionId, string sWorkspaceId, string sProvider, string sFileUrl, string sFileName, string sBoundingBox)
        {
            _logger.LogDebug("FilebufferDownload()");

            string sUrl = sBaseUrl + "filebuffer/download?fileUrl=" + sFileUrl + "&provider=" + sProvider + "&workspace=" + sWorkspaceId + "&bbox=" + sBoundingBox;

            if (sFileName != null)
            {
                sUrl = sUrl + "&name=" + sFileName;
            }

            string sEncodedUrl = HttpUtility.UrlEncode(sUrl);


            _wasdiHttpClient.DefaultRequestHeaders.Clear();
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("Content-Type", "application/json");
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("x-session-token", sSessionId);
            var response = await _wasdiHttpClient.GetAsync(sEncodedUrl);
            response.EnsureSuccessStatusCode();

            return await response.ConvertResponse<PrimitiveResult>();
        }

    }
}
