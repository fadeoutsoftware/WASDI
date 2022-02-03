using WasdiLib.Models;

namespace WasdiLib.Services
{
    internal interface IWorkflowService
    {
        List<Workflow> GetWorkflows(string sBaseUrl, string sSessionId);

        PrimitiveResult CreateWorkflow(string sBaseUrl, string sSessionId, Workflow oWorkflow);

    }
}
