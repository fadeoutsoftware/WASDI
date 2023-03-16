# INTRODUCTION

Theme to customize the Keycloak's interface with our WASDI theme.

# ARBORESCENCE

## src/main/resources/META-INF

This directory contains the configuration of the theme itself.

## src/main/resources/theme/WASDI

The directory contains all the customized template files and static resources (.ftl).

# CREATE THE JAR

## Prerequisites

To have:
  - JDK 8
  - Maven >= 3.6.3

## Create the JAR

```
# mvn --batch-mode --define revision=1.0 --define skipTests --update-snapshots clean package
```

# DEPLOY THE THEME

Copy the theme in:

```
# cp target/keycloak-theme-wasdi-*.jar /path/to/keycloak/themes/.
```

Restart Keycloak:

```
# systemctl restart keycloak
```
