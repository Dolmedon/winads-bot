package io.github.anisthesie.participants;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Participant {
    @Getter
    private Long pk;
    @Getter
    private String username;
    @Getter
    private List<String> stories;
    @Getter
    private List<Integer> numbers;

    public Participant(Long pk, String username, List<String> stories, List<Integer> numbers) {
        this.pk = pk;
        this.username = username;
        this.stories = stories;
        this.numbers = numbers;
    }

    public Participant(String line){
        final String[] types = line.split("\\|");
        if(types.length != 3)
            return;
        this.username = types[0].substring(0, types[0].indexOf(":"));
        this.numbers = Arrays.stream(types[0].substring(types[0].indexOf(":") + 1).split(",")).mapToInt(Integer::parseInt).boxed().collect(Collectors.toList());
        this.pk = Long.parseLong(types[1]);
        this.stories = new ArrayList<>();
        stories.addAll(Arrays.asList(types[2].split(",")));
    }

    public void addStory(String storyId){
        this.stories.add(storyId);
    }

    @Override
    public String toString() { // anisthesie:16,18,20|18273|1922,1283,1023
        StringBuilder ret = new StringBuilder(username);
        ret.append(":");
        numbers.forEach(n -> ret.append(n).append(","));
        ret.deleteCharAt(ret.length() - 1);
        ret.append("|").append(pk).append("|");
        stories.forEach(n -> ret.append(n).append(","));
        ret.deleteCharAt(ret.length() - 1);
        return ret.toString();
    }

    public void addNumber(Integer number){
        this.numbers.add(number);
    }

    public boolean isValid(){
        return username != null && pk != null;
    }

}
