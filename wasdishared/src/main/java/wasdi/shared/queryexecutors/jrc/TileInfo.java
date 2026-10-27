package wasdi.shared.queryexecutors.jrc;

class TileInfo {

	public String id;
    public String name;
    public String footprint;

    public TileInfo(String sId, String sName, String sFootprint) {
        this.id = sId;
        this.name = sName;
        this.footprint = sFootprint;
    }

    @Override
    public String toString() {
        return "TileInfo{" +
                "sId='" + id + '\'' +
                ", sName='" + name + '\'' +
                ", sFootprint='" + footprint + '\'' +
                '}';
    }

}

