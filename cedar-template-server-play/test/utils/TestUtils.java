package utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.junit.runner.Description;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.server.security.CedarApiKeyAuthRequest;
import org.metadatacenter.server.security.model.IAuthRequest;
import org.metadatacenter.server.security.model.user.CedarUser;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestUtils {

  public static String getTestAuthHeader() {
    CedarConfig cedarConfig = DataServices.getInstance().getCedarConfig();
    // TODO: use here a user specifically created for tests instead of admin
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

  public static String readFile(String path, Charset encoding)
      throws IOException
  {
    byte[] encoded = Files.readAllBytes(Paths.get(path));
    return new String(encoded, encoding);
  }

  public static JsonNode readAsJson(String s) {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode json = null;
    try {
      json = mapper.readTree(s);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return json;
  }

  public static String getTestHeader(Description description, String resourceType) {
    return "\n------ Test: " + description + " (Resource type: " + resourceType +  ") ------";
  }

}