/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.olingo.fit.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.apache.olingo.commons.api.edm.constants.ODataServiceVersion;
import org.apache.olingo.commons.core.data.AtomSerializer;
import org.apache.olingo.commons.core.op.InjectableSerializerProvider;
import org.apache.olingo.fit.metadata.Metadata;
import org.apache.olingo.fit.serializer.FITAtomDeserializer;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Commons {

  /**
   * Logger.
   */
  protected static final Logger LOG = LoggerFactory.getLogger(Commons.class);

  private static final Map<ODataServiceVersion, FITAtomDeserializer> ATOM_DESERIALIZER =
          new EnumMap<ODataServiceVersion, FITAtomDeserializer>(ODataServiceVersion.class);

  private static final Map<ODataServiceVersion, AtomSerializer> ATOM_SERIALIZER =
          new EnumMap<ODataServiceVersion, AtomSerializer>(ODataServiceVersion.class);

  private static final Map<ODataServiceVersion, ObjectMapper> JSON_MAPPER =
          new EnumMap<ODataServiceVersion, ObjectMapper>(ODataServiceVersion.class);

  private static final EnumMap<ODataServiceVersion, Metadata> METADATA =
          new EnumMap<ODataServiceVersion, Metadata>(ODataServiceVersion.class);

  protected static final Pattern MULTIKEY_PATTERN = Pattern.compile("(.*=.*,?)+");

  protected static final Map<String, Integer> SEQUENCE = new HashMap<String, Integer>();

  protected static final Map<String, String> MEDIA_CONTENT = new HashMap<String, String>();

  static {
    SEQUENCE.put("Customer", 1000);
    SEQUENCE.put("CustomerInfo", 1000);
    SEQUENCE.put("Car", 1000);
    SEQUENCE.put("Message", 1000);
    SEQUENCE.put("Order", 1000);
    SEQUENCE.put("ComputerDetail", 1000);
    SEQUENCE.put("AllGeoTypesSet", 1000);
    SEQUENCE.put("Orders", 1000);
    SEQUENCE.put("Customers", 1000);
    SEQUENCE.put("Person", 1000);
    SEQUENCE.put("RowIndex", 1000);
    SEQUENCE.put("Products", 1000);
    SEQUENCE.put("ProductDetails", 1000);
    SEQUENCE.put("PaymentInstrument", 10192);

    MEDIA_CONTENT.put("CustomerInfo", "CustomerinfoId");
    MEDIA_CONTENT.put("Car", "VIN");
    MEDIA_CONTENT.put("Car/Photo", null);
  }

  public static FITAtomDeserializer getAtomDeserializer(final ODataServiceVersion version) {
    if (!ATOM_DESERIALIZER.containsKey(version)) {
      ATOM_DESERIALIZER.put(version, new FITAtomDeserializer(version));
    }
    return ATOM_DESERIALIZER.get(version);
  }

  public static AtomSerializer getAtomSerializer(final ODataServiceVersion version) {
    if (!ATOM_SERIALIZER.containsKey(version)) {
      ATOM_SERIALIZER.put(version, new AtomSerializer(version, true));
    }

    return ATOM_SERIALIZER.get(version);
  }

  public static ObjectMapper getJSONMapper(final ODataServiceVersion version) {
    if (!JSON_MAPPER.containsKey(version)) {
      final ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

      mapper.setInjectableValues(new InjectableValues.Std()
              .addValue(Boolean.class, Boolean.TRUE)
              .addValue(ODataServiceVersion.class, version));

      mapper.setSerializerProvider(new InjectableSerializerProvider(mapper.getSerializerProvider(),
              mapper.getSerializationConfig()
              .withAttribute(ODataServiceVersion.class, version)
              .withAttribute(Boolean.class, Boolean.TRUE),
              mapper.getSerializerFactory()));

      JSON_MAPPER.put(version, mapper);
    }

    return JSON_MAPPER.get(version);
  }

  public static Metadata getMetadata(final ODataServiceVersion version) {
    if (!METADATA.containsKey(version)) {
      final InputStream is = Commons.class.getResourceAsStream("/" + version.name() + "/metadata.xml");

      METADATA.put(version, new Metadata(is, version));
    }

    return METADATA.get(version);
  }

  public static Map<String, String> getMediaContent() {
    return MEDIA_CONTENT;
  }

  public static String getEntityURI(final String entitySetName, final String entityKey) {
    // expected singleton in case of null key
    return entitySetName + (StringUtils.isNotBlank(entityKey) ? "(" + entityKey + ")" : "");
  }

  public static String getEntityBasePath(final String entitySetName, final String entityKey) {
    // expected singleton in case of null key
    return entitySetName + File.separatorChar
            + (StringUtils.isNotBlank(entityKey) ? getEntityKey(entityKey) + File.separatorChar : "");
  }

  public static String getLinksURI(
          final ODataServiceVersion version,
          final String entitySetName,
          final String entityId,
          final String linkName) throws IOException {
    return getEntityURI(entitySetName, entityId) + "/" + linkName;
  }

  public static String getLinksPath(
          final ODataServiceVersion version,
          final String entitySetName,
          final String entityId,
          final String linkName,
          final Accept accept) throws IOException {
    return getLinksPath(ODataServiceVersion.V30, getEntityBasePath(entitySetName, entityId), linkName, accept);

  }

  public static String getLinksPath(
          final ODataServiceVersion version, final String basePath, final String linkName, final Accept accept)
          throws IOException {
    try {
      return FSManager.instance(version)
              .getAbsolutePath(basePath + Constants.get(version, ConstantKey.LINKS_FILE_PATH)
              + File.separatorChar + linkName, accept);
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  public static String getEntityKey(final String entityId) {
    if (MULTIKEY_PATTERN.matcher(entityId).matches()) {
      // assume correct multi-key
      final String[] keys = entityId.split(",");
      final StringBuilder keyBuilder = new StringBuilder();
      for (String part : keys) {
        if (keyBuilder.length() > 0) {
          keyBuilder.append(" ");
        }
        keyBuilder.append(part.split("=")[1].replaceAll("'", "").trim());
      }
      return keyBuilder.toString();
    } else {
      return entityId.trim();
    }
  }

  public static InputStream getLinksAsATOM(final ODataServiceVersion version,
          final Map.Entry<String, Collection<String>> link) throws IOException {

    final StringBuilder builder = new StringBuilder();
    builder.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
    builder.append("<links xmlns=\"").append(Constants.get(version, ConstantKey.DATASERVICES_NS)).append("\">");

    for (String uri : link.getValue()) {
      builder.append("<uri>");
      if (URI.create(uri).isAbsolute()) {
        builder.append(uri);
      } else {
        builder.append(Constants.get(version, ConstantKey.DEFAULT_SERVICE_URL)).append(uri);
      }
      builder.append("</uri>");
    }

    builder.append("</links>");

    return IOUtils.toInputStream(builder.toString(), Constants.ENCODING);
  }

  public static InputStream getLinksAsJSON(final ODataServiceVersion version,
          final String entitySetName, final Map.Entry<String, Collection<String>> link)
          throws IOException {

    final ObjectNode links = new ObjectNode(JsonNodeFactory.instance);
    links.put(
            Constants.get(version, ConstantKey.JSON_ODATAMETADATA_NAME),
            Constants.get(version, ConstantKey.ODATA_METADATA_PREFIX) + entitySetName + "/$links/" + link.getKey());

    final ArrayNode uris = new ArrayNode(JsonNodeFactory.instance);

    for (String uri : link.getValue()) {
      final String absoluteURI;
      if (URI.create(uri).isAbsolute()) {
        absoluteURI = uri;
      } else {
        absoluteURI = Constants.get(version, ConstantKey.DEFAULT_SERVICE_URL) + uri;
      }
      uris.add(new ObjectNode(JsonNodeFactory.instance).put("url", absoluteURI));
    }

    if (uris.size() == 1) {
      links.setAll((ObjectNode) uris.get(0));
    } else {
      links.set("value", uris);
    }

    return IOUtils.toInputStream(links.toString(), Constants.ENCODING);
  }

  public static InputStream changeFormat(final InputStream is, final ODataServiceVersion version, final Accept target) {
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();

    try {
      IOUtils.copy(is, bos);
      IOUtils.closeQuietly(is);

      final ObjectMapper mapper = new ObjectMapper(
              new JsonFactory().configure(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS, true));
      final JsonNode node =
              changeFormat((ObjectNode) mapper.readTree(new ByteArrayInputStream(bos.toByteArray())), version, target);

      return IOUtils.toInputStream(node.toString(), Constants.ENCODING);
    } catch (Exception e) {
      LOG.error("Error changing format", e);
      return new ByteArrayInputStream(bos.toByteArray());
    } finally {
      IOUtils.closeQuietly(is);
    }
  }

  @SuppressWarnings("fallthrough")
  public static JsonNode changeFormat(final ObjectNode node, final ODataServiceVersion version, final Accept target) {
    final List<String> toBeRemoved = new ArrayList<String>();
    switch (target) {
      case JSON_NOMETA:
        // nometa + minimal
        toBeRemoved.add(Constants.get(version, ConstantKey.JSON_ODATAMETADATA_NAME));

      case JSON:
        // minimal
        toBeRemoved.add(Constants.get(version, ConstantKey.JSON_EDITLINK_NAME));
        toBeRemoved.add(Constants.get(version, ConstantKey.JSON_ID_NAME));
        toBeRemoved.add(Constants.get(version, ConstantKey.JSON_TYPE_NAME));

        final Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
          final Map.Entry<String, JsonNode> field = fields.next();
          if (field.getKey().endsWith(Constants.get(version, ConstantKey.JSON_MEDIA_SUFFIX))
                  || field.getKey().endsWith(Constants.get(version, ConstantKey.JSON_NAVIGATION_SUFFIX))
                  || field.getKey().endsWith(Constants.get(version, ConstantKey.JSON_TYPE_SUFFIX))) {
            toBeRemoved.add(field.getKey());
          } else if (field.getValue().isObject()) {
            changeFormat((ObjectNode) field.getValue(), version, target);
          } else if (field.getValue().isArray()) {
            for (final Iterator<JsonNode> subItor = field.getValue().elements(); subItor.hasNext();) {
              final JsonNode subNode = subItor.next();
              if (subNode.isObject()) {
                changeFormat((ObjectNode) subNode, version, target);
              }
            }
          }
        }
      case JSON_FULLMETA:
        //ignore: no changes
        break;

      default:
        throw new UnsupportedOperationException(target.name());
    }
    node.remove(toBeRemoved);

    return node;
  }

  public static String getETag(final String basePath, final ODataServiceVersion version) throws Exception {
    try {
      final InputStream is = FSManager.instance(version).readFile(basePath + "etag", Accept.TEXT);
      if (is.available() <= 0) {
        return null;
      } else {
        final String etag = IOUtils.toString(is);
        IOUtils.closeQuietly(is);
        return etag;
      }
    } catch (Exception e) {
      return null;
    }
  }

  public static Map.Entry<String, String> parseEntityURI(final String uri) {
    final String relPath = uri.substring(uri.lastIndexOf("/"));
    final int branchIndex = relPath.indexOf('(');

    final String es;
    final String eid;

    if (branchIndex > -1) {
      es = relPath.substring(0, branchIndex);
      eid = relPath.substring(branchIndex + 1, relPath.indexOf(')'));
    } else {
      es = relPath;
      eid = null;
    }

    return new SimpleEntry<String, String>(es, eid);
  }
}
