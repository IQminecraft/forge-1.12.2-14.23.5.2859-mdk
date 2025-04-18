package com.example.mymod;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class MyMessage implements IMessage {

    // メッセージのデータを格納するフィールド
    private int someData; // 例として整数データを追加

    public MyMessage() {
        // デフォルトコンストラクタ
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        // バイトバッファからメッセージデータを読み込む処理
        this.someData = buf.readInt(); // 例として整数を読み込む
    }

    @Override
    public void toBytes(ByteBuf buf) {
        // メッセージデータをバイトバッファに書き込む処理
        buf.writeInt(this.someData); // 例として整数を書き込む
    }
}