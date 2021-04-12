function sStatus=wSubset(Wasdi, sInputFile, sOutputFile, dLatN, dLonW, dLatS, dLonE)
% Make a Subset (tile) of an input image in a specified Lat Lon Rectangle
% Syntax
% sStatus=wSubset(Wasdi, sInputFile, sOutputFile, dLatN, dLonW, dLatS, dLonE)
% 
% INPUT
%    Wasdi: Wasdi object created after the wasdilib call
%	 sInputFile Name of the input file
%	 sOutputFile Name of the output file
%	 dLatN Lat North Coordinate
%	 dLonW Lon West Coordinate
%	 dLatS Lat South Coordinate
%	 dLonE Lon East Coordinate 
%
% OUTPUT
%   sStatus: Status of the operation
%

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   sStatus = Wasdi.subset(sInputFile, sOutputFile, dLatN, dLonW, dLatS, dLonE);
   
   disp(['Subset Status: ' sStatus]);
   
end
