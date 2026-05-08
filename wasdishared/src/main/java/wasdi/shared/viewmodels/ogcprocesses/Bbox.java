package wasdi.shared.viewmodels.ogcprocesses;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public class Bbox extends OgcProcessesViewModel {

	/**
	 * Gets or Sets crs
	 */
	public enum CrsEnum {
		_1_3_CRS84("http://www.opengis.net/def/crs/OGC/1.3/CRS84"),

		_0_CRS84H("http://www.opengis.net/def/crs/OGC/0/CRS84h");

		private String value;

		CrsEnum(String value) {
			this.value = value;
		}

		@Override
		@JsonValue
		public String toString() {
			return String.valueOf(value);
		}

		@JsonCreator
		public static CrsEnum fromValue(String text) {
			for (CrsEnum b : CrsEnum.values()) {
				if (String.valueOf(b.value).equals(text)) {
					return b;
				}
			}
			return null;
		}
	}	
	
	private List<BigDecimal> bbox = new ArrayList<BigDecimal>();

	private CrsEnum crs = CrsEnum._1_3_CRS84;

	public List<BigDecimal> getBbox() {
		return bbox;
	}

	public void setBbox(List<BigDecimal> bbox) {
		this.bbox = bbox;
	}

	public CrsEnum getCrs() {
		return crs;
	}

	public void setCrs(CrsEnum crs) {
		this.crs = crs;
	}
}
