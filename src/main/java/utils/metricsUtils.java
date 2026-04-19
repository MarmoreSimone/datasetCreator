package utils;

import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class metricsUtils {

    public static boolean isCommitAFix(String comment, HashSet<String> bugTickets) {

        //definisco la regex per riconoscere nel testo la presenza di una stringa del formato OPENJPA-XXXX
        Pattern pattern = Pattern.compile("OPENJPA-\\d+", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(comment);

        while (matcher.find()) {
            String ticketFound = matcher.group(); // Es: "OPENJPA-1422"

            //verifico se il ticket si trova nella lista di quelli buggy
            if (bugTickets.contains(ticketFound)) return true;
        }
        return false;
    }

}
