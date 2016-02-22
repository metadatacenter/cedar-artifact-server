package com.metadatacenter.server.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.fluent.Request;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.common.util.Base64Url;
import org.keycloak.representations.AccessToken;
import org.keycloak.util.JsonSerialization;
import org.metadatacenter.server.security.exception.*;
import org.metadatacenter.server.security.model.AuthorisedUser;
import org.metadatacenter.server.security.model.IAccountInfo;
import org.metadatacenter.server.security.model.IUserInfo;
import org.metadatacenter.server.security.model.SecurityRole;
import org.metadatacenter.server.security.model.keycloak.KeycloakAccountInfo;
import org.metadatacenter.server.security.model.keycloak.KeycloakUserInfo;
import org.metadatacenter.server.security.model.play.IAuthRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Http;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class KeycloakUtils {

  private final static String KEYCLOAK_JSON = "keycloak.json";
  public final static String KEYCLOAK_TOKEN_URL_SUFFIX = "/protocol/openid-connect/token";
  public final static String KEYCLOAK_ACCOUNT_URL_SUFFIX = "/account";
  public final static String KEYCLOAK_USERINFO_URL_SUFFIX = "/protocol/openid-connect/userinfo";

  private final static String SECRET_KEY = "secret";
  private static final int connectTimeout = 1000;
  private static final int socketTimeout = 10000;
  private static Logger log = LoggerFactory.getLogger(KeycloakUtils.class);

  public static <T> T parseToken(String encoded, Class<T> clazz) throws IOException {
    if (encoded == null) {
      return null;
    }

    String[] parts = encoded.split("\\.");
    if (parts.length < 2 || parts.length > 3) {
      throw new IllegalArgumentException("Parsing error");
    }

    byte[] bytes = Base64Url.decode(parts[1]);
    return JsonSerialization.readValue(bytes, clazz);
  }

  public static KeycloakDeployment buildDeployment() {
    InputStream config = Thread.currentThread().getContextClassLoader().getResourceAsStream(KeycloakUtils
        .KEYCLOAK_JSON);
    return KeycloakDeploymentBuilder.build(config);
  }

  public static String getRefreshTokenPostData(String keycloakRefreshToken) {
    List<NameValuePair> formparams = new ArrayList<>();
    formparams.add(new BasicNameValuePair("grant_type", "refresh_token"));
    formparams.add(new BasicNameValuePair("refresh_token", keycloakRefreshToken));
    UrlEncodedFormEntity form = null;
    try {
      form = new UrlEncodedFormEntity(formparams, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
      log.error("Error while encoding form content", e);
    }

    String content = null;
    try {
      content = IOUtils.toString(form.getContent(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      e.printStackTrace();
      log.error("Error while converting content", e);
    }
    return content;
  }

  public static String getBasicAuthToken(KeycloakDeployment deployment) {
    String resourceId = deployment.getResourceName();
    String clientSecret = (String) (deployment.getResourceCredentials().get(SECRET_KEY));
    StringBuilder sb = new StringBuilder();
    sb.append(resourceId).append(":").append(clientSecret);
    return Base64Url.encode(sb.toString().getBytes());
  }

  private static IAccountInfo getAccountInfo(IAuthRequest authRequest) {
    final KeycloakDeployment deployment = KeycloakDeploymentProvider.getInstance().getKeycloakDeployment();

    IAccountInfo accountInfo = null;

    String url = deployment.getRealmInfoUrl() + KeycloakUtils.KEYCLOAK_ACCOUNT_URL_SUFFIX;
    String authString = "bearer " + authRequest.getToken();
    String acceptString = "application/json";

    try {
      HttpResponse response = Request.Get(url)
          .addHeader(Http.HeaderNames.CONTENT_TYPE, Http.MimeTypes.FORM)
          .addHeader(Http.HeaderNames.AUTHORIZATION, authString)
          .addHeader(Http.HeaderNames.ACCEPT, acceptString)
          .connectTimeout(connectTimeout)
          .socketTimeout(socketTimeout)
          .execute()
          .returnResponse();

      int statusCode = response.getStatusLine().getStatusCode();
      //System.out.println("Status code:" + statusCode);
      String responseAsString = EntityUtils.toString(response.getEntity());
      if (statusCode == Http.Status.OK) {
        ObjectMapper mapper = new ObjectMapper();
        accountInfo = mapper.readValue(responseAsString, KeycloakAccountInfo.class);
      } else {
        //System.out.println("Reponse:" + responseAsString);
      }

    } catch (IOException ex) {
      log.error("Error while reading user details from Keycloak", ex);
    }

    return accountInfo;
  }

  private static IUserInfo getUserInfo(IAuthRequest authRequest) {
    final KeycloakDeployment deployment = KeycloakDeploymentProvider.getInstance().getKeycloakDeployment();
    IUserInfo userInfo = null;

    String url = deployment.getRealmInfoUrl() + KeycloakUtils.KEYCLOAK_USERINFO_URL_SUFFIX;
    String authString = "bearer " + authRequest.getToken();
    String acceptString = "application/json";

    try {
      HttpResponse response = Request.Get(url)
          .addHeader(Http.HeaderNames.CONTENT_TYPE, Http.MimeTypes.FORM)
          .addHeader(Http.HeaderNames.AUTHORIZATION, authString)
          .addHeader(Http.HeaderNames.ACCEPT, acceptString)
          .connectTimeout(connectTimeout)
          .socketTimeout(socketTimeout)
          .execute()
          .returnResponse();

      int statusCode = response.getStatusLine().getStatusCode();
      //System.out.println("Status code:" + statusCode);
      String responseAsString = EntityUtils.toString(response.getEntity());
      if (statusCode == Http.Status.OK) {
        ObjectMapper mapper = new ObjectMapper();
        userInfo = mapper.readValue(responseAsString, KeycloakUserInfo.class);
      } else {
        //System.out.println("Reponse:" + responseAsString);
      }

    } catch (IOException ex) {
      log.error("Error while reading user details from Keycloak", ex);
    }

    return userInfo;
  }

  public static void enforceRealmRoleOnOfflineToken(IAuthRequest authRequest, String roleName) throws CedarAccessException {
    try {
      if (authRequest.getToken() == null) {
        throw new AccessTokenMissingException();
      }
      AccessToken accessToken = KeycloakUtils.parseToken(authRequest.getToken(), AccessToken.class);
      if (accessToken == null) {
        throw new InvalidOfflineAccessTokenException();
      } else if (accessToken.isExpired()) {
        throw new AccessTokenExpiredException(accessToken.getExpiration());
      } else {
        if (accessToken.getRealmAccess() == null
            || accessToken.getRealmAccess().getRoles() == null
            || !accessToken.getRealmAccess().getRoles().contains(roleName)) {
          throw new MissingRealmRoleException(roleName);
        }
      }
    } catch (IOException e) {
      throw new InvalidOfflineAccessTokenException();
    }
  }

  public static void checkIfTokenIsStillActiveByUserInfo(IAuthRequest authRequest) throws CedarAccessException {
    IUserInfo userInfo = getUserInfo(authRequest);
    if (userInfo == null) {
      throw new FailedToLoadUserInfoException();
    }
  }

  public static AuthorisedUser getUserFromToken(AccessToken accessToken) {
    AuthorisedUser au = new AuthorisedUser();
    if (accessToken != null) {
      au.setIdentifier(accessToken.getPreferredUsername());
      List<SecurityRole> roles = new ArrayList<>();
      Set<String> realmRoleNames = accessToken.getRealmAccess().getRoles();
      for (String roleName : realmRoleNames) {
        SecurityRole sr = new SecurityRole();
        sr.setName(roleName);
        roles.add(sr);
      }
      au.setRoles(roles);
    }
    return au;
  }

}