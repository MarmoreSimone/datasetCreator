package utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Scanner;

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

    public static HashSet<String> retrieveTicketsID(){

        HashSet<String> ticketList = new HashSet<>();

        File file = new File("buggyTicket.txt");

        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                String riga = scanner.nextLine().trim();
                if (!riga.isEmpty()) {
                    ticketList.add(riga);
                }
            }

        } catch (FileNotFoundException _) {
            System.err.println("Errore: File non trovato!");
        }

        return ticketList;
    }

    }



