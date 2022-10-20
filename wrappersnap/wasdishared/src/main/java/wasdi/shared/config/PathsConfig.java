package wasdi.shared.config;

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
}
