function sWorkspaceName =wGetWorkspaceNameById(Wasdi, sWorkspaceId)
%Get the Name of a Workspace from the Id
%Syntax
%sWorkspaceId=wGetWorkspaceNameById(Wasdi, sWorkspaceName);
%
%:param Wasdi: Wasdi object created after the wasdilib call
%:param sWorkspaceId: Id of the workspace
%
%
%:Returns:
%  :sWorkspaceName: Name of the workspace

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   sWorkspaceName = char(Wasdi.getWorkspaceNameById(sWorkspaceId));
   
   disp(['Workspace Name  ' sWorkspaceName]);

end