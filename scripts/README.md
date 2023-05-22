# cleanSnapDirectories.sh

## Introduction

This script is used by WASDI:
  - to clean SNAP directories
  - recreate them if needed

This script is periodically executed using a crontab.

# renderJinjaTemplate.py

## Introduction

This script is used by WASDI to render Jinja template.

To discover this template format: https://jinja.palletsprojects.com/en/

## Sample template

Create a file called mySampleTemplate.j2 with this content:

```
Hello World!

Example with a simple string: {{ myVar1 }}

Example with a loop:
{%- for sCurrentValue in myList1 %}
{{ sCurrentValue }}
{%- endfor %}

Example with a dictionnary:
myDict1['key'] == {{ myDict1['key'] }}
myDict1['otherKey'] == {{ myDict1['otherKey'] }}
```

## Usage

### General command line

```
# python3 renderJinjaTemplate.py  --help
usage: renderJinjaTemplate.py [-h] --json-inline DICTDATAINLINEFROMCOMMANDLINE --template SSOURCETEMPLATE --rendered-file STARGETFILE [--strict]

optional arguments:
  -h, --help            show this help message and exit
  --json-inline DICTDATAINLINEFROMCOMMANDLINE
                        Datas to use to fill the template.
  --template SSOURCETEMPLATE
                        Template file to use as source.
  --rendered-file STARGETFILE
                        Full path to the targeted file when the template will be rendered.
  --strict              With this parameter, we enable the strict mode. It means, if a variable is used in a Jinja template but not given on the command line, we will stop with an error.
```

### Real case

Execute this command:

```
# python3 renderJinjaTemplate.py \
  --template mySampleTemplate.j2
  --rendered-file myFinalFile.txt
  --json-inline '{"myVar1": "myValue1", "myList1": ["a", "b"], "myDict1": {"key":"myValue", "otherKey": "otherValue"}}'
```

You obtain a file myFinalFile.txt which look like:

```
Hello World!

Example with a simple string: myValue1

Example with a loop:
a
b

Example with a dictionnary:
myDict1['key'] == myValue
myDict1['otherKey'] == otherValue
```

## Features

1. Can be permissive (without --strict) or strict (with --strict)
2. The rendered file is written in /tmp and compared to the final file (--rendered-file):
  - if checksum are different, the final file (--rendered-file) is replaced with the temporary file just rendered
  - if checksum are the same, the final file (--rendered-file) is not replaced: useful in an Ansible context to avoid to trigger a handler
