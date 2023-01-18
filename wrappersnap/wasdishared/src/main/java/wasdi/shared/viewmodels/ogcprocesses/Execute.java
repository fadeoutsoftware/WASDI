package wasdi.shared.viewmodels.ogcprocesses;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public class Execute {
	private Map<String, Object> inputs = new HashMap<String, Object>();
	private Map<String, Output> outputs = new HashMap<String, Output>();
	private ResponseEnum response = ResponseEnum.RAW;
	private Subscriber subscriber = null;
	
	/**
	 * Gets or Sets response
	 */
	public enum ResponseEnum {
		RAW("raw"),

		DOCUMENT("document");

		private String value;

		ResponseEnum(String value) {
			this.value = value;
		}

		@Override
		@JsonValue
		public String toString() {
			return String.valueOf(value);
		}

		@JsonCreator
		public static ResponseEnum fromValue(String text) {
			for (ResponseEnum b : ResponseEnum.values()) {
				if (String.valueOf(b.value).equals(text)) {
					return b;
				}
			}
			return null;
		}
	}

	public Map<String, Object> getInputs() {
		return inputs;
	}

	public void setInputs(Map<String, Object> inputs) {
		this.inputs = inputs;
	}

	public Map<String, Output> getOutputs() {
		return outputs;
	}

	public void setOutputs(Map<String, Output> outputs) {
		this.outputs = outputs;
	}

	public ResponseEnum getResponse() {
		return response;
	}

	public void setResponse(ResponseEnum response) {
		this.response = response;
	}

	public Subscriber getSubscriber() {
		return subscriber;
	}

	public void setSubscriber(Subscriber subscriber) {
		this.subscriber = subscriber;
	}

}
