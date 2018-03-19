import wasdi.shared.business.SnapWorkflow;
import wasdi.shared.data.SnapWorkflowRepository;

public class TestDataLayer {

	public static void main(String[] args) {

		String sWorkflowId = "a70c9a84-cf99-41d9-a481-0ff17651148d";
		SnapWorkflowRepository oSnapWorkflowRepository = new SnapWorkflowRepository();
		SnapWorkflow oWF = oSnapWorkflowRepository.GetSnapWorkflow(sWorkflowId);
		
		if (oWF == null) {
			System.out.println("null");
		}
		else {
			System.out.println("valid");
		}

	}

}
