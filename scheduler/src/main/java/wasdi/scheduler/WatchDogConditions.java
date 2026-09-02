package wasdi.scheduler;

import java.util.List;

import wasdi.shared.business.ProcessWorkspace;

/**
 * Pure watchdog predicates extracted for deterministic testing and reuse.
 */
public class WatchDogConditions {

	private WatchDogConditions() {
	}

	public static boolean isBlockingCondition(List<ProcessWorkspace> aoRunningList, List<ProcessWorkspace> aoReadyList, List<ProcessWorkspace> aoWaitingList, List<ProcessWorkspace> aoCreatedList) {
		return aoRunningList.size()==0 && aoReadyList.size()==0 && aoWaitingList.size()>0 && aoCreatedList.size()>0;
	}

	public static boolean isWaitingOnlyCondition(List<ProcessWorkspace> aoRunningList, List<ProcessWorkspace> aoReadyList, List<ProcessWorkspace> aoWaitingList, List<ProcessWorkspace> aoCreatedList) {
		return aoRunningList.size()==0 && aoReadyList.size()==0 && aoWaitingList.size()>0 && aoCreatedList.size()==0;
	}
}
