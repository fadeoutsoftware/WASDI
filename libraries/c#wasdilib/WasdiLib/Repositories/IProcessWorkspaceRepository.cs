using WasdiLib.Models;

namespace WasdiLib.Repositories
{
    internal interface IProcessWorkspaceRepository
    {
        Task<List<ProcessWorkspace>> GetProcessWorkspacesByWorkspaceId(string sWorkspaceBaseUrl, string sSessionId, string sWorkspaceId);

        Task<ProcessWorkspace> GetProcessWorkspaceByProcessId(string sWorkspaceBaseUrl, string sSessionId, string sProcessId);

        Task<string> GetProcessesStatus(string sWorkspaceBaseUrl, string sSessionId, List<string> asIds);

        Task<ProcessWorkspace> UpdateProcessStatus(string sWorkspaceBaseUrl, string sSessionId, string sProcessId, string sStatus, int iPerc);

        Task<ProcessWorkspace> UpdateProcessPayload(string sWorkspaceBaseUrl, string sSessionId, string sProcessId, string sData);

    }
}
