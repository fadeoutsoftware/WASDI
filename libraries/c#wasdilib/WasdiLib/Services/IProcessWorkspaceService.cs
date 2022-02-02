using WasdiLib.Models;


namespace WasdiLib.Services
{
    internal interface IProcessWorkspaceService
    {
        List<ProcessWorkspace> GetProcessWorkspacesByWorkspaceId(string sSessionId, string workspaceId);

        ProcessWorkspace GetProcessWorkspaceByProcessId(string sSessionId, string processId, string sWorkspaceBaseUrl);

        string GetProcessesStatus(string sSessionId, List<string> asIds, string sWorkspaceBaseUrl);

        ProcessWorkspace UpdateProcessStatus(string sSessionId, string processId, string sStatus,
            int iPerc, string sWorkspaceBaseUrl);

        ProcessWorkspace UpdateProcessPayload(string sSessionId, string processId, string sData,
            string sWorkspaceBaseUrl);

    }
}
