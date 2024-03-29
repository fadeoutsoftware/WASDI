function [asWorkflowNames, asWorkflowIds]=wGetWorkflows(Wasdi)
%Get the List of Workflows of the actual User
%Syntax
%[asWorkflowNames, asWorkflowIds]=wGetWorkflows(Wasdi);
%
%:param Wasdi: Wasdi object created after the wasdilib call
%
%
%:Returns:
%  :asWorkflowNames: array of strings that are the names of the workflows
%  :asWorkflowIds: array of strings that are the id of the workflows
  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   aoWorkflows = Wasdi.getWorkflows();
   
   iTot = aoWorkflows.size();
   
   disp(['Number of Workflows: ' sprintf("%d",iTot)]);

  %For each Workflow
  for iWf = 0:iTot-1
    oWorkflow = aoWorkflows.get(iWf);
    asWorkflowNames{iWf+1} = oWorkflow.get("name");
    asWorkflowIds{iWf+1} = oWorkflow.get("workflowId");
  end

   
end
