/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */

/**
 * A tool that provides functionality to introspect a WSDL.
 * 
 * @moduleGraph
 * @since 1.1,7
 */
module org.mule.wsdl.parser {

  exports org.mule.wsdl.parser;
  exports org.mule.wsdl.parser.exception;
  exports org.mule.wsdl.parser.locator;
  exports org.mule.wsdl.parser.model;
  exports org.mule.wsdl.parser.model.operation;

  requires org.mule.runtime.metadata.model.api;
  requires org.mule.runtime.metadata.model.xml;
  requires org.mule.runtime.metadata.model.persistence;

  requires com.google.gson;
  requires transitive mule.wsdl4j;
  requires transitive Saxon.HE;

  requires org.apache.commons.lang3;
  requires org.slf4j;

  requires transitive kotlin.stdlib;
  requires java.xml;

}
