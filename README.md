
Exclude development project dependencies from TimeMachine or Restic.

Heavily inspired by [stevegrunwell/asimov](https://github.com/stevegrunwell/asimov)

# Motivation

Last week my MacBook died and I had to restore my TimeMachine backup from my NAS.
It took 8 hours mostly because of all `.venv`, `node_modules` and co that I was too lazy to ignore.

Since TimeMachine does not allow regular expressions or rules, I would have to add each directory manually.

This project automates the process by searching all of the directories that satisfy some rules and
by adding them to the exclusion list.

It uses Scala Native to create a single binary that does not need the JVM.

# Usage

Grab the Apple Silicon binary from the releases.

```
asimov-sn -r rules.txt -b ~/projects
```

You can start with [this file](https://github.com/vincenzobaz/asimov-sn/blob/main/src/main/resources/exclusion_rules.txt)

More info on `asimov-sn --help`

# Rules

The possible formats for rules are:

 1. Sentinel: if a directory contains `pyproject.toml`, exclude `.venv`. Syntax: `SENT <directory-to-ignore> <sentinel-file>`
 2. Directory name: exclude all directories with a given name. Syntax: `SIMP <directory name>`
 3. Full path: exclude a specific absolute path. Syntax `FULL <absolute path>`

