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

            // configure services
            var aoServicies = new ServiceCollection()
                .AddScoped<IProcessWorkspaceService, ProcessWorkspaceService>()
                .AddScoped<IProductService, ProductService>()
                .AddScoped<IWasdiService, WasdiService>()
                .AddScoped<IWorkflowService, WorkflowService>()
                .AddScoped<IWorkspaceService, WorkspaceService>()

                .AddScoped<IProcessWorkspaceRepository, ProcessWorkspaceRepository>()
                .AddScoped<IProductRepository, ProductRepository>()
                .AddScoped<IWasdiRepository, WasdiRepository>()
                .AddScoped<IWorkflowRepository, WorkflowRepository>()
                .AddScoped<IWorkspaceRepository, WorkspaceRepository>();

            // configure logger
            aoServicies
                .AddLogging(oConfigure =>
                {
                    if (File.Exists("WasdiLib.nlog.config")) oConfigure.AddNLog("WasdiLib.nlog.config");
                    oConfigure.AddConsole();
                })
                .Configure<LoggerFilterOptions>(oOptions => oOptions.MinLevel = LogLevel.Error);


            // add HttpClients extension
            aoServicies.ConfigureHttpClients(ConfigurationRoot);

            ServiceProvider = aoServicies.BuildServiceProvider();
        }

        public static void LoadConfiguration(string path)
        {
            if (string.IsNullOrWhiteSpace(path))
                path = Path.GetFullPath("appsettings.json");

            // add configuration
            ConfigurationRoot = new ConfigurationBuilder()
                .AddJsonFile(path, optional: false, reloadOnChange: true)
                .Build();
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
