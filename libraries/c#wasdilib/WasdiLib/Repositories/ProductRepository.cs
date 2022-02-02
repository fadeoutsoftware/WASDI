using Microsoft.Extensions.Logging;

using WasdiLib.Extensions;
using WasdiLib.Models;

namespace WasdiLib.Repositories
{
    internal class ProductRepository : IProductRepository
    {

        private const string PRODUCTS_BY_WS_ID_PATH = "product/byws";

        private readonly ILogger<ProductRepository> _logger;

        private readonly HttpClient _wasdiHttpClient;

        public ProductRepository(ILogger<ProductRepository> logger, IHttpClientFactory httpClientFactory)
        {
            _logger = logger;

            _wasdiHttpClient = httpClientFactory.CreateClient("WasdiApi");
        }
        public async Task<List<Product>> GetProductsByWorkspaceId(string sSessionId, string sWorkspaceId)
        {
            _logger.LogDebug("GetProductsByWorkspaceId()");

            Dictionary<string, string> parameters = new Dictionary<string, string>();
            parameters.Add("workspace", sWorkspaceId);

            var content = new FormUrlEncodedContent(parameters);
            string query = content.ReadAsStringAsync().Result;
            if (!string.IsNullOrEmpty(query))
            {
                query = "?" + query;
            }

            _wasdiHttpClient.DefaultRequestHeaders.Clear();
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("Content-Type", "application/json");
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("x-session-token", sSessionId);

            var response = await _wasdiHttpClient.GetAsync(PRODUCTS_BY_WS_ID_PATH + query);
            response.EnsureSuccessStatusCode();

            return await response.ConvertResponse<List<Product>>();
        }

    }
}
