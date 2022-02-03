using WasdiLib.Models;

namespace WasdiLib.Repositories
{
    internal interface IWasdiRepository
    {

        Task<PrimitiveResult> HelloWasdi(string sBaseUrl);

        Task<LoginResponse> Login(string sBaseUrl, string sUser, string sPassword);

        Task<LoginResponse> CheckSession(string sBaseUrl, string sSessionId);

        Task<bool> FileExistsOnServer(string sWorkspaceBaseUrl, string sSessionId, string sWorkspaceId, bool bIsMainNode, string sFileName);

        Task<PrimitiveResult> CatalogUploadIngest(string sWorkspaceBaseUrl, string sSessionId, string sWorkspaceId, string sFileName, string sStyle);

        Task<PrimitiveResult> ProcessingMosaic(string sUrl, string sSessionId, MosaicSetting oMosaicSetting);

        Task<List<QueryResultViewModel>> SearchQueryList(string sUrl, string sSessionId, string sQueryBody);

        Task<PrimitiveResult> FilebufferDownload(string sBaseUrl, string sSessionId, string sWorkspaceId, string sProvider, string sFileUrl, string sFileName, string sBoundingBox);

        Task<string> AddProcessorsLog(string sWorkspaceBaseUrl, string sSessionId, string sProcessId, string sLogRow);

    }
}