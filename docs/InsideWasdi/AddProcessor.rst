.. _AddProcessor:

Add a New Processor Type to WASDI
=================================

Introduction
---------------------------
Each processor type is an option to deploy a new application in WASDI. Each new app must delcare the processor type.

Processor Types are mainly composed by:

* A Docker templalte folder: this is the folder where can be found the basic Dockerfile and other needed files to create a container with the user code
* A Processor Engine: each processor type has an associated Processor Engine. The processor engine has the main goal to implement these operations:
	* deploy: create an instance of the processor
	* redeploy + libraryUpdate: force the engine to generate an update of the app
	* run: execute an app
	* delete: delete the app
	* environmentUpdate: when supported, allow to manipulate the environment of the application (add remove update packages)
	* refreshPackagesInfo: when supported, must refresh the list of packages available in the workspace
* A dedicatated Configuration: all the processor have a common configuration in WasdiConfig->dockers->ProcessorTypes
	
Usually the source code and/or executables of the user are uploaded in WASDI and saved in a folder with the name of the new processor.


The docker template folder of the processor type is copied in the new processor folder.


The processor engine may manipulate the files (ie adding or editing some details) and then start the build of the Application.


Usually all the processor types are derived from the **DockerProcessorEngine** or the **DockerBuildOnceProcessorEngine** that are designed the create a container for each app and push in the WASDI Docker registry.

Add your New Processor Type
---------------------------

Declare the new Processor Type in 

.. code-block:: java

	wasdi.shared->business->processors->ProcessorTypes.java

The processor type name is used in the db to store the type of application.
Each processor type has also an associted folder that must be returned in the getTemplateFolder method of ProcessorTypes. 

The folder name can be the same of the type name, or can be different.

Create a new class in the Launcher Project in the namespace

.. code-block:: java

	wasdi.processors

derived from **WasdiProcessorEngine** or a subclass. 

Example: 

.. code-block:: java

	public class MeluxinaPipProcessorEngine extends DockerBuildOnceEngine {...}

The class must have a constructor that initializes the base class member variable 

.. code-block:: java

	m_sDockerTemplatePath

.. code-block:: java

	public class MeluxinaPipProcessorEngine extends DockerBuildOnceEngine {

		/**
		* Default constructor
		*/
		public MeluxinaPipProcessorEngine() {
			super();
			if (!m_sDockerTemplatePath.endsWith("/")) m_sDockerTemplatePath += "/";
			m_sDockerTemplatePath += ProcessorTypes.getTemplateFolder(ProcessorTypes.PYTHON_PIP_2);
			
			m_asDockerTemplatePackages = new String[8];
			m_asDockerTemplatePackages[0] = "flask";
			m_asDockerTemplatePackages[1] = "gunicorn";
			m_asDockerTemplatePackages[2] = "requests";
			m_asDockerTemplatePackages[3] = "numpy";
			m_asDockerTemplatePackages[4] = "wheel";
			m_asDockerTemplatePackages[5] = "wasdi";
			m_asDockerTemplatePackages[6] = "time";
			m_asDockerTemplatePackages[7] = "datetime";
		}
	}

It is also possible to declare the packages that are installed at build time, to avoid conflicts with the package manager.

To implement your processor engine, you may want to override one or more of the methods used to deploy and run the app.

To access the configuration you can use:

.. code-block:: java

	ProcessorTypeConfig oConfig = WasdiConfig.Current.dockers.getProcessorTypeConfig(ProcessorTypes.PYTHON_PIP_MELUXINA);

Add your processor in the Launcher class method:

.. code-block:: java

	wasdi.procesors.WasdiProcessorEngine.getProcessorEngine

Using your own Processor Type
-----------------------------

The processor Type shall be declared also on the client to allow users use it.

It is declared in the client file:

**app->components->edit->edit-toolbar->toolbar-dialogs->new-app-dialog->processor-tab-content->processor-tab-content.component.ts**

Varialbe m_aoProcessorTypes

You need to add here the same code you added to ProcessorTypes

