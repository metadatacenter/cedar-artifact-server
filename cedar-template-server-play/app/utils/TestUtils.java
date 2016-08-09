package utils;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.server.security.CedarApiKeyAuthRequest;
import org.metadatacenter.server.security.model.IAuthRequest;
import org.metadatacenter.server.security.model.user.CedarUser;

import java.io.IOException;

public class TestUtils {

  public static String getTestAuthHeader() {
    CedarConfig cedarConfig = DataServices.getInstance().getCedarConfig();
    // TODO: we should probably use a user specifically created for tests here instead of admin
    String adminUserUUID = cedarConfig.getKeycloakConfig().getAdminUser().getUuid();
    CedarUser user = null;
    try {
      user = DataServices.getInstance().getUserService().findUser(adminUserUUID);
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ProcessingException e) {
      e.printStackTrace();
    }
    IAuthRequest authRequest = new CedarApiKeyAuthRequest(user.getFirstActiveApiKey());
    return authRequest.getAuthHeader();
  }

}