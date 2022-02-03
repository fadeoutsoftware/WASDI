using Microsoft.Extensions.Logging;

using WasdiLib.Models;
using WasdiLib.Repositories;

namespace WasdiLib.Services
{
    internal class WasdiService : IWasdiService
    {
        private readonly ILogger<WasdiService> _logger;
        private readonly IWasdiRepository _repository;

        public WasdiService(ILogger<WasdiService> logger, IWasdiRepository repository)
        {
            _logger = logger;
            _repository = repository;
        }

        public PrimitiveResult HelloWasdi(string sBaseUrl)
        {
            _logger.LogDebug("HelloWasdi()");

            return _repository.HelloWasdi(sBaseUrl).GetAwaiter().GetResult();
        }

        public LoginResponse Authenticate(string sBaseUrl, string sUser, string sPassword)
        {
            return _repository.Login(sBaseUrl, sUser, sPassword).GetAwaiter().GetResult();
        }

        public LoginResponse CheckSession(string sBaseUrl, string sSessionId)
        {
            return _repository.CheckSession(sBaseUrl, sSessionId).GetAwaiter().GetResult();
        }

        public bool FileExistsOnServer(string sWorkspaceBaseUrl, string sSessionId, string sWorkspaceId, bool bIsMainNode, string sFileName)
        {
            _logger.LogDebug("FileExistsOnServer()");

            return _repository.FileExistsOnServer(sWorkspaceBaseUrl, sSessionId, sWorkspaceId, bIsMainNode, sFileName)
                .GetAwaiter().GetResult();
        }

        public PrimitiveResult CatalogUploadIngest(string sWorkspaceBaseUrl, string sSessionId, string sWorkspaceId, string sFileName, string sStyle)
        {
            _logger.LogDebug("CatalogUploadIngest()");

            return _repository.CatalogUploadIngest(sWorkspaceBaseUrl, sSessionId, sWorkspaceId, sFileName, sStyle)
                .GetAwaiter().GetResult();
        }

        public PrimitiveResult ProcessingMosaic(string sUrl, string sSessionId, MosaicSetting oMosaicSetting)
        {
            _logger.LogDebug("ProcessingMosaic()");

            return _repository.ProcessingMosaic(sUrl, sSessionId, oMosaicSetting)
                .GetAwaiter().GetResult();
        }

        public List<QueryResultViewModel> SearchQueryList(string sUrl, string sSessionId, string sQueryBody)
        {
            _logger.LogDebug("SearchQueryList()");

            return _repository.SearchQueryList(sUrl, sSessionId, sQueryBody)
                .GetAwaiter().GetResult();
        }

        public PrimitiveResult FilebufferDownload(string sBaseUrl, string sSessionId, string sWorkspaceId, string sProvider, string sFileUrl, string sFileName, string sBoundingBox)
        {
            _logger.LogDebug("FilebufferDownload()");

            return _repository.FilebufferDownload(sBaseUrl, sSessionId, sWorkspaceId, sProvider, sFileUrl, sFileName, sBoundingBox)
                .GetAwaiter().GetResult();
        }
    }
}
