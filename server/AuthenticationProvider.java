package ru.alexander_kramarenko.march.chat.server;

public interface AuthenticationProvider {
    void init();

    String getNicknameByLoginAndPassword(String login, String password);

    void changeNickname(String oldNickname, String newNickname);

    boolean isNickBusy(String nickname);

    void shutdown();
}
