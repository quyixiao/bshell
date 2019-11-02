package com.test;

import bsh.Interpreter;

public class callFromJava {

    public static void main(String argv[]) throws Exception {

        Interpreter interpreter = new Interpreter();
        interpreter.set("foo", 5);
        interpreter.eval("bar = foo*10");
        Integer bar = (Integer) interpreter.get("bar");

        if (bar.intValue() != 50)
            System.out.println("FAILED...");
        else
            System.out.println("passed...");
    }
}

