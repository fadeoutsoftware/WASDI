using Microsoft.Extensions.Logging;

using WasdiLib.Models;
using WasdiLib.Repositories;

namespace WasdiLib.Services
{
    internal class ProcessWorkspaceService : IProcessWorkspaceService
    {
        private readonly ILogger<ProcessWorkspaceService> _logger;
        private readonly IProcessWorkspaceRepository _repository;

        public ProcessWorkspaceService(ILogger<ProcessWorkspaceService> logger, IProcessWorkspaceRepository repository)
        {
            _logger = logger;
            _repository = repository;
        }

        public List<ProcessWorkspace> GetProcessWorkspacesByWorkspaceId(string sSessionId, string workspaceId)
        {
            _logger.LogDebug("GetProcessWorkspacesByProcessId({0})", workspaceId);

            return _repository.GetProcessWorkspacesByWorkspaceId(sSessionId, workspaceId).GetAwaiter().GetResult();
        }

        public ProcessWorkspace GetProcessWorkspaceByProcessId(string sSessionId, string processId, string sWorkspaceBaseUrl)
        {
            _logger.LogDebug("GetProcessWorkspaceByProcessId({0})", processId);

            return _repository.GetProcessWorkspaceByProcessId(sSessionId, processId, sWorkspaceBaseUrl).GetAwaiter().GetResult();
        }

        public string GetProcessesStatus(string sSessionId, List<string> asIds, string sWorkspaceBaseUrl)
        {
            _logger.LogDebug("GetProcessesStatus()");

            return _repository.GetProcessesStatus(sSessionId, asIds, sWorkspaceBaseUrl).GetAwaiter().GetResult();
        }

        public ProcessWorkspace UpdateProcessStatus(string sSessionId, string processId, string sStatus,
            int iPerc, string sWorkspaceBaseUrl)
        {
            _logger.LogDebug("UpdateProcessStatus()");

            return _repository.UpdateProcessStatus(sSessionId, processId, sStatus, iPerc, sWorkspaceBaseUrl)
                .GetAwaiter().GetResult();
        }

        public ProcessWorkspace UpdateProcessPayload(string sSessionId, string processId, string sData,
            string sWorkspaceBaseUrl)
        {
            _logger.LogDebug("UpdateProcessPayload()");

            return _repository.UpdateProcessPayload(sSessionId, processId, sData, sWorkspaceBaseUrl)
                .GetAwaiter().GetResult();
        }

    }
}
