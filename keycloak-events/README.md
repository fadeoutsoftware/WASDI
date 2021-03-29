#KeycloakSpiEvents
###General info
The project is used to generate a JAR and deploy it on a Keycloak running instance.
 This, to perform some custom operations, in response to specific events.
###Deploy
 To deploy the specific event listener, create the JAR by: 
 ```
mvn install
 ```
(it is a good practice to use the commnand clean beforehand)
```
mvn clean
 ``` 
The generated JAR can be found in the directory
```
{PROJECT_ROOT}/target/sample-event-listener.jar
 ``` 
Copy the JAR in the following directory of the standalone keycloak running instance:
```
{KEYCLOAK_ROOT}/standalone/deployments
 ``` 
The deployment is fired when the jar is uploaded.


### Checks of the installation 
To checks the installation, open the log:
```
tail -f {KEYCLOAK_ROOT}/standalone/log/server.log
 ```  
and look out or the following lines:
```
 INFO  [org.keycloak.subsystem.server.extension.KeycloakProviderDeploymentProcessor] (MSC service thread 1-3) Deploying Keycloak provider: sample-event-listener.jar

 INFO  [org.jboss.as.server] (DeploymentScanner-threads - 2) WFLYSRV0010: Deployed "sample-event-listener.jar" (runtime-name : "sample-event-listener.jar")

 ```  
well you're good to go!Try to trigger some events, and checks the results.
The projects comes with minor loggin capabilities.