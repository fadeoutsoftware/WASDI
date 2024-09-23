using WasdiLib.Models;

namespace WasdiLib.Repositories
{
    internal interface IProcessWorkspaceRepository
    {
        Task<List<ProcessWorkspace>> GetProcessWorkspacesByWorkspaceId(string sWorkspaceBaseUrl, string sSessionId, string sWorkspaceId);

        Task<List<ProcessWorkspace>> GetProcessWorkspacesByWorkspace(string sWorkspaceBaseUrl, string sSessionId, string sWorkspaceId,
            int iStartIndex, int? iEndIndex, string sStatus, string sOperationType, string sNamePattern);

        Task<ProcessWorkspace> GetProcessWorkspaceByProcessId(string sWorkspaceBaseUrl, string sSessionId, string sProcessId);

        Task<ProcessWorkspace> SetSubPid(string sWorkspaceBaseUrl, string sSessionId, string sProcessId, int iSubPid);

        Task<string> GetProcessesStatus(string sWorkspaceBaseUrl, string sSessionId, List<string> asIds);

        Task<string> GetProcessPayload(string sWorkspaceBaseUrl, string sSessionId, string sProcessObjId);

        Task<ProcessWorkspace> UpdateProcessStatus(string sWorkspaceBaseUrl, string sSessionId, string sProcessId, string sStatus, int iPerc);

        Task<ProcessWorkspace> UpdateProcessPayload(string sWorkspaceBaseUrl, string sSessionId, string sProcessId, string sData);

    }
}
