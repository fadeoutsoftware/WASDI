function sWorkspaceId =wOpenWorkspace(Wasdi, sWorkspaceName)
% Open a Workspace
% Syntax
% sWorkspaceId=wOpenWorkspace(Wasdi, sWorkspaceName);
% 
% INPUT
%    Wasdi: Wasdi object created after the wasdilib call
%    sWorkspaceName: Name of the workspace 
%  
%
%:Returns:
%   sWorkspaceId: id of the workspace

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   sWorkspaceId = char(Wasdi.openWorkspace(sWorkspaceName));
   
   disp(['Workspace Opened - Id: ' sWorkspaceId]);

end
