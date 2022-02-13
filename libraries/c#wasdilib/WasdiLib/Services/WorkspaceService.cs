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

        public List<Workspace> GetWorkspaces(string sBaseUrl, string sSessionId)
        {
            _logger.LogDebug("GetWorkspaces()");

            return _repository.GetWorkspaces(sBaseUrl, sSessionId).GetAwaiter().GetResult();
        }

        public WorkspaceEditorViewModel GetWorkspace(string sBaseUrl, string sSessionId, string workspaceId)
        {
            _logger.LogDebug("GetWorkspace({0})", workspaceId);

            return _repository.GetWorkspace(sBaseUrl, sSessionId, workspaceId).GetAwaiter().GetResult();
        }

        public PrimitiveResult CreateWorkspace(string sBaseUrl, string sSessionId, string workspaceName, string nodeCode)
        {
            _logger.LogDebug("CreateWorkspace({workspaceName}, {nodeCode})", workspaceName, nodeCode);

            return _repository.CreateWorkspace(sBaseUrl, sSessionId, workspaceName, nodeCode).GetAwaiter().GetResult();
        }

        public string DeleteWorkspace(string sBaseUrl, string sSessionId, string workspaceId)
        {
            _logger.LogDebug("DeleteWorkspace({0})", workspaceId);

            return _repository.DeleteWorkspace(sBaseUrl, sSessionId, workspaceId).GetAwaiter().GetResult();
        }

    }
}
