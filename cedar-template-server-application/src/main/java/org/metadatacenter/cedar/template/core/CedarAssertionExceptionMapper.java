package org.metadatacenter.cedar.template.core;

import org.metadatacenter.rest.exception.CedarAssertionException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class CedarAssertionExceptionMapper implements ExceptionMapper<CedarAssertionException> {

  public Response toResponse(CedarAssertionException exception) {
    return Response.status(exception.getCode())
        .entity(exception.asJson())
        .type(MediaType.APPLICATION_JSON)
        .build();
  }

}
