package controllers;

import org.metadatacenter.constant.ConfigConstants;
import org.metadatacenter.provenance.ProvenanceInfo;
import org.metadatacenter.server.play.AbstractCedarController;
import org.metadatacenter.server.security.Authorization;
import org.metadatacenter.server.security.exception.CedarAccessException;
import org.metadatacenter.server.security.model.IAuthRequest;
import org.metadatacenter.server.security.model.user.CedarUser;
import play.Configuration;
import play.Play;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class AbstractTemplateServerController extends AbstractCedarController {
  protected static List<String> FIELD_NAMES_EXCLUSION_LIST;
  protected static Configuration config;
  protected static String USER_BASE_PATH;

  static {
    config = Play.application().configuration();
    FIELD_NAMES_EXCLUSION_LIST = new ArrayList<>();
    FIELD_NAMES_EXCLUSION_LIST.addAll(config.getStringList(ConfigConstants.FIELD_NAMES_LIST_EXCLUSION));
    USER_BASE_PATH = config.getString(ConfigConstants.USER_DATA_ID_PATH_BASE);
  }

  protected static Integer ensureLimit(Integer limit) {
    return limit == null ? config.getInt(ConfigConstants.PAGINATION_DEFAULT_PAGE_SIZE) : limit;
  }

  protected static void checkPagingParameters(Integer limit, Integer offset) {
    // check offset
    if (offset < 0) {
      throw new IllegalArgumentException("Parameter 'offset' must be positive!");
    }
    // check limit
    if (limit <= 0) {
      throw new IllegalArgumentException("Parameter 'limit' must be greater than zero!");
    }
    int maxPageSize = config.getInt(ConfigConstants.PAGINATION_MAX_PAGE_SIZE);
    if (limit > maxPageSize) {
      throw new IllegalArgumentException("Parameter 'limit' must be at most " + maxPageSize + "!");
    }
  }

  protected static void checkPagingParametersAgainstTotal(Integer offset, long total) {
    if (offset != 0 && offset > total - 1) {
      throw new IllegalArgumentException("Parameter 'offset' must be smaller than the total count of objects, which " +
          "is " + total + "!");
    }
  }

  protected static List<String> getAndCheckFieldNames(String fieldNames, boolean summary) {
    if (fieldNames != null) {
      if (summary == true) {
        throw new IllegalArgumentException("It is no allowed to specify parameter 'fieldNames' and also set 'summary'" +
            " to true!");
      } else if (fieldNames.length() > 0) {
        return Arrays.asList(fieldNames.split(","));
      }
    }
    return null;
  }

  protected static ProvenanceInfo buildProvenanceInfo(IAuthRequest authRequest) {
    ProvenanceInfo pi = new ProvenanceInfo();
    String id = null;
    try {
      CedarUser accountInfo = Authorization.getUser(authRequest);
      id = accountInfo.getUserId();
    } catch (CedarAccessException e) {
      e.printStackTrace();
    }
    Date now = new Date();
    String nowString = xsdDateTimeFormat.format(now);
    String userId = USER_BASE_PATH + id;
    pi.setCreatedOn(nowString);
    pi.setCreatedBy(userId);
    pi.setLastUpdatedOn(nowString);
    pi.setLastUpdatedBy(userId);
    return pi;
  }
}