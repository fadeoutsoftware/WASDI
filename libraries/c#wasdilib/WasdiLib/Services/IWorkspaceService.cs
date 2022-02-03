using WasdiLib.Models;

namespace WasdiLib.Services
{
    internal interface IWorkspaceService
    {

        List<Workspace> GetWorkspaces(string sBaseUrl, string sSessionId);

        WorkspaceEditorViewModel GetWorkspace(string sBaseUrl, string sSessionId, string workspaceId);

        PrimitiveResult CreateWorkspace(string sBaseUrl, string sSessionId, string workspaceName, string nodeCode);

        string DeleteWorkspace(string sBaseUrl, string sSessionId, string workspaceId);

    }
}
