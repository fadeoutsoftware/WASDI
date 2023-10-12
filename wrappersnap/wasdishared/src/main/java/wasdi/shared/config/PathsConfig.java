package wasdi.shared.config;

import java.io.File;

import wasdi.shared.business.processors.Processor;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.utils.Utils;

/**
 * WASDI Paths Config
 * @author p.campanella
 *
 */
public class PathsConfig {
	
	/**
	 * Path of the web application
	 */
	public String tomcatWebAppPath;
	
	/**
	 * Base root path that contains subfolders:
	 * 	.workspaces
	 * 	.metadata
	 * 	.styles
	 * 	.workflows
	 * 	.processors
	 *  .images
	 */
	public String downloadRootPath;
	/**
	 * Path where Parameters are serialized
	 */
	public String serializationPath;
	/**
	 * Metadata files path
	 */
	public String metadataPath;
	/**
	 * Docker templates path
	 */
	public String dockerTemplatePath;
	/**
	 * Root of the local sftp server of the node
	 */
	public String sftpRootPath;
	/**
	 * Geoserver data dir
	 */
	public String geoserverDataDir;
	/**
	 * Matlab runtime path
	 */
	public String matlabRuntimePath;
	/**
	 * Sen 2 Core bin path
	 */
	public String sen2CorePath;
	/**
	 * User Home Path
	 */
	public String userHomePath;
	/**
	 * Missions file config path
	 */
	public String missionsConfigFilePath;
	/**
	 * Gdal bin Path
	 */
	public String gdalPath;
	
	/**
	 * Folder used by WASDI to write temporary files
	 */
	public String wasdiTempFolder;
	
	/**
	 * Path the python tool used to render jinja templates by java code.
	 * We use external script because there was an incompatibility with the most know jar implementing it
	 */
	public String jinjaTemplateRenderTool;
	
	/**
	 * Full path to execute python commands (ie /usr/bin/python3)
	 */
	public String pythonExecPath;
	
	/**
	 * Local node path of the folder that is mounted on the traefik docker to share configurations
	 */
	public String traefikMountedVolume;
	
    /**
     * Get the full workspace path for this parameter
     *
     * @param oParameter Base Parameter
     * @return full workspace path
     */
    public static String getWorkspacePath(BaseParameter oParameter) {
        try {
            return getWorkspacePath(oParameter, getWasdiBasePath());
        } catch (Exception e) {
            e.printStackTrace();
            return getWorkspacePath(oParameter, "/data/wasdi");
        }
    }

    /**
     * Get the full workspace path for this parameter (with last / included)
     *
     * @param oParameter Base Parameter of the launcher operation
     * @param sRootPath Root path of WASDI node
     * @return full workspace path
     */
    public static String getWorkspacePath(BaseParameter oParameter, String sRootPath) {
        // Get Base Path
        String sWorkspacePath = sRootPath;

        if (!(sWorkspacePath.endsWith("/") || sWorkspacePath.endsWith("//")))
            sWorkspacePath += "/";

        String sUser = oParameter.getUserId();

        if (Utils.isNullOrEmpty(oParameter.getWorkspaceOwnerId()) == false) {
            sUser = oParameter.getWorkspaceOwnerId();
        }

        // Get Workspace path
        sWorkspacePath += sUser;
        sWorkspacePath += "/";
        sWorkspacePath += oParameter.getWorkspace();
        sWorkspacePath += "/";

        return sWorkspacePath;
    }
    
	/**
	 * Obtain the local workspace path from configuration (base path), user and worksapce id
	 * @param sUserId User Id
	 * @param sWorkspace Workspace
	 * @return full workspace local path with the ending / included
	 */
	public static String getWorkspacePath(String sUserId, String sWorkspace) {
		// Take path
		String sDownloadRootPath = getWasdiBasePath();
		String sPath = sDownloadRootPath + sUserId + File.separator + sWorkspace + File.separator;

		return sPath;
	}
	
	/**
	 * Get the processor folder by Processor
	 * @param oProcessor Processor Object
	 * @return the folder of the processor
	 */
	public static String getProcessorFolder(Processor oProcessor) {
		if (oProcessor == null) return null;
		return getProcessorFolder(oProcessor.getName());
	}

	/**
	 * Get the processor folder by processor name
	 * @param sProcessorName the name of the processor
	 * @return the folder of the processor
	 */
	public static String getProcessorFolder(String sProcessorName) {
		// Set the processor path
		String sDownloadRootPath = getWasdiBasePath();;
		String sProcessorFolder = sDownloadRootPath + "processors" + File.separator + sProcessorName + File.separator;
		return sProcessorFolder;
	}
	
	/**
	 * Get the base path of images in WASDI
	 * @return
	 */
	public static String getImagesBasePath() {
		String sWasdiBasePath = getWasdiBasePath();
		String sImagesBasePath = sWasdiBasePath + "images/";
		return sImagesBasePath;
	}
	
	/**
	 * Get the root path of download folder of WASDI 
	 * 
	 * @return base download local path with the ending / included
	 */
	public static String getWasdiBasePath() {
		// Take path
		String sWasdiBasePath = WasdiConfig.Current.paths.downloadRootPath;

		if (Utils.isNullOrEmpty(sWasdiBasePath)) {
			sWasdiBasePath = "/data/wasdi/";
		}

		if (!sWasdiBasePath.endsWith(File.separator)) {
			sWasdiBasePath = sWasdiBasePath + File.separator;
		}

		return sWasdiBasePath;
	}
	
	/**
	 * Returns the WASDI Base path for Workflows
	 * @return Workflows Path
	 */
	public static String getWorkflowsPath() {
        String sDownloadRootPath = getWasdiBasePath();
        String sDirectoryPathname = sDownloadRootPath + "workflows/";
        return sDirectoryPathname;
	}
	
	/**
	 * Returns the WASDI Base Path for Styles
	 * @return Styles Path
	 */
	public static String getStylesPath() {
        String sStylePath =getWasdiBasePath();
        sStylePath += "styles" + File.separator;
        return sStylePath;
	}
}
