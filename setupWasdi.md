# WASDI installation


## download WASDI

clone the code from github:

`git clone https://github.com/fadeoutsoftware/WASDI.git`

## Configure ngnix

**TODO**

## Configure MongoDB

Create a user table:

```
$ mongo wasdi
db.createCollection("users")
db.users.insert({ "userId" : "myUserID", "name" : "myName", "password" : "myPassword", "surname" : "mySurname"})
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

Re-start the mongod instance with the --auth command line option: edit /etc/mongodb.conf with a text editor (e.g. `$ nano /etc/mongodb.conf`) and change the corresponding line to:

```
Set auth=true
```

Create wasdi collection user

```
$ mongo
db.auth("UserAdministrator", "myUserPassword")
use wasdi
db.createUser({ user:"***REMOVED***" ,   pwd: "myWasdiMongoPassword", roles: [{
role: "readWrite", db: "wasdi" }]})
```

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