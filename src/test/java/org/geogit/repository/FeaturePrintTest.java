package org.geogit.repository;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.custommonkey.xmlunit.XMLAssert;
import org.geogit.storage.ObjectWriter;
import org.geogit.storage.WrappedSerialisingFactory;
import org.geogit.test.RepositoryTestCase;
import org.opengis.feature.Feature;
import org.w3c.dom.Document;

public class FeaturePrintTest extends RepositoryTestCase {

	public void testPrint() throws Exception {
		WrappedSerialisingFactory fact = WrappedSerialisingFactory.getInstance();
		
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		ObjectWriter<Feature> writ = fact.createFeatureWriter(lines1);
		writ.write(bout);
		
		byte[] bytes = bout.toByteArray();
		
		fact.createBlobPrinter().print(bytes, System.out);
		
		bout = new ByteArrayOutputStream();
		fact.createBlobPrinter().print(bytes, new PrintStream(bout));
	
		Document dom = DocumentBuilderFactory.newInstance().newDocumentBuilder()
				.parse(new ByteArrayInputStream(bout.toByteArray()));
		assertNotNull(dom);
		XMLAssert.assertXpathExists("/feature/string", dom);
		XMLAssert.assertXpathEvaluatesTo(lines1.getProperty("sp").getValue().toString(), 
				"/feature/string", dom);
		XMLAssert.assertXpathExists("/feature/int", dom);
		XMLAssert.assertXpathEvaluatesTo(lines1.getProperty("ip").getValue().toString(),
				"/feature/int", dom);
		XMLAssert.assertXpathExists("/feature/wkb", dom);
		XMLAssert.assertXpathEvaluatesTo("LINESTRING (1 1, 2 2)",
				"/feature/wkb", dom);
		
		
	}

	@Override
	protected void setUpInternal() throws Exception {
		return;
	}

}
