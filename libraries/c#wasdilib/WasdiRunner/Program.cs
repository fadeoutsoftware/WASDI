using System.Reflection;

using WasdiLib;

namespace WasdiRunner
{
    internal class Program
    {
        static void Main(string[] args)
        {
            Wasdi wasdi = new();
            wasdi.Init();

            string sForceStatus = "DONE";

            try
            {
                IWasdiRunnable? wasdiRunnable = LoadRunnable(wasdi);

                if (wasdiRunnable == null)
                {
                    wasdi.WasdiLog("No Wasdi Runnable found. Aborting.");
                    sForceStatus = "ERROR";
                    return;
                }

                try
                {
                    wasdi.WasdiLog("Running Wasdi Proc Id: " + wasdi.GetMyProcId());
                    wasdiRunnable.Run(wasdi);

                    wasdi.WasdiLog("Processor done, bye bye");
                }
                catch (Exception ex)
                {
                    wasdi.WasdiLog(ex.Message);

                    sForceStatus = "ERROR";
                }

            }
            catch (Exception ex)
            {
                sForceStatus = "ERROR";

                wasdi.WasdiLog(ex.Message);
            }
            finally
            {
                string sMyProcessId = wasdi.GetMyProcId();
                string sFinalStatus = wasdi.GetProcessStatus(sMyProcessId);

                if (sFinalStatus != "STOPPED" && sFinalStatus != "DONE" && sFinalStatus != "ERROR")
                {
                    wasdi.WasdiLog("Forcing status to " + sForceStatus);
                    wasdi.UpdateProcessStatus(sMyProcessId, sForceStatus, 100);
                }
            }


        }

        private static IWasdiRunnable? LoadRunnable(Wasdi wasdi)
        {
            string sExeFilePath = Assembly.GetExecutingAssembly().Location;
            string? sWorkPath = Path.GetDirectoryName(sExeFilePath);

            if (sWorkPath == null)
                return null;

            string[] list = Directory.GetFiles(sWorkPath, "*.dll");
            foreach(string? fileName in list)
            { 

                if (fileName == null)
                    continue;

                Assembly assembly = Assembly.LoadFrom(fileName);

                foreach (Type t in assembly.GetTypes())
                {
                    if (typeof(IWasdiRunnable).IsAssignableFrom(t))
                    {
                        wasdi.WasdiLog("Found class " + t.ToString() + " implementing IWasdiRunnable");

                        return Activator.CreateInstance(t) as IWasdiRunnable;
                    }
                }
            }

            return null;
        }

    }
}
