package com.zj.im.net.helper;

import com.zj.im.utils.log.NetRecordUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.jetbrains.annotations.Contract;
import org.msgpack.MessagePack;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.msgpack.packer.Packer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * created by ZJJ
 * <p>
 * TCP pack utils
 */
public class TcpMessageUtility {

    private final static int MSG_HEAD_LENGTH = 4;

    @Contract(pure = true)
    private static byte[] convertMsgSizeToBytes(int n) {
        byte[] bigEndian = new byte[4];
        bigEndian[0] = (byte) ((n >> 24) & 0xFF);
        bigEndian[1] = (byte) ((n >> 16) & 0xFF);
        bigEndian[2] = (byte) ((n >> 8) & 0xFF);
        bigEndian[3] = (byte) (n & 0xFF);
        return bigEndian;
    }

    public static Map<String, Object> unpackMsg(byte[] rawMsg) throws IOException {
        NetRecordUtils.recordLastModifyReceiveData(rawMsg.length);
        ObjectMapper objectMapper = new ObjectMapper(new MessagePackFactory());
        TypeReference<Map<String, Object>> typeReference = new TypeReference<Map<String, Object>>() {
        };
        return objectMapper.readValue(rawMsg, typeReference);
    }

    public static byte[] packMap(Map<String, Object> map) throws IOException {
        MessagePack msg_pack = new MessagePack();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Packer packer = msg_pack.createPacker(out);
        packer.write(map);
        byte[] packedData = out.toByteArray();
        int len = packedData.length;
        byte[] pack = new byte[len + MSG_HEAD_LENGTH];
        byte[] head = convertMsgSizeToBytes(len);
        System.arraycopy(head, 0, pack, 0, MSG_HEAD_LENGTH);
        System.arraycopy(packedData, 0, pack, MSG_HEAD_LENGTH, len);
        NetRecordUtils.recordLastModifySendData(pack.length);
        return pack;
    }
}
