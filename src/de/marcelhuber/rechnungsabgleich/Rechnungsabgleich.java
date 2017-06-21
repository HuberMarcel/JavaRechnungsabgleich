package de.marcelhuber.rechnungsabgleich;

import java.io.*;
import java.nio.file.*;
import java.nio.file.FileSystem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.*;

/**
 *
 * @author Marcel Huber
 */
public class Rechnungsabgleich {

    private boolean debuggingForFileAndFolders; // wird automatisch mit false initialisiert

    private String[] versandproduktTyp = {"Standard", "Kompakt", "Gross"};
    private int[] versandproduktMaxBlattzahlOhneWeitereBlaetter = {1, 4, 10};
    // bitte beachten, dass diese zwei Zeilen in der richtigen Reihenfolge zusammengehören
    // siehe Initialisierungsblock
    private String[] spezialisierungsTyp = {"einzelSendung", "alleBlaetter", "weitereBlaetter"};
    private String selectedColumnName = "Versandprodukt";
    private Map<String, Map<String, Long>> ergebnisseVsPrTyp;                   // enthält die Ergebnisse der VersandproduktTypen
    private Map<String, Integer> versandproduktMaxBlattzahlOhneWeitereBlaetterMap;

    {
        // Initialisierung der Map, die beinhaltet, bei welchem Versandtyp ab wie vielen Blättern diese als weitere Blätter gezählt werden
        versandproduktMaxBlattzahlOhneWeitereBlaetterMap = new HashMap<>();
        for (int k = 0; k < versandproduktTyp.length; k++) {
            versandproduktMaxBlattzahlOhneWeitereBlaetterMap.put(versandproduktTyp[k],
                    versandproduktMaxBlattzahlOhneWeitereBlaetter[k]);
        }
        // Vorinitialisierung der Map für die einzelnen Kennzahlen
        ergebnisseVsPrTyp = new HashMap<>();
        for (String versandprodukt : versandproduktTyp) {
            Map<String, Long> inBlocMap = new HashMap<>();
            for (String spezialisierung : spezialisierungsTyp) {
                inBlocMap.put(spezialisierung, 0L);
                ergebnisseVsPrTyp.put(versandprodukt, inBlocMap);
//                System.out.println(ergebnisseVsPrTyp.get(versandprodukt).keySet());
//                Pause(2000);
            }
        }
    }

    private FileSystem fs = FileSystems.getDefault();
    private String pfadnameAktuellesVerzeichnis = new File("").getAbsolutePath();
    private Path pfadAktuellesVerzeichnis = fs.getPath(pfadnameAktuellesVerzeichnis);
    private Path pathFolderCSVDatei = fs.getPath(pfadAktuellesVerzeichnis + "/../../Projekte");
    private String csvDateiname = "/newSimulationData.csv";
    private Path pathCSVDatei = fs.getPath(pathFolderCSVDatei.toString(), csvDateiname);
    private String separator = ",";
    private List<String[]> tabelle = new ArrayList<>();
    private Map<String, Integer> selectedColumnNumberMap;
    private String[] namesOfSelectedColumnNumbers = {"Blattzahl", "Versandprodukt"};
    private String[] headerOfCSV;

    public static void main(String[] args) {
        Rechnungsabgleich dummy = new Rechnungsabgleich();
        dummy.go();
    }

    private void go() {
        startCalculation(pathCSVDatei);
        showResults();
    }

    private void showResults() {
        for (String versandprodukt : versandproduktTyp) {
            for (String spezialisierung : spezialisierungsTyp) {
                System.out.print("Berechnete Zahl für " + versandprodukt
                        + " / " + spezialisierung + ": ");
                System.out.println(ergebnisseVsPrTyp.get(versandprodukt).get(spezialisierung));
            }
            System.out.println("");
        }
    }

    private void startCalculation(Path pfadZurCSVDatei) {
        if (debuggingForFileAndFolders) {
//        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(pfadZurCSVDatei.getParent());) {
            try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(pathFolderCSVDatei);) {
                for (Path dateiName : dirStream) {
                    System.out.println(dateiName);
                }
            } catch (IOException ex) {
                Logger.getLogger(Rechnungsabgleich.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        File parentFolder = new File(pfadZurCSVDatei.getParent().toString());
        if (debuggingForFileAndFolders) {
            System.out.println("Existiert das Elternverzeichnis?          " + parentFolder.exists());
            System.out.println("Ist das Elternverzeichnis lesbar?         " + parentFolder.canRead());
            System.out.println("Ist das Elternverzeichnis überschreibbar? " + parentFolder.canWrite());
            parentFolder.setReadable(true);
            parentFolder.setWritable(true);
            File file = new File(pfadZurCSVDatei.toString());
            file.setReadable(true);
            file.setWritable(true);
//        System.out.println("XXXXXXXXXX: " + pfadZurCSVDatei.toString());
            System.out.println("Existiert die Datei?                      " + file.exists());
            System.out.println("Ist die Datei lesbar?                     " + file.canRead());
            System.out.println("Ist die Datei überschreibbar?             " + file.canWrite());
        }
        String zeile;
        try (
                FileReader fr = new FileReader(pfadZurCSVDatei.toString());
                BufferedReader br = new BufferedReader(fr);) {
            headerOfCSV = br.readLine().split(separator);
            // wir lesen die csv-Datei zeilenweise ein - das hier ist die Kopfzeile als Array
            findAndSetSelectedColumnNumbers(headerOfCSV);
            while ((zeile = br.readLine()) != null) {
                calculateErgebnisseVsPrTyp(zeile.split(separator));
            }
        } catch (IOException ex) {
            Logger.getLogger(Rechnungsabgleich.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void findAndSetSelectedColumnNumbers(String[] zeileCSV) {
        selectedColumnNumberMap = new HashMap<>();
        if (headerOfCSV != null) {
            for (int k = 1; k <= headerOfCSV.length; k++) {
                for (String name : namesOfSelectedColumnNumbers) {
                    if (headerOfCSV[k - 1].equalsIgnoreCase(name)) {
                        selectedColumnNumberMap.put(name, k);
                    }
                }
            }
//            System.out.println(selectedColumnNumberMap.keySet());
//            System.out.println(selectedColumnNumberMap.values());
//            System.out.println("Versandprodukt steht in Spalte Nr.: " + selectedColumnNumberMap.get("Versandprodukt"));
//            System.out.println("Blattzahl steht in Spalte Nr.:      " + selectedColumnNumberMap.get("Blattzahl"));
        }
    }

    private void calculateErgebnisseVsPrTyp(String[] zeilenDaten) {
        String versandproduktDerZeile = zeilenDaten[selectedColumnNumberMap.get("Versandprodukt") - 1];
        // Erinnerung: Die Spaltennummer wurde 1-basiert gezählt, das zeilenDatenArray ist aber 0-basiert, daher die -1 am Ende
//        System.out.println("Versandprodukt                 : " + versandproduktDerZeile);
        try {
            long blattzahlDerZeile = Long.parseLong(zeilenDaten[selectedColumnNumberMap.get("Blattzahl") - 1]);
            // Erinnerung: Die Spaltennummer wurde 1-basiert gezählt, das zeilenDatenArray ist aber 0-basiert, daher die -1 am Ende
//            System.out.println("Anzahl der Blätter dieser Zeile: " + blattzahlDerZeile);
            long valueInMapActually = ergebnisseVsPrTyp.get(versandproduktDerZeile).get("einzelSendung");
            // aktueller Wert des Versandprodukts der Form Einzelsendung in der Map
            ergebnisseVsPrTyp.get(versandproduktDerZeile).put("einzelSendung", ++valueInMapActually);
            // WARNUNG: PREINCREMENT IST HIER WICHTIG!!
            // spezialisierungsTyp[0] ist "einzelSendung"  -- spezialisierungsTyp[1] ist weitereBlaetter
            // -- spezialisierungsTyp[0] ist "alleBlaetter"
            valueInMapActually = ergebnisseVsPrTyp.get(versandproduktDerZeile).get("alleBlaetter");
            valueInMapActually += blattzahlDerZeile;
            ergebnisseVsPrTyp.get(versandproduktDerZeile).put("alleBlaetter", valueInMapActually);
            long zusatzBlaetter;
            long oberGrenze = versandproduktMaxBlattzahlOhneWeitereBlaetterMap.get(versandproduktDerZeile);
            if (blattzahlDerZeile >= oberGrenze) {
                zusatzBlaetter = blattzahlDerZeile - oberGrenze;
//                System.out.println("Versandprodukt: " + versandproduktDerZeile);
//                System.out.println("Obergrenze    : " + oberGrenze);
//                System.out.println("Differenz     : " + zusatzBlaetter);
//                Pause(1_000);
                valueInMapActually = ergebnisseVsPrTyp.get(versandproduktDerZeile).get("weitereBlaetter");
                ergebnisseVsPrTyp.get(versandproduktDerZeile).put("weitereBlaetter", valueInMapActually + zusatzBlaetter);
            }
        } catch (NumberFormatException ex) {
            System.out.println("Kontrollieren Sie die Spaltennummer für Blattzahl!");
        }
//        ergebnisseVsPrTyp.get()
    }

    private void Pause(long timeInMillis) {
        try {
            Thread.sleep(timeInMillis);
        } catch (InterruptedException ex) {
            Logger.getLogger(Rechnungsabgleich.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
