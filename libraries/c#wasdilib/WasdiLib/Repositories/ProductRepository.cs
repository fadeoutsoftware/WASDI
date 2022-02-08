using System.Net.Http.Headers;
using System.Text;

using Microsoft.Extensions.Logging;

using WasdiLib.Extensions;
using WasdiLib.Models;

namespace WasdiLib.Repositories
{
    internal class ProductRepository : IProductRepository
    {

        private const string PRODUCTS_BY_WS_ID_PATH = "product/byws";
        private const string PRODUCT_BY_NAME_PATH = "product/byname";
        private const string PRODUCT_DELETE_PATH = "product/delete";
        private const string PRODUCT_UPLOAD_FILE_PATH = "product/uploadfile";

        private readonly ILogger<ProductRepository> _logger;

        private readonly HttpClient _wasdiHttpClient;

        public ProductRepository(ILogger<ProductRepository> logger, IHttpClientFactory httpClientFactory)
        {
            _logger = logger;

            _wasdiHttpClient = httpClientFactory.CreateClient("WasdiApi");
        }

        public async Task<List<Product>> GetProductsByWorkspaceId(string sBaseUrl, string sSessionId, string sWorkspaceId)
        {
            _logger.LogDebug("GetProductsByWorkspaceId()");

            _wasdiHttpClient.DefaultRequestHeaders.Clear();
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("Content-Type", "application/json");
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("x-session-token", sSessionId);

            Dictionary<string, string> parameters = new Dictionary<string, string>();
            parameters.Add("workspace", sWorkspaceId);

            var content = new FormUrlEncodedContent(parameters);
            string query = content.ReadAsStringAsync().Result;
            if (!string.IsNullOrEmpty(query))
                query = "?" + query;

            var response = await _wasdiHttpClient.GetAsync(sBaseUrl + PRODUCTS_BY_WS_ID_PATH + query);
            response.EnsureSuccessStatusCode();

            return await response.ConvertResponse<List<Product>>();
        }

        public async Task<Product> GetProductByName(string sBaseUrl, string sSessionId, string sWorkspaceId, string sName)
        {
            _logger.LogDebug("GetProductByName()");

            _wasdiHttpClient.DefaultRequestHeaders.Clear();
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("Content-Type", "application/json");
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("x-session-token", sSessionId);

            Dictionary<string, string> parameters = new Dictionary<string, string>();
            parameters.Add("workspace", sWorkspaceId);
            parameters.Add("name", sName);

            var content = new FormUrlEncodedContent(parameters);
            string query = content.ReadAsStringAsync().Result;
            if (!string.IsNullOrEmpty(query))
                query = "?" + query;

            var response = await _wasdiHttpClient.GetAsync(sBaseUrl + PRODUCT_BY_NAME_PATH + query);
            response.EnsureSuccessStatusCode();

            return await response.ConvertResponse<Product>();
        }

        public async Task<PrimitiveResult> DeleteProduct(string sWorkspaceBaseUrl, string sSessionId, string sWorkspaceId, string sProduct)
        {
            _logger.LogDebug("DeleteProduct({0})", sWorkspaceId);

            _wasdiHttpClient.DefaultRequestHeaders.Clear();
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("Content-Type", "application/json");
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("x-session-token", sSessionId);

            Dictionary<string, string> parameters = new Dictionary<string, string>();
            parameters.Add("name", sProduct);
            parameters.Add("workspace", sWorkspaceId);
            parameters.Add("deletelayer", "true");
            parameters.Add("deletefile", "true");

            var content = new FormUrlEncodedContent(parameters);
            string query = content.ReadAsStringAsync().Result;

            if (!string.IsNullOrEmpty(query))
                query = "?" + query;


            var response = await _wasdiHttpClient.DeleteAsync(sWorkspaceBaseUrl + PRODUCT_DELETE_PATH + query);
            response.EnsureSuccessStatusCode();

            return await response.ConvertResponse<PrimitiveResult>();
        }

        public async Task<bool> UploadFile(string sWorkspaceBaseUrl, string sSessionId, string sWorkspaceId, string sSavePath, string sFileName)
        {
            _logger.LogDebug("UploadFile()");

            _wasdiHttpClient.DefaultRequestHeaders.Clear();

            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("x-session-token", sSessionId);


            string sBoundary = "**WasdiLib.C#**" + Guid.NewGuid().ToString() + "**WasdiLib.C#**";

            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("Content-Type", "multipart/form-data; boundary=" + sBoundary);
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("Connection", "Keep-Alive");
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("User-Agent", "WasdiLib.C#");


            Dictionary<string, string> parameters = new Dictionary<string, string>();
            parameters.Add("name", sFileName);
            parameters.Add("workspace", sWorkspaceId);

            var formUrlEncodedContent = new FormUrlEncodedContent(parameters);
            string query = formUrlEncodedContent.ReadAsStringAsync().Result;
            if (!String.IsNullOrEmpty(query))
                query = "?" + query;

            string sUrl = sWorkspaceBaseUrl;
            sUrl += PRODUCT_UPLOAD_FILE_PATH;

            sUrl += query;

            string sFullPath = sSavePath + sFileName;
            ByteArrayContent fileContent = new ByteArrayContent(await File.ReadAllBytesAsync(sFullPath));

            String headerValue = "form-data; name=\"file\"; filename=\"" + sFileName + "\"";
            byte[] bytes = Encoding.UTF8.GetBytes(headerValue);
            headerValue = Encoding.UTF8.GetString(bytes, 0, bytes.Length);

            fileContent.Headers.Add("Content-Disposition", headerValue);

            fileContent.Headers.ContentType = MediaTypeHeaderValue.Parse("multipart/form-data");


            MultipartFormDataContent form = new MultipartFormDataContent();
            form.Add(fileContent);

            var response = await _wasdiHttpClient.PostAsync(sUrl, form);


            _logger.LogInformation("response status code: {0}", response.StatusCode);

            var responseContent = await response.Content.ReadAsStringAsync();

            _logger.LogDebug(responseContent);

            _logger.LogInformation("Uploading is complete.");

            return response.IsSuccessStatusCode;
        }

    }
}
