function sStatus=wMosaic(Wasdi, asInputFileNames, sOutputFile, dPixelSizeX, dPixelSizeY)
% Search for EO images
% Syntax
% sStatus=wMosaic(Wasdi, asInputFileNames, sOutputFile, dPixelSizeX, dPixelSizeY)
% 
% INPUT
%    Wasdi: Wasdi object created after the wasdilib call
%	 asInputFileNames: Array of input file names
%	 sOutputFile: Name of the output file
%	 dPixelSizeX: Pixel X Size
%	 dPixelSizeY: Pixel Y Size
%
% OUTPUT
%   sStatus: end status of the mosaic operation
%

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   sStatus = Wasdi.mosaic(asInputFileNames, sOutputFile, dPixelSizeX, dPixelSizeY);
   
   disp(['Mosaic Status: ' sStatus]);
   
end
