function bDownloadActive = wGetDownloadActive(Wasdi)
%Gets whether download is active or not
%Syntax
%bDownloadActive = wGetDownloadActive(Wasdi)
%
%
%:param Wasdi: Wasdi object created after the wasdilib call
%
%:Returns:
%   :bDownloadActive: true if download is active, false otherwise

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not exist')
    return
   end
   
   bDownloadActive = Wasdi.getDownloadActive();
   
end