using WasdiLib.Models;

namespace WasdiLib.Repositories
{
    internal interface IProductRepository
    {

        Task<List<Product>> GetProductsByWorkspaceId(string sBaseUrl, string sSessionId, string sWorkspaceId);
        Task<Product> GetProductByName(string sBaseUrl, string sSessionId, string sWorkspaceId, string sName);

        Task<PrimitiveResult> DeleteProduct(string sWorkspaceBaseUrl, string sSessionId, string sWorkspaceId, string sProduct);

        Task<bool> UploadFile(string sWorkspaceBaseUrl, string sSessionId, string sWorkspaceId, string sSavePath, string sFileName);

    }
}
