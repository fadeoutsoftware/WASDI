function sWorkspaceId =openWorkspace(Wasdi, sWorkspaceName)
% Open a Workspace
% Syntax
% sWorkspaceId=openWorkspace(Wasdi, sWorkspaceName);
% 
% INPUT
% :param Wasdi: Wasdi object created after the wasdilib call
% :param sWorkspaceName: Name of the workspace 
%
% :returns sWorkspaceId: id of the workspace
;
  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   sWorkspaceId = Wasdi.openWorkspace(sWorkspaceName);
     disp('Workspace Opened');
   
   disp(['Workspace Id: ' sWorkspaceId]);

endfunction
