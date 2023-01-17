package wasdi.shared.viewmodels.ogcprocesses;

import java.util.HashMap;

import wasdi.shared.utils.Utils;

public class ApiException extends HashMap<String, Object>  {
	
	private String type = null;
	private String title = null;
	private Integer status = null;
	private String detail = null;
	private String instance = null;
	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public Integer getStatus() {
		return status;
	}
	public void setStatus(Integer status) {
		this.status = status;
	}
	public String getDetail() {
		return detail;
	}
	public void setDetail(String detail) {
		this.detail = detail;
	}
	public String getInstance() {
		return instance;
	}
	public void setInstance(String instance) {
		this.instance = instance;
	}
	
	/**
	 * Utility method for the generic exception View Model
	 * @return ApiException View Model
	 */
	public static ApiException getInternalServerError() {
		return getInternalServerError("");
	}
	
	/**
	 * Utility method for the generic exception View Model that will use a specific description of the detail
	 * @param sDescription Exception detail description
	 * @return ApiException View Model
	 */
	public static ApiException getInternalServerError(String sDescription) {
		ApiException oApiException = new ApiException();
		
		oApiException.setStatus(500);
		oApiException.setTitle("Internal Server Error");
		oApiException.setType("Exception");
		
		
		if (Utils.isNullOrEmpty(sDescription)) sDescription = "There was an unexpected error, sorry";
		oApiException.setDetail(sDescription);
		
		return oApiException;
	}

}
