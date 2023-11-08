package wasdi.shared.viewmodels.products;

/**
 * Represents the properties of a single file in a workspace
 * @author p.campanella
 *
 */
public class ProductPropertiesViewModel {
	
	private String fileName;
	private String friendlyName;
	private long lastUpdateTimestampMs;
	private long size;
	private String checksum;
	private String style;
	
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getFriendlyName() {
		return friendlyName;
	}
	public void setFriendlyName(String friendlyName) {
		this.friendlyName = friendlyName;
	}
	public long getLastUpdateTimestampMs() {
		return lastUpdateTimestampMs;
	}
	public void setLastUpdateTimestampMs(long lastUpdateTimestampMs) {
		this.lastUpdateTimestampMs = lastUpdateTimestampMs;
	}
	public String getChecksum() {
		return checksum;
	}
	public void setChecksum(String checksum) {
		this.checksum = checksum;
	}
	public String getStyle() {
		return style;
	}
	public void setStyle(String style) {
		this.style = style;
	}
	public long getSize() {
		return size;
	}
	public void setSize(long size) {
		this.size = size;
	}

}
