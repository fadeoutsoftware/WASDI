using WasdiLib.Models;

namespace WasdiLib.Services
{
    internal interface IProductService
    {

        List<Product> GetProductsByWorkspaceId(string sBaseUrl, string sSessionId, string sWorkspaceId);

        PrimitiveResult DeleteProduct(string sWorkspaceBaseUrl, string sSessionId, string sWorkspaceId, string sProduct);

    }
}
