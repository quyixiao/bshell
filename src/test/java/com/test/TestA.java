package com.test;

import bsh.Interpreter;
import org.junit.Test;

public class TestA {



    @Test
    public void testAMulB() throws Exception {
        Interpreter i = new Interpreter();  // Construct an interpreter
        try {
            Object object = i.source("/Users/quyixiao/project/bshell/src/test/source/ab1.bsh");
            System.out.println(object);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
