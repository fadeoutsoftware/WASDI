using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;

using Newtonsoft.Json;

using WasdiLib.Helpers;
using WasdiLib.Models;

namespace WasdiLib.Client
{
    internal class Program
    {

        private static string _workspaceName = "ERA5_WORKSPACE_TEST";

        static void Main(string[] args)
        {
            Wasdi wasdi = new Wasdi();
            wasdi.Init();

            wasdi.PrintStatus();

            wasdi.SetVerbose(true);

            wasdi.PrintStatus();

            GetProcessorPath(wasdi);


            WasdiLog(wasdi);
            Hello(wasdi);
            GetWorkspacesNames(wasdi);


            /*
            GetWorkspaces(wasdi);
            GetWorkspaceIdByName(wasdi);
            CreateWorkspace_DeleteWorkspace(wasdi);
            GetProcessWorkspacesByWorkspaceId(wasdi);
            GetProductsByWorkspaceId(wasdi);
            GetWorkflows(wasdi);
            */


            /*
            String sStartDate = wasdi.GetParam("DATEFROM");
            String sEndDate= wasdi.GetParam("DATETO");
            String sBbox = wasdi.GetParam("BBOX");
            String sWorkflow = wasdi.GetParam("WORKFLOW");

            double dLatN = 44.0;
            double dLonW = 35.0;
            double dLatS = 45.0;
            double dLonE = 36.0;


            if (sBbox != null)
            {
                String[] asLatLons = sBbox.Split(',');

                dLatN = Double.Parse(asLatLons[0]);
                dLonW = Double.Parse(asLatLons[1]);
                dLatS = Double.Parse(asLatLons[2]);
                dLonE = Double.Parse(asLatLons[3]);
            }

            wasdi.WasdiLog("Start searching images");

            List<QueryResult> aoResults = wasdi.SearchEOImages("S1", sStartDate, sEndDate,dLatN,dLonW,dLatS,dLonE,"GRD",null,null,null);

            wasdi.WasdiLog("Found " + aoResults.Count + " Images");

            if (aoResults.Count > 0)
            {
                wasdi.ImportProduct(aoResults[0]);

                List<string> asInputs = new List<string>();
                List<string> asOutputs = new List<string>();

                asInputs.Add(aoResults[0].Title + ".zip");
                asOutputs.Add("preprocessed.tif");

                wasdi.ExecuteWorkflow(asInputs, asOutputs, sWorkflow);
            }


            // call another app: HelloWasdiWorld
            Dictionary dictionary  = new Dictionary("name");
            // wasdi.ExecuteProcessor("HelloWasdiWorld", dictionary);



            wasdi.WasdiLog("FINISHED");
            UpdateStatus(wasdi);
            */

        }

        private static void Hello(Wasdi wasdi)
        {
            wasdi.WasdiLog("Hello:");
            wasdi.WasdiLog(wasdi.Hello());
        }

        private static void GetProcessorPath(Wasdi wasdi)
        {
            wasdi.WasdiLog("GetProcessorPath:");
            wasdi.WasdiLog(wasdi.GetProcessorPath());
        }

        private static void WasdiLog(Wasdi wasdi)
        {
            wasdi.WasdiLog("WasdiLog: Prova");
        }

        private static void UpdateStatus(Wasdi wasdi)
        {
            wasdi.WasdiLog("UpdateStatus:");

            string sStatus = "DONE";
            int iPerc = 100;

            wasdi.UpdateStatus(sStatus, iPerc);
        }

        private static void GetWorkspaces(Wasdi wasdi)
        {
            wasdi.WasdiLog("GetWorkspaces:");

            List<Workspace> workspaces = wasdi.GetWorkspaces();

            foreach (Workspace workspace in workspaces)
            {
                Console.WriteLine(JsonConvert.SerializeObject(workspace));
            }
        }

        private static void GetWorkspacesNames(Wasdi wasdi)
        {
            wasdi.WasdiLog("GetWorkspacesNames:");

            List<string> workspacesNames = wasdi.GetWorkspacesNames();

            foreach (string workspaceName in workspacesNames)
            {
                Console.WriteLine(workspaceName);
            }
        }

        private static void CreateWorkspace_DeleteWorkspace(Wasdi wasdi)
        {
            wasdi.WasdiLog("CreateWorkspace_DeleteWorkspace:");

            string workspaceName = wasdi.GetParam("WSNAME");

            string workspaceId = wasdi.GetWorkspaceIdByName(workspaceName);

            if (workspaceId == null)
                workspaceId = wasdi.CreateWorkspace(workspaceName);

            wasdi.WasdiLog("Workspace: " + "Name: " + workspaceName + "; " + "Id: " + workspaceId);


            string outcome = wasdi.DeleteWorkspace(workspaceId);
            wasdi.WasdiLog("Deleted Workspace: " + outcome);
        }

        private static void GetProcessWorkspacesByWorkspaceId(Wasdi wasdi)
        {
            wasdi.WasdiLog("GetProcessWorkspacesByWorkspaceId:");


            //string workspaceId = wasdi.GetWorkspaceIdByName(_workspaceName);
            //wasdi.OpenWorkspace(_workspaceName);

            List<ProcessWorkspace> processWorkspaces = wasdi.GetProcessWorkspacesByWorkspaceId(wasdi.GetActiveWorkspace());

            foreach (ProcessWorkspace processWorkspace in processWorkspaces)
            {
                wasdi.WasdiLog(SerializationHelper.ToJson(processWorkspace));
            }
        }

        private static void GetWorkspaceIdByName(Wasdi wasdi)
        {
            wasdi.WasdiLog("GetWorkspaceIdByName:");


            string workspaceId = wasdi.GetWorkspaceIdByName(_workspaceName);

            string workspaceUrl = wasdi.GetWorkspaceUrlByWsId(workspaceId);

            wasdi.WasdiLog("workspaceUrl: " + workspaceUrl);
            Console.WriteLine("workspaceId: " + workspaceId);

        }

        private static void GetProductsByWorkspaceId(Wasdi wasdi)
        {
            wasdi.WasdiLog("GetProductsByWorkspaceId:");


            string workspaceId = wasdi.GetWorkspaceIdByName(_workspaceName);

            List<string> productList = wasdi.GetProductsByWorkspaceId(workspaceId);

            foreach (string product in productList)
            {
                Console.WriteLine(product);
            }
        }

        private static void GetWorkflows(Wasdi wasdi)
        {
            wasdi.WasdiLog("GetWorkflows:");

            List<Workflow> workflows = wasdi.GetWorkflows();

            foreach (Workflow workflow in workflows)
            {
                wasdi.WasdiLog(JsonConvert.SerializeObject(workflow));
            }
        }

    }

}
