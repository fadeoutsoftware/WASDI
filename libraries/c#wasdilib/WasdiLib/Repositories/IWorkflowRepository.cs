using WasdiLib.Models;

namespace WasdiLib.Repositories
{
    internal interface IWorkflowRepository
    {
        Task<List<Workflow>> GetWorkflows(string sSessionId);

        Task<WasdiResponse> CreateWorkflow(string sSessionId, Workflow workflow);

    }
}
