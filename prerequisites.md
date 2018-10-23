
# Prerequisites

## Java

Java JDK (not RE) version 1.8. To check the installed version:

$ java -version

Note for **Windows** users: make sure the install path does not contain spaces. This is required in order to avoid issues when [using Maven](https://maven.apache.org/guides/getting-started/windows-prerequisites.html). Example of an appropriate path for a Windows installation: `C:\path\to\Java\`

**TODO**
*check whether the no-space-issue is really an issue* / how to overcome it

## Native [Java Advanced Imaging API (JAI)](http://www.oracle.com/technetwork/java/javase/tech/jai-142803.html)

It's important to install the official Oracle JDK with [native JAI](https://geoserver.geo-solutions.it/edu/en/install_run/jai_io_install.html)

**TODO**
*Verify whether ImageIO is optional/useful/mandatory*

## NGINX (optional under Windows)

Download and install [NGINX](https://nginx.org/en/). Here are the instructions for [installing NGINX on Ubuntu](https://www.digitalocean.com/community/tutorials/how-to-install-nginx-on-ubuntu-16-04)

## Tomcat

- Download and Install [Tomcat](http://tomcat.apache.org/) **[version 8.5](https://tomcat.apache.org/download-80.cgi)**

## HDF5

**TODO**
*native HDF5*

## Websocket

Install [Websocketd](http://websocketd.com/) to use [websockets](https://www.html5rocks.com/en/tutorials/websockets/basics/)


## Wget (optional under Windows)

- Download and install [wget](https://www.gnu.org/software/wget/)

## SFTPmanager

**TODO**
*sftpmanager*


## MongoDB

- [Download](https://www.mongodb.com/download-center/community) and [install](https://docs.mongodb.com/manual/administration/install-community/) [MongoDB](https://www.mongodb.com/) (make sure to head for the download, not for a Database-as-a-Service solution)
- install a MongoDB client:
  - free:
    - (**suggested**) [robomongo](https://robomongo.org/) [robo 3T](https://robomongo.org/download) (lighter)
	- [MongoDB Compass](https://www.mongodb.com/download-center/compass), the official MongoDB administration GUI. *Note to **Windows** users: officially, it comes together with MongoDB (uncheck the flag during the installation process if you don't want it), however, at the time of writing, we could not [find it](https://stackoverflow.com/questions/47696519/windows-mongodb-installed-compass-but-cant-find-compass-within-system) after the installation and had to download and install it separately.*
  - paid:
    - [Studio 3T](https://studio3t.com/knowledge-base/articles/installation/) (complete mongoDB IDE, requires License)
    - [Navicat](https://www.navicat.com/en/products/navicat-for-mongodb)
   
## Rabbit MQ

- [install](https://www.erlang.org/downloads) the [Erlang](https://www.erlang.org/) programming language. Make sure to install it using an administrator account
- [download](https://www.rabbitmq.com/download.html) and [install](https://www.rabbitmq.com/download.html#installation-guides) [Rabbit MQ](https://www.rabbitmq.com/)

## Geoserver

- Download and install [GeoServer](http://geoserver.org/)

## Git

[Download](https://git-scm.com/downloads) and install [git](https://git-scm.com/)

## Maven

- [Download](https://maven.apache.org/download.html) and [install](http://maven.apache.org/install.html) Maven
  - *Optional* (yet suggested): [get familiar with Maven (in 5 minutes)](https://maven.apache.org/guides/getting-started/maven-in-five-minutes.html)

## Eclipse

- [Download](https://www.eclipse.org/downloads/) and install [Eclipse](https://www.eclipse.org/)
- Setup Eclipse for Maven using the [M2Eclipse plugin](http://www.eclipse.org/m2e/). Here's an [unofficial guide](http://www.vogella.com/tutorials/EclipseMaven/article.html)
- Install the [Eclipse Web Tools Platform SDK](https://www.eclipse.org/webtools/). Later, you will be able to configure Eclipse for working with Tomcat: here's a [unofficial (yet useful) guide](https://www.mulesoft.com/tcat/tomcat-eclipse) with an example
- *optional* Setup Eclipse for git using [egit](https://www.eclipse.org/egit/). [Unofficial guide](http://www.vogella.com/tutorials/EclipseGit/article.html)

----

You're done!
Proceed to [install snap software packages](./snap.md) or [go back](./README.md) for the process summary.