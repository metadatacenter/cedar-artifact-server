# CEDAR Template Server

To run the server

    java \
      -Dkeycloak.config.path="$CEDAR_HOME/keycloak.json" \
      -jar $CEDAR_HOME/cedar-template-server/cedar-template-server-application/target/cedar-template-server-application-*.jar \
      server \
      "$CEDAR_HOME/cedar-template-server/cedar-template-server-application/config.yml"

To access the application:

[http://localhost:9001/]()

To access the admin port:

[http://localhost:9101/]()