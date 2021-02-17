package wasdi.shared.utils;

/**
 * Created by Marco Menapace on 2020-11-26
 * 
 * Fadeout software
 *
 */


import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import wasdi.shared.launcherOperations.LauncherOperationsUtils;


public class TestLauncherOperationUtils {
	
	@Test
	void testValid() {
		assertTrue(LauncherOperationsUtils.isValidLauncherOperation("INGEST"));
	}
	@Test
	void testError() {
		assertFalse(LauncherOperationsUtils.isValidLauncherOperation("vndoicdsniovn"));
	}

}

