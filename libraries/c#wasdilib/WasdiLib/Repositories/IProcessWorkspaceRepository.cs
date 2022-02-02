using WasdiLib.Models;

namespace WasdiLib.Repositories
{
    internal interface IProcessWorkspaceRepository
    {
        Task<List<ProcessWorkspace>> GetProcessWorkspacesByWorkspaceId(string sSessionId, string workspaceId);

        Task<ProcessWorkspace> GetProcessWorkspaceByProcessId(string sSessionId, string processId, string sWorkspaceBaseUrl);

        Task<string> GetProcessesStatus(string sSessionId, List<string> asIds, string sWorkspaceBaseUrl);

        Task<ProcessWorkspace> UpdateProcessStatus(string sSessionId, string processId, string sStatus,
            int iPerc, string sWorkspaceBaseUrl);

        Task<ProcessWorkspace> UpdateProcessPayload(string sSessionId, string processId, string sData,
            string sWorkspaceBaseUrl);

    }
}
