package it.fadeout.rest.resources;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ProcessWorkspaceAggregatedViewModel {

	private String schedulerName;
	private String operationType;
	private String operationSubType;

	private Integer procWaiting;
	private Integer procReady;
	private Integer procRunning;
	private Integer procCreated;

//	private Integer procError;
//	private Integer procDone;
//	private Integer procStopped;

}
