# WASDI installation


## Download WASDI

clone the code from github:

`git clone https://github.com/fadeoutsoftware/WASDI.git`

## Configure ngnix

**TODO**

## Configure MongoDB

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

## Rabbit

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

----

> **Note:** from here on it's pretty raw, and oriented mostly to a linux server
> **TODO** refine for windows

----


## Tomcat folders

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

https://stackoverflow.com/questions/10076754/how-does-tomcat-locate-the-webapps-directory


## SFTP / websocketd

```
$ cd /usr/lib/wasdi/sftpmanager
$ screen ./websocketd --port=6703 ./service.sh
```

## NGINX

Configure nginx in `/etc/nginx/sites-available/default`

## Permissions

verify that the user can write in tomcat folders

create folders:

```
.snap
/nfs/download
```

## GeoServer

Unpack geoserver
move the .war file into tomcat

Change geoserver password: enter with admin geoserver and set the new password
Will be used in launcher config
Log into geoserver
Change admin passsword
Setup log in production
[Activate wps](http://docs.geoserver.org/latest/en/user/services/wps/install.html#configuring-wps)

(Checked with version 2.10.1)

##HDF5

Install the native HDF5 libraries
https://wiki-bsse.ethz.ch/display/JHDF5/Download+Page
https://wiki-bsse.ethz.ch/display/JHDF5/Documentation+page

more info:

https://stackoverflow.com/questions/9227099/hdf5-in-java-what-are-the-difference-between-the-availabe-apis
https://portal.hdfgroup.org/display/support

include the library into `catalina.sh`

## Tomcat configuration

```
$ locate catalina.sh
$ touch setenv.sh
$ nano setenv.sh
$ export CATALINA_OPTS="$CATALINA_OPTS -Xms4G"
$ export CATALINA_OPTS="$CATALINA_OPTS -Xmx16G"
```

Then restart it.

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

----

# Setup


## Prerequisites setup

** TODO: **

- webserver/tomcat setup
- SFTP setup
- GeoServer setup
- configure r/w permissions on files and folders


## Build


** TODO: **

- eclipse maven project setup
- how to build
- how to setup users and passwords

## Deploy

** TODO **

----

You're done!

[./README.md]