package controllers;

import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.model.CedarNodeType;
import org.metadatacenter.provenance.ProvenanceInfo;
import org.metadatacenter.server.play.AbstractCedarController;
import org.metadatacenter.server.security.Authorization;
import org.metadatacenter.server.security.exception.CedarAccessException;
import org.metadatacenter.server.security.model.IAuthRequest;
import org.metadatacenter.server.security.model.user.CedarUser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class AbstractTemplateServerController extends AbstractCedarController {
  protected static CedarConfig cedarConfig;
  protected static List<String> FIELD_NAMES_EXCLUSION_LIST;
  protected static String USER_BASE_PATH;

  static {
    cedarConfig = CedarConfig.getInstance();
    FIELD_NAMES_EXCLUSION_LIST = new ArrayList<>();
    FIELD_NAMES_EXCLUSION_LIST.addAll(cedarConfig.getTemplateRESTAPI().getExcludedFields());
    USER_BASE_PATH = cedarConfig.getLinkedDataPrefix(CedarNodeType.USER);
  }

  protected static Integer ensureLimit(Integer limit) {
    return limit == null ? cedarConfig.getTemplateRESTAPI().getPagination().getDefaultPageSize() : limit;
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
    int maxPageSize = cedarConfig.getTemplateRESTAPI().getPagination().getMaxPageSize();
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