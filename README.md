# WASDI

Web Advanced Space Developer Interface

----

## Prerequisites

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

### snap

Install [snap](./snap.md)

----

## Build

clone the repo and build the projects:
```
git clone https://github.com/fadeoutsoftware/WASDI.git
cd WASDI\wrappersnap\
cd wasdishared
mvn clean install
```

**TODO** *import into eclipse*
**TODO** *build wrappersnap\launcher*
**TODO** *build wasdiwebserver*



