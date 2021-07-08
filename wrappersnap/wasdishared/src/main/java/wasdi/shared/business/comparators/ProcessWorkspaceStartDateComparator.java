package wasdi.shared.business.comparators;

import java.util.Comparator;

import wasdi.shared.business.ProcessWorkspace;

public class ProcessWorkspaceStartDateComparator implements Comparator<ProcessWorkspace> {

	@Override
	public int compare(ProcessWorkspace oProcWs1, ProcessWorkspace oProcWs2) {
		return oProcWs1.getOperationStartDate().compareTo(oProcWs2.getOperationStartDate());
	}

}
