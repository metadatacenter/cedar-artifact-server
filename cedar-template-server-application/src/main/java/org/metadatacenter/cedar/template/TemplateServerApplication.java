package org.metadatacenter.cedar.template;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.metadatacenter.bridge.CedarDataServices;
import org.metadatacenter.cedar.template.core.CedarAssertionExceptionMapper;
import org.metadatacenter.cedar.template.health.TemplateServerHealthCheck;
import org.metadatacenter.cedar.template.resources.*;
import org.metadatacenter.server.security.Authorization;
import org.metadatacenter.server.security.AuthorizationKeycloakAndApiKeyResolver;
import org.metadatacenter.server.security.IAuthorizationResolver;
import org.metadatacenter.server.security.KeycloakDeploymentProvider;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import java.util.EnumSet;

import static org.eclipse.jetty.servlets.CrossOriginFilter.*;

public class TemplateServerApplication extends Application<TemplateServerConfiguration> {
  public static void main(String[] args) throws Exception {
    new TemplateServerApplication().run(args);
  }

  @Override
  public String getName() {
    return "template-server";
  }

  @Override
  public void initialize(Bootstrap<TemplateServerConfiguration> bootstrap) {
    // Init Keycloak
    KeycloakDeploymentProvider.getInstance();
    // Init Authorization Resolver
    IAuthorizationResolver authResolver = new AuthorizationKeycloakAndApiKeyResolver();
    Authorization.setAuthorizationResolver(authResolver);
    Authorization.setUserService(CedarDataServices.getUserService());
  }

  @Override
  public void run(TemplateServerConfiguration configuration, Environment environment) {
    final IndexResource index = new IndexResource();
    environment.jersey().register(index);

    final TemplateFieldsResource fields = new TemplateFieldsResource();
    environment.jersey().register(fields);

    final TemplateElementsResource elements = new TemplateElementsResource();
    environment.jersey().register(elements);

    final TemplatesResource templates = new TemplatesResource();
    environment.jersey().register(templates);

    final TemplateInstancesResource instances = new TemplateInstancesResource();
    environment.jersey().register(instances);

    final TemplateServerHealthCheck healthCheck = new TemplateServerHealthCheck();
    environment.healthChecks().register("message", healthCheck);

    environment.jersey().register(new CedarAssertionExceptionMapper());

    // Enable CORS headers
    final FilterRegistration.Dynamic cors = environment.servlets().addFilter("CORS", CrossOriginFilter.class);

    // Configure CORS parameters
    cors.setInitParameter(ALLOWED_ORIGINS_PARAM, "*");
    cors.setInitParameter(ALLOWED_HEADERS_PARAM,
        "X-Requested-With,Content-Type,Accept,Origin,Referer,User-Agent,Authorization");
    cors.setInitParameter(ALLOWED_METHODS_PARAM, "OPTIONS,GET,PUT,POST,DELETE,HEAD,PATCH");

    // Add URL mapping
    cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");

  }
}
