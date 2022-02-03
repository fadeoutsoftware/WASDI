using WasdiLib.Models;

namespace WasdiLib.Repositories
{
    internal interface IProductRepository
    {

        Task<List<Product>> GetProductsByWorkspaceId(string sBaseUrl, string sSessionId, string sWorkspaceId);

    }
}
