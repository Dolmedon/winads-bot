package io.github.anisthesie.bot;

import com.github.instagram4j.instagram4j.IGClient;
import com.github.instagram4j.instagram4j.exceptions.IGLoginException;
import com.github.instagram4j.instagram4j.requests.direct.DirectThreadsBroadcastRequest;
import lombok.Getter;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Account {

    @Getter
    private String username, password;
    @Getter
    private IGClient client;

    public Account(String username, String password, boolean auth) {
        this.username = username;
        this.password = password;
        if (auth) login();
    }

    public boolean login() {
        if (isLogged())
            return true;
        try {
            client = IGClient.builder()
                    .username(username)
                    .password(password)
                    .login();
        } catch (IGLoginException e) {
            e.printStackTrace();
            return false;
        }
        return isLogged();
    }

    public void sendMessage(long pk, String text) {
        try {
            new DirectThreadsBroadcastRequest(new DirectThreadsBroadcastRequest.BroadcastTextPayload(text, pk)).execute(getClient()).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    public boolean isLogged() {
        return client != null && client.isLoggedIn();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return account.username.equalsIgnoreCase(this.username) && account.password.equals(this.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, password);
    }
}
