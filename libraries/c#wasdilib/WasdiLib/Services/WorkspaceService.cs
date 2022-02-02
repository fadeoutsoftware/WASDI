using Microsoft.Extensions.Logging;

using WasdiLib.Models;
using WasdiLib.Repositories;

namespace WasdiLib.Services
{
    internal class WorkspaceService : IWorkspaceService
    {
        private readonly ILogger<WorkspaceService> _logger;
        private readonly IWorkspaceRepository _repository;

        public WorkspaceService(ILogger<WorkspaceService> logger, IWorkspaceRepository repository)
        {
            _logger = logger;
            _repository = repository;
        }

        public List<Workspace> GetWorkspaces(string sSessionId)
        {
            _logger.LogDebug("GetWorkspaces()");

            return _repository.GetWorkspaces(sSessionId).GetAwaiter().GetResult();
        }

        public WorkspaceEditorViewModel GetWorkspace(string sSessionId, string workspaceId)
        {
            _logger.LogDebug("GetWorkspace({0})", workspaceId);

            return _repository.GetWorkspace(sSessionId, workspaceId).GetAwaiter().GetResult();
        }

        public WasdiResponse CreateWorkspace(string sSessionId, string workspaceName, string nodeCode)
        {
            _logger.LogDebug("CreateWorkspace({workspaceName}, {nodeCode})", workspaceName, nodeCode);

            return _repository.CreateWorkspace(sSessionId, workspaceName, nodeCode).GetAwaiter().GetResult();
        }

        public string DeleteWorkspace(string sSessionId, string workspaceId)
        {
            _logger.LogDebug("DeleteWorkspace({0})", workspaceId);

            return _repository.DeleteWorkspace(sSessionId, workspaceId).GetAwaiter().GetResult();
        }

    }
}
