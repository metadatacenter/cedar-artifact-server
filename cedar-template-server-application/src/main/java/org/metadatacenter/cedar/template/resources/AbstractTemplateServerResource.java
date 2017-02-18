package org.metadatacenter.cedar.template.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.model.CedarNodeType;
import org.metadatacenter.server.jsonld.LinkedDataUtil;
import org.metadatacenter.server.model.provenance.ProvenanceInfo;
import org.metadatacenter.util.provenance.ProvenanceUtil;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import java.util.*;

import static com.fasterxml.jackson.databind.node.JsonNodeType.NULL;

public class AbstractTemplateServerResource {

  protected
  @Context
  UriInfo uriInfo;

  protected
  @Context
  HttpServletRequest request;

  protected final CedarConfig cedarConfig;

  protected final LinkedDataUtil linkedDataUtil;

  protected final ProvenanceUtil provenanceUtil;

  protected static List<String> FIELD_NAMES_EXCLUSION_LIST;


  protected AbstractTemplateServerResource(CedarConfig cedarConfig) {
    this.cedarConfig = cedarConfig;
    this.linkedDataUtil = cedarConfig.buildLinkedDataUtil();
    this.provenanceUtil = new ProvenanceUtil(linkedDataUtil);
    FIELD_NAMES_EXCLUSION_LIST = new ArrayList<>();
    FIELD_NAMES_EXCLUSION_LIST.addAll(cedarConfig.getTemplateRESTAPI().getExcludedFields());
  }

  protected void checkImportModeSetProvenanceAndId(CedarNodeType cedarNodeType, JsonNode element,
                                                   ProvenanceInfo pi, Optional<Boolean> importMode) {
    boolean im = (importMode != null && importMode.isPresent() && importMode.get());
    if (im) {
      if ((element.get("@id") == null) || (NULL.equals(element.get("@id").getNodeType()))) {
        throw new IllegalArgumentException("You must specify @id when importing data");
      }
    } else {
      if ((element.get("@id") != null) && (!NULL.equals(element.get("@id").getNodeType()))) {
        throw new IllegalArgumentException("Specifying @id for new objects is not allowed");
      }
      provenanceUtil.addProvenanceInfo(element, pi);
      String id = linkedDataUtil.buildNewLinkedDataId(cedarNodeType);
      ((ObjectNode) element).put("@id", id);
    }
  }

  protected Integer ensureLimit(Optional<Integer> limit) {
    if (limit == null || !limit.isPresent()) {
      return cedarConfig.getTemplateRESTAPI().getPagination().getDefaultPageSize();
    } else {
      return limit.get();
    }
  }

  protected Integer ensureOffset(Optional<Integer> offset) {
    if (offset == null || !offset.isPresent()) {
      return 0;
    } else {
      return offset.get();
    }
  }

  protected Boolean ensureSummary(Optional<Boolean> summary) {
    if (summary == null || !summary.isPresent()) {
      return false;
    } else {
      return summary.get();
    }
  }

  protected void checkPagingParameters(Integer limit, Integer offset) {
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

  protected static List<String> getAndCheckFieldNames(Optional<String> fieldNames, boolean summary) {
    if (fieldNames != null && fieldNames.isPresent()) {
      if (summary == true) {
        throw new IllegalArgumentException(
            "It is no allowed to specify parameter 'field_names' and also set 'summary' to true!");
      } else if (fieldNames.get().length() > 0) {
        return Arrays.asList(fieldNames.get().split(","));
      }
    }
    return null;
  }

  protected static void checkPagingParametersAgainstTotal(Integer offset, long total) {
    if (offset != 0 && offset > total - 1) {
      throw new IllegalArgumentException(
          "Parameter 'offset' must be smaller than the total count of objects, which is " + total + "!");
    }
  }


}
