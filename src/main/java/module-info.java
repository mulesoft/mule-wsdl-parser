/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */

/**
 * A tool that provides functionality to introspect a WSDL.
 * 
 * @moduleGraph
 * @since 1.1.7 (this version is modularized because it is the one used by the SOAP service.)
 */
module org.mule.wsdl.parser {

  exports org.mule.wsdl.parser;
  exports org.mule.wsdl.parser.exception;
  exports org.mule.wsdl.parser.locator;
  exports org.mule.wsdl.parser.model;
  exports org.mule.wsdl.parser.model.operation;

  requires org.mule.runtime.metadata.model.api;
  requires org.mule.runtime.metadata.model.xml;

  requires org.apache.cxf.wsdl;
  requires mule.wsdl4j;
  requires Saxon.HE;

  requires org.apache.commons.lang3;
  requires org.slf4j;

  requires transitive kotlin.stdlib;
  requires java.xml;

}
