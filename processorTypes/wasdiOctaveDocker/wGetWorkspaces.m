function [asWorkspaceNames, asWorkspaceIds]=wGetWorkspaces(Wasdi);
% Get the List of Workspace of the actual User
% Syntax
% [asWorkspaceNames, asWorkspaceIds]=wGetWorkspaces(Wasdi);
% 
% INPUT
%    Wasdi: Wasdi object created after the wasdilib call
%  
%
% OUTPUT
%   asWorkspaceNames: array of strings that are the names of the workspaces
%   asWorkspaceIds: array of strings that are the id of the workspaces

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   aoWorkspaces = Wasdi.getWorkspaces();
   
   iTotWs = aoWorkspaces.size();
   
   disp(['Number of Workspaces: ' sprintf("%d",iTotWs)]);

  % For each workspace
  for iWs = 0:iTotWs-1
    oWorkspace = aoWorkspaces.get(iWs);
    asWorkspaceNames{iWs+1} = oWorkspace.get("workspaceName");
    asWorkspaceIds{iWs+1} = oWorkspace.get("workspaceId");
  end

   
end
