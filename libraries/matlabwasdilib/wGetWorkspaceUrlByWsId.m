function sWorkspaceUrl = wGetWorkspaceUrlByWsId(Wasdi, sWorkspaceId)
% Gets the workspace URL given its ID
% Syntax:
% sWorkspaceUrl = wGetWorkspaceUrlByWsId(Wasdi, sWorkspaceId)
% 
% INPUT
%    Wasdi: Wasdi object created after the wasdilib call
%
%:Returns:
%   sBaseUrl: the base URL for WASDI

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not exist')
    return
   end
   
   sWorkspaceUrl = char(Wasdi.getWorkspaceUrlByWsId(sWorkspaceId));
   
end
