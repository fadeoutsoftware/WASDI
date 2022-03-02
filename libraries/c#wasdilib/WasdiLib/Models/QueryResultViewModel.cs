using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace WasdiLib.Models
{
    public class QueryResultViewModel
    {

        public string Preview { get; set; }
        public string Title { get; set; }
        public string Summary { get; set; }
        public string Id { get; set; }
        public string Link { get; set; }
        public string Footprint { get; set; }
        public string Provider { get; set; }

        public Dictionary<string, string> properties { get; set; }
    }
}
