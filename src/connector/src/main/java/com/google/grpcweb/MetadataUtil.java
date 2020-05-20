package com.google.grpcweb;

import io.grpc.Metadata;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

class MetadataUtil {
  private static final String BINARY_HEADER_SUFFIX = "-bin";
  private static final String GRPC_HEADER_PREFIX = "x-grpc-";
  private static final List<String> EXCLUDED = Arrays.asList("x-grpc-web", "content-type",
      "grpc-accept-encoding", "grpc-encoding");

  static Metadata getHtpHeaders(HttpServletRequest req) {
    Metadata httpHeaders = new Metadata();
    Enumeration<String> headerNames = req.getHeaderNames();
    if (headerNames == null) {
      return httpHeaders;
    }
    // copy all headers "x-grpc-*" into Metadata
    // TODO: do we need to copy all "x-*" headers instead?
    while (headerNames.hasMoreElements()) {
      String headerName = headerNames.nextElement();
      if (EXCLUDED.contains(headerName.toLowerCase())) {
        continue;
      }
      if (headerName.toLowerCase().startsWith(GRPC_HEADER_PREFIX)) {
        // Get all the values of this header.
        Enumeration<String> values = req.getHeaders(headerName);
        if (values != null) {
          // Java enumerations have klunky API. lets convert to a list.
          // this will be a short list usually.
          List<String> list = Collections.list(values);
          for (String s : list) {
            if (headerName.toLowerCase().endsWith(BINARY_HEADER_SUFFIX)) {
              // Binary header
              httpHeaders.put(
                  Metadata.Key.of(headerName, Metadata.BINARY_BYTE_MARSHALLER), s.getBytes());
            } else {
              // String header
              httpHeaders.put(
                  Metadata.Key.of(headerName, Metadata.ASCII_STRING_MARSHALLER), s);
            }
          }
        }
      }
    }
    return httpHeaders;
  }

  static Map<String, String> getHttpHeadersFromMetadata(Metadata trailer) {
    Map<String, String> map = new HashMap<>();
    for (String key : trailer.keys()) {
      if (EXCLUDED.contains(key.toLowerCase())) {
        continue;
      }
      if (key.endsWith(Metadata.BINARY_HEADER_SUFFIX)) {
        // TODO allow any object type here
        byte[] value =  trailer.get(Metadata.Key.of(key, Metadata.BINARY_BYTE_MARSHALLER));
        map.put(key, new String(value));
      } else {
        String value = trailer.get(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER));
        map.put(key, value);
      }
    }
    return map;
  }
}
