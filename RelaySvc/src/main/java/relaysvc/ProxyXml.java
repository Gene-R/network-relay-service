/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package relaysvc;

import com.sun.org.apache.xml.internal.utils.DefaultErrorHandler;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 *
 * @author eugener
 */
public class ProxyXml {

    final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

    private final Configuration cfg = Configuration.getInstance();
    private final Logger logger = cfg.getLogger();
    private final String configXml;
    private final List<ProxyConfig> proxyConfigs = new ArrayList<ProxyConfig>();
    private final static String RELAY_SVC_XSD = "/relaysvc.xsd";

    public ProxyXml(String configXml) {
        this.configXml = configXml;
    }

    public void load() {

        try {
            logger.debug("Loading proxy XML configuration from: " + configXml);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setIgnoringElementContentWhitespace(true);
            factory.setNamespaceAware(true);

            InputStream is = this.getClass().getResourceAsStream(RELAY_SVC_XSD);
            if (is == null) {
                throw new IOException("Could not find XML Schema Definition file: " + RELAY_SVC_XSD);
            }
            Schema schema = schemaFactory.newSchema(new StreamSource(is));
            factory.setSchema(schema);

            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new DefaultErrorHandler());
            Document doc = builder.parse(new InputSource(configXml));
            XPath xpath = XPathFactory.newInstance().newXPath();
            XPathExpression expr = xpath.compile("/relaysvc/proxy");
            NodeList proxies = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
            for (int i = 0; i < proxies.getLength(); i++) {
                Node proxy = proxies.item(i);
                StringBuilder sb = new StringBuilder();
                String type = (String) xpath.compile("type").evaluate(proxy, XPathConstants.STRING);
                sb.append(type);
                sb.append("|");

                String src = (String) xpath.compile("src").evaluate(proxy, XPathConstants.STRING);
                sb.append(src);
                sb.append("|");

                NodeList dsts = (NodeList) xpath.compile("dsts/dst").evaluate(proxy, XPathConstants.NODESET);
                boolean empty = true;
                for (int j = 0; j < dsts.getLength(); j++) {
                    if (!empty) {
                        sb.append(",");
                    }
                    String dst = dsts.item(j).getTextContent();
                    sb.append(dst);
                    empty = false;
                }
                sb.append("|");
                String policy = (String) xpath.compile("policy").evaluate(proxy, XPathConstants.STRING);
                sb.append(policy);
                sb.append("|");
                empty = true;
                NodeList flags = (NodeList) xpath.compile("flags/flag").evaluate(proxy, XPathConstants.NODESET);
                for (int j = 0; j < flags.getLength(); j++) {
                    if (!empty) {
                        sb.append(",");
                    }
                    String flag = flags.item(j).getTextContent();
                    sb.append(flag);
                    empty = false;
                }
                proxyConfigs.add(new ProxyConfig(sb.toString(), configXml));
            }
            System.out.println();
        } catch (Exception ex) {
            logger.error("ProxyXml.load(): " + ex.getMessage());
        }
    }

    public List<ProxyConfig> getProxyConfigs() {
        return proxyConfigs;
    }

}
