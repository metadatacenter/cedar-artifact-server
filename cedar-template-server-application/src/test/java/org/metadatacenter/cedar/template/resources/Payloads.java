package org.metadatacenter.cedar.template.resources;

import com.fasterxml.jackson.databind.JsonNode;
import org.metadatacenter.model.validation.JsonLdMapper;

import java.io.IOException;
import java.io.InputStream;

public class Payloads {

  public static JsonNode useExampleInstance001() {
    InputStream in = Payloads.class.getClassLoader().getResourceAsStream("instances/example-001.jsonld");
    try {
      return JsonLdMapper.MAPPER.readTree(in);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
