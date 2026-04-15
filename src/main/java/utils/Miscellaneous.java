package utils;

import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class Miscellaneous {

    public static Date toDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return formatter.parse(dateString);
        } catch (ParseException e) {
            throw new RuntimeException("errore critico: formato data non valido '" + dateString + "'. USA: yyyy-MM-dd", e);
        }
    }
}
