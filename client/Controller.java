package ru.alexander_kramarenko.march.chat.client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    private static final Logger log = LogManager.getLogger(Controller.class);


    @FXML
    TextField msgField, loginField;

    @FXML
    PasswordField passwordField;

    @FXML
    TextArea msgArea;

    @FXML
    HBox loginPanel, msgPanel;

    @FXML
    ListView<String> clientsList;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String username;

    public void setUsername(String username) {
        this.username = username;
        boolean usernameIsNull = username == null;
        loginPanel.setVisible(usernameIsNull);
        loginPanel.setManaged(usernameIsNull);
        msgPanel.setVisible(!usernameIsNull);
        msgPanel.setManaged(!usernameIsNull);
        clientsList.setVisible(!usernameIsNull);
        clientsList.setManaged(!usernameIsNull);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setUsername(null);
    }

    public void login() {
        if (loginField.getText().isEmpty()) {
            showErrorAlert("Имя пользователя не может быть пустым");
            log.trace("Поле имени пользователя оставлено пустым");
            return;
        }

        if (socket == null || socket.isClosed()) {
            connect();
        }

        try {
            out.writeUTF("/login " + loginField.getText() + " " + passwordField.getText());
            log.debug("Отправлена команда /login " + loginField.getText() + " " + passwordField.getText());
        } catch (IOException e) {
            log.throwing(Level.ERROR, e);
        }
    }

    public void connect() {
        try {
            socket = new Socket("localhost", 8189);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            Thread t = new Thread(() -> {
                try {
                    log.trace("Попытка запустить поток " + Thread.currentThread().getName());
                    // Цикл авторизации
                    while (true) {
                        String msg = in.readUTF();
                        if (msg.startsWith("/login_ok ")) {
                            setUsername(msg.split("\\s")[1]);
                            log.trace("/login_ok сообщение " + msg);
                            break;
                        }
                        if (msg.startsWith("/login_failed ")) {
                            String cause = msg.split("\\s", 2)[1];
                            msgArea.appendText(cause + "\n");
                            log.trace("Неудачный логин /login_failed " + cause + "\n");
                        }
                    }
                    // Цикл общения
                    while (true) {
                        String msg = in.readUTF();
                        // todo вынести этот блок
                        if (msg.startsWith("/")) {
                            if (msg.startsWith("/clients_list ")) {
                                // /clients_list Bob Max Jack
                                String[] tokens = msg.split("\\s");
                                Platform.runLater(() -> {
                                    clientsList.getItems().clear();
                                    for (int i = 1; i < tokens.length; i++) {
                                        clientsList.getItems().add(tokens[i]);
                                    }
                                    log.trace("Выполнено обновление списка пользователей");
                                });
                            }
                            continue;
                        }
                        msgArea.appendText(msg + "\n");
                        log.trace("Принято сообщение " + msg);
                    }
                } catch (IOException e) {
                    log.throwing(Level.ERROR, e);
                } catch (NullPointerException e) {
                    log.throwing(Level.ERROR, e);
                } finally {
                    disconnect();
                    log.warn("По блоку finally выполнен разрыв соединения вызовом disconnect()");
                }
            });
            t.start();
        } catch (IOException e) {
            log.throwing(Level.ERROR, e);
            log.error("Невозможно подключиться к серверу");
            showErrorAlert("Невозможно подключиться к серверу");
        }
    }

    public void sendMsg() {
        try {
            out.writeUTF(msgField.getText());
            log.trace("Отправлено сообщение " + msgField.getText());
            msgField.clear();
            msgField.requestFocus();
        } catch (IOException e) {
            log.throwing(Level.ERROR, e);
            log.error("Невозможно отправить сообщение");
            showErrorAlert("Невозможно отправить сообщение");
        }
    }

    private void disconnect() {
        log.trace("Пользователь " + username + " отключается");
        setUsername(null);
        try {
            if (socket != null) {
                socket.close();
                log.trace("Сокет " + socket + " закрыт");
            }
        } catch (IOException e) {
            log.throwing(Level.ERROR, e);
        }
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(message);
        alert.setTitle("March Chat FX");
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
