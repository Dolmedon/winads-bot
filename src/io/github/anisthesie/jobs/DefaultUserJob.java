package io.github.anisthesie.jobs;

import com.github.instagram4j.instagram4j.models.media.reel.ReelMedia;
import com.github.instagram4j.instagram4j.requests.feed.FeedUserStoryRequest;
import com.github.instagram4j.instagram4j.responses.feed.FeedUserStoryResponse;
import io.github.anisthesie.Main;
import io.github.anisthesie.participants.ParticipantsHandler;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Optional;

import static io.github.anisthesie.Main.account;

public class DefaultUserJob implements Job {
    private static int delay_counter = 0;
    private static int delay_cicle = 0;
    private static Long delay_mod = 0L;
    private static int time_counter = 0;
    private static Long history_ftime_stamp = 0L;
    private static Long hora_inicial = 0L;

    public static void redefineDelay(){
        Long tiempo_actual = new Date().getTime() / 1000;
        Long tiempo_tolow  = DefaultUserJob.history_ftime_stamp - 30*60;

        if (tiempo_actual < tiempo_tolow){
            DefaultUserJob.delay_mod = (tiempo_actual - DefaultUserJob.hora_inicial) / 60;
        } else {
            DefaultUserJob.delay_mod = (DefaultUserJob.history_ftime_stamp - tiempo_actual) / 60;
        }
        Double porcentual_mod = (DefaultUserJob.delay_mod + Main.delay) * 0.25;
        DefaultUserJob.delay_mod = DefaultUserJob.delay_mod + Long.valueOf( (int) Math.floor(Math.random()*porcentual_mod-porcentual_mod/2));

        if (DefaultUserJob.history_ftime_stamp < DefaultUserJob.delay_mod + Main.delay + tiempo_actual){
            DefaultUserJob.delay_mod = DefaultUserJob.history_ftime_stamp - DefaultUserJob.delay_mod + Main.delay + tiempo_actual -1;
        }
        System.out.println("Delay redefinido para proximo ciclo en: " + (DefaultUserJob.delay_mod + Main.delay) + " segundo(s).");
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        DefaultUserJob.delay_counter = DefaultUserJob.delay_counter + 1;
        DefaultUserJob.time_counter = DefaultUserJob.time_counter + 1;

        if (DefaultUserJob.hora_inicial == 0L){
            DefaultUserJob.hora_inicial = new Date().getTime() / 1000;
        }

        if (DefaultUserJob.delay_counter < (Main.delay + DefaultUserJob.delay_mod)){
            return;
        } else {
            if (Math.toIntExact(DefaultUserJob.history_ftime_stamp) != 0){
                DefaultUserJob.delay_counter = 0;
                DefaultUserJob.delay_cicle = DefaultUserJob.delay_cicle + 1;
                DefaultUserJob.redefineDelay();
            }
        }

        System.out.println("\nComprobando por nuevos participantes...");
        if (!account.login()) {
            System.out.println("No se pudo iniciar sesi贸n.");
            System.out.println("Reintentando en 3 minutos...");
            return;
        }
        FeedUserStoryRequest storyRequest = new FeedUserStoryRequest(account.getClient().getSelfProfile().getPk());
        FeedUserStoryResponse storyResponse = account.getClient().actions().story().userStory(account.getClient().getSelfProfile().getPk()).join();
        if (storyResponse.getReel() == null) {
            System.out.println("Comprobaci贸n completa. No hay historias disponibles");
            return;
        }

        Optional<ReelMedia> optionalReelMedia = storyResponse.getReel().getItems().stream().filter(reel ->
                reel.getExtraProperties().containsKey("story_quizs") &&
                        reel.getExtraProperties().containsKey("story_quiz_participant_infos")).findFirst();
        if (!optionalReelMedia.isPresent()) {
            System.out.println("Comprobaci贸n completa. Ninguna historia contiene cuestionario.");
            return;
        }
        ReelMedia reelMedia = optionalReelMedia.get();
         // PROBAR
        
        // Lista de participantes completa
        Integer number_pages = (Integer) ((ArrayList) reelMedia.getExtraProperties().get("story_quiz_participant_infos")).size();
        ArrayList<LinkedHashMap<String, Object>> participants;
        for (int i = 0; i > (number_pages-1); i++) {
        		// Lista de participantes dividida en p醙inas (?)
        		ArrayList<LinkedHashMap<String, Object>> participants_page = ((ArrayList<LinkedHashMap<String, Object>>) ((LinkedHashMap<String, Object>) ((ArrayList) reelMedia.getExtraProperties().get("story_quiz_participant_infos")).get(i)).get("participants"));
        	for (int j = 0; j < participants_page.size(); j++) {
        		// A馻dir participantes de la p醙ina obtenida a la lista de participantes completa
        		participants.append(participants_page.get(j));
        	}
        }
        
        // --PROBAR--
        Integer answer = (Integer) ((LinkedHashMap<String, Object>) ((ArrayList<LinkedHashMap<String, Object>>) reelMedia.getExtraProperties().get("story_quizs")).get(0).get("quiz_sticker"))
                .get("correct_answer");

        DefaultUserJob.history_ftime_stamp = storyResponse.getReel().getExpiring_at();

        participants.stream().filter(participant -> participant.get("answer").equals(answer) &&
                !((String) ((LinkedHashMap<String, Object>) participant.get("user")).get("username")).equalsIgnoreCase(account.getUsername())).forEach(participant -> {
            LinkedHashMap<String, Object> user = ((LinkedHashMap<String, Object>) participant.get("user"));
            final String username = (String) user.get("username");
            final Long pk = Long.parseLong(String.valueOf(user.get("pk")));

            try {
                ParticipantsHandler.processParticipant(pk, username, reelMedia.getId());
            } catch (MalformedURLException e){
                System.out.println("MalformedURLException.");
            }
        });
        System.out.println("Comprobaci贸n completa.");
    }
}
