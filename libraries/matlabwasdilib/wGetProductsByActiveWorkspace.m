function asProducts=wGetProductsByActiveWorkspace(Wasdi)
%Get the List of Products in the active Workspace
%Syntax
%asProducts=wGetProductsByActiveWorkspace(Wasdi);
%
%:param Wasdi: Wasdi object created after the wasdilib call
%
%
%:Returns:
%  :asProducts: array of strings that are the names of the products


  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   aoProducts= Wasdi.getProductsByActiveWorkspace();
   disp('got product list');
   
   iTot = aoProducts.size();
   
   disp(['products count ' sprintf("%d",iTot )]);

  %Per tutte le centraline trovate
  for iWs = 0:iTot-1
    asProducts{iWs+1} = aoProducts.get(iWs);
  end

   
end
