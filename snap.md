
Make sure to fulfill the [prerequisites](./prerequisites.md) before continuing.

# SNAP

Instructions to download [SNAP](https://senbox.atlassian.net/wiki/spaces/SNAP/pages/10879039/How+to+build+SNAP+from+sources) and sentinel toolboxes 1-3 in a directory (assume it is called `snap`), and build them. In details:

create a directory and cd into it:

```
aPath/ $ mkdir snap
aPath/ $ cd snap
```

## Snap core packages

### Snap engine

```
aPath/snap/ $ git clone https://github.com/senbox-org/snap-engine
aPath/snap/ $ cd snap-engine
aPath/snap/snap-engine/ $ mvn clean install
```

### Snap desktop

```
aPath/snap/ $ git clone https://github.com/senbox-org/snap-desktop
aPath/snap/ $ cd snap-desktop
aPath/snap/snap-desktop/ $ mvn clean install
```

[back to Snap core packages](#snap-core-packages)


## Sentinel toolboxes

Clone the repositories of the toolboxes 1-3

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

Note: according to the developers, builds may fail because of unit tests failing (in fact, that happened consistently in our tests). If that is case, just replace the build above command with

```
aPath/snap/s#tbx/ $ mvn clean install -DskipTests=true
```

to skip the tests (the latter worked smoothly in our tests): you're just going to use the toolboxes, not contributing to them. Also, please report the incident on the appropriate ESA's forum: [s1tbx](https://forum.step.esa.int/c/s1tbx/problem-reports), [s2tbx](https://forum.step.esa.int/c/s2tbx/problem-reports) and [s3tbx](https://forum.step.esa.int/c/s3tbx/problem-reports)

[back to Sentinel toolboxes](#sentinel-toolboxes)

## PROBA-V toolbox

Finally, install the PROBA-V toolbox:

```
aPath/snap/ $ git clone https://github.com/senbox-org/probavbox.git
aPath/snap/ $ cd probavox
aPath/snap/probavox/ $ mvn clean install
```

[back to PROBA-V toolbox](#proba-v-toolbox)

----

You're done!
Proceed to [install and configure WASDI](./setupWasdi.md) or [go back](./README.md) for the process summary.