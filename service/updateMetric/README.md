# INTRODUCTION

Service to provide server metrics to WASDI.
These metrics are used by the scheduler to know which node is loaded/which node has enough resources.

# PIP librairies

This service depends on these Python librairies:
  - psutil
  - python-keycloak
  - urllib3

We recommend to install these librairies in a dedicated virtual environment.

# SystemD service

Deploy the file wasdi-update-metric.service.j2 in /etc/systemd/system/wasdi-update-metric.service

Replace the variable '{{ something }}' with the appropriate values

Reload SystemD:

```
# sudo systemctl daemon-reload
```

Start the service:

```
# sudo systemctl start wasdi-update-metric.service
```
