function sProcessId = wAsynchExecuteWorkflow(Wasdi, sWorkflow, asInputFiles, asOutputFiles)
% Executes a SNAP workflow in Asynch mode. The workflow has to be uploaded in WASDI: it can be public or private of a user. 
% If it is private, it must be triggered from the owner.
% Syntax
% sProcessId = wExecuteWorkflow(Wasdi, sWorkflow, asInputFiles, asOutputFiles);
% 
% INPUT
%    Wasdi: Wasdi object created after the wasdilib call
%    sWorkflow: Name of the workflow
%    asInputFiles: array of strings with the name of the input files. Must be one file for each Read Node of the workflow, in the exact order
%    asOutputFiles: array of strings with the name of the output files. Must be one file for each Write Node of the workflow, in the exact order
%  
%
% OUTPUT
%   sProcessId: Id of the process representing the Workflow execution. Can be used as input to the wWaitProcess method or wGetProcessStatus methods to check the execution.

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
  
   sProcessId = char(Wasdi.asynchExecuteWorkflow(asInputFiles, asOutputFiles, sWorkflow));
   
   disp(['Process Id: ' sProcessId]);

end