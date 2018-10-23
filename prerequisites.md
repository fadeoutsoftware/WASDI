
> **Caveat:** still pretty raw, and oriented mostly to a linux server

**TODO** *refine for windows*

----


# Prerequisites installation, and setup for WASDI

## Java

Java JDK (not RE) version 1.8. To check the installed version:

$ java -version

Note for **Windows** users: make sure the install path does not contain spaces. This is required in order to avoid issues when [using Maven](https://maven.apache.org/guides/getting-started/windows-prerequisites.html). Example of an appropriate path for a Windows installation: `C:\path\to\Java\`

**TODO**
*check whether the no-space-issue is a real issue and how to overcome it (maybe w/ a shortcut? a softlink from the git bash?)*

### Native [Java Advanced Imaging API (JAI)](http://www.oracle.com/technetwork/java/javase/tech/jai-142803.html)

It's important to install the official Oracle JDK with [native JAI](https://geoserver.geo-solutions.it/edu/en/install_run/jai_io_install.html)

**TODO**
*Verify whether ImageIO is optional/useful/mandatory*

[back to Java](#java)

## MongoDB

- [Download](https://www.mongodb.com/download-center/community) and [install](https://docs.mongodb.com/manual/administration/install-community/) [MongoDB](https://www.mongodb.com/) (make sure to head for the download, not for a Database-as-a-Service solution)
- install a MongoDB client:
  - free:
    - (**suggested**) [robomongo](https://robomongo.org/) [robo 3T](https://robomongo.org/download) (lighter)
	- [MongoDB Compass](https://www.mongodb.com/download-center/compass), the official MongoDB administration GUI. *Note to **Windows** users: officially, it comes together with MongoDB (uncheck the flag during the installation process if you don't want it), however, at the time of writing, we could not [find it](https://stackoverflow.com/questions/47696519/windows-mongodb-installed-compass-but-cant-find-compass-within-system) after the installation and had to download and install it separately.*
  - paid:
    - [Studio 3T](https://studio3t.com/knowledge-base/articles/installation/) (complete mongoDB IDE, requires License)
    - [Navicat](https://www.navicat.com/en/products/navicat-for-mongodb)
	
### Configure MongoDB

**TODO** *check again and refine*

Create database `wasdi`

```
$ mongo wasdi
```

From the mongo prompt, create a user table:

```
db.createCollection("users")
db.users.insert(
  {
    "userId" : "myUserID",
	"name" : "myName",
	"password" : "myPassword",
	"surname" : "mySurname"
  }
)
```

Then, create Mongo admin account

```
use admin
db.createUser(
  {
    user: "UserAdministrator",
    pwd: "myUserPassword",
    roles: [ { role: "userAdminAnyDatabase", db: "admin" } ]
  }
)
```

Stop the service and [enable authentication](https://docs.mongodb.com/manual/tutorial/enable-authentication/). You can either:

- use the [configuration file](https://docs.mongodb.com/manual/administration/configuration/) (strongly suggested option): uncomment `security:` and add `authorization: enabled`, then restart the service. OS dependent details:
  - on linux: you will need root privileges. The file is usually `/etc/mongodb.conf` (**TODO** *check this one*)  (or auth=true if you don't use the YAML format - that depends on your installation).
  - on Windows: you will need administration privileges. [Edit `mongod.cfg`](https://docs.mongodb.com/manual/reference/configuration-options/#security.authorization) (usually in `C:\Program Files\MongoDB\Server\4.0\bin\`)
- pass the --auth command line option when launched

Create wasdi collection user

```
$ mongo
db.auth("UserAdministrator", "myUserPassword")
use wasdi
db.createUser(
  {
    user:"***REMOVED***" ,
	pwd: "myWasdiMongoPassword",
	roles: [ { role: "readWrite", db: "wasdi" } ]
	]
  }
)
```

[back to MongoDB](#mongodb)

## NGINX

**TODO** *check: optional under Windows?*

Download and install [NGINX](https://nginx.org/en/). Here are the instructions for [installing NGINX on Ubuntu](https://www.digitalocean.com/community/tutorials/how-to-install-nginx-on-ubuntu-16-04)

Configure nginx in `/etc/nginx/sites-available/default`

[back to NGINX](#NGINX)

## Tomcat

- Download and Install [Tomcat](http://tomcat.apache.org/) **[version 8.5](https://tomcat.apache.org/download-80.cgi)**

### Configure Tomcat

**TODO** *check and refine*

#### Tomcat folders and permissions

Create the following Tomcat folders:

```
/usr/lib/wasdi/launcher
/usr/lib/wasdi/params
/usr/lib/wasdi/sftpmanager
/data/wasdi
/data/wasdi/metadata
/data/wasdi/wps
/data/wasdi/catalogue
/data/wasdi/dockertemplate
/data/wasdi/processors
```

verify that the user can write in tomcat folders

create folders:

```
.snap
/nfs/download
```

https://stackoverflow.com/questions/10076754/how-does-tomcat-locate-the-webapps-directory

#### Tomcat configuration

```
$ locate catalina.sh
$ touch setenv.sh
$ nano setenv.sh
$ export CATALINA_OPTS="$CATALINA_OPTS -Xms4G"
$ export CATALINA_OPTS="$CATALINA_OPTS -Xmx16G"
```

Then restart it.

#### Tomcat RAM and parameters

```
$ cd /etc/default
Nano tomcat8
JAVA_OPTS="-Djava.awt.headless=true -Xmx10g -XX:+UseConcMarkSweepGC -XX:+AggressiveOpts -Xverify:none -Dsun.java2d.noddraw=true -Dsun.awt.nopixfmt=true -Dsun.java2d.dpiaware=false -Dsnap.jai.tileCacheSize=4096 -Djava.library-path=/opt/gdal"
```

Note: check Java command line for the Launcher
Note: check RAM available to catalina

http://tomcat.apache.org/tomcat-5.5-doc/config/realm.html
http://tomcat.apache.org/tomcat-8.0-doc/realm-howto.html

Launcher configuration: `config.properties`

User's home must be tomcat'home. On Ubuntu:  `/user/share/tomcat`

[back to Tomcat](#tomcat)

## HDF5

Install the native HDF5 libraries
https://wiki-bsse.ethz.ch/display/JHDF5/Download+Page
https://wiki-bsse.ethz.ch/display/JHDF5/Documentation+page

more info:

https://stackoverflow.com/questions/9227099/hdf5-in-java-what-are-the-difference-between-the-availabe-apis
https://portal.hdfgroup.org/display/support

include the library into `catalina.sh`

[back to HDF5](#HDF5)

## Wget, SFTPmanager and Websocket

**TODO** *check under Windows*

- install [wget](https://www.gnu.org/software/wget/) 
- install [Websocketd](http://websocketd.com/) to use [websockets](https://www.html5rocks.com/en/tutorials/websockets/basics/)

```
$ cd /usr/lib/wasdi/sftpmanager
$ screen ./websocketd --port=6703 ./service.sh
```

[back to](#wget,-SFTPmanager-and-Websocket)
   
## Rabbit MQ

- [install](https://www.erlang.org/downloads) the [Erlang](https://www.erlang.org/) programming language. Make sure to install it using an administrator account
- [download](https://www.rabbitmq.com/download.html) and [install](https://www.rabbitmq.com/download.html#installation-guides) [Rabbit MQ](https://www.rabbitmq.com/)

### Configure Rabbit

**TODO** *check and refine*

[install the web STOMP plugin:](https://www.rabbitmq.com/web-stomp.html)

```
rabbitmq-plugins enable rabbitmq_web_stomp
```

Create rabbit user

```
rabbitmqctl add_user rabbitUser rabbitUser
rabbitmq-plugins enable rabbitmq_management
```

Create rabbit user admin

```
rabbitmqctl add_user admin password
rabbitmqctl set_user_tags admin administrator
```

Access [rabbit management](https://www.rabbitmq.com/management.html)
Delete user `guest`
Enable user `rabbitUser rabbitUser'

[back to Rabbit MQ](#rabbit-MQ)

## Geoserver

- Download and install [GeoServer](http://geoserver.org/)

### Configure GeoServer

Unpack geoserver
move the .war file into tomcat

Change geoserver password: enter with admin geoserver and set the new password
Will be used in launcher config
Log into geoserver
Change admin passsword
Setup logging in production
[Activate wps](http://docs.geoserver.org/latest/en/user/services/wps/install.html#configuring-wps)

(Checked with version 2.10.1)

[back to GeoServer](#geoserver)

##Development tools

### Git

[Download](https://git-scm.com/downloads) and install [git](https://git-scm.com/)

### Maven

- [Download](https://maven.apache.org/download.html) and [install](http://maven.apache.org/install.html) Maven
  - *Optional* (yet suggested): [get familiar with Maven (in 5 minutes)](https://maven.apache.org/guides/getting-started/maven-in-five-minutes.html)

### Eclipse

- [Download](https://www.eclipse.org/downloads/) and install [Eclipse](https://www.eclipse.org/)
- Setup Eclipse for Maven using the [M2Eclipse plugin](http://www.eclipse.org/m2e/). Here's an [unofficial guide](http://www.vogella.com/tutorials/EclipseMaven/article.html)
- Install the [Eclipse Web Tools Platform SDK](https://www.eclipse.org/webtools/). Later, you will be able to configure Eclipse for working with Tomcat: here's a [unofficial (yet useful) guide](https://www.mulesoft.com/tcat/tomcat-eclipse) with an example
- *optional* Setup Eclipse for git using [egit](https://www.eclipse.org/egit/). [Unofficial guide](http://www.vogella.com/tutorials/EclipseGit/article.html)

[back to Development tools](#development-tools)

----

You're done!
Proceed to [install snap software packages](./snap.md) or [go back](./README.md) for the process summary.