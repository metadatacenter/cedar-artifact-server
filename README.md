# cedar-template-server

CEDAR server to manage templates, template elements, and template instances.

This project is implemented in Java using [Play Framework](http://www.playframework.com/).
The multimodule structure of this project is based on the template [play-cedar-service](https://github.com/metadatacenter/play-cedar-service),
which allows the use Maven with the Play framework.

The project contains two subdirectories:

- cedar-template-server-core: Core server functionality
- cedar-template-server-play: Play-based interface to server

## Versions

* Java: 1.8
* Play Framework: 2.3.8
* MongoDB: 3.0.0

## Getting started

Clone the project:

    git clone https://github.com/metadatacenter/cedar-template-server.git

Install MongoDB (using Homebrew):

    brew install mongodb

Start the MongoDB server:

    mongod

## Running the tests

Go to the project root folder and execute the Maven "test" goal:

    mvn test

## Starting the services

At the project root folder:

    mvn install
    cd cedar-template-server-play
    mvn play2:run

By default, the services will be running at http://localhost:9000.

## Documentation

Documentation for the server can be found in the [project wiki](https://github.com/metadatacenter/cedar-template-server/wiki).

A detailed description of its REST API is contained in the RESTAPI.html file in the doc subdirectory.

## IntelliJ IDEA 14 configuration

Instructions on how to configure IntelliJ 14 to build and run this project are available [here] (https://github.com/metadatacenter/cedar-docs/wiki/Maven-Play-project-configuration-in-IntelliJ-IDEA-14).

## Questions

If you have questions about this repository, please subscribe to the [CEDAR Developer Support
mailing list](https://mailman.stanford.edu/mailman/listinfo/cedar-developers).
After subscribing, send messages to cedar-developers at lists.stanford.edu.


