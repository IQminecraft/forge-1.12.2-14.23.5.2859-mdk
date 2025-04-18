package com.example.mymod;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.relauncher.Side;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.StringBuilder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Arrays;
import com.example.mymod.AESHelper;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

@Mod(modid = MyMod.MODID, name = MyMod.NAME, version = MyMod.VERSION)
public class MyMod {
    public static final String MODID = "mymod";
    public static final String NAME = "My Mod";
    public static final String VERSION = "1.0";

    public static SimpleNetworkWrapper network;

    private static final Logger logger = LogManager.getLogger(MyMod.class);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // MODの初期化処理
        // チャンネル名を設定
        String channelName = "mymod:channel"; // チャンネル名を適切に設定

        // SimpleNetworkWrapperを作成
        network = NetworkRegistry.INSTANCE.newSimpleChannel(channelName);

        // メッセージを登録
        network.registerMessage(MyMessageHandler.class, MyMessage.class, 0, Side.CLIENT);
        network.registerMessage(MyMessageHandler.class, MyMessage.class, 1, Side.SERVER);

        // GameParamsを初期化
        GameParams.init();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // MODの初期化処理
        MinecraftForge.EVENT_BUS.register(this);
        try {
            String filterPath = GameParams.getValue("filterpath");
            if (filterPath != null) {
                System.out.println("Filter Path: " + filterPath);
            } else {
                System.out.println("Filter path is null"); // デバッグメッセージ
                throw new Exception("Filter path is null");
            }
        } catch (Exception e) {
            String errorMessage = "Error: " + e.getMessage();
            System.out.println(errorMessage); // エラーメッセージをコンソールに表示
            sendChatMessageToAllPlayers(errorMessage);
        }
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        // プレイヤーがログインしたときにフィルターパスをチャットに表示
        String filterPath = GameParams.getValue("filterpath");
        String decryptionKey = GameParams.getValue("filterkey");
        String message = "Filter path or decryption key is not set."; // 初期値を設定

        if (filterPath != null && decryptionKey != null) {
            try {
                // 復号化処理を呼び出す
                ArrayList<String> md5List = readFilterReMd5(filterPath + "/gamelib.txt", decryptionKey);
                ArrayList<String> reList = readFilterReFromFile(filterPath + "/GAME_LIB.txt", decryptionKey, true,
                        md5List.size() == 1 ? (String) md5List.get(0) : null);
                ArrayList<String> combined = new ArrayList<>();
                combined.addAll(reList);

                // combinedの内容をテキストファイルに出力
                writeToFile(filterPath + "/output.txt", combined);

                // 復号化された内容をメッセージとして送信
                message = String.join("\n", combined); // combinedをStringに変換
            } catch (Exception e) {
                message = "Error during decryption: " + e.getMessage();
                System.out.println(message); // エラーメッセージをコンソールに表示
            }
        }

        // 3秒後にメッセージを送信
        MinecraftServer server = net.minecraftforge.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server != null) {
            String finalMessage = message;
            server.addScheduledTask(() -> {
                sendChatMessageToPlayer(event.player, finalMessage);
            });
        }
    }

    private void sendChatMessageToPlayer(EntityPlayer player, String message) {
        player.sendMessage(new TextComponentString(message));
    }

    private void sendChatMessageToAllPlayers(String message) {
        // MinecraftServerのインスタンスを取得
        MinecraftServer server = net.minecraftforge.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server != null) { // サーバーが存在するか確認
            for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
                player.sendMessage(new TextComponentString(message));
            }
        } else {
            System.out.println("サーバーが存在しないため、メッセージを送信できません。");
        }
    }

    private static ArrayList<String> readFilterReMd5(String path, String decryptionKey) {
        ArrayList<String> md5List = new ArrayList();
        if (null != decryptionKey && !decryptionKey.equals("")) {
            Path fileLocation = Paths.get(path);

            try {
                byte[] data = Files.readAllBytes(fileLocation);
                byte[] reBytes = AESHelper.Decrypt(data, decryptionKey);
                String reStr = new String(reBytes, StandardCharsets.UTF_8);
                String[] reStrArray = reStr.split("\n");
                md5List = new ArrayList(Arrays.asList(reStrArray));
            } catch (IOException var8) {
                logger.info("md5 file does not exist!");
            } catch (Exception var9) {
                logger.info("decrpt md5 file error");
            }
        }

        return md5List;
    }

    private static String getStringMd5(byte[] original) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(original);
            byte[] digest = md.digest();
            StringBuffer sb = new StringBuffer();

            for (byte b : digest) {
                sb.append(String.format("%02x", b & 255));
            }

            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "";
        }
    }

    private static ArrayList<String> readFilterReFromFile(String path, String decryptionKey, boolean checkMd5,
            String md5) {
        ArrayList<String> reList = new ArrayList();
        if (null != decryptionKey && "" != decryptionKey && (!checkMd5 || null != md5)) {
            Path fileLocation = Paths.get(path);

            try {
                byte[] data = Files.readAllBytes(fileLocation);
                byte[] reBytes = AESHelper.Decrypt(data, decryptionKey);
                String reStr = new String(reBytes, StandardCharsets.UTF_8);
                String strMd5 = getStringMd5(data);
                if (!reStr.equals("") && checkMd5 && !md5.equals(strMd5)) {
                }

                String[] reStrArray = reStr.split("\n");
                reList = new ArrayList(Arrays.asList(reStrArray));
            } catch (IOException var11) {
                logger.info("reFile does not exist!");
            } catch (Exception var12) {
                logger.info("decrpt filter file error");
            }
        }

        return reList;
    }

    private void writeToFile(String filePath, ArrayList<String> content) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (String line : content) {
                writer.write(line);
                writer.newLine(); // 各行の後に改行を追加
            }
            System.out.println("Output written to file: " + filePath);
        } catch (IOException e) {
            System.out.println("Error writing to file: " + e.getMessage());
        }
    }

}