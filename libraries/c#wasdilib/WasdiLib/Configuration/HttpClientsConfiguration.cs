using Microsoft.Extensions.DependencyInjection;
using Polly;

namespace WasdiLib.Configuration
{
    internal static class HttpClientsConfiguration
    {
        public static void ConfigureHttpClients(this IServiceCollection services)
        {
            services.AddHttpClient("WasdiApi", c =>
            {
                c.DefaultRequestHeaders!.Add("User-Agent", "WasdiLib.C#");
            }).AddTransientHttpErrorPolicy(p =>
                p.WaitAndRetryAsync(3, _ => TimeSpan.FromMilliseconds(500)
            ));
        }
    }
}
