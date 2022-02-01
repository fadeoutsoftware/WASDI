/*using Microsoft.Extensions.Configuration;*/
using Microsoft.Extensions.Logging;

using WasdiLib.Models;
using WasdiLib.Repositories;

namespace WasdiLib.Services
{
    internal class WasdiService : IWasdiService
    {
        private readonly ILogger<WasdiService> _logger;
        private readonly IWasdiRepository _repository;

        public WasdiService(ILogger<WasdiService> logger, IWasdiRepository repository)
        {
            _logger = logger;
            _repository = repository;
        }

        public WasdiResponse HelloWasdi()
        {
            _logger.LogDebug("HelloWasdi()");

            return _repository.HelloWasdi().GetAwaiter().GetResult();
        }

    }
}
