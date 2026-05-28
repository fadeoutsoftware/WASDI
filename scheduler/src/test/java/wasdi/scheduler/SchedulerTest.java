package wasdi.scheduler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import wasdi.shared.business.ProcessWorkspace;

public class SchedulerTest {
	private static int s_iPassed = 0;
	private static int s_iFailed = 0;

	public static void main(String[] asArgs) {
		runCase("FIFO when fair round robin disabled", SchedulerTest::testFifoSelectionWhenFairRoundRobinDisabled);
		runCase("User round robin burst path", SchedulerTest::testUserRoundRobinBurstPath);
		runCase("Late joiner gets turn after current burst", SchedulerTest::testLateJoinerGetsTurnAfterCurrentBurst);
		runCase("ParentId round robin path inside single user", SchedulerTest::testParentIdRoundRobinPathInsideSingleUser);
		runCase("Remove created fallback removes head when target missing", SchedulerTest::testRemoveCreatedFallbackRemovesHeadWhenTargetNotFound);
		runCase("Null userId maps to UNKNOWN and remains schedulable", SchedulerTest::testNullUserIdPath);
		runCase("Special waiting no-unblock increments streak", SchedulerTest::testSpecialWaitingNoUnblockIncrementsStreak);
		runCase("Special waiting reaches fallback after N cycles", SchedulerTest::testSpecialWaitingNoUnblockFallbackAfterThreshold);
		runCase("Special waiting immediate fallback when running is empty", SchedulerTest::testSpecialWaitingImmediateFallbackWhenNoRunning);
		runCase("Watchdog blocking condition predicate", SchedulerTest::testWatchDogBlockingConditionPredicate);
		runCase("Watchdog waiting-only condition predicate", SchedulerTest::testWatchDogWaitingOnlyConditionPredicate);

		System.out.println("\nScheduler logical tests summary: passed=" + s_iPassed + " failed=" + s_iFailed);

		if (s_iFailed > 0) {
			throw new IllegalStateException("Scheduler logical tests failed: " + s_iFailed);
		}
	}

	private static void runCase(String sName, Runnable oCase) {
		try {
			oCase.run();
			s_iPassed++;
			System.out.println("[PASS] " + sName);
		}
		catch (Throwable oEx) {
			s_iFailed++;
			System.err.println("[FAIL] " + sName + " -> " + oEx.getMessage());
		}
	}

	private static void testFifoSelectionWhenFairRoundRobinDisabled() {
		ProcessScheduler oScheduler = createScheduler(false, 1, false, 1);

		List<ProcessWorkspace> aoCreatedList = new ArrayList<ProcessWorkspace>();
		aoCreatedList.add(createProcessWorkspace("A", "A1", 1, ""));
		aoCreatedList.add(createProcessWorkspace("B", "B1", 2, ""));

		assertEquals("FIFO dispatch order", "A1,B1", dispatchMany(oScheduler, aoCreatedList, 2));
	}

	private static void testUserRoundRobinBurstPath() {
		ProcessScheduler oScheduler = createScheduler(true, 2, false, 1);

		List<ProcessWorkspace> aoCreatedList = new ArrayList<ProcessWorkspace>();
		aoCreatedList.add(createProcessWorkspace("A", "A1", 1, ""));
		aoCreatedList.add(createProcessWorkspace("A", "A2", 2, ""));
		aoCreatedList.add(createProcessWorkspace("A", "A3", 3, ""));
		aoCreatedList.add(createProcessWorkspace("B", "B1", 4, ""));
		aoCreatedList.add(createProcessWorkspace("B", "B2", 5, ""));

		assertEquals("User burst dispatch order", "A1,A2,B1,B2,A3", dispatchMany(oScheduler, aoCreatedList, 5));
	}

	private static void testLateJoinerGetsTurnAfterCurrentBurst() {
		ProcessScheduler oScheduler = createScheduler(true, 3, false, 1);

		List<ProcessWorkspace> aoCreatedList = new ArrayList<ProcessWorkspace>();
		aoCreatedList.add(createProcessWorkspace("A", "A1", 1, ""));
		aoCreatedList.add(createProcessWorkspace("A", "A2", 2, ""));
		aoCreatedList.add(createProcessWorkspace("A", "A3", 3, ""));
		aoCreatedList.add(createProcessWorkspace("A", "A4", 4, ""));

		List<String> asDispatchOrder = new ArrayList<String>(Arrays.asList(
			dispatchNext(oScheduler, aoCreatedList),
			dispatchNext(oScheduler, aoCreatedList)
		));

		aoCreatedList.add(createProcessWorkspace("B", "B1", 5, ""));
		aoCreatedList.add(createProcessWorkspace("B", "B2", 6, ""));

		asDispatchOrder.add(dispatchNext(oScheduler, aoCreatedList));
		asDispatchOrder.add(dispatchNext(oScheduler, aoCreatedList));
		asDispatchOrder.add(dispatchNext(oScheduler, aoCreatedList));

		assertEquals("Late joiner dispatch order", "A1,A2,A3,B1,B2", joinValues(asDispatchOrder));
	}

	private static void testParentIdRoundRobinPathInsideSingleUser() {
		ProcessScheduler oScheduler = createScheduler(true, 10, true, 1);

		List<ProcessWorkspace> aoCreatedList = new ArrayList<ProcessWorkspace>();
		aoCreatedList.add(createProcessWorkspace("A", "AX1", 1, "X"));
		aoCreatedList.add(createProcessWorkspace("A", "AX2", 2, "X"));
		aoCreatedList.add(createProcessWorkspace("A", "AY1", 3, "Y"));
		aoCreatedList.add(createProcessWorkspace("A", "AY2", 4, "Y"));

		assertEquals("ParentId dispatch order", "AX1,AY1,AX2,AY2", dispatchMany(oScheduler, aoCreatedList, 4));
	}

	private static void testRemoveCreatedFallbackRemovesHeadWhenTargetNotFound() {
		ProcessScheduler oScheduler = createScheduler(false, 1, false, 1);

		List<ProcessWorkspace> aoCreatedList = new ArrayList<ProcessWorkspace>();
		aoCreatedList.add(createProcessWorkspace("A", "A1", 1, ""));
		aoCreatedList.add(createProcessWorkspace("A", "A2", 2, ""));

		ProcessWorkspace oUnknown = createProcessWorkspace("X", "DOES_NOT_EXIST", 3, "");
		oScheduler.removeCreatedProcessFromList(aoCreatedList, oUnknown);

		assertEquals("Fallback removed wrong item", "A2", aoCreatedList.get(0).getProcessObjId());
	}

	private static void testNullUserIdPath() {
		ProcessScheduler oScheduler = createScheduler(true, 1, false, 1);

		List<ProcessWorkspace> aoCreatedList = new ArrayList<ProcessWorkspace>();
		aoCreatedList.add(createProcessWorkspace(null, "N1", 1, ""));
		aoCreatedList.add(createProcessWorkspace("B", "B1", 2, ""));

		assertEquals("Null user dispatch order", "N1,B1", dispatchMany(oScheduler, aoCreatedList, 2));
	}

	private static void testSpecialWaitingNoUnblockIncrementsStreak() {
		TestProcessScheduler oScheduler = createSpecialWaitingScheduler();
		oScheduler.m_iEmergencyFallbackEveryNCycles = 3;
		oScheduler.m_iEmergencyNoUnblockStreak = 0;

		ProcessWorkspace oCreated = createProcessWorkspace("A", "C1", 1, "");
		oScheduler.m_aoLaunchedProcesses.put(oCreated.getProcessObjId(), new Date());

		List<ProcessWorkspace> aoRunningList = new ArrayList<ProcessWorkspace>();
		aoRunningList.add(createProcessWorkspace("A", "R1", 1, ""));

		List<ProcessWorkspace> aoReadyList = new ArrayList<ProcessWorkspace>();
		List<ProcessWorkspace> aoCreatedList = new ArrayList<ProcessWorkspace>(Arrays.asList(oCreated));
		List<ProcessWorkspace> aoWaitingList = new ArrayList<ProcessWorkspace>(Arrays.asList(createProcessWorkspace("A", "W1", 1, "")));

		oScheduler.cycle(aoRunningList, aoReadyList, aoCreatedList, aoWaitingList);

		assertEqualsInt("Emergency no-unblock streak should increment", 1, oScheduler.m_iEmergencyNoUnblockStreak);
	}

	private static void testSpecialWaitingNoUnblockFallbackAfterThreshold() {
		TestProcessScheduler oScheduler = createSpecialWaitingScheduler();
		oScheduler.m_iEmergencyFallbackEveryNCycles = 3;
		oScheduler.m_iEmergencyNoUnblockStreak = 2;

		ProcessWorkspace oCreated = createProcessWorkspace("A", "C1", 1, "");
		oScheduler.m_aoLaunchedProcesses.put(oCreated.getProcessObjId(), new Date());

		List<ProcessWorkspace> aoRunningList = new ArrayList<ProcessWorkspace>();
		aoRunningList.add(createProcessWorkspace("A", "R1", 1, ""));

		List<ProcessWorkspace> aoReadyList = new ArrayList<ProcessWorkspace>();
		List<ProcessWorkspace> aoCreatedList = new ArrayList<ProcessWorkspace>(Arrays.asList(oCreated));
		List<ProcessWorkspace> aoWaitingList = new ArrayList<ProcessWorkspace>(Arrays.asList(createProcessWorkspace("A", "W1", 1, "")));

		oScheduler.cycle(aoRunningList, aoReadyList, aoCreatedList, aoWaitingList);

		assertEqualsInt("Emergency no-unblock streak should reset after fallback", 0, oScheduler.m_iEmergencyNoUnblockStreak);
	}

	private static void testSpecialWaitingImmediateFallbackWhenNoRunning() {
		TestProcessScheduler oScheduler = createSpecialWaitingScheduler();
		oScheduler.m_iEmergencyFallbackEveryNCycles = 3;
		oScheduler.m_iEmergencyNoUnblockStreak = 10;

		ProcessWorkspace oCreated = createProcessWorkspace("A", "C1", 1, "");
		oScheduler.m_aoLaunchedProcesses.put(oCreated.getProcessObjId(), new Date());

		List<ProcessWorkspace> aoRunningList = new ArrayList<ProcessWorkspace>();
		List<ProcessWorkspace> aoReadyList = new ArrayList<ProcessWorkspace>();
		List<ProcessWorkspace> aoCreatedList = new ArrayList<ProcessWorkspace>(Arrays.asList(oCreated));
		List<ProcessWorkspace> aoWaitingList = new ArrayList<ProcessWorkspace>(Arrays.asList(createProcessWorkspace("A", "W1", 1, "")));

		oScheduler.cycle(aoRunningList, aoReadyList, aoCreatedList, aoWaitingList);

		assertEqualsInt("Emergency no-unblock streak should reset when running queue is empty", 0, oScheduler.m_iEmergencyNoUnblockStreak);
	}

	private static void testWatchDogBlockingConditionPredicate() {
		List<ProcessWorkspace> aoRunningList = new ArrayList<ProcessWorkspace>();
		List<ProcessWorkspace> aoReadyList = new ArrayList<ProcessWorkspace>();
		List<ProcessWorkspace> aoWaitingList = new ArrayList<ProcessWorkspace>(Arrays.asList(createProcessWorkspace("A", "W1", 1, "")));
		List<ProcessWorkspace> aoCreatedList = new ArrayList<ProcessWorkspace>(Arrays.asList(createProcessWorkspace("A", "C1", 1, "")));

		assertTrue("Watchdog blocking condition should be true for waiting+created and no running/ready", WatchDogConditions.isBlockingCondition(aoRunningList, aoReadyList, aoWaitingList, aoCreatedList));
		assertTrue("Watchdog blocking condition should be false when created is empty", !WatchDogConditions.isBlockingCondition(aoRunningList, aoReadyList, aoWaitingList, new ArrayList<ProcessWorkspace>()));
	}

	private static void testWatchDogWaitingOnlyConditionPredicate() {
		List<ProcessWorkspace> aoRunningList = new ArrayList<ProcessWorkspace>();
		List<ProcessWorkspace> aoReadyList = new ArrayList<ProcessWorkspace>();
		List<ProcessWorkspace> aoWaitingList = new ArrayList<ProcessWorkspace>(Arrays.asList(createProcessWorkspace("A", "W1", 1, "")));
		List<ProcessWorkspace> aoCreatedList = new ArrayList<ProcessWorkspace>();

		assertTrue("Watchdog waiting-only condition should be true for waiting only", WatchDogConditions.isWaitingOnlyCondition(aoRunningList, aoReadyList, aoWaitingList, aoCreatedList));
		assertTrue("Watchdog waiting-only condition should be false when created has entries", !WatchDogConditions.isWaitingOnlyCondition(aoRunningList, aoReadyList, aoWaitingList, Arrays.asList(createProcessWorkspace("A", "C1", 1, ""))));
	}

	private static ProcessScheduler createScheduler(boolean bFairRoundRobin, int iBurstUser, boolean bFairRoundRobinParentId, int iBurstParent) {
		ProcessScheduler oScheduler = new ProcessScheduler();
		oScheduler.m_bFairRoundRobin = bFairRoundRobin;
		oScheduler.m_iFairRoundRobinMaxProcessesCount = iBurstUser;
		oScheduler.m_bFairRoundRobinParentId = bFairRoundRobinParentId;
		oScheduler.m_iFairRoundRobinParentIdMaxProcessesCount = iBurstParent;
		resetFairState(oScheduler);
		return oScheduler;
	}

	private static void resetFairState(ProcessScheduler oScheduler) {
		oScheduler.m_asFairRoundRobinUsers.clear();
		oScheduler.m_iFairRoundRobinCurrentUserIndex = 0;
		oScheduler.m_iFairRoundRobinCurrentUserBurstCount = 0;
		oScheduler.m_aaoFairRoundRobinParentGroupsByUser.clear();
		oScheduler.m_aoFairRoundRobinCurrentParentIndexByUser.clear();
		oScheduler.m_aoFairRoundRobinCurrentParentBurstCountByUser.clear();
	}

	private static String dispatchNext(ProcessScheduler oScheduler, List<ProcessWorkspace> aoCreatedList) {
		ProcessWorkspace oSelected = oScheduler.getNextCreatedProcessToDispatch(aoCreatedList);

		if (oSelected == null) {
			throw new IllegalStateException("Scheduler returned null with a non-empty created list");
		}

		oScheduler.removeCreatedProcessFromList(aoCreatedList, oSelected);
		return oSelected.getProcessObjId();
	}

	private static String dispatchMany(ProcessScheduler oScheduler, List<ProcessWorkspace> aoCreatedList, int iCount) {
		List<String> asDispatchOrder = new ArrayList<String>();

		for (int i = 0; i < iCount; i++) {
			asDispatchOrder.add(dispatchNext(oScheduler, aoCreatedList));
		}

		return joinValues(asDispatchOrder);
	}

	private static ProcessWorkspace createProcessWorkspace(String sUserId, String sProcessId, double dOperationTimestamp, String sParentId) {
		ProcessWorkspace oProcessWorkspace = new ProcessWorkspace();
		oProcessWorkspace.setUserId(sUserId);
		oProcessWorkspace.setProcessObjId(sProcessId);
		oProcessWorkspace.setOperationType("DOWNLOAD");
		oProcessWorkspace.setOperationTimestamp(dOperationTimestamp);
		oProcessWorkspace.setParentId(sParentId);
		return oProcessWorkspace;
	}

	private static void assertEquals(String sMessage, String sExpected, String sActual) {
		if (!sExpected.equals(sActual)) {
			throw new IllegalStateException(sMessage + ": expected " + sExpected + " but got " + sActual);
		}
	}

	private static void assertEqualsInt(String sMessage, int iExpected, int iActual) {
		if (iExpected != iActual) {
			throw new IllegalStateException(sMessage + ": expected " + iExpected + " but got " + iActual);
		}
	}

	private static void assertTrue(String sMessage, boolean bCondition) {
		if (!bCondition) {
			throw new IllegalStateException(sMessage);
		}
	}

	private static String joinValues(List<String> asItems) {
		return String.join(",", asItems);
	}

	private static TestProcessScheduler createSpecialWaitingScheduler() {
		TestProcessScheduler oScheduler = new TestProcessScheduler();
		oScheduler.m_bFairRoundRobin = false;
		oScheduler.m_bSpecialWaitCondition = true;
		oScheduler.m_iMaxWaitingQueue = 0;
		oScheduler.m_iNumberOfConcurrentProcess = 2;
		oScheduler.m_asOperationTypes.add("DOWNLOAD");
		return oScheduler;
	}

	private static class TestProcessScheduler extends ProcessScheduler {
		@Override
		protected List<ProcessWorkspace> getRunningList(List<ProcessWorkspace> aoRunning) {
			return aoRunning;
		}

		@Override
		protected List<ProcessWorkspace> getReadyList(List<ProcessWorkspace> aoReady) {
			return aoReady;
		}

		@Override
		protected List<ProcessWorkspace> getCreatedList(List<ProcessWorkspace> aoCreated) {
			return aoCreated;
		}

		@Override
		protected List<ProcessWorkspace> getWaitingList(List<ProcessWorkspace> aoWaiting) {
			return aoWaiting;
		}

		@Override
		protected void waitForProcessToStart() {
			// No wait in logical tests.
		}
	}
}