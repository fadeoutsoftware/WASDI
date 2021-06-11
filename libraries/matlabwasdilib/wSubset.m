function sStatus=wSubset(Wasdi, sInputFile, sOutputFile, dLatN, dLonW, dLatS, dLonE)
%Make a Subset (tile) of an input image in a specified Lat Lon Rectangle
%Syntax
%sStatus=wSubset(Wasdi, sInputFile, sOutputFile, dLatN, dLonW, dLatS, dLonE)
%
%:param Wasdi: Wasdi object created after the wasdilib call
%:param sInputFile: Name of the input file
%:param sOutputFile: Name of the output file
%:param dLatN: Lat North Coordinate
%:param dLonW: Lon West Coordinate
%:param dLatS: Lat South Coordinate
%:param dLonE: Lon East Coordinate
%
%:Returns:
%  :sStatus: Status of the operation
%

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   sStatus = Wasdi.subset(sInputFile, sOutputFile, dLatN, dLonW, dLatS, dLonE);
   
   disp(['Subset Status: ' sStatus]);
   
end
