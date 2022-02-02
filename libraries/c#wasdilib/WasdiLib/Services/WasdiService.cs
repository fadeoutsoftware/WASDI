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

        public WasdiResponse HelloWasdi()
        {
            _logger.LogDebug("HelloWasdi()");

            return _repository.HelloWasdi().GetAwaiter().GetResult();
        }

        public LoginResponse Authenticate(string sUser, string sPassword)
        {
            return _repository.Login(sUser, sPassword).GetAwaiter().GetResult();
        }

        public LoginResponse CheckSession(string sSessionId)
        {
            return _repository.CheckSession(sSessionId).GetAwaiter().GetResult();
        }

        public bool FileExistsOnServer(string sSessionId, string workspaceId, string sWorkspaceBaseUrl, bool bIsMainNode, string sFileName)
        {
            _logger.LogDebug("FileExistsOnServer()");

            return _repository.FileExistsOnServer(sSessionId, workspaceId, sWorkspaceBaseUrl, bIsMainNode, sFileName)
                .GetAwaiter().GetResult();
        }

        public WasdiResponse CatalogUploadIngest(string sSessionId, string workspaceId, string sWorkspaceBaseUrl,
            string sFileName, string sStyle)
        {
            _logger.LogDebug("CatalogUploadIngest()");

            return _repository.CatalogUploadIngest(sSessionId, workspaceId, sWorkspaceBaseUrl, sFileName, sStyle)
                .GetAwaiter().GetResult();
        }

    }
}
