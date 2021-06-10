function sWorkspaceOwner = wGetWorkspaceOwnerByName(Wasdi, sWorkspaceName)
%
% Gets the owner of the workspace given its name
% Syntax
% sWorkspaceOwner = wGetWorkspaceOwnerByName(Wasdi, sWorkspaceName)
% 
% INPUT
%    Wasdi: Wasdi object created after the wasdilib call
%    sWorkspaceName: the name of the workspace
%
% OUTPUT
%   sWorkspaceOwner: the owner of the workspace

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not exist')
    return
   end
   
   sWorkspaceOwner = Wasdi.getWorkspaceOwnerByName(sWorkspaceName);
   
end
