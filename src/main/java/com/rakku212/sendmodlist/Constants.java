package com.rakku212.sendmodlist;

//? if >=1.17 {
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//?} else {
/*import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
*///?}

public class Constants {
    public static final String MOD_ID = "sendmodlist";
    public static final String MOD_NAME = "SendModList";
    //? if >=1.17 {
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);
    //?} else {
    /*public static final Logger LOG = LogManager.getLogger(MOD_NAME);
    *///?}
}
