using WasdiLib;
using WasdiLib.Models;


namespace WasdiLib.Client
{
    public class MyFirstWasdiApp : IWasdiRunnable
    {

        public string Run(Wasdi wasdi)
        {

            RunExecuteWorkflow(wasdi);

            RunExecuteProcessor(wasdi);

            return "DONE";
        }



        private void RunExecuteWorkflow(Wasdi wasdi)
        {
            string sStartDate = wasdi.GetParam("DATEFROM");
            string sEndDate = wasdi.GetParam("DATETO");
            string sBbox = wasdi.GetParam("BBOX");
            string sWorkflow = wasdi.GetParam("WORKFLOW");

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
            List<QueryResult> aoResults = wasdi.SearchEOImages("S1", sStartDate, sEndDate, dLatN, dLonW, dLatS, dLonE, "GRD", null, null, null);
            wasdi.WasdiLog("Found " + aoResults.Count + " Images");

            if (aoResults.Count > 0)
            {
                wasdi.ImportProduct(aoResults[0]);

                List<string> asInputs = new List<string>();
                asInputs.Add(aoResults[0].Title + ".zip");

                List<string> asOutputs = new List<string>();
                asOutputs.Add("preprocessed.tif");

                wasdi.ExecuteWorkflow(asInputs, asOutputs, sWorkflow);
            }
            wasdi.WasdiLog("FINISHED");
        }

        private void RunExecuteProcessor(Wasdi wasdi)
        {

            // call another app: HelloWasdiWorld
            Dictionary<string, object> dictionary = new Dictionary<string, object>()
                        { { "name", "Wasdi User" } };
            wasdi.ExecuteProcessor("HelloWasdiWorld", dictionary);
        }

    }

}
