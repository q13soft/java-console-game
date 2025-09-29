package com.example.dungeon.core;

import com.example.dungeon.model.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class Game {
    private final GameState state = new GameState();
    private final Map<String, Command> commands = new LinkedHashMap<>();

    static {
        WorldInfo.touch("Game");
    }

    public Game() {
        registerCommands();
        bootstrapWorld();
    }

    private void registerCommands() {
        commands.put("help", (ctx, a) -> System.out.println("Команды: " + String.join(", ", commands.keySet())));
        commands.put("gc-stats", (ctx, a) -> {
            Runtime rt = Runtime.getRuntime();
            long free = rt.freeMemory(), total = rt.totalMemory(), used = total - free;
            System.out.println("Память: used=" + used + " free=" + free + " total=" + total);
        });
        commands.put("look", (ctx, a) -> System.out.println(ctx.getCurrent().describe()));
        commands.put("move", (ctx, a) -> {
            if (a.size() != 1) {
                throw new InvalidCommandException("Путь указан некоректно, команда look - покажет доступные выходы");
            }
            String way = a.getFirst().toLowerCase();
            Room myRoom = ctx.getCurrent();
            Room room2 = myRoom.getNeighbors().get(way);
            if (room2 != null) {
                ctx.setCurrent(room2);
                System.out.println("Вы вошли в комнату: "+room2.getName());
                System.out.println(room2.describe());   // show look
            } else {
                throw new InvalidCommandException(way + "- этот выход закрыт, команда look - покажет доступные выходы");
            }
            // throw new InvalidCommandException("TODO-1: реализуйте перемещение игрока");
        });
        commands.put("take", (ctx, a) -> {
            if (a.size() < 1) {
                throw new InvalidCommandException("Не выбран предмет");
            }
            String nameItem = String.join(" ", a);
            Room myRoom = ctx.getCurrent();
            Optional<Item> findItem = myRoom.getItems().stream()
                    .filter(item -> item.getName().equalsIgnoreCase(nameItem))
                    .findFirst();
            if (findItem.isPresent()) {
                Item item = findItem.get();
                myRoom.getItems().remove(item);
                ctx.getPlayer().getInventory().add(item);
                System.out.println("Взят: "+item.getName());
            } else {
                throw new InvalidCommandException(nameItem+" - нет такого предмета");
            }
            // throw new InvalidCommandException("TODO-2: реализуйте взятие предмета");
        });
        commands.put("inventory", (ctx, a) -> {
            System.out.println("TODO-3: вывести инвентарь (Streams)");
        });
        commands.put("use", (ctx, a) -> {
            throw new InvalidCommandException("TODO-4: реализуйте использование предмета");
        });
        commands.put("fight", (ctx, a) -> {
            throw new InvalidCommandException("TODO-5: реализуйте бой");
        });
        commands.put("save", (ctx, a) -> SaveLoad.save(ctx));
        commands.put("load", (ctx, a) -> SaveLoad.load(ctx));
        commands.put("scores", (ctx, a) -> SaveLoad.printScores());
        commands.put("exit", (ctx, a) -> {
            System.out.println("Пока!");
            System.exit(0);
        });
    }

    private void bootstrapWorld() {
        Player hero = new Player("Герой", 20, 5);
        state.setPlayer(hero);

        Room square = new Room("Площадь", "Каменная площадь с фонтаном.");
        Room forest = new Room("Лес", "Шелест листвы и птичий щебет.");
        Room cave = new Room("Пещера", "Темно и сыро.");
        square.getNeighbors().put("north", forest);
        forest.getNeighbors().put("south", square);
        forest.getNeighbors().put("east", cave);
        cave.getNeighbors().put("west", forest);

        forest.getItems().add(new Potion("Малое зелье", 5));
        forest.setMonster(new Monster("Волк", 1, 8));

        state.setCurrent(square);
    }

    public void run() {
        System.out.println("DungeonMini (TEMPLATE). 'help' — команды.");
        try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                System.out.print("> ");
                String line = in.readLine();
                if (line == null) break;
                line = line.trim();
                if (line.isEmpty()) continue;
                List<String> parts = Arrays.asList(line.split("\s+"));
                String cmd = parts.getFirst().toLowerCase(Locale.ROOT);
                List<String> args = parts.subList(1, parts.size());
                Command c = commands.get(cmd);
                try {
                    if (c == null) throw new InvalidCommandException("Неизвестная команда: " + cmd);
                    c.execute(state, args);
                    state.addScore(1);
                } catch (InvalidCommandException e) {
                    System.out.println("Ошибка: " + e.getMessage());
                } catch (Exception e) {
                    System.out.println("Непредвиденная ошибка: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("Ошибка ввода/вывода: " + e.getMessage());
        }
    }
}
