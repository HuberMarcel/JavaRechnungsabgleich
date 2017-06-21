package de.marcelhuber.rechnungsabgleich;

import java.io.*;
import java.nio.file.*;
import java.nio.file.FileSystem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.*;

/**
 *
 * @author Marcel Huber
 */
public class Rechnungsabgleich {

    private FileSystem fs = FileSystems.getDefault();
    private String pfadnameAktuellesVerzeichnis = new File("").getAbsolutePath();
    private Path pfadAktuellesVerzeichnis = fs.getPath(pfadnameAktuellesVerzeichnis);
    private Path pathFolderCSVDatei = fs.getPath(pfadAktuellesVerzeichnis + "/../../Projekte");
    private String csvDateiname = "/test.csv";
    private Path pathCSVDatei = fs.getPath(pathFolderCSVDatei.toString(), csvDateiname);
    private String separator = ",";
    private List<String[]> tabelle = new ArrayList<>();
    private String selectedColumnName = "Versandprodukt";
    private int selectedColumnNumber;                                           // die Spaltennummer von selectedColumnName - allerdings 1-basiert 
    private String[] versandproduktTypen = {"Standard", "Kompakt", "Gross"};
    private Map<String, Long> ergebnisVsPrTypen;
    private Map<String, Long> ergebnisVsPrTypenMitWeiterenBlaettern;

    public static void main(String[] args) {
        Rechnungsabgleich dummy = new Rechnungsabgleich();
        dummy.go();
    }

    private void go() {
        openAndRead(pathCSVDatei);
    }

    private void openAndRead(Path pfadZurCSVDatei) {
//        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(pfadZurCSVDatei.getParent());) {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(pathFolderCSVDatei);) {
            for (Path dateiName : dirStream) {
                System.out.println(dateiName);
            }
        } catch (IOException ex) {
            Logger.getLogger(Rechnungsabgleich.class.getName()).log(Level.SEVERE, null, ex);
        }
        File parentFolder = new File(pfadZurCSVDatei.getParent().toString());
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
        try (
                FileReader fr = new FileReader(pfadZurCSVDatei.toString());
                BufferedReader br = new BufferedReader(fr);) {
            String zeile;
            while ((zeile = br.readLine()) != null) {
                tabelle.add(zeile.split(separator));
            }
            workWithTheCSVData();
        } catch (IOException ex) {
            Logger.getLogger(Rechnungsabgleich.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void workWithTheCSVData() throws IOException {
        if (tabelle == null) {
            throw new IOException("KEINE DATEN");
        }
        String[] headerOfSCV = tabelle.get(0);
        for (int k = 1; k <= headerOfSCV.length; k++) {
            if (headerOfSCV[k - 1].equalsIgnoreCase(selectedColumnName)) {
                selectedColumnNumber = k;
            }
        }
        System.out.println(selectedColumnName + " in Spalte Nr.: " + selectedColumnNumber + " (1-basiert gezählt)!");
    }
}
