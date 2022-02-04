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

        public List<ProcessWorkspace> GetProcessWorkspacesByWorkspaceId(string sWorkspaceBaseUrl, string sSessionId, string sWorkspaceId)
        {
            _logger.LogDebug("GetProcessWorkspacesByProcessId({0})", sWorkspaceId);

            return _repository.GetProcessWorkspacesByWorkspaceId(sWorkspaceBaseUrl, sSessionId, sWorkspaceId).GetAwaiter().GetResult();
        }

        public List<ProcessWorkspace> GetProcessWorkspacesByWorkspace(string sWorkspaceBaseUrl, string sSessionId, string sWorkspaceId,
            int iStartIndex, int iEndIndex, string sStatus, string sOperationType, string sNamePattern)
        {
            _logger.LogDebug("GetProcessWorkspacesByProcessId({0}, {1}, {2}, {3}, {4}, {5})", sWorkspaceId, iStartIndex, iEndIndex, sStatus, sOperationType, sNamePattern);

            return _repository.GetProcessWorkspacesByWorkspace(sWorkspaceBaseUrl, sSessionId, sWorkspaceId, iStartIndex, iEndIndex, sStatus, sOperationType, sNamePattern).GetAwaiter().GetResult();
        }

        public ProcessWorkspace GetProcessWorkspaceByProcessId(string sWorkspaceBaseUrl, string sSessionId, string sProcessId)
        {
            _logger.LogDebug("GetProcessWorkspaceByProcessId({0})", sProcessId);

            return _repository.GetProcessWorkspaceByProcessId(sWorkspaceBaseUrl, sSessionId, sProcessId).GetAwaiter().GetResult();
        }

        public string GetProcessesStatus(string sWorkspaceBaseUrl, string sSessionId, List<string> asIds)
        {
            _logger.LogDebug("GetProcessesStatus()");

            return _repository.GetProcessesStatus(sWorkspaceBaseUrl, sSessionId, asIds).GetAwaiter().GetResult();
        }

        public ProcessWorkspace UpdateProcessStatus(string sWorkspaceBaseUrl, string sSessionId, string sProcessId, string sStatus, int iPerc)
        {
            _logger.LogDebug("UpdateProcessStatus()");

            return _repository.UpdateProcessStatus(sWorkspaceBaseUrl, sSessionId, sProcessId, sStatus, iPerc).GetAwaiter().GetResult();
        }

        public ProcessWorkspace UpdateProcessPayload(string sWorkspaceBaseUrl, string sSessionId, string sProcessId, string sData)
        {
            _logger.LogDebug("UpdateProcessPayload()");

            return _repository.UpdateProcessPayload(sWorkspaceBaseUrl, sSessionId, sProcessId, sData).GetAwaiter().GetResult();
        }

    }
}
