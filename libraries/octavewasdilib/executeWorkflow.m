function sStatus =executeWorkflow(Wasdi, sWorkflow, asInputFiles, asOutputFiles)
% Executes a SNAP workflow. The workflow has to be uploaded in WASDI: it can be public or private of a user.
% If it is private, it must be triggered from the owner.
% Syntax
% sStatus = wExecuteWorkflow(Wasdi, sWorkflow, asInputFiles, asOutputFiles)
%
% :param Wasdi: Wasdi object created after the wasdilib call
% :param sWorkflow: Name of the workflow
% :param asInputFiles: array of strings with the name of the input files. Must be one file for each Read Node of the workflow, in the exact order
% :param asOutputFiles: array of strings with the name of the output files. Must be one file for each Write Node of the workflow, in the exact order
% :returns: ``sStatus`` Exit Workflow Process Status as a String: CREATED,  RUNNING,  STOPPED,  DONE,  ERROR
%

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
  
   sStatus = Wasdi.executeWorkflow(asInputFiles, asOutputFiles, sWorkflow);
   
   disp(['Process Status ' sStatus]);

endfunction