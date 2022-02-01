using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;
using NLog.Extensions.Logging;

using WasdiLib.Services;
using WasdiLib.Repositories;

namespace WasdiLib.Configuration
{
    internal class Startup
    {
        public static IServiceProvider? ServiceProvider;
        public static IConfigurationRoot? ConfigurationRoot;

        public static void RegisterServices()
        {

            // add configuration
            ConfigurationRoot = new ConfigurationBuilder()
                .AddJsonFile(Path.GetFullPath("appsettings.json"), optional: false, reloadOnChange: true)
                .Build();

            // configure services
            var services = new ServiceCollection()
                .AddScoped<IWasdiService, WasdiService>()
                .AddScoped<IWasdiRepository, WasdiRepository>();


            // configure logger
            services
                .AddLogging(configure =>
                {
                    configure.AddNLog("WasdiLib.nlog.config");
                    configure.AddConsole();
                })
                .Configure<LoggerFilterOptions>(options => options.MinLevel = LogLevel.Debug);


            // add HttpClients extension
            services.ConfigureHttpClients(ConfigurationRoot);

            ServiceProvider = services.BuildServiceProvider();
        }

        public static void SetupErrorLogger()
        {
            AppDomain.CurrentDomain.UnhandledException += UnhandledExceptionTrapper;
        }

        static void UnhandledExceptionTrapper(object sender, UnhandledExceptionEventArgs exception)
        {
            var _logger = ServiceProvider?.GetService<ILogger<WasdiLib>>();
            _logger?.LogError(exception.ExceptionObject as Exception, "An error has occured" + Environment.NewLine);
            _logger?.LogWarning("Exiting because an error occured! Please check logs for error details.");
            Environment.Exit(1);
        }

    }
}
