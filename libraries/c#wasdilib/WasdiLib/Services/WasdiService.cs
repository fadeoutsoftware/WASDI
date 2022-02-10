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

        public PrimitiveResult Hello(string sBaseUrl)
        {
            _logger.LogDebug("HelloWasdi()");

            return _repository.Hello(sBaseUrl).GetAwaiter().GetResult();
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

            return _repository.FileExistsOnServer(sWorkspaceBaseUrl, sSessionId, sWorkspaceId, bIsMainNode, sFileName).GetAwaiter().GetResult();
        }

        public string CatalogDownload(string sWorkspaceBaseUrl, string sSessionId, string sWorkspaceId, string sSavePath, string sFileName)
        {
            _logger.LogDebug("CatalogDownload()");

            return _repository.CatalogDownload(sWorkspaceBaseUrl, sSessionId, sWorkspaceId, sSavePath, sFileName).GetAwaiter().GetResult();
        }

        public PrimitiveResult AsynchCopyFileToSftp(string sWorkspaceBaseUrl, string sSessionId, string sWorkspaceId, bool bIsOnServer, string sRelativePath, string sFileName, string sProcessId)
        {
            _logger.LogDebug("AsynchCopyFileToSftp()");

            return _repository.AsynchCopyFileToSftp(sWorkspaceBaseUrl, sSessionId, sWorkspaceId, bIsOnServer, sRelativePath, sFileName, sProcessId).GetAwaiter().GetResult();
        }

        public PrimitiveResult CatalogUploadIngest(string sWorkspaceBaseUrl, string sSessionId, string sWorkspaceId, string sFileName, string sStyle)
        {
            _logger.LogDebug("CatalogUploadIngest()");

            return _repository.CatalogUploadIngest(sWorkspaceBaseUrl, sSessionId, sWorkspaceId, sFileName, sStyle).GetAwaiter().GetResult();
        }

        public PrimitiveResult ProcessingMosaic(string sUrl, string sSessionId, MosaicSetting oMosaicSetting)
        {
            _logger.LogDebug("ProcessingMosaic()");

            return _repository.ProcessingMosaic(sUrl, sSessionId, oMosaicSetting).GetAwaiter().GetResult();
        }

        public List<QueryResultViewModel> SearchQueryList(string sUrl, string sSessionId, string sQueryBody)
        {
            _logger.LogDebug("SearchQueryList()");

            return _repository.SearchQueryList(sUrl, sSessionId, sQueryBody).GetAwaiter().GetResult();
        }

        public PrimitiveResult FilebufferDownload(string sBaseUrl, string sSessionId, string sWorkspaceId, string sProvider, string sFileUrl, string sFileName, string sBoundingBox)
        {
            _logger.LogDebug("FilebufferDownload()");

            return _repository.FilebufferDownload(sBaseUrl, sSessionId, sWorkspaceId, sProvider, sFileUrl, sFileName, sBoundingBox).GetAwaiter().GetResult();
        }

        public string AddProcessorsLog(string sWorkspaceBaseUrl, string sSessionId, string sProcessId, string sLogRow)
        {
            _logger.LogDebug("AddProcessorsLog()");

            return _repository.AddProcessorsLog(sWorkspaceBaseUrl, sSessionId, sProcessId, sLogRow).GetAwaiter().GetResult();
        }

        public PrimitiveResult ProcessingSubset(string sBaseUrl, string sSessionId, string sWorkspaceId, string sInputFile, string sOutputFile, string sSubsetSetting)
        {
            _logger.LogDebug("ProcessingSubset()");

            return _repository.ProcessingSubset(sBaseUrl, sSessionId, sWorkspaceId, sInputFile, sOutputFile, sSubsetSetting).GetAwaiter().GetResult();
        }

        public PrimitiveResult ProcessingMultisubset(string sBaseUrl, string sSessionId, string sWorkspaceId, bool bIsOnServer, string sInputFile, string sProcessId, Dictionary<string, object> payloadDictionary)
        {
            _logger.LogDebug("ProcessingMultisubset()");

            return _repository.ProcessingMultisubset(sBaseUrl, sSessionId, sWorkspaceId, bIsOnServer, sInputFile, sProcessId, payloadDictionary).GetAwaiter().GetResult();
        }

        public RunningProcessorViewModel ProcessorsRun(string sBaseUrl, string sSessionId, string sWorkspaceId, string sProcessorName, string sEncodedParams)
        {
            _logger.LogDebug("ProcessorsRun()");

            return _repository.ProcessorsRun(sBaseUrl, sSessionId, sWorkspaceId, sProcessorName, sEncodedParams).GetAwaiter().GetResult();
        }

    }
}
