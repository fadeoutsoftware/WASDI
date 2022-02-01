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

    }
}
