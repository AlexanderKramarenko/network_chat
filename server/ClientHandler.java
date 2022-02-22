package ru.alexander_kramarenko.march.chat.server;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.Socket;

public class ClientHandler {

    private static final Logger log = LogManager.getLogger(ClientHandler.class);

    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String username;

    public String getUsername() {
        return username;
    }

    public ClientHandler(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        // комментируя this.in провоцируем NullPointerException при попытке логин клиента
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        new Thread(() -> {
            try {
                log.trace("Попытка запустить поток " + Thread.currentThread().getName());
                while (true) { // Цикл авторизации
                    String msg = in.readUTF();
                    if (msg.startsWith("/login ")) {
                        // /login Bob 100xyz
                        log.debug("Получена команда с префиксом /login :" + msg);
                        String[] tokens = msg.split("\\s+");
                        if (tokens.length != 3) {
                            sendMessage("/login_failed Введите имя пользователя и пароль");
                            log.info("/login_failed Введите имя пользователя и пароль");
                            continue;
                        }
                        String login = tokens[1];
                        String password = tokens[2];

                        String userNickname = server.getAuthenticationProvider().getNicknameByLoginAndPassword(login, password);
                        if (userNickname == null) {
                            sendMessage("/login_failed Введен некорретный логин/пароль");
                            log.warn("/login_failed Введен некорретный логин/пароль");
                            continue;
                        }
                        if (server.isUserOnline(userNickname)) {
                            sendMessage("/login_failed Учетная запись уже используется");
                            log.warn("/login_failed Учетная запись уже используется");
                            continue;
                        }
                        username = userNickname;
                        sendMessage("/login_ok " + username);
                        server.subscribe(this);
                        log.trace("/login_ok " + username);
                        break;
                    }
                }

                while (true) { // Цикл общения с клиентом
                    String msg = in.readUTF();
                    if (msg.startsWith("/")) {
                        executeCommand(msg);
                        log.debug("Получена команда :" + msg);
                        continue;
                    }
                    server.broadcastMessage(username + ": " + msg);
                    log.info("Отправлено широковещательное сообщение : " + username + ": " + msg);
                }
            } catch (IOException e) {
                log.throwing(Level.ERROR, e);
            } catch (NullPointerException e) {
                log.throwing(Level.ERROR, e);
            } finally {
                disconnect();
                log.warn("По блоку finally выполнен разрыв соединения вызовом disconnect()");

            }
        }).start();
    }

    private void executeCommand(String cmd) {
        // /w Bob Hello, Bob!!!
        if (cmd.startsWith("/w ")) {
            String[] tokens = cmd.split("\\s+", 3);
            if (tokens.length != 3) {
                sendMessage("Server: Введена некорректная команда");
                log.trace("Server: Введена некорректная команда приватного сообщения типа /w");
                return;
            }
            server.sendPrivateMessage(this, tokens[1], tokens[2]);
            log.trace("Отправлено приватное сообщение : от " + this + " кому : " + tokens[1] + " Сообщение : " + tokens[2]);
            return;
        }

        // /change_nick myNewNickname
        if (cmd.startsWith("/change_nick ")) {
            String[] tokens = cmd.split("\\s+");
            if (tokens.length != 2) {
                sendMessage("Server: Введена некорректная команда");
                log.trace("Введена некорректная команда на смену ника с префиксом /change_nick ");
                return;
            }
            String newNickname = tokens[1];
            if (server.getAuthenticationProvider().isNickBusy(newNickname)) {
                sendMessage("Server: Такой никнейм уже занят");
                log.trace("Попытка сменить ник на уже занятый. Пользователь : " + this + " Новый ник : " + newNickname);
                return;
            }
            server.getAuthenticationProvider().changeNickname(username, newNickname);
            log.trace("Смена ник с : " + username + " на :" + newNickname);
            username = newNickname;
            sendMessage("Server: Вы изменили никнейм на " + newNickname);
            server.broadcastClientsList();
        }
    }

    public void sendMessage(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            log.throwing(Level.ERROR, e);
            disconnect();
            log.warn("Неудачная отправка сообщения.  Вызов disconnect()");
        }
    }

    public void disconnect() {
        server.unsubscribe(this);
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                log.throwing(Level.ERROR, e);
            }
        }
    }
}
