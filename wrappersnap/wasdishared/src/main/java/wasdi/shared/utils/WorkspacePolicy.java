package wasdi.shared.utils;

import org.apache.commons.math3.exception.NullArgumentException;

public class WorkspacePolicy {
	
	private static final int MINPRODUCTNAMELEN = 8;
	private static final int MINWORKSPACEIDLEN = 8;

	public Boolean validProductName(String sName) {
		if(null == sName) {
			return false;
		}
		if( sName.length() < MINPRODUCTNAMELEN) {
			return false;
		} else {
			return true;
		}
	}

	public boolean validWorkspaceId(String sWorkspaceId) {
		if(null == sWorkspaceId) {
			throw new NullArgumentException();
		}
		if(sWorkspaceId.length() < MINWORKSPACEIDLEN) {
			return false;
		} else {
			return true;
		}
	}
}
