package wasdi.shared.viewmodels.ogcprocesses.schemas;

import wasdi.shared.viewmodels.ogcprocesses.Schema;

public class ImageSchema extends Schema {
	
	public ImageSchema() {
		type = "string";
	}
	
	public String contentEncoding = "binary";
	
	public String contentMediaType;
	
	public static ImageSchema getTiff() {
		ImageSchema oImageSchema = new ImageSchema();
		oImageSchema.contentMediaType = "application/tiff; application=geotiff";
		return oImageSchema;
	}
	
	public static ImageSchema getJpg() {
		ImageSchema oImageSchema = new ImageSchema();
		oImageSchema.contentMediaType = "application/jp2";
		return oImageSchema;
	}
}
