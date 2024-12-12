FROM ubuntu:22.04


LABEL org.opencontainers.image.authors="WASDI SARL"


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
        dos2unix \
        gdal-bin \
        libgdal-dev \
        git \
        gunicorn \
        nano \
        python3 \
        python3-flask \
        python3-gdal \
        python3-requests \
        python3-pip \
        python3-venv \
        python3-wheel \
        wget && \
    rm --recursive --force /var/lib/apt/lists/* && \
    find / -xdev -type d -name __pycache__ -exec rm --recursive --force {} +


# RUN specific to install the JRE
RUN mkdir --parents \
        {{ sWasdiJre17RootDirectoryPath }} \
        {{ sWasdiJre17RootDirectoryPath }}/{{ sWasdiJre17Version }} && \
    chown {{ sWasdiJre17RootDirectoryOwner }}:{{ sWasdiJre17RootDirectoryGroup }} \
        {{ sWasdiJre17RootDirectoryPath }} \
        {{ sWasdiJre17RootDirectoryPath }}/{{ sWasdiJre17Version }} && \
    chmod {{ sWasdiJre17RootDirectoryMode }} \
        {{ sWasdiJre17RootDirectoryPath }} \
        {{ sWasdiJre17RootDirectoryPath }}/{{ sWasdiJre17Version }} && \
    wget \
        --no-verbose \
        --output-document /tmp/{{ sWasdiJre17SourceUrl | basename }} \
        --progress=bar:force:noscroll \
        --show-progress \
        {{ sWasdiJre17SourceUrl }} && \
    tar \
        --directory {{ sWasdiJre17RootDirectoryPath }}/{{ sWasdiJre17Version }} \
        --extract \
        --file /tmp/{{ sWasdiJre17SourceUrl | basename }} \
        --strip-components=1 && \
    ln \
        --symbolic \
        {{ sWasdiJre17RootDirectoryPath }}/{{ sWasdiJre17Version }} \
        {{ sWasdiJre17RootDirectoryPath }}/latest && \
    chown --no-dereference {{ sWasdiJre17RootDirectoryOwner }}:{{ sWasdiJre17RootDirectoryGroup }} \
        {{ sWasdiJre17RootDirectoryPath }}/latest && \
    rm --force \
        /tmp/{{ sWasdiJre17SourceUrl | basename }}

ENV JAVA_HOME="{{ sWasdiJre17RootDirectoryPath }}/{{ sWasdiJre17Version }}"
ENV PATH="{{ sWasdiJre17RootDirectoryPath }}/{{ sWasdiJre17Version }}/bin:${PATH}"


# RUN specific to install Octave
RUN apt-get update && \
    apt-get install \
        octave-* && \
    rm --recursive --force /var/lib/apt/lists/* && \
    find / -xdev -type d -name __pycache__ -exec rm --recursive --force {} +


# From now, all commands executed are executed as {{ sWasdiSystemUserName }}
USER {{ sWasdiSystemUserName }}


# Create the application directory
# Create the virtual environment
RUN mkdir --parents \
        {{ sWasdiSystemUserHome }}/application


# Set the workdir to {{ sWasdiSystemUserHome }}/application
WORKDIR {{ sWasdiSystemUserHome }}/application


## FROM HERE, THERE IS NO MORE CACHE ##
# Copy the needed files
COPY --chown={{ sWasdiSoftwareRootDirectoryOwner }}:{{ sWasdiSoftwareRootDirectoryGroup }} ./ {{ sWasdiSystemUserHome }}/application/


# Create the virtual environment
# Install the common PIP libraries
RUN python3 -m venv --system-site-packages {{ sWasdiSystemUserHome }}/venv && \
    pip3 install --no-cache-dir --no-compile \
        numpy && \
    pip3 install --no-cache-dir --no-compile {{ sPythonPipLibraryWasdiRepository }} \
        wasdi && \
    find {{ sWasdiSystemUserHome }} -xdev -type d -name __pycache__ -exec rm --recursive --force {} +


# From now, all commands executed are executed with the user 'root'
USER root


# Install packages only if {{ sWasdiSystemUserHome }}/application/packages.txt exists
RUN bash {{ sWasdiSystemUserHome }}/application/installUserPackage.sh \
        --application-directory {{ sWasdiSystemUserHome }}/application \
        --home-directory {{ sWasdiSystemUserHome }} \
        --package-apt


# From now, all commands executed are executed as {{ sWasdiSystemUserName }}
USER {{ sWasdiSystemUserName }}


# Start the container
CMD [ \
    "{{ sWasdiSystemUserHome }}/venv/bin/python3", \
    "/usr/bin/gunicorn", \
    "--workers", \
    "1", \
    "--max-requests", \
    "1", \
    "--timeout", \
    "3600", \
    "--bind", \
    "0.0.0.0:5000", \
    "wasdiProcessorServer:app" \
]
