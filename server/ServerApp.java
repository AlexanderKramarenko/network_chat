package ru.alexander_kramarenko.march.chat.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServerApp {

    private static final Logger log = LogManager.getLogger(ServerApp.class);

    public static void main(String[] args) {
        log.trace("Вошли в приложение Сервер.");
        new Server(8189);
    }
}
// Реализован логгер в серверной части и в части клиента