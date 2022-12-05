package wasdi.shared.queryexecutors.skywatch.models;

import java.util.List;

@lombok.Data
public class DeletePipelineResponse {

	private List<Error> errors;

	@lombok.Data
	public static class Error {
		private String message;
	}

}