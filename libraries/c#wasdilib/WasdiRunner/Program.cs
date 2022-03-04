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
                wasdi.WasdiLog("WasdiRunner v1.01");
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

                if (fileName == "WasdiLib.dll")
                {
                    wasdi.WasdiLog("Jumping WASDI.lib assembly");
                    continue;
                }

                Assembly assembly = Assembly.LoadFrom(fileName);

                foreach (Type type in assembly.GetTypes())
                {
                    if (typeof(IWasdiRunnable).IsAssignableFrom(type) && type.IsClass && !type.IsAbstract)
                    {
                        wasdi.WasdiLog("Found class " + type.ToString() + " implementing IWasdiRunnable");

                        return Activator.CreateInstance(type) as IWasdiRunnable;
                    }
                }
            }

            return null;
        }

    }
}
