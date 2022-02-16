namespace WasdiLib.Client
{
    internal class Program 
    {
        static void Main(string[] args)
        {
            Wasdi wasdi = new();
            wasdi.Init();

            IWasdiRunnable wasdiRunnable = new MyFirstWasdiApp();

            try
            {
                string customStatus = wasdiRunnable.Run(wasdi);

                UpdateStatusCustom(wasdi, customStatus);
            }
            catch (Exception ex)
            {
                wasdi.WasdiLog(ex.Message);

                UpdateStatusError(wasdi);
            }

            wasdi.WasdiLog("FINISHED");
        }

        private static void UpdateStatusSuccess(Wasdi wasdi)
        {
            wasdi.WasdiLog("UpdateStatus:");
            string sStatus = "DONE";
            int iPerc = 100;
            wasdi.UpdateStatus(sStatus, iPerc);
        }

        private static void UpdateStatusError(Wasdi wasdi)
        {
            wasdi.WasdiLog("UpdateStatus:");
            string sStatus = "ERROR";
            int iPerc = 0;
            wasdi.UpdateStatus(sStatus, iPerc);
        }

        private static void UpdateStatusCustom(Wasdi wasdi, string customStatus)
        {
            wasdi.WasdiLog("UpdateStatus:");

            int iPerc = 100;
            wasdi.UpdateStatus(customStatus, iPerc);
        }

    }

}
