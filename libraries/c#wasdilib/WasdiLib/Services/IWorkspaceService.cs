using WasdiLib.Models;

namespace WasdiLib.Services
{
    internal interface IWorkspaceService
    {

        List<Workspace> GetWorkspaces(string sSessionId);

        WorkspaceEditorViewModel GetWorkspace(string sSessionId, string workspaceId);

        WasdiResponse CreateWorkspace(string sSessionId, string workspaceName, string nodeCode);

        string DeleteWorkspace(string sSessionId, string workspaceId);

    }
}
