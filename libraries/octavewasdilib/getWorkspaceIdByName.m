function sWorkspaceId =getWorkspaceIdByName(Wasdi, sWorkspaceName);
% Get the Id of a Workspace from the name
% Syntax
% sWorkspaceId=getWorkspaceIdByName(Wasdi, sWorkspaceName);
% 
% INPUT
% :param Wasdi: Wasdi object created after the wasdilib call
% :param sWorkspaceName: Name of the workspace 
%  
% :returns: ``sWorkspaceId`` id of the workspace


  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   sWorkspaceId = Wasdi.getWorkspaceIdByName(sWorkspaceName);
   disp('got ws id');
   
   disp(['Workspace Id  ' sWorkspaceId]);

endfunction