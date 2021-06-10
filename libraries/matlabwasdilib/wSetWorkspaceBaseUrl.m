function wSetWorkspaceBaseUrl(Wasdi, sUrl)
% Set the workspace base URL
% Syntax
% wSetWorkspaceBaseUrl(Wasdi, sUrl)
% 
% INPUT
%    Wasdi: Wasdi object created after the wasdilib call
%    sUrl: the workspace base URL

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   Wasdi.setWorkspaceBaseUrl(sUrl)
   
end