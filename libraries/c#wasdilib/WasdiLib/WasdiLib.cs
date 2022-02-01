using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;

using WasdiLib.Configuration;
using WasdiLib.Helpers;
using WasdiLib.Models;
using WasdiLib.Services;

namespace WasdiLib
{
    public class WasdiLib
    {

        private readonly ILogger<WasdiLib> _logger;


        private IWasdiService _wasdiService;

        public WasdiLib()
        {
            Startup.RegisterServices();
            Startup.SetupErrorLogger();

            _logger = Startup.ServiceProvider.GetService<ILogger<WasdiLib>>();
            _logger.LogDebug("WasdiLib()");

            _wasdiService = Startup.ServiceProvider.GetService<IWasdiService>();

        }

        public string HelloWasdi()
        {
            _logger.LogDebug("HelloWasdi()");

            WasdiResponse wasdiResponse = _wasdiService.HelloWasdi();

            string message = String.Empty;

            if (wasdiResponse != null)
                message = wasdiResponse?.StringValue;

            return message;
        }


    }
}
