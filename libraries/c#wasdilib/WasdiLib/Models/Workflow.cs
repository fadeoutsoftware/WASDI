namespace WasdiLib.Models
{
    public class Workflow
    {

        public string Description { get; set; }
        public List<string> InputFileNames { get; set; }
        public List<string> InputNodeNames { get; set; }
        public string Name { get; set; }
        public string NodeUrl { get; set; }
        public List<string> OutputFileNames { get; set; }
        public List<string> OutputNodeNames { get; set; }
        public bool Public { get; set; }
        public bool SharedWithMe { get; set; }
        public string UserId { get; set; }
        public string WorkflowId { get; set; }

    }
}
