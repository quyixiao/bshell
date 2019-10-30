package com.test;

import bsh.Interpreter;
import org.junit.Test;

public class TestA {



    @Test
    public void testAMulB() throws Exception {
        Interpreter i = new Interpreter();  // Construct an interpreter
        try {
            Object object = i.source("/Users/quyixiao/git/bshell/src/test/source/ab.bsh");
            System.out.println(object);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
