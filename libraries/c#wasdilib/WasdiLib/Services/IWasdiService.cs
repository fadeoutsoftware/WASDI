using WasdiLib.Models;

namespace WasdiLib.Services
{
    internal interface IWasdiService
    {
        PrimitiveResult HelloWasdi(string sBaseUrl);

        LoginResponse Authenticate(string sBaseUrl, string sUser, string sPassword);

        LoginResponse CheckSession(string sBaseUrl, string sSessionId);

        bool FileExistsOnServer(string sWorkspaceBaseUrl, string sSessionId, string sWorkspaceId, bool bIsMainNode, string sFileName);

        PrimitiveResult CatalogUploadIngest(string sWorkspaceBaseUrl, string sSessionId, string sWorkspaceId, string sFileName, string sStyle);

        PrimitiveResult ProcessingMosaic(string sUrl, string sSessionId, MosaicSetting oMosaicSetting);

        List<QueryResultViewModel> SearchQueryList(string sUrl, string sSessionId, string sQueryBody);

        PrimitiveResult FilebufferDownload(string sBaseUrl, string sSessionId, string sWorkspaceId, string sProvider, string sFileUrl, string sFileName, string sBoundingBox);
    }

}