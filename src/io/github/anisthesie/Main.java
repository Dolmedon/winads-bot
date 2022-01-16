package io.github.anisthesie;

import io.github.anisthesie.bot.Account;
import io.github.anisthesie.jobs.DefaultUserJob;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {

    public static Account account = null;
    public static String[] backendParams = new String[] {"https://winads.greenborn.com.ar/api/web/numbers/add?s=","$2y$13$maydzLNRwqH4cQP2L8FCQu6OxIU/.GpzxwRxcqM3Tnzhk9uLMzcrm"};
    public static String message = "Enhorabuena! Has acertado el juego! Tu número es: %number%";
    public static Integer delay = 1;

    public static void main(String[] args) {
        System.out.println("Cargando archivo de configuración...");
        final File configFile = new File("config.txt");
        final Path path = Paths.get(configFile.toURI());
        if (!configFile.exists()) {
            System.out.println("No se pudo encontrar el archivo config.txt, Creando un archivo nuevo...");
            try {
                configFile.createNewFile();
                List<String> lines = new ArrayList<>();
                lines.add("credentials:username,password");
                lines.add("message:Enhorabuena! Has acertado el juego! Tu número es: %number%");
                lines.add("delay:1");
                lines.add("backend:url,token");
                Files.write(path, lines);
            } catch (IOException e) {
                System.out.println("Ha ocurrido un error, por favor cree el archivo de forma manual.");
                e.printStackTrace();
                return;
            }
            System.out.println("Archivo creado correctamente.");
            System.out.println("Archivo: " + configFile.getAbsolutePath());
            System.out.println();
            return;
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(path);
        } catch (IOException e) {
            System.out.println("Ha ocurrido un error, no se puede leer el archivo.");
            e.printStackTrace();
            return;
        }
        if (lines.isEmpty()) {
            System.out.println("El archivo está vacío, por favor completelo.");
            return;
        }
        AtomicInteger delay = new AtomicInteger(1);
        lines.forEach(line -> {
            if (line.startsWith("credentials:")) {
                String[] credentials = line.replace("credentials:", "").split(",");
                account = new Account(credentials[0], credentials[1], false);
                if (account.getUsername().equals("username") && account.getPassword().equals("password")) {
                    System.out.println("Por favor complete sus credenciales de acceso en el archivo config.txt.");
                    System.exit(-1);
                }
            } else if (line.startsWith("message:"))
                message = line.replace("message:", "");
            else if (line.startsWith("delay:")) {
                try {
                    int i = Integer.parseInt(line.replace("delay:", ""));
                    delay.set(i);
                } catch (NumberFormatException e){
                    System.out.println("Delay inválido, se define en 1.");
                    delay.set(1);
                }
            } else if (line.startsWith("backend:")){
                backendParams = line.replace("backend:", "").split(",");
            }

        });

        if (account == null) {
            System.out.println("En archivo de configuración, no se especificaron los datos de su cuenta.");
            return;
        }

        System.out.println("Iniciando sesión con: " + account.getUsername() + "...");
        if (account.login())
            System.out.println("\tSesión iniciada correctamente: " + account.getUsername());
        else {
            System.out.println("No fue posible iniciar sesión.");
            return;
        }
        System.out.println("Comenzando con delay definido en "+delay+" segundo(s).");

        try {
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
            Main.delay = delay.get();
            final JobKey jobKey = JobKey.jobKey("main");
            final JobDetail jobDetail = JobBuilder.newJob(DefaultUserJob.class).withIdentity(jobKey).build();
            final Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(jobKey.getName())
                    .startNow()
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withIntervalInSeconds(1)
                            .repeatForever())
                    .build();
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }
}
