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
    useradd --create-home --home-dir {{ sWasdiSystemUserHome }} --uid {{ sWasdiSystemUserId }} --gid {{ sWasdiSystemGroupId }} --non-unique --shell /bin/bash {{ sWasdiSystemUserName }}


# RUN generic to update the system and install common packages
RUN echo "APT::Install-Suggests \"0\";" > /etc/apt/apt.conf.d/99local && \
    echo "APT::Install-Recommends \"0\";" >> /etc/apt/apt.conf.d/99local && \
    echo "APT::Get::Assume-Yes \"true\";" >> /etc/apt/apt.conf.d/99local && \
    echo "APT::Keep-Downloaded-Packages \"false\";" >> /etc/apt/apt.conf.d/99local && \
    echo "Dpkg::Options::--force-confold;" >> /etc/apt/apt.conf.d/99local && \
    apt-get update && \
    apt-get install \
        ca-certificates && \
    echo "deb https://mirror.ubuntu.ikoula.com/ jammy main restricted universe multiverse"           > /etc/apt/sources.list && \
    echo "deb https://mirror.ubuntu.ikoula.com/ jammy-updates main restricted universe multiverse"  >> /etc/apt/sources.list && \
    echo "deb https://mirror.ubuntu.ikoula.com/ jammy-security main restricted universe multiverse" >> /etc/apt/sources.list && \
    apt-get update && \
    apt-get dist-upgrade && \
    apt-get install \
        curl \
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


# Specific for IDL:
# - create directories
# - extract the archive in a temporary directory
# - install the license server
# - clean temporary files
RUN mkdir --parents \
        /.idl \
        /tmp/install && \
    chown {{ sWasdiSystemUserName }}:{{ sWasdiSystemGroupName }} \
        /.idl && \
    chmod 750 \
        /.idl

ADD envi552-linux.tar /tmp/install/

RUN bash /tmp/install/install-wasdi.sh && \
    touch /usr/local/harris/license/o_licenseserverurl.txt && \
    chown {{ sWasdiSoftwareRootDirectoryOwner }}:{{ sWasdiSoftwareRootDirectoryGroup }} /usr/local/harris/license/o_licenseserverurl.txt && \
    chmod 640 /usr/local/harris/license/o_licenseserverurl.txt && \
    rm --recursive --force \
        /usr/local/harris/envi55/idl/examples \
        /usr/local/harris/envi55/idl/help \
        /tmp/install


# From now, all commands executed are executed as {{ sWasdiSystemUserName }}
USER {{ sWasdiSystemUserName }}


# Create the application directory
# Create the virtual environment
RUN mkdir --parents \
        {{ sWasdiSystemUserHome }}/application \
        {{ sWasdiSystemUserHome }}/venv && \
    python3 -m venv --system-site-packages {{ sWasdiSystemUserHome }}/venv && \
    pip3 install --no-cache-dir --no-compile wasdi


# Set the workdir to {{ sWasdiSystemUserHome }}/application
WORKDIR {{ sWasdiSystemUserHome }}/application


# Overwrite install script with the one which is close to the Dockerfile
COPY --chown={{ sWasdiSoftwareRootDirectoryOwner }}:{{ sWasdiSoftwareRootDirectoryGroup }} ./ {{ sWasdiSystemUserHome }}/application/


# Install libraries only if {{ sWasdiSystemUserHome }}/application/pip.txt exists
#RUN bash {{ sWasdiSystemUserHome }}/application/installUserPackage.sh --failure-is-ok --home-directory {{ sWasdiSystemUserHome }}/application --package-pip


# From now, all commands executed are executed with the user 'root'
USER root


# Install packages only if {{ sWasdiSystemUserHome }}/application/packages.txt exists
RUN bash {{ sWasdiSystemUserHome }}/application/installUserPackage.sh --failure-is-ok --home-directory {{ sWasdiSystemUserHome }}/application --package-apt


# From now, all commands executed are executed as {{ sWasdiSystemUserName }}
USER {{ sWasdiSystemUserName }}


# Start the container
CMD ["/bin/bash", "{{ sWasdiSystemUserHome }}/application/docker-entrypoint.sh"]
