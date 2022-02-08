namespace WasdiLib.Models
{
    internal class WorkspaceEditorViewModel
    {

        public string WorkspaceId { get; set; }
        public string Name { get; set; }
        public string UserId { get; set; }
        public string ApiUrl { get; set; }
        public Int64 CreationDate { get; set; }
        public Int64 LastEditDate { get; set; }
        public List<string> SharedUsers { get; set; }
        public string NodeCode { get; set; }
        public long ProcessCount { get; set; }
        public string CloudProvider { get; set; }
        public string SlaLink { get; set; }

    }
}
