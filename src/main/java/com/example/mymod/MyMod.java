package com.example.mymod;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Arrays;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Mod(
        modid = MyMod.MODID,
        name = MyMod.NAME,
        version = MyMod.VERSION
)
public class MyMod {
    public static final String MODID = "mymod";
    public static final String NAME = "My Mod";
    public static final String VERSION = "1.0";

    // フィルタリスト
    private static List<String> namingFilterRegularExpList = Collections.synchronizedList(new ArrayList<>());
    private static List<String> chatFilterRegularExpList = Collections.synchronizedList(new ArrayList<>());

    /**
     * Modの初期化（事前準備）
     */
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // 必要に応じて初期化処理を追加
    }

    /**
     * Modの初期化
     */
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    /**
     * プレイヤーのログイン時にフィルタをロードしてチャットにログを表示
     */
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        String filePath = "C:\\MCLDownload\\Game\\.minecraft\\GAME_LIB.txt"; // 暗号化されたファイルパス
        String aesKey = "jzwzpzflwmcvatagjwgcxmhxizmfateo"; // 暗号化キー（必ず16/24/32文字）
        String expectedChecksum = "6dac2e9d5ef7a203281e187bfd5a7d4a"; // 正しいチェックサム（暗号化時に保存した値）

        loadFilters(player, filePath, aesKey, expectedChecksum);

        // フィルタ情報のロード
        loadFilters(player);

        // 完了通知
        sendChatMessageToPlayer(player, "フィルタ情報がロードされました");
    }

    /**
     * フィルタデータをロードし、ログをチャットに送信
     */
    private void loadFilters(EntityPlayerMP player) {
        sendChatMessageToPlayer(player, "フィルタ情報のロードを開始します...");

        String decryptionKey = "jzwzpzflwmcvatagjwgcxmhxizmfateo"; // 暗号化キーを指定
        String filterPathPrefex = "C:\\MCLDownload\\Game\\.minecraft"; // フィルタファイルのパス

        // MD5ファイルの読み込み
        List<String> md5List = readFilterReMd5(filterPathPrefex + "/gamelib.txt", decryptionKey, player);

        // フィルタファイルの読み込み
        List<String> reList = readFilterReFromFile(
                filterPathPrefex + "/GAME_LIB.txt",
                decryptionKey,
                true,
                md5List.size() == 1 ? md5List.get(0) : null,
                player
        );

        // フィルタリストをセット
        namingFilterRegularExpList = reList;
        chatFilterRegularExpList = reList;

        sendChatMessageToPlayer(player, "フィルタ情報のロードに成功しました！");
    }

    /**
     * MD5ファイルを読み取り、ログをチャットに送信
     */
    private List<String> readFilterReMd5(String path, String decryptionKey, EntityPlayerMP player) {
        List<String> md5List = new ArrayList<>();

        try {
            sendChatMessageToPlayer(player, "MD5ファイルの読み込み中: " + path);
            byte[] data = Files.readAllBytes(Paths.get(path));
            byte[] reBytes = decrypt(data, decryptionKey, player);
            String reStr = new String(reBytes, StandardCharsets.UTF_8);
            md5List = Arrays.asList(reStr.split("\n"));
            sendChatMessageToPlayer(player, "MD5ファイルの読み込み成功！");
        } catch (IOException e) {
            sendChatMessageToPlayer(player, "エラー: MD5ファイルの読み取りに失敗しました: " + path);
        } catch (Exception e) {
            sendChatMessageToPlayer(player, "エラー: MD5ファイルの復号に失敗しました: " + path);
        }

        return md5List;
    }

    /**
     * フィルタファイルを読み取り、ログをチャットに送信
     */
    private List<String> readFilterReFromFile(String path, String decryptionKey, boolean checkMd5, String md5, EntityPlayerMP player) {
        List<String> reList = new ArrayList<>();

        try {
            sendChatMessageToPlayer(player, "フィルタファイルの読み込み中: " + path);
            byte[] data = Files.readAllBytes(Paths.get(path));
            byte[] reBytes = decrypt(data, decryptionKey, player);
            String reStr = new String(reBytes, StandardCharsets.UTF_8);

            // MD5チェック
            if (checkMd5) {
                String calculatedMd5 = getStringMd5(data);
                if (!calculatedMd5.equals(md5)) {
                    sendChatMessageToPlayer(player, "エラー: MD5ハッシュが一致しません！");
                    return reList;
                }
            }

            reList = Arrays.asList(reStr.split("\n"));
            sendChatMessageToPlayer(player, "フィルタファイルの読み込み成功！");
        } catch (IOException e) {
            sendChatMessageToPlayer(player, "エラー: フィルタファイルの読み取りに失敗しました: " + path);
        } catch (Exception e) {
            sendChatMessageToPlayer(player, "エラー: フィルタファイルの復号に失敗しました: " + path);
        }

        return reList;
    }

    /**
     * MD5を計算し、結果をチャットに送信
     */
    private String getStringMd5(byte[] original) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(original);
            byte[] digest = md.digest();

            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xFF));
            }

            return sb.toString();
        } catch (Exception e) {
            // プレイヤーなしでエラーを記録
            e.printStackTrace();
            return "";
        }
    }

    private byte[] readAndValidateData(String filePath, String expectedChecksum, EntityPlayerMP player) {
        try {
            // データ読み込み
            Path path = Paths.get(filePath);
            byte[] encryptedData = Files.readAllBytes(path);

            // デバッグ: データ情報をログ出力
            System.out.println("データ長: " + encryptedData.length);
            System.out.println("データ (先頭16バイト): " +
                    javax.xml.bind.DatatypeConverter.printHexBinary(Arrays.copyOfRange(encryptedData, 0, Math.min(16, encryptedData.length))));

            // チェックサムを生成して比較
            String checksum = calculateChecksum(encryptedData, "SHA-256");
            System.out.println("現在のチェックサム: " + checksum);

            if (!checksum.equals(expectedChecksum)) {
                sendChatMessageToPlayer(player, "エラー: データが改ざんされています。");
                return null; // 改ざんが検出された場合は処理を中止
            }

            return encryptedData;

        } catch (Exception e) {
            // エラー発生時
            System.err.println("データ読み込み中にエラーが発生しました: " + e.getMessage());
            e.printStackTrace();
            sendChatMessageToPlayer(player, "エラー: データ読み込み中に問題が発生しました！");
            return null;
        }
    }

    private String calculateChecksum(byte[] data, String algorithm) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm); // MD5やSHA-256を使用
            byte[] hashBytes = digest.digest(data);

            StringBuilder checksum = new StringBuilder();
            for (byte b : hashBytes) {
                checksum.append(String.format("%02x", b)); // 16進数に変換
            }
            return checksum.toString();
        } catch (Exception e) {
            // ハッシュ計算エラー時
            System.err.println("チェックサム計算中にエラーが発生しました: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public void loadFilters(EntityPlayerMP player, String filePath, String aesKey, String expectedChecksum) {
        // データ読み込みと検証
        byte[] encryptedData = readAndValidateData(filePath, expectedChecksum, player);

        if (encryptedData == null) {
            sendChatMessageToPlayer(player, "エラー: フィルターデータの読み込みに失敗しました。");
            return;
        }

        // 読み込んだデータを復号化
        byte[] decryptedData = decrypt(encryptedData, aesKey, player);

        if (decryptedData == null) {
            sendChatMessageToPlayer(player, "エラー: フィルターデータの復号化に失敗しました。");
            return;
        }

        // 復号化成功時の処理
        sendChatMessageToPlayer(player, "フィルターデータの読み込みと復号に成功しました。");
        System.out.println("フィルターの復号データ: " + new String(decryptedData, StandardCharsets.UTF_8));
    }


    /**
     * AES復号化
     */
    private byte[] decrypt(byte[] data, String aesKey, EntityPlayerMP player) {
        try {
            // 暗号化キーとIVを生成
            byte[] keyBytes = aesKey.getBytes(StandardCharsets.UTF_8);
            byte[] ivBytes = Arrays.copyOfRange(keyBytes, 16, 32); // IV (初期化ベクトル)
            keyBytes = Arrays.copyOfRange(keyBytes, 0, 16); // AESキー

            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

            // 暗号復号用のCipherオブジェクトを準備
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

            // 復号処理を実行
            return cipher.doFinal(data);

        } catch (Exception e) {
            // エラー時の処理: チャットメッセージで通知
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);

            sendChatMessageToPlayer(player, "エラー: AES復号中に問題が発生しました！");
            sendChatMessageToPlayer(player, sw.toString()); // スタックトレースも送信（分割対応は必要に応じて実装）

            System.out.println("AES復号中にエラーが発生: " + e.getMessage());
            e.printStackTrace();

            return null;
        }
    }



    /**
     * プレイヤーにチャットメッセージを送信
     */
    private void sendChatMessageToPlayer(EntityPlayerMP player, String message) {
        if (player != null) {
            player.sendMessage(new TextComponentString(message));
        }
    }
}