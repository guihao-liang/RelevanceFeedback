package adb;

import java.util.regex.Pattern;

/**
 * Created by GuihaoLiang on 10/5/16.
 * used to test unicode
 */
public class TestGui {
    // test regex
    public static void main(String[] args) {
        String test = "SumMary: Find Contemporary ?????-  ‘gui ’hao “liang” Blues AkaA Albums, Artists and Songs, guiHao and Editorial Picked Contemporary Blues Music on \n JavaScript AllMusic";

        // Pattern regex = Pattern.compile("[\\s\\p{Punct}]+", Pattern.UNICODE_CHARACTER_CLASS);
//        String[] res = test.split("[\\s\\p{Punct}‘’“”]+(\\p{Upper}?\\p{Lower}+\\p{Upper}+\\p{Alnum}*)?[\\s\\p{Punct}‘’“”]?");
        String[] res = test.split("[\\s\\p{Punct}‘’“”]+");
        // String[] res = regex.split(test);
        for (String s : res) {
            //if (s.isEmpty()) System.out.println("empty");
            System.out.println(s);
        }
    }
}
