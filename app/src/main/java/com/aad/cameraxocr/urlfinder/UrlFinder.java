package com.aad.cameraxocr.urlfinder;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UrlFinder {

    static Pattern URLPATTERN = Pattern.compile("(https?:\\/\\/(?:www\\.|(?!www))[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9]\\.[^\\s]{2,}|www\\.[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9]\\.[^\\s]{2,}|https?:\\/\\/(?:www\\.|(?!www))[a-zA-Z0-9]+\\.[^\\s]{2,}|www\\.[a-zA-Z0-9]+\\.[^\\s]{2,})");

    public static boolean doesURLExists(String text){
        if (text.isEmpty()){
            return false;
        }

        return URLPATTERN.matcher(text).find();
    }

    public static String getUrl(String text){
        if (text.isEmpty()){
            return null;
        }
        Matcher matcher = URLPATTERN.matcher(text);
        if (matcher.find()){
            int matchStart = matcher.start(1);
            int matchEnd = matcher.end();

            return text.substring(matchStart, matchEnd);
        }
        return null;
    }
}
