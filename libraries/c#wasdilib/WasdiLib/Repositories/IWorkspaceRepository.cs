using WasdiLib.Models;

namespace WasdiLib.Repositories
{
    internal interface IWorkspaceRepository
    {

        Task<List<Workspace>> GetWorkspaces(string sSessionId);

        Task<WorkspaceEditorViewModel> GetWorkspace(string sSessionId, string sWorkspaceId);

        Task<WasdiResponse> CreateWorkspace(string sSessionId, string workspaceName, string nodeCode);

        Task<string> DeleteWorkspace(string sSessionId, string workspaceId);

    }
}
