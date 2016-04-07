import com.typesafe.config.ConfigFactory;
import org.metadatacenter.server.security.*;
import play.*;
import play.libs.F.Promise;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import utils.DataServices;

import java.io.File;
import java.lang.reflect.Method;

import static play.mvc.Results.badRequest;
import static play.mvc.Results.notFound;

public class Global extends GlobalSettings {

  private final Logger.ALogger accessLogger = Logger.of("access");

  @Override
  public Configuration onLoadConfig(Configuration config, File path, ClassLoader classloader, Mode mode) {
    // System.out.println("Execution mode: " + mode.name());
    // Modifies the configuration according to the execution mode (DEV, TEST, PROD)
    if (mode.name().compareTo("TEST") == 0) {
      return new Configuration(ConfigFactory.load("application." + mode.name().toLowerCase() + ".conf"));
    } else {
      return onLoadConfig(config, path, classloader); // default implementation
    }
  }

  // If the framework doesn’t find an action method for a request, the onHandlerNotFound operation will be called:
  @Override
  public Promise<Result> onHandlerNotFound(Http.RequestHeader request) {
    return Promise.<Result>pure(notFound(request.uri()));
  }

  // The onBadRequest operation will be called if a route was found, but it was not possible to bind the request
  // parameters
  @Override
  public Promise<Result> onBadRequest(Http.RequestHeader request, String error) {
    return Promise.<Result>pure(badRequest(error));
  }

  /* For CORS */
  private class ActionWrapper extends Action.Simple {
    public ActionWrapper(Action<?> action) {
      this.delegate = action;
    }

    @Override
    public Promise<Result> call(Http.Context ctx) throws java.lang.Throwable {
      Promise<Result> result = this.delegate.call(ctx);
      Http.Response response = ctx.response();
      response.setHeader("Access-Control-Allow-Origin", "*");
      return result;
    }
  }

  /* Log all requests */
  @Override
  @SuppressWarnings("rawtypes")
  public Action<?> onRequest(Http.Request request, Method method) {
    // Log request
    accessLogger.info("method=" + request.method() + " uri=" + request.uri()
        + " remote-address=" + request.remoteAddress());
    // The ActionWrapper is used for CORS
    return new ActionWrapper(super.onRequest(request, method));
  }


  @Override
  public void onStart(Application application) {
    // init data services
    DataServices.getInstance();
    // init keycloak deployment
    KeycloakDeploymentProvider.getInstance();
    // init authorization resolver
    IAuthorizationResolver authResolver = null;
    Configuration config = application.configuration();
    Boolean noAuth = config.getBoolean("authentication.noAuth");
    if (noAuth != null && noAuth.booleanValue()) {
      authResolver = new AuthorizationNoauthResolver();
    } else {
      authResolver = new AuthorizationKeycloakAndApiKeyResolver(DataServices.getInstance().getUserService());
    }
    Authorization.setAuthorizationResolver(authResolver);
    Authorization.setUserService(DataServices.getInstance().getUserService());
    // onStart
    super.onStart(application);
  }

  @Override
  public void onStop(Application application) {
    super.onStop(application);
  }
}