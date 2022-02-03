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
            var services = new ServiceCollection()
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
