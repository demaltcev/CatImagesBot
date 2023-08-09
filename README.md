#JavaTelegramBot

https://t.me/CatImagesBot - this is my example of longpolling bot. He is still working. This bot sending random cat images to you in 12am and 20pm accroding to Moscow time.

Source code is using Spring boot 2.7.14, maven, and java 11. You should download Spring initializer https://start.spring.io/ to start the project.

Paste your bot.name and bot.token from bot_father into application.properties file to link this code with your bot.

This bot use mySQL data base, so it wasnt work, if you dont install mySQL workbench on you desktop or server. This bot linked to my linux ubuntu VPS. 
The path to my cat images linked under linux system paths. 
So if want use it for windows, you should change all linux-paths to windows-paths in CatImagesBot.java. (change all images methods)
