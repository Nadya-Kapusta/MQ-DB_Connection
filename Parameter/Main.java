package Parametrisation;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import java.io.IOException;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) throws Throwable {
        Actions action = new Actions();
        action.init();
        long pacing = 300;
        for (int i = 0; i <= 1; i++){
            long startIter = System.currentTimeMillis();
            action.actions();
            long endIter = System.currentTimeMillis();
            if ((endIter - startIter) <= pacing) Thread.sleep(pacing - (endIter - startIter));
        }
        action.end();
    }
}
