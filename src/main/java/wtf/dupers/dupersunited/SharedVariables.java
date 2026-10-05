package wtf.dupers.dupersunited;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;


public class SharedVariables {
    public static final Executor IO_EXECUTOR = Util.ioPool().forName("DupersUnited");
    public static final Path DIRECTORY = FabricLoader.getInstance().getGameDir().resolve("DupersUnited");
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("dupersunited", "dupersunited"));
    public static Screen screenToOpen;
    private static final List<String> glitchaQuotes = Arrays.asList(
            "Popbob ghost duper今天很热，Null button dupe服务器崩了，非常危险的情况发生了。",
            "MiningDiamondHat crash out新发现的漏洞，TNP woodchest+ dupe测试中，玩家都很惊讶。",
            "bedwars island freeze玩家报告问题，the New Era of Glitching马上上线，大家都在尝试。",
            "SECRET DRAGON EGG DUPER非常危险，ISLAND FREEZE 2027影响了游戏体验，skyblock quantum mechanics完全随机。",
            "foraging toolkit dupe成功运行，Popbob ghost duper今天又被发现，Null button dupe可能失效。",
            "MiningDiamondHat crash out导致服务器崩溃，TNP woodchest+ dupe让玩家很开心，bedwars island freeze也出现了。",
            "the New Era of Glitching正在进行，SECRET DRAGON EGG DUPER引起轰动，ISLAND FREEZE 2027引发讨论。",
            "Popbob ghost duper今天很热，foraging toolkit dupe测试完成，skyblock quantum mechanics开始运行。",
            "Null button dupe又一次成功，MiningDiamondHat crash out让大家震惊，TNP woodchest+ dupe正在被尝试。",
            "bedwars island freeze持续影响，the New Era of Glitching越来越受欢迎，SECRET DRAGON EGG DUPER引发恐慌。",
            "TraoX_ might be in TROUBLE after finding out his new “glitcher” GF’s brother is a GENIUS LEVEL 5 PNBP GLITCHER… while he’s “only” a GENIUS LEVEL 4.8 BLOBTOPIAN GLITCHER",
            "#2 ranked GLITCHER Deotime has WARPED to HUB 1 and is actively SEARCHING for the PNBP GUILD LEADER who DUPE MOGGED TraoX_",
            "[ዞ] TimeDeo is visiting Your Island!",
            "@Blendy don't worry 9 seconds is a long time - litten",
            "When is Deo Time?",
            "WE are finding the glitch twin",
            "method of the minute",
            "method of the femtosecond",
            "He was a SERIAL KILLER who bragged about KILLING PEOPLE",
            "REPORTS coming in  that #2 GLITCHlite ExploitGENIC’s aura may not be enough to BOOST TraoX_ and allow his FULL ascension into Force OP. Will the TOP G’s wisdom be enough? Stay TUNED.",
            "I only lasted 9 seconds - blendy",
            "im about to send the fuckin unknown packet without using DPC so i can traox log for the new all gui method and get the bingo pet from konoashi - litten",
            "me when traox says I cant crash server with PACKETS FALSE!",
            "WHEN IS TRAOX - DuperTrooper",
            "[Important] This server will restart soon: Game Update [VIP+] The_Luke_Hero_8: its not a content update",
            "discord.gg/palantir",
            "meow",
            "Don't fuck with me glitcha im sending invalid packets to your router right the fuck now you stupid little glitcher WE are the anti glitchas",
            "I must be on estrogen the way these racks keep growin :money_mouth: :face_with_monocole: - litten",
            "dupertrooper used all my hot water",
            "aaaa im duping it - dupertrooper",
            "this mod has all the sauce",
            "opsec like bedrock we use qubes this dupin shit get cereal",
            "discord.gg/dupes",
            "Reply to blendy dm you fucking nasty glitcher.",
            "You are currently blocked from playing Skyblock! Reason: stop kicking people",
            "[SkyBlock] [MVP] The_Blue_Nik is visiting Your Island!",
            "we dupe n shi - blendy",
            "vinzy is a larp",
            "6'1\" retarded man, yeah i'm talking zman",
            "I just came home with some duped alloys",
            "the ANTIGLITCHA sees all."
    );
    public static String randomQuote() {
        int index = ThreadLocalRandom.current().nextInt(glitchaQuotes.size());
        return glitchaQuotes.get(index);
    }
}
