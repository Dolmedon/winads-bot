package io.github.anisthesie.participants;

import io.github.anisthesie.Main;
import io.github.anisthesie.jobs.DefaultUserJob;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static io.github.anisthesie.Main.account;

public class ParticipantsHandler {

    private static final File file = new File("participants.txt");
    private static ArrayList<Participant> participants_numbers = new ArrayList<Participant>();

    public static void processParticipant(long pk, String username, String storyId) throws MalformedURLException {
        if (!setupFile()) {
            System.out.println("El usuario " + username + " no se puede registrar. Probando en la siguiente iteración.");
            return;
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(Paths.get(file.toURI()));
        } catch (IOException e) {
            System.out.println("Imposible leer el archivo participants.txt");
            e.printStackTrace();
            return;
        }

        final Participant participant = new Participant(pk, username, Collections.singletonList(storyId), Collections.singletonList(1));

        //Se comprueba que el participante no tenga un nuemero asignado, para si no volver a repetir la peticion de asignación
        if (!ParticipantsHandler.participantNumberIsAsigned(ParticipantsHandler.participants_numbers, participant)){
            ParticipantsHandler.participants_numbers.add(participant);
            //Se hace peticion a backend de numeros
            String endPointNumbersDB = Main.backendParams[0]+participant.toString()+"&t="+Main.backendParams[1];
            String respuesta = "";
            try {
                respuesta = peticionHttpGet(endPointNumbersDB);
                System.out.println("Numero registrado:\n" + respuesta);
                if (!respuesta.contains("participante_ya_resgistrado")){
                    lines.add(respuesta);
                }
            } catch (Exception e) {
                System.out.println("El número no se puede registrar.");
                e.printStackTrace();
                return;
            }

            try {
                Files.write(Paths.get(file.toURI()), lines);
            } catch (IOException e) {
                System.out.println("El usuario " + username + " no se pudo registrar en el archivo.");
                System.out.println("Intentando en la siguiente iteración");
                e.printStackTrace();
                return;
            }
        } else {
            System.out.println("Usuario: " + username + " ya registrado, no se realiza petición");
        }

        if (!account.login()) {
            System.out.println("No fue posible iniciar sesión");
            return;
        }
    }

    private static Boolean participantNumberIsAsigned(ArrayList <Participant> lista, Participant participante){
        for (Participant participante_l: lista){
            if ( participante_l.getUsername().equals(participante.getUsername())
                 && participante_l.getStories().get(0).equals(participante.getStories().get(0)) ) {
                return true;
            }
        }
        return false;
    }

    private static String peticionHttpGet(String urlParaVisitar) throws Exception {
        StringBuilder resultado = new StringBuilder();
        URL url = new URL(urlParaVisitar);

        // Abrir la conexión e indicar que será de tipo GET
        HttpURLConnection conexion = (HttpURLConnection) url.openConnection();
        conexion.setRequestMethod("GET");

        BufferedReader rd = new BufferedReader(new InputStreamReader(conexion.getInputStream()));
        String linea;

        while ((linea = rd.readLine()) != null) {
            resultado.append(linea);
        }

        rd.close();
        return resultado.toString();
    }

    private static Integer getNextNumber(List<Participant> participants) {
        int biggest = 0;
        for (Participant p : participants) {
            int current = Collections.max(p.getNumbers());
            if (current > biggest)
                biggest = current;
        }
        return biggest + 1;
    }

    private static boolean setupFile() {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                System.out.println("No se pudo crear el archivo participants.txt.");
            }
            return checkFile();
        }
        return checkFile();
    }

    private static boolean checkFile() {
        return file.exists() && file.canRead() && file.canWrite();
    }

}
