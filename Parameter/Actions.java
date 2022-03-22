package Parametrisation;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.print.attribute.standard.Destination;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.font.TextMeasurer;
import java.beans.ExceptionListener;
import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class Actions {
    String mqHOST;
    String mqPORT;
    String mqQUEUENAME;
    String mqMANAGER;
    String mqCHANNEL;

    String thin = "@***-**.**.com";
    String port = "1234";
    String sid = "Meow1";
    String username = "tro";
    String password = "trololo";

    Statement stmt;
    Statement notificationStmt;
    Connection conn;
    PreparedStatement preparedStatement;

    Connection mqConnection;
    Session mqSession;
    Destination mqDestination;
    MessageProducer mqMessageProducer;

    DocumentBuilderFactory dbf;
    DocumentBuilder db;
    Document doc;

    TransformerFactory transformerFactory;
    Transformer transformer;

    String line_csv;
    String[] values = new String[800];

    public void setMqConnection() {

        try {
            MQConnectionFactry mqcf = new MQConnectionFactory();
            mqcf.setHostName(mqHOST);
            mqcf.setQueueManager(mqMANAGER);
            mqcf.setPort(Integer.parseInt(mqPORT));
            mqcf.setChannel(mqCHANNEL);
            mqcf.setTransportType (WMQ.AUTO_ACKNOWLEDGE);
            mqConnection = mqcf.createCOnnection();
            mqConnection.setExceptionListener(new ExceptionListener()) {
                public void onException(JMSException jmse) {jmse.printStackTrace(System.err);}
            });
            mqConnection.start();
            mqSession = mqConnection.createSession(true, Session.AUTO_ACKNOWLEDGE);
            System.out.println("MQ connection initialized");
        } catch (JMSException e){
            System.err.println("Error occured while initializing connection!");
            e.printStackTrace;
        }
    }

    public void setDbConnection() {
        try {
            Class.forName("orale.jdbc.driver.OracleDriver");
            System.out.println("Successfully connected");
        } catch (ClassNotFoundException e){
            System.out.println("Not connected");
            e.printStackTrace();
        }
        conn = DriverManager.getConnction("jdbc:oracle:thin:" + thin + Integer.parseInt(port) + "/" + sid, username, password);
        stmt = conn.createStatement();
        notificationStmt = conn.createStatement();
        if (conn != null) {
            System.out.println("Successfully connected to DB");
        } else {
            System.out.println("Connection error!!!");
        }

        conn.setAutoCommit(false);

    }

    public int init() throws Throwable {

        mqHOST = "***";
        mqPORT = "***";
        mqQUEUENAME = "***";
        mqMANAGER = "***";
        mqCHANNEL = "***";

        dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        db = dbf.newDocumentBuilder();
        doc = db.parse(new File("smth.xml"));
        transformerFactory = TransformerFactory.newInstance();
        transformer = transformerFactory.newTransformer();

        BufferedReader br = new BufferedReader(new FileReader("smth.csv"));
        while ((line_csv = br.readLine()) != null) {
            values = line_csv.split(";");
        }
        setDbConnection();

        String query = "Insert into TABLE (id, regid, status) values (?, ?, 0)";
        preparedStatement = conn.prepareStatement(query);

        setMqConnection();
        return 0;
    }

    public int actions() {
        char[] symbols = {'a','c','d','s','f','1','2','3','4'};
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        Random rand = new Random();
        int randInt = rand.nextInt(values.length);

        String rqUid = RandomStringUtils.random(32, symbols);
        String rqTm = sdf.format(new Date());
        String acctId = values[randInt];
        String id = RandomStringUtils.random(6, symbols);

        ((Element) doc.getElementsByTagName("RqUID").item(0)).setAttribute("Value", rqUid);
        ((Element) doc.getElementsByTagName("RqTm").item(0)).setAttribute("Value", rqTm);
        ((Element) doc.getElementsByTagName("AcctId").item(0)).setAttribute("Value", acctId);

        StringWriter stringWriter = new StringWriter();
        try {
            transformer.transform(new DOMSource(doc), new StreamResult(stringWriter));
        } catch (TransformerException e){
            e.printStackTrace();
        }
        String msg = stringWriter.toString();

/////////////// sending msg to MQ ///////////////////
        try {
            mqDestination = mqSession.createQueue(mqQUEUENAME);
            mqMessageProducer = mqSession.createProducer(mqDestination);
            TextMessage textMessage = mqSession.createTextMessage(msg);
            mqMessageProducer.send(textMessage);
            mqMessageProducer.close();
            mqSession.commit();
            System.out.println("Message sent");
        } catch (JMSException e) {
            e.printStackTrace();
        }

/////////////// writing info into the SQL table ///////////////////
        try{
            preparedStatement.setString(1, id);
            preparedStatement.setString(2, acctId);
        } catch (SQLException e) {
            throwables.printStackTrace();
        }
        try {
            preparedStatement.execute();
        } catch (SQLException e) {
            throwables.printStackTrace();
        }
        try {
            conn.commit();
        } catch (SQLException e){
            throwables.printStackTrace();
        }

        return 0;
    }

    public void end() throws SQLException {
        mqConnection.close();
        conn.close();
    }
}
