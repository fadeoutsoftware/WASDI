using WasdiLib.Models;

namespace WasdiLib.Repositories
{
    internal interface IWorkspaceRepository
    {

        Task<List<Workspace>> GetWorkspaces(string sBaseUrl, string sSessionId);

        Task<WorkspaceEditorViewModel> GetWorkspace(string sBaseUrl, string sSessionId, string sWorkspaceId);

        Task<PrimitiveResult> CreateWorkspace(string sBaseUrl, string sSessionId, string sWorkspaceName, string sNodeCode);

        Task<string> DeleteWorkspace(string sBaseUrl, string sSessionId, string sWorkspaceId);

    }
}
