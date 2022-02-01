using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;
using NLog.Extensions.Logging;

namespace WasdiLib.Client.Configuration
{
    internal class Startup
    {
        public static IServiceProvider ServiceProvider;

        public static void RegisterServices()
        {
            // configure services
            var services = new ServiceCollection();

            // configure logger
            services
                .AddLogging(configure =>
                {
                    configure.AddNLog("WasdiLib.Client.nlog.config");
                    configure.AddConsole();
                })
                .Configure<LoggerFilterOptions>(options => options.MinLevel = LogLevel.Debug);

            ServiceProvider = services.BuildServiceProvider();
        }
    }
}
