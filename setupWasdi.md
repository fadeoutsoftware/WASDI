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

From within mongo, create a user table:

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

Create Mongo admin account

```
$ mongo
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

- use the [configuration file](https://docs.mongodb.com/manual/administration/configuration/):
  - on linux: you will need root privileges. The file is usually `/etc/mongodb.conf` (**TODO** *check this one*) and set `security.authorization: enabled` (or auth=true if you don't use the YAML format - that depends on your installation).
  - on Windows: you will need administration privileges. [Edit `mongod.cfg`](https://docs.mongodb.com/manual/reference/configuration-options/#security.authorization) (usually in `C:\Program Files\MongoDB\Server\4.0\bin\` and add `security.authorization: enabled`. 
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

----

# Setup


## Prerequisites setup

** TODO: **

- mongo setup
- rabbit setup
- webserver/tomcat setup
- SFTP setup
- GeoServer setup
- configure file r/w permissions


## Build


** TODO: **

- eclipse maven project setup
- how to build
- how to setup users and passwords

## Deploy

** TODO **