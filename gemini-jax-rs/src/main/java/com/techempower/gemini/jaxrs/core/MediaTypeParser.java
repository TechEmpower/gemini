package com.techempower.gemini.jaxrs.core;

/**
 * Specifications:
 * <ul>
 *   <li><a href="https://tools.ietf.org/html/rfc7230#section-3.2.6">RFC 7230 Section 3.2.6</a></li>
 *  <li><a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">RFC 7231 Section 5.3.3</a></li>
 *   <li>
 *     <a href="https://download.oracle.com/javaee-archive/jax-rs-spec.java.net/jsr339-experts/att-3593/spec.pdf">
 *        JAX-RS spec
 *     </a> section 3.5 and 3.8
 *   </li>
 * </ul>
 */
public interface MediaTypeParser
{
  QMediaTypeGroup parse(String mediaType);
}
