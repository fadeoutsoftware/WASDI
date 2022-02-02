using WasdiLib.Models;

namespace WasdiLib.Services
{
    internal interface IProductService
    {

        List<Product> GetProductsByWorkspaceId(string sSessionId, string sWorkspaceId);

    }
}
