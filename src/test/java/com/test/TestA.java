package com.test;

import bsh.Interpreter;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;


public class TestA {



    @Test
    public void testAMulB() throws Exception {
        Interpreter i = new Interpreter();  // Construct an interpreter
        Interpreter.DEBUG = true;
        try {
            Object object = i.source("/Users/quyixiao/project/bshell/src/test/source/ab1.bsh");
            System.out.println(JSON.toJSONString(object));
            System.out.println(object);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Test
    public void testSwith() throws Exception {

        Interpreter i = new Interpreter();  // Construct an interpreter
        Interpreter.DEBUG = true;
        try {
            Object object = i.source("/Users/quyixiao/project/bshell/src/test/source/swith.bsh");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
