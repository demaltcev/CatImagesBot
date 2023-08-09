package com.example.CatImagBotProject.service;

import com.example.CatImagBotProject.config.BotConfig;
import com.example.CatImagBotProject.model.User;
import com.example.CatImagBotProject.model.UserRepository;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class CatImagBot extends TelegramLongPollingBot {
    final BotConfig config;

    @Autowired
    private UserRepository userRepository;

    static final String HELP_TEXT = "Бот создан для получения ежедневных картинок c котиками.\n\n" + "Команды расположены в боковом меню бота.\n\n" + "Напишите /start, чтобы увидеть " + "приветственное сообщение.\n\n" + "Напишите /follow, чтобы подписаться на рассылку котиков.\n\n" + "Напишите /deletedata, чтобы отписаться.\n\n";
    static final String ERROR_TEXT = "Извини, команда не распознана, попробуй написать /start или подсмотреть в /help.";
    static final String FOLLOW_TEXT = "Подписка на котиков оформлена! Котики будут приходить в 12:00 и 20:00 по МСК.";
    static final String STILL_FOLLOWED = "Вы уже подписаны. Ожидайте котиков!)";
    static final String DELETE_TEXT = "Подписка на котиков отключена:(";
    static final String GOOD_DAY = EmojiParser.parseToUnicode("Хорошего дня" + " :smiley_cat:");
    static final String GOOD_EVENING = EmojiParser.parseToUnicode("Хорошего вечера" + " :smirk_cat:");

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    public CatImagBot(BotConfig config) {
        this.config = config;

        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "Начать пользоваться ботом"));
        listOfCommands.add(new BotCommand("/follow", "Подписаться на рассылку котиков"));
        listOfCommands.add(new BotCommand("/deletedata", "Отписаться"));
        listOfCommands.add(new BotCommand("/help", "Как пользоваться ботом?"));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bot`s command list" + e.getMessage());
        }
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            switch (messageText) {
                case "/start":
                    sendCurrentImage(chatId, 1000);
                    startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case "/follow":
                    registerUser(update.getMessage());
                    break;
                case "/help":
                    sendMessege(chatId, HELP_TEXT);
                    break;
                case "/deletedata":
                    deleteUser(update.getMessage());
                    break;
                default:
                    sendMessege(chatId, ERROR_TEXT);
                    break;
            }


        }
    }

    //метод для трай-кетч для отправки сообщения
    private void executeTextMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occured: " + e.getMessage());
        }
    }

    //метод для трай-кетч для отправки фото
    private void executePhotoMessage(SendPhoto photo) {
        try {
            execute(photo);
        } catch (TelegramApiException e) {
            log.error("Error occured: " + e.getMessage());
        }

    }

    private void registerUser(Message message) {
        if (userRepository.findById(message.getChatId()).isEmpty()) {
            sendMessege(message.getChatId(), FOLLOW_TEXT);
            var chatId = message.getChatId();
            var chat = message.getChat();
            User user = new User();
            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));
            userRepository.save(user);
            log.info("user saved: " + user);
        } else {
            sendMessege(message.getChatId(), STILL_FOLLOWED);
        }
    }

    private void deleteUser(Message message) {
        if (userRepository.findById(message.getChatId()).isPresent()) {
            userRepository.delete(userRepository.findById(message.getChatId()).get());
            sendMessege(message.getChatId(), DELETE_TEXT);
            log.info("user deleted: " + message.getChatId());
        }
    }

    //метод для отправки сообщения пользователю
    private void sendMessege(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        executeTextMessage(message);
    }

    //метод, выполняемый после команды /start
    private void startCommandReceived(long chatId, String name) {
        String answer = "Привет, " + name + ", рад видеть тебя здесь. " +
                "Я создан для того, чтобы радовать " +
                "тебя каждый день новыми котиками.\n" +
                "Чтобы подписаться на ежедневную рассылку " +
                "котиков напиши /follow.\n";
        log.info("Replied to user " + name);
        sendMessege(chatId, answer);

    }


    //метод для отправки определенной картинки
    private void sendCurrentImage(long chatId, int numberOfImage) {
        //путь к вартинкам
        int part1 = numberOfImage;
        String imageName = part1 + ".jpeg";
        String path = "/root/bots/images/" + imageName;
        //прописываем поиск пути и отправку картинки
        SendPhoto sendPhoto = SendPhoto.builder().chatId(String.valueOf(chatId)).photo(new InputFile(new File(path))).build();
        executePhotoMessage(sendPhoto);

    }

    //метод для отправки рандомной картинки
    private void sendRandomImage(long chatId) {
        //путь к вартинкам
        int part1 = (int) (Math.random() * 192) + 1;
        String imageName = part1 + ".jpeg";
        String path = "/root/bots/images/" + imageName;

        //String path = "C:\\var\\"+imageName;
        //String path = "/root/bots/images/"+imageName;
        //прописываем поиск пути и отправку картинки
        SendPhoto sendPhoto = SendPhoto.builder().chatId(String.valueOf(chatId)).photo(new InputFile(new File(path))).build();
        executePhotoMessage(sendPhoto);
    }


    private void prepareAndSendMessage(long chatId, String textToSent) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSent);
        executeTextMessage(message);
    }


    @Scheduled(cron = "${cron.scheduler1}")
    private void sendAllImagesNoon() {
        var users = userRepository.findAll();
        for (User user : users) {
            sendMessege(user.getChatId(), GOOD_DAY);
            sendRandomImage(user.getChatId());
            log.info("image sended to user: " + user.getUserName());
        }
    }

    @Scheduled(cron = "${cron.scheduler2}")
    private void sendAllImagesEvening() {
        var users = userRepository.findAll();
        for (User user : users) {
            sendMessege(user.getChatId(), GOOD_EVENING);
            sendRandomImage(user.getChatId());
            log.info("image sended to user: " + user.getUserName());
        }
    }


}
