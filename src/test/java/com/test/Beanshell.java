package com.test;

import bsh.Interpreter;
import org.junit.Test;

public class Beanshell {

    /**
     * @param args
     */
    @Test
    public void testBillqQry() throws Exception {
        Interpreter i = new Interpreter();  // Construct an interpreter
        Interpreter.DEBUG.set(true);
        try {
            Object object = i.source("/Users/quyixiao/git/beanshell/src/test/bsh/login.bsh");
            System.out.println(object);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}