using Microsoft.Extensions.Logging;

using WasdiLib.Models;
using WasdiLib.Repositories;

namespace WasdiLib.Services
{
    internal class ProductService : IProductService
    {
        private readonly ILogger<ProductService> _logger;
        private readonly IProductRepository _repository;

        public ProductService(ILogger<ProductService> logger, IProductRepository repository)
        {
            _logger = logger;
            _repository = repository;
        }

        public List<Product> GetProductsByWorkspaceId(string sSessionId, string workspaceId)
        {
            _logger.LogDebug("GetProcessWorkspacesByProcessId({0})", workspaceId);

            return _repository.GetProductsByWorkspaceId(sSessionId, workspaceId).GetAwaiter().GetResult();
        }

    }
}
