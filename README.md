# cedar-artifact-server

[![CI](https://github.com/metadatacenter/cedar-artifact-server/actions/workflows/ci.yml/badge.svg?branch=develop)](https://github.com/metadatacenter/cedar-artifact-server/actions/workflows/ci.yml)

Server that manages CEDAR artifacts (templates, elements fields, template instances).

This project is implemented in Java using [Dropwizard](http://www.dropwizard.io/).

The project contains two subdirectories:

- cedar-artifact-server-core: Core server functionality
- cedar-artifact-server-application: Dropwizard-based interface to server

## Versions

* Java: 19
* MongoDB: 3.0.0

## Getting started

Clone the project:

    git clone https://github.com/metadatacenter/cedar-artifact-server.git

Install MongoDB (using Homebrew):

    brew install mongodb

Start the MongoDB server:

    mongod

## Documentation

Documentation for the server can be found in the [project wiki](https://github.com/metadatacenter/cedar-docs/wiki).

## Questions

If you have questions about this repository, please subscribe to the
[CEDAR Developer Supportmailing list](https://mailman.stanford.edu/mailman/listinfo/cedar-developers).

After subscribing, send messages to cedar-developers at lists.stanford.edu.


