package wasdi.processors;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

import wasdi.ConfigReader;
import wasdi.LauncherMain;
import wasdi.shared.business.Processor;
import wasdi.shared.utils.Utils;

/**
 * Wrap main docker functionalities
 *
 * @author p.campanella
 */
public class DockerUtils {

    /**
     * Wasdi Processor
     */
    Processor m_oProcessor;

    /**
     * Folder of the processor
     */
    String m_sProcessorFolder;

    /**
     * Wasdi Working Path
     */
    String m_sWorkingRootPath;

    /**
     * Log file for docker operations
     */
    String m_sDockerLogFile = "/usr/lib/wasdi/launcher/logs/dockers.log";

    /**
     * User that run the docker
     */
    String m_sUser = "tomcat8";

    /**
     * Create a new instance
     *
     * @param oProcessor       Processor
     * @param sProcessorFolder Processor Folder
     * @param sWorkingRootPath WASDI Working path
     */
    public DockerUtils(Processor oProcessor, String sProcessorFolder, String sWorkingRootPath, String sTomcatUser) {
        m_oProcessor = oProcessor;
        m_sProcessorFolder = sProcessorFolder;
        m_sWorkingRootPath = sWorkingRootPath;
        m_sUser = sTomcatUser;
    }

    /**
     * Deploy a docker
     */
    public boolean deploy() {

        try {

            // Generate Docker Name
            String sProcessorName = m_oProcessor.getName();

            String sDockerName = "wasdi/" + sProcessorName + ":" + m_oProcessor.getVersion();

            // Initialize Args
            ArrayList<String> asArgs = new ArrayList<>();

            // Generate shell script file
            String sBuildScriptFile = m_sProcessorFolder + "deploywasdidocker.sh";

            File oBuildScriptFile = new File(sBuildScriptFile);

            try (BufferedWriter oBuildScriptWriter = new BufferedWriter(new FileWriter(oBuildScriptFile))) {
                // Fill the script file
                if (oBuildScriptWriter != null) {
                    LauncherMain.s_oLogger.debug("DockerProcessorEngine.deploy: Creating " + sBuildScriptFile + " file");

                    oBuildScriptWriter.write("#!/bin/bash");
                    oBuildScriptWriter.newLine();
                    oBuildScriptWriter.write("echo Deploy Docker Started >> " + m_sDockerLogFile);
                    oBuildScriptWriter.newLine();
                    oBuildScriptWriter.write("docker build -t" + sDockerName + " " + m_sProcessorFolder + " --build-arg USR_NAME=" + m_sUser + " --build-arg USR_ID=$(id -u " + m_sUser + ")" +
                            " --build-arg GRP_NAME=" + m_sUser + " --build-arg GRP_ID=$(id -g " + m_sUser + ")" +
                            " $1 >> " + m_sDockerLogFile + " 2>&1");
                    oBuildScriptWriter.newLine();
                    oBuildScriptWriter.write("echo Deploy Docker Done >> " + m_sDockerLogFile);
                    oBuildScriptWriter.flush();
                    oBuildScriptWriter.close();
                }
            }

            // Wait a little bit to let the file be written
            Thread.sleep(2000);

            // Make it executable
            Runtime.getRuntime().exec("chmod u+x " + sBuildScriptFile);

            // And wait a little bit to make the chmod done
            Thread.sleep(2000);

            // Run the script
            WasdiProcessorEngine.shellExec(sBuildScriptFile, asArgs);

            LauncherMain.s_oLogger.debug("DockerUtils.deploy: created image " + sDockerName);
        } catch (Exception oEx) {
            Utils.debugLog("DockerUtils.deploy: " + oEx.toString());
            return false;
        }

        return true;
    }

    /**
     * Run the docker
     */
    public boolean run() {
        return run(m_oProcessor.getPort());
    }

    /**
     * Run the docker at the specified port
     *
     * @param iProcessorPort Port to use
     */
    public boolean run(int iProcessorPort) {

        try {
            // Get the docker name
            String sDockerName = "wasdi/" + m_oProcessor.getName() + ":" + m_oProcessor.getVersion();
            String sCommand = "docker";

            // Initialize Args
            ArrayList<String> asArgs = new ArrayList<>();

            // Processor Script File
            String sRunFile = m_sProcessorFolder + "runwasdidocker.sh";
            File oRunFile = new File(sRunFile);

            // Check if it is already done:

            if (!oRunFile.exists() || oRunFile.length() == 0L) {

                // Command
                asArgs.add(sCommand);

                // Action
                asArgs.add("run");

                // User
                asArgs.add("-u$(id -u " + m_sUser + "):$(id -g " + m_sUser + ")");

                // Working Path
                asArgs.add("-v" + m_sWorkingRootPath + ":/data/wasdi");

                // Processor folder
                asArgs.add("--mount");
                asArgs.add("type=bind,src=" + m_sProcessorFolder + ",dst=/wasdi");

                // Port
                asArgs.add("-p127.0.0.1:" + iProcessorPort + ":5000");

                // Extra host mapping, useful for some instances when the server host can't be resolved
                // The symptoms of such problem is that the POST call from the Docker container timeouts
                String sExtra_Host = ConfigReader.getPropValue("EXTRA_HOST");
                if (!Utils.isNullOrEmpty(sExtra_Host)) {
                    LauncherMain.s_oLogger.debug("DockerUtils.run Found extra host in configuration file");
                    LauncherMain.s_oLogger.debug("DockerUtils.run adding host mapping to the run arguments");
                    asArgs.add("--add-host=" + sExtra_Host);
                }


                // Docker name
                asArgs.add(sDockerName);

                // Generate the command line
                String sCommandLine = "";


                for (String sArg : asArgs) {
                    sCommandLine += sArg + " ";
                }

                LauncherMain.s_oLogger.debug("DockerUtils.run CommandLine: " + sCommandLine);

                try (BufferedWriter oRunWriter = new BufferedWriter(new FileWriter(oRunFile))) {
                    if (null != oRunWriter) {
                        LauncherMain.s_oLogger.debug("DockerUtils.run: Creating " + sRunFile + " file");

                        oRunWriter.write("#!/bin/bash");
                        oRunWriter.newLine();
                        oRunWriter.write("echo Run Docker Started >> " + m_sDockerLogFile);
                        oRunWriter.newLine();
                        oRunWriter.write(sCommandLine + " $1 >> " + m_sDockerLogFile + " 2>&1");
                        oRunWriter.newLine();
                        oRunWriter.write("echo Run Docker Done >> " + m_sDockerLogFile);
                        oRunWriter.flush();
                        oRunWriter.close();
                    }
                }

                Runtime.getRuntime().exec("chmod u+x " + sRunFile);

                asArgs.clear();
            }

            try {
                if (!oRunFile.canExecute()) {
                    Runtime.getRuntime().exec("chmod u+x " + sRunFile);
                }
            } catch (Exception oInnerEx) {
                Runtime.getRuntime().exec("chmod u+x " + sRunFile);
            }

            // Execute the command to start the docker
            WasdiProcessorEngine.shellExec(sRunFile, asArgs, false);

            LauncherMain.s_oLogger.debug("DockerUtils.run " + sDockerName + " started");
        } catch (Exception oEx) {
            Utils.debugLog("DockerUtils.run: " + oEx.toString());
            return false;
        }

        return true;
    }

    /**
     * Delete using Processor member variable
     *
     * @return
     */
    public boolean delete() {
        if (m_oProcessor != null) {
            return delete(m_oProcessor.getName(), m_oProcessor.getVersion());
        } else return false;
    }

    /**
     * Delete using processor name and default "1" version
     *
     * @param sProcessorName
     * @return
     */
    public boolean delete(String sProcessorName) {
        return delete(sProcessorName, "1");
    }

    /**
     * Delete using processor name and processor version
     *
     * @param sProcessorName
     * @param sVersion
     * @return
     */
    public boolean delete(String sProcessorName, String sVersion) {

        try {

            // docker ps -a | awk '{ print $1,$2 }' | grep <imagename> | awk '{print $1 }' | xargs -I {} docker rm -f {}
            // docker rmi -f <imagename>

            String sDockerName = "wasdi/" + sProcessorName + ":" + sVersion;

            String sDeleteScriptFile = m_sProcessorFolder + "cleanwasdidocker.sh";
            File oDeleteScriptFile = new File(sDeleteScriptFile);

            if (!oDeleteScriptFile.exists() || oDeleteScriptFile.length() == 0L) {
                try (BufferedWriter oDeleteScriptWriter = new BufferedWriter(new FileWriter(oDeleteScriptFile))) {
                    if (oDeleteScriptWriter != null) {
                        LauncherMain.s_oLogger.debug("DockerUtils.delete: Creating " + sDeleteScriptFile + " file");

                        oDeleteScriptWriter.write("#!/bin/bash");
                        oDeleteScriptWriter.newLine();
                        oDeleteScriptWriter.write("docker ps -a | awk '{ print $1,$2 }' | grep " + sDockerName + " | awk '{print $1 }' | xargs -I {} docker rm -f {}");
                        oDeleteScriptWriter.newLine();
                        oDeleteScriptWriter.flush();
                        oDeleteScriptWriter.close();
                    }
                }

                Runtime.getRuntime().exec("chmod u+x " + sDeleteScriptFile);
            } else {
                Runtime.getRuntime().exec("chmod u+x " + sDeleteScriptFile);
            }


            Runtime.getRuntime().exec(sDeleteScriptFile);

            // Wait for docker to finish
            Thread.sleep(15000);

            // Delete this image
            ArrayList<String> asArgs = new ArrayList<>();
            // Remove the container image
            asArgs.add("rmi");
            asArgs.add("-f");
            asArgs.add(sDockerName);

            String sCommand = "docker";

            WasdiProcessorEngine.shellExec(sCommand, asArgs, false);

            // Wait for docker to finish
            Thread.sleep(15000);
        } catch (Exception oEx) {
            Utils.debugLog("DockerUtils.delete: " + oEx.toString());
            return false;
        }

        return true;
    }
}
