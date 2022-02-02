using WasdiLib.Models;

namespace WasdiLib.Services
{
    internal interface IWorkflowService
    {
        List<Workflow> GetWorkflows(string sSessionId);

        WasdiResponse CreateWorkflow(string sSessionId, Workflow workflow);

    }
}
