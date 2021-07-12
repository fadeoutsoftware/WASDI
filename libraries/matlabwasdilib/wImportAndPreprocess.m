function wImportAndPreprocess(Wasdi, asProductLinks, asProductNames, sWorkflowName, sSuffix, sProvider=[])
%Import and preprocess a collection of EO products
%Syntax
%wImportAndPreprocess(Wasdi, asProductLinks, asProductNames, sWorkflowName, sSuffix)
%
%:param Wasdi: Wasdi object created after the wasdilib call
%:param asProductLinks: collection of Product Direct Link as returned by wSearchEOImages
%:param asProductNames: collection of Product names, as returned by wSearchEOImages
%:param sWorkflowName: the name of the SNAP workflow to be applied to downloaded imagesc
%:param sSuffix: the suffix to append to the preprocessed files
%:param sProvider: optional, the provider from where data must be collected
%

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   asLinks = javaObject('java.util.ArrayList',length(asProductLinks))
   asNames = javaObject('java.util.ArrayList',length(asProductNames))
   for i = 1:length(asProductLinks)
     asLinks.add(asProductLinks{i})
     asNames.add(asProductNames{i})
   endfor
   
   Wasdi.importAndPreprocessWithLinks(asLinks, asNames, sWorkflowName, sSuffix, sProvider);
   
end
