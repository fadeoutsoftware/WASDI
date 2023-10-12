package wasdi.shared.queryexecutors.skywatch.models;

import java.util.List;

public class DeletePipelineResponse {
	
	public DeletePipelineResponse() {
		
	}
	
	private List<Error> errors;

	public static class Error {
		public Error() {
			
		}
		private String message;
		public String getMessage() {
			return message;
		}
		public void setMessage(String message) {
			this.message = message;
		}
	}

	public List<Error> getErrors() {
		return errors;
	}

	public void setErrors(List<Error> errors) {
		this.errors = errors;
	}

}