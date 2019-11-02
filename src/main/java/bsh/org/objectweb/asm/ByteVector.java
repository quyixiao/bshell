//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package bsh.org.objectweb.asm;

final class ByteVector {
    byte[] data;
    int length;

    public ByteVector() {
        this.data = new byte[64];
    }

    public ByteVector(int initialSize) {
        this.data = new byte[initialSize];
    }

    public ByteVector put1(int b) {
        int length = this.length;
        if (length + 1 > this.data.length) {
            this.enlarge(1);
        }

        this.data[length++] = (byte) b;
        this.length = length;
        return this;
    }

    public ByteVector put11(int b1, int b2) {
        int length = this.length;
        if (length + 2 > this.data.length) {
            this.enlarge(2);
        }

        byte[] data = this.data;
        data[length++] = (byte) b1;
        data[length++] = (byte) b2;
        this.length = length;
        return this;
    }

    public ByteVector put2(int s) {
        int length = this.length;
        if (length + 2 > this.data.length) {
            this.enlarge(2);
        }

        byte[] data = this.data;
        data[length++] = (byte) (s >>> 8);
        data[length++] = (byte) s;
        this.length = length;
        return this;
    }

    public ByteVector put12(int b, int s) {
        int length = this.length;
        if (length + 3 > this.data.length) {
            this.enlarge(3);
        }

        byte[] data = this.data;
        data[length++] = (byte) b;
        data[length++] = (byte) (s >>> 8);
        data[length++] = (byte) s;
        this.length = length;
        return this;
    }

    public ByteVector put4(int i) {
        int length = this.length;
        if (length + 4 > this.data.length) {
            this.enlarge(4);
        }

        byte[] data = this.data;
        data[length++] = (byte) (i >>> 24);
        data[length++] = (byte) (i >>> 16);
        data[length++] = (byte) (i >>> 8);
        data[length++] = (byte) i;
        this.length = length;
        return this;
    }

    public ByteVector put8(long l) {
        int length = this.length;
        if (length + 8 > this.data.length) {
            this.enlarge(8);
        }

        byte[] data = this.data;
        int i = (int) (l >>> 32);
        data[length++] = (byte) (i >>> 24);
        data[length++] = (byte) (i >>> 16);
        data[length++] = (byte) (i >>> 8);
        data[length++] = (byte) i;
        i = (int) l;
        data[length++] = (byte) (i >>> 24);
        data[length++] = (byte) (i >>> 16);
        data[length++] = (byte) (i >>> 8);
        data[length++] = (byte) i;
        this.length = length;
        return this;
    }

    public ByteVector putUTF(String s) {
        int charLength = s.length();
        int byteLength = 0;

        int length;
        for (length = 0; length < charLength; ++length) {
            char c = s.charAt(length);
            if (c >= 1 && c <= 127) {
                ++byteLength;
            } else if (c > 2047) {
                byteLength += 3;
            } else {
                byteLength += 2;
            }
        }

        if (byteLength > 65535) {
            throw new IllegalArgumentException();
        } else {
            length = this.length;
            if (length + 2 + byteLength > this.data.length) {
                this.enlarge(2 + byteLength);
            }

            byte[] data = this.data;
            data[length++] = (byte) (byteLength >>> 8);
            data[length++] = (byte) byteLength;

            for (int i = 0; i < charLength; ++i) {
                char c = s.charAt(i);
                if (c >= 1 && c <= 127) {
                    data[length++] = (byte) c;
                } else if (c > 2047) {
                    data[length++] = (byte) (224 | c >> 12 & 15);
                    data[length++] = (byte) (128 | c >> 6 & 63);
                    data[length++] = (byte) (128 | c & 63);
                } else {
                    data[length++] = (byte) (192 | c >> 6 & 31);
                    data[length++] = (byte) (128 | c & 63);
                }
            }

            this.length = length;
            return this;
        }
    }

    public ByteVector putByteArray(byte[] b, int off, int len) {
        if (this.length + len > this.data.length) {
            this.enlarge(len);
        }

        if (b != null) {
            System.arraycopy(b, off, this.data, this.length, len);
        }

        this.length += len;
        return this;
    }

    private void enlarge(int size) {
        int length1 = 2 * this.data.length;
        int length2 = this.length + size;
        byte[] newData = new byte[length1 > length2 ? length1 : length2];
        System.arraycopy(this.data, 0, newData, 0, this.length);
        this.data = newData;
    }
}
