package wasdi.operations;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import wasdi.ProcessWorkspaceUpdateSubscriber;
import wasdi.dataproviders.ProviderAdapter;
import wasdi.dataproviders.ProviderAdapterFactory;
import wasdi.io.WasdiProductReader;
import wasdi.io.WasdiProductReaderFactory;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.DownloadedFile;
import wasdi.shared.business.DownloadedFileCategory;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.config.DataProviderConfig;
import wasdi.shared.config.PathsConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.DownloadedFilesRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.parameters.DownloadFileParameter;
import wasdi.shared.payloads.DownloadPayload;
import wasdi.shared.queryexecutors.QueryExecutor;
import wasdi.shared.queryexecutors.QueryExecutorFactory;
import wasdi.shared.utils.EndMessageProvider;
import wasdi.shared.utils.MissionUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.gis.BoundingBoxUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.products.ProductViewModel;

/**
 * Download Operation
 * 
 * Use a DownloadFileParameter
 * 
 * This operation takes in input the url and credentials of a data provider 
 * and get the image from there.
 * 
 * First the operation search for an already existing copy of the file in the local node and 
 * if it is present it just makes a copy.
 * 
 * If not available the DataProvider object is demanded to take the file.
 * 
 * After the file is available, the operation try to read to the file to get the View Model.
 * 
 * If everything is ok the file is added to the Db in DownloadedFile table and is added to the workspace.
 * 
 * @author p.campanella
 *
 */
public class Download extends Operation implements ProcessWorkspaceUpdateSubscriber {
	
	/**
	 * List of Data Providers ordered for ranking: the first is the best
	 */
	ArrayList<ProviderAdapter> m_aoDataProviderRanking = new ArrayList<ProviderAdapter>();
	
	/**
	 * Index of the Data Provider actually used in the ranked list
	 */
	private int m_iDataProviderIndex = 0;

	@Override
	public boolean executeOperation(BaseParameter oParam, ProcessWorkspace oProcessWorkspace) {
		
		WasdiLog.infoLog("Download.executeOperation");
		
		if (oParam == null) {
			WasdiLog.errorLog("Parameter is null");
			return false;
		}
		
		if (oProcessWorkspace == null) {
			WasdiLog.errorLog("Process Workspace is null");
			return false;
		}

        String sFileName = "";

        try {
        	
        	DownloadFileParameter oParameter = (DownloadFileParameter) oParam;
        	
        	if (Utils.isNullOrEmpty(oParameter.getProvider())) {
        		oParameter.setProvider("AUTO");
        	}
        	        	
            m_oProcessWorkspaceLogger.log("Fetch Start - REQUESTED PROVIDER " + oParameter.getProvider());

            ProviderAdapter oProviderAdapter = null; 
            
            if (oParameter.getProvider().equals("AUTO")) {
            	oProviderAdapter = getBestProviderAdapater(oParameter, oProcessWorkspace);
            	
            	if (oProviderAdapter != null) {
                    WasdiLog.debugLog("Got Data Provider " + oProviderAdapter.getCode());
                    m_oProcessWorkspaceLogger.log("Fetch - SELECTED PROVIDER " + oProviderAdapter.getCode());            	            		
            	}
            	else {
                    WasdiLog.errorLog("Download.executeOperation: Impossible to get a valid Data Provider");
                    m_oProcessWorkspaceLogger.log("ERROR - Impossible to get a valid Data Provider");
                    return false;
            	}
            }
            else {
            	oProviderAdapter = getProviderAdapater(oParameter.getProvider(), oParameter, oProcessWorkspace);
            }
            
            if (oProviderAdapter == null) {
            	m_oProcessWorkspaceLogger.log("ERROR searching a valid Data Provider. Abort.");
            	return false;
            }
            
            // Read the Platform Type from the parameter
    		String sPlatformType = oParameter.getPlatform();
    		
    		// If not, try to autodetect it
    		if (Utils.isNullOrEmpty(sPlatformType)) {
    			sPlatformType = MissionUtils.getPlatformFromSatelliteImageFileName(sFileName);
    		}            
            
            // get file size
            long lFileSizeByte = oProviderAdapter.getDownloadFileSize(oParameter.getUrl());
            // set file size
            setFileSizeToProcess(lFileSizeByte, oProcessWorkspace);
            
            String sDownloadPath = PathsConfig.getWorkspacePath(oParameter);

            // Product view Model
            ProductViewModel oVM = null;

            // Get the file name
            String sFileNameWithoutPath = oProviderAdapter.getFileName(oParameter.getUrl());
            WasdiLog.debugLog("Download.executeOperation: File to download: " + sFileNameWithoutPath);
            m_oProcessWorkspaceLogger.log("Importing in WASDI file: " + sFileNameWithoutPath);
            
            
            // Check if the file is already available
            DownloadedFile oAlreadyDownloaded = fileAlreadyAvailable(sFileNameWithoutPath, oParameter.getWorkspace(), oParameter.getWorkspaceOwnerId());
            
            DownloadedFilesRepository oDownloadedRepo = new DownloadedFilesRepository();
            
            if (oAlreadyDownloaded == null) {
                WasdiLog.debugLog("Download.executeOperation: File not already downloaded. Download it");

                if (!Utils.isNullOrEmpty(sFileNameWithoutPath)) {
                	
                    oProcessWorkspace.setProductName(sFileNameWithoutPath);
                    
                    // update the process
                    m_oProcessWorkspaceRepository.updateProcess(oProcessWorkspace);
                    
                    // Send Rabbit notification
                    m_oSendToRabbit.SendUpdateProcessMessage(oProcessWorkspace);
                    
                } else {
                    WasdiLog.errorLog("Download.executeOperation: sFileNameWithoutPath is null or empty!!");
                }

                oProviderAdapter.subscribe(this);
                
                // Until we cannot fetch the file                
                while (Utils.isNullOrEmpty(sFileName)) {
                	
                	DataProviderConfig oDataProviderConfig = WasdiConfig.Current.getDataProviderConfig(oProviderAdapter.getCode());
                	
                	if (oDataProviderConfig != null) {
                        // Download the File
                		try {
                			sFileName = oProviderAdapter.executeDownloadFile(oParameter.getUrl(), oDataProviderConfig.user, oDataProviderConfig.password, sDownloadPath, oProcessWorkspace, oParameter.getMaxRetry());	
                		}
                		catch (Exception oEx) {
							WasdiLog.errorLog("Download.executeOperation: exception in oProviderAdapter.executeDownloadFile", oEx );
						}
                        
                        // Is it null?!?
                        if (Utils.isNullOrEmpty(sFileName)) {

                            int iLastError = oProviderAdapter.getLastServerError();
                            String sError = "There was an error contacting the provider";

                            if (iLastError > 0) {
                            	sError += ": query obtained HTTP Error Code " + iLastError;
                            }

                            m_oProcessWorkspaceLogger.log(sError);                        
                        }
                        else {
                        	try {
                                // Control Check for the file Name
                                sFileName = WasdiFileUtils.fixPathSeparator(sFileName);

                                // Get The product view Model
                				WasdiProductReader oProductReader = WasdiProductReaderFactory.getProductReader(new File(sFileName));
                				sFileName = oProductReader.adjustFileAfterDownload(sFileName, sFileNameWithoutPath);
                				File oProductFile = new File(sFileName);
                				
                				sFileNameWithoutPath = oProductFile.getName(); 
                				
                				try {
                					oVM = oProductReader.getProductViewModel();
                				}
                				catch (Exception oVMEx) {
                					WasdiLog.warnLog("Download.executeOperation: exception reading Product View Model " + oVMEx.toString());
        						}
                				
                				if (oVM == null) {
                    				// Reset the cycle to search a better solution
                    				sFileName = "";        					
                				}                        		
                        	}
                        	catch (Exception oReadFileEx) {
								WasdiLog.errorLog("Download.executeOperation: exception trying to read the downloaded file, reset FileName to try with other data providers", oReadFileEx);
								sFileName = "";
							}
                        }                		
                	}
                	else {
                		WasdiLog.errorLog("Download.executeOperation: DataProviderConfig is null for Provider Adapter " + oProviderAdapter.getCode() + " try the next one if exists");
                	}
                    
                    
                    if (Utils.isNullOrEmpty(sFileName)) {
                        oProviderAdapter.unsubscribe(this);
                        oProviderAdapter.closeConnections();
                        
                        oProviderAdapter = getNextDataProvider(oParameter);
                                                
                        if (oProviderAdapter == null) {
                        	WasdiLog.warnLog("Download.executeOperation: data provider finished, return false");
                        	return false;
                        }
                        
                        oProviderAdapter.subscribe(this);
                        
                        m_oProcessWorkspaceLogger.log("Download.executeOperation: got next data provider " + oProviderAdapter.getCode());
                        WasdiLog.infoLog("Download.executeOperation: got next data provider " + oProviderAdapter.getCode());                    	
                    }
                }


                oProviderAdapter.unsubscribe(this);
                WasdiLog.debugLog("Download.executeOperation: calling closeConnections");
                oProviderAdapter.closeConnections();

                m_oProcessWorkspaceLogger.log("Got File, try to read");

				
                // Save it in the register
                oAlreadyDownloaded = new DownloadedFile();
                oAlreadyDownloaded.setFileName(sFileNameWithoutPath);
                oAlreadyDownloaded.setFilePath(sFileName);
                oAlreadyDownloaded.setProductViewModel(oVM);
                oAlreadyDownloaded.setPlatform(sPlatformType);

                String sBoundingBox = oParameter.getBoundingBox();

                if (!Utils.isNullOrEmpty(sBoundingBox)) {
                    if (sBoundingBox.startsWith("POLY") || sBoundingBox.startsWith("MULTI")) {
                        sBoundingBox = BoundingBoxUtils.polygonToBounds(sBoundingBox);
                    }

                    oAlreadyDownloaded.setBoundingBox(sBoundingBox);
                } else {
                    WasdiLog.debugLog("Download.executeOperation: bounding box not available in the parameter");
                }

                oAlreadyDownloaded.setCategory(DownloadedFileCategory.DOWNLOAD.name());
                oDownloadedRepo.insertDownloadedFile(oAlreadyDownloaded);
                
            } else {
                WasdiLog.debugLog("Download.executeOperation: File already downloaded: make a copy");

                // Yes!! Here we have the path
                sFileName = oAlreadyDownloaded.getFilePath();

                WasdiLog.debugLog("Download.executeOperation: Check if file exists");

                // Check the path where we want the file
                String sDestinationFileWithPath = sDownloadPath + sFileNameWithoutPath;

                // Is it different?
                if (sDestinationFileWithPath.equals(sFileName) == false) {
                    // if file doesn't exist
                    if (!new File(sDestinationFileWithPath).exists()) {
                        // Yes, make a copy
                        FileUtils.copyFile(new File(sFileName), new File(sDestinationFileWithPath));
                        // Files.createLink(link, existing)
                        sFileName = sDestinationFileWithPath;
                    } else {
                        // If it exists...
                        sFileName = sDestinationFileWithPath;
                    }
                }

            } 
            
            // Final Check: do we have at the end a valid file name?
            if (Utils.isNullOrEmpty(sFileName)) {
            	
            	// No, we are in error
                WasdiLog.warnLog("Download.executeOperation: file is null there must be an error");

                String sError = "The name of the file to download result null";
                m_oSendToRabbit.SendRabbitMessage(false, LauncherOperations.DOWNLOAD.name(), oParameter.getWorkspace(), sError, oParameter.getExchange());
                oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
                
                return false;
            } 
            
            // Ok, add the product to the db
            addProductToDbAndWorkspaceAndSendToRabbit(oVM, sFileName, oParameter.getWorkspace(), oParameter.getExchange(), LauncherOperations.DOWNLOAD.name(), oParameter.getBoundingBox());

            m_oProcessWorkspaceLogger.log("Operation Completed");
            m_oProcessWorkspaceLogger.log(new EndMessageProvider().getGood());

            DownloadPayload oDownloadPayload = new DownloadPayload();
            oDownloadPayload.setFileName(WasdiFileUtils.getFileNameWithoutLastExtension(sFileName));
            oDownloadPayload.setProvider(oParameter.getProvider());
            if (oProviderAdapter != null) {
            	oDownloadPayload.setSelectedProvider(oProviderAdapter.getCode());
            }

            setPayload(oProcessWorkspace, oDownloadPayload);
            
            WasdiLog.debugLog("Download.executeOperation: operation done");
            updateProcessStatus(oProcessWorkspace, ProcessStatus.DONE, 100);
            
            return true;
            
        } 
        catch (Exception oEx) {
        	
            WasdiLog.errorLog("Download.executeOperation: Exception ", oEx);

            String sError = oEx.toString();
            oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
            m_oSendToRabbit.SendRabbitMessage(false, LauncherOperations.DOWNLOAD.name(), oParam.getWorkspace(), sError, oParam.getExchange());
        }

        WasdiLog.debugLog("Download.executeOperation: return file name " + sFileName);

        return false;
		
	}
	
    @Override
    public void notify(ProcessWorkspace oProcessWorkspace) {

        if (oProcessWorkspace == null) return;

        try {
            ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();

            // update the process
            if (!oProcessWorkspaceRepository.updateProcess(oProcessWorkspace))
                WasdiLog.errorLog("Download.executeOperationFile: Error during process update with process Perc");

            // send update process message
            if (m_oSendToRabbit != null) {
                if (!m_oSendToRabbit.SendUpdateProcessMessage(oProcessWorkspace)) {
                	WasdiLog.errorLog("Download.executeOperationFile: Error sending rabbitmq message to update process list");
                }
            }
        } catch (Exception oEx) {
        	WasdiLog.errorLog("Download.executeOperationFile: Exception: " + oEx);
        	WasdiLog.debugLog("Download.executeOperationFile: " + ExceptionUtils.getStackTrace(oEx));
        }
    }
    
    /**
     * Check if the file is already available
     * @param sFileNameWithoutPath
     * @return
     */
	protected DownloadedFile fileAlreadyAvailable(String sFileNameWithoutPath, String sWorkspaceId, String sWorkspaceOwnerId) {

        String sDownloadPath = PathsConfig.getWorkspacePath(sWorkspaceOwnerId, sWorkspaceId);
        
        DownloadedFile oAlreadyDownloaded = null;
        
        if (Utils.isNullOrEmpty(sFileNameWithoutPath)) return oAlreadyDownloaded;

        try {
            
            DownloadedFilesRepository oDownloadedRepo = new DownloadedFilesRepository();

            // First check if it is already in this workspace:
            oAlreadyDownloaded = oDownloadedRepo.getDownloadedFileByPath(sDownloadPath + sFileNameWithoutPath);

            if (oAlreadyDownloaded == null) {
            	
            	// Check if it is already downloaded, in any other workpsace
            	
                WasdiLog.debugLog("Download.fileAlreadyAvailable: Product NOT found in the workspace, search in other workspaces");
                
                List<DownloadedFile> aoExistingList = oDownloadedRepo.getDownloadedFileListByName(sFileNameWithoutPath);

                // Check if any of this is in this node
                for (DownloadedFile oDownloadedCandidate : aoExistingList) {

                    if (new File(oDownloadedCandidate.getFilePath()).exists()) {
                        oAlreadyDownloaded = oDownloadedCandidate;
                        WasdiLog.debugLog("Download.fileAlreadyAvailable: found already existing copy on this computing node");
                        break;
                    }
                }
            }

            if (oAlreadyDownloaded != null)  
            {
            	WasdiLog.debugLog("Download.fileAlreadyAvailable: File already downloaded: make a copy");
            	
            	String sFileNameWithFullPath = "";

                // Yes!! Here we have the path
                sFileNameWithFullPath = oAlreadyDownloaded.getFilePath();

                WasdiLog.debugLog("Download.fileAlreadyAvailable: Check if file exists");

                // Check the path where we want the file
                String sDestinationFileWithPath = PathsConfig.getWorkspacePath(sWorkspaceOwnerId, sWorkspaceId) + sFileNameWithoutPath;

                // Is it different?
                if (sDestinationFileWithPath.equals(sFileNameWithFullPath) == false) {
                    // if file doesn't exist
                    if (!new File(sDestinationFileWithPath).exists()) {
                        // Yes, make a copy
                        FileUtils.copyFile(new File(sFileNameWithFullPath), new File(sDestinationFileWithPath));
                        // Files.createLink(link, existing)
                    }
                }
                
                sFileNameWithFullPath = sDestinationFileWithPath;
                
                if (!new File(sFileNameWithFullPath).exists()) {
                	oAlreadyDownloaded = null;
                }
                else {
                	oAlreadyDownloaded.setFilePath(sFileNameWithFullPath);
                }

            } 
            
            return oAlreadyDownloaded;
            
        } catch (Exception oEx) {        	
            WasdiLog.errorLog("Download.fileAlreadyAvailable: Exception ", oEx);
        }

        return oAlreadyDownloaded;		
	}
    
	/**
	 * Select the best Provider Adapter for this Parameter (so file to download)
	 * @param oParameter
	 * @param oProcessWorkspace
	 * @return
	 */
	public ProviderAdapter getBestProviderAdapater(DownloadFileParameter oParameter, ProcessWorkspace oProcessWorkspace) {
		
		/**
		 * Parallel list of scores for each Data Provider
		 */
		ArrayList<Integer> aiScores = new ArrayList<Integer>();		
		
		try {
			
			// For all the Data Providers			
			for (DataProviderConfig oDataProviderConfig : WasdiConfig.Current.dataProviders) {
				
				// Create and configure the Provider Adapter
	            ProviderAdapter oProviderAdapter = getProviderAdapater(oDataProviderConfig.name, oParameter, oProcessWorkspace);
	            
	            if (oProviderAdapter == null) {
	            	continue;
	            }
	            
	            // Compute the score for this Provider Adapter				
	            int iScore = oProviderAdapter.getScoreForFile(oParameter.getName(), oParameter.getPlatform());
	            
	            // Score must be > 0, otherwise file is not supported
	            if (iScore > 0) {
	            	
	            	// Search the position of this score in the ranking
	            	int iIndex = 0;
	            	
	            	for (iIndex = 0; iIndex < aiScores.size(); iIndex++) {
	            		if (aiScores.get(iIndex)<iScore) break; 
	            	}
	            	
	            	// Insert the data Provider in the correct position
	            	aiScores.add(iIndex, iScore);
	            	m_aoDataProviderRanking.add(iIndex,oProviderAdapter);
	            }	            
			}
			
			// For the selected data providers, starting from the best
			for (m_iDataProviderIndex = 0; m_iDataProviderIndex<m_aoDataProviderRanking.size(); m_iDataProviderIndex++) {
				
				// Check if the file is in the catalogues
				ProviderAdapter oProviderAdapter = m_aoDataProviderRanking.get(m_iDataProviderIndex);
				
				if (doesProviderAdapterFindFile(oProviderAdapter, oParameter)) {
					// Ok return this
					return oProviderAdapter;
				}
			}
			
			if (m_aoDataProviderRanking.size()>0) {
				// We have data providers that should support the platform, but we are not finding it
				WasdiLog.infoLog("Download.getBestProviderAdapater: there are providers " + m_aoDataProviderRanking.size() + " but none looks to find the file " + oParameter.getName() + ". We try in any case to proceed");
				if (m_oProcessWorkspaceLogger!=null) m_oProcessWorkspaceLogger.log("There are providers [" + m_aoDataProviderRanking.size() + "] that should, but none looks to find the file " + oParameter.getName() + ". We try in any case to proceed");
				
				// We can try in any case to proceed:
				return m_aoDataProviderRanking.get(0);
			}
			
		}
		catch (Exception oEx) {
            WasdiLog.errorLog("Download.getBestProviderAdapater: Exception "+ ExceptionUtils.getStackTrace(oEx));
        }
		
		return null;
	}
	
	/**
	 * If something goes wrong, returns the next valid Data Provider
	 * @return
	 */
	public ProviderAdapter getNextDataProvider(DownloadFileParameter oParameter) {
		
		// Move to the next Data Provider
		m_iDataProviderIndex++;
		// Keep the index of the one we should test now
		int iInitialDataProviderIndex = m_iDataProviderIndex;
		
		// For the selected data providers, starting from the last
		for (; m_iDataProviderIndex<m_aoDataProviderRanking.size(); m_iDataProviderIndex++) {
			
			// Check if the file is in the catalogues
			ProviderAdapter oProviderAdapter = m_aoDataProviderRanking.get(m_iDataProviderIndex);
			
			if (doesProviderAdapterFindFile(oProviderAdapter, oParameter)) {
				// Ok we fond our file
	            DataProviderConfig oDataProviderConfig = WasdiConfig.Current.getDataProviderConfig(oProviderAdapter.getCode());
	            oParameter.setDownloadUser(oDataProviderConfig.user);
	            oParameter.setDownloadPassword(oDataProviderConfig.password);
				
				// Return this Provider Adapter
				return oProviderAdapter;
			}
		}
		
		try {
			// Here it means that we did not found anything. Lets check if it is a problem of query or if providers are finished			
			if (iInitialDataProviderIndex<m_aoDataProviderRanking.size()) {
				
				// Yes, we have al least one data provider to try 
				ProviderAdapter oProviderAdapter = m_aoDataProviderRanking.get(iInitialDataProviderIndex);
				
				// Log it
				WasdiLog.warnLog("Download.getNextDataProvider: The provider " + oProviderAdapter.getCode() + " Should support the platform but is not finding " + oParameter.getName() + ". We try in any case because all failed");
				
				// Try to set credentials
	            DataProviderConfig oDataProviderConfig = WasdiConfig.Current.getDataProviderConfig(oProviderAdapter.getCode());
	            oParameter.setDownloadUser(oDataProviderConfig.user);
	            oParameter.setDownloadPassword(oDataProviderConfig.password);
	            
	            // Re-set the data provider index
	            m_iDataProviderIndex = iInitialDataProviderIndex;
				
				// Ok return this
				return oProviderAdapter;				
			}			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("Download.getNextDataProvider: exception trying to recover at least a Data Provider: ", oEx);
		}
		
		// If we arrive here there is no chance to find the file
		return null;
	}
	
	/**
	 * Check if a Data Provider finds the requested file in the catalogue	
	 * @param oProviderAdapter
	 * @param oParameter
	 * @return
	 */
	boolean doesProviderAdapterFindFile(ProviderAdapter oProviderAdapter, DownloadFileParameter oParameter) {
		QueryExecutor oQueryExecutor = QueryExecutorFactory.getExecutor(oProviderAdapter.getCode());
		
		// Must obtain the URI!!
		String sFileUri = oQueryExecutor.getUriFromProductName(oParameter.getName(), WasdiConfig.Current.getDataProviderConfig(oProviderAdapter.getCode()).defaultProtocol, oParameter.getUrl(), oParameter.getPlatform());
		
		// Close connections
		oQueryExecutor.closeConnections();
		
		if (!Utils.isNullOrEmpty(sFileUri)) {
			// If we got the URI, this is the best Provider Adapter
			
			// Set the URI to the parameter
			oParameter.setUrl(sFileUri);
			// Return the Provider Adapter
			return true;
		}
		
		return false;
		
	}
	
	/**
	 * Get a configured instance of a Provider Adapter
	 * @param sCode Code of the provider adapter
	 * @param oParameter DownloedFileParameter
	 * @param oProcessWorkspace Process Workspace
	 * @return ProviderAdapter configured
	 */
	public ProviderAdapter getProviderAdapater(String sCode, DownloadFileParameter oParameter, ProcessWorkspace oProcessWorkspace) {
		try {
			
            ProviderAdapter oProviderAdapter = new ProviderAdapterFactory().supplyProviderAdapter(sCode);
            
            DataProviderConfig oDataProviderConfig = WasdiConfig.Current.getDataProviderConfig(sCode);
            oParameter.setDownloadUser(oDataProviderConfig.user);
            oParameter.setDownloadPassword(oDataProviderConfig.password);
            
			oProviderAdapter.readConfig();
            oProviderAdapter.setProviderUser(oParameter.getDownloadUser());
            oProviderAdapter.setProviderPassword(oParameter.getDownloadPassword());
            oProviderAdapter.setProcessWorkspace(oProcessWorkspace);
            
            return oProviderAdapter;

		}
		catch (Exception oEx) {
            WasdiLog.errorLog("Download.getProviderAdapater: Exception " + ExceptionUtils.getStackTrace(oEx));
        }
		
		return null;
	}

}
