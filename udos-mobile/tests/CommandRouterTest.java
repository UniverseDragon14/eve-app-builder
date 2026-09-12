package com.universaldragon.udosmobile;

public final class CommandRouterTest {
    private static int count;

    private static void check(String input, boolean wake, boolean awaiting, CommandRouter.Action expected) {
        CommandRouter.Command actual = CommandRouter.parse(input, wake, awaiting);
        if (actual.action != expected) {
            throw new AssertionError(input + ": expected " + expected + ", got " + actual.action);
        }
        count++;
    }

    public static void main(String[] args) {
        check("Hey Nova, open camera", true, false, CommandRouter.Action.CAMERA);
        check("HEY DRAGON: open settings!", true, false, CommandRouter.Action.SETTINGS);
        check("Hey EVE open apps", true, false, CommandRouter.Action.APPS);
        check("open camera", true, false, CommandRouter.Action.IGNORE);
        check("open camera", true, true, CommandRouter.Action.CAMERA);
        check("Hey Nova", true, false, CommandRouter.Action.PROMPT);
        check("நோவா கேமரா திற", true, false, CommandRouter.Action.CAMERA);
        check("கேமரா", false, false, CommandRouter.Action.CAMERA);
        check("அமைப்புகள்", false, false, CommandRouter.Action.SETTINGS);
        check("நிறுத்து", true, false, CommandRouter.Action.SLEEP);
        check("sleep", true, false, CommandRouter.Action.SLEEP);
        check("Hey Nova stop listening", true, false, CommandRouter.Action.SLEEP);
        check("Hey Nova do not open camera", true, false, CommandRouter.Action.UNKNOWN);
        check("do not open settings", false, false, CommandRouter.Action.UNKNOWN);
        check("a happy day", false, false, CommandRouter.Action.UNKNOWN);
        check("novascotia camera", true, false, CommandRouter.Action.IGNORE);
        check("open camera and delete files", false, false, CommandRouter.Action.UNKNOWN);
        check("shell rm -rf /", false, false, CommandRouter.Action.UNKNOWN);
        check("curl https://example.com", false, false, CommandRouter.Action.UNKNOWN);
        check("<img src=x onerror=UDOS.openCamera()>", false, false, CommandRouter.Action.UNKNOWN);
        check("Hey Nova speak open camera", true, false, CommandRouter.Action.SPEAK);
        check("VOICE CHECK", false, false, CommandRouter.Action.VOICE_STATUS);
        check("voice tamil", false, false, CommandRouter.Action.TAMIL);
        check("voice english", false, false, CommandRouter.Action.ENGLISH);
        check("pi status", false, false, CommandRouter.Action.PI_STATUS);
        check("status", false, false, CommandRouter.Action.STATUS);
        check(null, false, false, CommandRouter.Action.UNKNOWN);
        check("", true, false, CommandRouter.Action.IGNORE);
        check(new String(new char[1001]), false, false, CommandRouter.Action.UNKNOWN);
        String spoken = CommandRouter.parse("speak Vanakkam Aslam", false, false).text;
        if (!"Vanakkam Aslam".equals(spoken)) throw new AssertionError("Speech payload changed");
        count++;
        System.out.println("PASS: " + count + " bounded command and wake-gating checks");
    }
}
