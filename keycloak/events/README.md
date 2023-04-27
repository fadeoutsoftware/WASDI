# INTRODUCTION

The project is used to generate a JAR and deploy it on a Keycloak running instance.
This, to perform some custom operations, in response to specific events.

# CREATE THE JAR

## Prerequisites

To have:
  - JDK 17
  - Maven >= 3.9.0

## Create the JAR

Enter in the right directory:

```
# cd /path/to/the/clone/of/the/WASDI/git/repository
# cd keycloak/events
```

Build with Maven:

```
# mvn --batch-mode --define revision=1.0 --define skipTests --update-snapshots clean package
```

# DEPLOY THE PROVIDER

Copy the theme in:

```
# cp target/keycloak-event-listener-wasdi-*.jar /path/to/keycloak/providers/.
```

Restart Keycloak:

```
# systemctl restart keycloak
```

Connect to Keycloak and enable the event listener in: <your realm> > Realm settings > Events
