using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Polly;

namespace WasdiLib.Configuration
{
    internal static class HttpClientsConfiguration
    {
        public static void ConfigureHttpClients(this IServiceCollection services, IConfiguration configuration)
        {
            services.AddHttpClient("WasdiApi", c =>
            {
                c!.BaseAddress = new Uri(configuration!["BASEURL"]!);
                c.DefaultRequestHeaders!.Add("User-Agent", "WasdiLib.C#");
            }).AddTransientHttpErrorPolicy(p =>
                p.WaitAndRetryAsync(3, _ => TimeSpan.FromMilliseconds(500)
            ));
        }
    }
}
