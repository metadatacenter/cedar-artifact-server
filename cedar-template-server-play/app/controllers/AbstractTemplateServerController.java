package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.model.CedarNodeType;
import org.metadatacenter.server.model.provenance.ProvenanceInfo;
import org.metadatacenter.server.play.AbstractCedarController;
import org.metadatacenter.util.provenance.ProvenanceUtil;
import play.libs.F;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.fasterxml.jackson.databind.node.JsonNodeType.NULL;

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


  protected static void checkImportModeSetProvenanceAndId(CedarNodeType cedarNodeType, JsonNode element,
                                                          ProvenanceInfo pi, F.Option<Boolean> importMode) {
    boolean im = (importMode != null && importMode.isDefined() && importMode.get());
    System.out.println("***TEMPLATE: CheckImport:" + importMode + ":" + im);
    if (im) {
      if ((element.get("@id") == null) || (NULL.equals(element.get("@id").getNodeType()))) {
        throw new IllegalArgumentException("You must specify @id when importing data");
      }
    } else {
      if ((element.get("@id") != null) && (!NULL.equals(element.get("@id").getNodeType()))) {
        throw new IllegalArgumentException("Specifying @id for new objects is not allowed");
      }
      ProvenanceUtil.addProvenanceInfo(element, pi);
      String id = cedarConfig.getLinkedDataPrefix(cedarNodeType) + UUID.randomUUID().toString();
      ((ObjectNode) element).put("@id", id);
    }
  }
}