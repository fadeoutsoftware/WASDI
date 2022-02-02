namespace WasdiLib.Models
{
    public class ProcessWorkspace
    {
        public string FileSize { get; set; }
        public string LastChangeDate { get; set; }
        public string OperationDate { get; set; }
        public string OperationEndDate { get; set; }
        public string OperationStartDate { get; set; }
        public string OperationSubType { get; set; }
        public string OperationType { get; set; }
        public string Payload { get; set; }
        public int Pid { get; set; }
        public string ProcessObjId { get; set; }
        public string ProductName { get; set; }
        public int ProgressPerc { get; set; }
        public string Status { get; set; }
        public string UserId { get; set; }
    }
}
