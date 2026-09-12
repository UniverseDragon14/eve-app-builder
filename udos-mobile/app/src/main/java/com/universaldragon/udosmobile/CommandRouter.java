package com.universaldragon.udosmobile;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Bounded launcher commands. Recognition text never becomes executable code. */
final class CommandRouter {
    enum Action {
        IGNORE, UNKNOWN, PROMPT, WAKE, SLEEP, LISTEN, SPEAK, READ,
        CAMERA, SETTINGS, APPS, SITE, STATUS, VOICE_STATUS, TOOLS, MAP,
        PI_STATUS, ENGLISH, TAMIL
    }

    static final class Command {
        final Action action;
        final String text;

        Command(Action action, String text) {
            this.action = action;
            this.text = text;
        }
    }

    private static final Pattern PREFIX = Pattern.compile(
            "^(?:(?:hey|ஹே)\\s+)?(?:nova|dragon|eve|நோவா|டிராகன்|ஈவ்)"
                    + "(?:[\\s,.:;!?-]+|$)", Pattern.CASE_INSENSITIVE);

    private CommandRouter() {}

    static Command parse(String input, boolean wakeListening, boolean awaitingCommand) {
        if (input == null || input.length() > 1000) return result(Action.UNKNOWN, "");
        String text = Normalizer.normalize(input, Normalizer.Form.NFC).trim();
        String normalized = normalize(text);
        if (isStop(normalized)) return result(Action.SLEEP, "");

        Matcher prefix = PREFIX.matcher(text);
        boolean addressed = prefix.find();
        if (wakeListening && !addressed && !awaitingCommand) return result(Action.IGNORE, "");
        if (addressed) text = text.substring(prefix.end()).trim();
        normalized = normalize(text);
        if (addressed && normalized.isEmpty()) return result(Action.PROMPT, "");
        if (isStop(normalized)) return result(Action.SLEEP, "");

        switch (normalized) {
            case "wake": case "wake up": return result(Action.WAKE, "");
            case "voice": case "mic": case "listen": return result(Action.LISTEN, "");
            case "speak": case "read": case "read response": return result(Action.READ, "");
            case "camera": case "open camera": case "கேமரா":
            case "கேமரா திற": case "கேமராவை திற": return result(Action.CAMERA, "");
            case "settings": case "setting": case "phone settings": case "open settings":
            case "அமைப்புகள்": case "செட்டிங்ஸ்": return result(Action.SETTINGS, "");
            case "apps": case "app list": case "open apps": case "apps settings":
                return result(Action.APPS, "");
            case "live": case "live udos": case "website": case "open website":
            case "udos website": return result(Action.SITE, "");
            case "status": case "home": case "நிலை": return result(Action.STATUS, "");
            case "voice status": case "voice check": return result(Action.VOICE_STATUS, "");
            case "tools": case "tool": return result(Action.TOOLS, "");
            case "map": case "project map": case "brain map": case "multiverse":
                return result(Action.MAP, "");
            case "pi": case "pi status": case "pi brain": case "brain status":
            case "server status": return result(Action.PI_STATUS, text);
            case "voice english": return result(Action.ENGLISH, "en-US");
            case "voice tamil": return result(Action.TAMIL, "ta-IN");
            default:
                if (text.toLowerCase(Locale.ROOT).startsWith("speak ")) {
                    return result(Action.SPEAK, text.substring(6).trim());
                }
                return result(Action.UNKNOWN, text);
        }
    }

    private static String normalize(String text) {
        return text.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ")
                .replaceAll("[.!?]+$", "").trim();
    }

    private static boolean isStop(String text) {
        return text.equals("sleep") || text.equals("sleep mode") || text.equals("stop")
                || text.equals("stop listening") || text.equals("நிறுத்து");
    }

    private static Command result(Action action, String text) {
        return new Command(action, text);
    }
}
