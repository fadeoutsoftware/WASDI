function asProducts=wGetProductsByWorkspace(Wasdi, sWorkspaceName)
%Get the List of Products in a Workspace
%Syntax
%asProducts=wGetProductsByWorkspace(Wasdi, sWorkspaceName);
%
%:param Wasdi: Wasdi object created after the wasdilib call
%:param sWorkspaceName: name of the workspace
%
%
%:Returns:
%  asProducts: array of strings that are the names of the products


  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   aoProducts= Wasdi.getProductsByWorkspace(sWorkspaceName);
   disp('got product list');
   
   iTot = aoProducts.size();
   
   disp(['products count ' sprintf("%d",iTot )]);

  %Per tutte le centraline trovate
  for iWs = 0:iTot-1
    asProducts{iWs+1} = aoProducts.get(iWs);
  end

   
end
