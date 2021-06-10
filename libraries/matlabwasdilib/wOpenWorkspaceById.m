function sWorkspaceId=wOpenWorkspaceById(Wasdi,sWorkspaceId)
% Opens a workspace given its ID
% Syntax
% sWorkspaceId=wOpenWorkspaceById(Wasdi,sWorkspaceId)
% 
% INPUT
%   Wasdi: Wasdi object created after the wasdilib call
%   sWorkspaceId: ID of the workspace to open
%
%:Returns:
%   sWorkspaceId: the ID of the workspace if succesfully opened, empty string otherwise
%

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not exist')
    return
   end
   
   sWorkspaceId=Wasdi.openWorkspaceById(sWorkspaceId)
   
   
end
