package wasdi.shared.business.comparators;

import java.util.Comparator;

import wasdi.shared.business.ProcessWorkspace;

/**
 * Compares two process workspace based on the Operation Start Date
 * @author p.campanella
 *
 */
public class ProcessWorkspaceStartDateComparator implements Comparator<ProcessWorkspace> {

	@Override
	public int compare(ProcessWorkspace oProcWs1, ProcessWorkspace oProcWs2) {
		return oProcWs1.getOperationStartTimestamp().compareTo(oProcWs2.getOperationStartTimestamp());
	}

}
