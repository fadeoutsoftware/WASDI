package wasdi.shared.viewmodels.ogcprocesses.schemas;

import java.util.ArrayList;

import wasdi.shared.viewmodels.ogcprocesses.Schema;

public class StringInListSchema extends Schema {
	public StringInListSchema() {
		type = "string";
	}
	
	protected ArrayList<String> _enum = new ArrayList<String>();

	public ArrayList<String> getEnum() {
		return _enum;
	}

	public void setEnum(ArrayList<String> _enum) {
		this._enum = _enum;
	}
}
