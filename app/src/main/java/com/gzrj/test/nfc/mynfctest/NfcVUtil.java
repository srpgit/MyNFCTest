package com.gzrj.test.nfc.mynfctest;

import android.nfc.tech.NfcV;

import java.io.IOException;

/**
 * NfcV读写工具类
 * Created by RP_S on 2017/10/16.
 *
 * @version 2017/10/18
 */

public class NfcVUtil {
    public static final String CHAR_SET = "utf-8";

    /**
     * 指令集
     */
    private enum Commands {
        default_flag(0x22),

        read_block(0x20),

        read_batch(0x23),

        write_block(0x21),

        write_batch(0x24),

        write_AFI(0x27),

        lock_AFI(0x28),

        write_DSFID(0x29),

        lock_DSFID(0x2A),

        tag_info(0x2B);

        private int code;

        Commands(int code) {
            this.code = code;
        }
    }

    private NfcV mNfcV;
    /*
     * UID数组形式
     */
    private byte[] ID;
    private String UID;
    //Data Storage Format Identifier
    private String DSFID;
    //Application Family Identifier
    private String AFI;
    /*
     * block的个数
     */
    private int blockNumber;
    /*
     * 一个block长度
     */
    private int oneBlockSize;
    /**
     * block总存储空间，字节数
     */
    private int blockSpace;
    /*
     * 信息
     */
    private byte[] infoRmation;

    /**
     * 初始化
     *
     * @param mNfcV NfcV对象
     * @throws IOException
     */
    public NfcVUtil(NfcV mNfcV) throws IOException {
        this.mNfcV = mNfcV;
        ID = this.mNfcV.getTag().getId();
        byte[] uid = new byte[ID.length];
        int j = 0;
        for (int i = ID.length - 1; i >= 0; i--) {
            uid[j] = ID[i];
            j++;
        }
        this.UID = printHexString(uid);
        getInfoRmation();
    }

    /**
     * nfc元件唯一id
     *
     * @return
     */
    public String getUID() {
        return UID;
    }

    /**
     * Data Storage Format Identifier
     *
     * @return
     */
    public String getDSFID() {
        return DSFID;
    }

    /**
     * Application Family Identifier
     *
     * @return
     */
    public String getAFI() {
        return AFI;
    }

    /**
     * block总存储空间，字节数
     */
    public int getBlockSpace() {
        return getBlockNumber() * getOneBlockSize();
    }

    public int getBlockNumber() {
        return blockNumber + 1;
    }

    public int getOneBlockSize() {
        return oneBlockSize + 1;
    }

    /**
     * 如未连接，先连接。连接失败时抛出异常
     *
     * @throws ConnectFailedException
     */
    public void assertConnected() throws ConnectFailedException {
        if (!mNfcV.isConnected()) {
            try {
                mNfcV.connect();
            } catch (IOException e) {
                throw new ConnectFailedException();
            }
        }
    }

    private byte[] initCmd(Commands command) {
        return initCmd(command, 0);
    }

    private byte[] initCmd(Commands command, int dataLength) {
        byte[] cmd = new byte[2 + ID.length + dataLength];
        cmd[0] = (byte) Commands.default_flag.code;
        cmd[1] = (byte) command.code;
        System.arraycopy(ID, 0, cmd, 2, ID.length);
        return cmd;
    }

    /**
     * 打印字符数组
     *
     * @param bytes
     * @return
     */
    private String byteString(byte[] bytes) {
        StringBuilder s = new StringBuilder();
        for (byte b : bytes) {
            s.append(b).append("_");
        }
        return s.toString();
    }

    /**
     * 将byte[]转换成16进制字符串
     *
     * @param data 要转换成字符串的字节数组
     * @return 16进制字符串
     */

    private String printHexString(byte[] data) {
        StringBuffer s = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            String hex = Integer.toHexString(data[i] & 0xFF).toUpperCase();
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            s.append(hex);
        }
        return s.toString();
    }

    /**
     * 取得标签信息
     */
    private byte[] getInfoRmation() throws IOException {
        assertConnected();
        byte[] cmd = initCmd(Commands.tag_info);
        infoRmation = mNfcV.transceive(cmd);
        blockNumber = infoRmation[12];
        oneBlockSize = infoRmation[13];
        AFI = printHexString(new byte[]{infoRmation[11]});
        DSFID = printHexString(new byte[]{infoRmation[10]});
        return infoRmation;
    }

    /**
     * 清空数据区
     *
     * @return
     */
    public boolean clearAllBlocks() throws IOException {
        assertConnected();
        int n = this.getBlockNumber();
        byte[] bytes = new byte[this.getOneBlockSize()];
        //用0清空
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = 0x00;
        }
        for (int i = 0; i < n; i++) {
            this.write(i, bytes);
        }
        return true;
    }

    /**
     * 读取所有block数据
     *
     * @return
     * @throws IOException
     */
    public byte[] readAllBlocks() throws IOException {
        assertConnected();
        byte cmd[] = initCmd(Commands.default_flag.read_batch, 2);
        cmd[10] = 0x00;
        cmd[11] = (byte) this.getBlockNumber();
        byte res[] = mNfcV.transceive(cmd);
        if (res[0] == 0x00) {
            byte result[] = new byte[res.length - 1];
            System.arraycopy(res, 0, result, 0, this.getOneBlockSize() * this.getBlockNumber());
            return result;
        }
        return null;
    }

    /**
     * 读取所有block信息，返回字符串
     *
     * @return
     * @throws IOException
     */
    public String readAll() throws IOException {
        return new String(this.readAllBlocks(), CHAR_SET);
    }

    //failed。会失败，原因尚未找到
    private boolean write(String s) throws IOException {
        if (s == null || s.length() == 0) {
            return true;
        }
        assertConnected();
        byte[] bytes = s.getBytes(CHAR_SET);
        int end = (int) Math.ceil((bytes.length + 0.0) / this.getOneBlockSize());
        end = Math.min(end, this.getBlockNumber());
        //截取bytes
        int len = end * this.getOneBlockSize();
        //bytes只可能小于等于data长度
        byte[] data = new byte[len];
        //将bytes内容复制到data中，多余的位置为空
        System.arraycopy(bytes, 0, data, 0, bytes.length);

        byte[] cmd = initCmd(Commands.write_batch, 2 + data.length);

        cmd[10] = 0x00;
        cmd[11] = (byte) end;
        System.arraycopy(data, 0, cmd, 12, data.length);

        byte res[] = mNfcV.transceive(cmd);
        return res[0] == 0x00;
    }

    /**
     * 写入一block数据
     *
     * @param block block编号
     * @param data  数据，长度只能是1block长度
     * @return
     * @throws IOException
     * @throws IllegalArgumentException
     */
    public boolean write(int block, byte[] data) throws IOException, IllegalArgumentException {
        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }
        if (data.length != this.getOneBlockSize()) {
            throw new IllegalArgumentException("data length must be one block size:" + this.getOneBlockSize());
        }
        assertConnected();
        byte[] cmd = initCmd(Commands.write_block, 1 + data.length);
        cmd[10] = (byte) block;
        System.arraycopy(data, 0, cmd, 11, data.length);
        byte res[] = mNfcV.transceive(cmd);
        return res[0] == 0x00;
    }

    /**
     * 从第零block开始写入字符串，使用默认编码{@link NfcVUtil#CHAR_SET}。
     * 若字符串长度大于block总空间，则多余的内容丢失。会先清空所有内容
     *
     * @param s 字符串
     * @return
     * @throws IOException
     * @throws IllegalArgumentException
     */
    public boolean writeString(String s) throws IOException, IllegalArgumentException {
        if (s == null || s.length() == 0) {
            return true;
        }

        this.clearAllBlocks();

        byte[] bytes = s.getBytes(CHAR_SET);

        int end = (int) Math.ceil((bytes.length + 0.0) / this.getOneBlockSize());

        end = Math.min(end, this.getBlockNumber());

        //截取bytes
        int len = end * this.getOneBlockSize();
        //bytes只可能小于等于data长度
        byte[] data = new byte[len];
        //将bytes内容复制到data中，多余的位置为空
        System.arraycopy(bytes, 0, data, 0, Math.min(bytes.length, data.length));

        for (int i = 0; i < end; i++) {
            byte[] block = new byte[this.getOneBlockSize()];
            System.arraycopy(data, i * this.getOneBlockSize(), block, 0, block.length);
            this.write(i, block);
        }
        return true;
    }

    /**
     * 写afi。写入以后下次读取就是这个值了
     *
     * @param AFI
     * @return
     * @throws IOException
     */
    public boolean writeAFI(byte AFI) throws IOException {
        assertConnected();
        byte[] cmd = initCmd(Commands.write_AFI, 1);
        cmd[10] = AFI;
        byte res[] = mNfcV.transceive(cmd);
        return res[0] == 0x00;
    }

    /**
     * 锁afi。不清楚锁是什么作用，锁了以后不能改？
     *
     * @return
     * @throws IOException
     */
    public boolean lockAFI() throws IOException {
        assertConnected();
        byte[] cmd = initCmd(Commands.lock_AFI);
        byte res[] = mNfcV.transceive(cmd);
        return res[0] == 0x00;
    }

    /**
     * 写dsfid。写入以后下次读取就是这个值了
     *
     * @param DSFID
     * @return
     * @throws IOException
     */
    public boolean writeDSFID(byte DSFID) throws IOException {
        assertConnected();
        byte[] cmd = initCmd(Commands.write_DSFID, 1);
        cmd[10] = DSFID;
        byte res[] = mNfcV.transceive(cmd);
        return res[0] == 0x00;
    }

    /**
     * 锁dsfid。不清楚锁是什么作用，锁了以后不能改？
     *
     * @return
     * @throws IOException
     */
    public boolean lockDSFID() throws IOException {
        assertConnected();
        byte[] cmd = initCmd(Commands.lock_DSFID);
        byte res[] = mNfcV.transceive(cmd);
        return res[0] == 0x00;
    }

}
