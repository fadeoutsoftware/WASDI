
% Check if there is the wasdipath variable
iPathExists = exist("wasdipath");

% Initialize path as folder containing this file
sPath = fileparts(mfilename('fullpath'));

% Check if we need to use the user supplied one
if iPathExists > 0
   sPath = wasdipath
end

% add internal path 
addpath(genpath(sPath))
% add WASDI Library Jar
javaaddpath(fullfile(sPath, 'jwasdilib-0.9.1.jar'))

% Log
disp('WASDI MATLAB LIB INITIALIZED (v.0.09.01)')

% Create the Wasdi Object
Wasdi = javaObject ("wasdi.jwasdilib.WasdiLib");

Wasdi.init();