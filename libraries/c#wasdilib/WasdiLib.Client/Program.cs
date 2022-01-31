using System;

namespace WasdiLib.Client
{
    internal class Program
    {
        static void Main(string[] args)
        {
            Console.WriteLine("Program.Main()");
            WasdiLib wasdi = new WasdiLib();
            Console.WriteLine(wasdi.HelloWasdi());
        }
    }

}
