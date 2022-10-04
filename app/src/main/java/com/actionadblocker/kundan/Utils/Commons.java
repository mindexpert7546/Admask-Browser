package com.actionadblocker.kundan.Utils;

import android.net.Uri;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;

import com.actionadblocker.kundan.Models.FileType;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public  class Commons {

    public static List<String> extractUrls(String input) {
        List<String> result = new ArrayList<String>();

        input=input.trim();
        if(URLUtil.isValidUrl(input))
        {
            result.add(input);
        }
        else
        {
            Pattern pattern = Pattern.compile(
                    "\\b(((ht|f)tp(s?)\\:\\/\\/|~\\/|\\/)|www.)" +
                            "(\\w+:)?(([-\\w]+\\.)+(com|org|net|gov" +
                            "|mil|biz|info|mobi|name|aero|jobs|museum" +
                            "|travel|[a-z]{2}))(:[\\d]{1,5})?" +
                            "(((\\/([-\\w~!$+|.,=]|%[a-f\\d]{2})+)+|\\/)+|\\?|#)?" +
                            "((\\?([-\\w~!$+|.,*:]|%[a-f\\d{2}])+=?" +
                            "([-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)" +
                            "(&(?:[-\\w~!$+|.,*:]|%[a-f\\d{2}])+=?" +
                            "([-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)*)*" +
                            "(#([-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)?\\b");

            Matcher matcher = pattern.matcher(input);
            while (matcher.find()) {
                result.add(matcher.group());
            }
        }

        return result;
    }

    public static FileType GetFileType(Uri u)
    {
        String type = "."+ MimeTypeMap.getFileExtensionFromUrl(u.toString());
        if(type.equals(".jpeg") || type.equals(".jpeg")  || type.equals(".png")|| type.equals(".jpg") || type.equals(".gif") || type.equals(".webp"))
        {
            return  FileType.IMAGE;
        }
        else if(type.equals(".mp4") || type.equals(".mkv")|| type.equals(".webem") || type.equals(".wmv") )
        {
            return  FileType.VIDEO;
        }
        else if(type.equals(".mp3") )
        {
            return  FileType.AUDIO;
        }
        else
        {
            return FileType.OTHER;
        }
    }



}
