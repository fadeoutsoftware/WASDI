# WASDI

Web Advanced Space Developer Interface

----
[![Documentation Status](https://readthedocs.org/projects/wasdi/badge/?version=latest)](https://wasdi.readthedocs.io/en/latest/?badge=latest)
## Develop

### Prerequisites

#### Git

[Download](https://git-scm.com/downloads) and install [git](https://git-scm.com/)

#### Java

Install [Java SE Development kit 8](https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)

#### Maven

- [Download](https://maven.apache.org/download.html) and [install](http://maven.apache.org/install.html) Maven
  - *Optional* (yet suggested): [get familiar with Maven (in 5 minutes)](https://maven.apache.org/guides/getting-started/maven-in-five-minutes.html)

#### Tomcat

- Download and Install [Tomcat](http://tomcat.apache.org/) **[version 8.5](https://tomcat.apache.org/download-80.cgi)**

  
#### Eclipse

- [Download](https://www.eclipse.org/downloads/) and install [Eclipse](https://www.eclipse.org/). Choose the Eclipse IDE for **Java EE** Developers.
- Setup Eclipse for Maven using the [M2Eclipse plugin](http://www.eclipse.org/m2e/). Here's an [unofficial guide](http://www.vogella.com/tutorials/EclipseMaven/article.html)
- Install the [Eclipse Web Tools Platform SDK](https://www.eclipse.org/webtools/). Later, you will be able to configure Eclipse for working with Tomcat: here's a [unofficial (yet useful) guide](https://www.mulesoft.com/tcat/tomcat-eclipse) with an example
- *optional* Setup Eclipse for git using [egit](https://www.eclipse.org/egit/). [Unofficial guide](http://www.vogella.com/tutorials/EclipseGit/article.html)

#### Mongo DB

WASDI relies on mongo DB. Here you are two possibilities:

1. connect the local version of WASDI to an existing DB server. In this case you would just need a client (suggested: [robo3t](https://robomongo.org/download)) to perform ordinary maintenance
1. install a full fledged MongoDB **TODO** *how to configure Mongo DB*

#### snap

Install [snap](./snap.md)

----

### configure your setup for working with the project

clone the repo:

```
git clone https://github.com/fadeoutsoftware/WASDI.git
```

Then you can build the project.

#### Build with Maven:

```
cd WASDI
cd wrappersnap
cd wasdishared
mvn clean install
cd ../launcher
mvn clean install
cd ../../wasdiwebserver
mvn clean install
```

#### Build with Eclipse:

1. import wrappersnap\wasdishared as a maven project
1. import wrappersnap\launcher as a maven project
1. import wasdiwebserver as a maven project

**TODO** *how to configure* - attach images of the config

make sure Elicpse uses the jre within the jdk and not another one installed separately: Windows -> Preferences -> Java -> installed JREs
 
----

## Deploy

- **TODO** how to deploy
