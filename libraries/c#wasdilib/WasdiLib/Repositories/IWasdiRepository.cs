using WasdiLib.Models;

namespace WasdiLib.Repositories
{
    internal interface IWasdiRepository
    {

        Task<WasdiResponse> HelloWasdi();

        Task<LoginResponse> Login(string sUser, string sPassword);

        Task<LoginResponse> CheckSession(string sSessionId);

        Task<bool> FileExistsOnServer(string sSessionId, string workspaceId, string sWorkspaceBaseUrl, bool bIsMainNode, string sFileName);

        Task<WasdiResponse> CatalogUploadIngest(string sSessionId, string workspaceId, string sWorkspaceBaseUrl,
            string sFileName, string sStyle);

    }
}