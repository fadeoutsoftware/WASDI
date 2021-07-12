function sStatus =addFileToWASDI(Wasdi, sFileName);
    % Ingest a new file in the Active WASDI Workspace waiting for the result
    %
    %    :param Wasdi: Wasdi object created after the wasdilib call
    %    :param sFileName: Name of the file to add
    %    :returns: a
    if exist("Wasdi") < 1
        disp('Wasdi variable does not existst')
        return
    end
    sStatus = Wasdi.addFileToWASDI(sFileName);
    disp(['Ingest Process Status ' sStatus]);
end