FROM ubuntu:22.04


MAINTAINER "WASDI SARL"


# From now, all commands executed are executed with the user 'root'
USER root


# This environment variable is setted to avoid packages to ask a confirmation
ENV DEBIAN_FRONTEND="noninteractive"

# This variable is setted to make Python stop creating files '.pyc'
ENV PYTHONDONTWRITEBYTECODE="False"

# This variable is setted to force the 'print' method of Python to flush message directly
ENV PYTHONUNBUFFERED="True"

# This variable is setted to use binaries from our virtual environment
ENV PATH="{{ sWasdiSystemUserHome }}/venv/bin:${PATH}"


# RUN generic to create user, group, directories, etc
RUN groupadd --gid {{ sWasdiSystemGroupId }} --non-unique {{ sWasdiSystemGroupName }} && \
    useradd --create-home --home-dir {{ sWasdiSystemUserHome }} --uid {{ sWasdiSystemUserId }} --gid {{ sWasdiSystemGroupId }} --non-unique --shell /bin/bash {{ sWasdiSystemUserName }} && \
    mkdir --parents {{ sWasdiSoftwareRootDirectoryPath }} && \
    chown {{ sWasdiSoftwareRootDirectoryOwner }}:{{ sWasdiSoftwareRootDirectoryGroup }} {{ sWasdiSoftwareRootDirectoryPath }} && \
    chmod {{ sWasdiSoftwareRootDirectoryMode }} {{ sWasdiSoftwareRootDirectoryPath }}


# RUN generic to update the system and install common packages
RUN echo "APT::Install-Suggests \"0\";" > /etc/apt/apt.conf.d/99local && \
    echo "APT::Install-Recommends \"0\";" >> /etc/apt/apt.conf.d/99local && \
    echo "APT::Get::Assume-Yes \"true\";" >> /etc/apt/apt.conf.d/99local && \
    echo "APT::Keep-Downloaded-Packages \"false\";" >> /etc/apt/apt.conf.d/99local && \
    echo "Dpkg::Options::--force-confold;" >> /etc/apt/apt.conf.d/99local && \
    apt-get update && \
    apt-get install \
        ca-certificates && \
    echo "deb https://mirror.ubuntu.ikoula.com/ jammy main restricted universe multiverse"          > /etc/apt/sources.list && \
    echo "deb https://mirror.ubuntu.ikoula.com/ jammy-updates main restricted universe multiverse"  >> /etc/apt/sources.list && \
    echo "deb https://mirror.ubuntu.ikoula.com/ jammy-security main restricted universe multiverse" >> /etc/apt/sources.list && \
    apt-get update && \
    apt-get dist-upgrade && \
    apt-get install \
        gdal-bin \
        git \
        gunicorn \
        libxpm4 \
        libxmu6 \
        nano \
        python3 \
        python3-flask \
        python3-gdal \
        python3-requests \
        python3-pip \
        python3-venv \
        python3-wheel && \
    rm --recursive --force /var/lib/apt/lists/* && \
    find / -xdev -type d -name __pycache__ -exec rm --recursive --force {} +


# RUN specific for IDL
RUN mkdir --parents \
        /.idl \
        /usr/local/harris && \
    chown {{ sWasdiSystemUserName }}:{{ sWasdiSystemGroupName }} \
        /.idl \
        /usr/local/harris && \
    chmod 750 \
        /.idl \
        /usr/local/harris


# Set the workdir to {{ sWasdiSystemUserHome }}/application
WORKDIR {{ sWasdiSystemUserHome }}/application


# From now, all commands executed are executed as {{ sWasdiSystemUserName }}
USER {{ sWasdiSystemUserName }}


# RUN specific for IDL:
# - create needed directories
# - create our Python virtual environment
# Please note:
#   - 'wasdi' is installed independently to be able to set the repository
#   - there is no cache: I suppose because it is too much dynamic
RUN mkdir --parents \
        {{ sWasdiSystemUserHome }}/application \
        {{ sWasdiSystemUserHome }}/install \
        {{ sWasdiSystemUserHome }}/tool \
        {{ sWasdiSystemUserHome }}/venv && \
    chmod 750 \
        {{ sWasdiSystemUserHome }}/application \
        {{ sWasdiSystemUserHome }}/install \
        {{ sWasdiSystemUserHome }}/tool \
        {{ sWasdiSystemUserHome }}/venv && \
    python3 -m venv --system-site-packages {{ sWasdiSystemUserHome }}/venv && \
    pip3 install --no-cache-dir --no-compile {{ sWasdiSystemUserHome }} wasdi


# Extract the envi archive
ADD envi552-linux.tar {{ sWasdiSystemUserHome }}/install/


# Install ENVI
# Remove two directories not needed
# Remove the installation directory no longer used
# Install the license file
RUN bash {{ sWasdiSystemUserHome }}/install/install-wasdi.sh s && \
    rm --recursive --force \
        /usr/local/harris/envi55/idl/examples \
        /usr/local/harris/envi55/idl/help \
        {{ sWasdiSystemUserHome }}/installinstall


# Overwrite install script with the one which is close to the Dockerfile
COPY --chown={{ sWasdiSoftwareRootDirectoryOwner }}:{{ sWasdiSoftwareRootDirectoryGroup }} ./ {{ sWasdiSystemUserHome }}/application/


# Install libraries only if {{ sWasdiSystemUserHome }}/application/pip.txt exists
#RUN bash {{ sWasdiSystemUserHome }}/tool/installUserPackage.sh --failure-is-ok --home-directory {{ sWasdiSystemUserHome }}/application --package-pip


# From now, all commands executed are executed with the user 'root'
USER root


# Install packages only if {{ sWasdiSystemUserHome }}/application/packages.txt exists
RUN bash {{ sWasdiSystemUserHome }}/tool/installUserPackage.sh --failure-is-ok --home-directory {{ sWasdiSystemUserHome }}/application --package-apt


# From now, all commands executed are executed as {{ sWasdiSystemUserName }}
USER {{ sWasdiSystemUserName }}


# Start the container
CMD ["/bin/bash", "sContainerUserHomeDirectory/application/docker-entrypoint.sh"]
