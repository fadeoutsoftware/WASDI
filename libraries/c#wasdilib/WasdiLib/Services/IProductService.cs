using WasdiLib.Models;

namespace WasdiLib.Services
{
    internal interface IProductService
    {

        List<Product> GetProductsByWorkspaceId(string sBaseUrl, string sSessionId, string sWorkspaceId);

    }
}
