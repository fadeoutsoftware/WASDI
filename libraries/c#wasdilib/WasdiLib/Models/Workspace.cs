using System.Runtime.CompilerServices;


[assembly: InternalsVisibleTo("WasdiLib.Client.Program")]

namespace WasdiLib.Models
{
    public class Workspace
    {

        public string WorkspaceId { get; set; }

        public string WorkspaceName { get; set; }

        public string OwnerUserId { get; set; }

        public List<string> SharedUsers { get; set; }

    }
}
