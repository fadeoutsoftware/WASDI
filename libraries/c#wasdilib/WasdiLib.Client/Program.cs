using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;

using WasdiLib.Client.Configuration;

namespace WasdiLib.Client
{
    internal class Program
    {
        static void Main(string[] args)
        {
            Startup.RegisterServices();
            var _logger = Startup.ServiceProvider.GetService<ILogger<Program>>();

            _logger.LogInformation("Program.Main()");

            WasdiLib wasdi = new WasdiLib();
            _logger.LogInformation(wasdi.HelloWasdi());
        }

    }

}
