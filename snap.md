#Snap

Instructions to download [SNAP](https://senbox.atlassian.net/wiki/spaces/SNAP/pages/10879039/How+to+build+SNAP+from+sources) and sentinel toolboxes 1-3 in a directory (assume it is called `snap`), and build them. In details:

create a directory and cd into it:

```
aPath/ $ mkdir snap
aPath/ $ cd snap
```


##SNAP engine

```
aPath/snap/ $ git clone https://github.com/senbox-org/snap-engine
aPath/snap/ $ cd snap-engine
aPath/snap/snap-engine/ $ mvn clean install
```

##SNAP desktop

```
aPath/snap/ $ git clone https://github.com/senbox-org/snap-desktop
aPath/snap/ $ cd snap-desktop
aPath/snap/snap-desktop/ $ mvn clean install
```

##toolboxes

clone the repositories of the toolboxes 1-3

```
aPath/snap/ $ git clone https://github.com/senbox-org/s1tbx.git
aPath/snap/ $ git clone https://github.com/senbox-org/s2tbx.git
aPath/snap/ $ git clone https://github.com/senbox-org/s3tbx.git
```

Then build the projects in the same order. The general procedure is:

```
aPath/snap/ $ cd s#tbx
aPath/snap/s#tbx/ $ mvn clean install
```
Just make sure to replace the character `#` above with the appropriate digit, i.e, `1`, `2`, or `3`

According to the developers, builds may fail because of unit tests failing (that happened consistently in our tests). If that is case, replace the build above command with

```
aPath/snap/s#tbx/ $ mvn clean install -DskipTests=true
```

to skip the tests (the latter worked smoothly in our tests). Also, please report the incident on the appropriate ESA's forum: [s1tbx](https://forum.step.esa.int/c/s1tbx/problem-reports), [s2tbx](https://forum.step.esa.int/c/s2tbx/problem-reports) and [s3tbx](https://forum.step.esa.int/c/s3tbx/problem-reports)


##PROBA-V toolbox


```
aPath/snap/ $ git clone https://github.com/senbox-org/probavbox.git
aPath/snap/ $ cd probavox
aPath/snap/probavox/ $ mvn clean install
```

You're done!