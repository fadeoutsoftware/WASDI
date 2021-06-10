function wSetActiveWorkspaceId(Wasdi, sNewActiveWorkspaceId)
%Set the active workspace
%Syntax
%wSetActiveWorkspaceId(Wasdi, sNewActiveWorkspaceId)
%
%:param Wasdi: Wasdi object created after the wasdilib call
%:param sNewActiveWorkspaceId: the workspace ID to open
%

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
    Wasdi.setActiveWorkspace(sNewActiveWorkspaceId)
end