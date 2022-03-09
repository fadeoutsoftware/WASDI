namespace WasdiLib
{
    /// <summary>
    /// Wasdi interface to be implemented by a class that is meant to run on the Wasdi platform.
    /// </summary>
    public interface IWasdiRunnable
    {

        /// <summary>
        /// This is the entry point of an application deployed on the Wasdi platform.
        /// The Wasdi platform will invoke the implementation of this Run method.
        /// </summary>
        /// <param name="wasdi">the wasdi object injected by the platform</param>
        /// <returns>the status of the execution (i.e. CREATED,  RUNNING,  STOPPED,  DONE,  ERROR, WAITING, READY)</returns>
        void Run(Wasdi wasdi);

    }
}
