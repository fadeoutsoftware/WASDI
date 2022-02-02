using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;

using Newtonsoft.Json;

using WasdiLib.Helpers;
using WasdiLib.Models;


using WasdiLib.Client.Configuration;

namespace WasdiLib.Client
{
    internal class Program
    {

        private static string _workspaceName = "ERA5_WORKSPACE_TEST";
        static void Main(string[] args)
        {
            Startup.RegisterServices();
            var _logger = Startup.ServiceProvider.GetService<ILogger<Program>>();

            _logger.LogInformation("Program.Main()");

            WasdiLib wasdi = new WasdiLib();
            wasdi.Init();


            HelloWasdi(wasdi);
            GetWorkspaces(wasdi);
            CreateWorkspace_DeleteWorkspace(wasdi);
            GetProcessWorkspacesByWorkspaceId(wasdi);
            GetWorkspaceIdByName(wasdi);
            GetProductsByWorkspaceId(wasdi);
            GetWorkflows(wasdi);
        }

        private static void HelloWasdi(WasdiLib wasdi)
        {
            Console.WriteLine();
            Console.WriteLine("HelloWasdi:");

            Console.WriteLine(wasdi.HelloWasdi());

            Console.WriteLine();
        }

        private static void GetWorkspaces(WasdiLib wasdi)
        {
            Console.WriteLine();
            Console.WriteLine("GetWorkspaces:");

            List<Workspace> workspaces = wasdi.GetWorkspaces();

            foreach (Workspace workspace in workspaces)
            {
                Console.WriteLine(JsonConvert.SerializeObject(workspace));
            }

            Console.WriteLine();
        }

        private static void CreateWorkspace_DeleteWorkspace(WasdiLib wasdi)
        {
            Console.WriteLine("");
            Console.WriteLine("CreateWorkspace_DeleteWorkspace:");

            string workspaceName = "TestWorkspace";

            string workspaceId = wasdi.GetWorkspaceIdByName(workspaceName);

            if (workspaceId == null)
                workspaceId = wasdi.CreateWorkspace(workspaceName);

            Console.WriteLine("Workspace: " + "Name: " + workspaceName + "; " + "Id: " + workspaceId);


            string outcome = wasdi.DeleteWorkspace(workspaceId);
            Console.WriteLine("Deleted Workspace: {0}", outcome);

            Console.WriteLine("");
        }

        private static void GetProcessWorkspacesByWorkspaceId(WasdiLib wasdi)
        {
            Console.WriteLine();
            Console.WriteLine("GetProcessWorkspacesByWorkspaceId:");


            string workspaceId = wasdi.GetWorkspaceIdByName(_workspaceName);

            List<ProcessWorkspace> processWorkspaces = wasdi.GetProcessWorkspacesByWorkspaceId(workspaceId);

            foreach (ProcessWorkspace processWorkspace in processWorkspaces)
            {
                Console.WriteLine(SerializationHelper.ToJson(processWorkspace));
            }

            Console.WriteLine();
        }

        private static void GetWorkspaceIdByName(WasdiLib wasdi)
        {
            Console.WriteLine();
            Console.WriteLine("GetWorkspaceIdByName:");


            string workspaceId = wasdi.GetWorkspaceIdByName(_workspaceName);

            string workspaceUrl = wasdi.GetWorkspaceUrlByWsId(workspaceId);

            Console.WriteLine("workspaceUrl: " + workspaceUrl);

            Console.WriteLine();
        }

        private static void GetProductsByWorkspaceId(WasdiLib wasdi)
        {
            Console.WriteLine();
            Console.WriteLine("GetProductsByWorkspaceId:");


            string workspaceId = wasdi.GetWorkspaceIdByName(_workspaceName);

            List<string> productList = wasdi.GetProductsByWorkspaceId(workspaceId);

            foreach (string product in productList)
            {
                Console.WriteLine(product);
            }

            Console.WriteLine();
        }

        private static void GetWorkflows(WasdiLib wasdi)
        {
            Console.WriteLine();
            Console.WriteLine("GetWorkflows:");

            List<Workflow> workflows = wasdi.GetWorkflows();

            foreach (Workflow workflow in workflows)
            {
                Console.WriteLine(JsonConvert.SerializeObject(workflow));
            }

            Console.WriteLine();
        }

    }

}
