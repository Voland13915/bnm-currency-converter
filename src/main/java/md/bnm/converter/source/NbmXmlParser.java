package md.bnm.converter.source;

import md.bnm.converter.model.Currency;
import md.bnm.converter.model.PublishedRate;
import md.bnm.converter.service.RateLoadErrorType;
import md.bnm.converter.service.RateLoadException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Secure JAXP XML parser for official NBM exchange rate responses.
 * Disables DTDs and external entities to prevent XXE.
 * Distinguishes empty rate responses (valid XML with no Valute elements)
 * from malformed responses (syntax errors, missing root Date, or missing/invalid fields).
 */
public class NbmXmlParser {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public record ParsedXml(LocalDate date, Map<String, PublishedRate> rates) {
        public ParsedXml {
            rates = Collections.unmodifiableMap(new LinkedHashMap<>(rates));
        }

        public boolean hasRates() {
            return !rates.isEmpty();
        }
    }

    private final DocumentBuilderFactory factory;

    public NbmXmlParser() {
        this.factory = createSecureFactory();
    }

    private static DocumentBuilderFactory createSecureFactory() {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(false);
        try {
            // Disallow DTDs completely
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            // Disable external DTD and entities
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            dbf.setXIncludeAware(false);
            dbf.setExpandEntityReferences(false);
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("Failed to configure secure XML parser factory", e);
        }

        try {
            dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        } catch (IllegalArgumentException ignored) {
            // Attribute may not be supported by all JAXP implementations
        }

        return dbf;
    }

    private DocumentBuilder createDocumentBuilder() throws RateLoadException {
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new StrictErrorHandler());
            return builder;
        } catch (ParserConfigurationException e) {
            throw new RateLoadException(RateLoadErrorType.MALFORMED_RESPONSE,
                    "Ошибка конфигурации XML-парсера: " + e.getMessage(), e);
        }
    }

    public ParsedXml parse(String xmlContent) throws RateLoadException {
        if (xmlContent == null || xmlContent.isBlank()) {
            throw new RateLoadException(RateLoadErrorType.MALFORMED_RESPONSE,
                    "Получен пустой ответ XML от сервера НБМ.");
        }
        try {
            DocumentBuilder builder = createDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlContent)));
            return parseDocument(doc);
        } catch (SAXException | IOException e) {
            throw new RateLoadException(RateLoadErrorType.MALFORMED_RESPONSE,
                    "Некорректный синтаксис XML в ответе НБМ: " + e.getMessage(), e);
        }
    }

    public ParsedXml parse(InputStream inputStream) throws RateLoadException {
        if (inputStream == null) {
            throw new RateLoadException(RateLoadErrorType.MALFORMED_RESPONSE,
                    "Поток данных ответа XML отсутствует (null).");
        }
        try {
            DocumentBuilder builder = createDocumentBuilder();
            Document doc = builder.parse(inputStream);
            return parseDocument(doc);
        } catch (SAXException | IOException e) {
            throw new RateLoadException(RateLoadErrorType.MALFORMED_RESPONSE,
                    "Некорректный синтаксис XML в ответе НБМ: " + e.getMessage(), e);
        }
    }

    private ParsedXml parseDocument(Document doc) throws RateLoadException {
        Element root = doc.getDocumentElement();
        if (root == null || !"ValCurs".equalsIgnoreCase(root.getTagName())) {
            throw new RateLoadException(RateLoadErrorType.MALFORMED_RESPONSE,
                    "Некорректный корневой элемент XML ответа (ожидался ValCurs).");
        }

        String dateAttr = root.getAttribute("Date");
        if (dateAttr == null || dateAttr.isBlank()) {
            throw new RateLoadException(RateLoadErrorType.MALFORMED_RESPONSE,
                    "Отсутствует обязательный атрибут Date в элементе ValCurs.");
        }

        LocalDate responseDate;
        try {
            responseDate = LocalDate.parse(dateAttr.trim(), DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new RateLoadException(RateLoadErrorType.MALFORMED_RESPONSE,
                    "Некорректный формат даты в ответе НБМ: " + dateAttr, e);
        }

        NodeList childNodes = root.getChildNodes();
        Map<String, PublishedRate> rates = new LinkedHashMap<>();

        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element valuteElem = (Element) node;
            if (!"Valute".equalsIgnoreCase(valuteElem.getTagName())) {
                continue;
            }

            PublishedRate rate = parseValuteElement(valuteElem);
            String code = rate.currencyCode().toUpperCase();
            rates.put(code, rate);
        }

        return new ParsedXml(responseDate, rates);
    }

    private PublishedRate parseValuteElement(Element valuteElem) throws RateLoadException {
        String numCode = getChildText(valuteElem, "NumCode");
        String charCode = getChildText(valuteElem, "CharCode");
        String nominalStr = getChildText(valuteElem, "Nominal");
        String name = getChildText(valuteElem, "Name");
        String valueStr = getChildText(valuteElem, "Value");

        if (charCode == null || charCode.isBlank()) {
            throw new RateLoadException(RateLoadErrorType.MALFORMED_RESPONSE,
                    "Элемент Valute не содержит обязательного поля CharCode.");
        }
        if (nominalStr == null || nominalStr.isBlank()) {
            throw new RateLoadException(RateLoadErrorType.MALFORMED_RESPONSE,
                    "Элемент Valute (" + charCode + ") не содержит обязательного поля Nominal.");
        }
        if (valueStr == null || valueStr.isBlank()) {
            throw new RateLoadException(RateLoadErrorType.MALFORMED_RESPONSE,
                    "Элемент Valute (" + charCode + ") не содержит обязательного поля Value.");
        }
        if (name == null || name.isBlank()) {
            throw new RateLoadException(RateLoadErrorType.MALFORMED_RESPONSE,
                    "Элемент Valute (" + charCode + ") не содержит обязательного поля Name.");
        }

        int nominal;
        try {
            nominal = Integer.parseInt(nominalStr.trim());
        } catch (NumberFormatException e) {
            throw new RateLoadException(RateLoadErrorType.MALFORMED_RESPONSE,
                    "Некорректный номинал валюты " + charCode + ": " + nominalStr, e);
        }
        if (nominal <= 0) {
            throw new RateLoadException(RateLoadErrorType.MALFORMED_RESPONSE,
                    "Номинал валюты " + charCode + " должен быть положительным числом, получено: " + nominal);
        }

        BigDecimal value;
        try {
            value = new BigDecimal(valueStr.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new RateLoadException(RateLoadErrorType.MALFORMED_RESPONSE,
                    "Некорректное значение курса валюты " + charCode + ": " + valueStr, e);
        }

        Currency currency = new Currency(charCode.trim().toUpperCase(), name.trim(), numCode != null ? numCode.trim() : null);
        return new PublishedRate(currency, nominal, value);
    }

    private String getChildText(Element parent, String tagName) {
        NodeList list = parent.getElementsByTagName(tagName);
        if (list.getLength() == 0) {
            return null;
        }
        Node node = list.item(0);
        return node.getTextContent();
    }

    private static class StrictErrorHandler implements ErrorHandler {
        @Override
        public void warning(SAXParseException exception) {
            // Ignored
        }

        @Override
        public void error(SAXParseException exception) throws SAXException {
            throw exception;
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            throw exception;
        }
    }
}
