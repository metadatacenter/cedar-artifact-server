import com.typesafe.config.ConfigFactory;
import play.Application;
import play.Configuration;
import play.GlobalSettings;
import play.Mode;
import play.libs.F.Promise;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import utils.DataServices;

import java.io.File;

public class Global extends GlobalSettings {

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

  @Override
  public Action<?> onRequest(Http.Request request, java.lang.reflect.Method actionMethod) {
    return new ActionWrapper(super.onRequest(request, actionMethod));
  }

  @Override
  public void onStart(Application application) {
    DataServices.getInstance();
    super.onStart(application);
  }

  @Override
  public void onStop(Application application) {
    super.onStop(application);
  }
}