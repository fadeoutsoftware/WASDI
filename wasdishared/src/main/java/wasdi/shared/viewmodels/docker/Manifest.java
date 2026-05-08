package wasdi.shared.viewmodels.docker;

import java.util.ArrayList;

public class Manifest {
	public int schemaVersion;
	public String name;
	public String tag;
	public String architecture;
	public ArrayList<BlobSum> fsLayers;
	public ArrayList<History> history;
}
