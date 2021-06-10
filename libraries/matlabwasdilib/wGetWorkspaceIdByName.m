function sWorkspaceId =wGetWorkspaceIdByName(Wasdi, sWorkspaceName)
%Get the Id of a Workspace from the name
%Syntax
%sWorkspaceId=wGetWorkspaceIdByName(Wasdi, sWorkspaceName);
%
%:param Wasdi: Wasdi object created after the wasdilib call
%:param sWorkspaceName: Name of the workspace
%
%
%:Returns:
%  sWorkspaceId: id of the workspace

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   sWorkspaceId = char(Wasdi.getWorkspaceIdByName(sWorkspaceName));
   
   disp(['Workspace Id  ' sWorkspaceId]);

end