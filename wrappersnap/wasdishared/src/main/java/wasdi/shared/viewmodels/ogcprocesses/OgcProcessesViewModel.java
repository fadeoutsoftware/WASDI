package wasdi.shared.viewmodels.ogcprocesses;

import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.log.WasdiLog;

public class OgcProcessesViewModel {

	@Override
	public String toString() {
		try {
			String sJsonView = JsonUtils.stringify(this);
			return sJsonView;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("OgcProcessesViewModel.toString: exception " + oEx.toString());
		}
		return super.toString();
	}
}
