using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace WasdiLib.Models
{
    internal class RunningProcessorViewModel
    {
        public string ProcessorId { get; set; }
        public string Name { get; set; }
        public string ProcessingIdentifier { get; set; }
        public string Status { get; set; }
        public string JsonEncodedResult { get; set; }
    }
}
