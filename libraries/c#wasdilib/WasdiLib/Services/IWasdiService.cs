using WasdiLib.Models;

namespace WasdiLib.Services
{
    internal interface IWasdiService
    {
        WasdiResponse HelloWasdi();

        LoginResponse Authenticate(string sUser, string sPassword);

        LoginResponse CheckSession(string sSessionId);

        bool FileExistsOnServer(string sSessionId, string workspaceId, string sWorkspaceBaseUrl, bool bIsMainNode, string sFileName);

        WasdiResponse CatalogUploadIngest(string sSessionId, string workspaceId, string sWorkspaceBaseUrl,
            string sFileName, string sStyle);
    }

}