# cedar-templates-api

Backend services to support operations on Metadata Templates. 

This project is implemented in Java using [Play Framework](http://www.playframework.com/). The multimodule structure of this project is based on the template [play-cedar-service](https://github.com/metadatacenter/play-cedar-service), which allows to use the Maven build system with Play.

The project contains two subdirectories:
- cedar-templates-api-core: Services core logic. 
- cedar-temlates-api-play: Play-based functionality of the services.

## Versions
* Java: 1.8
* Play Framework: 2.3.8
* MongoDB: 3.0.0

## Getting started

Clone the project:

`$ git clone https://github.com/metadatacenter/cedar-templates-api.git`

Install MongoDB (using Homebrew):

`$ brew install mongodb`

Start the MongoDB server:

`$ mongod`

## Running the tests

Go to the project root folder and execute the Maven "test" goal:

```
$ mvn test
```

## Starting the services

At the project root folder:

```
$ mvn install
$ cd cedar-templates-api-play
$ mvn play2:run
```

By default, the services will be running at http://localhost:9000.

## IntelliJ IDEA 14 configuration

Instructions on how to configure IntelliJ 14 to build and run this project are available [here] (https://github.com/metadatacenter/cedar-docs/wiki/Maven-Play-project-configuration-in-IntelliJ-IDEA-14).