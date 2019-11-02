//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package bsh.org.objectweb.asm;

final class Item {
    short index;
    int type;
    int intVal;
    long longVal;
    float floatVal;
    double doubleVal;
    String strVal1;
    String strVal2;
    String strVal3;
    int hashCode;
    Item next;

    Item() {
    }

    Item(short index, Item i) {
        this.index = index;
        this.type = i.type;
        this.intVal = i.intVal;
        this.longVal = i.longVal;
        this.floatVal = i.floatVal;
        this.doubleVal = i.doubleVal;
        this.strVal1 = i.strVal1;
        this.strVal2 = i.strVal2;
        this.strVal3 = i.strVal3;
        this.hashCode = i.hashCode;
    }

    void set(int intVal) {
        this.type = 3;
        this.intVal = intVal;
        this.hashCode = this.type + intVal;
    }

    void set(long longVal) {
        this.type = 5;
        this.longVal = longVal;
        this.hashCode = this.type + (int) longVal;
    }

    void set(float floatVal) {
        this.type = 4;
        this.floatVal = floatVal;
        this.hashCode = this.type + (int) floatVal;
    }

    void set(double doubleVal) {
        this.type = 6;
        this.doubleVal = doubleVal;
        this.hashCode = this.type + (int) doubleVal;
    }

    void set(int type, String strVal1, String strVal2, String strVal3) {
        this.type = type;
        this.strVal1 = strVal1;
        this.strVal2 = strVal2;
        this.strVal3 = strVal3;
        switch (type) {
            case 1:
            case 7:
            case 8:
                this.hashCode = type + strVal1.hashCode();
                return;
            case 12:
                this.hashCode = type + strVal1.hashCode() * strVal2.hashCode();
                return;
            default:
                this.hashCode = type + strVal1.hashCode() * strVal2.hashCode() * strVal3.hashCode();
        }
    }

    boolean isEqualTo(Item i) {
        if (i.type != this.type) {
            return false;
        } else {
            switch (this.type) {
                case 1:
                case 7:
                case 8:
                    return i.strVal1.equals(this.strVal1);
                case 2:
                case 9:
                case 10:
                case 11:
                default:
                    return i.strVal1.equals(this.strVal1) && i.strVal2.equals(this.strVal2) && i.strVal3.equals(this.strVal3);
                case 3:
                    return i.intVal == this.intVal;
                case 4:
                    return i.floatVal == this.floatVal;
                case 5:
                    return i.longVal == this.longVal;
                case 6:
                    return i.doubleVal == this.doubleVal;
                case 12:
                    return i.strVal1.equals(this.strVal1) && i.strVal2.equals(this.strVal2);
            }
        }
    }
}
