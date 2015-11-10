package svgspike;

import com.google.common.primitives.Doubles;
import org.apache.batik.dom.svg.*;
import org.apache.batik.util.XMLResourceDescriptor;

import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import com.google.common.io.Files;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.svg.SVGMatrix;
import org.w3c.dom.svg.SVGPoint;
import org.w3c.dom.svg.SVGSVGElement;


/**
 * Created by pisarenko on 10.11.2015.
 */
public final class BatikTest {
    @Test
    public void test() throws XPathExpressionException {
        try {
            final File initialFile =
                    new File("src/test/resources/scene05_signs.svg");
            InputStream sceneFileStream = Files.asByteSource(initialFile).openStream();


            String parser = XMLResourceDescriptor.getXMLParserClassName();
            SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(parser);
            String uri = "http://www.example.org/diagram.svg";
            final SVGOMDocument doc = (SVGOMDocument) f.createDocument(
                    uri, sceneFileStream);

            final NodeList nodes =
                    doc.getDocumentElement().getElementsByTagName("g");
            SVGOMGElement signSouth = null;


            for (int i=0; (i < nodes.getLength()) && (signSouth == null); i++) {
                final Node curNode = nodes.item(i);
                final Node id = curNode.getAttributes().getNamedItem("id");
                if ("signS".equals(id.getTextContent())) {
                    signSouth = (SVGOMGElement) curNode;
                }

                System.out.println("curNode: " + nodes);
            }
            System.out.println("signSouth: " + signSouth);

            final NodeList rectNodes = signSouth.getElementsByTagName("rect");
            System.out.println("rectNodes: " + rectNodes);

            SVGOMRectElement rectNode = (SVGOMRectElement) rectNodes.item(0);

            System.out.println("rectNode: " + rectNode);

            final double x = doubleNodeValue(rectNode.getAttributeNode("x"));
            final double y = doubleNodeValue(rectNode.getAttributeNode("y"));

            final SVGSVGElement docElem = (SVGSVGElement)
                    doc.getDocumentElement();
            final SVGPoint svgPoint = docElem.createSVGPoint();

            svgPoint.setX((float) x);
            svgPoint.setY((float) y);

            System.out.println("signSouth.getScreenCTM(): " +
                    signSouth.getScreenCTM());
            double signSouthX = magicallyCalculateXCoordinate();
            double signSouthY = magicallyCalculateYCoordinate();

            Assert.assertEquals(109.675, signSouthX, 0.0000001);
            Assert.assertEquals(533.581, signSouthY, 0.0000001);
        } catch (IOException ex) {
            Assert.fail(ex.getMessage());
        }
    }

    private double magicallyCalculateYCoordinate() {
        return 0;
    }

    private double magicallyCalculateXCoordinate() {
        return 0;
    }

    public double doubleNodeValue(final Node cx) {
        return Doubles.tryParse(cx.getNodeValue()).doubleValue();
    }

}
