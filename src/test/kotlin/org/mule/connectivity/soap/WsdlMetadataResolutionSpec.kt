package org.mule.connectivity.soap

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasSize
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.mule.metadata.api.model.impl.DefaultObjectFieldType
import org.mule.metadata.api.model.impl.DefaultObjectType
import org.mule.wsdl.parser.WsdlParser
import javax.xml.namespace.QName

class WsdlMetadataResolutionSpec : Spek({

    given("a wsdl file") {
        it("should collect the schema embedded in the wsdl types tag and assert the extracted content using namespaces defined in the wsdl") {
            val metaData = WsdlParser.parse(TestUtils.getResourcePath("wsdl/namespaces.wsdl")).loader.value
                    .load(QName("http://namespaces.soaplite.com/perl", "oaAddress"), "oaAddress")
            assertThat(((((metaData.get() as DefaultObjectType).fields as java.util.ArrayList<*>)[0] as DefaultObjectFieldType)
                    .value as DefaultObjectType).fields, hasSize(19))
        }

    }
})

