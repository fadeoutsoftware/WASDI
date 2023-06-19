#!/bin/bash

# find and delete snap temporary files accessed last time more than 5 minutes ago
if [[ -d "{{ sWasdiDataTemporarySnapTomcatRootDirectoryPath }}" ]]
then
    find {{ sWasdiDataTemporarySnapTomcatRootDirectoryPath }}/ -amin +5 -exec rm --recursive {} \; 2>/dev/null
fi

# then immediately repair dirs...
mkdir --parents {{ sWasdiDataTemporarySnapTomcatRootDirectoryPath }}
chown --recursive {{ sWasdiDataTemporarySnapTomcatRootDirectoryOwner }}:{{ sWasdiDataTemporarySnapTomcatRootDirectoryGroup }} {{ sWasdiDataTemporarySnapTomcatRootDirectoryPath }}
chmod {{ sWasdiDataTemporarySnapCacheRootDirectoryMode }} {{ sWasdiDataTemporarySnapTomcatRootDirectoryPath }}

# check link from /tmp exists and recreate if necessary
if [[ ! -h "/tmp/snap-{{ sWasdiSystemUserName }}" ]]
then
    ln --symbolic {{ sWasdiDataTemporarySnapTomcatRootDirectoryPath }} /tmp/snap-{{ sWasdiSystemUserName }}
fi

# change owner and group of the symbolic link
chown --no-dereference {{ sWasdiDataTemporarySnapTomcatRootDirectoryOwner }}:{{ sWasdiDataTemporarySnapTomcatRootDirectoryGroup }} /tmp/snap-{{ sWasdiSystemUserName }}

# find and delete snap cache files accessed last time more than 5 minutes ago
if [[ -d "{{ sWasdiDataTemporarySnapCacheRootDirectoryPath }}" ]]
then
    find {{ sWasdiDataTemporarySnapCacheRootDirectoryPath }}/ -amin +5 -exec rm --recursive {} \; 2>/dev/null
fi

# then immediately repair dirs...
mkdir --parents {{ sWasdiDataTemporarySnapCacheRootDirectoryPath }}
chown --recursive {{ sWasdiDataTemporarySnapCacheRootDirectoryOwner }}:{{ sWasdiDataTemporarySnapCacheRootDirectoryGroup }} {{ sWasdiDataTemporarySnapCacheRootDirectoryPath }}
chmod {{ sWasdiDataTemporarySnapCacheRootDirectoryMode }} {{ sWasdiDataTemporarySnapCacheRootDirectoryPath }}

# check link from /tmp/snap-cache exists and recreate if necessary
if [[ ! -h "/tmp/snap-cache" ]]
then
    ln --symbolic {{ sWasdiDataTemporarySnapCacheRootDirectoryPath }} /tmp/snap-cache
fi

# change owner and group of the symbolic link
chown --no-dereference {{ sWasdiDataTemporarySnapCacheRootDirectoryOwner }}:{{ sWasdiDataTemporarySnapCacheRootDirectoryGroup }} /tmp/snap-cache

# check link from {{ sWasdiSystemUserHome }}/.snap/var/cache exists and recreate if necessary
if [[ ! -h "{{ sWasdiSystemUserHome }}/.snap/var/cache" ]]
then
    ln --symbolic {{ sWasdiDataTemporarySnapCacheRootDirectoryPath }} {{ sWasdiSystemUserHome }}/.snap/var/cache
fi

# change owner and group of the symbolic link
chown --no-dereference {{ sWasdiDataTemporarySnapCacheRootDirectoryOwner }}:{{ sWasdiDataTemporarySnapCacheRootDirectoryGroup }} {{ sWasdiSystemUserHome }}/.snap/var/cache
