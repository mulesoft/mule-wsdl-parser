/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */

/**
 * A tool that provides functionality to introspect a WSDL.
 * @moduleGraph
 * @since 1.5
 */
module org.mule.wsdl.parser {

  requires org.mule.runtime.metadata.model.api;
  requires org.mule.runtime.metadata.model.xml;
  requires org.mule.runtime.metadata.model.persistence;
  
  requires com.google.gson;
  requires org.slf4j;
  requires mule.wsdl4j;
  
  requires kotlin.stdlib;
  requires java.xml;

}
