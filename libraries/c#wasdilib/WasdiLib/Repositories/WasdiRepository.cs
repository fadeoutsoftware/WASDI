using System.Text;

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

        public async Task<WasdiResponse> HelloWasdi()
        {
            _logger.LogDebug("HelloWasdi()");

            _wasdiHttpClient.DefaultRequestHeaders.Clear();
            var response = await _wasdiHttpClient.GetAsync(HELLO_WASDI_PATH);
            response.EnsureSuccessStatusCode();

            return await response.ConvertResponse<WasdiResponse>();
        }

        public async Task<LoginResponse> Login(string sUser, string sPassword)
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
            var response = await _wasdiHttpClient.PostAsync(LOGIN_PATH, requestPayload);
            response.EnsureSuccessStatusCode();

            return await response.ConvertResponse<LoginResponse>();
        }

        public async Task<LoginResponse> CheckSession(string sSessionId)
        {
            _logger.LogDebug("CheckSession()");

            _wasdiHttpClient.DefaultRequestHeaders.Clear();
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("Content-Type", "application/json");
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("x-session-token", sSessionId);

            var response = await _wasdiHttpClient.GetAsync(CHECK_SESSION_PATH);
            response.EnsureSuccessStatusCode();

            return await response.ConvertResponse<LoginResponse>();
        }

        public async Task<bool> FileExistsOnServer(string sSessionId, string workspaceId, string sWorkspaceBaseUrl,
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
            parameters.Add("workspace", workspaceId);

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

            WasdiResponse wasdiResponse = response.ConvertResponse<WasdiResponse>().GetAwaiter().GetResult();

            return wasdiResponse.BoolValue;
        }

        public async Task<WasdiResponse> CatalogUploadIngest(string sSessionId, string workspaceId, string sWorkspaceBaseUrl,
            string sFileName, string sStyle)
        {
            _logger.LogDebug("CatalogUploadIngest()");

            string sUrl = sWorkspaceBaseUrl;
            sUrl += CATALOG_UPLOAD_INGETS_PATH;


            Dictionary<string, string> parameters = new Dictionary<string, string>();
            parameters.Add("token", sSessionId);
            parameters.Add("file", sFileName);
            parameters.Add("workspace", workspaceId);

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

            return await response.ConvertResponse<WasdiResponse>();
        }

    }
}
