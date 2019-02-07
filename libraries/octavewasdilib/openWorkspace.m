function sWorkspaceId =openWorkspace(Wasdi, sWorkspaceName);
% Open a Workspace
% Syntax
% sWorkspaceId=openWorkspace(Wasdi, sWorkspaceName);
% 
% INPUT
%    Wasdi: Wasdi object created after the wasdilib call
%    sWorkspaceName: Name of the workspace 
%  
%
% OUTPUT
%   sWorkspaceId: id of the workspace

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   sWorkspaceId = Wasdi.openWorkspace(sWorkspaceName);
     disp('Workspace Opened');
   
   disp(['Workspace Id: ' sWorkspaceId]);

endfunction
