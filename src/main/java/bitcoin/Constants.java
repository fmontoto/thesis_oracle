package bitcoin;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by fmontoto on 23-11-16.
 */
public class Constants {

    static private Constants instance = null;

    private Map<String, Byte> hashTypes;
    private Map<String, Byte> opcodes;
    private Map<Byte, String> reversedOpcodes;
    private Map<Byte, String> reversedHashTypes;

    private Constants() {
        hashTypes = new HashMap<String, Byte>();
        opcodes = new HashMap<String, Byte>();
        reversedOpcodes = new HashMap<Byte, String>();
        reversedHashTypes = new HashMap<Byte, String>();

        addHashType("ALL",            (byte) 0x01);
        addHashType("NONE",           (byte) 0x02);
        addHashType("SINGLE",         (byte) 0x03);
        addHashType("ANYONECANPAY",   (byte) 0x80);

// Data Control
        addOpcode("OP_0",           (byte) 0x00);
        addOpcode("OP_PUSHDATA1",   (byte) 0x4c);
        addOpcode("OP_PUSHDATA2",   (byte) 0x4d);
        addOpcode("OP_PUSHDATA4",   (byte) 0x4e);
        addOpcode("OP_1NEGATE",     (byte) 0x4f);
        addOpcode("OP_RESERVED",    (byte) 0x50);
        addOpcode("OP_1",           (byte) 0x51);
        addOpcode("OP_2",           (byte) 0x52);
        addOpcode("OP_3",           (byte) 0x53);
        addOpcode("OP_4",           (byte) 0x54);
        addOpcode("OP_5",           (byte) 0x55);
        addOpcode("OP_6",           (byte) 0x56);
        addOpcode("OP_7",           (byte) 0x57);
        addOpcode("OP_8",           (byte) 0x58);
        addOpcode("OP_9",           (byte) 0x59);
        addOpcode("OP_10",          (byte) 0x5a);
        addOpcode("OP_11",          (byte) 0x5b);
        addOpcode("OP_12",          (byte) 0x5c);
        addOpcode("OP_13",          (byte) 0x5d);
        addOpcode("OP_14",          (byte) 0x5e);
        addOpcode("OP_15",          (byte) 0x5f);
        addOpcode("OP_16",          (byte) 0x60);


// Flow Control
        addOpcode("OP_NOP",         (byte) 0x61);
        addOpcode("OP_VER",         (byte) 0x62);
        addOpcode("OP_IF",          (byte) 0x63);
        addOpcode("OP_NOTIF",       (byte) 0x64);
        addOpcode("OP_VERIF",       (byte) 0x65);
        addOpcode("OP_VERNOTIF",    (byte) 0x66);
        addOpcode("OP_ELSE",        (byte) 0x67);
        addOpcode("OP_ENDIF",       (byte) 0x68);
        addOpcode("OP_VERIFY",      (byte) 0x69);
        addOpcode("OP_RETURN",      (byte) 0x6a);

// Stack
        addOpcode("OP_TOALTSTACK",  (byte) 0x6b);
        addOpcode("OP_FROMALTSTACK",(byte) 0x6c);
        addOpcode("OP_2DROP",       (byte) 0x6d);
        addOpcode("OP_2DUP",        (byte) 0x6e);
        addOpcode("OP_3DUP",        (byte) 0x6f);
        addOpcode("OP_2OVER",       (byte) 0x70);
        addOpcode("OP_2ROT",        (byte) 0x71);
        addOpcode("OP_2SWAP",       (byte) 0x72);
        addOpcode("OP_IFDUP",       (byte) 0x73);
        addOpcode("OP_DEPTH",       (byte) 0x74);
        addOpcode("OP_DROP",        (byte) 0x75);
        addOpcode("OP_DUP",         (byte) 0x76);
        addOpcode("OP_NIP",         (byte) 0x77);
        addOpcode("OP_OVER",        (byte) 0x78);
        addOpcode("OP_PICK",        (byte) 0x79);
        addOpcode("OP_ROLL",        (byte) 0x7a);
        addOpcode("OP_ROT",         (byte) 0x7b);
        addOpcode("OP_SWAP",        (byte) 0x7c);
        addOpcode("OP_TUCK",        (byte) 0x7d);

// Splice
        addOpcode("OP_CAT",         (byte) 0x7e);
        addOpcode("OP_SUBSTR",      (byte) 0x7f);
        addOpcode("OP_LEFT",        (byte) 0x80);
        addOpcode("OP_RIGHT",       (byte) 0x81);
        addOpcode("OP_SIZE",        (byte) 0x82);

// BitLogic
        addOpcode("OP_INVERT",      (byte) 0x83);
        addOpcode("OP_AND",         (byte) 0x84);
        addOpcode("OP_OR",          (byte) 0x85);
        addOpcode("OP_XOR",         (byte) 0x86);
        addOpcode("OP_EQUAL",       (byte) 0x87);
        addOpcode("OP_EQUALVERIFY", (byte) 0x88);
        addOpcode("OP_RESERVED1",   (byte) 0x89);
        addOpcode("OP_RESERVED2",   (byte) 0x8a);


// Arithmetic
        addOpcode("OP_1ADD",        (byte) 0x8b);
        addOpcode("OP_1SUB",        (byte) 0x8c);
        addOpcode("OP_2MUL",        (byte) 0x8d);
        addOpcode("OP_2DIV",        (byte) 0x8e);
        addOpcode("OP_NEGATE",      (byte) 0x8f);
        addOpcode("OP_ABS",         (byte) 0x90);
        addOpcode("OP_NOT",         (byte) 0x91);
        addOpcode("OP_0NOTEQUAL",   (byte) 0x92);

        addOpcode("OP_ADD",         (byte) 0x93);
        addOpcode("OP_SUB",         (byte) 0x94);
        addOpcode("OP_MUL",         (byte) 0x95);
        addOpcode("OP_DIV",         (byte) 0x96);
        addOpcode("OP_MOD",         (byte) 0x97);
        addOpcode("OP_LSHIFT",      (byte) 0x98);
        addOpcode("OP_RSHIFT",      (byte) 0x99);

        addOpcode("OP_BOOLAND",             (byte) 0x9a);
        addOpcode("OP_BOOLOR",              (byte) 0x9b);
        addOpcode("OP_NUMEQUAL",            (byte) 0x9c);
        addOpcode("OP_NUMEQUALVERIFY",      (byte) 0x9d);
        addOpcode("OP_NUMNOTEQUAL",         (byte) 0x9e);
        addOpcode("OP_LESSTHAN",            (byte) 0x9f);
        addOpcode("OP_GREATERTHAN",         (byte) 0xa0);
        addOpcode("OP_LESSTHANOREQUAL",     (byte) 0xa1);
        addOpcode("OP_GREATERTHANOREQUAL",  (byte) 0xa2);
        addOpcode("OP_MIN",                 (byte) 0xa3);
        addOpcode("OP_MAX",                 (byte) 0xa4);
        addOpcode("OP_WITHIN",              (byte) 0xa5);

// Crypto
        addOpcode("OP_RIPEMD160",           (byte) 0xa6);
        addOpcode("OP_SHA1",                (byte) 0xa7);
        addOpcode("OP_SHA256",              (byte) 0xa8);
        addOpcode("OP_HASH160",             (byte) 0xa9);
        addOpcode("OP_HASH256",             (byte) 0xaa);
        addOpcode("OP_CODESEPARATOR",       (byte) 0xab);
        addOpcode("OP_CHECKSIG",            (byte) 0xac);
        addOpcode("OP_CHECKSIGVERIFY",      (byte) 0xad);
        addOpcode("OP_CHECKMULTISIG",       (byte) 0xae);
        addOpcode("OP_CHECKMULTISIGVERIFY", (byte) 0xaf);

// Expansion
        addOpcode("OP_NOP1",                (byte) 0xb0);
        addOpcode("OP_CHECKLOCKTIMEVERIFY", (byte) 0xb1);
        addOpcode("OP_CHECKSEQUENCEVERIFY", (byte) 0xb2);
        addOpcode("OP_NOP4",                (byte) 0xb3);
        addOpcode("OP_NOP5",                (byte) 0xb4);
        addOpcode("OP_NOP6",                (byte) 0xb5);
        addOpcode("OP_NOP7",                (byte) 0xb6);
        addOpcode("OP_NOP8",                (byte) 0xb7);
        addOpcode("OP_NOP9",                (byte) 0xb8);
        addOpcode("OP_NOP10",               (byte) 0xb9);

// PseudoWords
        addOpcode("OP_SMALLINTEGER",        (byte) 0xfa);
        addOpcode("OP_PUBKEYS",             (byte) 0xfb);
        addOpcode("OP_PUBKEYHASH",          (byte) 0xfd);
        addOpcode("OP_PUBKEY",              (byte) 0xfe);
        addOpcode("OP_INVALIDOPCODE",       (byte) 0xff);
    }

    static public byte[] pushDataOpcode(int dataLength) {
        if(dataLength < 0)
            throw new InvalidParameterException("Data must be greater than 0.");
        if(dataLength < 75)
            return new byte[]{(byte)dataLength};
        if(dataLength < 256)
            return new byte[]{0x4c, (byte)(dataLength & 0xFF)};
        throw new NotImplementedException();
    }
    static public byte getOpcode(String name) {
        return getInstance().opcodes.get(name);
    }


    static public String getOpcodeName(byte b) {
        return getInstance().reversedOpcodes.get(b);
    }

    static public Boolean isHashType(Byte b) {
        return getInstance().reversedHashTypes.containsKey(b);
    }

    static public Boolean isOpcode(Byte b) {
        return getInstance().reversedOpcodes.containsKey(b);
    }

    static public Constants getInstance() {
        if(instance == null)
            instance = new Constants();
        return instance;
    }

    static public Byte getHashType(String name) {
        return getInstance().hashTypes.get(name);
    }

    static public String getHashTypeName(byte b) {
        return getInstance().reversedHashTypes.get(b);
    }

    static private void add(String name, Byte b, Map<String, Byte> map, Map<Byte, String> reverseMap) {
        map.put(name, b);
        reverseMap.put(b, name);
    }

    private void addOpcode(String name, Byte code) {
        add(name, code, opcodes, reversedOpcodes);
    }

    private void addHashType(String name, Byte code) {
        add(name, code, hashTypes, reversedHashTypes);
    }

}


